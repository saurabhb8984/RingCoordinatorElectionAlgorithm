import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/*Saurabh Botre
 1001409374*/
public class InputReader extends Thread{
	public int processNumber;
	public int portNumber;
	public BufferedReader inputReader;
	public PrintWriter pw;
	public RCAUserInterface rc;
	public ServerSocket serverSocket;
	public Socket client;	
	public int endPort=0;
	public int coordinator = 0;
	
	//Constructor used to initialize thread
	public InputReader(int portNumber, int processNumber ,RCAUserInterface rc, ServerSocket servSoc){
		this.portNumber = portNumber; 
		this.processNumber = processNumber; 
		this.rc = rc; 
		this.serverSocket = servSoc;
	}

	public void run(){
		while(true) {
			try {
				client = serverSocket.accept(); 
				inputReader = new BufferedReader(new InputStreamReader(client.getInputStream()));		
				String token = inputReader.readLine(); 
				Thread.sleep(2000);
				String[] tokens = token.split(" ");
				//Call method based on token type
				if(tokens[0].equals(ElectionUtility.TOKENTYPE1)){  
					processElection(token, tokens);
				}
				else if(tokens[0].equals(ElectionUtility.TOKENTYPE2)){  
					processCoordinatorInfo(token, tokens);
				}
				else if(tokens[0].equals(ElectionUtility.TOKENTYPE3)){ 
					processInteraction(token, tokens);
				}
				else if(tokens[0].equals(ElectionUtility.TOKENTYPE4)){
					processAlive(tokens);
				}
			} catch (Exception e) {
				System.out.println("Exception : " + e);
			}  finally {
				try {
					inputReader.close();
				} catch (IOException ex) {
					System.out.println("Exception : " + ex);
				}
			}
		}
	}

	/**
	 * Check that process is alive
	 * @param tokens
	 */
	private void processAlive(String[] tokens) {
		int targetNo = Integer.parseInt(tokens[1]);
		if(targetNo == processNumber) {
			if(targetNo == coordinator) {
				checkNextActive(1);
			} else {
				rc.currentActive = targetNo - 1;
				String tokenNextAlive = ElectionUtility.TOKENTYPE3+" " + coordinator + " " + processNumber;
				checkAvailablity(rc.coordinator, processNumber+1, processNumber, tokenNextAlive);
			}
		}
	}

