package jake2.client.particles;

import jake2.client.ParticleSystem;

public final class ParticleEffects {

    private ParticleEffects() {}

    /**
     * Spawns a radial burst of {@code count} particles at (cx, cy, cz).
     * Directions are uniform on a unit sphere; speed is in
     * {@code [0.3*speed, speed]}; lifetime is in
     * {@code [lifespanMin, lifespanMax]}.
     *
     * <p>No-op if the particle subsystem is disabled via cl_new_particles.
     */

    // ===== Generic burst utility =====
    public static void spawnBurst(float cx, float cy, float cz,
                                  int count, float speed,
                                  float lifespanMin, float lifespanMax,
                                  int colorRgb, float sz) {
        if (!ParticleSystem.isEnabled()) return;

        for (int i = 0; i < count; i++) {
            float theta = (float) (Math.random() * Math.PI * 2);
            float phi   = (float) Math.acos(2.0 * Math.random() - 1.0);
            float dirX  = (float) (Math.sin(phi) * Math.cos(theta));
            float dirY  = (float) (Math.sin(phi) * Math.sin(theta));
            float dirZ  = (float) Math.cos(phi);

            float sp = speed * (0.3f + 0.7f * (float) Math.random());
            float ls = lifespanMin + (lifespanMax - lifespanMin) * (float) Math.random();

            ParticleSystem.spawn(cx, cy, cz,
                    dirX * sp, dirY * sp, dirZ * sp,
                    ls, colorRgb, sz);
        }
    }

    // ===== Named effects =====

    /** Orange explosion — rockets, grenades, plasma, barrels. */
    public static void explosion(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                250, 450f, 0.6f, 1.8f, 0xFF8800, 8f);
        spawnBurst(origin[0], origin[1], origin[2],
                80, 600f, 0.2f, 0.5f, 0xFFFF44, 5f);
    }

    /** Green BFG explosion. */
    public static void bfgExplosion(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                300, 550f, 0.8f, 2.5f, 0x00FF00, 9f);
        spawnBurst(origin[0], origin[1], origin[2],
                100, 700f, 0.3f, 0.7f, 0xAAFFAA, 6f);
    }

    /** Dark red blood spray. */
    public static void blood(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                70, 180f, 0.3f, 0.9f, 0xCC0000, 5f);
    }

    /** Green blood (alien monsters). */
    public static void greenBlood(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                60, 180f, 0.3f, 0.9f, 0x00CC00, 5f);
    }

    /** Yellow sparks — bullet hits on metal. */
    public static void sparks(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                40, 350f, 0.2f, 0.7f, 0xFFFF88, 3f);
    }

    /** Grey smoke — rocket trails, steam, explosion residue. */
    public static void smoke(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                15, 40f, 1.5f, 3.0f, 0x888888, 10f);
    }

    /** Blue/white water splash. */
    public static void waterSplash(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                60, 250f, 0.4f, 1.2f, 0xAADDFF, 6f);
    }

    /** Cyan electric sparks — tesla, electric traps. */
    public static void electricSparks(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                50, 400f, 0.15f, 0.5f, 0x88DDFF, 3f);
    }

    /** Green blaster trail. */
    public static void blasterTrail(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                8, 60f, 0.2f, 0.6f, 0x44FF88, 4f);
    }

    /** Magenta rail trail. */
    public static void railTrail(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                30, 120f, 0.5f, 1.5f, 0xCC88FF, 6f);
    }

    /** White/blue teleport effect. */
    public static void teleport(float[] origin) {
        spawnBurst(origin[0], origin[1], origin[2],
                150, 300f, 0.5f, 1.5f, 0xCCCCFF, 7f);
        spawnBurst(origin[0], origin[1], origin[2],
                50, 500f, 0.2f, 0.6f, 0xFFFFFF, 4f);
    }
}
