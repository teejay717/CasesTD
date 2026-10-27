package btd.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * The track the enemies walk along. Points come from data/maps/map1_path.csv, which is drawn on a
 * 1280x720 canvas, so the x values are squeezed to fit the map area next to the side panel.
 */
public class GameMap {

    public static final int WIDTH = 960;   // map area; the side panel takes the rest of the 1280 window
    public static final int HEIGHT = 720;
    public static final int CELL = 40;     // tower placement grid

    private static final double FILE_WIDTH = 1280.0;
    private static final double PATH_CLEARANCE = 38; // towers must be at least this far from the track center

    private final List<double[]> points = new ArrayList<>();
    private final List<Double> cumulative = new ArrayList<>();
    private double totalLength;

    /** Loads the path file, for example data/maps/map1_path.csv. */
    public static GameMap load(String csvFile) {
        Path file = Paths.get(csvFile);
        List<double[]> pts = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("x")) {
                    continue; // skip the header
                }
                String[] parts = line.split(",");
                pts.add(new double[]{
                        Double.parseDouble(parts[0].trim()) * WIDTH / FILE_WIDTH,
                        Double.parseDouble(parts[1].trim())});
            }
        } catch (IOException | NumberFormatException e) {
            throw new IllegalStateException("Cannot read the map path " + file.toAbsolutePath()
                    + ". Run the game from the project root.", e);
        }
        return new GameMap(pts);
    }

    public GameMap(List<double[]> pathPoints) {
        points.addAll(pathPoints);
        cumulative.add(0.0);
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            totalLength += Math.hypot(b[0] - a[0], b[1] - a[1]);
            cumulative.add(totalLength);
        }
    }

    public double getTotalLength() {
        return totalLength;
    }

    public List<double[]> getPoints() {
        return points;
    }

    /** The {x, y} position after walking this far along the track. */
    public double[] pointAt(double distance) {
        if (distance <= 0) {
            return points.get(0).clone();
        }
        if (distance >= totalLength) {
            return points.get(points.size() - 1).clone();
        }
        int i = 1;
        while (cumulative.get(i) < distance) {
            i++;
        }
        double[] a = points.get(i - 1);
        double[] b = points.get(i);
        double segment = cumulative.get(i) - cumulative.get(i - 1);
        double t = (distance - cumulative.get(i - 1)) / segment;
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
    }

    /** Shortest distance from a point to the track line. */
    public double distanceToPath(double x, double y) {
        double best = Double.MAX_VALUE;
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            best = Math.min(best, distanceToSegment(x, y, a[0], a[1], b[0], b[1]));
        }
        return best;
    }

    /** True if a tower may be built here: inside the map, below the top bar, and off the track. */
    public boolean canBuildAt(double x, double y) {
        return x >= 20 && x <= WIDTH - 20 && y >= 60 && y <= HEIGHT - 20
                && distanceToPath(x, y) >= PATH_CLEARANCE;
    }

    private static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }
}
