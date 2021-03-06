/**
 * Author: Albert Li
 * Start Date: 10/8/16
 *
 * Description: Handles the conversion of Java-produced information into Arduino-readable and executable code.
 *
 * Changelog:
 *      UPCOMING CHANGES -
 *
 *      11/9/16 and on -
 *          Most of the code is functional now, so I will mostly be posting small updates via commits on GitHub.
 *          Only major changes will be reflected hRere.
 *
 *      v2.0.0 - 10/9/16
 *          - Changed out physical assembly entirely. Added ability to insert the board.
 *
 *      v1.1.0 - 10/25/16
 *          - Added in ability to calibrate marker position before printing begins
 *          - Changed menu slightly
 *          - Fixed initial values for marker up/down positioning
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
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import jssc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

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
    private static boolean freeDraw = false;
    private static boolean inFreeDraw = false;
    private static boolean isUp = false;

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
                        } else if (receivingMessage == true) {
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
                            } else {
                                message.append((char)b);
                            }
                        }

                    }

                } catch (SerialPortException ex) {
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
                try {
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        serialPort.writeString("d");
                        //System.out.println("dg\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        serialPort.writeString("a");
                        //System.out.println("ag\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        serialPort.writeString("w");
                        //System.out.println("wg\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        serialPort.writeString("s");
                        //System.out.println("sg\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        pass = true;
                    }

                    if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                        if(freeDraw) {
                            if (isUp) {
                                serialPort.writeString("j");

                            } else {
                                serialPort.writeString("y");

                            }

                            isUp = !isUp;
                        }
                    }

                    if(e.getKeyCode() == KeyEvent.VK_Q) {
                        if (freeDraw)
                            inFreeDraw = false;
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            public void keyPressed(KeyEvent e) {
                try {
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        serialPort.writeString("d");
                        //System.out.println("dg\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        serialPort.writeString("a");
                        //System.out.println("ag\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        serialPort.writeString("w");
                        //System.out.println("wg\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        serialPort.writeString("s");
                        //System.out.println("sg\n");
                    }

                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        pass = true;
                    }

                    if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                        if(freeDraw) {
                            if (isUp) {
                                serialPort.writeString("j");

                            } else {
                                serialPort.writeString("y");

                            }

                            isUp = !isUp;
                        }
                    }

                    if(e.getKeyCode() == KeyEvent.VK_Q) {
                        if (freeDraw)
                            inFreeDraw = false;
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
        DefaultCaret caret = (DefaultCaret)ta.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

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
        File img7 = new File("images/Test2.png"); // Three Lines

        // DEFAULT SETTINGS FOR THE PICTURE.
        double pixelThresholdPercent = .01;
        int thickness = 5; // fairly static, not sure if I will change it
        double rgbSensitivityThreshold = 48; // just a default value, will be changed in the code
        BufferedImage image = null;

        // Selection menu for samples. Will work on ability to pass in images at will.
        while (true) {

            System.out.println("Welcome to the MarkerBot Control Interface!");

            System.out.println("\nTo enter inputs, type what you want to send and press ENTER afterwards.");

            System.out.println("\nIf you are an advanced user who wants to modify MARKER THICKNESS or ");
            System.out.println("RGB SENSITIVITY, enter \"Y\". Enter anything else to continue as a normal user.\n");

            String setting = getInput();

            if (setting.equals("Y") || setting.equals("y")) {
                advanced = true;
                System.out.print("\nAdvanced Mode Activated");

            } else {
                System.out.print("Normal Mode Activated");

            }

            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {

                }
                System.out.print(".");
            }
            System.out.println();
            System.out.println("________________________________________________________");
            System.out.println();

            System.out.println("\nLook through the menu and enter the number you'd like to print: \n");
            System.out.println("(1) Cool Geometric Shape");
            System.out.println("(2) Mona Lisa");
            System.out.println("(3) Text Sample");
            System.out.println("(4) Interesting Sign");
            System.out.println("(5) The Brain");
            System.out.println("(6) Thermodynamic Diagram");
            System.out.println("(7) Three Lines");
            System.out.println("(8) Free Draw!\n");
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

                    case "7":
                        moveOn = true;
                        if (!advanced)
                            rgbSensitivityThreshold = 192;
                        image = ImageIO.read(img7);
                        invalid = false;
                        break;

                    case "8":
                        moveOn = true;
                        freeDraw = true;
                        inFreeDraw = true;
                        invalid = false;
                        break;

                    default:
                        System.out.println("\nERROR: Please enter a number between 1 and 8!\n");
                        break;
                }
            }

            if (advanced && !freeDraw) {

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

                        rgbSensitivityThreshold = num;
                        System.out.println("\nRGB Sensitivity set to: " + rgbSensitivityThreshold + "\n");

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

                        thickness = num;
                        System.out.println("\nMarker Thickness set to: " + thickness + "\n");

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

            if (!freeDraw) {
                System.out.println();
                System.out.println("________________________________________________________");
                System.out.println();
                System.out.println("Would you like to receive coordinate data from the Arduino? Enter \"Y\"");
                System.out.println("to accept, anything else for no.\n");

                String receive = getInput();

                if (!receive.equals("Y") && !receive.equals("y")) {
                    receiveData = false;
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

            }

            if (moveOn) break;

        }

        // Find all possible ports
        String[] portNames = SerialPortList.getPortNames();

        if(portNames.length == 0) {
            System.out.println("\nERROR: There are no serial ports! Make sure your connection is secure!");
            System.out.println("Press Enter to exit.");
            try {
                getInput();
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        ArrayList<String> buffer = new ArrayList<>(); // the buffer object that will hold the path instructions

        if(!freeDraw) {
            System.out.print("\nPROCESSING DATA! PLEASE BE PATIENT");
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {

                }
                System.out.print(".");
            }
            System.out.println("\n");


            // Block that generates the path
            Picture pic = new Picture(image, pixelThresholdPercent, rgbSensitivityThreshold);
            int subIslandPixelThreshold = pic.getPixelThreshold();
            PathGenerator pg = new PathGenerator(pic, thickness, subIslandPixelThreshold);
            Path path = pg.makePath();
            ArrayList<Path.Point<Picture.Pixel, Boolean>> pathList = path.getPath();

            // useful variables
            int pathLength = pathList.size();
            int pathIndex = 0;
            double xPrime = pg.getxPrime(); // x' and y' are the adjusted coordinates for the starting position of the marker after centering
            double yPrime = pg.getyPrime();
            double ipr = pg.getIpr(); // inch-pixel ratio

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
                    toSend = toSend + "c" + (int) xPrime + "." + (int) yPrime + "." + point.getTime() + "\n";
                    buffer.add(toSend);
                    // ready = false;
                    System.out.print(""); // NEED THIS HERE TO RESOLVE A MULTITHREAD PROCESSING GLITCH

                } else if (pathIndex == pathLength - 1) {
                    toSend = toSend + "p" + point.getKey().getX() + "." + point.getKey().getY() + "." + penString + "." + point.getTime() + "\n";
                    buffer.add(toSend);
                    System.out.print("");
                    buffer.add("z");
                    // ready = false;
                    System.out.print("");

                } else {
                    toSend = toSend + "p" + point.getKey().getX() + "." + point.getKey().getY() + "." + penString + "." + point.getTime() + "\n";
                    buffer.add(toSend);
                    // ready = false;
                    System.out.print("");
                }

                pathIndex++;

            }

            /* DEBUGGING BLOCK
            for (int i = 0; i < portNames.length; i++) {
                System.out.println(portNames[i]);
            }
            */

            System.out.println("PROCESSING COMPLETE!\n");

        }

        int index = 0; // index for the number of available ports
        int bufferIndex = 0; // index for the set of instructions in the buffer

        while (index < portNames.length) {
            try {

                System.out.println("Attemping Serial Communication...\n");

                // Finding valid port + serial communication settings
                serialPort = new SerialPort(portNames[index]);
                serialPort.openPort();
                serialPort.setParams(9600,8,1,0);
                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);
                serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
                serialPort.writeString("y");

                // If the program has reached this point, the port connection is almost certainly successful
                System.out.println("Port Connection Successful!");

                // waits until the Arduino is ready before sending data
                while(!ready) {
                    System.out.print(""); // syncs multithread processing
                }

                System.out.println("________________________________________________________");
                System.out.println();
                System.out.println("Do you need to remove the marker? Enter \"Y\" to eject the marker,");
                System.out.println("anything else to ignore.\n");

                String receive = getInput();

                if(receive.equals("Y") || receive.equals("y")) {

                    serialPort.writeString("k");
                    receive = "";

                    System.out.println("The marker has been ejected. To reinsert, GENTLY rest it on the rails");
                    System.out.println("and enter \"Y\".\n");

                    while(!receive.equals("y") && !receive.equals("Y")) {
                        receive = getInput();
                    }

                    serialPort.writeString("y");

                }

                receive = "";

                System.out.println("________________________________________________________");
                System.out.println();
                System.out.println("Do you need to remove/insert the board? Enter \"Y\" to do so,");
                System.out.println("anything else to ignore.\n");

                receive = getInput();

                if(receive.equals("Y") || receive.equals("y")) {

                    receive = "";

                    System.out.println("The printer is ready for insertion. Place the board near the servo");
                    System.out.println("and enter \"Y\".\n");

                    while(!receive.equals("y") && !receive.equals("Y")) {
                        receive = getInput();
                    }

                    serialPort.writeString("b");

                    System.out.println("________________________________________________________");
                    System.out.println();
                    System.out.println("The board has been inserted!");

                }

                if(!freeDraw) {
                    System.out.println("________________________________________________________");
                    System.out.println();
                    System.out.println("[IMPORTANT]");
                    System.out.println("Calibrate the marker's origin using the ARROW KEYS if needed. Try to place the marker");
                    System.out.println("about 1 inch from each of the sides.");
                    System.out.println("\n*** DO NOT HOLD DOWN THE KEYS! LIGHTLY TAP THEM OR THE PRINTER WILL STOP WORKING. ***");
                    System.out.println("If this happens, simply restart the printer.");
                    System.out.println("\nPress ENTER to continue.");
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
                }

                System.out.println("________________________________________________________");
                System.out.println();
                System.out.println("NOTE: If the program doesn't work, there is probably a synchronization issue stemming" +
                        " from issues with serial communication.");
                System.out.println("Simply try restarting communication by restarting the" +
                        " program or reconnecting the hardware.");
                System.out.println("\n*** IF YOU SEE THE SERVOS STALLING UNPLUG TO AVOID DAMAGE TO THE ELECTRONICS! ***");
                System.out.println("\nPress ENTER to begin printing.\n");
                getInput();

                if(!freeDraw) {
                    System.out.println();
                    System.out.println("________________________________________________________");
                    System.out.println();
                    System.out.println("***** Printing... *****\n");

                    // communicates instructions one at a time to the Arduino. only communicates as quickly as the
                    // the Arduino is ready to receive (that's the purpose of the ready variable).

                    ta.setCaretPosition(ta.getDocument().getLength());

                    while (true) {
                        System.out.print("");
                        if (bufferIndex < buffer.size() && ready) {
                            ready = false;
                            serialPort.writeString(buffer.get(bufferIndex));
                            bufferIndex++;
                        }
                        if (done) break;
                    }
                }

                if(freeDraw) {

                    System.out.println();
                    System.out.println("________________________________________________________");
                    System.out.println();
                    System.out.println("Free Draw Mode has been activated! Draw whatever you want on the board.");
                    System.out.println("Use the arrow keys to move along the X and Y axes and use SHIFT to toggle the");
                    System.out.println("marker being up or down. Press Q to quit.\n");

                    ta.addKeyListener(kl);

                    while(inFreeDraw) {
                        int len = ta.getDocument().getLength();
                        ta.setCaretPosition(len);
                    }

                    System.out.println();
                    System.out.println("________________________________________________________");
                    System.out.println();
                    System.out.println("Free Draw Mode Deactivated!\n");

                    try {
                        Thread.sleep(1500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    ta.removeKeyListener(kl);

                }

                serialPort.closePort();

            } catch (SerialPortException e) {
                index++;
                if (index == portNames.length) {
                    System.out.println(e);
                }
            }

            System.out.println();
            System.out.println("________________________________________________________");
            System.out.println();
            System.out.println("\nSerial Communication Complete! Enjoy your print!");
            System.out.println("This window will close in 5 seconds.");

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));

            break;

        }

    }
}