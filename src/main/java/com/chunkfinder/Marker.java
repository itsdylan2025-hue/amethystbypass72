package com.chunkfinder;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.function.Predicate;

public enum Marker {
    KELP("Kelp", false, 1, s -> s.is(Blocks.KELP) || s.is(Blocks.KELP_PLANT)),
    CAVE_VINES("Cave Vines", false, 1, s -> s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT)),
    VINES("Vines", false, 1, s -> s.is(Blocks.VINE)),
    AMETHYST("Amethyst", true, 2, s -> s.is(Blocks.AMETHYST_BLOCK) || s.is(Blocks.BUDDING_AMETHYST)
            || s.is(Blocks.AMETHYST_CLUSTER) || s.is(Blocks.LARGE_AMETHYST_BUD)
            || s.is(Blocks.MEDIUM_AMETHYST_BUD) || s.is(Blocks.SMALL_AMETHYST_BUD)),
    BAMBOO("Bamboo", false, 1, s -> s.is(Blocks.BAMBOO) || s.is(Blocks.BAMBOO_SAPLING)),
    BEE_NEST("Bee Nest", false, 3, s -> s.is(Blocks.BEE_NEST) || s.is(Blocks.BEEHIVE)),
    ROTATED_DEEPSLATE("Rotated Deepslate", true, 8,
            s -> s.is(Blocks.DEEPSLATE) && s.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y),
    CHESTS("Chests", true, 4, s -> s.getBlock() instanceof ChestBlock || s.is(Blocks.BARREL)),
    SHULKERS("Shulkers", true, 6, s -> s.getBlock() instanceof ShulkerBoxBlock),
    BEDS("Beds", true, 4, s -> s.getBlock() instanceof BedBlock),
    FURNACES("Furnaces", true, 3, s -> s.getBlock() instanceof AbstractFurnaceBlock),
    REDSTONE("Redstone", true, 3, s -> s.is(Blocks.HOPPER) || s.is(Blocks.DISPENSER) || s.is(Blocks.DROPPER)
            || s.is(Blocks.OBSERVER) || s.is(Blocks.PISTON) || s.is(Blocks.STICKY_PISTON)
            || s.is(Blocks.REPEATER) || s.is(Blocks.COMPARATOR));

    public final String label;
    public final boolean defaultOn;
    public final int weight;
    private final Predicate<BlockState> test;

    Marker(String label, boolean defaultOn, int weight, Predicate<BlockState> test) {
        this.label = label;
        this.defaultOn = defaultOn;
        this.weight = weight;
        this.test = test;
    }

    public boolean test(BlockState state) {
        return test.test(state);
    }

    private static final Set<Block> LIGHTS = Set.of(
            Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
            Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.GLOWSTONE, Blocks.SEA_LANTERN,
            Blocks.SHROOMLIGHT, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
            Blocks.REDSTONE_LAMP, Blocks.JACK_O_LANTERN);

    /** Used by the Light Finder toggle. */
    public static boolean isLight(BlockState state) {
        return LIGHTS.contains(state.getBlock());
    }
}
