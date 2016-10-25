/**
 * Author: Albert Li
 * Start Date: 10/8/16
 *
 * Description: Handles the conversion of Java-produced information into Arduino-readable and executable code.
 *
 * Changelog:
 *      UPCOMING CHANGES -
 *          - [IMPORTANT] Add in ability to move pen up and down
 *      v1.0.1 - 10/24/16
 *          - Added in message warning about serial communication issues.
 *          - Added in cheesy loading dots for my own amusement
 *      v1.0.0 - 10/23/16
 *          - First workable release. Not tested with assembly yet (still being designed).
 *          - Communicator with ability to:
 *              - Generate a path from an image and produce a set of Arduino-readable instructions
 *              - Receive signals from the Arduino to control speed that instructions are sent
 *      v0.1.0 - 10/22/16
 *          - Implemented ability to correctly parse serial data FROM the arduino. This will allow the program to
 *            know when to send in new data
 *      v0.0.1 - 10/8/16
 *          - Set up simple test case with LED to confirm that JSSC library is functional
 *
 */

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import jssc.*;

import javax.imageio.ImageIO;
import javax.swing.*;

public class ArduinoCommunicator {

    // the port object that is central to communication
    private static SerialPort serialPort;

    // ready is a variable tracking whether the Arduino has processed one set of instructions and is ready
    // to receive the next set. done tracks whether serial communication is finished. advanced indicates
    // whether the user is advanced and can modify special values.
    private static boolean ready = false;
    private static boolean done = false;
    private static boolean advanced = false;
    private static boolean receiveData = true;
    private static boolean pass = false;

    // these depend on the physical dimensions of the board
    private static final double DRAW_WINDOW_WIDTH = 12; // inches
    private static final double DRAW_WINDOW_HEIGHT = 9; // inches

    private static JTextArea ta;

    // Reads incoming messages from the Arduino
    private static class PortReader implements SerialPortEventListener {

        // Fixes a "Toolkit not initialized" error
        JFXPanel p = new JFXPanel();

        // String Reception
        StringBuilder message = new StringBuilder();
        Boolean receivingMessage = false;

        // Sets format for receiving and constructing messages FROM the Arduino. This is to bypass the difference
        // in speed between processing and serial communication. All messages begin with ">" and end with "\r".
        public void serialEvent(SerialPortEvent event) {
            if(event.isRXCHAR() && event.getEventValue() > 0){
                try {
                    byte buffer[] = serialPort.readBytes();
                    for (byte b: buffer) {
                        if (b == '>') {
                            receivingMessage = true;
                            message.setLength(0);
                        }
                        else if (receivingMessage == true) {
                            if (b == '\r') {
                                receivingMessage = false;
                                String toProcess = message.toString();
                                Platform.runLater(new Runnable() {
                                    @Override public void run() {
                                        if (toProcess.equals("r")) {
                                            ready = true;
                                            System.out.print("");
                                        } else if (toProcess.equals("d")) {
                                            done = true;
                                            System.out.print("");
                                        } else {
                                            if (receiveData) {
                                                System.out.println(toProcess);
                                            }
                                        }
                                    }
                                });
                            }
                            else {
                                message.append((char)b);
                            }
                        }
                    }
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                    System.out.println("serialEvent");
                }
            }
        }

    }

    // Replaces the Scanner when in a JFrame
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

