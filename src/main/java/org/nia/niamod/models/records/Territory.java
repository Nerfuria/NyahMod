package org.nia.niamod.models.records;

import net.minecraft.core.BlockPos;

public record Territory(String name, BlockPos minCorner, BlockPos maxCorner) {

    public Territory {
        BlockPos lowerCorner = new BlockPos(
                Math.min(minCorner.getX(), maxCorner.getX()),
                Math.min(minCorner.getY(), maxCorner.getY()),
                Math.min(minCorner.getZ(), maxCorner.getZ())
        );
        BlockPos upperCorner = new BlockPos(
                Math.max(minCorner.getX(), maxCorner.getX()),
                Math.max(minCorner.getY(), maxCorner.getY()),
                Math.max(minCorner.getZ(), maxCorner.getZ())
        );

        minCorner = lowerCorner;
        maxCorner = upperCorner;
    }

    public boolean contains(BlockPos position) {
        BlockPos flattenedPosition = position.atY(minCorner.getY());
        return flattenedPosition.getX() >= minCorner.getX()
                && flattenedPosition.getX() <= maxCorner.getX()
                && flattenedPosition.getZ() >= minCorner.getZ()
                && flattenedPosition.getZ() <= maxCorner.getZ();
    }

    public int distanceSquaredTo(BlockPos position) {
        BlockPos center = center();
        return (int) center.distSqr(position.atY(center.getY()));
    }

    public BlockPos center() {
        return new BlockPos(
                (minCorner.getX() + maxCorner.getX()) / 2,
                minCorner.getY(),
                (minCorner.getZ() + maxCorner.getZ()) / 2
        );
    }
}
