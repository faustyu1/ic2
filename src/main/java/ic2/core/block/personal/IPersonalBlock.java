package ic2.core.block.personal;

import java.util.UUID;

/**
 * A block owned by a player: the personal safe, the trade-o-mat and the energy-o-mat.
 *
 * <p>Two personal blocks of the same owner trust each other - a trade-o-mat reaches into its
 * owner's safe next to it for stock, which nobody else's machine can - as the PersonalAdjacentInv
 * of 1.12.2 allowed.
 */
public interface IPersonalBlock {
    /** The owner, or null while nobody has claimed the block. */
    UUID getOwnerId();
}
