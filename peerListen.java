package UnstructuredP2P;
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
import java.util.concurrent.ConcurrentMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;

public class peerListen extends Thread{
	
	public static Logger logger = Logger.getLogger("ListenLog");
	public FileHandler log_file;
	public static DatagramSocket Sock;
	public ConcurrentMap<String, String> RTObj;
	public int N_port;
	public String N_ip;
	public static List<String> N_resources = Collections.synchronizedList(new ArrayList<String>());
	
	public peerListen(int N_Port, String N_IP, ConcurrentHashMap<String, String> table,List<String> resources) {
		N_port = N_Port;
		N_ip = N_IP;
		RTObj = table;
		N_resources = resources;
	}
/*	
	public void sendResources(List<String> res) {
		N_resources = res;
	}*/
	
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
		System.out.println(reply[0]);
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

		try {
			Sock = new DatagramSocket(N_port);
			
			log_file = new FileHandler("Listen.Log");
			SimpleFormatter formatter = new SimpleFormatter();
		    log_file.setFormatter(formatter);
			logger.addHandler(log_file);
			logger.setUseParentHandlers(false);
		} catch (SecurityException e2) {
			System.err.println("Error: File handler SecurityException");
			
		} catch (IOException e2) {
			System.err.println(e2);
			System.err.println("Error: IOException. Socket Error");
		}
		
		while(true) {
			while(true) {
				try {

					String[] rcvReq = rcv();
					String[] msg = rcvReq[0].split(" ");
					if (Integer.parseInt(msg[0]) != rcvReq[0].length()-5) {
						System.err.println("Error: Corrupted message received. Going to listening mode.");
						logger.log(Level.WARNING, "corrupted message received. Going to listening mode.");
						break;
					}
					
					String IP = msg[2];
					String send_msg = "";
					String sockAdd = IP+ " " + msg[3];	
					
					switch (msg[1]) {
					
					case "JOIN":
						logger.log(Level.INFO, "Received Join message.");
						RTObj.put(sockAdd,"");
						if(RTObj.containsKey(sockAdd)) {
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
						RTObj.remove(sockAdd);
						if(!RTObj.containsKey(sockAdd)) {
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
						int noFiles = 0;
						String Files = " ";
						for(String file :N_resources) {
							if (file.contains(msg[4])) {
								Files = Files + " " + file;
								noFiles++;
							}
						}
						if (noFiles > 0) {
							System.out.println("Match(es) for the queried file found in the node.");
							logger.log(Level.INFO,"Match(es) for the queried file found in the node.");
							send_msg = "SEROK " + noFiles + N_ip + " " + N_port + Files;
							send_msg = String.format("%04d",send_msg.length()) + " " + send_msg;
							send(send_msg, msg[2], Integer.parseInt(msg[3]));
						}
						else {
							for (int i = 1; i < msg.length-1;i++) {
								send_msg = send_msg + msg[i];
							}
							send_msg = send_msg + Integer.toString(Integer.parseInt(msg[msg.length]) - 1);
							for (String key: RTObj.keySet()) {
								String[] keyArr = key.split(" " );
								if ((keyArr[0] != rcvReq[1]) && keyArr[1] != rcvReq[2]) {
									send(send_msg, keyArr[0], Integer.parseInt(keyArr[1]));
								}			
							}
						}
						break;
						
					case "RESOURCES":
						String filesLine = rcvReq[0].substring(15);
						System.out.println(filesLine);
						String[] filesList = filesLine.split("\n");
						for (String file : filesList) {
							N_resources.add(file);
						}
						System.out.println("Resources in this node:\n");
						for (String file : N_resources) {
							System.out.println(file);
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
					break;
				} catch (SecurityException e) {
					System.err.println("Error: SecurityException occurred.");
					logger.log(Level.WARNING, "SecurityException occurred.");
					break;
				}	
			}
			log_file.close();
		}
	}
}
