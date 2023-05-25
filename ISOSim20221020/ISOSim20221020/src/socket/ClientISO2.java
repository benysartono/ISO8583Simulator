package socket;
 
import helper.ISOUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
 
/**
 *
 * @author Martinus Ady H <mrt.itnewbies@gmail.com>
 */
public class ClientISO2 {
 
    //private final static Integer PORT_SERVER = 9902;
    private final static Integer PORT_SERVER = 12345;
 
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnknownHostException, IOException {
        //Socket clientSocket = new Socket("10.20.30.214", PORT_SERVER);
        Socket clientSocket = new Socket("0.0.0.0", PORT_SERVER);
        String networkRequest = buildNetworkReqMessage();
 
        PrintWriter outgoing = new PrintWriter(clientSocket.getOutputStream());
        InputStreamReader incoming = new InputStreamReader(clientSocket.getInputStream());
 
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
                System.out.println("Balasan diterima: ["+sb.toString()+"] len ["+sb.toString().length()+"]");
                System.out.println("Sent");
            }
        }
 
        outgoing.close();
        incoming.close();
        clientSocket.close();
    }
 
    private static String buildNetworkReqMessage() {
        StringBuilder networkReq = new StringBuilder();
 
        // MTI 0200
        networkReq.append("0200");
        // untuk request, DE yang aktif adalah DE[2,3,4,7,11,12,13,15,18,32,37,41,47,48,49 dan 63]
        String bitmapReq = ISOUtil.getHexaBitmapFromActiveDE(new int[] {2,3,4,7,11,12,13,15,18,32,37,41,47,48,49,63});
        networkReq.append(bitmapReq);
        // DE 2
        networkReq.append("1401112223331111");
        // DE 3
        networkReq.append("300000");
        // DE 4
        networkReq.append("000000500999");
        // DE 7
        networkReq.append("0616100046");
        // DE 11
        networkReq.append("000006");
        // DE 12
        networkReq.append("100046");
        // DE 13
        networkReq.append("0616");
        // DE 15
        networkReq.append("0616");
        // DE 18
        networkReq.append("7014");
        // DE 32
        networkReq.append("0286");
        // DE 37
        networkReq.append("000000000006");
        // DE 41
        networkReq.append("X000000123456789");
        // DE 47
        networkReq.append("018000000000000000000");
        // DE 48
        networkReq.append("0161234567890123456");
        // DE 49
        networkReq.append("360");
        // DE 63
        networkReq.append("006000002");

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