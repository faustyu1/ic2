package ic2.integration.rei;

import ic2.core.gui.GuiIC2;
import ic2.core.init.IC2ItemVariants;
import ic2.core.util.FluidContainers;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * IC2 fluids as REI entries.
 *
 * <p>Water and lava are Minecraft fluids and keep REI's fluid entry, so looking them up works as
 * for any fluid. The IC2 fluids are no Minecraft fluids in this port; they stand on the IC2 cell
 * filled with them, or the empty cell when there is no such cell (hydrogen, oxygen, milk...).
 * Either way the entry is drawn as the fluid itself, as the machine windows draw their tanks, with
 * its name and amount as the tooltip.
 */
final class ReiFluids {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private ReiFluids() {
    }

    static EntryIngredient of(String name, int mb) {
        return of(FluidRegistry.getFluid(name), mb);
    }

    static EntryIngredient of(Fluid fluid, int mb) {
        if (fluid == null) {
            return EntryIngredient.empty();
        }

        EntryStack<?> base;
        net.minecraft.fluid.Fluid mc = fluid.getMinecraftFluid();
        if (mc != null && mc != Fluids.EMPTY) {
            base = EntryStacks.of(mc, mb * DROPLETS_PER_MB);
        } else {
            Item emptyCell = IC2ItemVariants.get("fluid_cell", null);
            if (emptyCell == null) {
                return EntryIngredient.empty();
            }
            ItemStack cell = FluidContainers.filled(new ItemStack(emptyCell), fluid);
            base = EntryStacks.of(cell.isEmpty() ? new ItemStack(emptyCell) : cell);
        }
        return EntryIngredient.of(base.copy().withRenderer(new FluidRenderer(fluid, mb)));
    }

    /** The translated name, from the keys the 1.12.2 lang files have for the IC2 fluids. */
    static Text name(Fluid fluid) {
        String name = fluid.getName();
        return name.startsWith("ic2") ? Text.translatable(name.substring("ic2".length()))
                : Text.translatable("block.minecraft." + name);
    }

    private record FluidRenderer(Fluid fluid, int mb) implements EntryRenderer<Object> {
        @Override
        public void render(EntryStack<Object> entry, DrawContext graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
            GuiIC2.drawFluid(graphics, bounds.x, bounds.y, bounds.width, bounds.height, fluid, 1.0);
        }

        @Override
        public Tooltip getTooltip(EntryStack<Object> entry, TooltipContext context) {
            return Tooltip.create(context.getPoint(), name(fluid), Text.literal(mb + " mB").formatted(Formatting.GRAY));
        }
    }
}
