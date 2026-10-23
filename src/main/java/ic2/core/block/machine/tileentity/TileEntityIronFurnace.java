package ic2.core.block.machine.tileentity;

import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlotConsumableFuel;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotProcessable;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Fabric port of TileEntityIronFurnace: a furnace burning ordinary fuel (lava too) that smelts in
 * 160 ticks instead of 200. Fuel is only lit when there is something to smelt. The experience
 * collects in the furnace until the player takes it with the window's button.
 */
public class TileEntityIronFurnace extends TileEntityInventory {
    public static final short OPERATION_LENGTH = 160;

    public final InvSlotProcessable<ItemStack, ItemStack, ItemStack> inputSlot;
    public final InvSlotOutput outputSlot;
    public final InvSlotConsumableFuel fuelSlot;
    public int fuel;
    public int totalFuel;
    public short progress;
    protected double xp;

    public TileEntityIronFurnace(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.IRON_FURNACE_BE : type, pos, state, TeBlock.iron_furnace);
        this.inputSlot = new InvSlotProcessable<>(this, "input", 1, Recipes.furnace);
        this.outputSlot = new InvSlotOutput(this, "output", 1);
        this.fuelSlot = new InvSlotConsumableFuel(this, "fuel", 1, true);
    }

    public TileEntityIronFurnace(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        boolean needsInvUpdate = false;
        if (fuel <= 0 && canOperate()) {
            fuel = totalFuel = fuelSlot.consumeFuel();
            if (fuel > 0) {
                needsInvUpdate = true;
            }
        }

        if (fuel > 0 && canOperate()) {
            progress++;
            if (progress >= OPERATION_LENGTH) {
                progress = 0;
                operate();
                needsInvUpdate = true;
            }
        } else {
            progress = 0;
        }

        if (fuel > 0) {
            fuel--;
            setActive(true);
        } else {
            setActive(false);
        }
        if (needsInvUpdate || fuel > 0) {
            markDirty();
        }
    }

    private void operate() {
        MachineRecipeResult<ItemStack, ItemStack, ItemStack> result = inputSlot.process();
        outputSlot.add(result.getOutput().copy());
        inputSlot.consume(1);
        xp += result.getRecipe().getMetaData().getFloat("experience");
    }

    private boolean canOperate() {
        MachineRecipeResult<ItemStack, ItemStack, ItemStack> result = inputSlot.process();
        return result != null && outputSlot.canAdd(result.getOutput());
    }

    /** Hands the collected experience to the player as orbs; the fraction of a point stays. */
    public static double spawnXP(PlayerEntity player, double xp) {
        if (player.getWorld() instanceof ServerWorld world) {
            int points = (int) Math.floor(xp);
            if (points > 0) {
                ExperienceOrbEntity.spawn(world, new Vec3d(player.getX(), player.getY() + 0.5, player.getZ() + 0.5), points);
            }
            return xp - points;
        }
        return xp;
    }

    public double getXp() {
        return xp;
    }

    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if ("0".equals(event) && player != null) {
            xp = spawnXP(player, xp);
            markDirty();
        }
    }

    @Override
    public double getGuiValue(String name) {
        return switch (name) {
            case "fuel" -> fuel == 0 || totalFuel == 0 ? 0.0 : (double) fuel / totalFuel;
            case "progress" -> (double) progress / OPERATION_LENGTH;
            default -> super.getGuiValue(name);
        };
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        fuel = nbt.getInt("fuel");
        totalFuel = nbt.getInt("totalFuel");
        progress = nbt.getShort("progress");
        xp = nbt.getDouble("xp");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("fuel", fuel);
        nbt.putInt("totalFuel", totalFuel);
        nbt.putShort("progress", progress);
        nbt.putDouble("xp", xp);
    }
}
