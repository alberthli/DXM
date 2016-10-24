/**
 * Author: Albert Li
 * Start Date: 9/25/16
 *
 * Description: Takes in a raw image and prepares it for pen path processing
 *
 * Changelog:
 *      UPCOMING CHANGES -
 *              - Analyze picture to determine whether to invert colors (if more shading than empty space)
 *              - [IMPORTANT] Implement the capability to compress an image if it's too big or scale up an
 *                image if it's too small
 *      v1.0.1 - 10/22/16
 *          - Added an empty default constructor to instantiate Pixel objects outside of this class
 *      v1.0.0 - 10/4/16
 *          - NO CHANGES - MARKING FIRST RELEASE.
 *      v0.1.1 - 9/29/16
 *          - GENERAL CHANGES
 *              - Made Pixel threshold and RGB sensitivity final instance variables (Make this adjustable later)
 *          - [DEBUGGED] FIXES
 *              - Fixed error involving getRGB coordinate system vs matrix coordinate system (inconsistent
 *                x y : row col relation)
 *              - Fixed error that would invert black/white detection in images
 *              - Fixed constructor assignment error (calls out of order)
 *      v0.1.0 - 9/26/16
 *          - [INSPECTED][NOT DEBUGGED] Picture with capabilities to:
 *              - Retrieve array of pixels in it. Can return island of any pixel
 *              - Retrieve list of distinct islands in the picture
 *              - Indicate whether an arbitrary coordinate is in range of the picture
 */

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class Picture {

    //******************************//
    //***** INSTANCE VARIABLES *****//
    //******************************//

    private HashSet<Pixel> allPixels = new HashSet<>();
    private HashSet<Pixel> processed = new HashSet<>();
    private Pixel closest; // Pixel closest to the origin
    private Pixel[][] picture;
    private HashSet<Island> regions = new HashSet<>();
    private ArrayList<Island> notConsidered;
    private int rightBound;
    private int bottomBound;
    private static int count = 0; // TESTING ONLY
    private BufferedImage image;

    public static Pixel Pixel;

    // [NOTE] IMPORTANT: this number filters out islands which might register as stray marks.
    // The higher this number, the larger your islands have to be to be considered for
    // drawing by the pen.
    private int pixelThreshold;
    private double pixelThresholdPercent;
    private double rgbSensitivityThreshold; // NUMBER FROM 0-255. LOWER HAS HIGHER STANDARDS FOR MARKING.

    //******************************//
    //***** ENCAPSULATED CLASS *****//
    //******************************//

    public class Pixel {

        private int x;
        private int y;
        private int edgeFactor;
        private ArrayList<Pixel> adjacentPixels;
        private double distance;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getEdgeFactor() {
            return edgeFactor;
        }

        public ArrayList<Pixel> getAdjList() {
            return adjacentPixels;
        }

        public double getDistance() {
            return this.distance;
        }

        public void setEdgeFactor(int edgeFactor) {
            this.edgeFactor = edgeFactor;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public Pixel(int x, int y) {
            this.x = x;
            this.y = y;
            this.distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
            this.adjacentPixels = new ArrayList<>();
        }

        // Returns a list of all pixels adjacent to current Pixel p
        public void setAdjacentPixels() {

            int x = getX();
            int y = getY();

            // Top left
            if (inRange(x - 1, y - 1) && picture[x - 1][y - 1] != null) {
                adjacentPixels.add(picture[x - 1][y - 1]);
            }

            // Middle Left
            if (inRange(x, y - 1) && picture[x][y - 1] != null) {
                adjacentPixels.add(picture[x][y - 1]);
            }

            // Bottom Left
            if (inRange(x + 1, y - 1) && picture[x + 1][y - 1] != null) {
                adjacentPixels.add(picture[x + 1][y - 1]);
            }

            // Top Middle
            if (inRange(x - 1, y) && picture[x - 1][y] != null) {
                adjacentPixels.add(picture[x - 1][y]);
            }

            // Bottom Middle
            if (inRange(x + 1, y) && picture[x + 1][y] != null) {
                adjacentPixels.add(picture[x + 1][y]);
            }

            // Top Right
            if (inRange(x - 1, y + 1) && picture[x - 1][y + 1] != null) {
                adjacentPixels.add(picture[x - 1][y + 1]);
            }

            // Middle Right
            if (inRange(x, y + 1) && picture[x][y + 1] != null) {
                adjacentPixels.add(picture[x][y + 1]);
            }

            // Bottom Right
            if (inRange(x + 1, y + 1) && picture[x + 1][y + 1] != null) {
                adjacentPixels.add(picture[x + 1][y + 1]);
            }
        }

        // Returns the island that the pixel belongs to.
        // If it returns null, there's either an error OR
        // the pixel is no longer being considered for processing
        public Island getParentIsland() {
            for (Island island : regions) {
                if (island.getIsland().contains(this)) {
                    return island;
                }
            }

            return null; // only returns this if the Pixel has been discarded!
        }

        // Returns the squared distance between this Pixel and another
        public double sqDist(Pixel other) {
            return Math.pow((x - other.getX()), 2) + Math.pow((y - other.getY()), 2);
        }

        // Returns the squared distance between this Pixel a point
        public double sqDist(int currX, int currY) {
            return Math.pow((x - currX), 2) + Math.pow((y - currY), 2);
        }

    }

    public class Island {

        private HashSet<Pixel> island;
        private int boxUp;
        private int boxLeft;
        private int boxRight;
        private int boxDown;

        // initializes new island object
        public Island() {
            island = new HashSet<>();
            boxUp = Integer.MAX_VALUE;
            boxDown = Integer.MIN_VALUE;
            boxLeft = Integer.MAX_VALUE;
            boxRight = Integer.MIN_VALUE;
        }

        public HashSet<Pixel> getIsland() {
            return island;
        }

        public int getBoxUp() {
            return boxUp;
        }

        public int getBoxLeft() {
            return boxLeft;
        }

        public int getBoxRight() {
            return boxRight;
        }

        public int getBoxDown() {
            return boxDown;
        }

        public int size() {
            return island.size();
        }

        // As pixels are added, the bounding box of the island is recalculated
        public void addPixel(Pixel p) {
            island.add(p);

            if (p.getX() < boxUp) {
                boxUp = p.getX();
            }

            if (p.getX() > boxDown) {
                boxDown = p.getX();
            }

            if (p.getY() < boxLeft) {
                boxLeft = p.getY();
            }

            if (p.getY() > boxRight) {
                boxRight = p.getY();
            }

        }

    }

    //************************//
    //***** CONSTRUCTORS *****//
    //************************//

    // YOU SHOULD ALMOST NEVER CALL THIS CLASS. IT'S ONLY FOR INSTANTIATING PIXELS OUTSIDE OF THIS CLASS.
    public Picture() {

    }

    public Picture(BufferedImage image, double pixelThresholdPercent, double rgbSensitivityThreshold) {
        this.image = image;
        this.rgbSensitivityThreshold = rgbSensitivityThreshold;
        closest = null;
        imgToArray(image);

        notConsidered = new ArrayList<>();

        // By default, .01% of the image size is the threshold for discarding an island
        this.pixelThresholdPercent = pixelThresholdPercent;
        this.pixelThreshold = (int)(picture.length * picture[0].length * (pixelThresholdPercent / 100));

        getPixelGroups();
    }

    //***************************//
    //***** GETTERS/SETTERS *****//
    //***************************//

    public Pixel[][] getPicture() {
        return picture;
    }

    public HashSet<Island> getRegions() {
        return regions;
    }

    public int getRightBound() {
        return rightBound;
    }

    public int getBottomBound() {
        return bottomBound;
    }

    public ArrayList<Island> getNotConsidered() {
        return notConsidered;
    }

    public HashSet<Pixel> getAllPixels() {
        return allPixels;
    }

    public int getPixelThreshold() {
        return pixelThreshold;
    }

    public double getPixelThresholdPercent() {
        return pixelThresholdPercent;
    }

    public double getRgbSensitivityThreshold() {
        return rgbSensitivityThreshold;
    }

    public BufferedImage getImage() {
        return image;
    }

    //*******************//
    //***** METHODS *****//
    //*******************//

    // [DEBUGGED] Converts the image to a pixel array to use for pen path construction
    public void imgToArray(BufferedImage image) {

        picture = new Pixel[image.getHeight()][image.getWidth()];
        // Pixels can only have values LESS than the bounds (NOT INCLUSIVE)
        this.rightBound = picture[0].length;
        this.bottomBound = picture.length;

        // If the pixel is strong enough, a Pixel object is created. This is the first
        // level of filtration
        for (int x = 0; x < image.getHeight(); x++) {
            for (int y = 0; y < image.getWidth(); y++) {

                // RGB axes are switched from matrix axes
                Color c = new Color(image.getRGB(y, x));

                // [NOTE] IMPORTANT: Threshold values for this are pretty subjective.
                // Play around with them. I've currently set it at halfway b/t black and white
                if (c.getBlue() < rgbSensitivityThreshold ||
                        c.getRed() < rgbSensitivityThreshold ||
                        c.getGreen() < rgbSensitivityThreshold) {
                    Pixel p = new Pixel(x, y);
                    picture[x][y] = p;
                    allPixels.add(p);
                } else {
                    picture[x][y] = null;
                }

            }
        }

        // Iterates through all Pixels in the picture and sets their edge factor
        for (Pixel p : allPixels) {
            if (closest == null) {
                closest = p;
            } else if (closest.getDistance() > p.getDistance()) {
                closest = p;
            }
            p.setAdjacentPixels();
            p.setEdgeFactor(8 - p.getAdjList().size());
        }

    }

    // [DEBUGGED] Returns a list of sets of Pixels that are distinct, contiguous regions.
    public void getPixelGroups() {

        Queue<Pixel> q = new LinkedList<>();

        // Iterates through all pixels in the picture. Collects the pixels into "islands".
        for (Pixel p : allPixels) {
            if (!processed.contains(p)) {

                q.add(p);
                Island island = new Island();

                while (!q.isEmpty()) {

                    Pixel curr = q.poll();
                    island.addPixel(curr);
                    processed.add(curr);

                    for (Pixel neighbor : curr.getAdjList()) {
                        if (!processed.contains(neighbor) && !q.contains(neighbor)) {
                            q.add(neighbor);
                        }
                    }
                }
                regions.add(island);
            }
        }

        // Filters out islands that are too small to be considered. Uses the pixelThreshold variable.
        for (Island i : regions) {
            if (i.size() < this.pixelThreshold) {
                notConsidered.add(i);
            }
        }
        regions.removeAll(notConsidered);
    }

    // [DEBUGGED] Checks whether a coordinate is in range of the picture
    public boolean inRange(int x, int y) {

        if (x < 0 || x >= bottomBound) {
            return false;
        } else if (y < 0 || y >= rightBound) {
            return false;
        }

        return true;
    }

}
