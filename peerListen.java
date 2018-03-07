//package UnstructuredP2P;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import org.apache.commons.math3.distribution.ZipfDistribution;

public class peerListen extends Thread{
	
	public static Logger logger = Logger.getLogger("ListenLog");
	public FileHandler log_file;
	public static DatagramSocket Sock;
	public ConcurrentHashMap<String, String> RTObj;
	public int N_port;
	public String N_ip;
	public ConcurrentHashMap<String, ConcurrentHashMap<String, ArrayList<String>>> knownResourses = new ConcurrentHashMap<String, ConcurrentHashMap<String, ArrayList<String>>>();
	public ConcurrentHashMap<String, ArrayList<String>> innerMap;
	public List<String> N_resources = Collections.synchronizedList(new ArrayList<String>());
	public String[] resources;
	public int hops;
	public boolean queryFlag;
	
	public peerListen(int N_Port, String N_IP, ConcurrentHashMap<String, String> table, List<String> resources) {
		N_port = N_Port;
		N_ip = N_IP;
		RTObj = table;
		N_resources = resources;
	}
	
	public void addResourcesAndHops(String[] res, int hop) {
		resources = res;
		hops = hop;
	}
	
	public String[] rcv() {
		System.out.println("waiting for message");
		byte[] rcve = new byte[1023];
		DatagramPacket rcvpkt = new DatagramPacket(rcve, rcve.length);
		
		for (int i = 0; i < 3; i++) {
			try {
				Sock.receive(rcvpkt);
				System.out.println("Packet received.");
				logger.log(Level.INFO, "Packet received.");
				break;
			} catch (IOException e) {
				System.err.println("Error: Encountered IO exception. Got invalid IP address. Trying again. " 
						+Integer.toString(3-(i+1))+" times remaining.");
				logger.log(Level.WARNING, "IOException while receiveing packet");
			}
		}
		
		String reply[] = {new String(rcvpkt.getData(),0,rcvpkt.getLength()),
				rcvpkt.getAddress().toString().substring(1, rcvpkt.getAddress().toString().length()),
					Integer.toString(rcvpkt.getPort())};
		System.out.println(reply[0]+"||"+reply[1]+"||"+reply[2]);
		return reply;
	}
	
