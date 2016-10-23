/**
 * Author: Albert Li
 * Start Date: 9/26/16
 *
 * Description: Path object that holds all the points for the pen. Will
 * carry instructions that indicate whether the pen should move up or down
 * and coordinates for where the pen should move.
 *
 * Changelog:
 *      v1.0.0 - 10/4/16
 *          - NO CHANGES - MARKING FIRST RELEASE.
 *      v0.2.1 - 9/29/16
 *          - GENERAL CHANGES
 *              - Removed length instance variable, instead created length() function calling size of path ArrayList
 *              - [NOTE] Added capability to match point based on coordinates and
 *                not just Pixel uniqueness (MIGHT revert)
 *      v0.2 - 9/27/16
 *          - Added capabilities to:
 *              - Retrieve the point in the path containing Pixel p
 *              - Change the path Pixel by Pixel
 *          - Removed:
 *              - SubPath implementation, replaced with ArrayList of Points indicating whether
 *                the pen should be up or down when moving to that point.
 *      v0.1 - 9/26/16
 *          - [INSPECTED][NOT DEBUGGED] Path with capabilities to:
 *              - Return its length and see each sub-path
 *              - Add points to it
 */

import java.util.ArrayList;

public class Path {

    //******************************//
    //***** INSTANCE VARIABLES *****//
    //******************************//

    private ArrayList<Point<Picture.Pixel, Boolean>> path;

    //******************************//
    //***** ENCAPSULATED CLASS *****//
    //******************************//

    public class Point<K, V> {

        private K key;
        private V value;

        public Point(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public void setKey(K key) {
            this.key = key;
        }

        public void setValue(V value) {
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

    //************************//
    //***** CONSTRUCTORS *****//
    //************************//

    public Path() {
        this.path = new ArrayList<>();
    }

    //***************************//
    //***** GETTERS/SETTERS *****//
    //***************************//

    public ArrayList<Point<Picture.Pixel, Boolean>> getPath() {
        return path;
    }

    //*******************//
    //***** METHODS *****//
    //*******************//

    // [DEBUGGED] Adds a point to the path, specifies whether the pen should be up or down
    public void addPoint(Picture.Pixel p, boolean penDown) {
        path.add(new Point(p, penDown));
    }

    // [DEBUGGED] Returns the point in the path that matches the coordinates of Pixel p
    // Returns null if p isn't in the path
    public Point<Picture.Pixel, Boolean> getMatchingPoint(Picture.Pixel p) {
        for (Point<Picture.Pixel, Boolean> h : path) {
            if (h.getKey() == p || (p.getX() == h.getKey().getX() &&
                    p.getY() == h.getKey().getY())) {
                return h;
            }
        }
        return null;
    }

    // [DEBUGGED] Changes Pixel p to Pixel q in a path.
    public void changePath(Picture.Pixel p, Picture.Pixel q) {
        Point<Picture.Pixel, Boolean> h = getMatchingPoint(p);
        if (h != null) {
            h.setKey(q);
        }
    }

    // [DEBUGGED] Returns length of path in points
    public int length() {
        return getPath().size();
    }

}