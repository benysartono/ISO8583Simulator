package socket;

import helper.ISOUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import com.megatus.megaengine.listener.tcp.TcpListener;

/**
 *
 * @author Martinus Ady H <mrt.itnewbies@gmail.com>
 */
public class ServerISO {

	private static final Integer PORT = 9902;
	private static final Map<String, Integer> mappingDENetworkMsg = new HashMap<String, Integer>();

	/* Method ini berfungsi untuk menginisialisasi data element dan panjang tiap
	 * -tiap data element yang aktif */
	private static void initMappingDENetworkRequest() {
		/* [data-element] [panjang data element] */
		mappingDENetworkMsg.put("2", 99);
		mappingDENetworkMsg.put("3", 6);
		mappingDENetworkMsg.put("4", 12);
		mappingDENetworkMsg.put("7", 10);
		mappingDENetworkMsg.put("11", 6);
		mappingDENetworkMsg.put("12", 6);
		mappingDENetworkMsg.put("13", 4);
		mappingDENetworkMsg.put("15", 4);
		mappingDENetworkMsg.put("18", 4);
		mappingDENetworkMsg.put("32", 99);
		mappingDENetworkMsg.put("37", 12);
		mappingDENetworkMsg.put("39", 2);
		mappingDENetworkMsg.put("41", 16);
		mappingDENetworkMsg.put("47", 999);
		mappingDENetworkMsg.put("48", 999);
		mappingDENetworkMsg.put("49", 3);
		mappingDENetworkMsg.put("62", 999);
		mappingDENetworkMsg.put("63", 999);
	}


	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		TcpListener tcpListener;
		
		
		initMappingDENetworkRequest();
		ServerSocket serverSocket = new ServerSocket(PORT);
		System.out.println("Server siap menerima koneksi pada port ["+PORT+"]");
		System.out.println("Mulai");
		Socket socket = serverSocket.accept();
		while(true) {
			InputStreamReader inStreamReader = new InputStreamReader(socket.getInputStream());
			PrintWriter sendMsg = new PrintWriter(socket.getOutputStream());

			InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
			String clientIpAddress = socketAddress.getAddress().getHostAddress();
			Integer clientPort = socketAddress.getPort();
			System.out.println("\r");
			System.out.println("\r");
			System.out.println("Client IP Address: " + clientIpAddress);
			System.out.println("Client Port: " + clientPort);


			int data;
			StringBuffer sb = new StringBuffer();
			int counter = 0;

			// tambahan 4 karakter karena msg header adalah 4 digit msg length
			int lengthOfMsg = 4;
			while((data = inStreamReader.read()) != 0) {
				counter++;
				sb.append((char) data);
				if (counter == 4) lengthOfMsg += Integer.valueOf(sb.toString());

				// klo panjang msg dari MTI sampai END OF MSG sama dengan nilai
				// header maka lanjutkan ke method processingMsg();
				if (lengthOfMsg == sb.toString().length()) {
					System.out.println("Rec. Msg ["+sb.toString()+"] len ["+sb.toString().length()+"]");
					processingMsg(sb.toString(), sendMsg);
				}
			}
			socket.close();
			serverSocket.close();
			serverSocket = new ServerSocket(PORT);
			socket = serverSocket.accept();
		}
		//sendMsg.close();
		//inStreamReader.close();
		//socket.close();
		//serverSocket.close();
	}	

	/** Memproses msg yang dikirim oleh client berdasarkan nilai MTI.
	 * @param data request msg yang berisi [header 4byte][MTI][BITMAP][DATA ELEMENT]
	 * @param sendMsg object printWriter untuk menuliskan msg ke network stream
	 */
	private static void processingMsg(String data, PrintWriter sendMsg) {
		// msg.asli tanpa 4 digit msg.header
		String origMsgWithoutMsgHeader = data.substring(4, data.length());

		// cek nilai MTI
		if (ISOUtil.findMTI(origMsgWithoutMsgHeader).equalsIgnoreCase("0200")) {
			handleNetworkMsg(origMsgWithoutMsgHeader, sendMsg);
		}
	}

	/** Method ini akan memproses network management request dan akan menambahkan
	 * 1 data element yaitu data element 39 (response code) 000 ke client/sender
	 * @param networkMsg request msg yang berisi [header 4byte][MTI][BITMAP][DATA ELEMENT]
	 * @param sendMsg object printWriter untuk menuliskan msg ke network stream
	 */
	private static void handleNetworkMsg(String networkMsg, PrintWriter sendMsg) {
		int panjangBitmap = ISOUtil.findLengthOfBitmap(networkMsg);
		System.out.println("Panjang Bitmaps: " + panjangBitmap);
		String hexaBitmap = networkMsg.substring(4, 4+panjangBitmap);
		System.out.println("Hexa Bitmaps: " + hexaBitmap);

		// hitung bitmap
		String binaryBitmap = ISOUtil.findBinaryBitmapFromHexa(hexaBitmap);
		System.out.println("Binary Bitmaps" + binaryBitmap);
		String[] activeDE = ISOUtil.findActiveDE(binaryBitmap).split(";");

		StringBuilder networkResp = new StringBuilder();

		// setting MTI untuk reply network request
		networkResp.append("0210");

		// untuk reply, DE yang aktif adalah DE[2,3,4,7,11,12,13,15,18,32,37,39,41,47,48,49,62 dan 63]
		String bitmapReply = ISOUtil.getHexaBitmapFromActiveDE(new int[] {2,3,4,7,11,12,13,15,18,32,37,39,41,47,48,49,62,63});
		networkResp.append(bitmapReply);

		// index msg dimulai dr (4 digit MTI+panjang bitmap = index DE ke 3)
		int startIndexMsg = 4+ISOUtil.findLengthOfBitmap(networkMsg);
		int nextIndex = startIndexMsg;
		String sisaDefaultDE = "";

		// ambil nilai DE yang sama dulu
		for (int i=0;i<activeDE.length;i++) {
			// ambil bit ke 2
			if(activeDE[i].equalsIgnoreCase("2")) {
				startIndexMsg = nextIndex;
				// ambil dulu var.len utk DE 2
				int varLen = Integer.valueOf(networkMsg.substring(startIndexMsg, (startIndexMsg+2)));
				// 2 digit utk variabel len
				varLen += 2;
				nextIndex += varLen;
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(2, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("3")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(3, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("4")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(4, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("7")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(7, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("11")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(11, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("12")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(12, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("13")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(13, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("15")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(15, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("18")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(18, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("32")) {
				startIndexMsg = nextIndex;
				// ambil dulu var.len utk DE 32
				int varLen = Integer.valueOf(networkMsg.substring(startIndexMsg, (startIndexMsg+2)));
				// 2 digit utk variabel len
				varLen += 2;
				nextIndex += varLen;
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(32, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("37")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(37, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("39")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				networkResp.append(networkMsg.substring(startIndexMsg, nextIndex));
				debugMessage(39, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("41")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
				debugMessage(41, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("47")) {
				startIndexMsg = nextIndex;
				// ambil dulu var.len utk DE 47
				int varLen = Integer.valueOf(networkMsg.substring(startIndexMsg, (startIndexMsg+3)));
				// 3 digit utk variabel len
				varLen += 3;
				nextIndex += varLen;
				sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
				debugMessage(47, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("48")) {
				startIndexMsg = nextIndex;
				// ambil dulu var.len utk DE 48
				int varLen = Integer.valueOf(networkMsg.substring(startIndexMsg, (startIndexMsg+3)));
				// 3 digit utk variabel len
				varLen += 3;
				nextIndex += varLen;
				sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
				debugMessage(48, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("49")) {
				startIndexMsg = nextIndex;
				nextIndex += mappingDENetworkMsg.get(activeDE[i]);
				sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
				debugMessage(49, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("62")) {
				startIndexMsg = nextIndex;
				// ambil dulu var.len utk DE 62
				int varLen = Integer.valueOf(networkMsg.substring(startIndexMsg, (startIndexMsg+3)));
				// 3 digit utk variabel len
				varLen += 3;
				nextIndex += varLen;
				sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
				debugMessage(62, networkMsg.substring(startIndexMsg, nextIndex));
			} else if(activeDE[i].equalsIgnoreCase("63")) {
				startIndexMsg = nextIndex;
				// ambil dulu var.len utk DE 63
				int varLen = Integer.valueOf(networkMsg.substring(startIndexMsg, (startIndexMsg+3)));
				// 3 digit utk variabel len
				varLen += 3;
				nextIndex += varLen;
				sisaDefaultDE += networkMsg.substring(startIndexMsg, nextIndex);
				debugMessage(63, networkMsg.substring(startIndexMsg, nextIndex));
			} 
		}

		//kasih response kode 39 success
		networkResp.append("00");
		//tambahkan sisa default DE
		networkResp.append(sisaDefaultDE);

		// tambahkan length 4 digit utk msg.header
		String msgHeader = "";
		if (networkResp.length() < 10) msgHeader = "000" + networkResp.length();
		if (networkResp.length() < 100 && networkResp.length() >= 10) msgHeader = "00" + networkResp.length();
		if (networkResp.length() < 1000 && networkResp.length() >= 100) msgHeader = "0" + networkResp.length();
		if (networkResp.length() >= 1000) msgHeader = String.valueOf(networkResp.length());

		String finalMsg = msgHeader + networkResp.toString();

		// send to client
		sendMsg.print(finalMsg);
		sendMsg.flush();
	}

	private static String debugMessage(Integer fieldNo, String msg) {
		//System.out.println("["+fieldNo+"] ["+msg+"]");
		String strDebugMsg = null;
		strDebugMsg += ("["+fieldNo+"] ["+msg+"]"+ "\r\n");
		return strDebugMsg;
	}
}
