package psi;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Data {

	public String fileName;
	public int numThreads;
	public ArrayList<BigInteger[]> myFile;
	public ArrayList<BigInteger> encryptedIntersection;
	public ArrayList<BigInteger> finalIntersection;
	public HashMap<String, ArrayList<BigInteger> > intersectionSet;
	private PohligHellman ph;

	public Data(String fileName){
		this(fileName, 1);
	}

	public Data(String fileName, int numThreads){
		ph = new PohligHellman();
		this.fileName = fileName;
		this.numThreads = 1;
		Intersect.println("file name:" + fileName);
		myFile = new ArrayList<BigInteger[]>();
		intersectionSet = new HashMap<String, ArrayList<BigInteger> >();
	}

	public Data(String fileName, BigInteger prime){
		this(fileName, prime, 1);
	}

	public Data(String fileName, BigInteger prime, int numThreads){
		ph = new PohligHellman(prime);
		this.fileName = fileName;
		this.numThreads = numThreads;
		Intersect.println("file name:" + fileName);
		myFile = new ArrayList<BigInteger[]>();
		intersectionSet = new HashMap<String, ArrayList<BigInteger> >();
	}

	public void storeListForIntersection(String id, ArrayList<BigInteger[]> content){
		ArrayList<BigInteger> results = new ArrayList<BigInteger>();
		for (BigInteger[] b : content) {
			results.add(b[0]);
		}
		this.intersectionSet.put(id, results);
		Intersect.println("[Data]Stored data " + id + ".");
	}

	/**
	 * Reads in the file of ciphertexts and stores it in encodedFile.
	 * @return true if success, false if failure
	 */
	public boolean readInFile(){
		Intersect.println("[Data] start read in file...");
		Intersect.println("[Data]" + fileName);
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					//ignore comments
					continue;
				} else if (!line.startsWith("%")) {
					//not valid encryption
					System.err.println("[Error] Cannot read encrypted input data!");
					reader.close();
					return false;
				} else {
					//interpret line as ElGamal ciphertext
					line = line.substring(1).trim(); // expecting line to begin with %
					line = line.replaceAll("[\\[|\\]]","");
					String[] bigints = line.split(", ");
					BigInteger[] cipher = new BigInteger[bigints.length];
					for (int i = 0; i < cipher.length; i++) {
						cipher[i] = new BigInteger(bigints[i]);
					}
					myFile.add(cipher);
				}
			}
			reader.close();
			Intersect.println("[Data]read file success !");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}





	}

	public void encryptMyFile(CommutativeElGamal elg){
		encryptFile(myFile, elg);
		Intersect.println("[Data] Got my file encrypted!");
	}


	public void shuffleMyFile(){
		Collections.shuffle(myFile);
	}

	public void shuffleMyEncIntersection() {
		Collections.shuffle(encryptedIntersection);
	}



	/*
	public BigInteger murmurHashString(String str){
		// use murmurhash 64 
		Long hashResult = MurmurHash.hash64(str);
		return new BigInteger(hashResult.toString());
	}

	public long murmurHashBytes(byte[] data){
		int length = data.length;
		return MurmurHash.hash64(data, length);
	}

	public byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.putLong(x);
	    return buffer.array();
	}

	public long bytesToLong(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.put(bytes);
	    buffer.flip();//need flip 
	    return buffer.getLong();
	}
	 */

	public void encryptFile(ArrayList<BigInteger[]> file, CommutativeElGamal elg){
		EncryptWorker threads[];
		if (numThreads == 0) {
			threads = new EncryptWorker[file.size()];
		} else {
			threads = new EncryptWorker[numThreads];
		}
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new EncryptWorker(file.listIterator(i * file.size() / threads.length), 
					(i+1) * file.size() / threads.length, elg, ph);
			threads[i].start();
		}
		for (int i = 0; i < threads.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				System.err.println(e);
			}
		}
	}

	public void decryptMyIntersection() {
		decryptIntersection(encryptedIntersection);
	}

	public void decryptIntersection(ArrayList<BigInteger> list) {
		DecryptWorker threads[];
		if (numThreads == 0) {
			threads = new DecryptWorker[list.size()];
		} else {
			threads = new DecryptWorker[numThreads];
		}
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new DecryptWorker(list.listIterator(i * list.size() / threads.length), 
					(i+1) * list.size() / threads.length, ph);
			threads[i].start();
		}
		for (int i = 0; i < threads.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				System.err.println(e);
			}
		}
	}

	public int computeIntersection(){
		// return how many are the same
		encryptedIntersection = cloneList(this.intersectionSet.values().iterator().next()); // get one value
		for (ArrayList<BigInteger> en_list : this.intersectionSet.values()){
			encryptedIntersection.retainAll(en_list);
		}
		int num = encryptedIntersection.size();
		Intersect.println("[Data]Size of intersection = "+ num);
		return num;
	}

	public static ArrayList<BigInteger> cloneList(ArrayList<BigInteger> list) {
		ArrayList<BigInteger> clone = new ArrayList<BigInteger>();
		for(BigInteger item: list){
			clone.add(item);
		} 
		return clone;
	}
}
