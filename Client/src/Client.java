import java.io.*;
import java.net.*;
import java.util.*;

class Access {

	Node state;
	int E_Count = 0,N_q;


	public Access(Node _state) {
		this.state = _state;
	}

	public void InitiateRequestGeneration() {

		while (this.E_Count < 20) {

			try {
				Thread.sleep(new Random().nextInt(300)+ 300);
			} catch (InterruptedException e) {
               System.out.println("Initiate failed:\n");
			}

			Res_req();
			CriticalSection();
			Release_Resource();


		}
		state.sendCompletion();
	}

	private int rand_Gen(int number) {
		return 1 + new Random().nextInt(number);
	}

	public void Res_req() {
		state.g_count = 0;
		state.dat = new Date();
		state.T_msg = state.s_MC + state.r_MC;
		this.N_q = rand_Gen(state.quorums.size());
		System.out.println("Sending Request to Quorum: "+N_q);
		state.sendMessageToQuorum(N_q,MessageType.Request, this.E_Count);
		state.waitforGrantFromQuorum(N_q);

	}

	synchronized private void CriticalSection() {
		this.E_Count++;
		state.latency[this.E_Count-1] = new Date().getTime() - state.dat.getTime();
		state.messageCountCS[this.E_Count-1] = state.s_MC+ state.r_MC - state.T_msg; 
		System.out.println("Critical Section at :"+state.getMyTimeStamp());
		Write();
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Process out of CS");

	}

	public void Release_Resource() {
		System.out.println("Resource Released");
		state.sendMessageToQuorum(this.N_q, MessageType.Release, -1);
	}

