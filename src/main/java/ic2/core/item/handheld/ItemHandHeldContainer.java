package ic2.core.item.handheld;

import ic2.api.item.IBoxable;
import ic2.core.item.ItemIC2;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ItemToolbox} and {@code ItemContainmentbox}: a held item that opens the
 * inventory kept inside its own stack.
 */
public class ItemHandHeldContainer extends ItemIC2 implements IBoxable {
    private final HandHeldKind kind;

    public ItemHandHeldContainer(HandHeldKind kind) {
        super(kind.getId(), new Settings().maxCount(1));
        this.kind = kind;
    }

    public HandHeldKind getKind() {
        return kind;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient) {
            player.openHandledScreen(new HandHeldInventory(player, hand, kind));
        }

        return TypedActionResult.success(stack, world.isClient);
    }

    /** A box never goes inside another box, which is what 1.12.2 did too. */
    @Override
    public boolean canBeStoredInToolbox(ItemStack stack) {
        return false;
    }
}
