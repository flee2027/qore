package jake2.client;

import jake2.qcommon.exec.Cvar;
import jake2.qcommon.exec.cvar_t;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Particle system for qore.
 *
 * Principles:
 * - Fixed pool, no allocations in the frame (discipline + ZGC-friendly).
 * - Primitive fields (float, int) — no objects in the hot path.
 * - Spawn/update/death — pure logic, easy to test.
 *
 */
public final class ParticleSystem {

    private static cvar_t cl_new_particles;

    public static final int MAX_PARTICLES = 2048;

    /** Downward gravity along Z (in Quake Z is up). Tune to the world scale. */
    private static final float GRAVITY = 400.0f;

    // Pool: primitive fields, objects are created once in <clinit>
    private static final float[] px      = new float[MAX_PARTICLES];
    private static final float[] py      = new float[MAX_PARTICLES];
    private static final float[] pz      = new float[MAX_PARTICLES];
    private static final float[] vx      = new float[MAX_PARTICLES];
    private static final float[] vy      = new float[MAX_PARTICLES];
    private static final float[] vz      = new float[MAX_PARTICLES];
    private static final float[] life    = new float[MAX_PARTICLES];
    private static final float[] maxLife = new float[MAX_PARTICLES];
    private static final float[] size    = new float[MAX_PARTICLES];
    private static final int[]   rgb     = new int[MAX_PARTICLES];
    private static final boolean[] active = new boolean[MAX_PARTICLES];

    /** Round-robin cursor for O(1) slot search on average. */
    private static int cursor = 0;

    private static final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(MAX_PARTICLES * 3);
    private static final ByteBuffer colorBuffer  = BufferUtils.createByteBuffer(MAX_PARTICLES * 4);

    private ParticleSystem() {}

    public static void Init(){
        cl_new_particles = Cvar.getInstance().Get("cl_new_particles", "1", 0);
    }

    public static boolean isEnabled(){
        return cl_new_particles != null && cl_new_particles.value != 0f;
    }

    /**
     * Spawns a single particle. If the pool is full, overwrites the "oldest"
     * one (round-robin) so fresh effects are always visible.
     */
    public static void spawn(float x, float y, float z,
                             float dx, float dy, float dz,
                             float lifespan, int colorRgb, float sz) {
        int slot = findFreeSlot();
        px[slot] = x;
        py[slot] = y;
        pz[slot] = z;
        vx[slot] = dx;
        vy[slot] = dy;
        vz[slot] = dz;
        life[slot] = lifespan;
        maxLife[slot] = lifespan;
        rgb[slot] = colorRgb;
        size[slot] = sz;
        active[slot] = true;
    }

    /**
     * Round-robin free-slot search: usually O(1) because cursor advances each
     * time and most slots are dead under normal load.
     */
    private static int findFreeSlot() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            int idx = (cursor + i) % MAX_PARTICLES;
            if (!active[idx]) {
                cursor = (idx + 1) % MAX_PARTICLES;
                return idx;
            }
        }
        // Pool is full — overwrite the "oldest" one (per cursor)
        int idx = cursor;
        cursor = (cursor + 1) % MAX_PARTICLES;
        return idx;
    }

    /**
     * Physics update. Called every frame from UpdateScreen2.
     *
     * @param dt frame time in seconds (cls.frametime)
     */
    public static void update(float dt) {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (!active[i]) continue;

            life[i] -= dt;
            if (life[i] <= 0f) {
                active[i] = false;
                continue;
            }

            // Simple Newtonian physics (gravity down along Z, as in Quake)
            vy[i] -= GRAVITY * dt;
            px[i] += vx[i] * dt;
            py[i] += vy[i] * dt;
            pz[i] += vz[i] * dt;

            // TODO for the future: wall collision, "sparks bounce"
            // — that requires server access, not needed for now.
        }
    }

    public static void render() {
        if (cl_new_particles.value == 0f) return;

        int n = 0;
        vertexBuffer.clear();
        colorBuffer.clear();

        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (!active[i]) continue;

            vertexBuffer.put(px[i]).put(py[i]).put(pz[i]);

            float a = getLifeRatio(i);
            int c = rgb[i];
            colorBuffer.put((byte) ((c >> 16) & 0xFF)); // R
            colorBuffer.put((byte) ((c >> 8)  & 0xFF)); // G
            colorBuffer.put((byte) ( c        & 0xFF)); // B
            colorBuffer.put((byte) (a * 255));          // A
            n++;
        }

        if (n == 0) return;

        vertexBuffer.flip();
        colorBuffer.flip();

        // GL_VERTEX_ARRAY is already enabled globally — only set pointer
        GL11.glVertexPointer(3, 0, vertexBuffer);

        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glColorPointer(4, true, 0, colorBuffer);

        // Same local state pattern as R_DrawParticles()
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glPointSize(4.0f);

        GL11.glDrawArrays(GL11.GL_POINTS, 0, n);

        // Restore states — paired with the enables above
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /** Removes all particles (e.g. on map change). */
    public static void clear() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            active[i] = false;
        }
        cursor = 0;
    }

    // ===== Accessors for the renderer (Step 3) =====

    public static int count() {
        int n = 0;
        for (int i = 0; i < MAX_PARTICLES; i++) if (active[i]) n++;
        return n;
    }

    public static boolean isActive(int i)   { return active[i]; }
    public static float   getX(int i)       { return px[i]; }
    public static float   getY(int i)       { return py[i]; }
    public static float   getZ(int i)       { return pz[i]; }
    public static int     getColor(int i)   { return rgb[i]; }
    public static float   getSize(int i)    { return size[i]; }

    /** 1.0 → just born, 0.0 → dying now. Handy for alpha. */
    public static float getLifeRatio(int i) { return life[i] / maxLife[i]; }
}