package psi;
import java.io.*; 
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;

import psi.Msg.Type;

public class SocketForPrev extends Thread {

	private Socket prevSocket = null;
	//private PrintWriter out = null;
	//private BufferedReader in = null;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;

	private Intersect user;
	private String prevNodeIP;
	private int prevNodePort;
	private int bytesRead = 0;
	private ArrayDeque<MsgHandler> queue;
	public static final int MAX_TRIES = 10;
	public static final long SLEEP_BETWEEN_TRIES = 1000;

	public SocketForPrev(Intersect user, String ip, int port){
		this.user = user;
		this.prevNodeIP = ip;
		this.prevNodePort = port;
		queue = new ArrayDeque<MsgHandler>();
		start();
	}
	/*
	public void sendToPrevNode(String msg){
		out.println(msg);
		Intersect.println("[To Prev Node]" + msg);
	}

	public String readFromPrevNode(){
		String inLine = null;
		try {
			inLine = in.readLine();
			Intersect.println("[From Prev Node]" + inLine);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return inLine;
	}
	 */

	public int closeSocket(){
		try {
			//in.close();
			//out.close();
			user.prevConnected = false;
			inputStream.close();
			outputStream.close();
			prevSocket.close();
		} catch (IOException e) {

		}
		return bytesRead;
	}

	public void run() {
		Intersect.println ("Attemping to connect to host " +
				prevNodeIP + " on port " + prevNodePort + ".");

		for (int i = 0; i < MAX_TRIES && !user.prevConnected; i++) {
			try {
				this.prevSocket = new Socket(prevNodeIP, prevNodePort);
				//out = new PrintWriter(prevSocket.getOutputStream(), true);
				//in = new BufferedReader(new InputStreamReader(prevSocket.getInputStream()));

				outputStream = new ObjectOutputStream(prevSocket.getOutputStream());
				inputStream = new ObjectInputStream(prevSocket.getInputStream());
				Intersect.println("[Previous node] connected!");
				user.prevConnected = true;
			} catch (UnknownHostException e) {
				System.err.println("Don't know about host: " + prevNodeIP);
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Waiting for connection to " + prevNodeIP + ":" + prevNodePort);
				try {
					Thread.sleep(SLEEP_BETWEEN_TRIES);
				} catch (InterruptedException f) {
					continue;
				}
			}
		}
		// start waiting for prev node 
		if (user.prevConnected) { 
			waitingForPrevNodeMsgObj();
		} else {
			System.err.println("Could not connect to " + prevNodeIP);
			System.exit(1);
		}

	}

	/*
	// this is for string 
	public void waitingForPrevNodeMsg(){
		String inputLine = null;
		Intersect.println("Waiting for prev node to send msg ...");
		try {
			while ((inputLine = in.readLine()) != null){ 
				pendingMsgFromPrev(inputLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	 */

	public int getBytesRead() {
		return bytesRead;
	}

	public void waitingForPrevNodeMsgObj(){
		Msg msgIn = null;
		try {
			while (user.prevConnected) {
				msgIn = (Msg) inputStream.readObject();
				if (msgIn == null) {
					break;
				}

				// instrumentation stuff 
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(msgIn);
				oos.close();
				bytesRead += baos.toByteArray().length;

				MsgHandler msgHandle = new MsgHandler(user, msgIn);
				if (user.stageTwoReady || (msgIn.type != Type.STAGE_TWO && msgIn.type != Type.ERROR)) {
					msgHandle.start();
				} else {
					queue.add(msgHandle);
				}
			}
		} catch (EOFException e) {

		} catch (SocketException e) {

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void startQueue() {
		while (!queue.isEmpty()) {
			queue.remove().start();
		}
	}
}
