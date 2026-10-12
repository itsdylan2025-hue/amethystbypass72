package com.chunkfinder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class ChunkRenderer {
    // #F050F8 for sus chunks, yellow for light finder hits
    private static final float[] PINK = {240f / 255f, 80f / 255f, 248f / 255f};
    private static final float[] YELLOW = {1f, 0.9f, 0.3f};
    private static final float HEIGHT_BELOW = 4f;
    private static final float HEIGHT_ABOVE = 20f;
    private static boolean failed = false;

    private ChunkRenderer() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(ChunkRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        if (failed) {
            return;
        }
        ChunkFinderConfig cfg = ChunkFinderConfig.get();
        if (cfg.alpha <= 0 || (!cfg.susChunkFinder && !cfg.lightFinder)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        try {
            PoseStack pose = ctx.matrices();
            MultiBufferSource consumers = ctx.consumers();
            if (pose == null || consumers == null) {
                return;
            }
            Vec3 cam = ctx.worldState().cameraRenderState.pos;
            VertexConsumer vc = consumers.getBuffer(RenderTypes.debugFilledBox());
            Matrix4f m = pose.last().pose();
            float a = cfg.alpha / 100f;

            ActivityScanner.forEachFlagged((cx, cz, e) -> {
                float[] c = e.sus ? PINK : YELLOW;
                float x0 = (float) (cx * 16 - cam.x);
                float x1 = x0 + 16f;
                float z0 = (float) (cz * 16 - cam.z);
                float z1 = z0 + 16f;
                float y0 = (float) (e.y - HEIGHT_BELOW - cam.y);
                float y1 = (float) (e.y + HEIGHT_ABOVE - cam.y);

                quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, c, a);
                quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, c, a);
                quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, c, a);
                quad(vc, m, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, c, a);
                quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, c, a);
            });
        } catch (Throwable t) {
            failed = true;
            ChunkFinderClient.LOGGER.error("Chunk Finder renderer crashed and was disabled", t);
        }
    }

    /** Emits the quad twice (both winding orders) so it shows from either side. */
    private static void quad(VertexConsumer vc, Matrix4f m,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float[] c, float a) {
        v(vc, m, ax, ay, az, c, a);
        v(vc, m, bx, by, bz, c, a);
        v(vc, m, cx, cy, cz, c, a);
        v(vc, m, dx, dy, dz, c, a);
        v(vc, m, dx, dy, dz, c, a);
        v(vc, m, cx, cy, cz, c, a);
        v(vc, m, bx, by, bz, c, a);
        v(vc, m, ax, ay, az, c, a);
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float[] c, float a) {
        vc.addVertex(m, x, y, z).setColor(c[0], c[1], c[2], a);
    }
}