	public void send(String Message, String ip, int port) {
		System.out.println("Sending message");
		InetAddress IP;
		for (int i = 0; i < 3; i++) {
			try {
				IP = InetAddress.getByName(ip);
				byte[] send = Message.getBytes();
				DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, port);
				Sock.send(sndpkt);
				System.out.println("Packet Sent.");
				logger.log(Level.INFO, "Packet sent.");
				break;
			} catch (IOException e) {
				System.err.println(e);
				System.err.println("Error: Encountered IO exception. Got invalid IP address. Trying again. "
							+ Integer.toString(3-(i+1))+" times remaining.");
				logger.log(Level.WARNING, "IOException while receiveing packet");
			}
		}
	}
	
	public void run() {
		System.out.println("Entered listening.");
		List<String> searchMessage = new ArrayList<String>();

		try {
			Sock = new DatagramSocket(N_port);
			log_file = new FileHandler("Listen.Log");
			SimpleFormatter formatter = new SimpleFormatter();
		    log_file.setFormatter(formatter);
			logger.addHandler(log_file);
			logger.setUseParentHandlers(false);
		} catch (SecurityException e2) {
			System.err.println("File Handler SecurityException.");
			logger.log(Level.WARNING, "File Handler SecurityException.");
			
		} catch (IOException e2) {
			System.err.println("IOException Occured. Socket Error.");
			logger.log(Level.WARNING, "IOException Occured. Socket Error.");
			
		} 
		
		while(true) {
			while(true) {
				try {
					String[] rcvReq = rcv();
					String[] msg = rcvReq[0].split(" ");
					if (Integer.parseInt(msg[0]) != rcvReq[0].length() - 5) {
						System.out.println("Corrupted message received. Going to listening mode.");
						logger.log(Level.WARNING, "Corrupted message received. Going to listening mode.");
						break;
					}
					
					String send_msg = "";	
					
					switch (msg[1]) {
					
					case "JOIN":
						logger.log(Level.INFO, "Received Join message.");
						RTObj.put(msg[2] + " " + msg[3],"");
						if(RTObj.containsKey(msg[2] + " " + msg[3])) {
							send_msg = "0008 JOINOK 0";
							logger.log(Level.INFO, "Added node to Routing Table.");
						}
						else {
							send_msg = "0011 JOINOK 9999";
							logger.log(Level.INFO, "Adding note to Routing table failed.");
						}
						send(send_msg,rcvReq[1],Integer.parseInt(rcvReq[2]));
						break;
						
					case "LEAVE":
						logger.log(Level.INFO, "Received LEAVE message.");
						RTObj.remove(msg[2] + " " + msg[3]);
						if(!RTObj.containsKey(msg[2] + " " + msg[3])) {
							send_msg = "0009 LEAVEOK 0";
							logger.log(Level.INFO, "LEAVE successful.");
						}
						else {
							send_msg = "0012 LEAVEOK 9999";
							logger.log(Level.INFO, "LEAVE failed.");
						}
						send(send_msg,rcvReq[1],Integer.parseInt(rcvReq[2]));
						break;
						
					case "SER":
						//Query code
						logger.log(Level.INFO, "Received SEARCH (SER) message.");
						System.out.println("SER Received: " +rcvReq[0]);
						int gotHop = Integer.parseInt(msg[msg.length-2]);
						
						String saveMsg = "";
						for (int i =0; i<=msg.length-3; i++) {
							saveMsg = saveMsg + msg[i] + " " ;
						}
						saveMsg = saveMsg.trim() + " " + msg[msg.length-1];
						if (searchMessage.contains(saveMsg)) {
							System.out.println("Search request already forwarded");
							continue;
						}
						
						if (Integer.parseInt(msg[msg.length-2]) <= 0) {
							System.out.println("Hop count reached. Killing packet.");
							continue;
						}
						System.out.println("Req: "+saveMsg+"|");
						for (String mesg : searchMessage) {
							System.out.println(mesg + "|");
						}
						
						int noFiles = 0;
						String Files = "";
						String fName = "";
						for (int i = 4; i<=msg.length-3; i++) {
							fName = fName + msg[i] + " ";
						}
						fName = fName.trim();
						
						for(String file : N_resources) {
							if (file.contains(fName)) {
								Files = Files + file +"\n";
								noFiles++;
							}
						}
						if (noFiles > 0) {
							System.out.println("Match(es) for the queried file found in the node.");
							logger.log(Level.INFO,"Match(es) for the queried file found in the node.");
							send_msg = "SEROK " + noFiles + " " + N_ip + " " + N_port + " " + Integer.toString(gotHop-1) + " "  + msg[msg.length-1] + " " + Files;
							send_msg = String.format("%04d",send_msg.length()) + " " + send_msg;
							send(send_msg, msg[2], Integer.parseInt(msg[3]));
							logger.log(Level.INFO,"The SEROK message with the found files is sent to the query node.");
						}
						else {
							logger.log(Level.INFO, "The queried file is not found in this node. Forwarding the search message to"
									+ " the nodes in this node's routing table.");
							for (int i = 1; i <= msg.length - 3;i++) {
								send_msg = send_msg + msg[i] + " ";
							}
							send_msg = send_msg.trim();
							send_msg = send_msg + " " + Integer.toString(gotHop - 1) + " " + msg[msg.length-1];
							send_msg = String.format("%04d",send_msg.length()) + " " +send_msg;
							System.out.println("Sending: " + send_msg);
							for (String key: RTObj.keySet()) {
								String[] keyArr = key.split(" " );
								if (keyArr[0].equals(rcvReq[1]) && keyArr[1].equals(rcvReq[2])) {
									continue;
								}
								send(send_msg, keyArr[0], Integer.parseInt(keyArr[1]));
							}
						}
						searchMessage.add(saveMsg);
						break;
						
					case "SEROK":
						System.out.println("SEROK Received: " +rcvReq[0]);
						int noOfFilesFound = Integer.parseInt(msg[2]);
						String IP = msg[3];
						int port = Integer.parseInt(msg[4]);
						String SockAdd = IP+":"+port;
						int foundHops = Integer.parseInt(msg[5]);
						double sendTime = Double.parseDouble(msg[6]);
						double timeNow = System.currentTimeMillis();
						int headerLen = msg[0].length() + msg[1].length() + msg[2].length() + msg[3].length() + msg[4].length() + msg[5].length() + msg[6].length();
						String foundFilesString = rcvReq[0].substring(headerLen + 7);
						ArrayList<String> result = new ArrayList<String>();
						result.add(msg[5]);
						String[] foundFiles = foundFilesString.split("\n");
						System.out.println("File(s) successfully found: \n" + foundFilesString + "at "+ IP + ":" + port +" in "+ (hops)+"-"+(foundHops) + " hop(s) in "+(timeNow-sendTime)+" milliseconds");;
						queryFlag = true;
						for (String FileName: foundFiles) {
							innerMap = knownResourses.get(FileName);
							if (innerMap!=null) {
								innerMap.put(SockAdd, result );
							}
						}
						break;
						
					case "RESOURCES":
						String filesLine = rcvReq[0].substring(15);
						String[] filesList = filesLine.split("\n");
						for (String file : filesList) {
							N_resources.add(file);
						}
						break;
						
					default:
						break;
						
					}
					break;
				} 
				catch (NumberFormatException e1) {
					System.err.println("Error: Received alphabets in port number.");
					logger.log(Level.WARNING, "Received alphabets in port number.");
					e1.printStackTrace();
					break;
				} catch (SecurityException e) {
					System.err.println("Error: SecurityException occurred.");
					logger.log(Level.WARNING, "SecurityException occurred.");
					break;
				}	
			}
			logger.log(Level.INFO, "Closing the LOG file.");
			log_file.close();
		}
	}
}