    public static void main(String[] args) throws InterruptedException, IOException {

        // MAKES THE PROGRAM OPEN IN AN APPLET - This block was found and slightly modified from StackExchange.
        JFrame frame = new JFrame();
        KeyListener kl = new KeyListener() {
            public void keyTyped(KeyEvent e) {

            }

            public void keyPressed(KeyEvent e) {

                try {
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        serialPort.writeString("dg\n");
                        //System.out.println("dg\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        serialPort.writeString("ag\n");
                        //System.out.println("ag\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        serialPort.writeString("wg\n");
                        //System.out.println("wg\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        serialPort.writeString("sg\n");
                        //System.out.println("sg\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        pass = true;
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            public void keyReleased(KeyEvent e) {

                try {
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        serialPort.writeString("ds\n");
                        //System.out.println("ds\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        serialPort.writeString("as\n");
                        //System.out.println("as\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        serialPort.writeString("ws\n");
                        //System.out.println("ws\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        serialPort.writeString("ss\n");
                        //System.out.println("ss\n");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        };

        frame.add(new JLabel("Whiteboard Printer"), BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ta = new JTextArea(50, 100);

        TextAreaOutputStream taos = new TextAreaOutputStream(ta, 100);
        PrintStream ps = new PrintStream(taos);
        System.setOut(ps);
        System.setErr(ps);

        frame.add(new JScrollPane(ta));

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Test cases hardcoded in.
        File img1 = new File("images/Test3.jpg"); // Complex Geometric Shape - [1; rgb48]
        File img2 = new File("images/Test5.jpeg"); // Mona Lisa [1; rgb48]
        File img3 = new File("images/Test9.png"); // More Text [1; rgb192]
        File img4 = new File("images/Test14.jpeg"); // overdose sign [1; rgb48]
        File img5 = new File("images/Test16.jpeg"); // Brain
        File img6 = new File("images/Test21.png"); // Thermo Diagram (better quality)

        // SETTINGS FOR THE PICTURE. I might add in capabilities to change these.
        double pixelThresholdPercent = .01;
        int thickness = 5; // fairly static, not sure if I will change it
        double rgbSensitivityThreshold = 48; // just a default value, will be changed in the code
        BufferedImage image = null;

        Scanner scanner = new Scanner(System.in);

        // Selection menu for samples. Will work on ability to pass in images at will.
        while (true) {

            System.out.println("\nIf you are an advanced user who wants to modify MARKER THICKNESS or ");
            System.out.println("RGB SENSITIVITY, enter \"Y\". Enter anything else to continue as a normal user.");
            System.out.println("else to continue as a normal user.\n");

            String setting = getInput();

            if (setting.equals("Y") || setting.equals("y")) {
                advanced = true;
                System.out.print("\nAdvanced Mode Activated");

            } else {
                System.out.print("\nNormal Mode Activated");

            }

            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {

                }
                System.out.print(".");
            }

            System.out.println();

            System.out.println("\nLook through the menu and enter the number you'd like to print: \n");
            System.out.println("(1) Cool Geometric Shape");
            System.out.println("(2) Mona Lisa");
            System.out.println("(3) Text Sample");
            System.out.println("(4) Interesting Sign");
            System.out.println("(5) The Brain");
            System.out.println("(6) Thermodynamic Diagram\n");
            System.out.println("What would you like to print? Enter the number here: \n");

            boolean moveOn = false;
            boolean invalid = true;

            while (invalid) {

                String input = getInput();

                switch (input) {
                    case "1":
                        moveOn = true;
                        if (!advanced)
                            rgbSensitivityThreshold = 48;
                        image = ImageIO.read(img1);
                        invalid = false;
                        break;

                    case "2":
                        moveOn = true;
                        if (!advanced)
                            rgbSensitivityThreshold = 48;
                        image = ImageIO.read(img2);
                        invalid = false;
                        break;

                    case "3":
                        moveOn = true;
                        if (!advanced)
                            rgbSensitivityThreshold = 216;
                        image = ImageIO.read(img3);
                        invalid = false;
                        break;

                    case "4":
                        moveOn = true;
                        if (!advanced)
                            rgbSensitivityThreshold = 48;
                        image = ImageIO.read(img4);
                        invalid = false;
                        break;

                    case "5":
                        moveOn = true;
                        if (!advanced)
                            rgbSensitivityThreshold = 48;
                        image = ImageIO.read(img5);
                        invalid = false;
                        break;

                    case "6":
                        moveOn = true;
                        if (!advanced)
                            rgbSensitivityThreshold = 192;
                        image = ImageIO.read(img6);
                        invalid = false;
                        break;

                    default:
                        System.out.println("\nERROR: Please enter a number between 1 and 6!\n");
                        break;
                }
            }

            if (advanced) {

                System.out.println("\n\n[ADVANCED]");
                System.out.println("\nChoose the RGB SENSITIVITY. This is a number between 0 and 255");
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

                        rgbSensitivityThreshold = num;
                        System.out.println("\nRGB Sensitivity set to: " + rgbSensitivityThreshold + "\n");

                        for (int i = 0; i < 5; i++) {
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

                System.out.println("\n\n[ADVANCED]");
                System.out.println("\nChoose the MARKER THICKNESS. This is an ODD integer that controls");
                System.out.println("how thick the marker is, and thus controls how many pixels are");
                System.out.println("traversed by the marker, changing the path.\n");

                while (true) {
                    try {
                        String numString = getInput();
                        int num = Integer.parseInt(numString);

                        if (num % 2 != 1) {
                            System.out.println("\nPlease enter an ODD INTEGER (recommended value is 5)!\n");
                            continue;
                        }

                        thickness = num;
                        System.out.println("\nMarker Thickness set to: " + thickness + "\n");

                        for (int i = 0; i < 5; i++) {
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

            System.out.println("\nWould you like to receive coordinate data from the Arduino? Enter \"Y\"");
            System.out.println("to accept, anything else for no.\n");

            String receive = getInput();

            if (!receive.equals("Y") && !receive.equals("y")) {
                receiveData = false;
                System.out.println("\nCoordinate Data Deactivated.");
            } else {
                System.out.println("\nCoordinate Data Activated.");
            }

            if (moveOn) break;

        }

        System.out.println("\nNOTE: If the program doesn't work, there is probably a synchronization issue stemming" +
                " from issues with serial communication.");
        System.out.println("Simply try restarting communication by restarting the" +
                " program or reconnecting the hardware.");
        System.out.println("\nPress ENTER to continue.\n");
        getInput();
        System.out.println("Attemping Serial Communication\n");

        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }
            System.out.print(".");
        }
        System.out.println();

        // Find all possible ports
        String[] portNames = SerialPortList.getPortNames();

        if (portNames.length == 0) {
            System.out.println("\nERROR: There are no serial-ports! Make sure your connection is secure!");
            System.out.println("Press Enter to exit.");
            try {
                getInput();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // Block that generates the path
        Picture pic = new Picture(image, pixelThresholdPercent, rgbSensitivityThreshold);
        int subIslandPixelThreshold = pic.getPixelThreshold();
        PathGenerator pg = new PathGenerator(pic, thickness, subIslandPixelThreshold);
        Path path = pg.makePath();
        ArrayList<Path.Point<Picture.Pixel, Boolean>> pathList = path.getPath();

        // useful variables
        int pathLength = pathList.size();
        int pathIndex = 0;
        int picHeight = pic.getPicture().length;
        int picWidth = pic.getPicture()[0].length;
        double xPrime; // x' and y' are the adjusted coordinates for the starting position of the marker after centering
        double yPrime;
        double ipr; // inch-pixel ratio
        ArrayList<String> buffer = new ArrayList<>(); // the buffer object that will hold the path instructions

        // Calculating inch-pixel ratio and x' and y' (remember x and y are using matrix coordinates)
        if (((double)picWidth) / ((double)picHeight) < DRAW_WINDOW_WIDTH / DRAW_WINDOW_HEIGHT) {
            ipr = DRAW_WINDOW_HEIGHT / (double)picHeight;
            yPrime = ((DRAW_WINDOW_WIDTH - ipr * picWidth) / 2) / ipr;
            xPrime = ((DRAW_WINDOW_HEIGHT - ipr * picHeight) / 2) / ipr;
        } else {
            ipr = DRAW_WINDOW_WIDTH / (double)picWidth;
            yPrime = ((DRAW_WINDOW_WIDTH - ipr * picWidth) / 2) / ipr;
            xPrime = ((DRAW_WINDOW_HEIGHT - ipr * picHeight) / 2) / ipr;
        }

        String penString;

        // Filling the buffer - we store the buffer processor-side because there's a limit to the size
        // of the serial communication buffer for the Arduino (64 bytes)
        while (pathIndex < pathLength) {

            Path.Point<Picture.Pixel, Boolean> point = pathList.get(pathIndex);
            String toSend = "";

            if (pathList.get(pathIndex).getValue()) {
                penString = "d";
            } else {
                penString = "u";
            }

            if (pathIndex == 0) { // configuration block - ipr and x' and y'
                toSend = toSend + "q" + ipr + "\n";
                buffer.add(toSend);
                System.out.print("");
                toSend = "";
                toSend = toSend + "c" + (int)xPrime + "." + (int)yPrime + "." + penString + "\n";
                buffer.add(toSend);
                ready = false;
                System.out.print(""); // NEED THIS HERE TO RESOLVE A MULTITHREAD PROCESSING GLITCH

            } else if (pathIndex == pathLength - 1) {
                toSend = toSend + "p" + point.getKey().getX() + "." + point.getKey().getY() + "." + penString + "\n";
                buffer.add(toSend);
                System.out.print("");
                buffer.add("z");
                ready = false;
                System.out.print("");

            } else {
                toSend = toSend + "p" + point.getKey().getX() + "." + point.getKey().getY() + "." + penString + "\n";
                buffer.add(toSend);
                ready = false;
                System.out.print("");
            }

            pathIndex++;

        }

        /* DEBUGGING BLOCK
        for (int i = 0; i < portNames.length; i++) {
            System.out.println(portNames[i]);
        }
        */

        int index = 0; // index for the number of available ports
        int bufferIndex = 0; // index for the set of instructions in the buffer

        while (index < portNames.length) {
            try {

                // Finding valid port + serial communication settings
                serialPort = new SerialPort(portNames[index]);
                serialPort.openPort();
                serialPort.setParams(9600,8,1,0);
                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);
                serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);

                // If the program has reached this point, the port connection is almost certainly successful
                System.out.println("\n***** Port Connection Successful! *****");
                System.out.print("\n     SENDING DATA");

                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {

                    }
                    System.out.print(".");
                }
                System.out.println("\n");

                // waits until the Arduino is ready before sending data
                while (!ready) {
                    System.out.print(""); // syncs multithread processing
                }

                // communicates instructions one at a time to the Arduino. only communicates as quickly as the
                // the Arduino is ready to receive (that's the purpose of the ready variable).
                while(true) {
                    System.out.print("");
                    if (bufferIndex < buffer.size() && ready) {
                        ready = false;
                        serialPort.writeString(buffer.get(bufferIndex));
                        bufferIndex++;
                    }

                    if (done) break;
                }

                serialPort.closePort();

            } catch (SerialPortException e) {
                index++;
                if (index == portNames.length) {
                    System.out.println(e);
                }
            }

            System.out.println("\n***** Serial Communication Complete! *****");
            break;

        }

    }
}