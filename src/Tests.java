/**
 * Created by Albert on 9/29/16.
 *
 * Suite of tests. Mostly obsolete except quicker checks now that Animator is operable.
 *
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Tests {

    // Method used for printing matrix in table format. Change what it prints to check different visualizations
    public static void printPicture(Picture.Pixel[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for(int c = 0; c < grid[r].length; c++)
                if (grid[r][c] != null) {
                    if (grid[r][c].getEdgeFactor() > 0) {
                        System.out.print(grid[r][c].getEdgeFactor() + " ");
                    } else {
                        System.out.print(". ");
                    }
                } else {
                    System.out.print("  ");
                }
            System.out.println();
        }
    }

    // Method used to print a path in a readable format
    public static void printPath(Path path) {
        for (int i = 0; i < path.length(); i++) {
            System.out.print("(" + path.getPath().get(i).getKey().getX() + ", "
                    + path.getPath().get(i).getKey().getY() + ")"
                    + " | pen down: " + path.getPath().get(i).getValue());
            System.out.println();
        }
    }

    public static void printEdgeMap(Map<Picture.Pixel, Boolean> edgeMap) {
        for (Map.Entry<Picture.Pixel, Boolean> entry : edgeMap.entrySet()) {
            System.out.print("(" + entry.getKey().getX() + ", "
                    + entry.getKey().getY() + ")"
                    + " | Inside: " + entry.getValue());
            System.out.println();
        }
    }

    public static void main(String[] args) {

        // Image Files
        // [X; rgbY] means it passed the Xth pass of testing at an rgb sensitivity of Y. An asterisk
        // next to the X means it's a little slow.
        File img0 = new File("images/Test0.png"); // Single Line - [1; rgb128]
        File img1 = new File("images/Test1.jpeg"); // Single Longer Line - [1; rgb128]
        File img2 = new File("images/Test2.png"); // Three Fat Lines - [1; rgb128]
        File img3 = new File("images/Test3.jpg"); // Complex Geometric Shape - [1; rgb48]
        File img4 = new File("images/Test4.jpg"); // Detailed Leaf [1*; rgb48]
        File img5 = new File("images/Test5.jpeg"); // Mona Lisa [1; rgb48]
        File img6 = new File("images/Test6.jpeg"); // Starry Night [1; rgb48]
        File img7 = new File("images/Test7.jpeg"); // Smoking Skeleton [1**; rgb48]
        File img8 = new File("images/Test8.jpeg"); // Text with Lots of Artifacts [1; rgb48]
        File img9 = new File("images/Test9.png"); // More Text [1; rgb192]
        File img10 = new File("images/Test10.png"); // Blurry Text [1; rgb192]
        File img11 = new File("images/Test11.jpg"); // Drawings [1; rgb192]
        File img12 = new File("images/Test12.jpg"); // Funny Cat [1*; rgb192]
        File img13 = new File("images/Test13.jpeg"); // Tiger [1; rgb128]
        File img14 = new File("images/Test14.jpeg"); // overdose sign [1; rgb128]
        File img15 = new File("images/Test15.jpg"); // Pattern [1; rgb128]
        File img16 = new File("images/Test16.jpeg"); // Brain
        File img17 = new File("images/Test17.jpeg"); // Zebra
        File img18 = new File("images/Test18.jpeg"); // Old Person
        File img19 = new File("images/Test19.jpg"); // Michael Jordan
        File img20 = new File("images/Test20.jpg"); // Thermo Diagram
        File img21 = new File("images/Test21.png"); // Thermo Diagram (better quality)
        File img22 = new File("images/Test22.jpg"); // World Map
        File img23 = new File("images/Test23.jpg"); // Africa Map
        File img24 = new File("images/Test24.jpg"); // Asia Map
        File img25 = new File("images/Test25.png"); // World Map (Smaller)
        File img26 = new File("images/Test26.jpeg"); // Grid (lots of jpeg)
        File img27 = new File("images/Test27.jpg"); // Cool Swirl
        File img28 = new File("images/Test28.png"); // Cool Design
        File img29 = new File("images/Test29.jpg"); // Cool Design again
        File img30 = new File("images/Test30.jpg"); // Circle Design
        File img31 = new File("images/Test31.jpeg"); // Square Design
        File img32 = new File("images/Test32.jpg"); // Dickbutt

        File real1 = new File("images/Real1.jpg"); // Plug in wall
        File real2 = new File("images/Real2.jpg"); // Cardboard Box

        // Test Parameters:
        double pixelThresholdPercent = .01;
        double rgbSensitivityThreshold = 192;

        // If you want to do all the tests on the same image, change this parameter. Otherwise,
        // manually change all the tests below.
        File applyToAllTests = img19;

        //************************ TESTS FOR PICTURE.JAVA *************************//

        // [TEST 1] Checks whether Pixel array is created correctly
        // -SEEMS TO WORK WELL FOR MANY DIFFERENT SHAPES - TESTED EDGE FACTOR TOO.

        BufferedImage imagePrintTest;

        try {
            imagePrintTest = ImageIO.read(applyToAllTests);
            Picture p = new Picture(imagePrintTest, pixelThresholdPercent, rgbSensitivityThreshold);
            printPicture(p.getPicture());
            System.out.println();
            System.out.println("************************ TESTS FOR PICTURE.JAVA *************************");
            System.out.println();
            System.out.println("[ABOVE: TEST 1. IMAGE PRINTING TEST.]");
            System.out.println();
            System.out.println("RGB SENSITIVITY PARAMETER (0.0-255.0): " + p.getRgbSensitivityThreshold());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // [TEST 2] Checks whether islands are formed and filtered correctly
        // -WORKS WITH 3 LINES (REGIONS = 3)
        // -FIXED ASSIGNMENT ERROR - CONSTRUCTOR CALLS OUT OF ORDER

        BufferedImage imageRegionTest;

        try {

            imageRegionTest = ImageIO.read(applyToAllTests);
            Picture p = new Picture(imageRegionTest, pixelThresholdPercent, rgbSensitivityThreshold);

            System.out.println();
            System.out.println();
            System.out.println("[BELOW: TEST 2. ISLAND FILTRATION TEST.]");
            System.out.println();

            System.out.println("Total Pixels In Picture: " + p.getPicture().length * p.getPicture()[0].length);
            System.out.println("Total Pixels Selected: " + p.getAllPixels().size());
            System.out.println("Pixel Threshold (Island Size Ceiling) [" +
                    p.getPixelThresholdPercent() + "%]: " + p.getPixelThreshold());
            System.out.println("Total Islands: " + (p.getRegions().size() + p.getNotConsidered().size()));
            System.out.println();

            System.out.println("ISLANDS DISCARDED: " + p.getNotConsidered().size());
            for (Picture.Island i : p.getNotConsidered()) {
                System.out.println("Island Size: " + i.size());
            }
            System.out.println();

            System.out.println("ISLANDS FOR PRINTING: " + p.getRegions().size());
            for (Picture.Island i : p.getRegions()) {
                System.out.println("Island Size: " + i.size());
            }
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //************************ TESTS FOR PATH.JAVA *************************//

        try {

            imagePrintTest = ImageIO.read(applyToAllTests);
            Picture p = new Picture(imagePrintTest, pixelThresholdPercent, rgbSensitivityThreshold);
            Path path = new Path();

            // Test Pixels
            Picture.Pixel pix1 = p.new Pixel(1, 1);
            Picture.Pixel pix2 = p.new Pixel(2, 2);
            Picture.Pixel pix3 = p.new Pixel(3, 3);
            Picture.Pixel pix4 = p.new Pixel(4, 4);
            Picture.Pixel pix5 = p.new Pixel(5, 5);
            Picture.Pixel pix6 = p.new Pixel(6, 6);
            Picture.Pixel pix7 = p.new Pixel(1, 1);

            System.out.println("************************ TESTS FOR PATH.JAVA *************************");
            System.out.println();
            System.out.println();
            System.out.println("[BELOW: TEST 3. CREATING PATH WITH 2 PIXELS.]");
            System.out.println();
            path.addPoint(pix1, false, 0);
            path.addPoint(pix2, true, 0);
            printPath(path);
            System.out.println("Path Length: " + path.length());
            System.out.println();
            System.out.println();

            System.out.println("[BELOW: TEST 4. ADDING 2 MORE PIXELS INTO EXISTING PATH.]");
            System.out.println();
            path.addPoint(pix3, true, 0);
            path.addPoint(pix4, true, 0);
            printPath(path);
            System.out.println("Path Length: " + path.length());
            System.out.println();
            System.out.println();

            System.out.println("[BELOW: TEST 5. CHANGING 2 PIXELS IN PATH]");
            System.out.println();
            path.changePath(pix2, pix5);
            path.changePath(pix3, pix6);
            printPath(path);
            System.out.println("Path Length: " + path.length());
            System.out.println();
            System.out.println();

            System.out.println("[BELOW: TEST 6. TESTING PIXEL MATCH FUNCTION.]");
            System.out.println();
            System.out.println("Testing same point object - ");
            System.out.println("(" + path.getMatchingPoint(pix1).getKey().getX() + ", "
                    + path.getMatchingPoint(pix1).getKey().getY() + ")"
                    + " | pen down: " + path.getMatchingPoint(pix1).getValue());
            System.out.println();
            System.out.println("Testing different point object with same coordinates - ");
            System.out.println("(" + path.getMatchingPoint(pix7).getKey().getX() + ", "
                    + path.getMatchingPoint(pix7).getKey().getY() + ")"
                    + " | pen down: " + path.getMatchingPoint(pix7).getValue());
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //************************ TESTS FOR PATHGENERATOR.JAVA *************************//

        BufferedImage imagePathTest;

        try {

            System.out.println("************************ TESTS FOR PATHGENERATOR.JAVA *************************");
            System.out.println();
            System.out.println();

            // PathGenerator Parameters
            imagePathTest = ImageIO.read(applyToAllTests);
            Picture pic = new Picture(imagePathTest, pixelThresholdPercent, rgbSensitivityThreshold);
            int thickness = 7;
            int subIslandPixelThreshold = pic.getPixelThreshold();

            // PathGenerator Object
            PathGenerator pg = new PathGenerator(pic, thickness, subIslandPixelThreshold);

            System.out.println("[BELOW: TEST 7. PATHGENERATOR CONSTRUCTOR TEST.]");
            /*
            System.out.println();
            System.out.println("Cursor Position: (" + pg.getCursorX() +
                    "," + pg.getCursorY() + ")");
            System.out.println("Thickness: " + pg.getThickness());
            System.out.println("Initial Size of \"traversed\" Set: " + pg.getTraversed().size());
            System.out.println("Sub-island Pixel Threshold: " + pg.getSubIslandPixelThreshold());
            System.out.println("Initial Number of Islands: " + pg.getIslandsLeft().size());
            System.out.println();
            */

            // This is huge, comment out if you don't want to do an inspection
            /*
            System.out.println("Printed EdgeMap:");
            System.out.println();
            printEdgeMap(pg.getEdgeMap());
            System.out.println();
            */

            System.out.println("EdgeMap Size (Equal to Sum of Island Sizes): " + pg.getEdgeMap().size());
            System.out.println();
            System.out.println();

            // [NOTE] If you actually look at the test results, you will notice that the distances on average trend up
            // as more and more Pixels are processed, but that sometimes it will dip lower than Pixels previously
            // processed. That's because I'm approximating ROUND shapes as SQUARES/RECTANGLES, which might not seem
            // like the best idea, but makes the coding much easier because I don't have to keep track of universal
            // spatial coordinates - this is much more data efficient. I'm sure there's a way to improve it though.
            System.out.println("[BELOW: TEST 8. CLOSEST UNPROCESSED PIXEL TEST.]");
            System.out.println();

            /*
            Picture.Pixel pixelTest = pg.getClosestUnprocessedPixelGlobal();

            System.out.println("PIXELS NUMBERED WITH COORDINATES AND DISTANCE:");
            System.out.println();
            int count8 = 1;
            while (pixelTest != null) {
                System.out.println(count8++ + ": (" + pixelTest.getX() + ", " + pixelTest.getY() + ")" +
                        "| Distance: " + pixelTest.getDistance());
                pg.getTraversed().add(pixelTest);
                pixelTest = pg.getClosestUnprocessedPixelGlobal();
            }

            pg.getTraversed().clear();
            System.out.println();
            System.out.println();
            */

            // [BUG] Something is bleeding Pixels here. Not sure what.
            System.out.println("[BELOW: TEST 9. NEXT IN ISLAND TEST.]");
            System.out.println();

            int count9 = 1;
            int count91 = 1;

            // [NOTE] THIS TAKES AN OBSCENE AMOUNT OF TIME BECAUSE OF INEFFICIENCIES IN THE FILTERING OF
            // SUBISLANDS. GOTTA FIX THIS SOON. HOWEVER, IT IS FUNCTIONAL!
            // - FOR MONA LISA CASE MISSING A FEW HUNDRED PIXELS

            /*
            pg.setCursorX(pg.getClosestUnprocessedPixelGlobal().getX());
            pg.setCursorY(pg.getClosestUnprocessedPixelGlobal().getY());

            for (Picture.Island i : pg.getPic().getRegions()) {
                Picture.Pixel p = pg.nextInIsland(i);

                System.out.println();
                System.out.println("PIXELS IN ISLAND #" + count91++);
                System.out.println();

                while (p != null) {
                    System.out.println(count9++ + ": (" + p.getX() + ", " + p.getY() + ")");
                    pg.getTraversed().add(p);
                    p = pg.nextInIsland(i);
                }
            }
            pg.getTraversed.clear();
            */

            System.out.println("[BELOW: TEST 10. PATH GENERATION.]");
            System.out.println();

            Path path = pg.makePath();
            printPath(path);
            System.out.println("Length of Path: " + path.length());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
