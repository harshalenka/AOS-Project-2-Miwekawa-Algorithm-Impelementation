import java.io.*;
import java.util.*;

class NeighbourNode {
	String HostName;
	int p_num;

	NeighbourNode(String _hostName, int _portNumber) {
		this.HostName = _hostName;
		this.p_num = _portNumber;
	}
}

class Config{
	final static HashMap<Integer,Node> nodeList = new HashMap<>();

	public static Node read(String Path, String hostName, int hostNumIndex) throws IOException {
		BufferedReader b = new BufferedReader(new FileReader(Path));
		HashMap<Integer, NeighbourNode> map = new HashMap<>();
		HashMap<Integer, NeighbourNode> UIDofNeighbors = new HashMap<Integer, NeighbourNode>();
		String r = "",reg1="(?m)^#.*",reg2="(?m)^[ \\t]*\\r?\\n";

		
		String f = "";
		while ((r = b.readLine()) != null) {
			f += r+"\n";
		}
		b.close();
		f = f.replaceAll(reg1, "");
		f = f.replaceAll(reg2,"");

		String[] line = f.split("\n");
		int no = 0;
		
		int nodeNumber = Integer.parseInt(line[no++]);
		Node node = new Node();
		int m_I = -1;

		for (int xyz = 0; xyz < nodeNumber; xyz++) {
			r = line[no++].trim();
			String[] s = r.split("\\s+");
			int UID = Integer.parseInt(s[0]);
			String Hostname = s[1];
			int Port = Integer.parseInt(s[2]);
			map.put(UID, new NeighbourNode(Hostname, Port));
			if (hostNumIndex == UID)
				m_I = UID;
			nodeList.put(UID, new Node(UID, Port, Hostname, null));
		}

		node = nodeList.get(hostNumIndex);

		int noofClientConnections = Integer.parseInt(line[no++]);

		for( int xyz = 0; xyz < noofClientConnections; xyz++) {
			String[] s = line[no++].trim().split("\\s+");
			if (m_I == Integer.parseInt(s[0])) {
				for (int i = 1; i < s.length; i++) {
					UIDofNeighbors.put(Integer.parseInt(s[i]), map.get(Integer.parseInt(s[i])));

				}
			}
		}
		
		node.uIDofNeighbors = UIDofNeighbors;
		
		return node;
	}
}