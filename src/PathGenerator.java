/**
 * Author: Albert Li
 * Start Date: 9/25/16
 *
 * Description: Takes in a Picture object and has the capabilities to generate a Path object.
 *
 * Changelog:
 *      UPCOMING CHANGES -
 *              - [] Add more tiebreaking capabilities for island processing!
 *              - [-] Implement smart path editing
 *                  - [X] Move points IN if they experience a lot of overflow. Use edge PUSH indicators.
 *                  - [] Move points OUT if that would not reduce existing coverage and it would cover
 *                       non-traversed Pixels (NOTE: this means NOT putting discarded Pixels in traversed)
 *                       without a lot of increase in overflow.
 *              - Come up with way to use true Euclidean distance instead of square approximation (maybe)
 *              - MUST improve the filterSubIsland method performance! For large subislands or single large islands
 *                this method takes an obscene amount of time to check the subisland.
 *      v1.0.3 - 10/22/16
 *          - ADDED
 *              - Origin is now always the last point in path
 *      v1.0.2 - 10/11/16
 *          - FIXED
 *              - More accurate push factor value found by taking into account larger marker thickness
 *      v1.0.1 - 10/7/16
 *          - FIXED
 *              - Bug causing marker thickness above 5 to run in an infinite loop
 *      v1.0.0 - 10/4/16 - FIRST WORKING RELEASE OF ALGORITHM
 *          - FIXED
 *              - Bug in reduceOverflow causing lines to be incorrectly redrawn.
 *          - [BUG] Stops processing somewhere for thickness above 3 and 5!!!
 *      v0.3.1 - 10/3/16
 *          - FIXED
 *              - Massive for loop bug causing the mark(int x, int y) and hypothetical marking methods
 *                    to fail.
 *              - Error involving switching the X and Y coordinates for bounding box creation
 *              - Null Pointer Exception for non-traversed islands accidentally traversed completely by
 *                    marker thickness on independent other islands (while loop would not terminate correctly)
 *              - Error involving array out of bounds for path-fixing algorithm (added inrange condition)
 *              - Method calculating Pixel adjacency not having correct range
 *              - Error involving miscalculation of the penDown parameter for path creation
 *              - Mixed up x and y values for adjacency tests (inside() and adjacentToMarker())
 *      v0.3.0 - 9/29/16
 *          - BUGS
 *              - Processing in island takes a long time for some reason
 *              - makePath() has an infinite loop, probably caused by "traversed" perpetually remaining at
 *                size 0. Not sure why this is.
 *          - GENERAL CHANGES
 *              - Changed the order of methods in file to reflect order they should be called
 *          - FIXED
 *              - Termination condition for getClosestUnprocessedPixelGlobal()
 *              - Fixed EXTREMELY long time for filterSubIsland(). Still takes a long time!
 *          - [NOT DEBUGGED] Added capabilities to:
 *              - Mark traversed pixels that lie between two points
 *              - Traverse islands by edge first, then fill in the middle
 *      v0.2.0 - 9/27/16
 *          - [NOT DEBUGGED] Added capabilities to:
 *              - Tiebreak based on hypothetical pixels marked
 *              - Determine whether a Pixel is "close to edge" as a function of thickness. This value
 *                is stored in a Map as a boolean
 *              - Determine from a Pixel or any point how much "overflow" there is when a marker marks there
 *              - Return a Pixel based on Push Factor analysis for path correction
 *              - Determine what Pixels are contained in any given marker active area based on coordinates
 *              - Determine from a set of Pixels which ones are traversed by Pixels other than a specified one
 *              - Clean path by reducing overflow using many of the above new capabilities
 *          - Fixed:
 *              - Fixed some parameter inputs to better make use of instance variables
 *      v0.1.0 - 9/26/16
 *          - [NOT DEBUGGED] PathGenerator with capabilities to:
 *              - Set cursor position
 *              - Set marker thickness (square value)
 *              - Get the closest unprocessed pixel from any arbitrary point in a picture
 *              - Get the closest unprocessed pixel in the island currently being processed
 *              - Determine the size of a sub-island of a pixel during island processing (for comparison)
 *              - Determine the next Pixel to go to in an island, accounting for whether or not the
 *                next Pixel is adjacent or not
 *              - Mark a Pixel that has been added to path
 *              - Determine the hypothetical number of pixels that would be newly processed if the
 *                marker were to move there (for comparison)
 *              - Get a list of unprocessed Pixels adjacent to the current marker position
 *              - Filter subislands during island processing which become too small for consideration
 *              - Determine whether two pixels are adjacent
 *              - Generate a path based on pixel edge factor
 */

import java.util.*;

public class PathGenerator {

    //******************************//
    //***** INSTANCE VARIABLES *****//
    //******************************//

    private Picture pic;
    private int cursorX;
    private int cursorY;
    private int thickness; // should be an ODD number
    private HashSet<Picture.Pixel> traversed;
    private HashSet<Picture.Island> islandsLeft;
    private int subIslandPixelThreshold;
    private Map<Picture.Pixel, Boolean> edgeMap;
    private HashSet<Picture.Pixel> addedToSubisland;
    private int count = 0; // for debugging only

