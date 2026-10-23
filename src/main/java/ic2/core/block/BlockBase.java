package ic2.core.block;

import net.minecraft.block.Block;

/**
 * Fabric 1.21.1 port of IC2 BlockBase.
 * Original 1.12: abstract class extends Block implements IBlockModelProvider
 *   with BlockName/Material/ItemBlock registration and Forge model hooks.
 * Fabric stub: extends Block with Settings constructor and hardness/resistance stubs.
 * Hardness/resistance теперь задаются через Settings.strength(), методы оставлены для совместимости.
 */
public class BlockBase extends Block {

    public BlockBase(Settings settings) {
        super(settings);
    }

    /**
     * Stub for legacy IC2/Forge API — hardness теперь в Settings.strength().
     * Сохранён для совместимости портируемого кода.
     */
    public float getHardness() {
        return 1.0F;
    }

    /**
     * Stub for legacy IC2/Forge API — explosion resistance теперь в Settings.strength().
     * Сохранён для совместимости портируемого кода.
     */
    public float getExplosionResistance() {
        return 1.0F;
    }
}
