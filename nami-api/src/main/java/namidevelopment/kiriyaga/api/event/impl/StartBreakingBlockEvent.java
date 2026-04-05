package namidevelopment.kiriyaga.api.event.impl;

import namidevelopment.kiriyaga.api.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class StartBreakingBlockEvent extends Event {
    public BlockPos blockPos;
    public Direction direction;

    public boolean manual;

    public StartBreakingBlockEvent(BlockPos blockPos, Direction direction) {
        this(blockPos, direction, true);
    }

    public StartBreakingBlockEvent(BlockPos blockPos, Direction direction, boolean manual) {
        this.blockPos = blockPos;
        this.direction = direction;
        this.manual = manual;
    }
}