    //************************//
    //***** CONSTRUCTORS *****//
    //************************//

    public PathGenerator(Picture pic, int thickness, int subIslandPixelThreshold) {
        this.pic = pic;
        this.cursorX = 0;
        this.cursorY = 0;
        this.thickness = thickness;
        this.traversed = new HashSet<>();
        this.addedToSubisland = new HashSet<>();
        this.subIslandPixelThreshold = subIslandPixelThreshold;
        islandsLeft = new HashSet<>();
        edgeMap = new HashMap<>();
        for (Picture.Island i : pic.getRegions()) {
            this.islandsLeft.add(i);
        }
        // Adds filtered pixels from the Picture class into the traversed hashset
        for (Picture.Island i : pic.getNotConsidered()) {
            for (Picture.Pixel p : i.getIsland()) {
                traversed.add(p);
            }
        }
        // Stores whether each Pixel is "close to edge" or not in a map
        for (Picture.Pixel p : this.pic.getAllPixels()) {
            edgeMap.put(p, inside(p));
        }
    }

    // [DEBUGGED]
    public PathGenerator(Picture pic, int thickness, int cursorX,
                         int cursorY, int subIslandPixelThreshold) {
        this.pic = pic;
        this.cursorX = cursorX;
        this.cursorY = cursorY;
        this.thickness = thickness;
        this.traversed = new HashSet<>();
        this.addedToSubisland = new HashSet<>();
        this.subIslandPixelThreshold = subIslandPixelThreshold;
        islandsLeft = new HashSet<>();
        edgeMap = new HashMap<>();
        for (Picture.Island i : pic.getRegions()) {
            this.islandsLeft.add(i);
        }
        // Adds filtered pixels from the Picture class into the traversed hashset
        for (Picture.Island i : pic.getNotConsidered()) {
            for (Picture.Pixel p : i.getIsland()) {
                traversed.add(p);
            }
        }
        // Stores whether each Pixel is "close to edge" or not in a map
        for (Picture.Pixel p : this.pic.getAllPixels()) {
            edgeMap.put(p, inside(p));
        }
    }

    //***************************//
    //***** GETTERS/SETTERS *****//
    //***************************//

    public Picture getPic() {
        return pic;
    }

