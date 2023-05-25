package socket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import helper.ISOUtil;

public class SocketThread extends Thread {
	protected ServerSocket serverSocket;
    private static final Map<String, Integer> mappingDENetworkMsg = new HashMap<String, Integer>();

	public SocketThread(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public void run() {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			InputStreamReader inStreamReader;
			inStreamReader = new InputStreamReader(socket.getInputStream());
			PrintWriter sendMsg;
			sendMsg = new PrintWriter(socket.getOutputStream());

			int data;
			StringBuffer sb = new StringBuffer();
			int counter = 0;

			// tambahan 4 karakter karena msg header adalah 4 digit msg length
			int lengthOfMsg = 4;
			while((data = inStreamReader.read()) != 0) {
				counter++;
				sb.append((char) data);
				if (counter == 4) lengthOfMsg += Integer.valueOf(sb.toString());

				if (lengthOfMsg == sb.toString().length()) {
	                String respMessage ="";
	                String sbLastDigit = sb.toString().substring(sb.length()-1);
	                respMessage=buildNetworkReqMessageFromFile(sbLastDigit);
	                processingMsg(respMessage, sendMsg);
				}
			} //--
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	} 
	
	
	private String buildNetworkReqMessageFromFile(String lastDigit) {
		String finalNetworkReqMsgFromFile = new String();

		try {
			String fileNm = "isomsg" + lastDigit + ".txt";
			File myObj = new File(fileNm);
			System.out.print("Waiting for Backend Process ....");
			while(!myObj.exists()) {

			}
			Scanner myReader = new Scanner(myObj);  
			while (myReader.hasNextLine()) {
				finalNetworkReqMsgFromFile = myReader.nextLine();
			}
			myReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		} 


		return finalNetworkReqMsgFromFile;

	}


	/** Memproses msg yang dikirim oleh client berdasarkan nilai MTI.
	 * @param data request msg yang berisi [header 4byte][MTI][BITMAP][DATA ELEMENT]
	 * @param sendMsg object printWriter untuk menuliskan msg ke network stream
	 */
	private void processingMsg(String data, PrintWriter sendMsg) {
		// msg.asli tanpa 4 digit msg.header
		String origMsgWithoutMsgHeader = data.substring(4, data.length());

		// cek nilai MTI
		//if (ISOUtil.findMTI(origMsgWithoutMsgHeader).equalsIgnoreCase("0200")) {
		handleNetworkMsg(origMsgWithoutMsgHeader, sendMsg);
		//}
	}

	/** Method ini akan memproses network management request dan akan menambahkan
	 * 1 data element yaitu data element 39 (response code) 000 ke client/sender
	 * @param networkMsg request msg yang berisi [header 4byte][MTI][BITMAP][DATA ELEMENT]
	 * @param sendMsg object printWriter untuk menuliskan msg ke network stream
	 */
	private void handleNetworkMsg(String networkMsg, PrintWriter sendMsg) {
		sendMsg.print(networkMsg);
		sendMsg.flush();
	}

	private void debugMessage(Integer fieldNo, String msg) {
		System.out.println("["+fieldNo+"] ["+msg+"]");
	}

	
}
