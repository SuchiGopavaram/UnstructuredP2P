package UnstructuredP2P;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;
import java.util.logging.Level;
import UnstructuredP2P.unstructuredPeer;

public class peerListen extends Thread{
	public static DatagramSocket Sock;
	public static Logger logger;
	
	public peerListen() {
		
	}
	
	public void run() {
		while(true) {
			while(true) {
				try {
					String rcvReq = rcv();
					String[] msg = rcvReq.split(" ");
					if (Integer.parseInt(msg[0])!=rcvReq.length()) {
						System.out.println("corrupted message received. Going to listening mode.");
						break;
					}
					String IP = msg[2];
					int Port = Integer.parseInt(msg[3]);
					String send_msg;
					switch (msg[1]) {
					case "JOIN":
						unstructuredPeer.RT.put(IP,msg[3]);
						if(unstructuredPeer.RT.get(IP)==msg[3]) {
							send_msg = "0013 JOINOK 0";
						}
						else {
							send_msg = "0016 JOINOK 9999";
						}
						send(send_msg,IP,Port);
						break;
					case "LEAVE":
						unstructuredPeer.RT.remove(IP, msg[3]);
						if(unstructuredPeer.RT.get(IP)==msg[3]) {
							send_msg = "0014 LEAVEOK 0";
						}
						else {
							send_msg = "0017 LEAVEOK 9999";
						}
						send(send_msg,IP,Port);
						break;
					case "QUERY":
						//Query code
						break;
					default:
						break;
					}
					break;
				} 
				catch (IOException e) {
					System.err.println("IOException Occured.");
					break;
				}
				catch (NumberFormatException e1) {
					System.err.println("Received alphabets in port number.");
					break;
				}
			}
		}
	}
	
	public static String rcv() throws IOException {
		Sock = new DatagramSocket();
		logger.log(Level.INFO, "Socket has been created.");
		byte[] rcv = new byte[1023];
		DatagramPacket rcvpkt = new DatagramPacket(rcv, rcv.length);
		Sock.receive(rcvpkt);
		String reply = new String(rcvpkt.getData(),0,rcvpkt.getLength());
		System.out.println(reply);
		Sock.close();
		logger.log(Level.INFO, "Socket has been closed.");
		return reply;
	}
	
	public static void send(String Message, String ip, int Port) throws IOException {
		Sock = new DatagramSocket();
		logger.log(Level.INFO, "Socket has been created.");
		InetAddress IP = InetAddress.getByName(ip);
		byte[] send = Message.getBytes();
		DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
		Sock.send(sndpkt);
		Sock.close();
		logger.log(Level.INFO, "Socket has been closed.");
	}
	
	
}
