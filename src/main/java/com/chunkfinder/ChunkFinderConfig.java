package com.chunkfinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChunkFinderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ChunkFinderConfig instance = new ChunkFinderConfig();

    /** Bumped whenever a setting changes so the scanner rescans. */
    public static int revision = 0;

    public boolean susChunkFinder = true;
    public boolean lightFinder = false;
    public boolean chatAlerts = true;
    public int simulationDistance = 4;
    public int sensitivity = 4;
    public int alpha = 30;
    public Map<String, Boolean> markers = new LinkedHashMap<>();

    public static ChunkFinderConfig get() {
        return instance;
    }

    public boolean isOn(Marker m) {
        return markers.getOrDefault(m.name(), m.defaultOn);
    }

    public void set(Marker m, boolean on) {
        markers.put(m.name(), on);
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("chunkfinder.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                ChunkFinderConfig loaded = GSON.fromJson(r, ChunkFinderConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                ChunkFinderClient.LOGGER.warn("Could not read chunkfinder.json, using defaults", e);
            }
        }
        if (instance.markers == null) {
            instance.markers = new LinkedHashMap<>();
        }
        instance.clamp();
    }

    public void clamp() {
        simulationDistance = Mth.clamp(simulationDistance, 1, 16);
        sensitivity = Mth.clamp(sensitivity, 1, 10);
        alpha = Mth.clamp(alpha, 0, 100);
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            ChunkFinderClient.LOGGER.warn("Could not save chunkfinder.json", e);
        }
    }

    /** Call after changing a setting. */
    public void changed() {
        clamp();
        revision++;
        save();
    }
}
