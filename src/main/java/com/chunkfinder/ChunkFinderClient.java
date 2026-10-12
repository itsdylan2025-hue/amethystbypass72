package com.chunkfinder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkFinderClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Chunk Finder");
    public static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        ChunkFinderConfig.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.chunkfinder.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_CAPS_LOCK,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("chunkfinder", "main"))));

        ChunkRenderer.init();

        ClientLifecycleEvents.CLIENT_STARTED.register(ChunkFinderClient::forceKeybinds);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new FinderScreen());
                }
            }
            ActivityScanner.tick(mc);
        });
    }

    private static void bind(KeyMapping mapping, int key) {
        mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
    }

    /** Runs on every game start. Anything not listed here stays default. */
    private static void forceKeybinds(Minecraft mc) {
        try {
            Options o = mc.options;
            bind(o.keyHotbarSlots[4], GLFW.GLFW_KEY_X); // slot 5
            bind(o.keyHotbarSlots[5], GLFW.GLFW_KEY_C); // slot 6
            bind(o.keyHotbarSlots[8], GLFW.GLFW_KEY_Q); // slot 9
            bind(o.keyChat, GLFW.GLFW_KEY_SLASH);
            bind(o.keyCommand, GLFW.GLFW_KEY_T);
            bind(o.keyDrop, GLFW.GLFW_KEY_R);
            KeyMapping.resetMapping();
            o.save();
        } catch (Throwable t) {
            LOGGER.error("Could not force keybinds", t);
        }
    }
}
