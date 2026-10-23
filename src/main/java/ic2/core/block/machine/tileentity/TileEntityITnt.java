package ic2.core.block.machine.tileentity;

import ic2.core.block.EntityIC2Explosive;
import ic2.core.block.EntityItnt;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric port of {@code ic2.core.block.machine.tileentity.ITnt}: industrial TNT. Breaking it lights
 * it, as breaking dynamite does.
 */
public class TileEntityITnt extends Explosive {
    public TileEntityITnt(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.ITNT_BE : type, pos, state);
    }

    public TileEntityITnt(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    protected boolean explodeOnRemoval() {
        return true;
    }

    @Override
    protected EntityIC2Explosive getEntity(@Nullable LivingEntity igniter) {
        return new EntityItnt(getWorld(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
