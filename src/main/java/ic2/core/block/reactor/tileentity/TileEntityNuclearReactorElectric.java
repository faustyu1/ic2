package ic2.core.block.reactor.tileentity;

import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;
import ic2.api.reactor.IReactorComponent;
import ic2.api.recipe.ILiquidHeatExchangerManager;
import ic2.api.recipe.Recipes;
import ic2.core.ExplosionIC2;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByManager;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotReactor;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LiquidUtil;
import ic2.core.util.WorldSearchUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Fabric port of TileEntityNuclearReactorElectric.
 *
 * <p>The reactor itself does very little: once every {@link #getTickRate()} ticks it resets its
 * hull values and lets every component in the grid run, first the pulse pass and then the heat
 * pass. Everything - energy, heat, how hot the hull may get - comes out of what the components do
 * during those two passes, which is what makes a 1.12.2 reactor design behave the same here.
 */
public class TileEntityNuclearReactorElectric extends TileEntityInventory implements IReactor, IEnergySource {
    /** Rows of the grid; the columns depend on how many chambers are built around the reactor. */
    private static final int ROWS = 6;
    private static final int BASE_COLS = 3;

    public final InvSlotReactor reactorSlot;

    /** Coolant in, hot coolant out; only reachable from outside while the reactor is fluid cooled. */
    public final Fluids.InternalFluidTank inputTank;
    public final Fluids.InternalFluidTank outputTank;
    public final InvSlotConsumableLiquid coolantInputSlot;
    public final InvSlotConsumableLiquid hotCoolantInputSlot;
    public final InvSlotOutput coolantOutputSlot;
    public final InvSlotOutput hotCoolantOutputSlot;

    private boolean fluidCooled = false;

    private float output = 0.0f;
    private int heat = 0;
    private int maxHeat = 10000;
    private float hem = 1.0f;
    private int updateTicker = 0;
    private int emitHeatBuffer = 0;
    private int emitHeat = 0;

    public TileEntityNuclearReactorElectric(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.reactorSlot = new InvSlotReactor(this, "reactor", BASE_COLS * ROWS * 3);

        this.inputTank = fluids().addTank("inputTank", 10000, InvSlot.Access.NONE, InvSlot.InvSide.ANY,
                Fluids.fluidPredicate(Recipes.liquidHeatupManager));
        this.outputTank = fluids().addTank("outputTank", 10000, InvSlot.Access.NONE, InvSlot.InvSide.ANY,
                fluid -> true);
        this.coolantInputSlot = new InvSlotConsumableLiquidByManager(this, "coolantinputSlot", InvSlot.Access.I, 1,
                InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Drain, Recipes.liquidHeatupManager);
        this.hotCoolantInputSlot = new InvSlotConsumableLiquidByTank(this, "hotcoolinputSlot", InvSlot.Access.I, 1,
                InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Fill, this.outputTank);
        this.coolantOutputSlot = new InvSlotOutput(this, "coolantoutputSlot", 1);
        this.hotCoolantOutputSlot = new InvSlotOutput(this, "hotcoolantoutputSlot", 1);
    }

    /**
     * How much heat one millibucket of coolant carries away, before the fluid's own rate. The
     * config lets a pack change the whole fluid reactor's throughput with one number.
     */
    private static float huOutputModifier() {
        return 40.0f * (float) ConfigUtil.getDouble(MainConfig.get(), "balance/energy/FluidReactor/outputModifier");
    }

    public TileEntityNuclearReactorElectric(BlockPos pos, BlockState state) { this(null, pos, state); }
    public TileEntityNuclearReactorElectric() { this(null, BlockPos.ORIGIN, null); }

    // ---------------------------------------------------------------- tick

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        if (updateTicker++ % getTickRate() != 0) {
            return;
        }

        boolean toFluidCooled = isFluidReactor();
        if (fluidCooled != toFluidCooled) {
            if (toFluidCooled) {
                enableFluidMode();
            } else {
                disableFluidMode();
            }
            fluidCooled = toFluidCooled;
        }

        dropAllUnfittingStuff();

        output = 0.0f;
        maxHeat = 10000;
        hem = 1.0f;

        processChambers();

        if (fluidCooled) {
            processFluidSlots();
            convertHeatToCoolant();
        } else {
            emitHeat = emitHeatBuffer;
            emitHeatBuffer = 0;
        }

        if (calculateHeatEffects()) {
            return;
        }

        setActive(heat >= 1000 || output > 0.0f);
        markDirty();
    }

    private void processFluidSlots() {
        coolantInputSlot.processIntoTank(inputTank, coolantOutputSlot);
        hotCoolantInputSlot.processFromTank(outputTank, hotCoolantOutputSlot);
    }

    /**
     * Turns the heat the components handed over this cycle into hot coolant.
     *
     * <p>Whatever cannot be converted - because the output tank is full, or because the coolant
     * ran out - stays as heat in the hull, which is what makes a fluid reactor melt down when its
     * coolant loop stops.
     */
    private void convertHeatToCoolant() {
        float modifier = huOutputModifier();
        int huOutput = (int) (modifier * emitHeatBuffer);
        int outputRoom = (int) LiquidUtil.toMb(outputTank.getSpace());
        emitHeatBuffer = 0;

        ILiquidHeatExchangerManager manager = Recipes.liquidHeatupManager;
        ILiquidHeatExchangerManager.HeatExchangeProperty prop =
                inputTank.isEmpty() || manager == null ? null : manager.getHeatExchangeProperty(inputTank.getFluid().getFluid());

        if (outputRoom > 0 && prop != null && prop.huPerMB > 0) {
            int fluidOutput = huOutput / prop.huPerMB;
            if (outputTank.canFillFluidType(FluidVariant.of(prop.outputFluid))) {
                int drained;
                if (fluidOutput < outputRoom) {
                    // the heat that did not add up to a whole millibucket waits for the next cycle
                    emitHeatBuffer = (int) ((huOutput % prop.huPerMB) / modifier);
                    drained = (int) LiquidUtil.toMb(inputTank.drainInternal(LiquidUtil.mb(fluidOutput), false));
                } else {
                    drained = (int) LiquidUtil.toMb(inputTank.drainInternal(LiquidUtil.mb(outputRoom), false));
                }

                if (drained > 0) {
                    emitHeat = drained * prop.huPerMB;
                    huOutput -= (int) LiquidUtil.toMb(inputTank.drainInternal(LiquidUtil.mb(drained), true)) * prop.huPerMB;
                    outputTank.fillInternal(FluidVariant.of(prop.outputFluid), LiquidUtil.mb(drained), true);
                } else {
                    emitHeat = 0;
                }
            } else {
                emitHeat = 0;
            }
        } else {
            emitHeat = 0;
        }

        addHeat((int) (huOutput / modifier));
    }

    /**
     * Throws out everything the grid cannot hold: components in columns the chambers no longer
     * unlock, and - in fluid mode - the condensators that a fluid reactor refuses.
     */
    public void dropAllUnfittingStuff() {
        for (int i = 0; i < reactorSlot.size(); i++) {
            ItemStack stack = reactorSlot.get(i);
            if (!stack.isEmpty() && !isUsefulItem(stack, false)) {
                reactorSlot.put(i, ItemStack.EMPTY);
                eject(stack);
            }
        }

        for (int i = reactorSlot.size(); i < reactorSlot.rawSize(); i++) {
            ItemStack stack = reactorSlot.get(i);
            if (!stack.isEmpty()) {
                reactorSlot.put(i, ItemStack.EMPTY);
                eject(stack);
            }
        }
    }

    /**
     * Whether a component belongs in the grid. A fluid reactor takes no fresh heat storage - a
     * used one may stay, so a reactor switched to fluid mode does not throw away hot condensators.
     */
    public boolean isUsefulItem(ItemStack stack, boolean forInsertion) {
        if (stack.isEmpty()) {
            return false;
        }
        if (forInsertion && fluidCooled
                && stack.getItem() instanceof ic2.core.item.reactor.ItemReactorHeatStorage storage
                && storage.getCustomDamage(stack) > 0) {
            return false;
        }
        return stack.getItem() instanceof IReactorComponent;
    }

    private void eject(ItemStack stack) {
        if (world != null && !stack.isEmpty()) {
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        }
    }

    // ---------------------------------------------------------------- fluid mode

    /** A fluid reactor is not on the energy net, so it must not join it when it loads either. */
    @Override
    public void onLoad() {
        if (!fluidCooled) {
            super.onLoad();
        }
    }

    private void enableFluidMode() {
        // a fluid reactor makes no EU, so it leaves the energy net entirely
        onUnloaded();
        openTanks();
    }

    private void disableFluidMode() {
        onLoad();
        closeTanks();
    }

    private void openTanks() {
        fluids().changeConnectivity(inputTank, InvSlot.Access.I, InvSlot.InvSide.ANY);
        fluids().changeConnectivity(outputTank, InvSlot.Access.O, InvSlot.InvSide.ANY);
    }

    private void closeTanks() {
        fluids().changeConnectivity(inputTank, InvSlot.Access.NONE, InvSlot.InvSide.ANY);
        fluids().changeConnectivity(outputTank, InvSlot.Access.NONE, InvSlot.InvSide.ANY);
    }

    @Override
    public boolean isFluidCooled() {
        return fluidCooled;
    }

    /**
     * A reactor is fluid cooled when it is at full size, walled in on all six sides, and no other
     * such reactor is close enough for the two hulls to share casing blocks.
     */
    private boolean isFluidReactor() {
        if (!isFullSize() || !hasFluidChamber()) {
            return false;
        }

        boolean[] conflict = new boolean[1];
        WorldSearchUtil.findTileEntities(world, pos, 4, te -> {
            if (te == this || !(te instanceof TileEntityNuclearReactorElectric other)) {
                return false;
            }
            if (other.isFullSize() && other.hasFluidChamber()) {
                conflict[0] = true;
                return true;
            }
            return false;
        });

        return !conflict[0];
    }

    private boolean isFullSize() {
        return getReactorSize() == MAX_COLS;
    }

    /** Whether the 5x5x5 around the reactor is closed by reactor casing on every face. */
    private boolean hasFluidChamber() {
        int range = 2;
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        // the two horizontal faces, corners included
        for (int i = 0; i < 2; i++) {
            int y = pos.getY() + range * (i * 2 - 1);
            for (int z = pos.getZ() - range; z <= pos.getZ() + range; z++) {
                for (int x = pos.getX() - range; x <= pos.getX() + range; x++) {
                    if (!isFluidChamberBlock(cursor.set(x, y, z))) {
                        return false;
                    }
                }
            }
        }

        // the two faces along z, without the rows the faces above already cover
        for (int i = 0; i < 2; i++) {
            int z = pos.getZ() + range * (i * 2 - 1);
            for (int y = pos.getY() - range + 1; y <= pos.getY() + range - 1; y++) {
                for (int x = pos.getX() - range; x <= pos.getX() + range; x++) {
                    if (!isFluidChamberBlock(cursor.set(x, y, z))) {
                        return false;
                    }
                }
            }
        }

        // the two faces along x, without the edges the four faces above already cover
        for (int i = 0; i < 2; i++) {
            int x = pos.getX() + range * (i * 2 - 1);
            for (int y = pos.getY() - range + 1; y <= pos.getY() + range - 1; y++) {
                for (int z = pos.getZ() - range + 1; z <= pos.getZ() + range - 1; z++) {
                    if (!isFluidChamberBlock(cursor.set(x, y, z))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isFluidChamberBlock(BlockPos pos) {
        return world != null
                && world.getBlockEntity(pos) instanceof IReactorChamber chamber
                && chamber.isWall();
    }

    /** Runs every component twice: the pulse pass, then the heat pass. */
    public void processChambers() {
        int size = getReactorSize();

        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < ROWS; y++) {
                for (int x = 0; x < size; x++) {
                    ItemStack stack = reactorSlot.get(x, y);
                    if (!stack.isEmpty() && stack.getItem() instanceof IReactorComponent component) {
                        component.processChamber(stack, this, x, y, pass == 0);
                    }
                }
            }
        }
    }

    /**
     * What the heat does to the world around the reactor: fire, hurt, dry water up and, at the
     * very top, melt down.
     *
     * @return true when the reactor exploded and must not be touched any further
     */
    public boolean calculateHeatEffects() {
        if (heat < 4000 || world == null) {
            return false;
        }

        float power = (float) heat / maxHeat;
        if (power >= 1.0f) {
            explode();
            return true;
        }

        if (power >= 0.85f && world.random.nextFloat() <= 0.2f * hem) {
            BlockPos coord = getRandCoord(2);
            if (coord != null && world.getBlockState(coord).isAir()) {
                world.setBlockState(coord, Blocks.FIRE.getDefaultState());
            }
        }

        if (power >= 0.7f) {
            Box area = new Box(pos.getX() - 3, pos.getY() - 3, pos.getZ() - 3,
                    pos.getX() + 4, pos.getY() + 4, pos.getZ() + 4);
            for (LivingEntity entity : world.getNonSpectatingEntities(LivingEntity.class, area)) {
                entity.damage(world.getDamageSources().magic(), (int) (world.random.nextInt(4) * hem));
            }
        }

        if (power >= 0.5f && world.random.nextFloat() <= hem) {
            BlockPos coord = getRandCoord(2);
            if (coord != null && world.getBlockState(coord).isOf(Blocks.WATER)) {
                world.removeBlock(coord, false);
            }
        }

        if (power >= 0.4f && world.random.nextFloat() <= hem) {
            BlockPos coord = getRandCoord(2);
            if (coord != null && world.getBlockEntity(coord) == null
                    && world.getBlockState(coord).isBurnable()) {
                world.setBlockState(coord, Blocks.FIRE.getDefaultState());
            }
        }

        return false;
    }

    /** A random block position around the reactor, never the reactor itself. */
    public BlockPos getRandCoord(int radius) {
        if (radius <= 0 || world == null) {
            return null;
        }

        BlockPos ret;
        do {
            ret = pos.add(world.random.nextInt(2 * radius + 1) - radius,
                    world.random.nextInt(2 * radius + 1) - radius,
                    world.random.nextInt(2 * radius + 1) - radius);
        } while (ret.equals(pos));

        return ret;
    }

    // ---------------------------------------------------------------- IReactor

    @Override public BlockPos getPosition() { return pos; }
    @Override public World getWorldObj() { return world; }
    @Override public BlockEntity getCoreTe() { return this; }
    @Override public int getHeat() { return heat; }
    @Override public void setHeat(int heat) { this.heat = heat; }

    @Override
    public int addHeat(int amount) {
        heat += amount;
        return heat;
    }

    @Override public int getMaxHeat() { return maxHeat; }
    @Override public void setMaxHeat(int maxHeat) { this.maxHeat = maxHeat; }
    @Override public void addEmitHeat(int heat) { emitHeatBuffer += heat; }
    @Override public float getHeatEffectModifier() { return hem; }
    @Override public void setHeatEffectModifier(float modifier) { hem = modifier; }
    @Override public float getReactorEnergyOutput() { return output; }
    @Override public double getReactorEUEnergyOutput() { return getOfferedEnergy(); }
    @Override public float addOutput(float energy) { return output += energy; }
    @Override public int getTickRate() { return 20; }

    /** The reactor only runs while it has a redstone signal, as it did in 1.12.2. */
    @Override
    public boolean produceEnergy() {
        return world != null && hasRedstoneInput()
                && ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/nuclear") > 0.0;
    }

    /**
     * Whether the reactor is powered: directly, through one of its chambers, or - built into a
     * fluid reactor's hull - through a reactor redstone port of that hull. 1.12.2 linked the
     * chambers' and ports' redstone components to the reactor's for this.
     */
    public boolean hasRedstoneInput() {
        if (world.isReceivingRedstonePower(pos)) {
            return true;
        }

        for (Direction dir : Direction.values()) {
            BlockPos side = pos.offset(dir);
            if (world.getBlockEntity(side) instanceof TileEntityReactorChamberElectric && world.isReceivingRedstonePower(side)) {
                return true;
            }
        }

        if (fluidCooled) {
            BlockPos.Mutable cursor = new BlockPos.Mutable();
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        cursor.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                        if (world.getBlockEntity(cursor) instanceof TileEntityReactorRedstonePort
                                && world.isReceivingRedstonePower(cursor)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack getItemAt(int x, int y) {
        return x >= 0 && x < getReactorSize() && y >= 0 && y < ROWS ? reactorSlot.get(x, y) : ItemStack.EMPTY;
    }

    @Override
    public void setItemAt(int x, int y, ItemStack stack) {
        if (x >= 0 && x < getReactorSize() && y >= 0 && y < ROWS) {
            reactorSlot.put(x, y, stack == null ? ItemStack.EMPTY : stack);
        }
    }

    /** Three columns of its own plus one for every reactor chamber built against it. */
    public int getReactorSize() {
        if (world == null) {
            return MAX_COLS;
        }

        int cols = BASE_COLS;
        for (Direction dir : Direction.values()) {
            if (world.getBlockEntity(pos.offset(dir)) instanceof TileEntityReactorChamberElectric) {
                cols++;
            }
        }
        return cols;
    }

    private static final int MAX_COLS = 9;

    /**
     * Melts the reactor down. Every component says what it does to the blast - fuel makes it
     * bigger, plating damps it, reflectors shrink it - and the chambers go with it.
     */
    @Override
    public void explode() {
        if (world == null || world.isClient) {
            return;
        }

        float boomPower = 10.0f;
        float boomMod = 1.0f;

        for (int i = 0; i < reactorSlot.rawSize(); i++) {
            ItemStack stack = reactorSlot.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IReactorComponent component) {
                float influence = component.influenceExplosion(stack, this);
                if (influence > 0.0f && influence < 1.0f) {
                    boomMod *= influence;
                } else {
                    boomPower += influence;
                }
            }
            reactorSlot.put(i, ItemStack.EMPTY);
        }

        boomPower *= hem * boomMod;
        double limit = ConfigUtil.getDouble(MainConfig.get(), "protection/reactorExplosionPowerLimit");
        if (limit > 0) {
            boomPower = (float) Math.min(boomPower, limit);
        }

        for (Direction dir : Direction.values()) {
            BlockPos side = pos.offset(dir);
            if (world.getBlockEntity(side) instanceof TileEntityReactorChamberElectric) {
                world.removeBlock(side, false);
            }
        }

        world.removeBlock(pos, false);
        new ExplosionIC2(world, null, pos, boomPower, 0.01F, ExplosionIC2.Type.Nuclear).doExplosion();
    }

    // ---------------------------------------------------------------- IEnergySource

    @Override public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction side) { return !fluidCooled; }
    @Override public int getSourceTier() { return 5; }

    /** A fluid reactor puts its heat into the coolant loop instead, so it offers no EU at all. */
    @Override
    public double getOfferedEnergy() {
        return fluidCooled ? 0.0
                : output * 5.0 * ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/nuclear");
    }

    @Override
    public void drawEnergy(double amount) {
        // the reactor produces its output anew every cycle, so nothing is buffered here
    }

    /** Heat the reactor handed to its coolant this cycle; the fluid port reads it. */
    public int getEmitHeat() {
        return emitHeat;
    }

    @Override
    public double getGuiValue(String name) {
        return switch (name) {
            case "heat" -> maxHeat == 0 ? 0.0 : (double) heat / maxHeat;
            case "output" -> output;
            default -> super.getGuiValue(name);
        };
    }

    /** Lets the guidef hide the coolant part of the screen while the reactor makes EU. */
    @Override
    public boolean getGuiCondition(String name) {
        return switch (name) {
            case "fluidCooled" -> fluidCooled;
            case "electric" -> !fluidCooled;
            default -> super.getGuiCondition(name);
        };
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("heat", heat);
        nbt.putFloat("output", output);
        nbt.putBoolean("fluidCooled", fluidCooled);
        nbt.putInt("emitHeat", emitHeat);
        nbt.putInt("emitHeatBuffer", emitHeatBuffer);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        heat = nbt.getInt("heat");
        output = nbt.getFloat("output");
        fluidCooled = nbt.getBoolean("fluidCooled");
        emitHeat = nbt.getInt("emitHeat");
        emitHeatBuffer = nbt.getInt("emitHeatBuffer");
        if (fluidCooled) {
            openTanks();
        }
    }
}
