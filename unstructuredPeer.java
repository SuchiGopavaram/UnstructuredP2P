package UnstructuredP2P;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.math3.distribution.ZipfDistribution;

import java.util.logging.FileHandler;
import java.util.logging.Level;

public class unstructuredPeer {
	
	public static int N_port;
	public static String N_ip;
	public static int BS_port;
	public static String BS_ip;
	public static Logger logger = Logger.getLogger("NodeLog");
	public static DatagramSocket sock;
	public static ConcurrentHashMap<String, String> RT = new ConcurrentHashMap<String, String>();
	public static String[] resources;
	public static String uname = "Nodes20";
	public static List<String> N_resources = new ArrayList<String>();
	public static peerListen lis;
	public static int hops = 20;
	
	public static void main(String[] args) {
		try {
			FileHandler log_file = new FileHandler("Node.Log");
			SimpleFormatter formatter = new SimpleFormatter();
			log_file.setFormatter(formatter);
			logger.addHandler(log_file);
			logger.setUseParentHandlers(false);
			
			InetAddress Node_ip = InetAddress.getLocalHost();
			N_ip = Node_ip.getHostAddress();
			BS_ip = args[1];
			logger.log(Level.INFO, "Using the BootStrap Server with IP: " + BS_ip + " Port: " + BS_port);
			logger.log(Level.INFO, "Initializing node with IP address: " + N_ip + " on Port: " + N_port);
			N_port = Integer.parseInt(args[0]);
			BS_port = Integer.parseInt(args[2]);

			if ((N_port <= 5000 || N_port >= 65535) || (BS_port <= 5000 || BS_port >= 65535 )) {
				System.out.println("Please type an integer in the range of 5001 - 65535 for port number(s).");
				logger.log(Level.WARNING, "User assigned a port number which is out of port ranges.");
				System.exit(1);
			}
			
			
			lis = new peerListen(N_port, N_ip, RT);
			new Thread(lis).start();
			logger.log(Level.INFO,"Listen thread strated");
			
			sock = new DatagramSocket();
			logger.log(Level.INFO, "Socket has been created.");
			
			logger.log(Level.INFO, "Trying to register with the BootStrap server with username given by user: "+uname);
			//System.out.println("Registering to the Network in Bootstrap Server");
			unstructuredPeer.Register(uname);
			
			System.out.println("Sending join messages to the IP's received from Bootstrapper");
			logger.log(Level.INFO, "Trying to join with the nodes provided by the BootStrap server.");
			unstructuredPeer.join();
			
			System.out.println("Routing Table: ");
			for (String name: RT.keySet()){ 
	            System.out.println(name);  
			}
			
			Scanner sc = new Scanner(System.in);
			while(true) {
				String s = sc.nextLine();
				String[] S = s.split(" ");
				String fileName = "";
				for (int i = 1; i < S.length; i++) {
					fileName = fileName + S[i];
				}
				switch(S[0]){
				case "leave":
					logger.log(Level.INFO, "Trying to leave from BootStrap Server and nodes in the Routing Table.");
					System.out.println("Sending leave messages to the BootStrap Server and nodes");
					unstructuredPeer.leave(uname);
					RT.clear();
					logger.log(Level.INFO, "Left from BootStrap Server and nodes in the Routing Table.");
					
					System.out.println("Routing Table:");
					for (String name: RT.keySet()){ 
			            System.out.println(name);  
					}
					break;
					
				case "distribute":
					fileDist(Integer.parseInt(S[1]));
					lis.sendResources(N_resources);
					break;
					
				case "Query":
					//External query code.
					int noFiles = 0;
					String Files = "";
					for(String file : N_resources) {
						if (file.contains(S[1])) {
							Files = Files + file + "\n";
							noFiles++;
						}
					} 
					if (noFiles>0) {
						System.out.println("The queried file is already in this node.");
						logger.log(Level.INFO,"The queried file is already in this node.");
					}
					else {
						String query = "SER" + N_ip + " " + N_port + " " + S[1] + " " + hops;
						String queryMsg = String.format("%04d", query.length()) + " " + query;
						for (String Add : RT.keySet()) {
							String[] sockAdd = Add.split(" ");
							send(queryMsg, sockAdd[0], Integer.parseInt(sockAdd[1]));
						}
					}
					break;
					
				case "add":
					if (!N_resources.contains(fileName)) {
						N_resources.add(fileName);
					}
					else {
						System.out.println("Resource already present in this node.");
					}
					break;
					
				case "delete":
					//delete resource code.
					if (N_resources.contains(fileName)) {
						N_resources.remove(fileName);
					}
					else {
						System.out.println("Resource is not present in this node.");
					}
					break;
					
				case "print" :
					System.out.println("Routing Table: ");
					for (String name: RT.keySet()){ 
			            System.out.println(name);  
					}
					break;
					
				case "exit":
					log_file.close();
					lis.log_file.close();
					sc.close();
					System.exit(0);
					
				default:
					System.out.println("Usage: \n add <Resource name>: Adds resource to the node.\n"
							+ "delete <Resource name>: deletes resource from the node.\n"
							+ "leave: Leaves the network.\n"
							+ "print: Prints routing table.\n"
							+ "distribute <number of resources per node>: Distributes the resources.txt contents to all the nodes in the network. \n"
							+ "Query: "
							+ "exit: Exits the program."
							//add the added features here
							);
					break;
				}
			}
		}
		
		catch (NumberFormatException e) {
			System.err.println("Please give an integer port number(s).");
			logger.log(Level.WARNING, "User gave a non-numeric port number(s).");
			System.exit(1);
		}
		catch (IOException e) {
			System.out.println("IOException Occured.");
			System.err.println("Check the IP address. Only numbers less than 255 should be given in each field of IP.");
			logger.log(Level.WARNING, "IOException Occured. "
					+ "Check the IP address. User assigned an invalid range of IP number.");
			System.exit(1);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.err.println(e);
			System.err.println("Check the number of arguments given. "
					+ "\n Command Usage: java Unstructuredpeer REG <Node_Port> <BootStrap_IP> <BootStrap_Port> <UserName>");
			logger.log(Level.WARNING, "Check the number of arguments given." 
					+ "\n Command Usage: java Unstructuredpeer REG <Node_Port> <BootStrap_IP> <BootStrap_Port> <UserName>");
		}
		sock.close();
	}
	
