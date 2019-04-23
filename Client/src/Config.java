import java.io.*;
import java.util.*;

class NeighbourNode {
	String HostName;
	int PortNumber;

	NeighbourNode(String _hostName, int _portNumber) {
		this.HostName = _hostName;
		this.PortNumber = _portNumber;
	}
}

class Config {
	final static HashMap<Integer,Node> nodeList = new HashMap<>();

	public static Node read(String Path, String hostName, int hostNumIndex) throws IOException {
		HashMap<Integer, NeighbourNode> map = new HashMap<>();
		HashMap<Integer, NeighbourNode> UIDofNeighbors = new HashMap<Integer, NeighbourNode>();
		BufferedReader b = new BufferedReader(new FileReader(Path));
		String readLine = "",path = "",reg1="(?m)^#.*",reg2="(?m)^[ \t]*\r?\n";
		
		// Filtering the File 
		String file_Contents = "";
		while ((readLine = b.readLine()) != null) {
			file_Contents += readLine+"\n";
		}
		b.close();
		file_Contents = file_Contents.replaceAll(reg1, "");
		file_Contents = file_Contents.replaceAll(reg2,"");

		String[] line = file_Contents.split("\n");
		int no = 0;

		b = new BufferedReader(new FileReader(Path));
		path = line[no++];
		int nodeNumber = Integer.parseInt(line[no++]);
		Node node = new Node();
		int myUID = -1;
		try {

			for (int p = 0; p < nodeNumber; p++) {
				readLine = line[no++].trim();
				String[] s = readLine.split("\\s+");
				int UID = Integer.parseInt(s[0]);
				String Hostname = s[1];
				int Port = Integer.parseInt(s[2]);
				map.put(UID, new NeighbourNode(Hostname, Port));
				if (hostNumIndex == UID)
					myUID = UID;
				nodeList.put(UID,new Node(UID, Port, Hostname, null));
			}

			node = nodeList.get(hostNumIndex);
			int quorumNumbers = Integer.parseInt(line[no++]);

			for( int p = 0 ; p < quorumNumbers; p++ ) {
				String[] str = line[no++].trim().split("\\s+");
				ArrayList<Integer> temp = new ArrayList<>();
				for( int i = 1; i < str.length; i++ )
					temp.add(Integer.parseInt(str[i]));
				node.quorums.put(Integer.parseInt(str[0]), temp);
			}

			int noc = Integer.parseInt(line[no++]);

			for( int p = 0; p < noc; p++) {
				String[] s = line[no++].trim().split("\\s+");
				if (myUID == Integer.parseInt(s[0])) {
					for (int i = 1; i < s.length; i++) {
						UIDofNeighbors.put(Integer.parseInt(s[i]), map.get(Integer.parseInt(s[i])));
					}
				}
			}
			
			node.uIDofNeighbors = UIDofNeighbors;
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			b.close();
		}
		return node;
	}
}