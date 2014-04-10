package psi;

import java.math.BigInteger;
import java.util.Random;
import java.util.Arrays;

public class ElGamalTester {
	
	private static void ShuffleArray(Object[] array)
	{
	    Object temp;
	    int index;
	    Random random = new Random();
	    for (int i = array.length - 1; i > 0; i--)
	    {
	        index = random.nextInt(i + 1);
	        temp = array[index];
	        array[index] = array[i];
	        array[i] = temp;
	    }
	}

	public static void main(String[] args) {
		int x = 3;
		Random rand = new Random();
		CommutativeElGamal[] elgs = new CommutativeElGamal[x];
		PohligHellman[] phs = new PohligHellman[x];
		for (int i = 0; i < elgs.length; i++) {
			phs[i] = new PohligHellman();
			elgs[i] = new CommutativeElGamal(x-i);
		}
		
		final BigInteger[] datum = {new BigInteger(60, rand)};
		System.out.println("A : " + datum[0].toString());
		BigInteger[][] encrypted = new BigInteger[elgs.length][];
		BigInteger[][] partcrypted = new BigInteger[elgs.length][];
		BigInteger[][] decrypted = new BigInteger[elgs.length][];
		encrypted[0] = elgs[0].encrypt(datum);
		System.out.println("B0:" + Arrays.toString(encrypted[0]));
		for (int i = 1; i < elgs.length; i++) {
			encrypted[i] = elgs[i].encrypt(encrypted[i-1]);
			System.out.println("B" + i + ":" + Arrays.toString(encrypted[i]));
		}
		
		ShuffleArray(elgs);
		
		partcrypted[0] = elgs[0].partialDecrypt(encrypted[elgs.length-1]);
		for (int j = 1; j < partcrypted[0].length; j += 2) {
			partcrypted[0][j] = phs[0].encrypt(partcrypted[0][j]);
		}
		partcrypted[0][partcrypted[0].length - 1] = phs[0].encrypt(partcrypted[0][partcrypted[0].length - 1]); 
		System.out.println("C0:" + Arrays.toString(partcrypted[0]));
		for (int i = 1; i < elgs.length; i++) {
			partcrypted[i] = elgs[i].partialDecrypt(partcrypted[i-1]);
			for (int j = 1; j < partcrypted[i].length; j += 2) {
				partcrypted[i][j] = phs[i].encrypt(partcrypted[i][j]);
			}
			partcrypted[i][partcrypted[i].length - 1] = phs[i].encrypt(partcrypted[i][partcrypted[i].length - 1]);
			System.out.println("C" + i + ":" + Arrays.toString(partcrypted[i]));
		}
		
		ShuffleArray(phs);
		
		decrypted[0] = new BigInteger[]{phs[0].decrypt(partcrypted[phs.length-1][0])};
		System.out.println("D0:" + Arrays.toString(decrypted[0]));
		for (int i = 1; i < phs.length; i++) {
			decrypted[i] = new BigInteger[]{phs[i].decrypt(decrypted[i-1][0])};
			System.out.println("D" + i + ":" + Arrays.toString(decrypted[i]));
		}
		
		
		System.out.println(decrypted[elgs.length-1][0].equals(datum[0]));
		
		BigInteger datum2 = datum[0];
		System.out.println("A : " + datum2.toString());
		for (int i = 0; i < phs.length; i++) {
			datum2 = phs[i].encrypt(datum2);
			System.out.println("B" + i + ": " + datum2.toString());
		}
		for (int i = 0; i < phs.length; i++) {
			datum2 = phs[i].decrypt(datum2);
			System.out.println("C" + i + ": " + datum2.toString());
		}
		
	}

}
