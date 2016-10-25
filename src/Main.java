import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Main{
    public static void main( String [] args ) throws InterruptedException  {
        JFrame frame = new JFrame();
        frame.add( new JLabel(" Output" ), BorderLayout.NORTH );

        JTextArea ta = new JTextArea(15, 30);
        TextAreaOutputStream taos = new TextAreaOutputStream( ta, 60 );
        PrintStream ps = new PrintStream( taos );
        System.setOut( ps );
        System.setErr( ps );


        frame.add( new JScrollPane( ta )  );

        frame.pack();
        frame.setVisible( true );

        for( int i = 0 ; i < 100 ; i++ ) {
            System.out.println( i );
            Thread.sleep( 500 );
        }
    }
}