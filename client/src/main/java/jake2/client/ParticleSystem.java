package jake2.client;

import jake2.client.render.image_t;
import jake2.qcommon.exec.Cvar;
import jake2.qcommon.exec.cvar_t;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static jake2.qcommon.Defines.MAX_PARTICLES;

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
    private static final int[] rgb     = new int[MAX_PARTICLES];
    private static final boolean[] active = new boolean[MAX_PARTICLES];
    private static final byte[] type = new byte[MAX_PARTICLES]; // 0 = point, 1 = billboard
    private static final int[] texId = new int[MAX_PARTICLES]; // 0 = default, >0 = custom texture

    private static int fireFlyTexnum = 0;
    private static boolean texLookupFailed = false;

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

    public static int getFireFlyTexnum() {
        if (fireFlyTexnum != 0 || texLookupFailed) return fireFlyTexnum;

        image_t img = ClientGlobals.re.RegisterPic("/pics/billboard/firefly.png");

        if (img == null) {
            System.err.println("WARNING: Could not load /pics/billboard/firefly.png!");
            texLookupFailed = true;
            return 0;
        }

        fireFlyTexnum = img.texnum;
        return fireFlyTexnum;
    }

    public static void invalidateTextures(){
        fireFlyTexnum = 0;
        texLookupFailed = false;
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

        type[slot] = 0;
        texId[slot] = 0;

        active[slot] = true;
    }

    /**
     * Spawns a billboard particle (e.g., firefly).
     */
    public static void spawnBillboard(float x, float y, float z,
                                      float dx, float dy, float dz,
                                      float lifespan, int colorRgb, float sz, int textureId) {
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

        type[slot] = 1;
        texId[slot] = textureId;

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

            if (type[i] == 0) {

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
            } else if (type[i] == 1) {
                life[i] -= dt;

                if (life[i] <= 0f) {
                    active[i] = false;
                    continue;
                }

                vy[i] = (float) Math.sin(ClientGlobals.cl.time * 2.0f + i) * 15.0f;

                px[i] += vx[i] * dt;
                py[i] += vy[i] * dt;
                pz[i] += vz[i] * dt;

                if (i % 4 == 0) {
                    float lifeRatio = getLifeRatio(i);
                    float currentIntensity = 210.0f;

                    if (lifeRatio < 0.2f) {
                        currentIntensity = 210.0f * (lifeRatio / 0.2f);
                    }

                    int c = rgb[i];
                    float r = ((c >> 16) & 0xFF) / 255.0f;
                    float g = ((c >> 8) & 0xFF) / 255.0f;
                    float b = (c & 0xFF) / 255.0f;

                    int key = -2000 - i;
                    jake2.client.CL_fx.cdlight_t dl = jake2.client.CL_fx.AllocDlight(key);

                    if (dl != null) {
                        dl.origin[0] = px[i];
                        dl.origin[1] = py[i];
                        dl.origin[2] = pz[i];
                        dl.color[0] = r;
                        dl.color[1] = g;
                        dl.color[2] = b;
                        dl.radius = currentIntensity;

                        dl.die = ClientGlobals.cl.time + 100.0f;
                    }
                }
            }
        }
    }

    public static void render() {
        if (!isEnabled()) return;

        int pointCount = 0;
        vertexBuffer.clear();
        colorBuffer.clear();

        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (!active[i] || type[i] != 0) continue;

            vertexBuffer.put(px[i]).put(py[i]).put(pz[i]);
            float a = getLifeRatio(i);
            int c = rgb[i];
            colorBuffer.put((byte) ((c >> 16) & 0xFF));
            colorBuffer.put((byte) ((c >> 8)  & 0xFF));
            colorBuffer.put((byte) ( c        & 0xFF));
            colorBuffer.put((byte) (a * 255));
            pointCount++;
        }

        if (pointCount > 0) {
            vertexBuffer.flip();
            colorBuffer.flip();

            GL11.glVertexPointer(3, 0, vertexBuffer);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glColorPointer(4, true, 0, colorBuffer);

            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPointSize(4.0f);

            GL11.glDrawArrays(GL11.GL_POINTS, 0, pointCount);

            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        int billboardCount = 0;
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (active[i] && type[i] == 1) billboardCount++;
        }

        if (billboardCount > 0) {
            int tex = getFireFlyTexnum();

            if (tex != 0) {
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glDepthMask(false);

                for (int i = 0; i < MAX_PARTICLES; i++) {
                    if (!active[i] || type[i] != 1) continue;

                    float lifeRatio = getLifeRatio(i);
                    int c = rgb[i];
                    float r = ((c >> 16) & 0xFF) / 255.0f;
                    float g = ((c >> 8)  & 0xFF) / 255.0f;
                    float b = ( c        & 0xFF) / 255.0f;

                    float alpha = 1.0f;
                    float currentSize = size[i];
                    float growDuration = 0.2f;

                    if (lifeRatio > (1.0f - growDuration)) {
                        float t = (1.0f - lifeRatio) / growDuration;

                        float ease = t * t;

                        alpha = ease;
                        currentSize = size[i] * ease;
                    }

                    else if (lifeRatio < 0.3f) {
                        float t = lifeRatio / 0.3f;

                        float ease = t * t;

                        alpha = ease;

                        currentSize = size[i] * ease;
                    }

                    GL11.glColor4f(r, g, b, alpha);
                    drawBillboardQuad(px[i], py[i], pz[i], currentSize);
                }

                GL11.glDepthMask(true);
                GL11.glDisable(GL11.GL_BLEND);
            }
        }
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Removes all particles (e.g. on map change). */
    public static void clear() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            active[i] = false;
            type[i] = 0;
            texId[i] = 0;
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

    private static void drawBillboardQuad(float x, float y, float z, float size) {
        float half = size / 2.0f;
        float[] right = ClientGlobals.cl.v_right;
        float[] up    = ClientGlobals.cl.v_up;

        GL11.glBegin(GL11.GL_QUADS);

        // Top-Left
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(x - right[0]*half + up[0]*half,
                y - right[1]*half + up[1]*half,
                z - right[2]*half + up[2]*half);

        // Top-Right
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(x + right[0]*half + up[0]*half,
                y + right[1]*half + up[1]*half,
                z + right[2]*half + up[2]*half);

        // Bottom-Right
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(x + right[0]*half - up[0]*half,
                y + right[1]*half - up[1]*half,
                z + right[2]*half - up[2]*half);

        // Bottom-Left
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(x - right[0]*half - up[0]*half,
                y - right[1]*half - up[1]*half,
                z - right[2]*half - up[2]*half);

        GL11.glEnd();
    }
}