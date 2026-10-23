package ic2.core.crop;

import ic2.api.crops.BaseSeed;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.crops.ICropTile;
import ic2.core.IC2;
import ic2.core.block.TileEntityBlock;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Items;
import ic2.core.item.ItemCropSeed;
import ic2.core.recipe.OreDictionaryCompat;
import ic2.core.util.FluidContainers;
import ic2.core.util.LiquidUtil;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric port of TileEntityCrop - the crop stick and whatever grows on it.
 *
 * <p>Every 256 ticks the crop updates its view of the soil (humidity, nutrients, air quality, one
 * of them per tick in turn), grows by an amount that depends on how well that environment suits
 * its tier and stats, and drinks some of the water and fertilizer it was given. An empty stick
 * slowly grows weed unless it holds Weed-EX; a cross-crop stick instead tries to breed its
 * neighbours. Mature weeds spread to the crops around them and turn bare soil into grass.
 *
 * <p>What changed from 1.12.2 is only the plumbing: the client learns the crop from the vanilla
 * block entity update packet instead of IC2's field sync, and draws it from
 * {@link #getRenderData()} instead of an extended block state.
 */
public class TileEntityCrop extends TileEntityBlock implements ICropTile, RenderDataBlockEntity {
    public static int tickrate = 256;

    /** -1 until the first tick, which spreads the crops' updates over the tick rate. */
    private long ticker = -1L;
    public boolean dirty = true;
    private CropCard crop = null;
    private byte biomeHumidityBonus = 0;
    private boolean biomeHumidityKnown = false;
    private byte statGrowth;
    private byte statGain;
    private byte statResistance;
    private short storageNutrients;
    private short storageWater;
    private short storageWeedEX;
    private byte terrainAirQuality;
    private byte terrainHumidity;
    private byte terrainNutrients;
    private byte currentSize;
    private short growthPoints = 0;
    private byte scanLevel;
    private boolean crossingBase;
    private NbtCompound customData = new NbtCompound();

    public TileEntityCrop(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CROP_BE : type, pos, state);
    }

    public TileEntityCrop(BlockPos pos, BlockState state) {
        this(IC2Blocks.CROP_BE, pos, state);
    }

    public TileEntityCrop() {
        this(IC2Blocks.CROP_BE, BlockPos.ORIGIN, null);
    }

    // ---------------------------------------------------------------- persistence and sync

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        crossingBase = nbt.getBoolean("crossingBase");

        if (nbt.contains("cropOwner") && nbt.contains("cropId")) {
            crop = Crops.instance == null ? null : Crops.instance.getCropCard(nbt.getString("cropOwner"), nbt.getString("cropId"));
            statGrowth = nbt.getByte("statGrowth");
            statGain = nbt.getByte("statGain");
            statResistance = nbt.getByte("statResistance");
            storageNutrients = nbt.getShort("storageNutrients");
            storageWater = nbt.getShort("storageWater");
            storageWeedEX = nbt.getShort("storageWeedEX");
            terrainHumidity = nbt.getByte("terrainHumidity");
            terrainNutrients = nbt.getByte("terrainNutrients");
            terrainAirQuality = nbt.getByte("terrainAirQuality");
            currentSize = nbt.getByte("currentSize");
            growthPoints = nbt.getShort("growthPoints");
            scanLevel = nbt.getByte("scanLevel");
            customData = nbt.getCompound("customData");
        } else {
            crop = null;
        }

        // a sync from the server changed what the stick shows, so its chunk section is redrawn
        if (world != null && world.isClient) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("crossingBase", crossingBase);

        if (crop != null) {
            nbt.putString("cropOwner", crop.getOwner());
            nbt.putString("cropId", crop.getId());
            nbt.putByte("statGrowth", statGrowth);
            nbt.putByte("statGain", statGain);
            nbt.putByte("statResistance", statResistance);
            nbt.putShort("storageNutrients", storageNutrients);
            nbt.putShort("storageWater", storageWater);
            nbt.putShort("storageWeedEX", storageWeedEX);
            nbt.putByte("terrainHumidity", terrainHumidity);
            nbt.putByte("terrainNutrients", terrainNutrients);
            nbt.putByte("terrainAirQuality", terrainAirQuality);
            nbt.putByte("currentSize", currentSize);
            nbt.putShort("growthPoints", growthPoints);
            nbt.putByte("scanLevel", scanLevel);
            nbt.put("customData", customData.copy());
        }
    }

    /** The client draws the crop, so it gets the whole state - the same fields IC2 networked. */
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    /** What the crop model draws: the plant and its size, or the bare or crossing stick. */
    @Override
    public Object getRenderData() {
        return new CropRenderState(crop, currentSize, crossingBase);
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        if (ticker < 0L) {
            ticker = IC2.random.nextInt(tickrate);
        }
        ticker++;

        if (!biomeHumidityKnown) {
            updateBiomeHumidityBonus();
        }

        if (ticker % tickrate == 0L) {
            performTick();
        }

        if (dirty) {
            dirty = false;
            markDirty();
            BlockState state = world.getBlockState(pos);
            BlockState lit = BlockCrop.withLight(state, getLightValue());
            if (lit != state) {
                world.setBlockState(pos, lit, Block.NOTIFY_ALL);
            } else {
                world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
            }
        }
    }

    public void performTick() {
        if (ticker % ((long) tickrate * 10 << 2) == 0L) {
            updateBiomeHumidityBonus();
        }
        if (ticker % ((long) tickrate << 2) == 0L) {
            updateTerrainHumidity();
        }
        if ((ticker + tickrate) % ((long) tickrate << 2) == 0L) {
            updateTerrainNutrients();
        }
        if ((ticker + tickrate * 2L) % ((long) tickrate << 2) == 0L) {
            updateTerrainAirQuality();
        }

        if (crop == null && (!isCrossingBase() || !attemptCrossing()) && (!isCrossingBase() || !attemptSpreading())) {
            if (IC2.random.nextInt(100) != 0 || getStorageWeedEX() > 0) {
                if (getStorageWeedEX() > 0 && IC2.random.nextInt(10) == 0) {
                    storageWeedEX--;
                }
                return;
            }

            reset();
            crop = Crops.weed;
            setCurrentSize(1);
        }

        crop.tick(this);

        if (crop.canGrow(this)) {
            performGrowthTick();
            if (crop == null) {
                return;
            }

            if (growthPoints >= crop.getGrowthDuration(this)) {
                growthPoints = 0;
                setCurrentSize(getCurrentSize() + 1);
                dirty = true;
            }
        }

        if (storageNutrients > 0) {
            storageNutrients--;
        }
        if (storageWater > 0) {
            storageWater--;
        }

        if (crop.isWeed(this) && IC2.random.nextInt(50) - getStatGrowth() <= 2) {
            performWeedWork();
        }
    }

    public void performGrowthTick() {
        if (crop == null) {
            return;
        }

        int baseGrowth = 3 + IC2.random.nextInt(7) + getStatGrowth();
        int minimumQuality = CropMath.minimumQuality(crop.getProperties().getTier(), getStatGrowth(), statGain, statResistance);
        int providedQuality = crop.getWeightInfluences(this, getTerrainHumidity(), getTerrainNutrients(), getTerrainAirQuality()) * 5;
        int totalGrowth = CropMath.growthPoints(baseGrowth, minimumQuality, providedQuality, statResistance, IC2.random);

        if (totalGrowth < 0) {
            reset();
            totalGrowth = 0;
        }

        growthPoints = (short) (growthPoints + totalGrowth);
    }

    /**
     * A mature weed reaches for one side: a crop there turns into weed unless it resists or holds
     * Weed-EX, bare soil there grows grass.
     */
    public void performWeedWork() {
        BlockPos dstPos = pos.offset(Direction.fromHorizontal(IC2.random.nextInt(4)));
        BlockEntity dstRaw = world.getBlockEntity(dstPos);

        if (dstRaw instanceof TileEntityCrop neighbour) {
            CropCard neighborCrop = neighbour.getCrop();
            if (neighborCrop == null
                    || !neighborCrop.isWeed(neighbour) && IC2.random.nextInt(32) >= neighbour.getStatResistance() && !neighbour.hasWeedEX()) {
                int newGrowth = Math.max(getStatGrowth(), neighbour.getStatGrowth());
                if (newGrowth < 31 && IC2.random.nextBoolean()) {
                    newGrowth++;
                }

                neighbour.reset();
                neighbour.crop = Crops.weed;
                neighbour.setCurrentSize(1);
                neighbour.setStatGrowth(newGrowth);
            }
        } else if (world.isAir(dstPos)) {
            BlockPos soilPos = dstPos.down();
            Block block = world.getBlockState(soilPos).getBlock();
            if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.FARMLAND) {
                world.setBlockState(soilPos, Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
                world.setBlockState(dstPos, Blocks.SHORT_GRASS.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }

    /** Weed-EX turns the weed away, and is used up a little for every attempt it stops. */
    public boolean hasWeedEX() {
        if (storageWeedEX > 0) {
            storageWeedEX = (short) (storageWeedEX - 5);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- interaction

    /**
     * Right click with whatever the player holds: another stick makes this a cross-crop, fertilizer
     * and fluids are stored, a base seed is planted; anything else is the crop's own business,
     * which for most crops means harvesting.
     *
     * @return true when the click was used, so the held item does not act on its own
     */
    public boolean rightClick(PlayerEntity player, Hand hand) {
        ItemStack heldItem = player.getStackInHand(hand);
        boolean creative = player.getAbilities().creativeMode;

        if (!heldItem.isEmpty()) {
            if (crop == null && !crossingBase && heldItem.isOf(IC2Items.CROP_STICK)) {
                if (!creative) {
                    heldItem.decrement(1);
                }
                crossingBase = true;
                dirty = true;
                return true;
            }

            if (crop != null && heldItem.isOf(fertilizer())) {
                if (applyFertilizer(true)) {
                    dirty = true;
                }
                if (!creative) {
                    heldItem.decrement(1);
                }
                return true;
            }

            if (FluidContainers.isFluidContainer(heldItem)) {
                if (applyHydration(player, hand) || applyWeedEx(player, hand, true)) {
                    dirty = true;
                }
                return true;
            }

            BaseSeed baseSeed = Crops.instance.getBaseSeed(heldItem);
            if (crop == null && !crossingBase && baseSeed != null) {
                reset();
                setCrop(baseSeed.crop);
                currentSize = (byte) baseSeed.size;
                statGain = (byte) baseSeed.statGain;
                statGrowth = (byte) baseSeed.statGrowth;
                statResistance = (byte) baseSeed.statResistance;
                if (!creative) {
                    heldItem.decrement(1);
                }
                return true;
            }
        }

        return crop != null && crop.onRightClick(this, player);
    }

    /** Left click: the crop decides, by default it is picked; an empty cross-crop loses its stick. */
    public void leftClick(PlayerEntity player) {
        if (crop != null) {
            crop.onLeftClick(this, player);
        } else if (crossingBase) {
            crossingBase = false;
            dirty = true;
            Block.dropStack(world, pos, new ItemStack(IC2Items.CROP_STICK));
        }
    }

    /** A seed bag planted into the stick; weed and cross-crops do not take one. */
    public boolean tryPlantIn(CropCard crop, int size, int statGr, int statGa, int statRe, int scan) {
        if (crop == null || crop == Crops.weed || isCrossingBase()) {
            return false;
        }
        if (!crop.canGrow(this)) {
            return false;
        }

        reset();
        setCrop(crop);
        setCurrentSize(size);
        setStatGain(statGa);
        setStatGrowth(statGr);
        setStatResistance(statRe);
        setScanLevel(scan);
        dirty = true;
        return true;
    }

    /** Walking through a crop may trample it, which turns the farmland back into dirt. */
    public void onEntityCollision(Entity entity) {
        if (crop == null || !crop.onEntityCollision(this, entity) || world.isClient) {
            return;
        }

        if (IC2.random.nextInt(100) == 0 && IC2.random.nextInt(40) > statResistance) {
            reset();
            world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    /** The stick needs farmland under it; without it the crop is picked and the stick is gone. */
    public void onNeighborChange() {
        if (!world.getBlockState(pos.down()).isOf(Blocks.FARMLAND)) {
            pick();
            world.removeBlock(pos, false);
        }
    }

    /** Called before the player breaks the block: the plant is picked, the stick drops as loot. */
    public void onBlockBreak() {
        if (!world.isClient) {
            pick();
        }
    }

    /** What pick block gives: the seed bag of the plant, or the bare stick. */
    public ItemStack getPickStack() {
        return crop == null
                ? new ItemStack(IC2Items.CROP_STICK)
                : generateSeeds(crop, statGrowth, statGain, statResistance, scanLevel);
    }

    // ---------------------------------------------------------------- environment

    public void updateTerrainAirQuality() {
        int value = 0;
        int height = (int) Math.floor((double) (pos.getY() - 40) / 15.0);
        value += CropMath.clamp(height, 0, 2);

        int fresh = 9;
        // only the 2x2 corner towards negative x and z is looked at - the bounds are the original's
        for (int x = pos.getX() - 1; x < pos.getX() + 1 && fresh > 0; x++) {
            for (int z = pos.getZ() - 1; z < pos.getZ() + 1 && fresh > 0; z++) {
                BlockPos other = new BlockPos(x, pos.getY(), z);
                if (world.getBlockState(other).isSolidBlock(world, other) || world.getBlockEntity(other) instanceof TileEntityCrop) {
                    fresh--;
                }
            }
        }

        value += fresh / 2;
        if (world.isSkyVisible(pos.up())) {
            value += 4;
        }

        setTerrainAirQuality(value);
    }

    public void updateTerrainHumidity() {
        int humidity = biomeHumidityBonus;
        BlockState soil = world.getBlockState(pos.down());
        if (soil.contains(FarmlandBlock.MOISTURE) && soil.get(FarmlandBlock.MOISTURE) >= FarmlandBlock.MAX_MOISTURE) {
            humidity += 2;
        }
        if (storageWater >= 5) {
            humidity += 2;
        }

        humidity += (storageWater + 24) / 25;
        setTerrainHumidity(humidity);
    }

    public void updateTerrainNutrients() {
        int nutrients = Crops.instance.getNutrientBiomeBonus(world.getBiome(pos));

        for (int i = 1; i < 5 && world.getBlockState(pos.down(i)).isOf(Blocks.DIRT); i++) {
            nutrients++;
        }

        nutrients += (storageNutrients + 19) / 20;
        setTerrainNutrients(nutrients);
    }

    private void updateBiomeHumidityBonus() {
        Biome biome = world.getBiome(pos).value();
        biomeHumidityBonus = (byte) CropMath.biomeHumidityBonus(biome.weather.downfall, biome.getTemperature(pos));
        biomeHumidityKnown = true;
    }

    // ---------------------------------------------------------------- inputs

    /** Water from the held bucket or cell, up to 200; the emptied container goes back. */
    public boolean applyHydration(PlayerEntity player, Hand hand) {
        return drainHeld(player, hand, "water", 200, true);
    }

    /** Weed-EX from the held cell; a player tops up to 100, the Crop-Matron to 150. */
    public boolean applyWeedEx(PlayerEntity player, Hand hand, boolean manual) {
        return drainHeld(player, hand, "ic2weed_ex", manual ? 100 : 150, false);
    }

    /**
     * Takes water from a tank, as the Crop-Matron hands it over.
     *
     * @param available how much the tank holds
     * @return how much was taken
     */
    public int applyHydration(int available) {
        int taken = Math.min(available, Math.max(0, 200 - storageWater));
        storageWater = (short) (storageWater + taken);
        return taken;
    }

    /** Takes Weed-EX from a tank; see {@link #applyHydration(int)}. */
    public int applyWeedEx(int available, boolean manual) {
        int limit = manual ? 100 : 150;
        int taken = Math.min(available, Math.max(0, limit - storageWeedEX));
        storageWeedEX = (short) (storageWeedEX + taken);
        return taken;
    }

    /**
     * Pours a held container into a store.
     *
     * <p>1.12.2 drained just the missing amount through the container's fluid handler; buckets
     * and cells only ever give their whole content, so here the whole container is used and the
     * store takes what fits, which is what a player could do with them there as well.
     */
    private boolean drainHeld(PlayerEntity player, Hand hand, String fluidName, int limit, boolean water) {
        int stored = water ? storageWater : storageWeedEX;
        if (stored >= limit) {
            return false;
        }

        ItemStack held = player.getStackInHand(hand);
        ResourceAmount<FluidVariant> content = FluidContainers.contentOf(held);
        if (content == null || !fluidName.equals(LiquidUtil.nameOf(content.resource().getFluid())) || content.amount() <= 0) {
            return false;
        }

        int taken = (int) Math.min(LiquidUtil.toMb(content.amount()), limit - stored);
        if (water) {
            storageWater = (short) (storageWater + taken);
        } else {
            storageWeedEX = (short) (storageWeedEX + taken);
        }

        if (!player.getAbilities().creativeMode) {
            ItemStack emptied = FluidContainers.emptied(held);
            if (held.getCount() == 1) {
                player.setStackInHand(hand, emptied);
            } else {
                held.decrement(1);
                if (!emptied.isEmpty() && !player.getInventory().insertStack(emptied)) {
                    player.dropItem(emptied, false);
                }
            }
        }
        return true;
    }

    public boolean applyFertilizer(boolean manual) {
        if (storageNutrients >= 100) {
            return false;
        }
        storageNutrients = (short) (storageNutrients + (manual ? 100 : 90));
        return true;
    }

    /** The fertilizer item; it is a crop_res variant, so it is looked up rather than held. */
    public static Item fertilizer() {
        Item item = IC2ItemVariants.get("crop_res", "fertilizer");
        return item == null ? Items.AIR : item;
    }

    // ---------------------------------------------------------------- crossing

    private boolean attemptCrossing() {
        if (IC2.random.nextInt(3) != 0) {
            return false;
        }

        List<TileEntityCrop> neighbours = new ArrayList<>(4);
        checkCrossingAvailability(pos.north(), neighbours);
        checkCrossingAvailability(pos.south(), neighbours);
        checkCrossingAvailability(pos.east(), neighbours);
        checkCrossingAvailability(pos.west(), neighbours);
        if (neighbours.size() < 2) {
            return false;
        }

        CropCard[] crops = Crops.instance.getCrops().toArray(new CropCard[0]);
        if (crops.length == 0) {
            return false;
        }

        int[] ratios = new int[crops.length];
        int total = 0;
        for (int index = 0; index < ratios.length; index++) {
            CropCard candidate = crops[index];
            if (candidate.canGrow(this)) {
                for (TileEntityCrop neighbour : neighbours) {
                    total += CropMath.crossingRatio(candidate, neighbour.getCrop());
                }
            }
            ratios[index] = total;
        }

        if (total <= 0) {
            return false;
        }

        int chosen = CropMath.pickWeighted(ratios, IC2.random.nextInt(total));

        setCrossingBase(false);
        setCrop(crops[chosen]);
        dirty = true;
        setCurrentSize(1);

        int growth = 0;
        int resistance = 0;
        int gain = 0;
        for (TileEntityCrop neighbour : neighbours) {
            growth += neighbour.statGrowth;
            resistance += neighbour.statResistance;
            gain += neighbour.statGain;
        }

        int count = neighbours.size();
        growth = growth / count + IC2.random.nextInt(1 + 2 * count) - count;
        gain = gain / count + IC2.random.nextInt(1 + 2 * count) - count;
        resistance = resistance / count + IC2.random.nextInt(1 + 2 * count) - count;
        statGrowth = (byte) CropMath.clamp(growth, 0, 31);
        statGain = (byte) CropMath.clamp(gain, 0, 31);
        statResistance = (byte) CropMath.clamp(resistance, 0, 31);
        return true;
    }

    /** With exactly one crop beside it, an empty cross-crop may simply take a copy of it. */
    private boolean attemptSpreading() {
        List<TileEntityCrop> neighbours = new ArrayList<>(4);
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (world.getBlockEntity(pos.offset(direction)) instanceof TileEntityCrop sideCrop) {
                neighbours.add(sideCrop);
            }
        }

        if (neighbours.size() != 1) {
            return false;
        }

        TileEntityCrop sideCrop = neighbours.get(0);
        CropCard neighborCrop = sideCrop.getCrop();
        if (neighborCrop == null || !neighborCrop.canGrow(this) || !neighborCrop.canCross(sideCrop)) {
            return false;
        }

        if (CropMath.spreadBase(sideCrop.statGrowth, sideCrop.statResistance) < IC2.random.nextInt(16)) {
            return false;
        }

        setCrossingBase(false);
        setCrop(sideCrop.crop);
        dirty = true;
        setCurrentSize(1);
        statGrowth = sideCrop.statGrowth;
        statResistance = sideCrop.statResistance;
        statGain = sideCrop.statGain;
        return true;
    }

    private void checkCrossingAvailability(BlockPos other, List<TileEntityCrop> crops) {
        if (!(world.getBlockEntity(other) instanceof TileEntityCrop sideCrop)) {
            return;
        }

        CropCard neighborCrop = sideCrop.getCrop();
        if (neighborCrop != null && neighborCrop.canGrow(this) && neighborCrop.canCross(sideCrop)
                && CropMath.spreadBase(sideCrop.statGrowth, sideCrop.statResistance) >= IC2.random.nextInt(16)) {
            crops.add(sideCrop);
        }
    }

    /** The wrench leaves it alone, as in 1.12.2. */
    @Override
    public boolean wrenchCanRemove(net.minecraft.entity.player.PlayerEntity player) {
        return false;
    }

    // ---------------------------------------------------------------- ICropTile

    @Override public CropCard getCrop() { return crop; }

    @Override
    public void setCrop(CropCard crop) {
        this.crop = crop;
        if (world != null && !world.isClient) {
            updateTerrainHumidity();
            updateTerrainNutrients();
            updateTerrainAirQuality();
        }
    }

    @Override public int getCurrentSize() { return currentSize; }
    @Override public void setCurrentSize(int size) { currentSize = (byte) size; }

    @Override public int getStatGrowth() { return statGrowth; }
    @Override public void setStatGrowth(int growth) { statGrowth = (byte) growth; }

    @Override public int getStatGain() { return statGain; }
    @Override public void setStatGain(int gain) { statGain = (byte) gain; }

    @Override public int getStatResistance() { return statResistance; }
    @Override public void setStatResistance(int resistance) { statResistance = (byte) resistance; }

    @Override public int getStorageNutrients() { return storageNutrients; }
    @Override public void setStorageNutrients(int nutrients) { storageNutrients = (short) nutrients; }

    @Override public int getStorageWater() { return storageWater; }
    @Override public void setStorageWater(int water) { storageWater = (short) water; }

    @Override public int getStorageWeedEX() { return storageWeedEX; }
    @Override public void setStorageWeedEX(int weedEx) { storageWeedEX = (short) weedEx; }

    @Override public int getTerrainAirQuality() { return terrainAirQuality; }
    public void setTerrainAirQuality(int value) { terrainAirQuality = (byte) value; }

    @Override public int getTerrainHumidity() { return terrainHumidity; }
    public void setTerrainHumidity(int humidity) { terrainHumidity = (byte) humidity; }

    @Override public int getTerrainNutrients() { return terrainNutrients; }
    public void setTerrainNutrients(int nutrients) { terrainNutrients = (byte) nutrients; }

    @Override public int getScanLevel() { return scanLevel; }
    @Override public void setScanLevel(int level) { scanLevel = (byte) level; }

    @Override public int getGrowthPoints() { return growthPoints; }
    @Override public void setGrowthPoints(int points) { growthPoints = (short) points; }

    @Override public boolean isCrossingBase() { return crossingBase; }
    @Override public void setCrossingBase(boolean crossingBase) { this.crossingBase = crossingBase; }

    @Override public NbtCompound getCustomData() { return customData; }

    @Override public BlockPos getPosition() { return pos; }
    @Override public World getWorldObj() { return world; }
    @Override @Deprecated public World getWorld() { return world; }
    @Override @Deprecated public BlockPos getLocation() { return pos; }

    @Override
    public int getLightLevel() {
        return world == null ? 0 : world.getLightLevel(pos);
    }

    /** The light the plant gives off, which the block carries in its state. */
    public int getLightValue() {
        return crop == null ? 0 : crop.getEmittedLight(this);
    }

    /**
     * Picks the plant: an unripe one may give back its seed, a ripe one up to two, a seed
     * being likelier for a resistant or fast-growing plant and rarer for a very productive one.
     */
    @Override
    public boolean pick() {
        if (crop == null) {
            return false;
        }

        boolean bonus = crop.canBeHarvested(this);
        float firstchance = crop.dropSeedChance(this);
        firstchance = (float) ((double) firstchance * Math.pow(1.1, statResistance));
        int dropCount = 0;

        if (bonus) {
            if (world.random.nextFloat() <= (firstchance + 1.0F) * 0.8F) {
                dropCount++;
            }

            float chance = crop.dropSeedChance(this) + (float) getStatGrowth() / 100.0F;
            for (int index = 23; index < statGain; index++) {
                chance *= 0.95F;
            }

            if (world.random.nextFloat() <= chance) {
                dropCount++;
            }
        } else if (world.random.nextFloat() <= firstchance * 1.5F) {
            dropCount++;
        }

        List<ItemStack> drops = new ArrayList<>(dropCount);
        for (int index = 0; index < dropCount; index++) {
            drops.add(crop.getSeeds(this));
        }

        reset();
        if (!world.isClient) {
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    Block.dropStack(world, pos, drop);
                }
            }
        }

        return true;
    }

    @Override
    public boolean performManualHarvest() {
        List<ItemStack> dropItems = performHarvest();
        if (dropItems == null || dropItems.isEmpty()) {
            return false;
        }

        for (ItemStack stack : dropItems) {
            Block.dropStack(world, pos, stack);
        }
        return true;
    }

    /**
     * Harvests a ripe plant: the number of drops scatters around the crop's drop chance, which
     * gain raises, and each drop may come out one bigger, likelier the higher the gain.
     *
     * @return the drops, or null when the plant is not ripe
     */
    @Override
    public List<ItemStack> performHarvest() {
        if (crop == null || !crop.canBeHarvested(this)) {
            return null;
        }

        double chance = crop.dropGainChance() * Math.pow(1.03, getStatGain());
        int dropCount = CropMath.harvestCount(chance, IC2.random.nextGaussian());

        List<ItemStack> ret = new ArrayList<>();
        for (int i = 0; i < dropCount; i++) {
            for (ItemStack drop : crop.getGains(this)) {
                if (drop == null || drop.isEmpty()) {
                    continue;
                }
                ItemStack copy = drop.copy();
                if (IC2.random.nextInt(100) <= getStatGain()) {
                    copy.increment(1);
                }
                ret.add(copy);
            }
        }

        setCurrentSize(crop.getSizeAfterHarvest(this));
        dirty = true;
        return ret;
    }

    @Override
    public void reset() {
        crop = null;
        customData = new NbtCompound();
        statGain = 0;
        statResistance = 0;
        statGrowth = 0;
        terrainAirQuality = -1;
        terrainHumidity = -1;
        terrainNutrients = -1;
        growthPoints = 0;
        scanLevel = 0;
        currentSize = 1;
        dirty = true;
    }

    @Override
    public void updateState() {
        dirty = true;
    }

    /** Whether the block is somewhere under the crop, within the reach of its roots. */
    @Override
    public boolean isBlockBelow(Block required) {
        if (crop == null) {
            return false;
        }

        for (int index = 1; index < crop.getRootsLength(this); index++) {
            BlockState state = world.getBlockState(pos.down(index));
            if (state.isAir()) {
                return false;
            }
            if (state.isOf(required)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a block of an ore dictionary kind is under the crop. The kinds are tags since the
     * flattening ({@code oreIron} is {@code c:ores/iron}), so the block's item is tested against
     * the tag the name stands for.
     */
    @Override
    public boolean isBlockBelow(String oreDictionaryEntry) {
        if (crop == null) {
            return false;
        }

        var tag = OreDictionaryCompat.getTag(oreDictionaryEntry);
        for (int index = 1; index < crop.getRootsLength(this); index++) {
            BlockState state = world.getBlockState(pos.down(index));
            if (state.isAir()) {
                return false;
            }
            if (new ItemStack(state.getBlock().asItem()).isIn(tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack generateSeeds(CropCard crop, int growth, int gain, int resistance, int scan) {
        return ItemCropSeed.generateItemStackFromValues(crop, growth, gain, resistance, scan);
    }

    /** What the crop model needs to know to draw the stick. */
    public record CropRenderState(CropCard crop, int size, boolean crosscrop) {
    }
}
