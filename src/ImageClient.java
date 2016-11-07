/**
 * Created by Albert on 10/26/16.
 */

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

public class ImageClient {

    private static final int PORT = 13085;

    private static OutputStream outputStream;
    private static InputStream inputStream;

    private static boolean advanced = false;
    private static boolean pass = false;

    private static JTextArea ta;


    // Replaces the Scanner when in a JFrame - code modified from StackExchange
    public static String getInput() throws InterruptedException {

        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final CountDownLatch latch = new CountDownLatch(1);
        KeyEventDispatcher dispatcher = new KeyEventDispatcher() {
            // Anonymous class invoked from EDT
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    latch.countDown();
                return false;
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
        latch.await();  // current thread waits here until countDown() is called
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
        String[] wordsArray = ta.getText().split("\\s+");
        String lastWord = wordsArray[wordsArray.length - 1];
        return lastWord;
    }

    public static void main(String[] args) {

        try {

            JFrame frame = new JFrame();
            KeyListener kl = new KeyListener() {

                public void keyTyped(KeyEvent e) {
                    try {
                        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                            outputStream.write('d');
                        }

                        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                            outputStream.write('a');
                        }

                        if (e.getKeyCode() == KeyEvent.VK_UP) {
                            outputStream.write('w');
                        }

                        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                            outputStream.write('s');
                        }

                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            outputStream.write('j');
                            pass = true;
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                public void keyPressed(KeyEvent e) {
                    try {
                        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                            outputStream.write('d');
                        }

                        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                            outputStream.write('a');
                        }

                        if (e.getKeyCode() == KeyEvent.VK_UP) {
                            outputStream.write('w');
                        }

                        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                            outputStream.write('s');
                        }

                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            outputStream.write('j');
                            pass = true;
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                public void keyReleased(KeyEvent e) {
                    // NO EFFECT
                }
            };

            frame.add(new JLabel("Whiteboard Printer"), BorderLayout.CENTER);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ta = new JTextArea(50, 150);
            DefaultCaret caret = (DefaultCaret) ta.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);


            TextAreaOutputStream taos = new TextAreaOutputStream(ta, 100);
            PrintStream ps = new PrintStream(taos);
            System.setOut(ps);
            System.setErr(ps);

