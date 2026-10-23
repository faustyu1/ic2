package ic2.core.block.machine.tileentity;

import ic2.core.IC2;
import ic2.core.block.EntityIC2Explosive;
import ic2.core.block.EntityNuke;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import ic2.core.util.LogCategory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric port of {@code ic2.core.block.machine.tileentity.TileEntityNuke} (and of its base
 * {@code TileEntityBridgeNuke}): the nuke.
 *
 * <p>It needs industrial TNT in its outer slot to go off at all - power {@code 5 * cbrt(n)} for
 * {@code n} TNT - and turns nuclear with uranium or plutonium in the core, which gives it a
 * radiation range and, past enough TNT, more power. The power is capped by
 * {@code protection/nukeExplosionPowerLimit}; {@code protection/enableNuke} turns it off. Who placed
 * it and who lit it go to the player activity log.
 *
 * <p>The classic profile's fixed nuke ({@code TileEntityClassicNuke}) is not ported.
 */
public class TileEntityNuke extends Explosive {
    public int RadiationRange;
    public final InvSlotConsumable outsideSlot;
    public final InvSlotConsumable insideSlot;

    public TileEntityNuke(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.NUKE_BE : type, pos, state);
        // the core first: that was the order of the fields, which is the inventory order
        this.insideSlot = new InvSlotConsumableItemStack(this, "insideSlot", 1,
                new ItemStack(IC2ClassicBlocks.URANIUM_BLOCK),
                nuclear("uranium_238"), nuclear("uranium_235"), nuclear("small_uranium_235"),
                nuclear("plutonium"), nuclear("small_plutonium"));
        this.outsideSlot = new InvSlotConsumableItemStack(this, "outsideSlot", 1, new ItemStack(IC2Blocks.ITNT_BLOCK));
    }

    public TileEntityNuke(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    private static ItemStack nuclear(String variant) {
        Item item = IC2ItemVariants.get("nuclear", variant);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public int getRadiationRange() {
        return RadiationRange;
    }

    public void setRadiationRange(int range) {
        RadiationRange = range;
    }

    /** @return the power of the blast, or -1 without TNT to set it off */
    public float getNukeExplosivePower() {
        if (outsideSlot.isEmpty()) {
            return -1.0F;
        }

        int itntCount = outsideSlot.get().getCount();
        double ret = 5.0 * Math.pow(itntCount, 1.0 / 3.0);
        if (insideSlot.isEmpty()) {
            setRadiationRange(0);
        } else {
            ItemStack inside = insideSlot.get();
            int insideCount = inside.getCount();
            if (is(inside, nuclear("uranium_238"))) {
                setRadiationRange(itntCount);
            } else if (inside.isOf(IC2ClassicBlocks.URANIUM_BLOCK.asItem())) {
                setRadiationRange(itntCount * 6);
            } else if (is(inside, nuclear("small_uranium_235"))) {
                setRadiationRange(itntCount * 2);
                if (itntCount >= 64) {
                    ret += 0.05555555555555555 * Math.pow(insideCount, 1.6);
                }
            } else if (is(inside, nuclear("uranium_235"))) {
                setRadiationRange(itntCount * 2);
                if (itntCount >= 32) {
                    ret += 0.5 * Math.pow(insideCount, 1.4);
                }
            } else if (is(inside, nuclear("small_plutonium"))) {
                setRadiationRange(itntCount * 3);
                if (itntCount >= 32) {
                    ret += 0.05555555555555555 * Math.pow(insideCount, 2.0);
                }
            } else if (is(inside, nuclear("plutonium"))) {
                setRadiationRange(itntCount * 4);
                if (itntCount >= 16) {
                    ret += 0.5 * Math.pow(insideCount, 1.8);
                }
            }
        }

        ret = Math.min(ret, MainConfig.get().getDouble("protection/nukeExplosionPowerLimit", 60.0));
        return (float) ret;
    }

    private static boolean is(ItemStack stack, ItemStack kind) {
        return !kind.isEmpty() && stack.isOf(kind.getItem());
    }

    @Override
    public void onPlaced(ItemStack stack, @Nullable LivingEntity placer) {
        if (placer instanceof PlayerEntity player) {
            String playerName = player.getGameProfile().getName() + "/" + player.getGameProfile().getId();
            IC2.log.info(LogCategory.PlayerActivity, "Player %s placed a nuke at %s.", playerName, pos);
        }
        super.onPlaced(stack, placer);
    }

    @Override
    protected @Nullable EntityIC2Explosive getEntity(@Nullable LivingEntity igniter) {
        if (!MainConfig.get().getBool("protection/enableNuke", true)) {
            return null;
        }

        float power = getNukeExplosivePower();
        if (power < 0.0F) {
            return null;
        }

        return new EntityNuke(getWorld(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, getRadiationRange());
    }

    @Override
    protected void onIgnite(@Nullable LivingEntity igniter) {
        String cause = igniter == null ? "indirectly" : "by " + igniter.getClass().getSimpleName() + " " + igniter.getName().getString();
        IC2.log.info(LogCategory.PlayerActivity, "Nuke at %s was ignited %s.", pos, cause);
        outsideSlot.clear();
        insideSlot.clear();
    }
}