	public static String msgRT(String Message, String ip, int Port)throws IOException {
		logger.log(Level.INFO, "Sending the message to Socket address: " + ip + " " + Port);
		InetAddress IP = InetAddress.getByName(ip);
		System.out.println("Message in msgRT: " + Message);
		byte[] send = Message.getBytes();
		DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
		sock.send(sndpkt);
		byte[] rcv = new byte[1023];
		DatagramPacket rcvpkt = new DatagramPacket(rcv, rcv.length);
		sock.receive(rcvpkt);
		String reply = new String(rcvpkt.getData(),0,rcvpkt.getLength());
		System.out.println(reply);
		return reply;
	}
	
	public static void Register(String uname) throws IOException {
		String msg1 = "REG "+ N_ip + " " + Integer.toString(N_port) + " " + uname;
		int len = msg1.length();
		String msg = String.format("%04d", len) + " " + msg1;
		String reply =  msgRT(msg,BS_ip,BS_port);
		String[] rep = reply.split(" ");
		if (rep[1].equals("REGOK")) {
			if (rep[rep.length - 1].equals("9998")) {
				System.out.println("Node already registered.");
				logger.log(Level.WARNING, "User trying to register an already registered node.");
			}
			else if (rep[rep.length - 1].equals("9999")) {
				System.out.println("Error in registering.");
				logger.log(Level.WARNING, "Error in registering.");
				System.exit(1);
			}
			else if (rep[rep.length - 1].equals("-1")) {
				System.out.println("Unknown REG command.");
				logger.log(Level.WARNING, "Unknown REG command.");
				System.exit(1);
			}
			else {
				if (rep[3].equals("0")) {
					System.out.println("Node Registered Successfully.");
					logger.log(Level.INFO, "Node Registered Successfully.");
				}
				else if (rep[3].equals("1")){
					System.out.println("Node Registered Successfully.");
					logger.log(Level.INFO, "Node Registered Successfully.");
					RT.put(rep[4] + " " + rep[5], "");
				}
				else if (rep[3].equals("2")){
					System.out.println("Node Registered Successfully.");
					logger.log(Level.INFO, "Node Registered Successfully.");
					RT.put(rep[4] + " " + rep[5], "");
					RT.put(rep[6] + " " + rep[7], "");
				}
				else if (rep[3].equals("3")){
					System.out.println("Node Registered Successfully.");
					logger.log(Level.INFO, "Node Registered Successfully.");
					RT.put(rep[4] + " " + rep[5], "");
					RT.put(rep[6] + " " + rep[7], "");
					RT.put(rep[8] + " " + rep[9], "");
				}
				else {
					System.out.println("Received unknown message.");
					logger.log(Level.WARNING, "Received unknown message.");
					System.exit(1);
				}
			}
		}
		
		else if(rep[1].equals("BS")) {
			System.out.println("Unknown command, undefined characters to bootstrapper.");
			logger.log(Level.WARNING, "Unknown command, undefined characters to bootstrapper.");
			System.exit(1);
		}

	}
	
