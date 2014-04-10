package psi;
import java.math.BigInteger;
import java.net.*; 
import java.util.*;
import java.io.*; 
import java.lang.management.*;


public class Intersect extends Thread {

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

	private long startCpuTime = 0L;
	private long stageOneCpuTime = 0L;
	private long stageTwoCpuTime = 0L;
	private long realCpuTime = 0L;
	private long totalCpuTime = 0L;

	public static final int SLEEP = 10000;
	public static final String PORT = "PORT";
	public static final String INPUT_FILE = "INPUT";
	public static final String ID = "ID";
	public static final String PREVIOUS_IP = "PREVIP";
	public static final String PRIVATE_KEY = "PRIVATEKEY";
	public static final String NUM_USERS = "NUMUSERS";

	private static void usage() {
		System.err.println("Usage: java Intersect config [-i INPUT_FILE] [-k PUBLIC_KEY_FILE]");
	}

	public Intersect(String[] args) {

		if (args.length < 1) {
			usage();
			return;
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

		for (int i = 1; i < args.length; i += 2) {
			if (args[i].equals("-i")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(INPUT_FILE, args[i+1]);
				}
			} else if (args[i].equals("-k")) {
				if (args.length == i+1) {
					usage();
					return;
				} else {
					config.setProperty(PRIVATE_KEY, args[i+1]);
				}
			} else {
				usage();
				return;
			}
		}

		users = Integer.parseInt(config.getProperty(NUM_USERS));
		port = Integer.parseInt(config.getProperty(PORT));
		id = config.getProperty(ID);
		System.out.println("ID = " + id);

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

		// open up a new socket to communicate with next node

		try {
			listenSocket = new ServerSocket(port);
			System.out.println("IP:Host = " + "127.0.0.1" + ":" + port);
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

		myData = new Data(config.getProperty(INPUT_FILE), elg.getPrime());

		//System.out.println("Once all users have started, press ENTER to connect to other users:");
		//Scanner scan = new Scanner(System.in);
		//scan.nextLine();

		prevSocket = new SocketForPrev(this, ipPrev, portPrev);


		while (!((prevConnected == true) && (nextConnected == true))){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {

			}
		}
		System.out.println("Both connected !");
		// from here connected
		// read in file line by line
		// E(m) encrypt line by line murmur encrypt
		// P(E(m)) shuffle

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


		//System.out.println("User ready. When all users are ready, press ENTER to begin protocol stage one:");
		//scan.nextLine();
		System.out.println("Stage one beginning...");

		// encrypt my data first
		//time1 = System.currentTimeMillis();
		//cpuTime -= currentCpuTime();
		myData.encryptMyFile(elg);
		myData.shuffleMyEncFile();
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
		stageOneCpuTime = currentCpuTime() - startCpuTime;
		int stageOneBytesRead = prevSocket.getBytesRead();
		System.out.println("Stage three beginning...");
		//time2 = System.currentTimeMillis();
		//cpuTime -= currentCpuTime();
		myData.computeIntersection();
		myData.shuffleMyEncIntersection();		
		myData.decryptMyIntersection();

		Msg msg2 = Msg.createMyStageTwoMsg(this);
		//cpuTime += currentCpuTime();
		nextSocket.sendMsg(msg2);

		// wait for stage three to be done
		while (finalData < users + 1) {
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					System.out.println("S2");
				}
			}
		}
		
		
		System.out.println("Done!");
		nextSocket.closeSocket();
		
		stageTwoCpuTime = currentCpuTime() - (startCpuTime + stageOneCpuTime);
		/* Deactivated for testing
		Collections.sort(myData.finalIntersection);
		for (BigInteger bi : myData.finalIntersection) {
			String outputLine = BigIntegerEncoding.decode(bi);
			System.out.println(outputLine);
		}
		*/

		//cpuTime += prevSocket.getCpuTime();

		//tSystemTime = currentSystemTime();
		//tUserTime = currentUserTime();
		totalCpuTime = currentCpuTime();
		//systemTime += tSystemTime;
		//userTime += tUserTime;
		realCpuTime = totalCpuTime - startCpuTime;

		int totalBytesRead = prevSocket.closeSocket();
		
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
			bwstats.write(Integer.toString(stageOneBytesRead/1024));
			bwstats.write(",");
			bwstats.write(Integer.toString(totalBytesRead/1024));
			bwstats.write(",");
			bwstats.write(Long.toString(stageOneCpuTime/1000000L));
			bwstats.write(",");
			bwstats.write(Long.toString(stageTwoCpuTime/1000000L));
			bwstats.write(",");
			bwstats.write(Long.toString(totalCpuTime/1000000L));
			bwstats.newLine();
			bwstats.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Items: " + myData.myFile.size());
		//System.out.println("System Time: " + systemTime/1000000L + " ms");
		//System.out.println("User Time: " + userTime/1000000L + " ms");
		System.out.println("Stage One CPU Time: " + stageOneCpuTime/1000000L + " ms");
		System.out.println("Real CPU Time: " + realCpuTime/1000000L + " ms");
		//System.out.println("Total System Time: " + currentSystemTime()/1000000L + " ms");
		//System.out.println("Total User Time: " + currentUserTime()/1000000L + " ms");
		System.out.println("Total CPU Time: " + totalCpuTime/1000000L + " ms");

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

	public static void main(String[] args) {
		Intersect primary = new Intersect(args);
		primary.run();
	}



}
