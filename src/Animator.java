import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

/**
 * Author: Albert Li
 * Start Date: 10/4/16
 *
 * Description: Animates a path
 *
 * Changelog:
 *      v1.0.0 - 10/4/16 -
 *          - First pass of the animation program with capabilities to:
 *              - Draw the bounds of the islands of the picture
 *              - Show the pen path accurately, demonstrating where the pen
 *                goes up and down based on color (and animates the path)
 *              - Simplifies the final picture by removing island and pen up cues
 *
 */

public class Animator extends JPanel implements ActionListener {

    PathGenerator pg;
    Path path;
    int index;
    private BufferedImage image;
    private BufferedImage finalImage;
    private ArrayList<Picture.Island> islands;

    public static void main(String[] args) {

        try {

            // Image repository
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
            File img14 = new File("images/Test14.jpeg"); // overdose sign [1; rgb48]
            File img15 = new File("images/Test15.jpg"); // Pattern [1; rgb128]
            File img16 = new File("images/Test16.jpeg"); // Brain
            File img17 = new File("images/Test17.jpeg"); // Zebra
            File img18 = new File("images/Test18.jpeg"); // Old Person
            File img19 = new File("images/Test19.jpg"); // Michael Jordan
            File img20 = new File("images/Test20.jpg"); // Thermo Diagram
            File img21 = new File("images/Test21.png"); // Thermo Diagram (better quality)

            File real1 = new File("images/Real1.jpg"); // Plug in wall

            // VERY IMPORTANT VARIABLES!!!
            // - pixelThresholdPercent determines what size island should be filtered (if it's smaller in pixel
            // size than the percentage of the total pixels of the picture, that island is not considered)
            // - rgbSensitivityThreshold determines what RGB value of ANY color needs to be surpassed to be
            // registered as a pixel and not just stray marks
            // - thickness determines how many pixels x pixels thick the marker is. In reality, the marker is
            // probably closer to a circle, but we can use squares to approximate it for sufficiently small sizes.
            double pixelThresholdPercent = .01;
            double rgbSensitivityThreshold = 32;
            int thickness = 5;

            int timeDelay = 1; // Delay between line segments drawn in ms

            // Which image is being drawn
            File applyToAllTests = img13;

            // Don't mess with anything under this comment.
            BufferedImage imagePathTest = ImageIO.read(applyToAllTests);
            Picture pic = new Picture(imagePathTest, pixelThresholdPercent, rgbSensitivityThreshold);
            int subIslandPixelThreshold = pic.getPixelThreshold();
            PathGenerator pg = new PathGenerator(pic, thickness, subIslandPixelThreshold);

            Animator animator = new Animator(pg);

            JFrame frame = new JFrame();
            frame.add(animator);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            Timer timer = new Timer(timeDelay, animator);
            timer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Constructor for the Animator object.
    public Animator(PathGenerator pg) {
        this.pg = pg;
        this.path = pg.makePath();
        index = 0;
        this.islands = new ArrayList<>();
        for (Picture.Island i : pg.getPic().getRegions()) {
            islands.add(i);
        }
        image = new BufferedImage(pg.getPic().getPicture()[0].length,
                pg.getPic().getPicture().length, BufferedImage.TYPE_3BYTE_BGR);
        finalImage = new BufferedImage(pg.getPic().getPicture()[0].length,
                pg.getPic().getPicture().length, BufferedImage.TYPE_3BYTE_BGR);
    }

    // Sets the window size to the pixel size of the image (gives a good visual representation of size)
    public Dimension getPreferredSize() {
        return new Dimension(pg.getPic().getPicture()[0].length, pg.getPic().getPicture().length);
    }

    @Override
    // What actually controls each drawing animation
    public void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (index < path.length())
            updateBuffer();

        if (index == path.length() - 1) {
            g.drawImage(finalImage, 0, 0, null);
        } else {
            g.drawImage(image, 0, 0, null);
        }

    }

    // Updates the buffered images that are displayed as they are drawn onto. This is because swing
    // will erase all the previous drawing that you did, so we instead store a buffer and just draw onto
    // it and reprint the buffer onto the frame every iteration
    public void updateBuffer() {
        Graphics g = image.getGraphics();
        Graphics h = finalImage.getGraphics();

        int x1;
        int y1;
        int x2;
        int y2;
        Color c;

        if (islands.size() > 0) {
            Picture.Island i = islands.remove(0);

            g.setColor(Color.BLUE);
            g.drawRect(i.getBoxLeft(), i.getBoxUp(), i.getBoxRight() - i.getBoxLeft(),
                    i.getBoxDown() - i.getBoxUp());
            g.dispose();

        } else {

            if (index == 0) {
                x1 = 0;
                y1 = 0;
                x2 = path.getPath().get(0).getKey().getY();
                y2 = path.getPath().get(0).getKey().getX();

            } else {
                x1 = path.getPath().get(index - 1).getKey().getY();
                y1 = path.getPath().get(index - 1).getKey().getX();
                x2 = path.getPath().get(index).getKey().getY();
                y2 = path.getPath().get(index).getKey().getX();
            }

            // Red if pen is down, green otherwise
            if (path.getPath().get(index).getValue()) {
                c = Color.RED;

                g.setColor(c);
                g.drawLine(x1, y1, x2, y2);
                g.dispose();

                h.setColor(c);
                h.drawLine(x1, y1, x2, y2);
                h.dispose();
            } else {
                c = Color.GREEN;

                g.setColor(c);
                g.drawLine(x1, y1, x2, y2);
                g.dispose();
            }
        }
    }

    // Updates the index after each component is painted so the path can progress.
    public void actionPerformed(ActionEvent e) {
        if (index < path.length() - 1 && islands.size() == 0) {
            index++;
            repaint();
        } else if(islands.size() > 0) {
            repaint();
        }
    }

}