            frame.add(new JScrollPane(ta));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);

            // MENU START

            System.out.println("Welcome to the MarkerBot Control Interface!\n");

            // *********************************
            // ***** SETTING UP THE SOCKET *****
            // *********************************

            Socket socket = new Socket("localhost", PORT);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();

            // TEST CASE HARDCODED IN - CHANGE THIS LATER
            BufferedImage image = ImageIO.read(new File("images/Test3.jpg"));

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", byteArrayOutputStream);

            byte[] size = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array();
            outputStream.write(size);
            outputStream.write(byteArrayOutputStream.toByteArray());
            outputStream.flush();

            System.out.println("Your image has been sent!");

            System.out.println("\nTo enter inputs, type what you want to send and press ENTER afterwards.");

            System.out.println("\nIf you are an advanced user who wants to modify MARKER THICKNESS or ");
            System.out.println("RGB SENSITIVITY, enter \"Y\". Enter anything else to continue as a normal user.\n");

            String setting = getInput();

            if (setting.equals("Y") || setting.equals("y")) {
                outputStream.write('y');
                outputStream.flush();
                advanced = true;
                System.out.print("\nAdvanced Mode Activated");

            } else {
                outputStream.write('n');
                outputStream.flush();
                System.out.print("Normal Mode Activated");

            }

            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {

                }
                System.out.print(".");
            }

            if (advanced) {

                System.out.println();
                System.out.println("________________________________________________________");
                System.out.println();
                System.out.println("\n[ADVANCED]");
                System.out.println("Choose the RGB SENSITIVITY. This is a number between 0 and 255");
                System.out.println("that determines the RGB values that will cause a pixel to be");
                System.out.println("included in the drawing.\n");

                while (true) {
                    try {
                        String numString = getInput();
                        int num = Integer.parseInt(numString);

                        if (num < 0 || num > 255) {
                            System.out.println("\nPlease enter a number between 0 and 255!\n");
                            continue;
                        }

                        System.out.println("\nRGB Sensitivity set to: " + num + "\n");
                        byte[] rgbSensitivity = new byte[4];
                        outputStream.write(rgbSensitivity);
                        outputStream.flush();

                        for (int i = 0; i < 3; i++) {
                            try {
                                Thread.sleep(200);
                            } catch (Exception e) {

                            }
                            System.out.print(".");
                        }
                        System.out.println();

                        break;

                    } catch (Exception e) {
                        System.out.println("\nPlease enter a number between 0 and 255!\n");
                    }
                }

                System.out.println();
                System.out.println("________________________________________________________");
                System.out.println();
                System.out.println("\n[ADVANCED]");
                System.out.println("Choose the MARKER THICKNESS. This is an ODD integer that controls");
                System.out.println("how thick the marker is, and thus controls how many pixels are");
                System.out.println("traversed by the marker, changing the path. [RECOMMENDED: 5]\n");

                while (true) {
                    try {
                        String numString = getInput();
                        int num = Integer.parseInt(numString);

                        if (num % 2 != 1) {
                            System.out.println("\nPlease enter an ODD INTEGER! [RECOMMENDED: 5]\n");
                            continue;
                        }

                        System.out.println("\nMarker Thickness set to: " + num + "\n");
                        byte[] thicknessArray = new byte[4];
                        outputStream.write(thicknessArray);
                        outputStream.flush();

                        for (int i = 0; i < 3; i++) {
                            try {
                                Thread.sleep(200);
                            } catch (Exception e) {

                            }
                            System.out.print(".");
                        }
                        System.out.println();

                        break;

                    } catch (Exception e) {
                        System.out.println("\nPlease enter a number between 0 and 255!\n");
                    }
                }

            }

            System.out.println();
            System.out.println("________________________________________________________");
            System.out.println();
            System.out.println("Would you like to receive coordinate data from the Arduino? Enter \"Y\"");
            System.out.println("to accept, anything else for no.\n");

            String receive = getInput();

            if (!receive.equals("Y") && !receive.equals("y")) {
                outputStream.write('y');
                outputStream.flush();

                System.out.println();
                System.out.println("________________________________________________________");
                System.out.println();
                System.out.println("Coordinate Data Deactivated.");
            } else {
                System.out.println();
                System.out.println("________________________________________________________");
                System.out.println();
                System.out.println("Coordinate Data Activated.");
            }

            System.out.print("\nPROCESSING DATA! PLEASE BE PATIENT");
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {

                }
                System.out.print(".");
            }
            System.out.println("\n");

            byte[] readyArray = new byte[1];
            readyArray[0] = 'i';
            inputStream.read(readyArray);

            while (readyArray[0] != 'r') {
                inputStream.read(readyArray);
            }

            System.out.println("PROCESSING COMPLETE!\n");

            System.out.println("Attemping Serial Communication...\n");

            byte[] successArray = new byte[1];
            successArray[0] = 'i';
            inputStream.read(successArray);

            while (successArray[0] != 's') {
                inputStream.read(successArray);
            }

            System.out.println("Port Connection Successful!");

            System.out.println("________________________________________________________");
            System.out.println();
            System.out.println("[IMPORTANT]");
            System.out.println("Calibrate the marker's origin using the ARROW KEYS if needed. Try to place the marker");
            System.out.println("about 1 inch from each of the sides.");
            System.out.println("\n*** DO NOT HOLD DOWN THE KEYS! LIGHTLY TAP THEM OR THE PRINTER WILL STOP WORKING. ***");
            System.out.println("If this happens, simply restart the printer.");
            System.out.println("\nPress ENTER to continue.");

            // EDIT THE KEYLISTENER STUFF
            ta.addKeyListener(kl);

            while (!pass) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int len = ta.getDocument().getLength();
                ta.setCaretPosition(len);
            }

            ta.removeKeyListener(kl);

            System.out.println("________________________________________________________");
            System.out.println();
            System.out.println("NOTE: If the program doesn't work, there is probably a synchronization issue stemming" +
                    " from issues with serial communication.");
            System.out.println("Simply try restarting communication by restarting the" +
                    " program or reconnecting the hardware.");
            System.out.println("\n*** IF YOU SEE THE SERVOS STALLING UNPLUG TO AVOID DAMAGE TO THE ELECTRONICS! ***");
            System.out.println("\nPress ENTER to begin printing.\n");
            getInput();

            System.out.println();
            System.out.println("________________________________________________________");
            System.out.println();
            System.out.println("***** Printing... *****\n");

            byte[] completeArray = new byte[1];
            completeArray[0] = 'i';
            inputStream.read(completeArray);

            while (completeArray[0] != 'c') {
                inputStream.read(completeArray);
            }

            System.out.println();
            System.out.println("________________________________________________________");
            System.out.println();
            System.out.println("\nSerial Communication Complete! Enjoy your print!");
            System.out.println("This window will close in 5 seconds.");

            socket.close();

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
