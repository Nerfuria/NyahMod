package org.nia.niamod.models.records;

import net.minecraft.core.BlockPos;
import org.nia.niamod.NiamodClient;

public record Territory(String name, BlockPos leftCorner, BlockPos rightCorner, BlockPos middle) {

    public Territory(String name, BlockPos leftCorner, BlockPos rightCorner) {
        this(
                name,
                leftCorner,
                rightCorner,
                new BlockPos(
                        (leftCorner.getX() + rightCorner.getX()) / 2,
                        0,
                        (leftCorner.getZ() + rightCorner.getZ()) / 2
                )
        );
    }

    public int distance() {
        return (int) middle.distSqr(
                NiamodClient.mc.player.blockPosition().atY(0)
        );
    }
}