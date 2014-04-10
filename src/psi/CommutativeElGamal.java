package psi;

import java.math.BigInteger;

public class CommutativeElGamal extends ElGamal {

	private static final int MAX_ID = 10000; 
	
	private int id;

	/**
	 * Generates a new CommutativeElGamal object with preset group and generator,
	 * and randomly generated keys and id.
	 */
	public CommutativeElGamal() {
		super();
		id = getRNG().nextInt(MAX_ID);
	}

	/**
	 * Generates a new ElGamal object with selected group and generator, randomly generated keys,
	 * and random id.
	 * @param p A prime number.
	 * @param g A generator of the group Zp*.
	 */
	public CommutativeElGamal(BigInteger p, BigInteger g) {
		super(p, g);
		id = getRNG().nextInt(MAX_ID);
	}

	/**
	 * Generates a new ElGamal object with selected group, generator, private key, and random id.
	 * @param p A prime number.
	 * @param g A generator of the group Zp*.
	 * @param privateKey A number < p/2.
	 */
	public CommutativeElGamal(BigInteger p, BigInteger g, BigInteger privateKey) {
		super(p, g, privateKey);
		id = getRNG().nextInt(MAX_ID);
	}

	/**
	 * Generates a new ElGamal object with preset group and generator, selected 
	 * private key, and random id.
	 * @param id The id for this keypair.
	 * @param privateKey A number < p/2.
	 */
	public CommutativeElGamal(BigInteger privateKey) {
		super(privateKey);
		id = getRNG().nextInt(MAX_ID);
	}

	/**
	 * Generates a new CommutativeElGamal object with preset group and generator,
	 * randomly generated keys, and selected id.
	 * @param id The id for this keypair.
	 */
	public CommutativeElGamal(int id) {
		super();
		this.id = id;
	}

	/**
	 * Generates a new ElGamal object with selected group and generator, randomly generated keys,
	 * and selected id.
	 * @param id The id for this keypair.
	 * @param p A prime number.
	 * @param g A generator of the group Zp*.
	 */
	public CommutativeElGamal(int id, BigInteger p, BigInteger g) {
		super(p, g);
		this.id = id;
	}

	/**
	 * Generates a new ElGamal object with selected group, generator, private key, and id.
	 * @param id The id for this keypair.
	 * @param p A prime number.
	 * @param g A generator of the group Zp*.
	 * @param privateKey A number < p/2.
	 */
	public CommutativeElGamal(int id, BigInteger p, BigInteger g, BigInteger privateKey) {
		super(p, g, privateKey);
		this.id = id;
	}

	/**
	 * Generates a new ElGamal object with preset group and generator, and selected 
	 * private key and id.
	 * @param id The id for this keypair.
	 * @param privateKey A number < p/2.
	 */
	public CommutativeElGamal(int id, BigInteger privateKey) {
		super(privateKey);
		this.id = id;
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		this.id = id;
	}

	/**
	 * Encrypts data, using publicKey that belongs to the same group as this object.
	 * @param id ID of publicKey used for encryption
	 * @param publicKey public key
	 * @param data to encrypt
	 * @return Encrypted data (id, c1, c2)
	 */
	public BigInteger[] encrypt(int id, BigInteger publicKey, BigInteger data){
		BigInteger[] out = new BigInteger[3];
		BigInteger[] c = super.encrypt(publicKey, data);
		out[0] = new BigInteger(Integer.toString(id));
		out[1] = c[0];
		out[2] = c[1];
		return out;
	}

	
	/*
	 * This method is not recommended. It 
	 * Encrypts data, using public key that belongs to the same group as this ElGamal object.
	 * @param data BigInteger data to encrypt.
	 * @param publicKey Public key.
	 * @return Encrypted data (two bigIntegers)
	 * /
	public BigInteger[] encrypt(BigInteger publicKey, BigInteger data) {
		return encrypt(0, publicKey, data);
	}
	*/

	public BigInteger[] encrypt(BigInteger data) {
		return encrypt(id, getPublicKey(), data);
	}

	public BigInteger[] encrypt(int id, BigInteger publicKey, BigInteger[] data) {
		BigInteger[] out = new BigInteger[data.length + 2];
		BigInteger[] c = encrypt(id, publicKey, data[data.length-1]);
		int i;
		BigInteger idbi = new BigInteger(Integer.toString(id));
		//Copy (id, c1) pairs from old data until it's time to insert our id
		for (i = 0; data[i].compareTo(idbi) <= 0 && i < data.length - 1; i += 2) {
			out[i] = data[i];
			out[i+1] = data[i+1];
		}
		//It's now time to insert our (id, c1) pair
		out[i] = c[0];
		out[i+1] = c[1];
		//Now copy until the last element
		for (; i < data.length - 1; i++) {
			out[i+2] = data[i];
		}
		//Finally, insert the last element
		out[i+2] = c[2];
		return out;
	}

	public BigInteger[] encrypt(BigInteger[] data) {
		return encrypt(id, getPublicKey(), data);
	}

	public BigInteger[] partialDecrypt(BigInteger[] data) {
		int i;

		//Find the c1 used with this key's id
		for (i = 0; i < data.length - 1; i += 2) {
			try {
				if (data[i].intValueExact() == id) {
					break;
				}
			} catch (ArithmeticException e) {
				//This should never happen, but all the same.
				e.printStackTrace();
				continue;
			}
		}
		//If you can't find it, don't change data
		if (i == data.length - 1) {
			System.err.println("Error: Could not decrypt with id " + id);
			return data;
		}

		//Once it's found, remove a layer of encryption
		BigInteger[] partialCipher = {data[i+1], data[data.length-1]};
		BigInteger partialPlain = super.decrypt(partialCipher);
		BigInteger[] out = new BigInteger[data.length - 2];
		int j;
		for (j = 0; j < i; j++) {
			out[j] = data[j];
		}
		for (j = i; j < out.length - 1; j++) {
			out[j] = data[j+2];
		}
		out[out.length - 1] = partialPlain;

		return out;		
	}

	public BigInteger decrypt(BigInteger[] data) {
		if (data.length > 3) {
			System.err.println("Error! You cannot fully decrypt multiply-encrypted data.");
			return data[data.length - 1];
		}
		try {
			if (data.length == 3 && data[0].intValueExact() != id) {
				System.err.println("Error - id used to encrypt does not match this id");
				return data[data.length - 1];
			}
		} catch (ArithmeticException e) {
			//This should never happen, but all the same.
			e.printStackTrace();
			return data[data.length - 1];
		}
		BigInteger[] decryptData = {data[data.length - 2], data[data.length - 1]};
		return super.decrypt(decryptData);
	}

}
