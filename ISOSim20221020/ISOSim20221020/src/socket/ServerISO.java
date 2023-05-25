package socket;
 
import helper.ISOUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

 
/**
 *
 * @author Martinus Ady H <mrt.itnewbies@gmail.com>
 */
public class ServerISO {
 
    private static final Integer PORT = 12345;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server siap menerima koneksi pada port ["+PORT+"]");
        
        SocketThread socketThread1 = new SocketThread(serverSocket);
        socketThread1.start();
        SocketThread socketThread2 = new SocketThread(serverSocket);
        socketThread2.start();
        SocketThread socketThread3 = new SocketThread(serverSocket);
        socketThread3.start();
    } 
 


}
