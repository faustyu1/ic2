package ic2.core.item.tool;

import ic2.api.crops.CropCard;
import ic2.api.crops.ICropTile;
import ic2.api.item.ElectricItem;
import ic2.core.item.ItemElectric;
import ic2.core.item.handheld.CropnalyzerScreenHandler;
import ic2.core.item.handheld.HandHeldInventory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemCropnalyzer}.
 *
 * <p>In the air it opens the analysis screen; pointed at a crop it reads that crop out in chat for
 * the price of a level-two scan, which is what 1.12.2 charged for the same look.
 */
public class ItemCropnalyzer extends ItemElectric {

    public ItemCropnalyzer() {
        super("cropnalyzer", 2, 100000.0, 128.0, new Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient) {
            player.openHandledScreen(new CropnalyzerFactory(player, hand));
        }

        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (world.isClient || player == null || player.isSneaking()) {
            return ActionResult.PASS;
        }

        BlockEntity blockEntity = world.getBlockEntity(context.getBlockPos());
        if (!(blockEntity instanceof ICropTile crop) || crop.getCrop() == null) {
            return ActionResult.PASS;
        }

        double cost = CropnalyzerScreenHandler.energyForLevel(2);
        if (ElectricItem.manager.discharge(context.getStack(), cost, 3, true, false, false) < cost) {
            return ActionResult.PASS;
        }

        CropCard plant = crop.getCrop();
        send(player, "Crop name: " + Text.translatable(ic2.core.init.Localization.key(plant.getUnlocalizedName())).getString()
                + " (by " + plant.getDiscoveredBy() + ')');
        send(player, "Crop size: " + crop.getCurrentSize() + '/' + plant.getMaxSize());
        send(player, "Nutrient storage: " + crop.getStorageNutrients() + "/100");
        send(player, "Water storage: " + crop.getStorageWater() + "/200");
        send(player, "Weed-Ex storage: " + crop.getStorageWeedEX() + "/100");
        send(player, "Growth points: " + crop.getGrowthPoints() + '/' + plant.getGrowthDuration(crop));
        return ActionResult.SUCCESS;
    }

    /** A tool is not a battery: it never powers anything else, as in 1.12.2. */
    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    private static void send(PlayerEntity player, String line) {
        player.sendMessage(Text.literal(line), false);
    }

    /** Opens the analysis screen over the inventory kept inside the cropnalyzer's own stack. */
    private record CropnalyzerFactory(PlayerEntity owner, Hand hand) implements NamedScreenHandlerFactory {

        @Override
        public Text getDisplayName() {
            return owner.getStackInHand(hand).getName();
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            HandHeldInventory inventory = new HandHeldInventory(player, hand,
                    CropnalyzerScreenHandler.SIZE, stack -> true);
            return new CropnalyzerScreenHandler(syncId, playerInventory, inventory, inventory);
        }
    }
}
