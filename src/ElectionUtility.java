import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/*Saurabh Botre
 1001409374*/

public class ElectionUtility {
	//Declare token types used in entire algorithm
	public static final String TOKENTYPE1 = "TOKENTYPE1"; 
	public static final String TOKENTYPE2 = "TOKENTYPE2";
	public static final String TOKENTYPE3 = "TOKENTYPE3";
	public static final String TOKENTYPE4 = "TOKENTYPE4";
	
	/**
	 * Called whenever we need to start a process.
	 * @param token : Token to pass to proccessNo
	 * @param proccessNo : process number 
	 * @param host : address
	 */
	public static void startElection(String token, int proccessNo,String host) {
		if(proccessNo>5) {
			proccessNo = proccessNo - 5;
		}
		try {
			Socket socket = new Socket(host, 8080+proccessNo);
			PrintWriter out = new PrintWriter(socket.getOutputStream());
			out.println(token);
			out.flush(); 
			out.close(); 
			socket.close(); 
		} catch(Exception ex) {
			startElection(token, proccessNo+1,host);
		}
	}

	/**
	 * Method used to stop thread and socket of crashed process
	 * @param thread
	 * @param serverSocket
	 * @throws IOException
	 */
	public static void crashProcess(InputReader thread,ServerSocket serverSocket) throws IOException{
		thread.suspend();
		serverSocket.close();
	}

	/**
	 * Method used to start process again 
	 * @param thread
	 * @param crashedPort
	 * @param serverSocket
	 * @param processNumberInt
	 * @throws IOException
	 */
	public static void resetProcess(InputReader thread,int crashedPort,ServerSocket serverSocket,int processNumberInt) throws IOException{
		serverSocket = new ServerSocket(crashedPort);
		thread.resetProcess(crashedPort, processNumberInt, serverSocket); 
		thread.resume();
	}
}
