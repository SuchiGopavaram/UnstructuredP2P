package UnstructuredP2P;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Level;

public class unstructuredPeer {
	
	public static int N_port;
	public static String N_ip;
	public static int BS_port;
	public static String BS_ip;
	public static Logger logger = Logger.getLogger("NodeLog");
	public static FileHandler log_file;
	public static DatagramSocket sock;
	public static ConcurrentMap<String, String> RT = peerListen.RT;
	
	public static void main(String[] args) {
		try {
			log_file = new FileHandler("Node.Log");
			logger.setUseParentHandlers(false);
			//System.out.println("Starting Listen thread");
			new Thread(new peerListen()).start();
			logger.log(Level.INFO,"Listen thread strated");
			
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
			
			sock = peerListen.Sock;
			logger.log(Level.INFO, "Socket has been created.");
			
			System.out.println("Enter the Username that you want this node to connect to:");
			String uname = "Nodes20";
			System.out.println("Using username: "+uname);
			
			logger.log(Level.INFO, "Trying to register with the BootStrap server with username given by user: "+uname);
			//System.out.println("Registering to the Network in Bootstrap Server");
			unstructuredPeer.Register(uname);
			
			logger.log(Level.INFO, "Trying to join with the nodes provided by the BootStrap server.");
			System.out.println("Sending join messages to the IPs received from Bootstrapper");
			unstructuredPeer.join();
			
			System.out.println("Routing Table: ");
			for (String name: RT.keySet()){ 
	            System.out.println(name + " : " + RT.get(name));  
			}
			Scanner sc = new Scanner(System.in);
			String s = sc.nextLine();
			String[] S = s.split(" ");
			switch(S[0]){
			case "leave":
				logger.log(Level.INFO, "Trying to leave from BootStrap Server and nodes in the Routing Table.");
				System.out.println("Sending leave messages to the BootStrap Server and nodes");
				unstructuredPeer.leave(uname);
				
				System.out.println("Routing Table:");
				for (String name: RT.keySet()){
		            //String key =name.toString(); 
		            System.out.println(name + " : " + RT.get(name));  
				}
				
			case "add":
				//add resource code.
				
			case "delete":
				//delete resource code.
				
			case "exit":
				System.exit(0);
				
			default:
				System.out.println("Usage: \n add <Resource name>: Adds resource to the node. \n"
						+ "delete <Resource name>: deletes resource from the node. \n"
						+ "leave: Leaves the network. \n"
						+ "exit: Exits the program.");
			}
			sc.close();
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
			System.err.println("Check the number of arguments given. "
					+ "\n Command Usage: java Unstructuredpeer REG <Node_Port> <BootStrap_IP> <BootStrap_Port> <UserName>");
			logger.log(Level.WARNING, "Check the number of arguments given." 
					+ "\n Command Usage: java Unstructuredpeer REG <Node_Port> <BootStrap_IP> <BootStrap_Port> <UserName>");
		}
		
	}
	
	public static String msgRT(String Message, String ip, int Port) throws IOException {
		logger.log(Level.INFO, "Sending the message to Socket address: " + ip + " " + Port);
		InetAddress IP = InetAddress.getByName(ip);
		byte[] send = Message.getBytes();
		DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
		System.out.println("--------" +Message+ "-------"+ ip+":"+Integer.toString(Port));
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
				System.exit(1);
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
					RT.put(rep[4], rep[5]);
				}
				else if (rep[3].equals("2")){
					System.out.println("Node Registered Successfully.");
					logger.log(Level.INFO, "Node Registered Successfully.");
					RT.put(rep[4], rep[5]);
					RT.put(rep[6], rep[7]);
				}
				else if (rep[3].equals("3")){
					System.out.println("Node Registered Successfully.");
					logger.log(Level.INFO, "Node Registered Successfully.");
					RT.put(rep[4], rep[5]);
					RT.put(rep[6], rep[7]);
					RT.put(rep[8], rep[9]);
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
		while(true) {
			try {
				System.out.println("Join method. 1");
				String JoinMsg = "JOIN " + N_ip + " " + Integer.toString(N_port);
				int len = JoinMsg.length();
				JoinMsg = String.format("%04d", len) + " " + JoinMsg;
				System.out.println("Join method. 2");
				for (String num: RT.keySet()) {
					System.out.println("Join method. 3");
					String reply = msgRT(JoinMsg, num, Integer.parseInt(RT.get(num)));
					System.out.println("Join method. 4");
					String[] node_reply = reply.split(" ");
					if (node_reply[2] != "0") {
						RT.remove(num);
						logger.log(Level.INFO, num + " has been removed from the routing table as the JOIN message failed.");
					}
					else if (node_reply[2] == "0") {
						System.out.println(num + " has been added to the routing table as the JOIN message succeeded.");
					}
					if (node_reply[2] == "9999") {
						System.out.println("Node " + num + " did not added my IP in it's Routing Table!");
						logger.log(Level.INFO, "Node " + num + " did not added my IP in it's Routing Table!");
					}
				}
				System.out.println("Join method. 5");
				break;
				
			} catch (NumberFormatException e) {
				System.err.println("Routing table contains non-numeric characters in the port field.");
				logger.log(Level.WARNING, "Routing table contains non-numeric characters in the port field.");
			}
			catch (IOException e) {
				System.err.println("I/O error occured while joining to the network!");
			} 
		}
	}
	
	public static void leave(String uname) {
		while (true) {	
			try {
				String LeaveMsg = " DEL IPADDRESS " + N_ip + " " + Integer.toString(N_port) + " "+ uname;
				int len = LeaveMsg.length();
				LeaveMsg = String.format("%04d",  len) + " " + LeaveMsg;
				
				for(int i = 0; i < 3; i++) {
					String reply = msgRT(LeaveMsg, BS_ip, BS_port);
					String[] bs_reply = reply.split(" ");
					if (bs_reply[bs_reply.length - 1].equals("1")) {
						System.out.println("Left Bootstrap Server Successfully.");
						logger.log(Level.INFO, "Left Bootstrap Server Successfully.");
						break;
					}
					else if(bs_reply[1].equals("DEL")) {
						if (bs_reply[bs_reply.length - 1].equals("-1")) {
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
					}
					else {
						System.out.println("Bootstrapper did not remove my IP from it's List! Trying again. "
								+ Integer.toString(3 - (i + 1))+" times remaining");
						logger.log(Level.WARNING, "Bootstrapper did not remove my IP from it's List! Trying again. "
								+ Integer.toString(3 - (i + 1)) + " times remaining");
					}
				}	
				for (String num: RT.keySet()) {
					for(int i = 0; i < 3; i++) {
						String reply = msgRT(LeaveMsg, num, Integer.parseInt(RT.get(num)));
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
}
