package psi;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

public class BigIntegerEncoding {
	/**
	 * Returns a BigInteger representing string str. If str is longer than
	 * 127 chars or contains non-ASCII, returns null.
	 * @param str A string
	 * @return A BigInteger representing the str
	 */
	public static BigInteger encode(String str) {
		BigInteger out;
		try {
			byte[] bytes = str.getBytes("US-ASCII");
			out = new BigInteger(bytes);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		if (out.compareTo(ElGamal.prime1024) > 0) {
			System.err.println("String too long error: " + str);
			return null;
		}
		return out;
	}

	/**
	 * Returns a String that the given integer represents.
	 * @param bi A BigInteger representing a string
	 * @return the string
	 */
	public static String decode(BigInteger bi) {
		String out;
		try {
			byte[] bytes = bi.toByteArray();
			out = new String(bytes, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		return out;
	}
}