    public int getCursorX() {
        return cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    public int getThickness() {
        return thickness;
    }

    public void setCursorX(int cursorX) {
        this.cursorX = cursorX;
    }

    public void setCursorY(int cursorY) {
        this.cursorY = cursorY;
    }

    public HashSet<Picture.Pixel> getTraversed() {
        return traversed;
    }

    public HashSet<Picture.Island> getIslandsLeft() {
        return islandsLeft;
    }

    public int getSubIslandPixelThreshold() {
        return subIslandPixelThreshold;
    }

    public Map<Picture.Pixel, Boolean> getEdgeMap() {
        return edgeMap;
    }

    //*******************//
    //***** METHODS *****//
    //*******************//

    // [DEBUGGED]
    // Determines with respect to the picture and marker thickness whether the Pixel
    // can be classified as "close to the edge" for use in edge vs shading mode and
    // reduction of overflow. TRUE means the Pixel is INSIDE a region, sufficiently
    // far from the edge to be considered for shading.
    public boolean inside(Picture.Pixel p) {
        int borderDist = (thickness - 1) / 2;

        // The bounding box is defined as what is just out of range of
        // marker thickness
        int boxLeft = p.getY() - borderDist - 1;
        int boxRight = p.getY() + borderDist + 1;
        int boxUp = p.getX() - borderDist - 1;
        int boxDown = p.getX() + borderDist + 1;

        int currX = boxUp;
        int currY = boxLeft;

        // Checks each point in the bounding box
        for (int i = 0; i < 2 * (boxRight - boxLeft + boxDown - boxUp); i++) {

            // If the coordinate isn't in range or it's a null space, return false
            if (!pic.inRange(currX, currY) || pic.getPicture()[currX][currY] == null) {
                return false;
            }

            if (i <= boxRight - boxLeft - 1) {
                // Region (1): along top of box: move right
                currY++;

            } else if (i > boxRight - boxLeft - 1 &&
                    i <= boxRight - boxLeft + boxDown - boxUp - 1) {
                // Region (2): along right of box: move down
                currX++;

            } else if (i > boxRight - boxLeft + boxDown - boxUp - 1 &&
                    i <= 2 * (boxRight - boxLeft) + boxDown - boxUp - 1) {
                // Region (3): along bottom of box: move left
                currY--;

            } else {
                // Region (4): along left of box: move up
                currX--;

            }
        }

        // If every point passes, return true
        return true;
    }

    // [DEBUGGED]
    // [GLOBAL] Creates an expanding bounding box until at least one unprocessed Pixel is found.
    // The Pixel with the shortest distance is returned. This isn't guaranteed to return true
    // shortest distance because the border is a square, not a circle. But, at low distances, it
    // is a good approximation. In other words, this works best if your regions are densely packed.
    public Picture.Pixel getClosestUnprocessedPixelGlobal() {

        // If every Pixel has been traversed, return NULL
        if (getTraversed().size() == pic.getAllPixels().size()) {
            return null;
        }

        // Generating box bounds - will be corrected later in the while loop
        int boxLeft = cursorY;
        int boxRight = cursorY;
        int boxUp = cursorX;
        int boxDown = cursorX;

        // Candidate ArrayList
        ArrayList<Picture.Pixel> candidates = new ArrayList<>();

        while (candidates.size() == 0) {

            // Bounding box increases in size
            boxLeft--;
            boxRight++;
            boxUp--;
            boxDown++;

            // Checks box sizing and fixes it if it's out of bounds
            if (boxLeft < 0) {
                boxLeft = 0;
            }

            if (boxUp < 0) {
                boxUp = 0;
            }

            if (boxRight >= pic.getRightBound()) {
                boxRight = pic.getRightBound() - 1;
            }

            if (boxDown >= pic.getBottomBound()) {
                boxDown = pic.getBottomBound() - 1;
            }

            // Search starts in top left corner of the box
            int currY = boxLeft;
            int currX = boxUp;

            // Checks each point in the bounding box
            for (int i = 0; i < 2 * (boxRight - boxLeft + boxDown - boxUp); i++) {

                // If the coordinate has a pixel and it hasn't been traversed
                if (pic.getPicture()[currX][currY] != null &&
                        !traversed.contains(pic.getPicture()[currX][currY])) {
                    candidates.add(pic.getPicture()[currX][currY]);
                }

                if (i <= boxRight - boxLeft - 1) {
                    // Region (1): along top of box: move right
                    currY++;

                } else if (i > boxRight - boxLeft - 1 &&
                        i <= boxRight - boxLeft + boxDown - boxUp - 1) {
                    // Region (2): along right of box: move down
                    currX++;

                } else if (i > boxRight - boxLeft + boxDown - boxUp - 1 &&
                        i <= 2 * (boxRight - boxLeft) + boxDown - boxUp - 1) {
                    // Region (3): along bottom of box: move left
                    currY--;

                } else {
                    // Region (4): along left of box: move up
                    currX--;

                }
            }
        }

        // At this point, the candidate array is filled. You now just sort through candidates.
        Picture.Pixel closest = null;

        for (Picture.Pixel p : candidates) {
            if (closest == null) {
                closest = p;
            } else if (closest.sqDist(cursorX, cursorY) > p.sqDist(cursorX, cursorY)) {
                closest = p;
            } else if (closest.sqDist(cursorX, cursorY) == p.sqDist(cursorX, cursorY)) {
                int islandSize1 = closest.getParentIsland().getIsland().size();
                int islandSize2 = p.getParentIsland().getIsland().size();

                if (islandSize2 > islandSize1) {
                    closest = p;
                }
            }
        }

        // Should definitely return a pixel
        return closest;

    }

    // [NOTE] Pretty slow function overall it seems - need to improve efficiency

    // Takes in the current cursor position, considers
    // marker thickness, and returns a pixel adjacent to the active area
    // with the lowest edge factor. Returns null if it's reached the end of the island.
    public Picture.Pixel nextInIsland(Picture.Island i) {

        ArrayList<Picture.Pixel> adj = adjacentToMarker();

        // If the marker has reached a terminal point, figure out if the island is done processing
        // or not. If it's not a terminal point, keep processing the island.
        if (adj.size() == 0) {

            // Filter remaining subislands
            // [FIX] THE SLOW SPEED. NEED TO WRITE SOME NEW METHODS TO IMPROVE ITS SPEED

            addedToSubisland.clear();
            for (Picture.Pixel p : i.getIsland()) {
                if (!traversed.contains(p) && !addedToSubisland.contains(p)) {
                    filterSubIslands(p);
                }
            }


            // If there are no more unprocessed pixels in the island, return null. Otherwise,
            // return a pixel to hop to.
            Picture.Pixel close = getClosestUnprocessedPixelInIsland(i);
            return close;

        } else {

            // This is the tiebreaking chunk, makes multiple calls to discrete tiebreaking methods

            ArrayList<Picture.Pixel> adjList = adj;
            ArrayList<Picture.Pixel> nextList;
            ArrayList<Picture.Pixel> edgeList = tiebreakByEdge(adjList);

            // If there are no edge Pixels, shade; else, traverse on the edge
            if (edgeList.size() == 0) {
                nextList = adjList;
            } else {
                nextList = edgeList;
            }

            ArrayList<Picture.Pixel> lowestEFPixels = tiebreakByEF(nextList);

            // If there's only one then return that. Otherwise, tiebreak.
            if (lowestEFPixels.size() == 1) {
                return lowestEFPixels.get(0);
            }

            // If there's only one, return it. Otherwise, tiebreak by next metric.
            ArrayList<Picture.Pixel> greatestHypoMark = tiebreakByHypoMark(lowestEFPixels);
            if (greatestHypoMark.size() == 1) {
                return greatestHypoMark.get(0);
            }

            // Tiebreaks by which one covers the greatest number of edges (cleans markerpath)
            // [CURRENT] Doesn't tiebreak any further
            ArrayList<Picture.Pixel> greatestHypoEdgeMark = tiebreakByHypoEdgeMark(greatestHypoMark);
            return greatestHypoEdgeMark.get(0);
        }
    }

    // Returns a list of unprocessed Pixels adjacent to the active area
    public ArrayList<Picture.Pixel> adjacentToMarker() {

        int borderDist = (thickness - 1) / 2;

        // The bounding box is defined as what is just out of range of
        // marker thickness
        int boxLeft = cursorY - borderDist - 1;
        int boxRight = cursorY + borderDist + 1;
        int boxUp = cursorX - borderDist - 1;
        int boxDown = cursorX + borderDist + 1;

        int currX = boxUp;
        int currY = boxLeft;
        ArrayList<Picture.Pixel> adjList = new ArrayList<>();

        // Checks each point in the bounding box
        for (int i = 0; i < 2 * (boxRight - boxLeft + boxDown - boxUp); i++) {

            // If the coordinate is in range, has a pixel, and hasn't been traversed
            if (pic.inRange(currX, currY) && pic.getPicture()[currX][currY] != null &&
                    !traversed.contains(pic.getPicture()[currX][currY])) {
                adjList.add(pic.getPicture()[currX][currY]);
            }

            if (i <= boxRight - boxLeft - 1) {
                // Region (1): along top of box: move right
                currY++;

            } else if (i > boxRight - boxLeft - 1 &&
                    i <= boxRight - boxLeft + boxDown - boxUp - 1) {
                // Region (2): along right of box: move down
                currX++;

            } else if (i > boxRight - boxLeft + boxDown - boxUp - 1 &&
                    i <= 2 * (boxRight - boxLeft) + boxDown - boxUp - 1) {
                // Region (3): along bottom of box: move left
                currY--;

            } else {
                // Region (4): along left of box: move up
                currX--;

            }
        }
        return adjList;
    }

    // [DEBUGGED] REALLY slow for the case of large contiguous regions, but otherwise functional
    // After you reach a terminal point, check the remaining pixels in the island.
    // If the subislands don't reach a threshold size (subIslandPictureThreshold), then
    // discard the entire subisland from consideration by adding the pixels into the
    // traversed HashSet. NOTE: this isn't exactly accurate to how the logic works, but
    // adding Pixels to traversed is functionally the same as removing them from consideration.
    public void filterSubIslands(Picture.Pixel p) {

        Queue<Picture.Pixel> q = new LinkedList<>();

        q.add(p);

        while (!q.isEmpty()) {

            Picture.Pixel currPixel = q.poll();
            addedToSubisland.add(currPixel);

            for (Picture.Pixel neighbor : currPixel.getAdjList()) {
                if (!traversed.contains(neighbor) &&
                        !addedToSubisland.contains(neighbor) && !q.contains(neighbor)) {
                    q.add(neighbor);
                }
            }
        }

        if (addedToSubisland.size() < subIslandPixelThreshold) {
            traversed.addAll(addedToSubisland);
        }
    }

    // [ISLAND] Will get the next closest unprocessed pixel within the island. Should ONLY be used for
    // pen jumping. DO NOT use this to find where the marker goes next if it hasn't reached a terminal point.
    public Picture.Pixel getClosestUnprocessedPixelInIsland(Picture.Island island) {

        // Generating box bounds - will be corrected later in the while loop
        int boxLeft = cursorY;
        int boxRight = cursorY;
        int boxUp = cursorX;
        int boxDown = cursorX;

        // Candidate ArrayList
        ArrayList<Picture.Pixel> candidates = new ArrayList<>();

        while (candidates.size() == 0) {

            // Bounding box increases in size
            boxLeft--;
            boxRight++;
            boxUp--;
            boxDown++;

            // Checks box sizing and fixes it if it's out of bounds
            int ibl = island.getBoxLeft();
            if (boxLeft < ibl) {
                boxLeft = ibl;
            }

            int ibu = island.getBoxUp();
            if (boxUp < ibu) {
                boxUp = ibu;
            }

            int ibr = island.getBoxRight();
            if (boxRight > ibr) {
                boxRight = ibr;
            }

            int ibd = island.getBoxDown();
            if (boxDown > ibd) {
                boxDown = ibd;
            }

            // If NO candidates have been found and the box is at max bounds,
            // that means every pixel in the island has been processed
            // and the function returns NULL
            if (boxLeft == island.getBoxLeft() && boxDown == island.getBoxDown() &&
                    boxRight == island.getBoxRight() && boxUp == island.getBoxUp()) {
                return null;
            }

            // Search starts in top left corner of the box
            int currY = boxLeft;
            int currX = boxUp;

            // Checks each point in the bounding box
            for (int i = 0; i < 2 * (boxRight - boxLeft + boxDown - boxUp); i++) {

                // If the coordinate has a pixel and it hasn't been traversed and it's in the island
                if (pic.getPicture()[currX][currY] != null &&
                        !traversed.contains(pic.getPicture()[currX][currY]) &&
                        island.getIsland().contains(pic.getPicture()[currX][currY])) {
                    candidates.add(pic.getPicture()[currX][currY]);
                }

                if (i <= boxRight - boxLeft - 1) {
                    // Region (1): along top of box: move right
                    currY++;

                } else if (i > boxRight - boxLeft - 1 &&
                        i <= boxRight - boxLeft + boxDown - boxUp - 1) {
                    // Region (2): along right of box: move down
                    currX++;

                } else if (i > boxRight - boxLeft + boxDown - boxUp - 1 &&
                        i <= 2 * (boxRight - boxLeft) + boxDown - boxUp - 1) {
                    // Region (3): along bottom of box: move left
                    currY--;

                } else {
                    // Region (4): along left of box: move up
                    currX--;

                }
            }
        }

        // The candidate array is filled. What SHOULD happen is that prior to running this method,
        // the PathGenerator will remove sub-islands that are too small to be considered. So, of the
        // remaining candidate pixels, they belong to major islands. Tiebreaks by subisland size.
        Picture.Pixel closest = null;

        for (Picture.Pixel p : candidates) {
            if (closest == null) {
                closest = p;
            } else if (closest.sqDist(cursorX, cursorY) > p.sqDist(cursorX, cursorY)) {
                closest = p;
            } else if (closest.sqDist(cursorX, cursorY) == p.sqDist(cursorX, cursorY)) {
                if (subIslandSize(p) > subIslandSize(closest)) {
                    closest = p;
                }
            }
        }

        // Should definitely return a pixel
        return closest;
    }

    // Method for comparing subIsland size - doesn't create Island objects because there is
    // no stable way to determine what island pixels belong to without a global reference
    public int subIslandSize(Picture.Pixel p) {

        int size = 0;
        HashSet<Picture.Pixel> subIsland = new HashSet<>();
        Queue<Picture.Pixel> q = new LinkedList<>();

        q.add(p);

        while (!q.isEmpty()) {

            Picture.Pixel currPixel = q.poll();
            subIsland.add(currPixel);
            size++;

            for (Picture.Pixel neighbor : currPixel.getAdjList()) {
                if (!traversed.contains(neighbor) &&
                        !subIsland.contains(neighbor) && !q.contains(neighbor)) {
                    q.add(neighbor);
                }
            }
        }
        return size;
    }

    // Discrete method that tiebreaks by whether Pixels are on the edge or not
    public ArrayList<Picture.Pixel> tiebreakByEdge(ArrayList<Picture.Pixel> adjList) {
        ArrayList<Picture.Pixel> edgeList = new ArrayList<>();
        for (Picture.Pixel p : adjList) {
            if (!edgeMap.containsKey(p)) {
                edgeList.add(p);
            }
        }
        return edgeList;
    }

    // Discrete method that tiebreaks by edge factor
    public ArrayList<Picture.Pixel> tiebreakByEF(ArrayList<Picture.Pixel> adjList) {
        ArrayList<Picture.Pixel> lowestEFPixels = new ArrayList<>();
        int lowestEF = 8;

        // Finds lowest EF in adjacent pixels
        for (Picture.Pixel p : adjList) {
            int ef = p.getEdgeFactor();
            if (ef < lowestEF) {
                lowestEF = ef;
            }
        }

        // Gathers all pixels with lowest EF
        for (Picture.Pixel p : adjList) {
            if (p.getEdgeFactor() == lowestEF) {
                lowestEFPixels.add(p);
            }
        }

        return lowestEFPixels;
    }

    // Discrete method that tiebreaks pixels by hypothetical marks
    public ArrayList<Picture.Pixel> tiebreakByHypoMark(ArrayList<Picture.Pixel> lowestEFPixels) {
        // This chunk tiebreaks based on hypothetical marked area
        ArrayList<Picture.Pixel> tiebreaker = new ArrayList<>();
        int hypoMark = 0;

        // Finds the highest hypothetical mark
        for (Picture.Pixel p : lowestEFPixels) {
            int currMark = hypotheticalMark(p);

            if (currMark > hypoMark) {
                hypoMark = currMark;
            }
        }

        // Accumulates all Pixels with this hypomark
        for (Picture.Pixel p : lowestEFPixels) {
            int currMark = hypotheticalMark(p);

            if (currMark == hypoMark) {
                tiebreaker.add(p);
            }
        }

        return tiebreaker;
    }

    public ArrayList<Picture.Pixel> tiebreakByHypoEdgeMark(ArrayList<Picture.Pixel> prevArray) {
        // This chunk tiebreaks based on hypothetical marked area
        ArrayList<Picture.Pixel> tiebreaker = new ArrayList<>();
        int hypoMark = 0;

        // Finds the highest hypothetical mark
        for (Picture.Pixel p : prevArray) {
            int currMark = hypotheticalEdgeMark(p);

            if (currMark > hypoMark) {
                hypoMark = currMark;
            }
        }

        // Accumulates all Pixels with this hypomark
        for (Picture.Pixel p : prevArray) {
            int currMark = hypotheticalEdgeMark(p);

            if (currMark == hypoMark) {
                tiebreaker.add(p);
            }
        }

        return tiebreaker;
    }

    // Returns an int value that indicates how many unprocessed Pixels would be hypothetically marked
    // if the marker were immediately moved to that position.
    public int hypotheticalMark(Picture.Pixel p) {
        int hypotheticalMarked = 0;
        int borderDist = (thickness - 1) / 2;

        // The bounding box is defined as what lies in the thickness zone of the marker
        int boxLeft = p.getY() - borderDist;
        int boxRight = p.getY() + borderDist;
        int boxUp = p.getX() - borderDist;
        int boxDown = p.getX() + borderDist;

        // Iterates in the rectangular shape approximated to be the marker thickness and
        // marks all untraversed pixels as traversed.
        for (int x = boxUp; x < boxDown + 1; x++) {
            for (int y = boxLeft; y < boxRight + 1; y++) {
                if (pic.inRange(x, y) && pic.getPicture()[x][y] != null) {
                    hypotheticalMarked++;
                }
            }
        }

        return hypotheticalMarked;
    }

    // Returns an int value that indicates how many unprocessed EDGE Pixels would be hypothetically
    // marked if the marker were immediately moved to that position.
    public int hypotheticalEdgeMark(Picture.Pixel p) {
        int hypotheticalMarked = 0;
        int borderDist = (thickness - 1) / 2;

        // The bounding box is defined as what lies in the thickness zone of the marker
        int boxLeft = p.getY() - borderDist;
        int boxRight = p.getY() + borderDist;
        int boxUp = p.getX() - borderDist;
        int boxDown = p.getX() + borderDist;

        // Iterates in the rectangular shape approximated to be the marker thickness and
        // marks all untraversed pixels as traversed.
        for (int x = boxUp; x < boxDown + 1; x++) {
            for (int y = boxLeft; y < boxRight + 1; y++) {
                // If there's a Pixel there that is an edge
                if (pic.inRange(x, y) && pic.getPicture()[x][y] != null &&
                        pic.getPicture()[x][y].getEdgeFactor() > 0) {
                    hypotheticalMarked++;
                }
            }
        }

        return hypotheticalMarked;
    }

    // Determines whether two Pixels are adjacent based on marker thickness
    public boolean pixelsAdjacent(Picture.Pixel p, Picture.Pixel q) {
        int borderDist = (thickness - 1) / 2;

        int px = p.getX();
        int py = p.getY();
        int qx = q.getX();
        int qy = q.getY();

        // The bounding box is defined as the active area around p + 1
        int boxLeft = py - borderDist - 1;
        int boxRight = py + borderDist + 1;
        int boxUp = px - borderDist - 1;
        int boxDown = px + borderDist + 1;

        // if q is outside the bounding box, it's not adjacent
        if (qy < boxLeft || qy > boxRight || qx < boxUp || qx > boxDown) {
            return false;
        }

        return true;
    }

    // Marks pixels once the marker has been moved to that position
    public void mark() {
        mark(cursorX, cursorY);
    }

    public void mark(int x, int y) {
        int borderDist = (thickness - 1) / 2;

        // The bounding box is defined as what lies in the thickness zone of the marker
        int boxLeft = y - borderDist;
        int boxRight = y + borderDist;
        int boxUp = x - borderDist;
        int boxDown = x + borderDist;

        // Iterates in the rectangular shape approximated to be the marker thickness and
        // marks all untraversed pixels as traversed.
        for (int xl = boxUp; xl < boxDown + 1; xl++) {
            for (int yl = boxLeft; yl < boxRight + 1; yl++) {
                if (pic.inRange(xl, yl) && pic.getPicture()[xl][yl] != null) {
                    traversed.add(pic.getPicture()[xl][yl]);
                }
            }
        }
    }

    // Marks all extraneous pixels that lie between two Pixels.
    public void mark(Picture.Pixel p, Picture.Pixel q) {

        int x1 = p.getX();
        int y1 = p.getY();
        int x2 = q.getX();
        int y2 = q.getY();

        // Nothing is ever marked if Pixel centers are off by one or none
        if (Math.abs(x2 - x1) <= 1 || Math.abs(y2 - y1) <= 1) {
            return;
        }

        // Diagonal case
        if (Math.abs(x2 - x1) == Math.abs(y2 - y1)) {
            // Case 1: Goes down; Case 2: Goes up
            if (x2 - x1 > 0) {
                // Case 1: Goes right; Case 2: Goes left
                if (y2 - y1 > 0) {
                    for (int i = 0; i < Math.abs(x2 - x1) - 1; i++) {
                        x1++;
                        y1++;
                        mark(x1, y1);
                    }
                } else {
                    for (int i = 0; i < Math.abs(x2 - x1) - 1; i++) {
                        x1++;
                        y1--;
                        mark(x1, y1);
                    }
                }

            } else {
                // Case 1: Goes right; Case 2: Goes left
                if (y2 - y1 > 0) {
                    for (int i = 0; i < Math.abs(x2 - x1) - 1; i++) {
                        x1--;
                        y1++;
                        mark(x1, y1);
                    }
                } else {
                    for (int i = 0; i < Math.abs(x2 - x1) - 1; i++) {
                        x1--;
                        y1--;
                        mark(x1, y1);
                    }
                }
            }
            return;
        }

        // Leftover Cases: Somewhere between with extraneous marks
        // Case 1: Goes down; Case 2: Goes up
        if (x2 - x1 > 0) {
            // Case 1: Goes right; Case 2: Goes left
            if (y2 - y1 > 0) {
                while (x2 - x1 > 1 && y2 - y1 > 1) {
                    x1++;
                    y1++;
                    x2--;
                    y2--;
                    mark(x1, y1);
                    mark(x2, y2);
                }
            } else {
                while (x2 - x1 > 1 && y1 - y2 > 1) {
                    x1++;
                    y1--;
                    x2--;
                    y2++;
                    mark(x1, y1);
                    mark(x2, y2);
                }
            }

        } else {
            // Case 1: Goes right; Case 2: Goes left
            if (y2 - y1 > 0) {
                while (x2 - x1 > 1 && y2 - y1 > 1) {
                    x1--;
                    y1++;
                    x2++;
                    y2--;
                    mark(x1, y1);
                    mark(x2, y2);
                }
            } else {
                while (x2 - x1 > 1 && y2 - y1 > 1) {
                    x1--;
                    y1--;
                    x2++;
                    y2++;
                    mark(x1, y1);
                    mark(x2, y2);
                }
            }
        }
        return;
    }

    // Reduces the overflow of an already-placed path Pixel
    public void reduceOverflow(Picture.Pixel p, Path path) {

        int currOverflow = overflow(p);
        Picture.Pixel newP = pushFactor(p);

        // If the push factor doesn't yield a new pixel, do nothing
        if (newP == p) {
            return;
        }

        int newOverflow = overflow(newP);

        // If the overflow value for the new pixel is actually worse, do nothing
        if (newOverflow >= currOverflow) {
            return;
        }

        HashSet<Picture.Pixel> firstLeftovers = containedIn(p);
        HashSet<Picture.Pixel> inNewPRange = containedIn(newP);

        // Pixels in the original range that aren't in the new proposed range. These are the
        // initial leftover values.
        firstLeftovers.removeAll(inNewPRange);

        // Filters out the pixels that are traversed by other path points
        HashSet<Picture.Pixel> finalLeftovers = inRangeOfOther(firstLeftovers, p, path);
        int leftovers = finalLeftovers.size();

        // simple formula to determine if it's "worth it" to perform the pixel correction
        if (currOverflow - newOverflow >= leftovers) {
            path.changePath(p, newP);
        }
    }

    // Given a point, returns the overflow depending on marker thickness
    public int overflow(int x, int y) {
        int borderDist = (thickness - 1) / 2;

        // The bounding box is defined as what lies in the thickness zone of the marker
        int boxLeft = y - borderDist;
        int boxUp = x - borderDist;
        int overflow = 0;

        for (int i = boxUp; i < boxUp + thickness; i++) {
            for (int j = boxLeft; j < boxLeft + thickness; j++) {
                if (pic.inRange(i, j) && pic.getPicture()[i][j] == null) {
                    overflow++;
                }
            }
        }

        return overflow;
    }

    // Given a Pixel, returns the overflow depending on marker thickness
    public int overflow(Picture.Pixel p) {
        return overflow(p.getX(), p.getY());
    }

    // Returns another Pixel that the current Pixel should go to in order to reduce overflow.
    // If no such Pixel is found, it returns itself.
    public Picture.Pixel pushFactor(Picture.Pixel p) {

        int borderDist = (thickness - 1) / 2;

        int currX = p.getX();
        int currY = p.getY();

        // Variables that track the influence of the edges
        int xMovement = 0;
        int yMovement = 0;

        // Empty spaces exert a push on the pixel. This adds up the total influence.

        // Top left
        if (pic.inRange(p.getX() - 1, p.getY() - 1) && pic.getPicture()[p.getX() - 1][p.getY() - 1] == null) {
            xMovement++;
            yMovement++;
        }

        // Middle Left
        if (pic.inRange(p.getX(), p.getY() - 1) && pic.getPicture()[p.getX()][p.getY() - 1] == null) {
            yMovement++;
        }

        // Bottom Left
        if (pic.inRange(p.getX() + 1, p.getY() - 1) && pic.getPicture()[p.getX() + 1][p.getY() - 1] == null) {
            xMovement--;
            yMovement++;
        }

        // Top Middle
        if (pic.inRange(p.getX() - 1, p.getY()) && pic.getPicture()[p.getX() - 1][p.getY()] == null) {
            xMovement++;
        }

        // Bottom Middle
        if (pic.inRange(p.getX() + 1, p.getY()) && pic.getPicture()[p.getX() + 1][p.getY()] == null) {
            xMovement--;
        }

        // Top Right
        if (pic.inRange(p.getX() - 1, p.getY() + 1) && pic.getPicture()[p.getX() - 1][p.getY() + 1] == null) {
            xMovement++;
            yMovement--;
        }

        // Middle Right
        if (pic.inRange(p.getX(), p.getY() + 1) && pic.getPicture()[p.getX()][p.getY() + 1] == null) {
            yMovement--;
        }

        // Bottom Right
        if (pic.inRange(p.getX() + 1, p.getY() + 1) && pic.getPicture()[p.getX() + 1][p.getY() + 1] == null) {
            xMovement--;
            yMovement--;
        }

        // Resulting cases taking Push Factor into account
        if (xMovement == 0 && yMovement == 0) {
            return p;
        } else if (Math.abs(xMovement) == Math.abs(yMovement)) {

            xMovement = (xMovement / Math.abs(xMovement)) * borderDist;
            yMovement = (yMovement / Math.abs(yMovement)) * borderDist;

            if (pic.inRange(currX + xMovement, currY + yMovement) &&
                    pic.getPicture()[currX + xMovement][currY + yMovement] != null) {
                return pic.getPicture()[currX + xMovement][currY + yMovement];
            }
        } else if (Math.abs(xMovement) > Math.abs(yMovement)) {
            xMovement = (xMovement / Math.abs(xMovement)) * borderDist;

            if (pic.inRange(currX + xMovement, currY) &&
                    pic.getPicture()[currX + xMovement][currY] != null) {
                return pic.getPicture()[currX + xMovement][currY];
            }
        } else if (Math.abs(xMovement) < Math.abs(yMovement)) {
            yMovement = (yMovement / Math.abs(yMovement)) * borderDist;

            if (pic.inRange(currX, currY + yMovement) &&
                    pic.getPicture()[currX][currY + yMovement] != null) {
                return pic.getPicture()[currX][currY + yMovement];
            }
        }

        // If none of those are returned, then the Push Factor heuristic has failed and
        // the algorithm makes no attempt to correct this Pixel.
        return p;
    }

    // Given coordinates, returns all Pixels contained in an active area centered there
    public HashSet<Picture.Pixel> containedIn(int x, int y) {
        int borderDist = (thickness - 1) / 2;

        // The bounding box is defined as what lies in the thickness zone of the marker
        int boxLeft = y - borderDist;
        int boxUp = x - borderDist;
        HashSet<Picture.Pixel> h = new HashSet<>();

        for (int i = boxUp; i < boxUp + thickness; i++) {
            for (int j = boxLeft; j < boxLeft + thickness; j++) {
                if (pic.inRange(i, j) && pic.getPicture()[i][j] != null) {
                    h.add(pic.getPicture()[i][j]);
                }
            }
        }
        return h;
    }

    public HashSet<Picture.Pixel> containedIn(Picture.Pixel p) {
        return containedIn(p.getX(), p.getY());
    }

    // Takes a set of leftover points and checks whether the points are traversed by any path Pixels that are NOT p.
    // Returns a set of all points which are leftover and are only traversed by p.
    public HashSet<Picture.Pixel> inRangeOfOther(HashSet<Picture.Pixel> h, Picture.Pixel notP, Path path) {
        HashSet<Picture.Pixel> leftovers = h;
        HashSet<Picture.Pixel> notLeftovers = new HashSet<>();

        for (Picture.Pixel p : h) {
            for (Path.Point<Picture.Pixel, Boolean> point : path.getPath()) {

                // If the leftover is in the range of a point other than the provided Pixel
                if (point.getKey() != notP &&
                        containedIn(point.getKey().getX(), point.getKey().getY()).contains(p)) {
                    notLeftovers.add(p);
                    break;
                }
            }
        }

        // [NOTE] POSSIBLE IMPLEMENTATION IN FUTURE: Remove elements of leftovers from traversed
        // and perform another subisland filter after every element has been reprocessed.
        leftovers.removeAll(notLeftovers);
        return leftovers;
    }

    // Makes path in order
    public Path makePath() {

        Path path = new Path();

        Picture.Pixel curr = getClosestUnprocessedPixelGlobal();

        if (curr == null) {
            return path;
        }

        // While there are still islands left to consider OR all Pixels have been traversed.
        // The reason we need this second condition is because if the marker is sufficiently thick it
        // can mark across islands and skew the islands traversed calculation.
        while (!islandsLeft.isEmpty() && traversed.size() != pic.getAllPixels().size()) {

            // When this loop begins, the pen is hopping to the next island, so don't
            // mark between Pixels (only the endpoints)

            // Need this chunk to prevent re-adding same points to the path.
            if (path.length() == 0) {
                path.addPoint(curr, false);
            }

            setCursorX(curr.getX());
            setCursorY(curr.getY());
            mark();

            Picture.Island island = curr.getParentIsland();
            islandsLeft.remove(island);
            Picture.Pixel next = nextInIsland(island);

            if (next == null) {

                islandsLeft.remove(island);
                Picture.Pixel nextPix = getClosestUnprocessedPixelGlobal();
                island = nextPix.getParentIsland();
                if (nextPix != null) {
                    next = nextPix;
                } else {
                    return path;
                }
            }

            // Loops while there are still applicable Pixels in the island
            while (next != null) {

                // If the pixels are next to each other then pen stays down.
                // Otherwise, lift the pen.
                if (pixelsAdjacent(curr, next)) {
                    path.addPoint(next, true);
                    // If pen stays down, mark all intermediate extraneous Pixels
                    mark(curr, next);
                } else {
                    path.addPoint(next, false);
                }

                setCursorX(next.getX());
                setCursorY(next.getY());
                mark();
                curr = next;
                next = nextInIsland(island);
            }
        }

        // Reduces the overflow of points in the path
        for (Path.Point<Picture.Pixel, Boolean> point : path.getPath()) {
            reduceOverflow(point.getKey(), path);
        }

        // Returns to the origin at the end
        path.addPoint(new Picture().new Pixel(0, 0), false);

        return path;
    }

}
