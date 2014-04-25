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
	public ArrayList<BigInteger[]> myFile;
	public ArrayList<BigInteger[]> encryptedFile;
	public ArrayList<BigInteger> encryptedIntersection;
	public ArrayList<BigInteger> finalIntersection;
	public HashMap<String, ArrayList<BigInteger> > intersectionSet;
	private PohligHellman ph;

	public Data(String fileName){
		ph = new PohligHellman();
		this.fileName = fileName;
		Intersect.println("file name:" + fileName);
		myFile = new ArrayList<BigInteger[]>();
		encryptedFile = new ArrayList<BigInteger[]>();
		intersectionSet = new HashMap<String, ArrayList<BigInteger> >();
	}

	public Data(String fileName, BigInteger prime){
		ph = new PohligHellman(prime);
		this.fileName = fileName;
		Intersect.println("file name:" + fileName);
		myFile = new ArrayList<BigInteger[]>();
		encryptedFile = new ArrayList<BigInteger[]>();
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
		if (this.encryptedFile.size() != 0 ){
			Intersect.println("[Data] Already encrypted!");
			return;
		}
		encryptedFile = encryptFile(myFile, elg);
		Intersect.println("[Data] Got my file encrypted!");
		Intersect.println("[Test]last line:" + encryptedFile.get(encryptedFile.size()-1)[0]);
	}


	public void shuffleMyEncFile(){
		Collections.shuffle(encryptedFile);
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

	public ArrayList<BigInteger[]> encryptFile(ArrayList<BigInteger[]> file, CommutativeElGamal elg){
		ArrayList<BigInteger[]> encFile  = new ArrayList<BigInteger[]>();
		for (BigInteger[] val : file){

			/* 
			 * decrypt from ElGamal, then encrypt all remaining c1s and c2 with PohligHellman.
			 * For example, if xi is i's ElGamal private key, yi is the ephemeral private key that goes
			 * with i's public key, and and zi is i's Pohlig-Hellman encryption key, then 
			 * [1, g^y1, 2, g^y2, m*g^(x1y1+x2y2)]
			 * is operated on by 1, and becomes
			 * [2, g^y2z1, m*g^x2y2z1].
			 */
			BigInteger[] decrypted = elg.partialDecrypt(val);
			for (int i = 1; i < decrypted.length; i += 2) {
				decrypted[i] = ph.encrypt(decrypted[i]);
			}
			decrypted[decrypted.length - 1] = ph.encrypt(decrypted[decrypted.length - 1]); 
			encFile.add(decrypted);
		}
		return encFile;
	}

	public void decryptMyIntersection() {
		encryptedIntersection = decryptIntersection(encryptedIntersection);
	}

	public ArrayList<BigInteger> decryptIntersection(ArrayList<BigInteger> list) {
		ArrayList<BigInteger> decrypted = new ArrayList<BigInteger>();
		for (BigInteger bi : list) {
			decrypted.add(ph.decrypt(bi));
		}
		return decrypted;
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
