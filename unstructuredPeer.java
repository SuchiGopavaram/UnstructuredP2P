package Lab04;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class unstructuredPeer {
	
	public static int N_port;
	public static String N_ip;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		InetAddress Node_ip = InetAddress.getLocalHost();
		N_ip = Node_ip.getHostAddress();
		int BS_port = -1;
		String BS_ip = args[2];
		String uname = args[4];
		if (args[0].equals("REG")) {
			try {
				N_port = Integer.parseInt(args[1]);
				BS_port = Integer.parseInt(args[3]);
			}
			catch (Exception e) {
				System.err.println("Please type an integer in the range of 5001 - 65535 for port number(s)");
				System.exit(1);
			}
			unstructuredPeer.Register(BS_ip, BS_port, uname);
		}

	}
	
	public static String msgRT(String Message, String ip, int Port) throws IOException {
		DatagramSocket sock = new DatagramSocket();
		InetAddress IP = InetAddress.getByName(ip);
		byte[] send = Message.getBytes();
		DatagramPacket sndpkt = new DatagramPacket(send, send.length, IP, Port);
		sock.send(sndpkt);
		byte[] rcv = new byte[1023];
		DatagramPacket rcvpkt = new DatagramPacket(rcv, rcv.length);
		sock.receive(rcvpkt);
		String reply = new String(rcvpkt.getData(),0,rcvpkt.getLength());
		System.out.println(reply);
		sock.close();
		return reply;
	}
	
	public static String Register(String ip, int Port, String uname) throws IOException {
		String msg1 = " REG "+ N_ip + " " + Integer.toString(N_port) +" " + uname;
		int len = msg1.length() + 4;
		String msg = String.format("%04d", len) + msg1;
		return msgRT(msg,ip,Port);
	}
}
