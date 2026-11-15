package btd.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * [PURPOSE]       The map: the path enemies walk (list of waypoints), its total length, and where towers may be built.
 * [OOP / PATTERN] Domain Model / Static Factory Method (load()).
 * [CALLED BY]     Loaded by btd/ui/AppController.java (loadData()); used by btd/core/GameEngine.java,
 *                 btd/core/GameSession.java (canPlaceAt()), btd/model/enemy/Enemy.java (update()), GamePanel.
 * [CONNECTS TO]   data/maps/map1_path.csv.
 * [DEFENSE NOTE]  The CSV was drawn on a 1280-wide picture, but the map area is only 960 wide
 *                 (the right 320 px is the SidePanel). So every x is multiplied by 960 / 1280 = 0.75.
 */
public class GameMap {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 720;
    public static final int CELL = 40;

    private static final double FILE_WIDTH = 1280.0;
    private static final double PATH_CLEARANCE = 38;

    // Decorative roads that enemies do not walk: x1, y1, x2, y2 on the 1280x720 file canvas
    private static final double[][] EXTRA_ROADS = {
            {314, 0, 314, 720},      // left vertical road, full height
            {1068, 0, 1068, 720},    // right vertical road, full height
            {686, 0, 686, 119},      // top stub
            {686, 597, 686, 720},    // bottom stub
            {1068, 358, 1280, 358}   // middle road, right end
    };

    // Dead-end roads closed off by an obstacle. Drawn like the other roads, but towers may be
    // built on them (except on the obstacle itself, see OBSTACLES). Same 1280x720 file canvas.
    private static final double[][] DEAD_END_ROADS = {
            {1068, 119, 1280, 119},  // top road, right of the turn (chevron barricades)
            {0, 358, 314, 358},      // middle road, left end (crates)
            {0, 597, 314, 597}       // bottom road, left end (broken car)
    };

    // Things drawn on the map that towers can't sit on: left, top, right, bottom in map
    // pixels (960x720, already squeezed, unlike the road lists above)
    private static final double[][] OBSTACLES = {
            {865, 78, 900, 158},     // chevron barricades, top right
            {118, 313, 205, 400},    // crates, middle left
            {63, 560, 186, 636}      // broken car, bottom left
    };
    private static final double OBSTACLE_MARGIN = 10;

    private final List<double[]> points = new ArrayList<>();
    private final List<Double> cumulative = new ArrayList<>();
    private double totalLength;

    /**
     * [PURPOSE]       Reads the "x,y" waypoint lines from the CSV (skipping the header) and scales x to fit 960 px.
     * [OOP / PATTERN] Static Factory Method / CSV Parsing.
     * [CALLED BY]     btd/ui/AppController.java (loadData()).
     * [CONNECTS TO]   btd/core/GameMap constructor.
     */
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

    /**
     * [PURPOSE]       Constructs GameMap and precalculates cumulative Euclidean distances along path segments.
     * [OOP / PATTERN] Constructor / Spatial Precomputation.
     * [CALLED BY]     btd/core/GameMap.java (load()).
     * [DEFENSE NOTE]  cumulative.get(i) = distance from the start to waypoint i. Computing it once here
     *                 means pointAt() does not have to re-measure the path for every enemy every frame.
     */
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

    /** [PURPOSE] Total length of entire enemy walking track in pixels. */
    public double getTotalLength() {
        return totalLength;
    }

    /** [PURPOSE] Waypoint list coordinates defining the path vertices. */
    public List<double[]> getPoints() {
        return points;
    }

    /**
     * [PURPOSE] Decorative roads (EXTRA_ROADS + DEAD_END_ROADS), scaled to 960 px. Enemies never walk them.
     *           Only used by GamePanel.paintPath() to draw roads when map.png is missing.
     *           (Building is blocked on EXTRA_ROADS but allowed on DEAD_END_ROADS; see canBuildAt().)
     */
    public List<double[]> getExtraRoads() {
        List<double[]> result = new ArrayList<>();
        for (double[] r : EXTRA_ROADS) {
            result.add(scaleRoad(r));
        }
        for (double[] r : DEAD_END_ROADS) {
            result.add(scaleRoad(r));
        }
        return result;
    }

    /** [PURPOSE] Scales coordinate array from 1280px to 960px width. */
    private static double[] scaleRoad(double[] r) {
        return new double[]{r[0] * WIDTH / FILE_WIDTH, r[1], r[2] * WIDTH / FILE_WIDTH, r[3]};
    }

    /**
     * [PURPOSE]       Turns "distance walked" into an {x, y} position on the path.
     * [OOP / PATTERN] Linear Interpolation (LERP) along a polyline.
     * [CALLED BY]     btd/model/enemy/Enemy.java (update(), twice: once for the position, once 1 px ahead for the angle).
     * [CONNECTS TO]   cumulative list.
     * [DEFENSE NOTE]  1) walk the waypoints until cumulative[i] >= distance (simple loop, not binary search),
     *                 2) t = how far along that segment, from 0.0 to 1.0,
     *                 3) position = a + (b - a) * t.
     *                 Enemies only store one number (distance), and this method turns it into x and y.
     */
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

    /**
     * [PURPOSE]       Calculates shortest perpendicular distance from query point (x, y) to the nearest track segment.
     * [OOP / PATTERN] Point-to-Polyline Geometric Distance Calculation.
     * [CALLED BY]     btd/core/GameMap.java (canBuildAt()).
     */
    public double distanceToPath(double x, double y) {
        double best = Double.MAX_VALUE;
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            best = Math.min(best, distanceToSegment(x, y, a[0], a[1], b[0], b[1]));
        }
        return best;
    }

    /**
     * [PURPOSE]       Validates whether a tower can be placed at (x, y): within boundaries, off roads, and clear of obstacles.
     * [OOP / PATTERN] Spatial Collision Validation / Guard Method.
     * [CALLED BY]     btd/core/GameSession.java (canPlaceAt()) only.
     * [CONNECTS TO]   EXTRA_ROADS, OBSTACLES, PATH_CLEARANCE.
     * [DEFENSE NOTE]  Rejects a spot if: it is within 20 px of the map edge or above y = 60 (the HUD bar),
     *                 it is closer than 38 px to the enemy path or an EXTRA_ROAD, or it is inside an obstacle box (+10 px).
     *                 Tower-to-tower spacing is NOT checked here; GameSession.canPlaceAt() adds that.
     */
    public boolean canBuildAt(double x, double y) {
        if (!(x >= 20 && x <= WIDTH - 20 && y >= 60 && y <= HEIGHT - 20)
                || distanceToPath(x, y) < PATH_CLEARANCE) {
            return false;
        }
        for (double[] road : EXTRA_ROADS) {
            double[] r = scaleRoad(road);
            if (distanceToSegment(x, y, r[0], r[1], r[2], r[3]) < PATH_CLEARANCE) {
                return false;
            }
        }
        for (double[] o : OBSTACLES) {
            if (x >= o[0] - OBSTACLE_MARGIN && x <= o[2] + OBSTACLE_MARGIN
                    && y >= o[1] - OBSTACLE_MARGIN && y <= o[3] + OBSTACLE_MARGIN) {
                return false;
            }
        }
        return true;
    }

    /**
     * [PURPOSE] Shortest distance from point P to the line segment A-B.
     *           t = where P projects onto the line (0 = at A, 1 = at B), clamped to 0..1 so it stays on the segment.
     */
    private static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }
}
