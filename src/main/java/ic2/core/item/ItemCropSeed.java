package ic2.core.item;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.crops.ICropSeed;
import ic2.core.crop.CropManager;
import ic2.core.crop.TileEntityCrop;
import ic2.core.crop.cards.GenericCropCard;
import ic2.core.init.Localization;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Fabric stub for decompiled {@code ic2.core.item.ItemCropSeed} (ICropSeed bag).
 * Decompiled original extended {@code ItemIC2} implements {@code ICropSeed}, stored CropCard via NBT.
 * Fabric port: extends {@code ItemIC2}, holds {@code Identifier cropId} + nullable {@code CropCard} reference.
 * Implements {@link ICropSeed} via {@link DataComponentTypes#CUSTOM_DATA} ({@link NbtComponent}) — Fabric 1.21.1 replacement for {@code NBTTagCompound}.
 */
public class ItemCropSeed extends ItemIC2 implements ICropSeed {
    public final Identifier cropId;
    public final CropCard crop;

    public ItemCropSeed(String cropName) {
        super("crop_seed", new Item.Settings().maxCount(1));
        this.cropId = Identifier.of("ic2", cropName);
        this.crop = null;
    }

    public ItemCropSeed(Identifier cropId) {
        super("crop_seed");
        this.cropId = cropId;
        this.crop = null;
    }

    public ItemCropSeed(String cropName, CropCard crop) {
        super("crop_seed");
        this.cropId = Identifier.of("ic2", cropName);
        this.crop = crop;
    }

    public ItemCropSeed() {
        super("crop_seed");
        this.cropId = Identifier.of("ic2", "wheat");
        this.crop = null;
    }

    // ---- behaviour (ported from the 1.12.2 item) ----

    /**
     * The bag is named after its seed once scanned: "Wheat Seeds", "Oak Acorns"; unscanned it is
     * "Unknown Seeds".
     */
    @Override
    public Text getName(ItemStack stack) {
        CropCard crop = getCropFromStack(stack);
        int level = getScannedFromStack(stack);
        String cropKey = level == 0 ? "ic2.crop.unknown"
                : level >= 0 && crop != null ? crop.getUnlocalizedName() : "ic2.crop.invalid";
        String seedType = crop == null ? "ic2.crop.seeds" : crop.getSeedType();
        return Text.translatable(Localization.key(seedType), Text.translatable(Localization.key(cropKey)));
    }

    /** Fully scanned bags show their stats; any scanned bag of a generic crop shows its needs. */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        int level = getScannedFromStack(stack);
        if (level >= 4) {
            tooltip.add(Text.literal("Gr ").formatted(Formatting.DARK_GREEN)
                    .append(Text.literal(String.valueOf(getGrowthFromStack(stack))).formatted(Formatting.GRAY)));
            tooltip.add(Text.literal("Ga ").formatted(Formatting.GOLD)
                    .append(Text.literal(String.valueOf(getGainFromStack(stack))).formatted(Formatting.GRAY)));
            tooltip.add(Text.literal("Re ").formatted(Formatting.DARK_AQUA)
                    .append(Text.literal(String.valueOf(getResistanceFromStack(stack))).formatted(Formatting.GRAY)));
        }

        if (level >= 1 && getCropFromStack(stack) instanceof GenericCropCard generic) {
            for (String line : generic.getInformation()) {
                tooltip.add(Text.literal(line));
            }
        }
    }

    /** Plants the bag into an empty crop stick; the whole bag goes, it holds one seed. */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getWorld().getBlockEntity(context.getBlockPos()) instanceof TileEntityCrop crop)) {
            return ActionResult.PASS;
        }
        if (context.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        ItemStack stack = context.getStack();
        if (!crop.tryPlantIn(getCropFromStack(stack), 1, getGrowthFromStack(stack), getGainFromStack(stack),
                getResistanceFromStack(stack), getScannedFromStack(stack))) {
            return ActionResult.PASS;
        }

        PlayerEntity player = context.getPlayer();
        if (player == null || !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    /** One fully scanned bag of every crop, which is what the creative tab lists in 1.12.2. */
    public static List<ItemStack> creativeStacks() {
        List<ItemStack> ret = new ArrayList<>();
        if (Crops.instance != null) {
            for (CropCard crop : Crops.instance.getCrops()) {
                ret.add(generateItemStackFromValues(crop, 1, 1, 1, 4));
            }
        }
        return ret;
    }

    // ---- NBT helpers (Fabric 1.21.1: DataComponentTypes.CUSTOM_DATA) ----

    private static NbtCompound getSeedNbt(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private static void setSeedNbt(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    /**
     * Mirror of decompiled {@code generateItemStackFromValues(CropCard, int, int, int, int)}.
     * Writes {@code owner}/{@code id}/{@code growth}/{@code gain}/{@code resistance}/{@code scan} into CUSTOM_DATA.
     */
    public static ItemStack generateItemStackFromValues(CropCard crop, int statGrowth, int statGain, int statResistance, int scan) {
        // Use registered item if available, otherwise create a transient instance.
        ItemStack stack;
        try {
            if (ic2.core.init.IC2Items.CROP_SEED != null) {
                stack = new ItemStack(ic2.core.init.IC2Items.CROP_SEED);
            } else {
                stack = new ItemStack(new ItemCropSeed(crop != null ? crop.getId() : "wheat"));
            }
        } catch (Exception e) {
            stack = new ItemStack(new ItemCropSeed(crop != null ? crop.getId() : "wheat"));
        }
        if (crop != null) {
            NbtCompound nbt = getSeedNbt(stack);
            nbt.putString("owner", crop.getOwner());
            nbt.putString("id", crop.getId());
            nbt.putByte("growth", (byte) statGrowth);
            nbt.putByte("gain", (byte) statGain);
            nbt.putByte("resistance", (byte) statResistance);
            nbt.putByte("scan", (byte) scan);
            setSeedNbt(stack, nbt);
        }
        return stack;
    }

    // ---- ICropSeed ----

    @Override
    public CropCard getCropFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NbtCompound nbt = getSeedNbt(stack);
        if (nbt.contains("owner") && nbt.contains("id")) {
            String owner = nbt.getString("owner");
            String id = nbt.getString("id");
            if (owner == null || owner.isEmpty() || id == null || id.isEmpty()) return null;
            // Prefer Forge-style singleton if initialized, fallback to Fabric CropManager.
            if (Crops.instance != null) {
                try {
                    CropCard c = Crops.instance.getCropCard(owner, id);
                    if (c != null) return c;
                } catch (Exception ignored) {}
            }
            try {
                CropCard c = CropManager.getInstance().getCrop(owner, id);
                if (c != null) return c;
            } catch (Exception ignored) {}
            // Last resort: return constructor-held reference if it matches.
            if (crop != null && crop.getOwner().equals(owner) && crop.getId().equals(id)) return crop;
            return null;
        }
        // No NBT — fallback to field (e.g. freshly constructed seed item)
        return crop;
    }

    @Override
    public void setCropFromStack(ItemStack stack, CropCard crop) {
        if (stack == null || stack.isEmpty() || crop == null) return;
        NbtCompound nbt = getSeedNbt(stack);
        nbt.putString("owner", crop.getOwner());
        nbt.putString("id", crop.getId());
        setSeedNbt(stack, nbt);
    }

    @Override
    public int getGrowthFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        NbtCompound nbt = getSeedNbt(stack);
        return nbt.contains("growth") ? nbt.getByte("growth") & 0xFF : -1;
    }

    @Override
    public void setGrowthFromStack(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound nbt = getSeedNbt(stack);
        nbt.putByte("growth", (byte) value);
        setSeedNbt(stack, nbt);
    }

    @Override
    public int getGainFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        NbtCompound nbt = getSeedNbt(stack);
        return nbt.contains("gain") ? nbt.getByte("gain") & 0xFF : -1;
    }

    @Override
    public void setGainFromStack(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound nbt = getSeedNbt(stack);
        nbt.putByte("gain", (byte) value);
        setSeedNbt(stack, nbt);
    }

    @Override
    public int getResistanceFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        NbtCompound nbt = getSeedNbt(stack);
        return nbt.contains("resistance") ? nbt.getByte("resistance") & 0xFF : -1;
    }

    @Override
    public void setResistanceFromStack(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound nbt = getSeedNbt(stack);
        nbt.putByte("resistance", (byte) value);
        setSeedNbt(stack, nbt);
    }

    @Override
    public int getScannedFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        NbtCompound nbt = getSeedNbt(stack);
        return nbt.contains("scan") ? nbt.getByte("scan") & 0xFF : -1;
    }

    @Override
    public void setScannedFromStack(ItemStack stack, int value) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound nbt = getSeedNbt(stack);
        nbt.putByte("scan", (byte) value);
        setSeedNbt(stack, nbt);
    }

    @Override
    public void incrementScannedFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound nbt = getSeedNbt(stack);
        int cur = nbt.contains("scan") ? nbt.getByte("scan") & 0xFF : 0;
        nbt.putByte("scan", (byte) (cur + 1));
        setSeedNbt(stack, nbt);
    }
}
