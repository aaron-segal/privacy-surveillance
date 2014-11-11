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
		for (int i = iterator.nextIndex(); i < maxItem && iterator.hasNext(); i++){

			BigInteger val = iterator.next();
			iterator.set(ph.decrypt(val));
		}
	}
	
}
