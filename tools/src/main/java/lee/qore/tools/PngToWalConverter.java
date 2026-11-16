package lee.qore.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal PNG -> WAL converter for Quake II.
 * Only supports power-of-two dimensions and ignores alpha.
 * Uses the real Quake II palette (palette.lmp) for color lookup.
 */
public class PngToWalConverter {

    // Full WAL header size in bytes (name+width+height+offsets+next_name+flags+contents+value).
    private static final int HEADER_SIZE = 100;

    // Default palette path. Override with args[2] if needed.
    private static final String DEFAULT_PALETTE_PATH = "F:/Quake II/baseq2/palette.lmp";

    // 768 bytes = 256 colors * 3 channels (R, G, B).
    private static final byte[] PALETTE = new byte[768];

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: PngToWalConverter <input.png> <output.wal> [palette.lmp]");
            System.exit(1);
        }
        String palettePath = args.length >= 3 ? args[2] : DEFAULT_PALETTE_PATH;
        loadPalette(new File(palettePath));
        convert(new File(args[0]), new File(args[1]));
    }

    /** Reads palette.lmp into PALETTE. Call once before convert(). */
    private static void loadPalette(File f) throws IOException {
        if (!f.exists()) {
            throw new IOException("Palette not found: " + f.getAbsolutePath());
        }
        try (InputStream in = new FileInputStream(f)) {
            int read = in.readNBytes(PALETTE, 0, PALETTE.length);
            if (read != PALETTE.length) {
                throw new IOException("palette.lmp is " + read + " bytes, expected 768");
            }
        }
    }

    public static void convert(File pngFile, File walFile) throws IOException {
        BufferedImage img = ImageIO.read(pngFile);
        if (img == null) throw new IOException("Not a PNG: " + pngFile);

        int w = img.getWidth();
        int h = img.getHeight();
        if (!isPowerOfTwo(w) || !isPowerOfTwo(h)) {
            throw new IllegalArgumentException("WAL requires power-of-two dimensions: " + w + "x" + h);
        }

        // 1. Find the nearest palette index for each pixel.
        byte[] indices = new byte[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                indices[y * w + x] = (byte) nearestPaletteIndex(r, g, b);
            }
        }

        // 2. Build WAL header (exactly HEADER_SIZE bytes).
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);

        String texName = walFile.getName();
        int dot = texName.lastIndexOf('.');
        if (dot >= 0) texName = texName.substring(0, dot);
        byte[] nameBytes = texName.getBytes(StandardCharsets.US_ASCII);
        byte[] name = new byte[32];
        System.arraycopy(nameBytes, 0, name, 0, Math.min(31, nameBytes.length));
        header.put(name);

        header.putInt(w);
        header.putInt(h);

        int offset0 = HEADER_SIZE;
        int offset1 = offset0 + w * h;
        int offset2 = offset1 + (w / 2) * (h / 2);
        int offset3 = offset2 + (w / 4) * (h / 4);
        header.putInt(offset0).putInt(offset1).putInt(offset2).putInt(offset3);

        header.put(new byte[32]);
        header.putInt(0);
        header.putInt(0);
        header.putInt(0);

        // 3. Write header + four mipmap levels.
        try (OutputStream out = new FileOutputStream(walFile)) {
            out.write(header.array());
            writeMipmap(out, indices, w, h, 1);
            writeMipmap(out, indices, w, h, 2);
            writeMipmap(out, indices, w, h, 4);
            writeMipmap(out, indices, w, h, 8);
        }
    }

    /**
     * Finds the palette index whose RGB is closest to (r, g, b).
     * Distance is squared Euclidean — no sqrt needed since we only compare.
     */
    private static int nearestPaletteIndex(int r, int g, int b) {
        int bestIdx = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int p = 0; p < 256; p++) {
            int pr = PALETTE[p * 3] & 0xFF;
            int pg = PALETTE[p * 3 + 1] & 0xFF;
            int pb = PALETTE[p * 3 + 2] & 0xFF;
            int dr = r - pr;
            int dg = g - pg;
            int db = b - pb;
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = p;
            }
        }
        return bestIdx;
    }

    private static void writeMipmap(OutputStream out, byte[] src, int w, int h, int step) throws IOException {
        int mw = w / step;
        int mh = h / step;
        for (int y = 0; y < mh; y++) {
            for (int x = 0; x < mw; x++) {
                out.write(src[(y * step) * w + (x * step)]);
            }
        }
    }

    private static boolean isPowerOfTwo(int v) {
        return v > 0 && (v & (v - 1)) == 0;
    }
}