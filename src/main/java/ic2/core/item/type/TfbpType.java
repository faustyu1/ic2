package ic2.core.item.type;

import ic2.core.block.state.IIdProvider;

/**
 * Fabric port of ic2.core.item.tfbp.Tfbp.TfbpType: the energy a change costs and the range of each blueprint.
 * What each blueprint does lives in {@link ic2.core.item.tfbp.ItemTfbp}.
 */
public enum TfbpType implements IIdProvider {
   blank(0.0, 0),
   chilling(2000.0, 50),
   cultivation(4000.0, 40),
   desertification(2500.0, 40),
   flatification(4000.0, 40),
   irrigation(3000.0, 60),
   mushroom(8000.0, 25);

   public final double consume;
   public final int range;

   TfbpType(double consume, int range) {
      this.consume = consume;
      this.range = range;
   }

   @Override
   public String getName() {
      return this.name();
   }

   @Override
   public int getId() {
      return this.ordinal();
   }
}
