package psi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

/*
 * This is just a simple message handler thread.
 * Starting one thread per message, on the assumption
 * that there will be very few messages.
 */

public class MsgHandler extends Thread {

	private Intersect user;
	private Msg msg;

	public MsgHandler(Intersect user, Msg msg) {
		this.user = user;
		this.msg = msg;
	}

	public void gotPassOnToEncryptMsg(){
		// check if I need to encrypt, 
		if (!msg.operatedOnBy.contains(user.id)){
			//if yes, encrypt, and pass on to next
			System.out.println("process msg to pass on...");
			ArrayList<BigInteger[]> enc_cont = user.myData.encryptFile(msg.arrContent, user.elg); // encrypt
			Collections.shuffle(enc_cont);
			msg.arrContent = enc_cont; // shuffle
			msg.operatedOnBy.add(user.id); // add encrypted by
			user.nextSocket.sendMsg(msg); // send to next


		} else {
			// if I have already encrypted, 
			// create DoneEncrypted msg send to next
			System.out.println("Message encrypted by all users...");
			msg.type = Msg.Type.DONE_STAGE_ONE;
			user.myData.storeListForIntersection(msg.origin, msg.arrContent);	// store in Data
			msg.whoGot.add(user.id); // note i got the file
			user.nextSocket.sendMsg(msg); // send to next
		}
	}

	public void gotDoneEncryptedMsg() {
		// check if i got the message before
		// if got, stop pass it on
		// if not, store, add my name into who got and pass on
		if (true == msg.whoGot.contains(user.id)) {
			//long duration = System.currentTimeMillis() - user.time1;
			System.out.println("Done with stage one.");
			//System.out.println("Stage one duration: " + duration + " ms");
			synchronized (user) {
				user.notify();
			}
		} else {
			System.out.println("Final my message to broadcast...");
			user.myData.storeListForIntersection(msg.origin, msg.arrContent);	// store in Data
			msg.whoGot.add(user.id); // note i got the file
			user.nextSocket.sendMsg(msg); // send to next
		}

	}

	public void gotPassOnToDecryptMsg() {
		// check if I need to decrypt, 
		if (!msg.operatedOnBy.contains(user.id)){
			//if yes, decrypt, and pass on to next
			System.out.println("process msg to pass on...");
			ArrayList<BigInteger> dec_cont = user.myData.decryptIntersection(msg.content); // encrypt
			Collections.shuffle(dec_cont); // shuffle
			msg.content = dec_cont;
			msg.operatedOnBy.add(user.id); // add encrypted by
			user.nextSocket.sendMsg(msg); // send to next
		} else {
			// if I have already decrypted, 
			// stop
			user.myData.finalIntersection = msg.content;	// store in Data
			//long duration = System.currentTimeMillis() - user.time2;
			System.out.println("Done with stage two.");
			//System.out.println("Stage two duration: " + duration + " ms");
			synchronized (user) {
				user.finalData++;
				user.notify();
			}
		}
	}

	public void run() {

		// check message
		System.out.println("[From Prev obj]" + msg.type );
		if (msg.type == Msg.Type.STAGE_ONE) {
			gotPassOnToEncryptMsg();
		} else if (msg.type == Msg.Type.DONE_STAGE_ONE) {
			gotDoneEncryptedMsg();
		} else if (msg.type == Msg.Type.STAGE_TWO) {
			gotPassOnToDecryptMsg();
		}		
	}

}
