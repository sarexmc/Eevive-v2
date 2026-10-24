package com.revivalcampfire;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Efectos visuales del ritual (solo cliente). Linea de tiempo (ticks):
 *   0-130   anillo de energia en el cielo que desciende sobre la fogata
 *   40-400  campo de estrellas / luciernagas
 *   100-390 ondas concentricas teal/amarillas + brazos espirales + arcos orbitando
 *   240-300 destello blanco en cruz (overlay)
 *   255-350 haz de luz con rayos y la figura ascendiendo
 *   340-420 la energia se disipa
 */
public class ClientRitual {
    private static final double TAU = Math.PI * 2;
    private static final RandomSource R = RandomSource.create();

    private static final Vector3f TEAL = new Vector3f(0.05f, 0.90f, 0.80f);
    private static final Vector3f CYAN = new Vector3f(0.20f, 0.65f, 1.00f);
    private static final Vector3f YELLOW = new Vector3f(1.00f, 0.90f, 0.10f);
    private static final Vector3f LIME = new Vector3f(0.60f, 1.00f, 0.20f);

    private static final List<Active> ACTIVE = new ArrayList<>();

    private static class Active {
        final Vec3 c;
        int age = 0;
        Active(Vec3 c) { this.c = c; }
    }

    public static void start(Vec3 center) {
        ACTIVE.add(new Active(center));
    }

    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            return;
        }
        if (mc.isPaused()) return;
        Iterator<Active> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            a.age++;
            spawn(mc.level, a);
            if (a.age >= RitualManager.TOTAL) it.remove();
        }
    }

    private static void dust(Level l, Vector3f col, float size, double x, double y, double z) {
        l.addParticle(new DustParticleOptions(col, size), x, y, z, 0, 0, 0);
    }

    private static void spawn(Level l, Active a) {
        final int t = a.age;
        final double cx = a.c.x, cy = a.c.y, cz = a.c.z;

        // ---- anillo en el cielo ----
        if (t <= 130) {
            double d = t < 40 ? 0 : Math.min(1.0, (t - 40) / 90.0);
            double ease = d * d * (3 - 2 * d);
            double ry = cy + 15 - ease * 14.5;
            double rot = t * 0.18;
            for (int i = 0; i < 18; i++) {
                double ang = rot + i * TAU / 18.0;
                dust(l, TEAL, 1.0f, cx + Math.cos(ang) * 3.6, ry, cz + Math.sin(ang) * 3.6);
                double ang2 = -rot * 1.3 + i * TAU / 18.0;
                dust(l, YELLOW, 0.9f, cx + Math.cos(ang2) * 2.7, ry + 0.15, cz + Math.sin(ang2) * 2.7);
            }
            l.addParticle(ParticleTypes.END_ROD, cx, ry, cz, 0, 0, 0);
        }

        // ---- campo de estrellas ----
        if (t >= 40 && t <= 400) {
            float k = t < 100 ? (t - 40) / 60f : (t > 370 ? (400 - t) / 30f : 1f);
            int n = (int) (18 * k);
            for (int i = 0; i < n; i++) {
                double ang = R.nextDouble() * TAU;
                double rad = Math.sqrt(R.nextDouble()) * 34;
                double px = cx + Math.cos(ang) * rad, pz = cz + Math.sin(ang) * rad;
                double py = cy + 3 + R.nextDouble() * 32;
                double vx = (R.nextDouble() - 0.5) * 0.006, vz = (R.nextDouble() - 0.5) * 0.006;
                switch (R.nextInt(4)) {
                    case 0 -> l.addParticle(ParticleTypes.END_ROD, px, py, pz, vx, 0.002, vz);
                    case 1 -> l.addParticle(ParticleTypes.GLOW, px, py, pz, vx, 0.0, vz);
                    case 2 -> dust(l, YELLOW, 0.5f, px, py, pz);
                    default -> dust(l, CYAN, 0.5f, px, py, pz);
                }
            }
        }

        // ---- ondas, espirales y arcos ----
        if (t >= 100 && t <= 390) {
            float k = Math.min(1f, (t - 100) / 30f) * (t > 360 ? (390 - t) / 30f : 1f);

            for (int ring = 0; ring < 4; ring++) {
                double r = ((t * 0.09 + ring * 3.0) % 12.0) + 0.6;
                int pts = (int) (10 + r * 4);
                Vector3f col = (ring % 2 == 0) ? TEAL : YELLOW;
                for (int i = 0; i < pts; i++) {
                    if (R.nextFloat() > k) continue;
                    double ang = R.nextDouble() * TAU;
                    dust(l, col, 1.0f, cx + Math.cos(ang) * r, cy - 0.4, cz + Math.sin(ang) * r);
                }
            }
            for (int arm = 0; arm < 3; arm++) {
                for (int s = 0; s < 11; s++) {
                    if (R.nextFloat() > k * 0.7f) continue;
                    double rr = 1.5 + s * 1.0;
                    double ang = t * 0.11 + arm * TAU / 3.0 + s * 0.4;
                    Vector3f col = s < 5 ? LIME : (s < 8 ? YELLOW : TEAL);
                    dust(l, col, 1.1f, cx + Math.cos(ang) * rr, cy - 0.35, cz + Math.sin(ang) * rr);
                }
            }
            for (int i = 0; i < 3; i++) {
                double tilt = 0.5 + i * 0.9, yaw = i * 2.1 + t * 0.01;
                for (int j = 0; j < 4; j++) {
                    double ang = t * (0.22 + 0.06 * i) + j * 0.15;
                    double ox = Math.cos(ang) * 1.5, oz = Math.sin(ang) * 1.5;
                    double y1 = -oz * Math.sin(tilt);
                    double z1 = oz * Math.cos(tilt);
                    double x2 = ox * Math.cos(yaw) + z1 * Math.sin(yaw);
                    double z2 = -ox * Math.sin(yaw) + z1 * Math.cos(yaw);
                    dust(l, LIME, 0.6f, cx + x2, cy + 1.1 + y1, cz + z2);
                }
            }
        }

        // ---- explosion radial en el destello ----
        if (t == 240) {
            for (int i = 0; i < 90; i++) {
                double yaw = R.nextDouble() * TAU, pitch = (R.nextDouble() - 0.3) * Math.PI;
                double sp = 0.3 + R.nextDouble() * 0.5;
                l.addParticle(ParticleTypes.END_ROD, cx, cy + 1, cz,
                        Math.cos(yaw) * Math.cos(pitch) * sp, Math.sin(pitch) * sp, Math.sin(yaw) * Math.cos(pitch) * sp);
            }
        }

        // ---- haz de luz y rayos ----
        double fy = cy + 1 + Math.min(Math.max(t - 255, 0), 85) * 0.05;
        if (t >= 255 && t <= 350) {
            for (int i = 0; i < 10; i++) {
                dust(l, R.nextBoolean() ? YELLOW : LIME, 1.4f,
                        cx + (R.nextDouble() - 0.5) * 0.6, cy + R.nextDouble() * 34, cz + (R.nextDouble() - 0.5) * 0.6);
            }
            for (int i = 0; i < 7; i++) {
                double yaw = R.nextDouble() * TAU, pitch = (R.nextDouble() - 0.5) * 1.8;
                double dx = Math.cos(yaw) * Math.cos(pitch), dy = Math.sin(pitch), dz = Math.sin(yaw) * Math.cos(pitch);
                l.addParticle(ParticleTypes.END_ROD, cx, fy, cz, dx * 0.35, dy * 0.35, dz * 0.35);
                double rr = R.nextDouble() * 6;
                dust(l, YELLOW, 0.9f, cx + dx * rr, fy + dy * rr, cz + dz * rr);
            }
        }

        // ---- disipacion ----
        if (t >= 340) {
            int n = (int) (20 * (RitualManager.TOTAL - t) / 80f);
            for (int i = 0; i < n; i++) {
                l.addParticle(ParticleTypes.END_ROD,
                        cx + (R.nextDouble() - 0.5) * 3, fy + (R.nextDouble() - 0.5) * 3, cz + (R.nextDouble() - 0.5) * 3,
                        0, 0.03, 0);
            }
        }
    }

    // ------------------------------------------------------------------
    //  Overlay: destello blanco/cian con estrella de cuatro puntas
    // ------------------------------------------------------------------
    private static float flash(float t) {
        if (t < 238 || t > 300) return 0f;
        float f = t < 246 ? (t - 238) / 8f : 1f - (t - 246) / 54f;
        return f * f;
    }

    public static void renderFlash(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ACTIVE.isEmpty()) return;
        float best = 0f;
        for (Active a : ACTIVE) {
            double dist = mc.player.position().distanceTo(a.c);
            float prox = (float) Mth.clamp((80 - dist) / 60.0, 0, 1);
            best = Math.max(best, flash(a.age + pt) * prox);
        }
        if (best <= 0.01f) return;

        int bg = (int) (Mth.clamp(best, 0, 1) * 0.55f * 255);
        g.fill(0, 0, w, h, (bg << 24) | 0x66E6FF);

        int a = (int) (Mth.clamp(best, 0, 1) * 255);
        int col = (a << 24) | 0xFFFFFF;
        int cxp = w / 2, cyp = h / 2 - h / 10;
        int halfW = (int) (w * 0.55f * best), halfH = (int) (h * 0.65f * best);
        if (halfW < 4 || halfH < 4) return;
        for (int dx = -halfW; dx <= halfW; dx += 2) {
            float f = 1f - Math.abs(dx) / (float) halfW;
            int th = Math.max(1, (int) (f * f * 12));
            g.fill(cxp + dx, cyp - th / 2, cxp + dx + 2, cyp + th / 2 + 1, col);
        }
        for (int dy = -halfH; dy <= halfH; dy += 2) {
            float f = 1f - Math.abs(dy) / (float) halfH;
            int th = Math.max(1, (int) (f * f * 12));
            g.fill(cxp - th / 2, cyp + dy, cxp + th / 2 + 1, cyp + dy + 2, col);
        }
    }
}
