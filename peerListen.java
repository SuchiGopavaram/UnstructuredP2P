package UnstructuredP2P;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Level;

public class peerListen extends Thread{
	public static DatagramSocket Sock;
	public static Logger logger = Logger.getLogger("ListenLog");
	public static FileHandler log_file;
	
	public peerListen() {
		
	}
	
	public void run() {
		while(true) {
			while(true) {
				try {
					log_file = new FileHandler("Listen.Log");
					String rcvReq = rcv();
					String[] msg = rcvReq.split(" ");
					if (Integer.parseInt(msg[0]) != rcvReq.length()) {
						System.out.println("corrupted message received. Going to listening mode.");
						logger.log(Level.WARNING, "corrupted message received. Going to listening mode.");
						break;
					}
					String IP = msg[2];
					int Port = Integer.parseInt(msg[3]);
					String send_msg;
					switch (msg[1]) {
					case "JOIN":
						logger.log(Level.INFO, "Received Join message.");
						unstructuredPeer.RT.put(IP,msg[3]);
						if(unstructuredPeer.RT.get(IP) == msg[3]) {
							send_msg = "0013 JOINOK 0";
							logger.log(Level.INFO, "Added node to Routing Table.");
						}
						else {
							send_msg = "0016 JOINOK 9999";
							logger.log(Level.INFO, "Adding note to Routing table failed.");
						}
						send(send_msg,IP,Port);
						break;
					case "LEAVE":
						logger.log(Level.INFO, "Received LEAVE message.");
						unstructuredPeer.RT.remove(IP, msg[3]);
						if(unstructuredPeer.RT.get(IP)==msg[3]) {
							send_msg = "0014 LEAVEOK 0";
							logger.log(Level.INFO, "LEAVE successful.");
						}
						else {
							send_msg = "0017 LEAVEOK 9999";
							logger.log(Level.INFO, "LEAVE failed.");
						}
						send(send_msg,IP,Port);
						break;
					case "QUERY":
						logger.log(Level.INFO, "Socket has been created.");
						//Query code
						break;
					default:
						break;
					}
					break;
				} 
				catch (NumberFormatException e1) {
					System.err.println("Received alphabets in port number.");
					logger.log(Level.WARNING, "Received alphabets in port number.");
					break;
				} catch (SecurityException e) {
					System.err.println("SecurityException occurred.");
					logger.log(Level.WARNING, "SecurityException occurred.");
					break;
				} catch (IOException e) {
					System.err.println("IOException occured.");
					logger.log(Level.WARNING, "IOException occured.");
					break;
				}
				
			}
		}
	}
	
	public static String rcv() {
		
		for (int i = 0; i < 3; i++) {
			try {
				Sock = new DatagramSocket();
			} catch (SocketException e1) {
				System.err.println("Error occurred while creating socket. Trying again. "
						+Integer.toString(3-(i+1))+" times remaining.");
				logger.log(Level.WARNING, "Error creating socket. Trying again.");
			}
		}
		
		logger.log(Level.INFO, "Socket has been created.");
		byte[] rcv = new byte[1023];
		DatagramPacket rcvpkt = new DatagramPacket(rcv, rcv.length);
		
		for (int i = 0; i < 3; i++) {
			try {
				Sock.receive(rcvpkt);
				logger.log(Level.INFO, "Packet received.");
			} catch (IOException e) {
				System.err.println("Encountered IO exception. Got invalid IP address. Trying again. " 
						+Integer.toString(3-(i+1))+" times remaining.");
				logger.log(Level.WARNING, "IOException while receiveing packet");
			}
		}
		
		String reply = new String(rcvpkt.getData(),0,rcvpkt.getLength());
		System.out.println(reply);
		Sock.close();
		logger.log(Level.INFO, "Socket has been closed.");
		return reply;
	}
	
	public static void send(String Message, String ip, int Port) {
		for (int i = 0; i < 3; i++) {
			try {
				Sock = new DatagramSocket();
			} catch (SocketException e) {
				System.err.println("Error occurred while creating socket. Trying again. "
						+Integer.toString(3-(i+1))+" times remaining.");
				logger.log(Level.WARNING, "Error creating socket. Trying again.");
			}
		}
		
		logger.log(Level.INFO, "Socket has been created.");
		InetAddress IP;
		for (int i = 0; i < 3; i++) {
			try {
				IP = InetAddress.getByName(ip);
				byte[] send = Message.getBytes();
				DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
				Sock.send(sndpkt);
				logger.log(Level.INFO, "Packet sent.");
			} catch (UnknownHostException e) {
				System.err.println("Error occurred while getting self IP. Trying again. "
							+Integer.toString(3-(i+1))+" times remaining.");
				logger.log(Level.WARNING, "UnknownHostException while getting self IP");
			} catch (IOException e) {
				System.err.println("Encountered IO exception. Got invalid IP address. Trying again. "
							+ Integer.toString(3-(i+1))+" times remaining.");
				logger.log(Level.WARNING, "IOException while receiveing packet");
			}
		}	
		Sock.close();
		logger.log(Level.INFO, "Socket has been closed.");
	}
	
	
}
