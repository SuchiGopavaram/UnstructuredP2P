package UnstructuredP2P;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;

public class peerListen extends Thread{
	
	public Logger logger = Logger.getLogger("ListenLog");
	public FileHandler log_file;
	public DatagramSocket Sock;
	public ConcurrentMap<String, String> RTObj;
	public int N_port;
	
	public peerListen(int N_Port,ConcurrentHashMap<String, String> table) {
		N_port = N_Port;
		RTObj = table;
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
				System.err.println("Encountered IO exception. Got invalid IP address. Trying again. " 
						+Integer.toString(3-(i+1))+" times remaining.");
				logger.log(Level.WARNING, "IOException while receiveing packet");
			}
		}
		
		String reply[] = {new String(rcvpkt.getData(),0,rcvpkt.getLength()),
				rcvpkt.getAddress().toString().substring(1, rcvpkt.getAddress().toString().length()),
					Integer.toString(rcvpkt.getPort())};
		
		System.out.println(reply);
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
				System.err.println("Encountered IO exception. Got invalid IP address. Trying again. "
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
			System.err.println("file handler SecurityException");
			
		} catch (IOException e2) {
			System.err.println(e2);
			System.err.println("IOException. Socket Error");
			
		}
		
		while(true) {
			while(true) {
				try {
					String[] rcvReq = rcv();
					String[] msg = rcvReq[0].split(" ");
					if (Integer.parseInt(msg[0]) != rcvReq[0].length()-5) {
						System.out.println("corrupted message received. Going to listening mode.");
						logger.log(Level.WARNING, "corrupted message received. Going to listening mode.");
						break;
					}
					
					String IP = msg[2];
					String send_msg;
					
					switch (msg[1]) {
					
					case "JOIN":
						logger.log(Level.INFO, "Received Join message.");
						RTObj.put(IP,msg[3]);
						if(RTObj.get(IP) == msg[3]) {
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
						RTObj.remove(IP, msg[3]);
						if(!RTObj.containsKey(IP)) {
							send_msg = "0009 LEAVEOK 0";
							logger.log(Level.INFO, "LEAVE successful.");
						}
						else {
							send_msg = "0012 LEAVEOK 9999";
							logger.log(Level.INFO, "LEAVE failed.");
						}
						send(send_msg,rcvReq[1],Integer.parseInt(rcvReq[2]));
						break;
						
					case "QUERY":
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
				}	
			}
			log_file.close();
		}
	}
}
