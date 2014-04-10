/*
 * Encrypts lines or ciphertexts.
 * Lines beginning with # are echoed without encryption.
 * Lines beginning with % are assumed to be ciphertexts, which get re-encrypted.
 * All other lines are treated as strings.
 */

package psi;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Scanner;
public class Dec {

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("USAGE:");
			System.err.println("% Enc key_filename [input_filename] [output_filename]");
		}

		String key_filename = args[0];
		String output_filename, input_filename;
		if (args.length > 1) {
			input_filename = args[1];
		} else {
			input_filename = "-";
		}

		if (args.length > 2) {
			output_filename = args[2];
		} else {
			output_filename= "-";
		}
		
		int id;
		BigInteger privateKey;
		File priv, output = null, input = null;
		Scanner spriv, sinput = null;
		FileWriter ofw = null;
		BufferedWriter bww = null;
		boolean overwrite = false;
		
		try {
			// Private key file
			priv = new File(key_filename);
			if (!priv.exists()) {
				System.err.println("Error: File doesn't exist");
				return;
			}
			spriv = new Scanner(priv.getAbsoluteFile());
			id = Integer.parseInt(spriv.nextLine());
			privateKey = new BigInteger(spriv.nextLine());
			spriv.close(); 

			// Input
			if (!input_filename.equals("-")) {
				input = new File(input_filename);
				sinput = new Scanner(input.getAbsoluteFile());
			} else {
				sinput = new Scanner(System.in);
			}
			
			// Output
			if (!output_filename.equals("-")) {
				if (input_filename.equals(output_filename)) {
					overwrite = true;
					output_filename = output_filename + "_TEMP_TEMP";  
				}
				output = new File(output_filename);
				output.createNewFile();
				ofw = new FileWriter(output.getAbsoluteFile());
				bww = new BufferedWriter(ofw);
			} else {
				bww = new BufferedWriter(new OutputStreamWriter(System.out));
			}

			CommutativeElGamal elg = new CommutativeElGamal(id, privateKey);

			int skipped = 0;
			int decrypted = 0;
			while (sinput.hasNextLine()) {
				String str = sinput.nextLine();
				if (str.isEmpty()) {
					break;
				}

				if (str.startsWith("#")) {
					bww.write(str);
					skipped++;
				} else if (str.startsWith("%")) {
					str = str.substring(1).trim();
					str = str.replaceAll("[\\[|\\]]","");
					String[] bigints = str.split(", ");
					BigInteger[] cipher = new BigInteger[bigints.length];
					for (int i = 0; i < cipher.length; i++) {
						cipher[i] = new BigInteger(bigints[i]);
					}
					BigInteger[] post = elg.partialDecrypt(cipher);
					if (post.length == 1) { 
						bww.write(BigIntegerEncoding.decode(post[0]));
					} else {
						bww.write("%" + Arrays.toString(post));
					}
					decrypted++;
				} else {
					System.err.println("Error - line " + (decrypted + skipped) + " is not a valid ciphertext.");
					bww.write(str);
					skipped++;
				}
				bww.newLine();
				bww.flush();
			}
			System.out.print("Decryption complete. " + decrypted + " items decrypted");
			if (skipped > 0) {
				System.out.println(" and " + skipped + " items skipped.");
			} else{
				System.out.println(".");
			}
			sinput.close();
			bww.close();
			
			if (overwrite) {
				input.delete();
				if (!output.renameTo(input)) {
					System.err.println("Error - rename failed. Output should still be in " + output_filename);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

}
