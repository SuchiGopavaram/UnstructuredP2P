package UnstructuredP2P;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class peerListen extends Thread{
	static DatagramSocket Sock;
	
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
		byte[] rcv = new byte[1023];
		DatagramPacket rcvpkt = new DatagramPacket(rcv, rcv.length);
		Sock.receive(rcvpkt);
		String reply = new String(rcvpkt.getData(),0,rcvpkt.getLength());
		System.out.println(reply);
		Sock.close();
		return reply;
	}
	
	public static void send(String Message, String ip, int Port) throws IOException {
		Sock = new DatagramSocket();
		InetAddress IP = InetAddress.getByName(ip);
		byte[] send = Message.getBytes();
		DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
		Sock.send(sndpkt);
	}
	
	
}
