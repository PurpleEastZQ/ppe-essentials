package me.purpleeast.mods.ppe_essentials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

final class PpeRandomTeleport {
    private static final int ATTEMPTS = 16;

    private PpeRandomTeleport() {
    }

    static Optional<BlockPos> findDestination(ServerPlayer player) {
        ServerLevel level = PpeCompat.level(player);
        boolean nether = level.dimension().equals(Level.NETHER);
        double min = nether ? PpeConfig.rtpNetherMinDistance() : PpeConfig.rtpMinDistance();
        double max = nether ? PpeConfig.rtpNetherMaxDistance() : PpeConfig.rtpMaxDistance();
        double minSquared = min * min;
        double maxSquared = max * max;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.TAU;
            double distance = Math.sqrt(minSquared + player.getRandom().nextDouble() * (maxSquared - minSquared));
            BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, 0,
                    player.getZ() + Math.sin(angle) * distance);
            double dx = column.getX() + 0.5 - player.getX();
            double dz = column.getZ() + 0.5 - player.getZ();
            double actualDistance = dx * dx + dz * dz;
            if (actualDistance < minSquared || actualDistance > maxSquared
                    || !level.getWorldBorder().isWithinBounds(column)) {
                continue;
            }

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            if (nether) {
                // Search below the Nether roof for room to stand.
                y = Math.min(y, 126);
                while (!level.isOutsideBuildHeight(y - 1) && !canStandAt(level, column.atY(y))) {
                    y--;
                }
            }
            BlockPos destination = column.atY(y);
            if (canStandAt(level, destination)) {
                return Optional.of(destination);
            }
        }
        return Optional.empty();
    }

    private static boolean canStandAt(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        if (level.isOutsideBuildHeight(below) || level.isOutsideBuildHeight(pos.above())
                || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        var floor = level.getBlockState(below);
        return floor.getFluidState().isEmpty()
                && !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.CACTUS)
                && floor.isFaceSturdy(level, below, Direction.UP);
    }
}