	public void Write() {

		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter("log.txt",true);
			fileWriter.write("ID: "+ (state.ID)+ " timeStamp: "+ state.getMyTimeStamp()+"\n");
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
// Server Creation for client
 class TCPServer {
	String HostName;
	int PortNumber, ID;
	ServerSocket serversocket;
	Node state;
	public TCPServer(int ID, int serverPort, String hostName) {
		this.HostName = hostName;
		this.PortNumber = serverPort;
		this.ID = ID;
	}

	public TCPServer(Node _state) {
	
		this(_state.ID, _state.port, _state.getNodeHostName());
		this.state = _state;
	}

	public void listenSocket() {
		try {
			serversocket = new ServerSocket(PortNumber);
		} catch (IOException e) {

			System.out.println(e);
			System.exit(-1);
		}
		while (true) {
			TCPClient reqHandler;
			try {
				// Creates Client thread
				Socket clientreqSocket = serversocket.accept();
				reqHandler = new TCPClient(clientreqSocket, this.state);

				
				Thread t = new Thread(reqHandler);
				t.start();

			} catch (IOException e) {
				System.out.println("Accept failed");
				System.exit(-1);
			}
		}
	}

}


 class TCPClient extends Thread{

	String serverHostName, clientHostName;
	private int serverPortNumber, ID, serverUID;
	private Socket clientsocket;
	ObjectInputStream in;
	ObjectOutputStream out;
    Node state;
    boolean flag;
    
	public TCPClient(int ID, int serverPort, String serverHostName, String clientHostName, int serverUID , Node _state) {
		this.serverHostName = serverHostName;
		this.serverPortNumber = serverPort;
		this.ID = ID;
		this.clientHostName = clientHostName;
		this.serverUID = serverUID;
		this.state = _state;
	}
	
	TCPClient(Socket client, Node state) {
		this.clientsocket = client;
		this.state = state;
	}

	public Socket getClientSocket() {
		return this.clientsocket;
	}

	public int getServerUID() {
		return this.serverUID;
	}
	
	public ObjectInputStream getInputReader() {
		return this.in;
	}

	public ObjectOutputStream getOutputWriter() {
		return this.out;
	}
	
	public void run() {
		// Client handling
		try {
			in = new ObjectInputStream(clientsocket.getInputStream());
			out = new ObjectOutputStream(clientsocket.getOutputStream());
			out.flush();
		} catch (IOException e) {
			System.out.println("in or out failed");
			System.exit(-1);
		}

		while (true) {
			try {
				// Reading data from client
				Object msg = in.readObject();
				if (msg instanceof String) {
					String message = msg.toString();
					String[] msgArr = message.split("!");
					this.serverUID = Integer.parseInt(msgArr[1]);
					
					state.addClient(this.serverUID, this);
					
					System.out.println("Message from client: " + this.serverUID);
				}

				else if (msg instanceof Message) {
					Message broadcastMessage = (Message) msg;
					System.out.println("Msg: " + broadcastMessage.getsenderUID()+" "+broadcastMessage.getMsgType()+" tmp:"+broadcastMessage.getTimeStamp()+" at:"+state.getMyTimeStamp());
					this.state.messageHandler(broadcastMessage);
				}

			} catch (IOException | ClassNotFoundException e) {
				System.out.println("Read failed");
				System.exit(-1);
			}
		}
	}
	
	// client listner
	public void listenSocket() {

		try {
			clientsocket = new Socket(serverHostName, serverPortNumber, InetAddress.getByName(clientHostName), 0);
			out = new ObjectOutputStream(clientsocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(clientsocket.getInputStream());
			System.out.println("after InputStream:listen.....");
		} catch (UnknownHostException e) {
			System.out.println("Unknown host:" + serverHostName);
			System.exit(1);
		} catch (IOException e) {
			System.exit(1);
		}
	}

	public void H_MSg() {

		try {
			System.out.println("message to server " + this.serverUID);
			String msg = "Hi!" + this.ID;
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			System.exit(1);
		}
	}

	// listen for messages
	public void listenToMessages() {
		try {
			while (true) {
				
				Message message = (Message) in.readObject();
				this.state.messageHandler(message);
				
				System.out.println("Msg ID: " + message.getsenderUID()+" "+message.getMsgType()+" tmp: "+message.getTimeStamp().getTime()+" at: "+new Date().getTime());
			}
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("failed transmission");
			System.exit(1);
		}

	}
}



public class Client {
	public static void main(String[] args) {
		try {

			int hostNumIndex = Integer.parseInt(args[0]);;

			Node state = BuildNode(hostNumIndex);

			System.out.println("Server Address: " + state.ID);

			// Start server thread
			Runnable serverRunnable = new Runnable() {
				public void run() {
					TCPServer server = new TCPServer(state);
					// start listening for client requests
					server.listenSocket();
				}
			};
			// Sending Client Requests
			Thread serverthread = new Thread(serverRunnable);
			serverthread.start();

			Thread.sleep(3000);


			state.uIDofNeighbors.entrySet().forEach((neighbour) -> {
				Runnable clientRunnable = new Runnable() {
					public void run() {
						TCPClient client = new TCPClient(state.ID,
								neighbour.getValue().PortNumber, neighbour.getValue().HostName, state.getNodeHostName(), neighbour.getKey(),
								state);
						System.out.println("Connection sent from "+state.ID+" to ID: "+neighbour.getKey()+" at Port: "+neighbour.getValue().PortNumber);

						client.listenSocket();
						client.H_MSg();
						state.addClient(neighbour.getKey(),client);
						client.listenToMessages();
					}
				};
				Thread clientthread = new Thread(clientRunnable);
				clientthread.start();
			});

			Thread.sleep(3000);
			new Access(state).InitiateRequestGeneration();
			
		}catch(Exception e){
			e.printStackTrace();
		}

	}

	public static Node BuildNode(int hostNumIndex) {
		Node state = new Node();
		try {
			state = Config.read(
					"C:\\Users\\Yuzuru\\Desktop\\Project_2\\Client\\src\\readFile.txt",
							InetAddress.getLocalHost().getHostName(), hostNumIndex);
		} catch (Exception e) {
			System.out.println("Quorum Nodes Not Functioning.");
		}
		return state;
	}
}
