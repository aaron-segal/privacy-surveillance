package psi;

import java.math.BigInteger;
import java.util.ListIterator;

public class DecryptWorker extends Thread {

	private ListIterator<BigInteger> iterator;
	private PohligHellman ph;
	private int maxItem;
	public DecryptWorker(ListIterator<BigInteger> iterator, int maxItem, PohligHellman ph) {
		this.iterator = iterator;
		this.maxItem = maxItem;
		this.ph = ph;
	}
	
	public void run() {
		int i = iterator.nextIndex();
		Intersect.println("Starting decryption with item " + i);	
		for (; i < maxItem && iterator.hasNext(); i++){

			BigInteger val = iterator.next();
			iterator.set(ph.decrypt(val));
		}
		Intersect.println("Finished decryption at item " + i);
	}
	
}
