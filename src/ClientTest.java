import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by Albert on 10/26/16.
 */
public class ClientTest {

    public static void main(String[] args) throws Exception {
        ClientTest clientTest = new ClientTest();
        clientTest.run();
    }

    public void run() throws Exception {

        Socket socket = new Socket("localhost", 13085);
        PrintStream ps = new PrintStream(socket.getOutputStream());

        InputStreamReader ir = new InputStreamReader(socket.getInputStream());
        BufferedReader br = new BufferedReader(ir);

        String msg = br.readLine();
        System.out.println(msg);

        OutputStream os = socket.getOutputStream();

        while(true) {
            os.write('b');
        }

    }

}
