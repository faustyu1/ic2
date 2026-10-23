package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.item.IItemFluidTank;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.FluidContainers;
import ic2.core.util.LiquidUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of {@code ic2.core.block.machine.tileentity.TileEntityCanner}: the fluid/solid canning
 * machine. It runs 200 ticks at 4 EU/t per operation, in one of four modes switched in its window:
 *
 * <ul>
 * <li>{@link Mode#BottleSolid} - a container from the can slot and something to put in it, such
 * as food into tin cans or uranium into fuel rods;</li>
 * <li>{@link Mode#EmptyLiquid} - empties the container in the can slot into the output tank;</li>
 * <li>{@link Mode#BottleLiquid} - fills the container in the can slot from the input tank;</li>
 * <li>{@link Mode#EnrichLiquid} - turns fluid from the input tank and an additive into another
 * fluid, for example water and CF powder into construction foam or water and bio chaff into
 * biomass. The result goes into the container in the can slot first, the rest into the output
 * tank.</li>
 * </ul>
 *
 * <p>1.12.2 built this on four recipe managers shared with the API ({@code cannerBottle},
 * {@code cannerEnrich}, the fluid container fill and empty managers) and its slots worked through
 * them. The port keeps the recipe lists here and works out the operation itself; the recipes,
 * amounts and outcomes are those of {@code TileEntityCanner.init}. A container is a bucket, an IC2
 * cell or an item with a tank of its own ({@link IItemFluidTank}); the latter is filled with or
 * emptied of whatever fits and then handed to the output slot.
 */
public class TileEntityCanner extends TileEntityElectricMachine implements IUpgradableBlock {

    public enum Mode {
        BottleSolid,
        EmptyLiquid,
        BottleLiquid,
        EnrichLiquid;

        public static final Mode[] values = values();
    }

    /** A container, something to put in it, and what comes out. */
    public record BottleRecipe(Ingredient container, int containerAmount, Ingredient fill, int fillAmount, ItemStack output) {}

    /** Fluid and an additive that turn into another fluid. */
    public record EnrichRecipe(String input, int inputAmount, Ingredient additive, int additiveAmount, String output, int outputAmount) {
        Fluid inputFluid() {
            return LiquidUtil.getFluid(input);
        }

        Fluid outputFluid() {
            return LiquidUtil.getFluid(output);
        }
    }

    public static final List<BottleRecipe> BOTTLE_RECIPES = new ArrayList<>();
    public static final List<EnrichRecipe> ENRICH_RECIPES = new ArrayList<>();

    public static final int ENERGY_PER_TICK = 4;
    public static final int OPERATION_LENGTH = 200;

    private Mode mode = Mode.BottleSolid;
    private short progress;

    public final InvSlotConsumable inputSlot;
    public final InvSlotConsumable canInputSlot;
    public final InvSlotOutput outputSlot;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;
    public final Fluids.InternalFluidTank inputTank;
    public final Fluids.InternalFluidTank outputTank;

    public TileEntityCanner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CANNER_BE : type, pos, state, TeBlock.canner, ENERGY_PER_TICK * OPERATION_LENGTH, 1);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 1);
        this.outputSlot = new InvSlotOutput(this, "output", 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.inputSlot = new InvSlotConsumable(this, "input", 1) {
            @Override
            public boolean accepts(ItemStack stack) {
                return acceptsInput(stack);
            }
        };
        this.canInputSlot = new InvSlotConsumable(this, "canInput", 1) {
            @Override
            public boolean accepts(ItemStack stack) {
                return acceptsContainer(stack);
            }
        };
        this.inputTank = fluids().addTankInsert("inputTank", 8000, fluid -> true);
        this.outputTank = fluids().addTankExtract("outputTank", 8000);
    }

    public TileEntityCanner(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    // ---------------------------------------------------------------- recipes

    public static void init() {
        BOTTLE_RECIPES.clear();
        ENRICH_RECIPES.clear();

        Item fuelRod = item("crafting_fuel_rod");
        addBottleRecipe(fuelRod, 1, item("nuclear_uranium"), 1, new ItemStack(item("uranium_fuel_rod")));
        addBottleRecipe(fuelRod, 1, item("nuclear_mox"), 1, new ItemStack(item("mox_fuel_rod")));

        // a tin can holds about one hunger point; the 1.12.2 fish is the 1.21 cod
        Item tinCan = item("crafting_tin_can");
        Item filled = item("filled_tin_can");
        addCans(tinCan, filled, 1, Items.POTATO);
        addCans(tinCan, filled, 2, Items.COOKIE, Items.MELON_SLICE, Items.COD, Items.CHICKEN);
        addCans(tinCan, filled, 3, Items.PORKCHOP, Items.BEEF);
        addCans(tinCan, filled, 4, Items.APPLE, Items.CARROT);
        addCans(tinCan, filled, 5, Items.BREAD, Items.COOKED_COD);
        addCans(tinCan, filled, 6, Items.COOKED_CHICKEN, Items.BAKED_POTATO, Items.MUSHROOM_STEW, Items.PUMPKIN_PIE);
        addCans(tinCan, filled, 8, Items.COOKED_PORKCHOP, Items.COOKED_BEEF);
        addCans(tinCan, filled, 12, Items.CAKE);
        addBottleRecipe(tinCan, 1, Items.POISONOUS_POTATO, 2, new ItemStack(filled));
        addBottleRecipe(tinCan, 1, Items.ROTTEN_FLESH, 2, new ItemStack(filled));

        Ingredient lapis = Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/lapis")));
        addEnrichRecipe("water", 1000, Ingredient.ofItems(item("dust_milk")), 1, FluidName.milk.getName(), 1000);
        addEnrichRecipe("water", 1000, Ingredient.ofItems(item("crafting_cf_powder")), 1, FluidName.construction_foam.getName(), 1000);
        addEnrichRecipe("water", 1000, lapis, 8, FluidName.coolant.getName(), 1000);
        addEnrichRecipe(FluidName.distilled_water.getName(), 1000, lapis, 1, FluidName.coolant.getName(), 1000);
        addEnrichRecipe("water", 1000, Ingredient.ofItems(item("crafting_bio_chaff")), 1, FluidName.biomass.getName(), 1000);
        addEnrichRecipe("water", 6000, Ingredient.ofItems(Items.STICK), 1, FluidName.hot_water.getName(), 1000);
    }

    private static void addCans(Item tinCan, Item filled, int cans, Item... foods) {
        for (Item food : foods) {
            addBottleRecipe(tinCan, cans, food, 1, new ItemStack(filled, cans));
        }
    }

    public static void addBottleRecipe(Item container, int containerAmount, Item fill, int fillAmount, ItemStack output) {
        BOTTLE_RECIPES.add(new BottleRecipe(Ingredient.ofItems(container), containerAmount, Ingredient.ofItems(fill), fillAmount, output));
    }

    public static void addEnrichRecipe(String input, int inputAmount, Ingredient additive, int additiveAmount, String output, int outputAmount) {
        ENRICH_RECIPES.add(new EnrichRecipe(input, inputAmount, additive, additiveAmount, output, outputAmount));
    }

    private static Item item(String name) {
        return Registries.ITEM.get(Identifier.of("ic2", name));
    }

    static BottleRecipe findBottleRecipe(ItemStack container, ItemStack fill, boolean checkAmounts) {
        for (BottleRecipe recipe : BOTTLE_RECIPES) {
            if (recipe.container().test(container) && recipe.fill().test(fill)
                    && (!checkAmounts || container.getCount() >= recipe.containerAmount() && fill.getCount() >= recipe.fillAmount())) {
                return recipe;
            }
        }
        return null;
    }

    /** The recipe the tank's fluid and the additive make, when there is enough of both. */
    private static EnrichRecipe findEnrichRecipe(Fluids.InternalFluidTank tank, ItemStack additive) {
        for (EnrichRecipe recipe : ENRICH_RECIPES) {
            Fluid input = recipe.inputFluid();
            if (recipe.additive().test(additive) && !tank.isEmpty() && tank.getFluid().isOf(input)
                    && tank.getFluidAmount() >= LiquidUtil.mb(recipe.inputAmount()) && additive.getCount() >= recipe.additiveAmount()) {
                return recipe;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- slots

    private boolean acceptsInput(ItemStack stack) {
        return switch (mode) {
            case BottleSolid -> BOTTLE_RECIPES.stream().anyMatch(recipe -> recipe.fill().test(stack));
            case EnrichLiquid -> ENRICH_RECIPES.stream().anyMatch(recipe -> recipe.additive().test(stack));
            case EmptyLiquid, BottleLiquid -> false;
        };
    }

    private boolean acceptsContainer(ItemStack stack) {
        if (mode == Mode.BottleSolid) {
            return BOTTLE_RECIPES.stream().anyMatch(recipe -> recipe.container().test(stack));
        }
        return FluidContainers.isFluidContainer(stack);
    }

    // ---------------------------------------------------------------- operation

    /** One operation as it stands now, worked out without changing anything. */
    private interface Operation {
        void run();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        upgradeSlot.tickNoMark();

        Operation operation = getOperation();
        if (operation != null && useEnergy(ENERGY_PER_TICK)) {
            setActive(true);
            progress++;
            if (progress >= OPERATION_LENGTH) {
                operation.run();
                progress = 0;
                markDirty();
            }
        } else {
            if (operation == null) {
                progress = 0;
            }
            setActive(false);
        }
    }

    private Operation getOperation() {
        return switch (mode) {
            case BottleSolid -> bottleSolid();
            case EmptyLiquid -> emptyLiquid();
            case BottleLiquid -> bottleLiquid();
            case EnrichLiquid -> enrichLiquid();
        };
    }

    private Operation bottleSolid() {
        ItemStack container = canInputSlot.get();
        ItemStack fill = inputSlot.get();
        if (container.isEmpty() || fill.isEmpty()) {
            return null;
        }

        BottleRecipe recipe = findBottleRecipe(container, fill, true);
        if (recipe == null || !outputSlot.canAdd(recipe.output())) {
            return null;
        }

        return () -> {
            canInputSlot.consume(recipe.containerAmount());
            inputSlot.consume(recipe.fillAmount());
            outputSlot.add(recipe.output().copy());
        };
    }

    private Operation emptyLiquid() {
        ItemStack container = canInputSlot.get();
        if (container.isEmpty()) {
            return null;
        }

        IItemFluidTank itemTank = FluidContainers.tankOf(container);
        if (itemTank != null) {
            ResourceAmount<FluidVariant> content = itemTank.getFluid(container);
            if (content == null || outputTank.fillInternal(content.resource(), content.amount(), false) < content.amount()
                    || !outputSlot.canAdd(container)) {
                return null;
            }
            return () -> {
                ItemStack emptied = container.copy();
                itemTank.drain(emptied, content.amount(), true);
                outputTank.fillInternal(content.resource(), content.amount(), true);
                canInputSlot.consume(1);
                outputSlot.add(emptied);
            };
        }

        ResourceAmount<FluidVariant> content = FluidContainers.contentOf(container);
        ItemStack emptied = FluidContainers.emptied(container);
        if (content == null || outputTank.fillInternal(content.resource(), content.amount(), false) < content.amount()
                || !emptied.isEmpty() && !outputSlot.canAdd(emptied)) {
            return null;
        }

        return () -> {
            outputTank.fillInternal(content.resource(), content.amount(), true);
            canInputSlot.consume(1);
            if (!emptied.isEmpty()) {
                outputSlot.add(emptied);
            }
        };
    }

    private Operation bottleLiquid() {
        ItemStack container = canInputSlot.get();
        FluidVariant stored = inputTank.getFluid();
        if (container.isEmpty() || inputTank.isEmpty()) {
            return null;
        }

        IItemFluidTank itemTank = FluidContainers.tankOf(container);
        if (itemTank != null) {
            ItemStack filled = container.copyWithCount(1);
            long amount = itemTank.fill(filled, stored, inputTank.getFluidAmount(), true);
            if (amount <= 0 || !outputSlot.canAdd(filled)) {
                return null;
            }
            return () -> {
                inputTank.drainInternal(amount, true);
                canInputSlot.consume(1);
                outputSlot.add(filled);
            };
        }

        if (FluidContainers.contentOf(container) != null) {
            return null;
        }
        long capacity = FluidContainers.capacityOf(container);
        ItemStack filled = FluidContainers.filled(container, stored.getFluid());
        if (capacity <= 0 || inputTank.getFluidAmount() < capacity || filled.isEmpty() || !outputSlot.canAdd(filled)) {
            return null;
        }

        return () -> {
            inputTank.drainInternal(capacity, true);
            canInputSlot.consume(1);
            outputSlot.add(filled);
        };
    }

    private Operation enrichLiquid() {
        ItemStack additive = inputSlot.get();
        if (additive.isEmpty()) {
            return null;
        }

        EnrichRecipe recipe = findEnrichRecipe(inputTank, additive);
        if (recipe == null || recipe.outputFluid() == null) {
            return null;
        }

        // the container in the can slot is filled first, the tank takes the rest
        FluidVariant output = FluidVariant.of(recipe.outputFluid());
        long outputAmount = LiquidUtil.mb(recipe.outputAmount());
        ItemStack container = canInputSlot.get();
        IItemFluidTank itemTank = FluidContainers.tankOf(container);
        ItemStack filledContainer = ItemStack.EMPTY;
        long intoContainer = 0;
        boolean containerToOutput = false;

        if (itemTank != null) {
            filledContainer = container.copyWithCount(1);
            intoContainer = itemTank.fill(filledContainer, output, outputAmount, true);
            containerToOutput = itemTank.getAmount(filledContainer) >= itemTank.getCapacity(filledContainer);
        } else if (!container.isEmpty() && FluidContainers.contentOf(container) == null
                && outputAmount >= FluidContainers.capacityOf(container) && FluidContainers.capacityOf(container) > 0) {
            ItemStack filled = FluidContainers.filled(container, output.getFluid());
            if (!filled.isEmpty() && outputSlot.canAdd(filled)) {
                filledContainer = filled;
                intoContainer = FluidContainers.capacityOf(container);
                containerToOutput = true;
            }
        }
        if (containerToOutput && !outputSlot.canAdd(filledContainer)) {
            filledContainer = ItemStack.EMPTY;
            intoContainer = 0;
            containerToOutput = false;
        }

        long rest = outputAmount - intoContainer;
        if (rest > 0 && outputTank.fillInternal(output, rest, false) < rest) {
            return null;
        }

        ItemStack finalContainer = filledContainer;
        long finalIntoContainer = intoContainer;
        boolean finalToOutput = containerToOutput;
        return () -> {
            inputSlot.consume(recipe.additiveAmount());
            inputTank.drainInternal(LiquidUtil.mb(recipe.inputAmount()), true);
            if (finalIntoContainer > 0) {
                if (finalToOutput) {
                    canInputSlot.consume(1);
                    outputSlot.add(finalContainer);
                } else {
                    canInputSlot.put(finalContainer);
                }
            }
            if (rest > 0) {
                outputTank.fillInternal(output, rest, true);
            }
        };
    }

    // ---------------------------------------------------------------- mode, window

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            progress = 0;
            markDirty();
        }
    }

    /** Swaps the contents of the two tanks, so a result can be worked on further; only between operations. */
    public boolean switchTanks() {
        if (progress != 0) {
            return false;
        }
        FluidVariant input = inputTank.getFluid();
        long inputAmount = inputTank.getFluidAmount();
        inputTank.setFluid(outputTank.getFluid(), outputTank.getFluidAmount());
        outputTank.setFluid(input, inputAmount);
        markDirty();
        return true;
    }

    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        switch (event) {
            case "mode" -> setMode(Mode.values[(mode.ordinal() + 1) % Mode.values.length]);
            case "swapTanks" -> switchTanks();
            default -> super.onGuiEvent(player, event);
        }
    }

    @Override
    public boolean getGuiCondition(String name) {
        return switch (name) {
            case "bottleSolid" -> mode == Mode.BottleSolid;
            case "emptyLiquid" -> mode == Mode.EmptyLiquid;
            case "bottleLiquid" -> mode == Mode.BottleLiquid;
            case "enrichLiquid" -> mode == Mode.EnrichLiquid;
            default -> super.getGuiCondition(name);
        };
    }

    @Override
    public double getGuiValue(String name) {
        return "progress".equals(name) ? (double) progress / OPERATION_LENGTH : super.getGuiValue(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
    }

    @Override
    public double getEnergy() {
        return energyStored;
    }

    @Override
    public boolean useEnergy(double amount) {
        return super.useEnergy(amount);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("mode", mode.ordinal());
        nbt.putShort("progress", progress);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        int ordinal = nbt.getInt("mode");
        mode = ordinal >= 0 && ordinal < Mode.values.length ? Mode.values[ordinal] : Mode.BottleSolid;
        progress = nbt.getShort("progress");
    }
}
