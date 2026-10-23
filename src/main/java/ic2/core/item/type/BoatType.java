package ic2.core.item.type;

import ic2.core.block.state.IIdProvider;

/** Fabric port of ic2.core.item.ItemIC2Boat.BoatType. */
public enum BoatType implements IIdProvider {
   broken_rubber,
   rubber,
   carbon,
   electric;

   @Override
   public String getName() {
      return this.name();
   }

   @Override
   public int getId() {
      return this.ordinal();
   }
}
