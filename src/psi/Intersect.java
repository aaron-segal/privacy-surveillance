package psi;
import java.math.BigInteger;
import java.net.*; 
import java.util.*;
import java.io.*; 
import java.lang.management.*;


public class Intersect extends Thread {

	//if the -q flag is passed, nothing will be output except at the very end
	public static boolean quiet = false;
	//if the -s flag is passed, no timings or statistics will be saved
	private boolean suppress_timing = false; 

	protected boolean prevConnected = false;
	protected boolean nextConnected = false;
	protected int port;
	protected boolean keysGot = false;
	protected Data myData = null;
	protected SocketForNext nextSocket = null;
	protected SocketForPrev prevSocket = null;
	protected ServerSocket listenSocket = null;
	protected String id;
	protected CommutativeElGamal elg;
	//protected long time1, time2;
	protected int users, finalData = 0;
	protected int maxIntSize = -1;
	protected boolean wantToDecrypt = true;
	protected boolean stageTwoReady = false;
	protected int numThreads = 1; // 0 = use as many as possible

	private long startCpuTime = 0L;
	private long realCpuTime = 0L;
	private long totalCpuTime = 0L;
	private long wallTimeAll = 0L;
	private long wallTimeInput = 0L;
	private long wallTimeCalc = 0L;

	public static final int SLEEP = 10000;
	public static final String PORT = "PORT";
	public static final String INPUT_FILE = "INPUT";
	public static final String ID = "ID";
	public static final String PREVIOUS_IP = "PREVIP";
	public static final String PRIVATE_KEY = "PRIVATEKEY";
	public static final String NUM_USERS = "NUMUSERS";
	public static final String MAX_INTERSECTION = "MAXINTERSECTION";
	public static final String THREADS = "THREADS";

	// command line arguments take precedence over config file arguments

	private static void usage() {
		System.err.println("Usage: java Intersect config_file [-i input_file] [-k key_file] [-q] [-s] [-t #threads]");
	}

