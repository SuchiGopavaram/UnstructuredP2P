package UnstructuredP2P;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;
import java.util.logging.Level;

public class peerListen extends Thread{
	public static DatagramSocket Sock;
	public static Logger logger;
	
	public peerListen() {
		
	}
	
	public void run() {
		while(true) {
			try {
				String rcvReq = rcv();
			} catch (IOException e) {
				System.err.println("IOException Occured.");
				System.exit(1);
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