	/**
	 * Check coordinator is alive
	 * @param token
	 * @param tokens
	 */
	private void processInteraction(String token, String[] tokens) {
		String tokenCNo = tokens[1]; 
		String tokenPNo = tokens[2];  
		if(rc.isProcessSlept == true) {
			try {
				if(Integer.parseInt(tokenPNo) == processNumber) {
					if(tokens.length > 3) {//debug here
						if(tokens[3].equals("OK")){
							//debug
							if (processNumber+1 == coordinator){
								checkNextActive(processNumber+2);
							} else {
								checkNextActive(processNumber+1);
							}
						}
					} else {  
						rc.textArea.append("\n No Response from coordinator \n Election Started");
						rc.textArea.append("\n Sent: "+ processNumber);
						String currToken = ElectionUtility.TOKENTYPE1+" " + processNumber;
						ElectionUtility.startElection(currToken, processNumber+1,"localhost");
					}

				}
				else if(Integer.parseInt(tokenCNo) == processNumber) { 
					token = token + " OK";
					sendTokenToNextProcess(token, processNumber+1);
				}
				else { 
					sendTokenToNextProcess(token, processNumber+1);
				}

			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * Inform coordinator to all process 
	 * @param token
	 * @param tokens
	 */
	private void processCoordinatorInfo(String token, String[] tokens) {
		String token2 = tokens[1];  
		String token3 = tokens[2];   
		try {
			Thread.sleep(2000);
			if(Integer.parseInt(token2) == processNumber) { 
				rc.isProcessCoordinator = true;
				rc.textArea.append("\n This is New Coordinator after election.");
			}
			else { 
				if(rc.isProcessCoordinator == true) {
					rc.textArea.append("\n" + processNumber + " is nomore a coordinator");
					rc.isProcessCoordinator = false;
				}
			}
			if(Integer.parseInt(token3) == processNumber) {
				rc.textArea.append("\n Coordinator is " + token2+" after election.");
				rc.isProcessSlept = true;
				if (processNumber == coordinator){ 
					checkNextActive(processNumber+1); 
				} else { 
					String tokenAlive = ElectionUtility.TOKENTYPE3+" " + coordinator + " " + processNumber;
					checkAvailablity(rc.coordinator, processNumber+1, processNumber+1, tokenAlive);
				}
			}
			else { 
				if(!rc.isProcessCoordinator) {  
					rc.textArea.append("\n Coordinator is " + token2 +" after election." );
				}
				rc.coordinator = Integer.parseInt(token2);
				coordinator = Integer.parseInt(token2);
				sendTokenToNextProcess(token, processNumber+1); 
				rc.isProcessSlept = true;
				if(rc.currentActive == processNumber - 1)
				{
					if (processNumber == coordinator){
						checkNextActive(processNumber+1);
					} else {
						String tokenAlive = ElectionUtility.TOKENTYPE3+" " + coordinator + " " + processNumber;
						checkAvailablity(rc.coordinator, processNumber+1, processNumber, tokenAlive);
					}
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Process election across all available process
	 * @param token
	 * @param tokens
	 */
	private void processElection(String token, String[] tokens) {
		String printToken = token;
		rc.textArea.append("\n Received:  "+printToken.replace(ElectionUtility.TOKENTYPE1, ""));
		rc.isProcessSlept = false;
		try {
			if(Integer.parseInt(tokens[1]) == processNumber) {
				int[] processes = new int[5];
				processes[0] = processNumber;
				int counter = 1;
				int tokenCounter = 2;
				while(tokens.length > tokenCounter ) {
					processes[counter] = Integer.parseInt(tokens[tokenCounter]);
					counter++;
					tokenCounter++;
				}
				findMax(processes); 
				if(coordinator != 0) {
					informCoordinator(coordinator, processNumber, processNumber);
				}
			}
			else {
				if(processNumber > Integer.parseInt(tokens[tokens.length-1])){
					Thread.sleep(1500);
					String printToken1 = token;
					rc.textArea.append("\n Sent: "+ printToken1.replace(ElectionUtility.TOKENTYPE1, "") +" "+processNumber);
					sendTokenToNextProcess(token+" "+processNumber, processNumber+1);
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Send election token to next available process
	 * @param token
	 * @param procID
	 */
	private void sendTokenToNextProcess(String token, int procID) {
		if(procID>5) {
			procID = procID - 5;
		}
		try {
			Socket socket = new Socket("localhost", 8080+procID);
			PrintWriter out = new PrintWriter(socket.getOutputStream());
			out.println(token);
			out.flush();
			out.close();
			socket.close();
		} catch(Exception ex) {
			sendTokenToNextProcess(token, procID+1); 
		}
	}

	/**
	 * Find max process Id for election
	 * @param processes
	 */
	private void findMax(int[] processes) {
		int newCoordinator = processes[0];
		for(int i = 1; i<processes.length; i++) {
			if(processes[i]>newCoordinator) {					
				newCoordinator = processes[i];
			}
		}  
		rc.coordinator = newCoordinator;
		coordinator = newCoordinator;
	}

	/**
	 * Method used to inform coordinator to all process after election
	 * @param coord
	 * @param procNo
	 * @param electedNo
	 */
	private void informCoordinator(int coord, int procNo, int electedNo) {
		int tempProc;
		if(coord == procNo) {
			tempProc = 1;
		} else {
			tempProc = procNo + 1;
		}
		String token = ElectionUtility.TOKENTYPE2 +" " + coord + " " + electedNo;
		try {
			Socket socket = new Socket("localhost", 8080+tempProc);
			PrintWriter out = new PrintWriter(socket.getOutputStream());
			out.println(token);
			out.flush();
			out.close();
			socket.close();
		} catch(Exception ex) {
			informCoordinator(coord, tempProc, electedNo); 
		}
	}

	/**
	 * Method used to set variables of reset process
	 * @param portNumber
	 * @param processNumber
	 * @param servSoc
	 */
	public void resetProcess(int portNumber, int processNumber ,ServerSocket servSoc) {
		this.portNumber = portNumber; 
		this.processNumber = processNumber;  
		this.serverSocket = servSoc;	 
	}
	
	/**
	 * Method used to check availability of a process
	 * @param ooordinatorNumber
	 * @param processNumber
	 * @param processElectedNumber
	 * @param token
	 */
	private void checkAvailablity(int ooordinatorNumber, int processNumber, int processElectedNumber, String token) {
		if(rc.isProcessSlept == true) {
			long time = 1000 * processElectedNumber;
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(processNumber>5) {
				processNumber = processNumber - 5;
			}
			try {
				Socket socket = new Socket("localhost", 8080+processNumber);
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.println(token);
				out.flush();
				out.close();
				socket.close();
				rc.currentActive = processElectedNumber;
			} catch(Exception ex) {
				checkAvailablity(ooordinatorNumber, processNumber+1, processElectedNumber, token); // pass to next next if next was unavailable
			}
		}
	}

	/**
	 * Method used to check hat next process is alive
	 * @param nextProc : next process number
	 */
	public void checkNextActive(int nextProc) {
		if(nextProc>5) {
			nextProc = nextProc - 5;
		}
		String token = ElectionUtility.TOKENTYPE4+" " + nextProc;
		try {
			Socket socket = new Socket("localhost", 8080+nextProc);
			PrintWriter out = new PrintWriter(socket.getOutputStream());
			out.println(token);
			out.flush();
			out.close();
			socket.close();
		} catch(Exception ex) {
			if(nextProc+1 == coordinator) {
				checkNextActive(nextProc+2);
			} else {
				checkNextActive(nextProc+1);
			}
		}
	}
	
	
}
