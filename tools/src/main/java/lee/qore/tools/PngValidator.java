package lee.qore.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Scans a directory tree for PNG files and reports texture-pipeline issues:
 * - non-power-of-two dimensions (auto-resized by PngToWalConverter)
 * - alpha channel present (WAL ignores alpha, so transparency is lost)
 * - oversized textures (> 512 px, may hurt performance)
 *
 * Usage: PngValidator <textures-dir>
 */
public class PngValidator {

    // ---- stats collected across the scan ----
    private static int totalPng = 0;
    private static int npotCount = 0;
    private static int alphaCount = 0;
    private static int largeCount = 0;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: PngValidator <textures-dir>");
            System.exit(1);
        }
        File root = new File(args[0]);
        if (!root.isDirectory()) {
            System.err.println("Not a directory: " + root.getAbsolutePath());
            System.exit(1);
        }

        System.out.println("Scanning " + root.getAbsolutePath());
        System.out.println("---");
        validate(root);
        System.out.println("---");
        System.out.println("Total: " + totalPng + " PNG, "
                + npotCount + " NPOT, "
                + alphaCount + " alpha, "
                + largeCount + " large");
    }

    /**
     * Recursively walks the directory tree and validates each .png.
     */
    private static void validate(File file) throws IOException {
        if (file.isDirectory()){
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) validate(child);
            }
        }
        else {
            if (file.isFile()){
                if (file.getName().toLowerCase().endsWith(".png")){
                    totalPng++;
                    BufferedImage img = ImageIO.read(file);
                    if (img == null) {
                        System.out.println("  " + file.getName() + ": not a valid image, skipping");
                        return;
                    }
                    checkImage(file, img);
                }
            }
        }
    }

    /**
     * Validates a single loaded image and prints its report line.
     */
    private static void checkImage(File file, BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        StringBuilder issues = new StringBuilder();

        if (!isPowerOfTwo(w) || !isPowerOfTwo(h)){
            npotCount++;
            if (issues.length() > 0) issues.append(", ");
            issues.append("NPOT, will resize to ")
                    .append(nextPowerOfTwo(w)).append("x").append(nextPowerOfTwo(h));
        }

        if (img.getColorModel().hasAlpha()){
            alphaCount++;
            if (issues.length() > 0) issues.append(", ");
            issues.append("has alpha (dropped by WAL)");
        }

        if (w > 512 || h > 512){
            largeCount++;
            if (issues.length() > 0) issues.append(", ");
            issues.append("large, may hurt performance");
        }

        System.out.println(file.getName() + ": " + w + "x" + h + (issues.isEmpty() ? " OK" : " (" + issues.toString() + ")"));
    }

    private static boolean isPowerOfTwo(int v) {
        return v > 0 && (v & (v - 1)) == 0;
    }

    /** Smallest power of two >= v (e.g. 100 -> 128, 64 -> 64). */
    private static int nextPowerOfTwo(int v) {
        int p = 1;
        while (p < v) p <<= 1;
        return p;
    }
}