package socket;

import helper.ISOUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

/**
 *
 * 
 */
public class ClientISO {

	private static final String FILENAME = "socketconfig.xml"; 
	private final static Integer PORT_SERVER = 9902;
	//private final static Integer PORT_SERVER = 12345;


	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {

		// Instantiate the Factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		String ipaddress = null;
		int port = 0;

		try {

			// optional, but recommended
			// process XML securely, avoid attacks like XML External Entities (XXE)
			//dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

			// parse XML file
			DocumentBuilder db = dbf.newDocumentBuilder();

			Document doc = db.parse(new File(FILENAME));

			// optional, but recommended
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			//System.out.println("Root Element :" + doc.getDocumentElement().getNodeName());
			//System.out.println("------");

			// get <staff>
			NodeList nodelist = doc.getElementsByTagName("conf");
			for (int temp = 0; temp < nodelist.getLength(); temp++) {

				Node node = nodelist.item(temp);

				if (node.getNodeType() == Node.ELEMENT_NODE) {

					Element element = (Element) node;

					// get text
					ipaddress = element.getElementsByTagName("ipaddress").item(0).getTextContent();
					port = Integer.parseInt(element.getElementsByTagName("port").item(0).getTextContent());


				}
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}


		//Socket clientSocket = new Socket("10.244.252.101", PORT_SERVER);
		Socket clientSocket = new Socket(ipaddress, port);
		String networkRequest = buildNetworkReqMessageFromFile();

		PrintWriter outgoing = new PrintWriter(clientSocket.getOutputStream());
		InputStreamReader incoming = new InputStreamReader(clientSocket.getInputStream());


		for(int cnt=1; cnt<7; cnt++) {
			System.out.println("Iteration: " + cnt);
			outgoing.print(networkRequest);
			outgoing.flush();

			int data;
			StringBuffer sb = new StringBuffer();
			int counter = 0;
			// tambahan 4 karakter karena msg header adalah 4 digit msg length
			int lengthOfMsg = 4;
			while((data = incoming.read()) != 0) {
				counter++;
				sb.append((char) data);
				if (counter == 4) lengthOfMsg += Integer.valueOf(sb.toString());

				// klo panjang msg dari MTI sampai END OF MSG sama dengan nilai
				// header maka lanjutkan ke method processingMsg();
				if (lengthOfMsg == sb.toString().length()) {
					System.out.println("Rec. Msg ["+sb.toString()+"] len ["+sb.toString().length()+"]");
					System.out.println("Sent");
				}
			}

			outgoing = new PrintWriter(clientSocket.getOutputStream());
			incoming = new InputStreamReader(clientSocket.getInputStream());
		}


		outgoing.close();
		incoming.close();

		clientSocket.close();
	}


	private static String buildNetworkReqMessageFromFile() {
		String finalNetworkReqMsgFromFile = new String();

		try {
			File myObj = new File("isomsg.txt");
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


	private static String buildNetworkReqMessage() {
		StringBuilder networkReq = new StringBuilder();

		// MTI 0200
		networkReq.append("0200");
		// untuk request, DE yang aktif adalah DE[2,3,4,7,11,12,13,15,18,32,37,41,48,49 dan 63]
		String bitmapReq = ISOUtil.getHexaBitmapFromActiveDE(new int[] {2,3,4,7,11,12,13,15,18,32,37,41,48,49,63});
		networkReq.append(bitmapReq);
		// DE 2
		networkReq.append("160000000000000000");
		// DE 3
		networkReq.append("300000");
		// DE 4
		networkReq.append("000000000000");
		// DE 7
		networkReq.append("0207145356");
		// DE 11
		networkReq.append("251824");
		// DE 12
		networkReq.append("145356");
		// DE 13
		networkReq.append("0207");
		// DE 15
		networkReq.append("0207");
		// DE 18
		networkReq.append("7014");
		// DE 32
		networkReq.append("040008");
		// DE 37
		networkReq.append("000001424700");
		// DE 41
		networkReq.append("                ");
		// DE 48
		networkReq.append("0255013123252024000411121100");
		// DE 49
		networkReq.append("360");
		// DE 63
		networkReq.append("006007000");

		/*
        // DE 48 Additional Private Data
        final String clientID = "CLNT001";
        // length de 48
        String lengthBit48 = "";
        if (clientID.length() < 10) lengthBit48 = "00" + clientID.length();
        if (clientID.length() < 100 && clientID.length() >= 10) lengthBit48 = "0" + clientID.length();
        if (clientID.length() == 100) lengthBit48 = String.valueOf(clientID.length());
        networkReq.append(lengthBit48);
        networkReq.append(clientID);

        // DE 70 Network Information Code
        networkReq.append("001");
		 */

		// tambahkan 4 digit length of msg sbg header
		String msgHeader = "";
		if (networkReq.toString().length() < 10) msgHeader = "000" + networkReq.toString().length();
		if (networkReq.toString().length() < 100 && networkReq.toString().length() >= 10) msgHeader = "00" + networkReq.toString().length();
		if (networkReq.toString().length() < 1000 && networkReq.toString().length() >= 100) msgHeader = "0" + networkReq.toString().length();
		if (networkReq.toString().length() >= 1000) msgHeader = String.valueOf(networkReq.toString().length());

		StringBuilder finalNetworkReqMsg = new StringBuilder();
		finalNetworkReqMsg.append(msgHeader);
		finalNetworkReqMsg.append(networkReq.toString());
		System.out.println("Final Message: " + finalNetworkReqMsg.toString());
		return finalNetworkReqMsg.toString();
	}

}