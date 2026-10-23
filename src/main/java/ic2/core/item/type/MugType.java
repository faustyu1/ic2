package ic2.core.item.type;

import ic2.core.block.state.IIdProvider;

/** Fabric port of ic2.core.item.ItemMug.MugType. */
public enum MugType implements IIdProvider {
   empty,
   cold_coffee,
   dark_coffee,
   coffee;

   @Override
   public String getName() {
      return this.name();
   }

   @Override
   public int getId() {
      return this.ordinal();
   }
}
