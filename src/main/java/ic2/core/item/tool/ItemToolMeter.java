package ic2.core.item.tool;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.item.IBoxable;
import ic2.core.item.ItemIC2;
import ic2.core.item.handheld.MeterScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemToolMeter}: the EU reader.
 *
 * <p>Pointed at a cable, a generator or a machine it opens a screen that keeps measuring that
 * tile for as long as the screen is open. The tile is handed to the screen handler at open time,
 * the way 1.12.2 called {@code setUut} right after launching the GUI.
 */
public class ItemToolMeter extends ItemIC2 implements IBoxable {

    public ItemToolMeter() {
        super("meter", new Settings().maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (world.isClient || player == null) {
            return ActionResult.SUCCESS;
        }

        IEnergyTile tile = EnergyNet.instance == null
                ? null
                : EnergyNet.instance.getTile(world, context.getBlockPos());

        if (!(tile instanceof IEnergySource) && !(tile instanceof IEnergyConductor) && !(tile instanceof IEnergySink)) {
            player.sendMessage(Text.translatable("meter.notAnEnergyTile"), false);
            return ActionResult.SUCCESS;
        }

        player.openHandledScreen(new MeterFactory(tile, context.getStack()));
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean canBeStoredInToolbox(ItemStack stack) {
        return true;
    }

    /** Opens a reader already pointed at {@code target}. */
    private record MeterFactory(IEnergyTile target, ItemStack stack) implements NamedScreenHandlerFactory {

        @Override
        public Text getDisplayName() {
            return stack.getName();
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            PropertyDelegate properties = new ArrayPropertyDelegate(MeterScreenHandler.PROPERTY_COUNT);
            return new MeterScreenHandler(syncId, playerInventory, properties, target);
        }
    }
}
