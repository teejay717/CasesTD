package btd.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * [PURPOSE]       Represents the gameplay map, track waypoints, cumulative track distance, and tower build validation.
 * [OOP / PATTERN] Spatial Domain Model / Value Object / Boundary Enforcer.
 * [CALLED BY]     btd/Main.java (main()), btd/core/GameEngine.java, btd/core/GameSession.java, btd/ui/screens/GamePanel.java.
 * [CONNECTS TO]   data/maps/map1_path.csv, btd/model/enemy/Enemy.java.
 * [DEFENSE NOTE]  Waypoints loaded from CSV (1280x720 file canvas) are squeezed to fit active 960x720 canvas width.
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
     * [PURPOSE]       Reads CSV file of waypoint coordinates and scales them to fit canvas.
     * [OOP / PATTERN] Static Factory Method / CSV Parsing.
     * [CALLED BY]     btd/Main.java (main()).
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
     * [DEFENSE NOTE]  Precomputing cumulative lengths enables O(log N) or fast linear waypoint lookup in pointAt().
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

    /** [PURPOSE] Scaled auxiliary road segments where towers cannot be built. */
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
     * [PURPOSE]       Computes exact {x, y} coordinate on the track for a given distance traveled in pixels.
     * [OOP / PATTERN] Linear Interpolation (LERP) across polyline segments.
     * [CALLED BY]     btd/model/enemy/Enemy.java (update()).
     * [CONNECTS TO]   cumulative distance array.
     * [DEFENSE NOTE]  How pointAt works step-by-step:
     *                 1. Locates the two corner waypoints a (behind) and b (ahead) that sandwich the zombie
     *                    using precomputed cumulative distances.
     *                 2. Computes segment length = cumulative[i] - cumulative[i-1].
     *                 3. Computes progress ratio t = (distance - cumulative[i-1]) / segment (value from 0.0 to 1.0).
     *                 4. Uses LERP formula a + (b - a) * t to blend {x, y} coordinates along that segment.
     *                 Returns new double[]{ x, y } where p[0] is X pixel and p[1] is Y pixel.
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
     * [CALLED BY]     btd/core/GameSession.java (canPlaceAt()), btd/ui/screens/GamePanel.java (paintPlacementPreview()).
     * [CONNECTS TO]   EXTRA_ROADS, OBSTACLES, PATH_CLEARANCE.
     * [DEFENSE NOTE]  Enforces 38px track clearance, map margins, top HUD avoidance, and obstacle bounding boxes.
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
     * [SIMPLIFIED]    Checks how close your mouse is to the road so you cannot build towers on top of walking zombies.
     * [PURPOSE]       Calculates the shortest pixel distance from cursor (px, py) to a road segment between (ax, ay) and (bx, by).
     * [OOP / PATTERN] Vector Projection Collision Check.
     * [CALLED BY]     btd/core/GameMap.java (distanceToPath(), canBuildAt()).
     * [CONNECTS TO]   canBuildAt(), PATH_CLEARANCE (38px threshold).
     * [DEFENSE NOTE]  Formula: t = clamp[0, 1](((p - a) · d) / |d|^2), distance = |p - (a + t · d)| where d = b - a.
     *                 If distance < 38 pixels (PATH_CLEARANCE), canBuildAt() rejects tower placement.
     * 
     */

    // It measures the pixel distance from your mouse cursor to the zombie road; 
    // if you are closer than 38 pixels, the game blocks you from placing the tower.
    
    private static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax; // Road direction & length from point A to B
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;
        // How far along the road the mouse is closest to (0 = start, 0.5 = middle, 1 = end)
        double t = lengthSquared == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t)); // Keep within road endpoints (clamp between 0 and 1)
        // Distance in pixels from mouse to that closest spot on the road
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }
}
