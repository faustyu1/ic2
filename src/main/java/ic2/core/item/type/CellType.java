package ic2.core.item.type;

import ic2.core.block.state.IIdProvider;

/**
 * Fabric port of ic2.core.item.type.CellType.
 * The original carried Forge FluidStack/IFluidHandlerItem plumbing; on Fabric the fluid
 * binding lives in ItemIC2FluidContainer, so this enum keeps only the identity/meta data
 * (name, meta id, stack size, crop interaction flag) that recipes and registration need.
 */
public enum CellType implements IIdProvider {
   empty(0, false),
   water(1, true),
   lava(2, true),
   air(3, true),
   electrolyzed_water(4, true),
   biofuel(5, true),
   coalfuel(6, true),
   bio(7, true),
   hydrated_coal(8, true),
   weed_ex(9, true),
   hydration(10, true);

   private final int id;
   private final boolean fluidContainer;

   CellType(int id, boolean fluidContainer) {
      this.id = id;
      this.fluidContainer = fluidContainer;
   }

   @Override
   public String getName() {
      return this.name();
   }

   @Override
   public int getId() {
      return this.id;
   }

   public int getStackSize() {
      return this != weed_ex && this != hydration ? 64 : 1;
   }

   public boolean isFluidContainer() {
      return this.fluidContainer || this == empty;
   }

   public boolean hasCropAction() {
      return this == water || this == weed_ex || this == hydration;
   }
}
