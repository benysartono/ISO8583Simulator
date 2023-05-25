package socket;

import helper.ISOUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.megatus.megaengine.listener.tcp.TcpListener;
import com.megatus.megaengine.util.log.LoggerFactory;

/**
 *
 * @author Martinus Ady H <mrt.itnewbies@gmail.com>
 */


public class ServerISO {

	String strDebugMsg = null;
	private static volatile ServerISO instance = null;

	private static Logger LOG = LoggerFactory.getLogger("com.megatus");

	public static ServerISO getInstance()
	{
		if (instance == null) {
			synchronized (ServerISO.class) {
				if (instance == null) {
					instance = new ServerISO();
				}
			}
		}
		return instance;
	}

	ServerISO() {
		//	    setLogger(Logger.getLogger(MetaTUNCATE.class.getName()));
	}

	private static final Integer PORT = 9902;
	private static final Map<String, Integer> mappingDENetworkMsg = new HashMap<String, Integer>();

	/* Method ini berfungsi untuk menginisialisasi data element dan panjang tiap
	 * -tiap data element yang aktif */
	private void initMappingDENetworkRequest() {
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
	public String execute(Object args) throws IOException {
		LOG.warning("masuk main method");
		

		String rtnStr = null;
		String resStr = null;

		initMappingDENetworkRequest();

		InputSource inputSource = new InputSource(new StringReader((String)args));
		Document inDoc;
		XPath xpath = XPathFactory.newInstance().newXPath();
		String isoMsg = null;
		//if(inputSource != null) {
		try {
			inDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputSource);
			isoMsg = (String)xpath.evaluate("//reqContent/msg", inDoc, XPathConstants.STRING);
			//isoMsg = (String)xpath.evaluate("//reqContent/msg", inputSource, XPathConstants.STRING);
			LOG.warning("ISO-nya-: " + isoMsg);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} finally 
		{

			//while(true) {
			InputStream inputStream = new ByteArrayInputStream(isoMsg.getBytes());	
			int isoMsgLgth = isoMsg.getBytes().length;
			LOG.warning("inputStream nya: " + inputStream);
			int data;
			StringBuffer stringBuffer = new StringBuffer();
			int counter = 0;

			// tambahan 4 karakter karena msg header adalah 4 digit msg length
			int lengthOfMsg = 4;
			for(int in = 0; in < isoMsgLgth; in++) {
				data = inputStream.read();
				//while((data = inputStream.read()) != 0) {
				//LOG.warning("Data nya: " + data);
				counter++;
				stringBuffer.append((char) data);
				if (counter == 4) lengthOfMsg += Integer.valueOf(stringBuffer.toString());

				// klo panjang msg dari MTI sampai END OF MSG sama dengan nilai
				// header maka lanjutkan ke method processingMsg();
				if (lengthOfMsg == stringBuffer.toString().length()) {
					LOG.warning("stringBuffer nya: " + stringBuffer.toString());
					//System.out.println("Rec. Msg ["+sb.toString()+"] len ["+sb.toString().length()+"]");
					rtnStr = null;
					rtnStr = processingMsg(stringBuffer.toString());
					//processingMsg(sb.toString(), sendMsg);
				}
			}
			resStr = "<resVal>"
					+ "<msg><![CDATA["+rtnStr+"]]></msg>"
					+ "</resVal>";
			LOG.warning("resStr nya-: " + resStr);
			inputStream.close();
			return resStr;
			//System.out.println(resStr);
			//System.out.println("_______________________________________________________________________________________");
		}
		//}
	}	

	/** Memproses msg yang dikirim oleh client berdasarkan nilai MTI.
	 * @param data request msg yang berisi [header 4byte][MTI][BITMAP][DATA ELEMENT]
	 * @param sendMsg object printWriter untuk menuliskan msg ke network stream
	 */
	private String processingMsg(String data) {
		// msg.asli tanpa 4 digit msg.header
		String origMsgWithoutMsgHeader = data.substring(4, data.length());

		// cek nilai MTI
		if (ISOUtil.findMTI(origMsgWithoutMsgHeader).equalsIgnoreCase("0200")) {
			return handleNetworkMsg(origMsgWithoutMsgHeader);
		}
		else return null;
	}

	/** Method ini akan memproses network management request dan akan menambahkan
	 * 1 data element yaitu data element 39 (response code) 000 ke client/sender
	 * @param networkMsg request msg yang berisi [header 4byte][MTI][BITMAP][DATA ELEMENT]
	 * @param sendMsg object printWriter untuk menuliskan msg ke network stream
	 */
	private String handleNetworkMsg(String networkMsg) {
		int panjangBitmap = ISOUtil.findLengthOfBitmap(networkMsg);
		String hexaBitmap = networkMsg.substring(4, 4+panjangBitmap);

		// hitung bitmap
		String binaryBitmap = ISOUtil.findBinaryBitmapFromHexa(hexaBitmap);
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

		strDebugMsg = null;
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
		//sendMsg.print(finalMsg);
		//sendMsg.flush();

		//String rtnStr = "<result>"+ "<msg>" + finalMsg + "</msg>" + "</result>";
		//return rtnStr;

		return strDebugMsg;


	}

	private void debugMessage(Integer fieldNo, String msg) {
		//LOG.warning("["+fieldNo+"] ["+msg+"]");
		//String strDebugMsg = null;
		strDebugMsg += ("["+fieldNo+"] ["+msg+"]"+ "\r\n");
		//return strDebugMsg;
	}
}
