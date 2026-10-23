package ic2.core.block.steam;

import ic2.core.IC2;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2Items;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of TileEntityCokeKiln - the controller of the coke kiln multiblock.
 *
 * <p>The kiln is a 3x3x3 of refractory bricks: a grate in the middle of the floor, a hatch in the
 * middle of the roof, air in the centre and this block in the middle of one side, facing out.
 * Once a second it checks the structure and, if it stands, bakes one item from the hatch: a log
 * into charcoal and 250 mB of creosote, a piece of coal into coke and 500 mB of creosote, 1800
 * ticks each. The solid result goes into its own slot, the creosote into the grate's tank.
 */
public class TileEntityCokeKiln extends TileEntityInventory {
    /** One thing the kiln bakes: what goes in, what comes out and how long it takes. */
    public record Recipe(Predicate<ItemStack> input, ItemStack output, FluidName fluid, int fluidAmount, int duration) {
        public FluidStack fluidOutput() {
            return new FluidStack(fluid.getInstance(), fluidAmount);
        }
    }

    public static final List<Recipe> RECIPES = List.of(
            new Recipe(stack -> stack.isIn(ItemTags.LOGS_THAT_BURN), new ItemStack(Items.CHARCOAL), FluidName.creosote, 250, 1800),
            new Recipe(stack -> stack.isOf(Items.COAL), new ItemStack(IC2Items.COKE), FluidName.creosote, 500, 1800));

    protected final int tickRate = 20;
    protected int updateTicker = IC2.random.nextInt(tickRate);
    protected boolean isFormed;
    protected final InvSlotOutput outputSlot;
    protected int progress;
    protected int operationLength;
    protected Recipe recipe;
    protected float guiProgress;

    public TileEntityCokeKiln(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.COKE_KILN_BE : type, pos, state, TeBlock.coke_kiln);
        this.outputSlot = new InvSlotOutput(this, "output", 1, InvSlot.InvSide.ANY);
    }

    public TileEntityCokeKiln(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public static Recipe findRecipe(ItemStack input) {
        for (Recipe recipe : RECIPES) {
            if (recipe.input().test(input)) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        if (world == null) {
            return;
        }
        if (world.isClient) {
            tickClient();
            return;
        }
        if (updateTicker++ % tickRate != 0) {
            return;
        }

        isFormed = hasValidStructure();
        if (!isFormed) {
            progress = 0;
            guiProgress = 0.0F;
            setActive(false);
            return;
        }

        boolean needsInventoryUpdate = false;
        if (canWork()) {
            setActive(true);
            if (progress == 0) {
                needsInventoryUpdate = true;
            }
            int progressNeeded = recipe.duration();
            if (progress < progressNeeded) {
                progress += 20;
            }
            if (progress >= progressNeeded) {
                finishWork();
                needsInventoryUpdate = true;
            }
        } else {
            setActive(false);
        }

        guiProgress = progress != 0 && operationLength != 0 ? (float) progress / operationLength : 0.0F;
        if (needsInventoryUpdate) {
            markDirty();
        }
    }

    private void tickClient() {
        if (!getActive()) {
            return;
        }
        BlockPos front = pos.offset(getFacing());
        if (world.random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.FLAME, front.getX() + 0.5 - getFacing().getOffsetX() * 0.45,
                    pos.getY() + 0.3 + world.random.nextDouble() * 0.4, front.getZ() + 0.5 - getFacing().getOffsetZ() * 0.45, 0, 0, 0);
        }
        if (world.random.nextDouble() < 0.1) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE,
                    SoundCategory.BLOCKS, 1.0F, 1.0F, false);
        }
    }

    /** The block behind the kiln, one up (the hatch) or one down (the grate). */
    private BlockPos behind(int dy) {
        return new BlockPos(pos.getX() - getFacing().getOffsetX(), pos.getY() + dy, pos.getZ() - getFacing().getOffsetZ());
    }

    private TileEntityCokeKilnHatch hatch() {
        return world.getBlockEntity(behind(1)) instanceof TileEntityCokeKilnHatch hatch ? hatch : null;
    }

    private TileEntityCokeKilnGrate grate() {
        return world.getBlockEntity(behind(-1)) instanceof TileEntityCokeKilnGrate grate ? grate : null;
    }

    protected boolean canWork() {
        TileEntityCokeKilnHatch hatch = hatch();
        if (hatch == null) {
            return false;
        }
        ItemStack input = hatch.inventory.get();
        if (input.isEmpty()) {
            return false;
        }

        if (recipe != null && !recipe.input().test(input)) {
            reset();
        }
        if (recipe == null) {
            Recipe found = findRecipe(input);
            if (found == null) {
                return false;
            }
            updateRecipe(found);
        }

        if (!outputSlot.canAdd(recipe.output())) {
            return false;
        }
        TileEntityCokeKilnGrate grate = grate();
        return grate != null && grate.fluidTank.fillInternal(recipe.fluidOutput(), false) >= recipe.fluidAmount();
    }

    protected void finishWork() {
        TileEntityCokeKilnHatch hatch = hatch();
        if (hatch == null || hatch.inventory.get().isEmpty()) {
            return;
        }

        hatch.inventory.get().decrement(1);
        hatch.inventory.onChanged();
        outputSlot.add(recipe.output().copy());
        TileEntityCokeKilnGrate grate = grate();
        if (grate != null) {
            grate.fluidTank.fillInternal(recipe.fluidOutput(), true);
            grate.markDirty();
        }
        progress = 0;
    }

    protected void updateRecipe(Recipe recipe) {
        this.operationLength = recipe.duration();
        this.recipe = recipe;
    }

    protected void reset() {
        progress = 0;
        operationLength = 0;
        recipe = null;
    }

    /** Whether the 3x3x3 of refractory bricks with grate, hatch and hollow centre stands. */
    public boolean hasValidStructure() {
        BlockPos.Mutable cPos = new BlockPos.Mutable();
        for (int dy = -1; dy <= 1; dy++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    cPos.set(pos.getX() + x - getFacing().getOffsetX(), pos.getY() + dy, pos.getZ() + z - getFacing().getOffsetZ());
                    boolean centre = x == 0 && z == 0;
                    if (centre && dy == -1) {
                        if (!(world.getBlockEntity(cPos) instanceof TileEntityCokeKilnGrate)) {
                            return false;
                        }
                    } else if (centre && dy == 1) {
                        if (!(world.getBlockEntity(cPos) instanceof TileEntityCokeKilnHatch)) {
                            return false;
                        }
                    } else if (centre) {
                        if (!world.getBlockState(cPos).isAir()) {
                            return false;
                        }
                    } else if (dy == 0 && cPos.getX() == pos.getX() && cPos.getZ() == pos.getZ()) {
                        if (world.getBlockEntity(cPos) != this) {
                            return false;
                        }
                    } else if (!world.getBlockState(cPos).isOf(IC2ClassicBlocks.REFRACTORY_BRICKS)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean isFormed() {
        return isFormed;
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public double getGuiValue(String name) {
        return "progress".equals(name) ? guiProgress : super.getGuiValue(name);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        progress = nbt.getInt("progress");
        operationLength = nbt.getInt("operationLength");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("progress", progress);
        nbt.putInt("operationLength", operationLength);
    }
}
