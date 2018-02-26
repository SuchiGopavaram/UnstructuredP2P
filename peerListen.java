package UnstructuredP2P;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class peerListen extends Thread{
	
	public void run() {
		
	}
	
	public static void main(String[] args) {
		
	}
	
	public static void rcvSnd(String Message, String ip, int Port) throws IOException {
		DatagramSocket Sock = new DatagramSocket();
		InetAddress IP = InetAddress.getByName(ip);
		byte[] rcv = new byte[1023];
		DatagramPacket rcvpkt = new DatagramPacket(rcv, rcv.length);
		Sock.receive(rcvpkt);
		byte[] send = new byte[1023];
		DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
		Sock.send(sndpkt);
	}
	
	
}
