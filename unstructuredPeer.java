package Lab04;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class unstructuredPeer {
	
	public static int N_port;
	public static String N_ip;
	public static ConcurrentMap<Integer, String[]> RT = new ConcurrentHashMap<Integer, String[]>();
	
	public static void main(String[] args) {
		try {
			InetAddress Node_ip = InetAddress.getLocalHost();
			N_ip = Node_ip.getHostAddress();
			int BS_port = -1;
			String BS_ip = args[2];
			String uname = args[4];
			if (args[0].equals("REG")) {
				N_port = Integer.parseInt(args[1]);
				BS_port = Integer.parseInt(args[3]);
			}
			
			if ((N_port <= 5000 || N_port >= 65535) || (BS_port <= 5000 || BS_port >= 65535 )) {
				System.out.println("Please type an integer in the range of 5001 - 65535 for port number(s)");
				System.exit(1);
			}
			unstructuredPeer.Register(BS_ip, BS_port, uname);
			for (Integer name: RT.keySet()){
	            String key =name.toString();
	            String[] value = RT.get(name);  
	            System.out.println(key + ":" + value[0] + " " + value[1]);  
			} 
			unstructuredPeer.join(N_ip, N_port);
		}
		
		catch (NumberFormatException e) {
			System.err.println("Please give an integer port number(s).");
			System.exit(1);
		}
		catch (IOException e) {
			System.out.println("IOException Occured.");
			System.err.println("Check the IP address. Only numbers less than 255 should be given in each field of IP.");
			System.exit(1);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Check the number of arguments given. \n Command Usage: java Unstructuredpeer REG <Node_Port> <BootStrap_IP> <BootStrap_Port> <UserName>");
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
	
	public static void Register(String ip, int Port, String uname) throws IOException {
		String msg1 = " REG "+ N_ip + " " + Integer.toString(N_port) + " " + uname;
		int len = msg1.length() + 4;
		String msg = String.format("%04d", len) + msg1;
		String reply =  msgRT(msg,ip,Port);
		String[] rep = reply.split(" ");
		if (rep[1].equals("REGOK")) {
			if (rep[rep.length - 1].equals("9998")) {
				System.out.println("Node alredy registered");
				System.exit(1);
			}
			else if (rep[rep.length - 1].equals("9999")) {
				System.out.println("Error in registering");
				System.exit(1);
			}
			else if (rep[rep.length - 1].equals("-1")) {
				System.out.println("Unknown REG command");
				System.exit(1);
			}
			else {
				if (rep[3].equals("0")) {
					System.out.println("Node Registered Successfully.");
				}
				else if (rep[3].equals("1")){
					System.out.println("Node Registered Successfully.");
					String[] arr = {rep[4],rep[5]};
					RT.put(1, arr);
				}
				else if (rep[3].equals("2")){
					System.out.println("Node Registered Successfully.");
					String[] arr = {rep[4],rep[5]};
					String[] arr1= {rep[6],rep[7]};
					RT.put(1, arr);
					RT.put(2, arr1);
				}
				else if (rep[3].equals("3")){
					System.out.println("Node Registered Successfully.");
					String[] arr = {rep[4],rep[5]};
					String[] arr1= {rep[6],rep[7]};
					String[] arr2= {rep[8],rep[9]};
					RT.put(1, arr);
					RT.put(2, arr1);
					RT.put(3, arr2);
				}
				else {
					System.out.println("Received unknown message.");
					System.exit(1);
				}
			}
		}
		/*else if(rep[1].equals("DEL")) {
			if (rep[rep.length - 1].equals("-1")) {
				System.out.println("Error in DEL Command");
				System.exit(1);
			}
			else if (rep[rep.length - 1].equals("9998")) {
				System.out.println("(IP Address + Port ) not registered for username");
				System.exit(1);
			}
			else if (rep[rep.length - 1].equals("9999")) {
				System.out.println("Username not registered with bootstrapper");
				System.exit(1);
			}
		}*/
		else if(rep[1].equals("BS")) {
			System.out.println("Unknown command, undefined characters to bootstrapper.");
			System.exit(1);
		}

	}
	
	public static void join(String IP, int Port) throws IOException {
		String JoinMsg = " JOIN " + N_ip + " " + Integer.toString(N_port);
		int len = JoinMsg.length() + 4;
		String joinMsg = String.format("%04d", len) + JoinMsg;
		for (Integer num: RT.keySet()) {
			String reply = msgRT(joinMsg, RT.get(num)[0], Integer.parseInt(RT.get(num)[1]));
		}
		/*String reply = msgRT(joinMsg, IP, Port);
		String[] rep = reply.split(" ");
		System.out.println(reply);*/
	}

}