	public static void join()  {
		try {
			System.out.println("Join method. 1");
			String JoinMsg = "JOIN " + N_ip + " " + Integer.toString(N_port);
			int len = JoinMsg.length();
			JoinMsg = String.format("%04d", len) + " " + JoinMsg;
			System.out.println("Join method. 2");
			for (String num: RT.keySet()) {
				String[] sockAdd = num.split(" ");
				System.out.println(sockAdd[0]+":"+sockAdd[1]);
				System.out.println("Join method. 3");
				String reply = msgRT(JoinMsg, sockAdd[0], Integer.parseInt(sockAdd[1]));
				System.out.println("Join method. 4");
				String[] node_reply = reply.split(" ");
				if (node_reply[1] == "JOINOK"){
					if (node_reply[2] == "0") {
						System.out.println(num + " has been added to the routing table as the JOIN message succeeded.");
					}
					else {
						RT.remove(num);
						logger.log(Level.INFO, num + " has been removed from the routing table as the JOIN message failed.");
					}
					if (node_reply[2] == "9999") {
						System.out.println("Node " + num + " did not added my IP in it's Routing Table!");
						logger.log(Level.INFO, "Node " + num + " did not added my IP in it's Routing Table!");
					}
				}
			}
			System.out.println("Join method. 5");
			
		} catch (NumberFormatException e) {
			System.err.println(e);
			System.err.println("Routing table contains non-numeric characters in the port field.");
			logger.log(Level.WARNING, "Routing table contains non-numeric characters in the port field.");
		}
		catch (IOException e) {
			System.err.println("I/O error occured while joining to the network!");
		}
		
	}
	
	public static void leave(String uname) {
		while (true) {	
			try {
				String DelMsg = "DEL IPADDRESS " + N_ip + " " + Integer.toString(N_port) + " "+ uname;
				int delLen = DelMsg.length();
				DelMsg = String.format("%04d", delLen) + " " + DelMsg;
				
				for(int i = 0; i < 3; i++) {
					String reply = msgRT(DelMsg, BS_ip, BS_port);
					String[] bs_reply = reply.split(" ");
					if(bs_reply[1].equals("DEL")) {
						if (bs_reply[bs_reply.length - 1].equals("1")) {
							System.out.println("Left Bootstrap Server Successfully.");
							logger.log(Level.INFO, "Left Bootstrap Server Successfully.");
						}
						else if (bs_reply[bs_reply.length - 1].equals("-1")) {
							System.out.println("Error in DEL Command.");
							logger.log(Level.WARNING, "Error in DEL Command.");
						}
						else if (bs_reply[bs_reply.length - 1].equals("9998")) {
							System.out.println("(IP Address + Port ) not registered for username.");
							logger.log(Level.WARNING, "(IP Address + Port ) not registered for username.");
						}
						else if (bs_reply[bs_reply.length - 1].equals("9999")) {
							System.out.println("Username not registered with BootStrap Server.");
							logger.log(Level.WARNING, "Username not registered with BootStrap Server.");
						}
						break;
					}
					else {
						System.out.println("Bootstrapper did not remove my IP from it's List! Trying again. "
								+ Integer.toString(3 - (i + 1))+" times remaining");
						logger.log(Level.WARNING, "Bootstrapper did not remove my IP from it's List! Trying again. "
								+ Integer.toString(3 - (i + 1)) + " times remaining");
					}
				}
				
				String LeaveMsg = "LEAVE " + N_ip + " " + Integer.toString(N_port);
				int leaveLen = LeaveMsg.length();
				LeaveMsg = String.format("%04d", leaveLen) + " " + LeaveMsg;
				
				for (String num: RT.keySet()) {
					String[] sockAdd = num.split(" ");
					for(int i = 0; i < 3; i++) {
						String reply = msgRT(LeaveMsg, sockAdd[0], Integer.parseInt(sockAdd[1]));
						String[] node_reply = reply.split(" ");
						if (node_reply[2] != "0") {
							System.out.println("Left from " + num + " node Successfully");
							logger.log(Level.INFO, "Left from " + num + " node Successfully");
							break;
						}
						else if (node_reply[2] == "9999") {
							System.out.println("Node " + num + " did not remove my IP from it's Routing Table! Trying again. " 
									+ Integer.toString(3 - (i + 1))+" times remaining");
							logger.log(Level.WARNING, "Node" + num + " did not remove my IP from it's Routing Table! Trying again. "
									+ Integer.toString(3 - (i + 1))+" times remaining");
						}
					}	
				}
				break;
			}
			catch (IOException e) {
				System.err.println("I/O error occurred while joining to the network.");
				logger.log(Level.WARNING, "I/O error occurred while joining to the network.");
			}
			catch (NumberFormatException e) {
				System.err.println("Routing table contains non-numeric characters in the port field.");
				logger.log(Level.WARNING, "Routing table contains non-numeric characters in the port field.");
			}
		}
	}
	
