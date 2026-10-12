package com.chunkfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class ActivityScanner {
    public static final class Entry {
        public final long tick;
        public final boolean sus;
        public final boolean light;
        public final int y;

        Entry(long tick, boolean sus, boolean light, int y) {
            this.tick = tick;
            this.sus = sus;
            this.light = light;
            this.y = y;
        }
    }

    public interface FlaggedConsumer {
        void accept(int cx, int cz, Entry entry);
    }

    private static final Map<Long, Entry> CACHE = new HashMap<>();
    private static final Set<Long> ANNOUNCED = new HashSet<>();
    private static final int RESCAN_TICKS = 600;
    private static final int PER_MARKER_CAP = 6;
    private static final int LIGHT_MAX_Y = 48;
    private static final int CHUNKS_PER_TICK = 2;
    private static final int LOOKS_PER_TICK = 40;

    private static ClientLevel lastLevel;
    private static int lastRevision = -1;
    private static long tickCounter;
    private static int[][] offsets = new int[0][];
    private static int offsetsRadius = -1;
    private static int cursor;

    private ActivityScanner() {}

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long k) {
        return (int) (k >> 32);
    }

    private static int unpackZ(long k) {
        return (int) k;
    }

    private static void buildOffsets(int radius) {
        List<int[]> list = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    list.add(new int[]{dx, dz});
                }
            }
        }
        list.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
        offsets = list.toArray(new int[0][]);
        offsetsRadius = radius;
        cursor = 0;
    }

    public static void reset() {
        CACHE.clear();
        ANNOUNCED.clear();
        cursor = 0;
    }

    public static void forEachFlagged(FlaggedConsumer consumer) {
        for (Map.Entry<Long, Entry> e : CACHE.entrySet()) {
            Entry v = e.getValue();
            if (v.sus || v.light) {
                consumer.accept(unpackX(e.getKey()), unpackZ(e.getKey()), v);
            }
        }
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            lastLevel = null;
            reset();
            return;
        }
        if (level != lastLevel) {
            reset();
            lastLevel = level;
        }
        ChunkFinderConfig cfg = ChunkFinderConfig.get();
        if (ChunkFinderConfig.revision != lastRevision) {
            CACHE.clear();
            ANNOUNCED.clear();
            lastRevision = ChunkFinderConfig.revision;
        }
        if (!cfg.susChunkFinder && !cfg.lightFinder) {
            return;
        }
        if (offsetsRadius != cfg.simulationDistance) {
            buildOffsets(cfg.simulationDistance);
        }
        tickCounter++;
        int pcx = Mth.floor(player.getX()) >> 4;
        int pcz = Mth.floor(player.getZ()) >> 4;

        int budget = CHUNKS_PER_TICK;
        for (int look = 0; look < LOOKS_PER_TICK && budget > 0 && offsets.length > 0; look++) {
            int[] o = offsets[cursor];
            cursor = (cursor + 1) % offsets.length;
            int cx = pcx + o[0];
            int cz = pcz + o[1];
            long key = pack(cx, cz);
            Entry old = CACHE.get(key);
            if (old != null && tickCounter - old.tick < RESCAN_TICKS) {
                continue;
            }
            ChunkAccess chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
            if (chunk == null) {
                CACHE.remove(key);
                continue;
            }
            Entry fresh = scan(chunk, cfg, tickCounter);
            CACHE.put(key, fresh);
            budget--;
            if ((fresh.sus || fresh.light) && ANNOUNCED.add(key) && cfg.chatAlerts) {
                String what = fresh.sus ? "Sus chunk" : "Light";
                player.displayClientMessage(Component.literal("[Chunk Finder] " + what + " at x "
                        + (cx * 16 + 8) + ", z " + (cz * 16 + 8) + " (y ~ " + fresh.y + ")"), false);
            }
        }

        if (tickCounter % 20 == 0) {
            int far = cfg.simulationDistance + 2;
            CACHE.keySet().removeIf(k -> Math.abs(unpackX(k) - pcx) > far || Math.abs(unpackZ(k) - pcz) > far);
        }
    }

    private static Entry scan(ChunkAccess chunk, ChunkFinderConfig cfg, long now) {
        List<Marker> active = new ArrayList<>();
        if (cfg.susChunkFinder) {
            for (Marker m : Marker.values()) {
                if (cfg.isOn(m)) {
                    active.add(m);
                }
            }
        }
        final Marker[] act = active.toArray(new Marker[0]);
        final boolean light = cfg.lightFinder;
        Predicate<BlockState> any = s -> {
            for (Marker m : act) {
                if (m.test(s)) {
                    return true;
                }
            }
            return light && Marker.isLight(s);
        };

        int[] counts = new int[act.length];
        long ySum = 0;
        int yN = 0;
        int lightCount = 0;
        long lightYSum = 0;

        LevelChunkSection[] sections = chunk.getSections();
        int minSection = chunk.getMinSectionY();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(any)) {
                continue;
            }
            int baseY = (minSection + i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState s = sec.getBlockState(x, y, z);
                        if (s.isAir()) {
                            continue;
                        }
                        int wy = baseY + y;
                        for (int k = 0; k < act.length; k++) {
                            if (act[k].test(s)) {
                                counts[k]++;
                                ySum += wy;
                                yN++;
                            }
                        }
                        if (light && wy < LIGHT_MAX_Y && Marker.isLight(s)) {
                            lightCount++;
                            lightYSum += wy;
                        }
                    }
                }
            }
        }

        int score = 0;
        for (int k = 0; k < act.length; k++) {
            score += act[k].weight * Math.min(counts[k], PER_MARKER_CAP);
        }
        int need = Math.max(2, 24 - 2 * cfg.sensitivity);
        boolean sus = cfg.susChunkFinder && score >= need;

        int lightNeed = Math.max(1, 6 - cfg.sensitivity / 2);
        boolean lightFlag = light && lightCount >= lightNeed;

        int y = sus && yN > 0 ? (int) (ySum / yN) : lightCount > 0 ? (int) (lightYSum / lightCount) : 64;
        return new Entry(now, sus, lightFlag, y);
    }
}
