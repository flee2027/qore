package jake2.qcommon.imageio;

import jake2.qcommon.filesystem.FS;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Minimal PNG loader for qore.
 * Reads bytes via FS.LoadFile (works with .pak and disk),
 * decodes to raw RGBA (4 bytes per pixel), ready for GL_LoadPic(bits=32).
 *
 * Returns null if the file is missing or is not a valid image.
 */
public final class PNGImageLoader {

    public static final class Result {
        public final int width;
        public final int height;
        public final byte[] rgba; // width * height * 4, top-down, R,G,B,A
        public Result(int w, int h, byte[] p) { width = w; height = h; rgba = p; }
    }

    private PNGImageLoader() {}

    public static Result load(String name) {
        byte[] raw = FS.LoadFile(name);
        if (raw == null) return null;

        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
            if (img == null) return null;

            int w = img.getWidth();
            int h = img.getHeight();
            byte[] rgba = new byte[w * h * 4];

            int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
            for (int i = 0; i < pixels.length; i++) {
                int argb = pixels[i];
                rgba[i * 4]     = (byte) ((argb >> 16) & 0xFF); // R
                rgba[i * 4 + 1] = (byte) ((argb >> 8)  & 0xFF); // G
                rgba[i * 4 + 2] = (byte) ( argb        & 0xFF); // B
                rgba[i * 4 + 3] = (byte) ((argb >>> 24) & 0xFF); // A
            }
            return new Result(w, h, rgba);
        } catch (IOException e) {
            return null;
        }
    }
}