	public static void fileDist(int numOfRes) {
		try {
			String peerAddress;
			List<String> peerList = new ArrayList<String>();
			
			String ipList = "GET IPLIST " + uname;
			int len = ipList.length();
			String msg = String.format("%04d", len) + " " + ipList;
			
			String reply =  msgRT(msg,BS_ip,BS_port);
			String[] a = reply.split(" ");
			
			if (a[3].equals("OK")) {
				for (int i = 6; i <= a.length - 2; i = i + 2) {
					peerAddress = a[i] + ":" + a[i+1];
					peerList.add(peerAddress);
				}
			}
			
			File file  = new File("resources.txt");
			FileReader fr = new FileReader(file);						
			BufferedReader br = new BufferedReader(fr);
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("#")) {
					continue;
				}
				sb.append(line);
				sb.append("\n");
			}
			fr.close();
			
			System.out.println("File Names in this Node: ");
			resources = sb.toString().split("\n");

			/*for (int r = 0; r < numOfRes; r++) {
				int i = random.nextInt(resources.length);
				if (N_resources.contains(resources[i])) {
					r--;
					continue;
				}
				N_resources.add(resources[i]);
			}
			System.out.println(N_resources);*/
			int i = 0 ;
			for (String sockAddress : peerList) {
				String[] pList = sockAddress.split(" ");
				
				while ( i < resources.length) {
					List<String> subArr = Arrays.asList(resources).subList(i, i + numOfRes);
					StringBuffer sbuffer = new StringBuffer();
					sbuffer.append("Resources\n");
					for (String s : subArr)
					{
						sbuffer.append(s+"\n");
					}
					int resourcesLength = sbuffer.length();
					if ((pList[0] != N_ip) && (Integer.parseInt(pList[1]) != N_port)) {
						send(String.format("%04d", resourcesLength) + " " + sbuffer.toString(), pList[0], Integer.parseInt(pList[1]));
					}
					else N_resources = subArr;
					
					i = i + numOfRes;
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException Occurred.");
		}catch (IOException e) {
			System.out.println("IOException Occured.");
		}
	}
	
	public static void queries(int numOfQueries, Double s) {
		try {
			ZipfDistribution zf = new ZipfDistribution(resources.length, s);
			int searchKeyIndex;
			String searchKey;
			
			for (int i = 0; i < numOfQueries; i++) {
				searchKeyIndex = zf.sample() - 1;
				
				if (searchKeyIndex < 0) {
					searchKeyIndex = 0;
				}
				
				if (searchKeyIndex > resources.length) {
					searchKeyIndex = resources.length - 1;
				}
				
				searchKey = resources[searchKeyIndex];
				
				if (N_resources.contains(searchKey)) {
					System.out.println("The queried file is already in this node.");
					logger.log(Level.INFO,"The queried file is already in this node.");
				}
				else {
					String search = "SER" + N_ip + " " + N_port + " " + searchKey + " " + hops;
					String msg = String.format("%04d", search.length()) + " " + search;
					for (String key : RT.keySet()) {
						String[] sockAdd = key.split(" ");
						lis.send(msg, sockAdd[0], Integer.parseInt(sockAdd[1]));
						/*String[] repMsg = serRep.split(" " );
						if (repMsg[1].equals("SEROK")) {
							if (Integer.parseInt(repMsg[2]) >= 1){
								System.out.println("Search Successful. Found " + repMsg[2] + " file(s) at " + repMsg[3] + ":" + repMsg[4]);
								logger.log(Level.INFO, "Search Successful. Found " + repMsg[2] + " file(s) at " + repMsg[3] + ":" + repMsg[4]);
							}
							 add other errors.
						}*/
					}
				}
			}
		} catch (NumberFormatException e) {
			System.err.println("Error: Got non-integer port number.");
		}
	}
	
	public static void send(String Message, String ip, int Port) {
		try {
			logger.log(Level.INFO, "Sending the message to Socket address: " + ip + " " + Port);
			InetAddress IP;
			IP = InetAddress.getByName(ip);
			System.out.println("Message in msgRT: " + Message);
			byte[] send = Message.getBytes();
			DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
			sock.send(sndpkt);
		} catch (UnknownHostException e) {
			logger.log(Level.WARNING, "Error: Unable to resolve " + ip);
			System.err.println("Error: Unable to resolve " + ip);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Errror: IO exception while sending message");
			System.err.println("Errror: IO exception while sending message");
		}
	}
}
