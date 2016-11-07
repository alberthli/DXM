import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Albert on 10/26/16.
 */
public class ServerTest {

    public static void main(String[] args) throws Exception {
        ServerTest server = new ServerTest();
        server.run();
    }

    public void run() throws Exception {

        ServerSocket ssocket = new ServerSocket(13085);
        Socket socket = ssocket.accept();

        InputStreamReader ir = new InputStreamReader(socket.getInputStream());
        BufferedReader br = new BufferedReader(ir);

        while(true) {
            String msg = br.readLine();
            System.out.println(msg);

            if (msg != null) {
                PrintStream ps = new PrintStream(socket.getOutputStream());
                ps.println("msg received");
            }
        }

    }

}
