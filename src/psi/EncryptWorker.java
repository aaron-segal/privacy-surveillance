package psi;

import java.math.BigInteger;
import java.util.ListIterator;

public class EncryptWorker extends Thread {

	private ListIterator<BigInteger[]> iterator;
	private CommutativeElGamal elg;
	private PohligHellman ph;
	private int maxItem;
	public EncryptWorker(ListIterator<BigInteger[]> iterator, int maxItem, CommutativeElGamal elg, PohligHellman ph) {
		this.iterator = iterator;
		this.maxItem = maxItem;
		this.elg = elg;
		this.ph = ph;
	}
	
	public void run() {
		for (int i = iterator.nextIndex(); i < maxItem && iterator.hasNext(); i++){

			/* 
			 * decrypt from ElGamal, then encrypt all remaining c1s and c2 with PohligHellman.
			 * For example, if xi is i's ElGamal private key, yi is the ephemeral private key that goes
			 * with i's public key, and and zi is i's Pohlig-Hellman encryption key, then 
			 * [1, g^y1, 2, g^y2, m*g^(x1y1+x2y2)]
			 * is operated on by 1, and becomes
			 * [2, g^y2z1, m*g^x2y2z1].
			 */
			BigInteger[] val = iterator.next();
			BigInteger[] decrypted = elg.partialDecrypt(val);
			for (int j = 1; j < decrypted.length; j += 2) {
				decrypted[j] = ph.encrypt(decrypted[j]);
			}
			decrypted[decrypted.length - 1] = ph.encrypt(decrypted[decrypted.length - 1]); 
			iterator.set(decrypted);
		}
	}
	
}
