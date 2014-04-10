package psi;

import java.security.SecureRandom;
import java.util.Random;
import java.math.BigInteger;

public class PohligHellman {

	protected Random rng;
	private BigInteger p; // prime number
	private BigInteger encryptionKey;
	private BigInteger decryptionKey;
	
	public static BigInteger presetPrime = ElGamal.prime1024;
	

	/**
	 * Generates a new PohligHellman object with preset group and randomly generated keys.
	 */
	public PohligHellman() {
		rng = new SecureRandom();
		p = presetPrime;
		generateKeys();
	}

	/**
	 * Generates a new PohligHellman object with selected group.
	 * @param p A prime number.
	 */
	public PohligHellman(BigInteger p) {
		rng = new SecureRandom();
		this.p = p;
		generateKeys();
	}

	/**
	 * Generates a new PohligHellman object with the default group, and selected keys.
	 * Note that no check is done to ensure that the keys are valid.
	 * @param encryptionKey The encryption key.
	 * @param decryptionKey The decryption key. Must have encryptionKey * decryptionKey = 1 mod p-1.
	 */
	public PohligHellman(BigInteger encryptionKey, BigInteger decryptionKey) {
		rng = new SecureRandom();
		p = presetPrime;
		this.encryptionKey = encryptionKey;
		this.decryptionKey = decryptionKey;
	}

	/**
	 * Generates a new PohligHellman object with selected group and selected keys.
	 * Note that no check is done to ensure that the keys are valid.
	 * @param p A prime number.
	 * @param encryptionKey The encryption key.
	 * @param decryptionKey The decryption key. Must have encryptionKey * decryptionKey = 1 mod p-1.
	 */
	public PohligHellman(BigInteger p, BigInteger encryptionKey, BigInteger decryptionKey) {
		rng = new SecureRandom();
		this.p = p;
		this.encryptionKey = encryptionKey;
		this.decryptionKey = decryptionKey;
	}

	private void generateKeys() {
		encryptionKey = new BigInteger(p.bitLength()-1, rng);
		while (!encryptionKey.gcd(p.subtract(BigInteger.ONE)).equals(BigInteger.ONE)) {
			encryptionKey = encryptionKey.add(BigInteger.ONE);
			if (encryptionKey.bitLength() > p.bitLength()-1){
				encryptionKey = new BigInteger(p.bitLength()-1, rng);
			}

		}
		decryptionKey = encryptionKey.modInverse(p.subtract(BigInteger.ONE));
	}

	public BigInteger getPrime() {
		return p;
	}
	/*
	protected BigInteger getEncryptionKey() {
		return encryptionKey;
	}

	protected BigInteger getDecryptionKey() {
		return decryptionKey;
	}
	*/

	/**
	 * Encrypts data using this object's key.
	 * @param data BigInteger data to encrypt.
	 * @return Encrypted data.
	 */
	public BigInteger encrypt(BigInteger data) {
		return data.modPow(encryptionKey, p);
	}

	/**
	 * Decrypts ciphertext using this object's key.
	 * @param ciphertext The ciphertext to decrypt.
	 * @return Plaintext decryption of ciphertext.
	 */
	public BigInteger decrypt(BigInteger ciphertext) {
		return ciphertext.modPow(decryptionKey, p);
	}
}
