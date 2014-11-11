package psi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class DataMaker {

	public static int ITEM_LEN = 10;

	public static void main(String[] args) throws IOException {

		int items = Integer.parseInt(args[1]);
		int same = Integer.parseInt(args[2]);
		File file = new File(args[0]);
		file.createNewFile();
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		Random rand = new Random();

		for (int i = 0; i < same; i++) {
			bw.write(Integer.toString(i));
			for (int j = Integer.toString(i).length(); j < ITEM_LEN; j++) {
				bw.write("0");
			}
			if (i+1 < items) {
				bw.newLine();
			}
			bw.flush();
		}
		for (int i = same; i < items; i++) {
			for (int j = 0; j < ITEM_LEN; j++) {
				bw.write(Integer.toString(rand.nextInt(10)));
			}
			if (i+1 < items) {
				bw.newLine();
			}
			bw.flush();
		}
		bw.close();
		System.out.println(items + " items written.");
	}

}
