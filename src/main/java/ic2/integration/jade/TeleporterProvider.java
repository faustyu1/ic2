package ic2.integration.jade;

import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * A teleporter's link, and what a trip there would cost the player looking at it against the EU
 * in the storage blocks around it: a teleporter that lacks the energy does nothing at all, which
 * otherwise looks the same as a broken one.
 */
public enum TeleporterProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final Identifier UID = Identifier.of("ic2", "teleporter");

    @Override
    public void appendServerData(NbtCompound data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof TileEntityTeleporter teleporter) || !teleporter.hasTarget()) {
            return;
        }
        BlockPos target = teleporter.getTarget();
        data.putIntArray("target", new int[] {target.getX(), target.getY(), target.getZ()});
        data.putInt("cost", teleporter.getTripCost(accessor.getPlayer()));
        data.putBoolean("creative", teleporter.hasCreativeSource());
        data.putInt("energy", teleporter.getAvailableEnergy());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof TileEntityTeleporter)) {
            return;
        }
        NbtCompound data = accessor.getServerData();
        int[] target = data.getIntArray("target");
        if (target.length != 3) {
            tooltip.add(Text.translatable("ic2.jade.teleporter.unlinked"));
            return;
        }
        tooltip.add(Text.translatable("ic2.jade.teleporter.target", target[0], target[1], target[2]));
        int cost = data.getInt("cost");
        tooltip.add(Text.translatable("ic2.jade.teleporter.cost", cost));
        if (data.getBoolean("creative")) {
            tooltip.add(Text.translatable("ic2.jade.teleporter.creative"));
            return;
        }
        int energy = data.getInt("energy");
        tooltip.add(Text.translatable("ic2.jade.teleporter.energy", energy));
        if (energy < cost) {
            tooltip.add(Text.translatable("ic2.jade.teleporter.short").formatted(Formatting.RED));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
