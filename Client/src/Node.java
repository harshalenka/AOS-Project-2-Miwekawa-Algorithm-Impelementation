import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;

public class Node {
	int ID, port,T_msg,g_count, s_MC, r_MC;
	Date dat;
	String filePath="",HostName;
	HashMap<Integer, NeighbourNode> uIDofNeighbors;
	ServerSocket serverSocket;
	Map<Integer,TCPClient> connectedClients = (Map<Integer, TCPClient>) Collections.synchronizedMap(new HashMap<Integer,TCPClient>());
	HashMap<Integer,ArrayList<Integer>> quorums = new HashMap<>();
	int[] messageCountCS = new int[20];
	long[] latency = new long[20];
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public Node(int ID, int port, String hostName, HashMap<Integer, NeighbourNode> uIDofNeighbors) {
		this.ID = ID;
		this.port = port;
		this.HostName = hostName;
		this.uIDofNeighbors = uIDofNeighbors;
	}

	public Node() {
	}

	public void sendMessageToQuorum(int N_q, MessageType msgType, int csEntryCount) {
		synchronized (connectedClients) {
			ArrayList<Integer> quorum = quorums.get(N_q);
			for(int x : quorum) {
				TCPClient client = connectedClients.get(x);
				try {
					if(msgType == MessageType.Request)
						System.out.println("Sending Request to: "+x);
					else 
						System.out.println("Sending Release to: "+x);
					client.getOutputWriter().writeObject(new Message(dat, this.ID, msgType ));
				} catch (IOException e) {
					e.printStackTrace();
				}
				increments_MC();
			}
		}
	}

	public void sendCompletion() {
		increments_MC();
		synchronized (connectedClients) {
			TCPClient client = connectedClients.get(1);
			try {
				System.out.println("Completed");
				client.getOutputWriter().writeObject(new Message(new Date(),this.ID,MessageType.Completion));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void attachServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public int getNodeUID() {
		return this.ID;
	}

	public int getNodePort() {
		return this.port;
	}

	public String getNodeHostName() {
		return this.HostName;
	}

	public HashMap<Integer, NeighbourNode> getNeighbors() {
		return this.uIDofNeighbors;
	}

	public void addClient(int ID, TCPClient client) {
		synchronized (connectedClients) {
			connectedClients.put(ID, client);
		}
	}


	public 	Map<Integer,TCPClient> getAllConnectedClients() {
		return this.connectedClients;
	}

	synchronized public void messageHandler(Message msg) {
		incrementr_MC();
		if(msg.getMsgType() == MessageType.Grant) {
			incrementGrantMessageCount();
		}
		if(msg.getMsgType() == MessageType.Completion) {
			printReport();
		}
	}

	synchronized void incrementGrantMessageCount() {
		this.g_count++;
	}

	synchronized void increments_MC() {
		this.s_MC++;
	}

	synchronized void incrementr_MC() {
		this.r_MC++;
	}

	synchronized void messageCountCS(int csEntryCount) {
		this.messageCountCS[csEntryCount]++;
	}

	public String getMyTimeStamp() {
		return sdf.format(new Date());
	}

	public void waitforGrantFromQuorum(int N_q) {
		int numberOfGrants = this.quorums.get(N_q).size();
		System.out.println("Waiting ");
		while(numberOfGrants != this.g_count) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void printReport() {
		System.out.println("Total Sent Messages: "+s_MC);
		System.out.println("Total Received Messages: "+r_MC);
		
		for(int i = 0; i < 20; i++) {
			System.out.println(" Amout of Messages in CS["+i+"]: " + messageCountCS[i]);
		}

	}

}
