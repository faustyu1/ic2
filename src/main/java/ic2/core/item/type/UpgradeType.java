package ic2.core.item.type;

import ic2.core.block.state.IIdProvider;

/**
 * Fabric port of ic2.core.item.upgrade.ItemUpgradeModule.UpgradeType.
 * Lifted to its own file because 1.13 flattening turns each metadata variant into a
 * separately registered item, so the variant list is needed at registration time.
 */
public enum UpgradeType implements IIdProvider {
   overclocker(false),
   transformer(false),
   energy_storage(false),
   redstone_inverter(false),
   ejector(true),
   advanced_ejector(true),
   pulling(true),
   advanced_pulling(true),
   fluid_ejector(true),
   fluid_pulling(true),
   remote_interface(false);

   public final boolean directional;

   UpgradeType(boolean directional) {
      this.directional = directional;
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
