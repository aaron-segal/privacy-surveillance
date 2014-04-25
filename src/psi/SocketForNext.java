package psi;
import java.net.*; 
import java.util.ArrayList;
import java.io.*; 

public class SocketForNext extends Thread{

	private Socket nextSocket = null;
	private Intersect user;
	//private PrintWriter out = null;
	//private BufferedReader in = null;

	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;
	private ArrayList<Msg> msgsToSend = null;

	public static final long SLEEP = 100;

	public SocketForNext(Intersect user){
		this.user = user;
		this.msgsToSend = new ArrayList<Msg>();
	}

	public void run(){

		Intersect.println ("Waiting for connection.....");
		try { 
			nextSocket = user.listenSocket.accept(); 
			//out = new PrintWriter(nextSocket.getOutputStream(), true);
			//in = new BufferedReader( new InputStreamReader( nextSocket.getInputStream()));

			outputStream = new ObjectOutputStream(nextSocket.getOutputStream());
			inputStream = new ObjectInputStream(nextSocket.getInputStream());

		} 
		catch (IOException e) { 
			System.err.println("Accept failed."); 
			System.exit(1); 
		} 

		Intersect.println ("[Next Node] Connected!");

		user.nextConnected = true;


		while (user.nextConnected) {
			if (!msgsToSend.isEmpty()) {
				sendObjToNextNode(msgsToSend.remove(0));
			} else {
				try {
					Thread.sleep(SLEEP);
				} catch (InterruptedException e) {
					continue;
				}
			}
		}

	}

	public void sendMsg(Msg msg) {
		msgsToSend.add(msg);
	}

	private void sendObjToNextNode(Msg obj){
		try {
			outputStream.writeObject(obj);
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (obj.type == Msg.Type.STAGE_TWO || obj.type == Msg.Type.ERROR) {
			synchronized (user) {
				user.finalData++;
				user.notify();
			}
		}
	}

	/*
 	public void sendToNextNode(String msg){
		if (msg != null){
			this.out.println(msg);
			Intersect.println("[To Next Node]" + msg);
		}
	}

	public void readFromNextNode(){
		String inputLine = null; 
		try {
			inputLine = this.in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Intersect.println("[From Next Node]" + inputLine);
	}
	 */
	public void closeSocket(){

		try {
			//out.close(); 
			//in.close();
			user.nextConnected = false;
			outputStream.close();
			inputStream.close();
			nextSocket.close();
			user.listenSocket.close();
		} catch (IOException e) {

		} 
	}

	public String getMyIP(){
		try {
			String addr = InetAddress.getLocalHost().getHostAddress();

			return addr;
		} catch (UnknownHostException e) {
			return null;
		}
	}
}
