/*
 * Encrypts lines or ciphertexts.
 * Lines beginning with # are echoed without encryption.
 * Lines beginning with % are assumed to be ciphertexts, which get re-encrypted.
 * All other lines are treated as strings.
 */

package psi;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
public class Enc {

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

		ArrayList<Integer> ids = new ArrayList<Integer>();
		ArrayList<BigInteger> publicKeys = new ArrayList<BigInteger>();
		BigInteger prime = null, generator = null;
		File pub, output = null, input = null;
		Scanner spub, sinput = null;
		FileWriter ofw = null;
		BufferedWriter bww = null;
		boolean overwrite = false;

		try {
			// Public key file
			pub = new File(key_filename);
			if (!pub.exists()) {
				System.err.println("Error: File doesn't exist");
				return;
			}

			spub = new Scanner(pub.getAbsoluteFile());
			while (spub.hasNextLine()) {
				String line = spub.nextLine();
				String [] lineParts = new String[2];
				if (line.startsWith("#") || line.startsWith("!")) {
					continue;
				} else if (line.contains("=")) {
					lineParts = line.split("=", 2);
					lineParts[0] = lineParts[0].trim();
					lineParts[1] = lineParts[1].trim();
				} else if (line.contains(":")) {
					lineParts = line.split(":", 2);
				} else if (line.contains(" ")) {
					lineParts = line.split(" ", 2);
				} else {
					System.err.println("Could not parse key file.");
					spub.close();
					return;
				}
				if (lineParts[0].equalsIgnoreCase(KeyGen.ID)) {
					ids.add(Integer.parseInt(lineParts[1]));	
				} else if (lineParts[0].equalsIgnoreCase(KeyGen.PUBLIC_KEY)) {
					publicKeys.add(new BigInteger(lineParts[1]));
				} else if (lineParts[0].equalsIgnoreCase(KeyGen.PRIME)) {
					if (prime == null) {
						prime = new BigInteger(lineParts[1]);
					} else if (!prime.equals(new BigInteger(lineParts[1]))) {
						System.err.println("Error: Key file may have only one unique prime.");
						spub.close();
						return;
					}
				} else if (lineParts[0].equalsIgnoreCase(KeyGen.GENERATOR)) {
					if (generator == null) {
						generator = new BigInteger(lineParts[1]);
					} else if (!generator.equals(new BigInteger(lineParts[1]))) {
						System.err.println("Error: Key file may have only one unique generator.");
						spub.close();
						return;
					}
				}
			}

			spub.close(); 

			CommutativeElGamal elg = null;
			if (prime != null && generator != null) {
				elg = new CommutativeElGamal(prime, generator);
			} else if (prime == null && generator == null) {
				elg = new CommutativeElGamal();
			} else {
				System.err.println("Error: Must specify either prime AND generator, or neither.");
				return;
			}
			
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

			int skipped = 0;
			int encrypted = 0;
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
					BigInteger[] cipher= new BigInteger[bigints.length];
					for (int i = 0; i < cipher.length; i++) {
						cipher[i] = new BigInteger(bigints[i]);
					}
					for (int i = 0; i < ids.size(); i++) {
						cipher = elg.encrypt(ids.get(i), publicKeys.get(i), cipher);
					}
					bww.write("%" + Arrays.toString(cipher));
					encrypted++;
				} else {
					BigInteger[] cipher = new BigInteger[]{BigIntegerEncoding.encode(str)};
					for (int i = 0; i < ids.size(); i++) {
						cipher = elg.encrypt(ids.get(i), publicKeys.get(i), cipher);
					}
					bww.write("%" + Arrays.toString(cipher));
					encrypted++;
				}
				bww.newLine();
				bww.flush();
			}
			System.out.print("Encryption complete. " + encrypted + " items encrypted");
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
