import java.net.*;
import java.io.*;
import java.util.*;
class f_Acess {

	Node b_n;
	int completionMessageCount;
	
	public f_Acess(Node node) {
		this.b_n = node;
	}

	public void listen() {
		while(true) {
			if(b_n.isLocked() == false && b_n.msgQueue.size() != 0 ) {
				Message msg = b_n.getHeadMessageFromQueue();
				if(msg.getMsgType() == MessageType.Request) {
					b_n.setLocked(true);
					b_n.sendGrant(msg.getsenderUID());
				}
				else if(b_n.getNodeUID() == 1 && msg.getMsgType() == MessageType.Completion) {
					if(5 == ++completionMessageCount) {
						b_n.sendCompletion();
						b_n.printReport();
						break;
					}
				}
				else if(msg.getMsgType() == MessageType.Completion) {
					b_n.printReport();
					break;
				}
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
class TCPClient extends Thread{

	String serverHostName, clientHostName;
	private int serverPortNumber, UID, serverUID;
	private Socket clientsocket;
	ObjectInputStream in;
	ObjectOutputStream out;
    Node b_n;
    
	public TCPClient(int UID, int serverPort, String serverHostName, String clientHostName, int serverUID , Node _b_n) {
		this.serverHostName = serverHostName;
		this.serverPortNumber = serverPort;
		this.UID = UID;
		this.clientHostName = clientHostName;
		this.serverUID = serverUID;
		this.b_n = _b_n;
	}
	
	TCPClient(Socket client, Node b_n) {
		this.clientsocket = client;
		this.b_n = b_n;
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
		try {
			in = new ObjectInputStream(clientsocket.getInputStream());
			out = new ObjectOutputStream(clientsocket.getOutputStream());
			out.flush();
		} catch (IOException e) {
			System.exit(-1);
		}

		while (true) {
			try {

				Object msg = in.readObject();

				if (msg instanceof String) {
					String message = msg.toString();
					String[] msgArr = message.split("!");

					this.serverUID = Integer.parseInt(msgArr[1]);
					
					// add all the connected clients
					b_n.addClient(this.serverUID,this);
					
					
				}
                //recieve messages
				else if (msg instanceof Message) {
					Message broadcastMessage = (Message) msg;
					System.out.println("Messages recieved: " + broadcastMessage.getsenderUID()+" "+broadcastMessage.getMsgType()+" tmp"+broadcastMessage.getTimeStamp().getTime());
					this.b_n.addMessageToQueue(broadcastMessage);
				}

			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				System.out.println("Read failed");
				System.exit(-1);
			}
		}
	}
	
	public void listenSocket() {
		try {
			clientsocket = new Socket(serverHostName, serverPortNumber, InetAddress.getByName(clientHostName), 0);
			out = new ObjectOutputStream(clientsocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(clientsocket.getInputStream());
			System.out.println("Input, listenSocket");
		} catch (UnknownHostException e) {
			System.out.println("Unknown host:" + serverHostName);
			System.exit(1);
		} catch (IOException e) {

			System.exit(1);
		}
	}

	public void sendHandShakeMessage() {

		try {
			
			String msg = "Hi!" + this.UID;
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			System.out.println("failed transmission" + e);
			System.exit(1);
		}
	}

	public void listenToMessages() {
		try {
			while (true) {
				// listening
				Message message = (Message) in.readObject();
				this.b_n.messageHandler(message);
				
				System.out.println("Msg: " + message.getsenderUID()+" "+message.getMsgType()+" tmp: "+message.getTimeStamp().getTime()+" at: "+new Date().getTime());
			}
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("failed transmission");
			System.exit(1);
		}

	}
}


class TCPServer {
	String HostName;
	int PortNumber, UID;
	ServerSocket serversocket;
	Node b_n;

	public TCPServer(int UID, int serverPort, String hostName) {
		this.HostName = hostName;
		this.PortNumber = serverPort;
		this.UID = UID;
	}

	public TCPServer(Node _b_n) {

		this(_b_n.UID, _b_n.port, "localhost");
		this.b_n = _b_n;
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
				Socket clientreqSocket = serversocket.accept();
				reqHandler = new TCPClient(clientreqSocket, this.b_n);

				
				Thread t = new Thread(reqHandler);
				t.start();

			} catch (IOException e) {
				System.out.println("Accept failed");
				System.exit(-1);
			}
		}
	}

}


public class Server {
	public static void main(String[] args) {
		try {

			int hostNumIndex = Integer.parseInt(args[0]);;

			Node b_n = BuildNode(hostNumIndex);
			//listens to client  
			Runnable serverRunnable = new Runnable() {
				public void run() {
					TCPServer server = new TCPServer(b_n);
					
					server.listenSocket();
				}
			};
			Thread serverthread = new Thread(serverRunnable);
			serverthread.start();

			System.out.println("listening to reqwuests:");

			Thread.sleep(3000);
			if(b_n.getNodeUID() == 1) {
				b_n.uIDofNeighbors.entrySet().forEach((neighbour) -> {
					Runnable clientRunnable = new Runnable() {
						public void run() {
							TCPClient client = new TCPClient(b_n.UID,
									neighbour.getValue().PortNumber, neighbour.getValue().HostName, b_n.getNodeHostName(), neighbour.getKey(),
									b_n);
							System.out.println("Client Connection sent from "+b_n.UID+" to: "+neighbour.getKey()+" at Port: "+neighbour.getValue().PortNumber);

							client.listenSocket();
							client.sendHandShakeMessage();
							b_n.addClient(neighbour.getKey(),client);
							client.listenToMessages();
						}
					};
					Thread clientthread = new Thread(clientRunnable);
					clientthread.start();
				});
			}
			Thread.sleep(3000);

			new f_Acess(b_n).listen();
			
		}catch(Exception e){
			e.printStackTrace();
		}

	}

	public static Node BuildNode(int hostNumIndex) {
		Node b_n = new Node();
		try {
			b_n = Config.read(
					"C:\\Users\\Yuzuru\\Desktop\\Project_2\\Server\\src\\readFile.txt",
							InetAddress.getLocalHost().getHostName(), hostNumIndex);
		} catch (Exception e) {
			throw new RuntimeException("Unable to get nodeList", e);
		}
		return b_n;
	}
}