	public Intersect(String[] args) {

		wallTimeAll = System.currentTimeMillis();
		if (args.length < 1) {
			usage();
			System.exit(1);
		}

		Properties config = new Properties();
		try {
			FileReader configFile = new FileReader(args[0]);
			config.load(configFile);
			configFile.close();
		} catch (IOException e) {
			System.err.println("Could not load config file " + args[0]);
			return;
		}

		for (int i = 1; i < args.length; i ++) {
			if (args[i].equals("-i")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(INPUT_FILE, args[i+1]);
					i++;
				}
			} else if (args[i].equals("-k")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(PRIVATE_KEY, args[i+1]);
					i++;
				}
			} else if (args[i].equals("-q")) {
				quiet = true;
			} else if (args[i].equals("-s")) {
				suppress_timing = true;
			} else if (args[i].equals("-t")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(THREADS, args[i+1]);
					i++;
				}
			} else {
				usage();
				return;
			}
		}

		port = Integer.parseInt(config.getProperty(PORT));
		// open up a new socket to communicate with next node

		try {
			listenSocket = new ServerSocket(port);
			Intersect.println("IP:Host = " + "127.0.0.1" + ":" + port);
		} catch (IOException e) {
			System.err.println("Could not listen on port:" + port);
			return;
		} 

		nextSocket = new SocketForNext(this);
		nextSocket.start();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {

		}

		String[] address = config.getProperty(PREVIOUS_IP).split(":");
		String ipPrev = address[0]; // ip
		int portPrev = Integer.parseInt(address[1]); //port

		prevSocket = new SocketForPrev(this, ipPrev, portPrev);


		while (!((prevConnected == true) && (nextConnected == true))){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {

			}
		}
		Intersect.println("Both connected !");
		// from here connected
		// read in file line by line
		// E(m) encrypt line by line murmur encrypt
		// P(E(m)) shuffle

		wallTimeInput = System.currentTimeMillis();

		users = Integer.parseInt(config.getProperty(NUM_USERS));
		maxIntSize = Integer.parseInt(config.getProperty(MAX_INTERSECTION, "-1"));
		numThreads = Integer.parseInt(config.getProperty(THREADS, "1"));
		id = config.getProperty(ID);
		Intersect.println("ID = " + id);

		try {
			FileReader pkreader= new FileReader(config.getProperty(PRIVATE_KEY));
			Properties pkDefault = new Properties();
			pkDefault.setProperty(KeyGen.PRIME, ElGamal.prime1024.toString());
			pkDefault.setProperty(KeyGen.GENERATOR, ElGamal.generator1024.toString());
			Properties pk = new Properties(pkDefault);
			pk.load(pkreader);
			pkreader.close();
			int elgid = Integer.parseInt(pk.getProperty(KeyGen.ID));
			BigInteger privateKey = new BigInteger(pk.getProperty(KeyGen.PRIVATE_KEY));
			String primeString = pk.getProperty(KeyGen.PRIME);
			String genString = pk.getProperty(KeyGen.GENERATOR);
			if (primeString != null && genString != null) {
				elg = new CommutativeElGamal(elgid, new BigInteger(primeString), new BigInteger(genString), privateKey);
			} else {
				elg = new CommutativeElGamal(elgid, privateKey);
			}
		} catch (IOException e) {
			System.err.println("Could not load public key file " + config.getProperty(PRIVATE_KEY));
			return;
		}

		myData = new Data(config.getProperty(INPUT_FILE), elg.getPrime(), numThreads);

		File fileTest = new File(config.getProperty(INPUT_FILE));
		if (!fileTest.canRead()) {
			System.err.println("Error: Cannot read input file " + config.getProperty(INPUT_FILE));
			//scan.close();
			System.exit(1);
		}

		if (!myData.readInFile()) {
			System.exit(1);
		}

	}

	public void run() {

		//Beginning of actual thread
		//systemTime -= currentSystemTime();
		//userTime -= currentUserTime();
		startCpuTime = currentCpuTime();
		wallTimeCalc = System.currentTimeMillis();

		Intersect.println("Stage one beginning...");

		// encrypt my data first
		//time1 = System.currentTimeMillis();
		//cpuTime -= currentCpuTime();
		myData.encryptMyFile(elg);
		myData.shuffleMyFile();
		//start create message send to next node
		Msg mymsg = Msg.createMyStageOneMsg(this);
		//cpuTime += currentCpuTime();
		nextSocket.sendMsg(mymsg);

		while (myData.intersectionSet.size() < users) {
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {

				}
			}
		}

		//sSystemTime = currentSystemTime() - systemTime;
		//sUserTime = currentUserTime() - userTime;
		//stageOneCpuTime = currentCpuTime() - startCpuTime;
		Intersect.println("Stage three beginning...");
		//time2 = System.currentTimeMillis();
		//cpuTime -= currentCpuTime();
		int intSize = myData.computeIntersection();
		Msg msg2;
		if (maxIntSize > 0 && intSize > maxIntSize) {
			Intersect.println("Intersection too large - declining to decrypt.");
			msg2 = Msg.createMyErrorMsg(this);
			wantToDecrypt = false;
		} else {
			myData.shuffleMyEncIntersection();
			myData.decryptMyIntersection();
			msg2 = Msg.createMyStageTwoMsg(this);
		}

		//cpuTime += currentCpuTime();
		nextSocket.sendMsg(msg2);
		stageTwoReady = true;
		prevSocket.startQueue();

		// wait for stage three to be done
		while (finalData < users + 1) {
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					Intersect.println("S2");
				}
			}
		}


		Intersect.println("Done!");
		nextSocket.closeSocket();

		Collections.sort(myData.finalIntersection);
		for (BigInteger bi : myData.finalIntersection) {
			String outputLine = BigIntegerEncoding.decode(bi);
			Intersect.println(outputLine);
		}


		//cpuTime += prevSocket.getCpuTime();

		//tSystemTime = currentSystemTime();
		//tUserTime = currentUserTime();
		totalCpuTime = currentCpuTime();
		//systemTime += tSystemTime;
		//userTime += tUserTime;
		realCpuTime = totalCpuTime - startCpuTime;

		int totalBytesRead = prevSocket.closeSocket();

		long finishTime = System.currentTimeMillis();
		wallTimeAll = finishTime - wallTimeAll;
		wallTimeInput = finishTime - wallTimeInput;
		wallTimeCalc = finishTime - wallTimeCalc;

		if (!suppress_timing) {
			try {
				FileWriter fwstats = new FileWriter("stats" + id + ".txt", true);
				BufferedWriter bwstats = new BufferedWriter(fwstats);
				bwstats.write(Integer.toString(myData.myFile.size()));
				bwstats.write(",");
				if (elg.getPrime().equals(ElGamal.prime1024)) {
					bwstats.write("1024");
				} else if (elg.getPrime().equals(ElGamal.prime768)) {
					bwstats.write("768");
				} else {
					bwstats.write(Integer.toString(elg.getPrime().bitLength()));
				}
				bwstats.write(",");
				bwstats.write(Integer.toString(myData.finalIntersection.size()));
				bwstats.write(",");
				bwstats.write(Integer.toString(totalBytesRead/1024));
				bwstats.write(",");
				bwstats.write(Long.toString(realCpuTime/1000000L));
				bwstats.write(",");
				bwstats.write(Long.toString(totalCpuTime/1000000L));
				bwstats.write(",");
				bwstats.write(Long.toString(wallTimeCalc));
				bwstats.write(",");
				bwstats.write(Long.toString(wallTimeInput));
				bwstats.write(",");
				bwstats.write(Long.toString(wallTimeAll));
				bwstats.newLine();
				bwstats.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		if (quiet) {
			System.out.println("Items: " + myData.myFile.size() +
					"\tIntersection cardinality: " + myData.finalIntersection.size());
		}

		Intersect.println("Items: " + myData.myFile.size());
		//Intersect.println("System Time: " + systemTime/1000000L + " ms");
		//Intersect.println("User Time: " + userTime/1000000L + " ms");
		//Intersect.println("Stage One CPU Time: " + stageOneCpuTime/1000000L + " ms");
		Intersect.println("Real CPU Time: " + realCpuTime/1000000L + " ms");
		//Intersect.println("Total System Time: " + currentSystemTime()/1000000L + " ms");
		//Intersect.println("Total User Time: " + currentUserTime()/1000000L + " ms");
		Intersect.println("Total CPU Time: " + totalCpuTime/1000000L + " ms");
		Intersect.println("Total Wall Time: " + wallTimeAll + " ms");
		Intersect.println("Wall Time (without connection): " + wallTimeInput + " ms");
		Intersect.println("Wall Time (without file I/O): " + wallTimeCalc + " ms");

		System.exit(0);

	}

	/** Get CPU time in nanoseconds. */
	private long currentCpuTime( ) {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
		return bean.isCurrentThreadCpuTimeSupported( ) ?
				bean.getCurrentThreadCpuTime( ) : 0L;
	}

	/*s
	//
	private long currentUserTime( ) {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
		return bean.isCurrentThreadCpuTimeSupported( ) ?
				bean.getCurrentThreadUserTime( ) : 0L;
	}

	//
	private long currentSystemTime( ) {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
		return bean.isCurrentThreadCpuTimeSupported( ) ?
				(bean.getCurrentThreadCpuTime( ) - bean.getCurrentThreadUserTime( )) : 0L;
	}
	 */

	public static void println(String s) {
		if (!quiet) {
			System.out.println(s);
		}
	}

	public static void main(String[] args) {
		Intersect primary = new Intersect(args);
		primary.run();
	}



}
