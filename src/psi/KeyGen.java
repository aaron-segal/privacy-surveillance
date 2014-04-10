package psi;

/*
 * Will save output to outfile_priv and outfile_pub.
 */

import java.io.*;
import java.math.BigInteger;

public class KeyGen {

	public static String ID = "ID";
	public static String PRIVATE_KEY = "PRIVATE_KEY";
	public static String PUBLIC_KEY = "PUBLIC_KEY";
	public static String PRIME = "PRIME";
	public static String GENERATOR = "GENERATOR";
	
	public static void usage() {
		System.out.println("Usage: KeyGen outfile [-i id] [-p prime -g generator]");
		System.out.println(ElGamal.prime768);
	}
	
	public static void main(String[] args) {
		if (args.length < 1) {
			usage();
		}
		String filename = args[0];
		
		Integer id = null;
		BigInteger prime = null;
		BigInteger generator = null;
		
		for (int i = 1; i < args.length; i +=2 ) {
			if (args[i].equals("-i")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					id = Integer.parseInt(args[i+1]);
				}
			} else if (args[i].equals("-p")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					prime = new BigInteger(args[i+1]);
				}
			} else if (args[i].equals("-g")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					generator = new BigInteger(args[i+1]);
				}
			} else {
				usage();
			}
		}
		
		CommutativeElGamal elg = null;
		if (id == null && prime == null && generator == null) {
			elg = new CommutativeElGamal();
		} else if (id != null && prime == null && generator == null) {
			elg = new CommutativeElGamal(id);
		} else if (id == null && prime != null && generator != null) {
			elg = new CommutativeElGamal(prime, generator);
		} else if (id != null && prime != null && generator != null) {
			elg = new CommutativeElGamal(id, prime, generator);
		} else {
			System.err.println("Error: Must specify either prime AND generator, or neither.");
			return;
		}
		
		try {
			File priv = new File(filename + "_priv");
			File pub = new File(filename + "_pub");
			
			// if file doesn't exist, then create it
			priv.createNewFile();
			pub.createNewFile();
 
			FileWriter fwpriv = new FileWriter(priv.getAbsoluteFile());
			FileWriter fwpub = new FileWriter(pub.getAbsoluteFile());
			BufferedWriter bwpriv = new BufferedWriter(fwpriv);
			BufferedWriter bwpub = new BufferedWriter(fwpub);
			
			bwpriv.write(ID + "=" + Integer.toString(elg.getID()));
			bwpriv.newLine();
			bwpriv.write(PRIVATE_KEY + "=" + elg.getPrivateKey().toString());
			if (prime != null) {
				bwpriv.newLine();
				bwpriv.write(PRIME + "=" + elg.getPrime().toString());
				bwpriv.newLine();
				bwpriv.write(GENERATOR + "=" + elg.getGenerator().toString());
			}
			bwpriv.flush();
			bwpriv.close();
 
			bwpub.write(ID + "=" + Integer.toString(elg.getID()));
			bwpub.newLine();
			bwpub.write(PUBLIC_KEY + "=" + elg.getPublicKey().toString());
			if (prime != null) {
				bwpub.newLine();
				bwpub.write(PRIME + "=" + elg.getPrime().toString());
				bwpub.newLine();
				bwpub.write(GENERATOR + "=" + elg.getGenerator().toString());
			}
			bwpub.flush();
			bwpub.close();
			
			System.out.println("Done");
 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
