/**
 * Author: Albert Li
 * Start Date: 10/8/16
 *
 * Description: Handles the conversion of Java-produced information into Arduino-readable and executable code.
 *
 * Changelog:
 *      To-Do:
 *          - Figure out how to set up circuit to receive different commands and perhaps light up different LEDs
 *            depending on what path commands are being sent.
 *          - Figure out how to correctly time the signal communication so that the timing of path-processing is
 *            correct. In other words, how to make sure you wait until a sub-path is completed before sending the
 *            instructions to move to the next path.
 *      v0.2.0 - 10/23/16
 *          - [NOT DEBUGGED] Implemented ability to send coded buffered Strings to the Arduino for processing
 *      v0.1.0 - 10/22/16
 *          - Implemented ability to correctly parse serial data FROM the arduino. This will allow the program to
 *            know when to send in new data
 *      v0.0.1 - 10/8/16
 *          - Set up simple test case with LED to confirm that JSSC library is functional
 *
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import jssc.*;

import javax.imageio.ImageIO;

public class ArduinoCommunicator {

    private static SerialPort serialPort;
    private static boolean ready;
    private static final double DRAW_WINDOW_WIDTH = 12; // inches
    private static final double DRAW_WINDOW_HEIGHT = 9; // inches

    // Reads incoming messages from the Arduino
    private static class PortReader implements SerialPortEventListener {

        // Fixes a "Toolkit not initialized" error
        JFXPanel p = new JFXPanel();

        // String Reception
        StringBuilder message = new StringBuilder();
        Boolean receivingMessage = false;

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
                                        if (toProcess.equals("ready")) {
                                            ready = true;
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

    public static void main(String[] args) throws InterruptedException, IOException {

        File img1 = new File("images/Test3.jpg"); // Complex Geometric Shape - [1; rgb48]
        File img2 = new File("images/Test5.jpeg"); // Mona Lisa [1; rgb48]
        File img3 = new File("images/Test9.png"); // More Text [1; rgb192]
        File img4 = new File("images/Test14.jpeg"); // overdose sign [1; rgb48]
        File img5 = new File("images/Test16.jpeg"); // Brain
        File img6 = new File("images/Test21.png"); // Thermo Diagram (better quality)

        double pixelThresholdPercent = .01;
        int thickness = 5; // fairly static, not sure if I will change it
        double rgbSensitivityThreshold = 48; // just a default value, will be changed in the code
        BufferedImage image = null;

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.println("\nLook through the menu and enter the number you'd like to print: \n");
            System.out.println("(1) Cool Geometric Shape");
            System.out.println("(2) Mona Lisa");
            System.out.println("(3) Text Sample");
            System.out.println("(4) Interesting Sign");
            System.out.println("(5) The Brain");
            System.out.println("(6) Thermodynamic Diagram\n");
            System.out.println("What would you like to print? Enter the number here: ");

            String input = scanner.nextLine();
            boolean moveOn = false;

            switch(input) {
                case "1":
                    moveOn = true;
                    rgbSensitivityThreshold = 48;
                    image = ImageIO.read(img1);
                    break;

                case "2":
                    moveOn = true;
                    rgbSensitivityThreshold = 48;
                    image = ImageIO.read(img2);
                    break;

                case "3":
                    moveOn = true;
                    rgbSensitivityThreshold = 216;
                    image = ImageIO.read(img3);
                    break;

                case "4":
                    moveOn = true;
                    rgbSensitivityThreshold = 48;
                    image = ImageIO.read(img4);
                    break;

                case "5":
                    moveOn = true;
                    rgbSensitivityThreshold = 48;
                    image = ImageIO.read(img5);
                    break;

                case "6":
                    moveOn = true;
                    rgbSensitivityThreshold = 192;
                    image = ImageIO.read(img6);
                    break;

                default:
                    System.out.println("\nERROR: Please enter a number between 1 and 6!\n");
                    break;
            }

            if (moveOn) break;

        }

        // Find all possible ports
        String[] portNames = SerialPortList.getPortNames();

        if (portNames.length == 0) {
            System.out.println("\nERROR: There are no serial-ports! Make sure your connection is secure!");
            System.out.println("Press Enter to exit...\n");
            try {
                System.in.read();
            } catch (IOException e) {
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
        int pathLength = pathList.size();
        int pathIndex = 0;

        int picHeight = pic.getPicture().length;
        int picWidth = pic.getPicture()[0].length;
        double xPrime; // x' and y' are the adjusted coordinates for the starting position of the marker after centering
        double yPrime;
        double ipr;

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

        /* DEBUGGING
        for (int i = 0; i < portNames.length; i++) {
            System.out.println(portNames[i]);
        }
        */

        int index = 0;

        while (index < portNames.length) {
            try {
                // Finding valid port
                serialPort = new SerialPort(portNames[index]);
                serialPort.openPort();
                serialPort.setParams(9600,8,1,0);
                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
                        SerialPort.FLOWCONTROL_RTSCTS_OUT);
                serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
                System.out.println("\n***** Port Connection Successful! *****");
                System.out.println("\n     SENDING DATA...\n");
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Operations
                while(true) {
                    if (pathIndex < pathLength) {

                        Path.Point<Picture.Pixel, Boolean> point = pathList.get(pathIndex);
                        String toSend = "";

                        if (pathIndex == 0) { // configuration block - ipr and x' and y'
                            toSend = toSend + "q" + ipr + "\n";
                            serialPort.writeString(toSend);
                            // System.out.println(toSend); // Debug
                            toSend = "";
                            toSend = toSend + "c" + (int)xPrime + "." + (int)yPrime + "\n";
                            serialPort.writeString(toSend);
                            // System.out.println(toSend); // Debug

                        } else if (pathIndex == pathLength - 1) {
                            toSend = toSend + "p" + point.getKey().getX() + "." + point.getKey().getY() + "\n";
                            serialPort.writeString(toSend);
                            // System.out.println(toSend); // Debug
                            toSend = "";
                            toSend = toSend + "p" + (-(int)xPrime) + "." + (-(int)yPrime) + "\n";
                            serialPort.writeString(toSend);
                            // System.out.println(toSend); // Debug

                        } else {
                            toSend = toSend + "p" + point.getKey().getX() + "." + point.getKey().getY() + "\n";
                            serialPort.writeString(toSend);
                            // System.out.println(toSend); // Debug
                        }

                        pathIndex++;

                    } else {
                        serialPort.closePort();
                        break;
                    }
                }

            } catch (SerialPortException e) {
                index++;
                if (index == portNames.length) {
                    System.out.println(e);
                }
            }

            System.out.println("***** Serial Communication Complete! *****");
            break;

        }

    }
}
