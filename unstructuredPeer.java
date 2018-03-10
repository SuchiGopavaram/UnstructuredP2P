//package UnstructuredP2P;

import java.io.BufferedReader;											// Importing the necessary classes.
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
	
	public static int N_port;                                            //Declaring the global variables.
	public static String N_ip;
	public static int BS_port;
	public static String BS_ip;
	public static Logger logger = Logger.getLogger("NodeLog");
	public static DatagramSocket sock;
	public static ConcurrentHashMap<String, String> RT = new ConcurrentHashMap<String, String>();
	public ConcurrentHashMap<String, ConcurrentHashMap<String, ArrayList<String>>> knownResourses;
	public static String[] resources;
	public static String uname;
	public static ConcurrentHashMap<String, String> N_resources = new ConcurrentHashMap<String, String>();
	public static peerListen lis;
	public static int hops;
	
	public static void main(String[] args) {
		try {
			FileHandler log_file = new FileHandler("Node.Log");           //Providing a file to the file handler.
			SimpleFormatter formatter = new SimpleFormatter();            //Logging in Human readable format.
			log_file.setFormatter(formatter);
			logger.addHandler(log_file);
			logger.setUseParentHandlers(false);
			
			if (Arrays.asList(args).contains("--help") || Arrays.asList(args).contains("-h")) {
	            System.out.println(printHelpMessage());
	            System.exit(0);            
	        }
			
			InetAddress Node_ip = InetAddress.getLocalHost();
			N_ip = Node_ip.getHostAddress();
			BS_ip = args[1];
			logger.log(Level.INFO, "Using the BootStrap Server with IP: " + BS_ip + " Port: " + BS_port);
			logger.log(Level.INFO, "Initializing node with IP address: " + N_ip + " on Port: " + N_port);
			N_port = Integer.parseInt(args[0]);
			BS_port = Integer.parseInt(args[2]);

			if ((N_port <= 5000 || N_port >= 65535) || (BS_port <= 5000 || BS_port >= 65535 )) {		// Handling Port Exceptions.
				System.out.println("Error: Please type an integer in the range of 5001 - 65535 for port number(s).");
				logger.log(Level.WARNING, "User assigned a port number which is out of port ranges.");
				System.exit(1);
			}
			
			Scanner sc = new Scanner(System.in);						// Catching the input from the Keyboard.
			System.out.println("Give username of the network.");
			uname =sc.nextLine();
			logger.log(Level.INFO, "Initiated username as " + uname);
			System.out.println("Give the maximum number of hops.");
			hops = Integer.parseInt(sc.nextLine());
			logger.log(Level.INFO, "Number of maximum hops is " + hops);
				
			File File  = new File("resources.txt");					// Providing the file's name to be read.
			FileReader fr = new FileReader(File);						
			BufferedReader br = new BufferedReader(fr);
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {				// Reading the mentioned file.
				if (line.contains("#")) {							// Avoiding the lines starting with #.
					continue;
				}
				sb.append(line);
				sb.append("\n");
			}
			fr.close();												// Closing the file.
			br.close();												// Closing the file reader.

			resources = sb.toString().split("\n");					// Splitting the resources.
			
			sock = new DatagramSocket();								// Initializing the socket.
			logger.log(Level.INFO, "Socket has been created.");		
			logger.log(Level.INFO, "Trying to register with the BootStrap server with username given by user: " + uname);
			Register(uname);											// Calling Register method to register node with BootStrap server.
			
			lis = new peerListen(N_port, N_ip, RT, N_resources);        // Initializing the peerListen class.
			new Thread(lis).start();									// Starting a new thread for listening.
			logger.log(Level.INFO,"Listen thread started");
			
			lis.addResourcesAndHops(resources, hops);
			
			System.out.println("Sending join messages to the IP's received from Bootstrapper");
			logger.log(Level.INFO, "Trying to join with the nodes provided by the BootStrap server.");
			join();														// Calling join method to join into the network.
			
			
			while(true) {												// Accepting the User Entry Commands. 
				String s = sc.nextLine();
				String[] S = s.split(" ");
				String fileN = "";
				for (int i = 1; i < S.length; i++) {
					fileN = fileN + S[i] + " ";
				}
				fileN = fileN.trim();
				
				switch(S[0]){
				case "leave":										   // Catching the LEAVE message.
					logger.log(Level.INFO, "Trying to leave from BootStrap Server and nodes in the Routing Table.");
					System.out.println("Sending leave messages to the BootStrap Server and nodes");
					unstructuredPeer.leave(uname);					   // Calling the leave method.
					RT.clear();										   // Clearing the Routing Table of the node after leaving.
					logger.log(Level.INFO, "Left from BootStrap Server and nodes in the Routing Table.");
					
					System.out.println("Routing Table:");			   // Printing Routing Table after clearing it [Checking].
					for (String name: RT.keySet()){ 
			            System.out.println(name);  
					}
					break;
					
				case "distribute":									   // Catching the distribute message.
					System.out.println("Distributing file contents to all the nodes in the network.");
					logger.log(Level.INFO, "Distributing file contents to all the nodes in the network.");
					fileDist(Integer.parseInt(S[1]));				   // Calling fileDist method to distribute resources to all the nodes
					break;											   // in the network.
								
				case "query":                              			   // Catching the query message.
					//External query code.
					logger.log(Level.INFO, "External Query from the user received.");
					int noFiles = 0;
					String Files = "";
					for(String file : N_resources.keySet()) {
						if (file.contains(fileN)) {					  // Checking for file matches.
							Files = Files + file + "\n";
							noFiles++;						           // Counting the number of file matches.
						}
					} 
					if (noFiles > 0) {								   // Checking if file matches occured or not.
						System.out.println("The queried file is already in this node.");
						logger.log(Level.INFO,"The queried file is already in this node.");
					}
					else {												// Sending the search message.
						String query = "SER " + N_ip + " " + N_port + " " + fileN + " " + hops + " " + System.currentTimeMillis();
						String querySave = "SER " + N_ip + " " + N_port + " " + fileN + " " + System.currentTimeMillis();
						String queryMsg = String.format("%04d", query.length()) + " " + query;
						for (String Add : RT.keySet()) {
							String[] sockAdd = Add.split(" ");
							lis.send(queryMsg, sockAdd[0], Integer.parseInt(sockAdd[1]));
							if (!lis.searchMessage.contains(querySave)) {
								lis.searchMessage.add(querySave);
							}
							logger.log(Level.INFO,"The Search message is sent to all the nodes in the routing table.");
						}
					}
					break;
					
				case "queries":
					try {												// Generating given number of queries with the given zipf's distribution exponent. 
						queries(Integer.parseInt(S[1]), Double.parseDouble(S[2]));
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println("Usage:\n"
								+ "queries <no of queries> <Zipf's exponent>: "
								+ "Sends the number of queries as asked by the user with the given Zipf's distribution");
						}
					
					break;
			
				case "answered":
					System.out.println(lis.queriesAnswered);
					break;
					
				case "forwarded":
					System.out.println(lis.queriesForwarded);
					break;
					
				case "add":												// Logic for Adding the given resource to the node.
					//add resource code.
					boolean mark = false;
					for (String file : N_resources.keySet()) {
						if(file.equals(fileN)) {
							mark = true;
							continue;
						}
					}
					if (mark) {
						System.out.println("Resource is already present in the node.");
						System.out.println("Resources present in the node: \n" + N_resources);
					}
					else {
						N_resources.put(fileN,"");
					}
					break;
					
				case "remove":											// Logic for Removing the given resource to the node.
					//delete resource code.
					boolean Mark = false;
					for (String file : N_resources.keySet()) {
						if(file.equals(fileN)) {
							Mark = true;
							//continue;
						}
					}
					if (Mark) {
						N_resources.remove(fileN);
						System.out.println("Resource is removed from the node.");
					}
					else {
						System.out.println("Resource is not present in the node.");
						System.out.println("Resources present in the node: \n" + N_resources);
					}
					break;
					
				case "print" :											// Printing Resources, Routing table and its size.
					try {
						if (S[1].equals("resources")) {					// Printing Resources if user requested resources.
							System.out.println("Resources in this node:\n");
							for (String file : N_resources.keySet()) {
								System.out.println(file);
							}
						}
						else if ((S[1] + S[2]).equals("routingtable")) {// Printing Routing Table and its size.
							try {
								if (S[3].equals("size")) {
									System.out.println("Routing Table Size: "+ RT.size());
								}
							} catch(ArrayIndexOutOfBoundsException e){
								System.out.println("Routing Table: ");
								for (String name: RT.keySet()){ 
						            System.out.println(name);
								}
							}
						}
						else {											// Printing the correct usage in case user made a mistake.
							System.out.println("Usage:\n"
									+ "print routing table: 		prints the routing table\n"
									+ "print routing table size: 	prints the size of routing table\n"
									+ "print resources:				prints the resources present in the node\n");
						}
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println("Usage:\n"
								+ "print routing table: 		prints the routing table\n"
								+ "print routing table size: 	prints the size of routing table\n"
								+ "print resources:				prints the resources present in the node\n");
					}
					break;
					
				case "exit":											// Shutting down the node.
					logger.log(Level.INFO,"Shutting down the node.");
					log_file.close();
					lis.log_file.close();
					sc.close();
					sock.close();
					System.exit(0);
				case "DEL":												// Deleting the Network from the BootStrap Server.
					try {
						logger.log(Level.INFO,"Deleting the network from the BootStrap Server.");
						System.out.println("Deleting the network from the BootStrap Server.");
						if (S[1].equals("UNAME")) {
							delUname(S[2]);
						}
						logger.log(Level.INFO,"Shutting down the node.");
						log_file.close();
						lis.log_file.close();
						sc.close();
						System.exit(0);
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println("Usage:\n"
								+ "DEL UNAME <uname>: Deletes the network <uname> from the Bootstrap Server");
					}
					
				default:												// // Printing the correct usage in case user made a mistake.
					System.out.println("Usage: \n"
							+ "add <Resource name>:                     Adds resource to the node.\n"
							+ "remove <Resource name>:                  Deletes resource from the node.\n"
							+ "leave:                                   Leaves the network.\n"
							+ "DEL UNAME <username>:                    Deletes the network <username> from Bootstrap Server.\n"
							+ "print routing:                           Prints routing table.\n"
							+ "print routing table size:                Prints the size of routing table\n"
							+ "print resources:                         Prints resources in this node.\n"
							+ "answered:                                Gives the queries ansered till now.\n"
							+ "forwarded:                               Gives the number of queries forwarded till now.\n"
							+ "distribute <resources per node>:         Distributes the resources.txt contents to all the nodes in the network.\n"
							+ "query <(part of)file name>:              Queries the given file name or part of the file name\n"
							+ "queries <no of qeries> <zipfs exponent>: Generates the number of queries given wit hthat Zipf's exponent.\n"
							+ "exit:                                    Exits the program.\n"
							//add the added features here
							);
					break;
				}
			}
		}
		
		catch (NumberFormatException e) {
			System.err.println("Error: Please give an integer port number(s).");
			logger.log(Level.WARNING, "User gave a non-numeric port number(s).");
			System.exit(1);
		}
		catch (IOException e) {
			System.out.println("Error: IOException Occured.");
			System.err.println("Error: Check the IP address. Only numbers less than 255 should be given in each field of IP.");
			logger.log(Level.WARNING, "IOException Occured. "
					+ "User assigned an invalid range of IP number.");
			e.printStackTrace();
			System.exit(1);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Error: Check the number of arguments given. "
					+ "\n Command Usage: java Unstructuredpeer REG <Node_Port> <BootStrap_IP> <BootStrap_Port> <UserName>");
			logger.log(Level.WARNING, "Invalid number of arguments given." 
					+ "\n Command Usage: java Unstructuredpeer REG <Node_Port> <BootStrap_IP> <BootStrap_Port> <UserName>");
		}
		sock.close();
	}
	
	private static String printHelpMessage() {
        StringBuilder helpBuilder = new StringBuilder();
        helpBuilder.append("java unstructuredPeer <portnum> <bootstrap ip> <bootstrap port>");
        helpBuilder.append(System.getProperty("line.separator"));
        helpBuilder.append("All arguments are required.");
        helpBuilder.append(System.getProperty("line.separator"));
        helpBuilder.append("<portnum> is the port number at which the machine will listen to");
        helpBuilder.append(System.getProperty("line.separator"));
        helpBuilder.append("<bootstrap ip> is the ip address of the bootstrapper");
        helpBuilder.append(System.getProperty("line.separator"));
        helpBuilder.append("<bootstrap port> is the port num at which the bootstrapper will listen to");
        helpBuilder.append(System.getProperty("line.separator"));
        return helpBuilder.toString();
   }
	
															// msgRT method for sending and receiving the packets.
	public static String msgRT(String Message, String ip, int Port)throws IOException {
		logger.log(Level.INFO, "Sending the message to Socket address: " + ip + " " + Port);
		InetAddress IP = InetAddress.getByName(ip);
		byte[] send = Message.getBytes();
		DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
		sock.send(sndpkt);
		byte[] rcv = new byte[1023];
		DatagramPacket rcvpkt = new DatagramPacket(rcv, rcv.length);
		sock.receive(rcvpkt);
		logger.log(Level.INFO, "Received the message from Socket address: " + ip + " " + Port);
		String reply = new String(rcvpkt.getData(),0,rcvpkt.getLength());
		return reply;
	}
	
	public static void Register(String uname)  {			// Register method for registering a node in the BootStrap Server. 
		try {
			String msg1 = "REG "+ N_ip + " " + Integer.toString(N_port) + " " + uname;
			int len = msg1.length();
			String msg = String.format("%04d", len) + " " + msg1;
			String reply =  msgRT(msg,BS_ip,BS_port);      // Sending REG and receiving REGOK message, to and from the BootStrap Server respectively.
			String[] rep = reply.split(" ");
			if (rep[1].equals("REGOK")) {				   // Catching the received REGOK and checking for the registration status.
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
				System.out.println("Unknown command, undefined characters to BootStrap Server.");
				logger.log(Level.WARNING, "Unknown command, undefined characters to BootStrap Server while registering.");
				System.exit(1);
			}
		}catch (IOException e) {
			System.out.println("IOException Occured while registering to the BootStrap server.");
			logger.log(Level.WARNING, "IOException Occured while registering to the BootStrap server.");
		}
		
	}
	
	public static void join()  {                 // Join method to send join messages to the Socket Address provided by the BootStrap Server.
		try {
			String JoinMsg = "JOIN " + N_ip + " " + Integer.toString(N_port);
			int len = JoinMsg.length();
			JoinMsg = String.format("%04d", len) + " " + JoinMsg;
			for (String num: RT.keySet()) {
				String[] sockAdd = num.split(" "); // Sending and receiving the JOIN and JOINOK messages.
				String reply = msgRT(JoinMsg, sockAdd[0], Integer.parseInt(sockAdd[1]));
				String[] node_reply = reply.split(" ");
				if (node_reply[1] == "JOINOK"){   // Catching the received JOINOK and checking for the JOIN status.
					if (node_reply[2] == "0") {
						System.out.println(num + " has been added to the routing table as the JOIN message succeeded.");
					}
					else {
						RT.remove(num);         // If JOINOK has failed then that particular node is not added to RT.
						logger.log(Level.INFO, num + " has been removed from the routing table as the JOIN message failed.");
					}
					if (node_reply[2] == "9999") {
						System.out.println("Node " + num + " did not added my IP in it's Routing Table!");
						logger.log(Level.INFO, "Node " + num + " did not added my IP in it's Routing Table!");
					}
				}
			}
			
		} catch (NumberFormatException e) {
			System.err.println(e);
			System.err.println("Error: Routing table contains non-numeric characters in the port field.");
			logger.log(Level.WARNING, "Routing table contains non-numeric characters in the port field.");
		}
		catch (IOException e) {
			System.err.println("Error: I/O error occured while joining to the network.");
			logger.log(Level.WARNING, "Error: I/O error occured while joining to the network.");
		}
		
	}
	
	public static void leave(String uname) {  //  Leave method to inform all the nodes and BootStrap server that it is leaving.
		while (true) {	
			try {
				String DelMsg = "DEL IPADDRESS " + N_ip + " " + Integer.toString(N_port) + " "+ uname;
				int delLen = DelMsg.length();
				DelMsg = String.format("%04d", delLen) + " " + DelMsg;
				
				for(int i = 0; i < 3; i++) {
					String reply = msgRT(DelMsg, BS_ip, BS_port);  // Sending DEL message to the BootStrap server.
					String[] bs_reply = reply.split(" ");
					if(bs_reply[1].equals("DEL")) {				   // Checking the status if DEL operation.
						if (bs_reply[bs_reply.length - 1].equals("1")) {
							System.out.println("Left Bootstrap Server Successfully.");
							logger.log(Level.INFO, "Left Bootstrap Server Successfully.");
						}
						else if (bs_reply[bs_reply.length - 1].equals("-1")) {
							System.out.println("Error: in DEL Command.");
							logger.log(Level.WARNING, "Error in DEL Command.");
						}
						else if (bs_reply[bs_reply.length - 1].equals("9998")) {
							System.out.println("Error: (IP Address + Port ) not registered for username.");
							logger.log(Level.WARNING, "(IP Address + Port ) not registered for username.");
						}
						else if (bs_reply[bs_reply.length - 1].equals("9999")) {
							System.out.println("Error: Username not registered with BootStrap Server.");
							logger.log(Level.WARNING, "Username not registered with BootStrap Server.");
						}
						break;
					}
					else {
						System.out.println("Error: Bootstrapper did not remove my IP from it's List! Trying again. "
								+ Integer.toString(3 - (i + 1))+" times remaining");
						logger.log(Level.WARNING, "Bootstrapper did not remove my IP from it's List! Trying again. "
								+ Integer.toString(3 - (i + 1)) + " times remaining");
					}
				}
				                                                    // Sending LEAVE message to all the modes in its RT.
				String LeaveMsg = "LEAVE " + N_ip + " " + Integer.toString(N_port);
				int leaveLen = LeaveMsg.length();
				LeaveMsg = String.format("%04d", leaveLen) + " " + LeaveMsg;
				
				for (String num: RT.keySet()) {
					String[] sockAdd = num.split(" ");
					for(int i = 0; i < 3; i++) {
						String reply = msgRT(LeaveMsg, sockAdd[0], Integer.parseInt(sockAdd[1]));
						String[] node_reply = reply.split(" ");
						if (node_reply[2] != "0") {					// Checking the status if LEAVE operation.
							System.out.println("Left from " + num + " node Successfully");
							logger.log(Level.INFO, "Left from " + num + " node Successfully");
							break;
						}
						else if (node_reply[2] == "9999") {
							System.out.println("Error: Node " + num + " did not remove my IP from it's Routing Table! Trying again. " 
									+ Integer.toString(3 - (i + 1))+" times remaining");
							logger.log(Level.WARNING, "Node" + num + " did not remove my IP from it's Routing Table! Trying again. "
									+ Integer.toString(3 - (i + 1))+" times remaining");
						}
					}	
				}
				break;
			}
			catch (IOException e) {
				System.err.println("Error: I/O error occurred while joining to the network.");
				logger.log(Level.WARNING, "I/O error occurred while joining to the network.");
			}
			catch (NumberFormatException e) {
				System.err.println("Error: Routing table contains non-numeric characters in the port field.");
				logger.log(Level.WARNING, "Routing table contains non-numeric characters in the port field.");
			}
		}
	}
	
	public static void fileDist(int numOfRes) {						// Distributing the resources to all the nodes.
		try {
			List<String> peerList = getIpList();					// Acquiring the IPLIST from the BootStrap Server.

			int i = 0 ;
			for (String sockAddress : peerList) {					// Sending the resources to all the nodes in the IPLIST.
				String[] pList = sockAddress.split(":");				
				List<String> subArr = Arrays.asList(resources).subList(i, i + numOfRes);
				String sbuffer = "";
				sbuffer = sbuffer + "RESOURCES ";
				
				for (String s : subArr)
				{
					sbuffer = sbuffer + s + "\n";
				}
				int resourcesLength = sbuffer.length();			   // Checking for the Home node of file distribution.
				if (pList[0].equals(N_ip) && Integer.parseInt(pList[1]) == N_port) {
					for ( String file : subArr) {
						N_resources.put(file, "");
					}
				}
				else {
					System.out.println("sending resources");
					send(String.format("%04d", resourcesLength) + " " + sbuffer, pList[0], Integer.parseInt(pList[1]));
				}
				i = i + numOfRes;
				if (i == resources.length) {
					System.out.println("All resources distributed.");
					break;
				}			
			}
		} catch (FileNotFoundException e) {							// Handling all the required exceptions.
			System.err.println("Error: FileNotFoundException Occurred.");
			logger.log(Level.WARNING, "Error: FileNotFoundException Occurred while distributing the resources to the nodes.");
		} catch (IOException e) {
			System.err.println("Error: IOException Occured.");
			logger.log(Level.WARNING, "Error: IOException Occured while distributing the resources to the nodes.");
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Error: ArrayIndexOutOfBoundsException occured while distributing the resources to the nodes.");
			logger.log(Level.WARNING, "Error: ArrayIndexOutOfBoundsException occured while distributing the resources to the nodes.");
		}
	}
																	// Send method for only sending the packets.
	public static void send(String Message, String ip, int Port) {
		try {
			logger.log(Level.INFO, "Sending the message to Socket address: " + ip + " " + Port);
			InetAddress IP;
			IP = InetAddress.getByName(ip);
			byte[] send = Message.getBytes();
			DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
			sock.send(sndpkt);
		} catch (UnknownHostException e) {                          // Handling all the required exceptions.
			logger.log(Level.WARNING, "Error: Unable to resolve " + ip);
			System.err.println("Error: Unable to resolve " + ip);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Errror: IO exception while sending message");
			System.err.println("Errror: IO exception while sending message");
		}
	}
	
	public static List<String> getIpList() throws IOException {		// Method to acquire IPLIST from BootStrap Server.
		String peerAddress;
		List<String> peerList = new ArrayList<String>();
		
		String ipList = "GET IPLIST " + uname;						// Creating the GET IPLIST command.
		String msg = String.format("%04d", ipList.length()) + " " + ipList;
		logger.log(Level.INFO, "Requesting the IPLIST from the BootStrap Server.");
		String reply =  msgRT(msg,BS_ip,BS_port);					// Sending and receiving GET IPLIST command and it's reply.
		logger.log(Level.INFO, "Received the IPLIST from the BootStrap Server.");
		String[] a = reply.split(" ");
		
		if (a[3].equals("OK")) {									// Checking for the status of the received message.
			for (int i = 6; i <= a.length - 2; i = i + 2) {
				peerAddress = a[i] + ":" + a[i+1];
				peerList.add(peerAddress);
			}
		}
		return peerList;
	}
	
	public static void queries(int numOfQueries, Double s) {  		// Method for Generating given number of queries with given zipf's distribution exponent.
		try {
			System.out.println(resources.length);
			ZipfDistribution zf = new ZipfDistribution(resources.length, s);  // Initializing the zipf's distribution.
			int searchKeyIndex;
			String searchKey;
			
			for (int i = 0; i < numOfQueries; i++) {				// Generating the given number of queries.
				searchKeyIndex = zf.sample() - 1;
				
				if (searchKeyIndex < 0) {
					searchKeyIndex = 0;
				}
				
				if (searchKeyIndex > resources.length) {
					searchKeyIndex = resources.length - 1;
				}
				
				searchKey = resources[searchKeyIndex];				// Selecting the file name randomly based on priority.
				
				if (N_resources.contains(searchKey)) {				// Checking the node itself for the resource.
					System.out.println("The queried file is already in this node.");
					logger.log(Level.INFO,"The queried file is already in this node.");
				}
				else {
					System.out.println(searchKey);					// Creating the search message.
					String search = "SER " + N_ip + " " + N_port + " " + searchKey + " " + hops + " " + System.currentTimeMillis();
					String msg = String.format("%04d", search.length()) + " " + search;
					for (String key : RT.keySet()) {
						String[] sockAdd = key.split(" ");          // Sending the search message to all nodes in its RT.
						lis.send(msg, sockAdd[0], Integer.parseInt(sockAdd[1]));
						logger.log(Level.INFO, "The Search message is sent to all the nodes in the routing table.");
					}
				}
				while (true) {
					
					
					if (lis.queryFlag){
						break;						
					}
				}
			}
		} catch (NumberFormatException e) {
			System.err.println("Error: Got non-integer port number.");
			logger.log(Level.WARNING, "Error: Got non-integer port number.");
		}
	}
	
	public static void delUname(String uname) throws IOException {
		
		String delUname = "DEL UNAME " + uname;						// Creating the DEL UNAME command.
		logger.log(Level.INFO, "Requesting to delete the network " + uname + " from the BootStrap Server.");
		String msg = String.format("%04d", delUname.length()) + " " + delUname;
		msg = msgRT(msg, BS_ip, BS_port);							// Sending and receiving DEL UNAME command and it's reply.
		String[] reply = msg.split(" ");
		if (reply[3].equals("OK")) {								// Checking for the status of the received message.
			if (reply[4].equals(uname) && Integer.parseInt(reply[5]) == 1) {
				System.out.println("Network " + uname + " has been successfully deleted from the BootStrap Server.");
				logger.log(Level.INFO, "Network " + uname + " has been successfully deleted from the BootStrap Server.");
			}
			else if((reply.length - 1) == 9999) {
				System.err.println("Error while registering i.e. username is not present in the BootStrap Server.");
				logger.log(Level.WARNING, "Error while registering i.e. username is not present in the BootStrap Server.");
			}
		}
	}
	
	
}
