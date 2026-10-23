package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.init.IC2ItemVariants;
import ic2.core.item.armor.jetpack.JetpackAttachmentRecipe;
import ic2.core.item.armor.jetpack.JetpackHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/** The electric jetpack crafted onto a chestplate. Run with {@code ./gradlew runGametest}. */
public class JetpackGameTest implements FabricGameTest {

    /** Jetpack + iron chestplate + attachment plate give the chestplate with the jetpack and its 10k EU. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void jetpackAttachesToAChestplate(TestContext ctx) {
        ItemStack jetpack = jetpack(10000.0);
        CraftingRecipeInput input = grid(jetpack, new ItemStack(Items.IRON_CHESTPLATE), plate());

        Optional<RecipeEntry<CraftingRecipe>> recipe = ctx.getWorld().getRecipeManager()
                .getFirstMatch(RecipeType.CRAFTING, input, ctx.getWorld());
        ctx.assertTrue(recipe.isPresent() && recipe.get().value() instanceof JetpackAttachmentRecipe, "recipe " + recipe);

        ItemStack result = recipe.get().value().craft(input, ctx.getWorld().getRegistryManager());
        ctx.assertTrue(result.isOf(Items.IRON_CHESTPLATE), "result " + result);
        ctx.assertTrue(JetpackHandler.hasJetpackAttached(result), "no jetpack attached");
        ctx.assertTrue(ElectricItem.manager.getCharge(result) == 10000.0, "charge " + ElectricItem.manager.getCharge(result));
        ctx.assertTrue(ElectricItem.manager.getMaxCharge(result) == 30000.0, "max charge " + ElectricItem.manager.getMaxCharge(result));

        // a second jetpack, a blacklisted chestplate, a missing plate or two chestplates do not craft
        ctx.assertTrue(!matches(ctx, grid(jetpack(0), result, plate())), "attached twice");
        ctx.assertTrue(!matches(ctx, grid(jetpack(0), new ItemStack(item("quantum_chestplate")), plate())), "quantum chestplate");
        ctx.assertTrue(!matches(ctx, grid(jetpack(0), new ItemStack(Items.IRON_CHESTPLATE))), "no plate");
        ctx.assertTrue(!matches(ctx, grid(jetpack(0), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.DIAMOND_CHESTPLATE), plate())),
                "two chestplates");
        ctx.complete();
    }

    /** Hovering in the air costs 1 EU plus the electric jetpack's 6 a tick, taken from the chestplate. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void attachedJetpackHovers(TestContext ctx) {
        ServerPlayerEntity player = (ServerPlayerEntity) ctx.createMockCreativeServerPlayerInWorld();
        Vec3d air = ctx.getAbsolute(new Vec3d(1.5, 5.0, 1.5));
        player.setPosition(air);
        player.setOnGround(false);

        ItemStack chest = attached(10000.0);
        NbtCompound data = chest.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        data.putBoolean("hoverMode", true);
        chest.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        player.equipStack(EquipmentSlot.CHEST, chest);

        player.setVelocity(0.0, -0.5, 0.0);
        JetpackHandler.tick(player);
        ItemStack worn = player.getEquippedStack(EquipmentSlot.CHEST);
        ctx.assertTrue(ElectricItem.manager.getCharge(worn) == 10000.0 - 7.0, "charge " + ElectricItem.manager.getCharge(worn));
        ctx.assertTrue(player.getVelocity().y > -0.5, "no lift " + player.getVelocity());
        ctx.assertTrue(player.fallDistance == 0.0f, "fall distance " + player.fallDistance);
        ctx.complete();
    }

    /** A chestplate breaking while worn leaves the jetpack in the chest slot, with its charge. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void brokenChestplateGivesTheJetpackBack(TestContext ctx) {
        ServerPlayerEntity player = (ServerPlayerEntity) ctx.createMockCreativeServerPlayerInWorld();
        ItemStack chest = attached(2500.0);
        player.equipStack(EquipmentSlot.CHEST, chest);
        player.setOnGround(true);
        JetpackHandler.tick(player);

        chest.decrement(1); // what breaking does to the worn stack
        JetpackHandler.tick(player);
        ItemStack worn = player.getEquippedStack(EquipmentSlot.CHEST);
        ctx.assertTrue(worn.isOf(item("jetpack_electric")), "chest slot " + worn);
        ctx.assertTrue(ElectricItem.manager.getCharge(worn) == 2500.0, "charge " + ElectricItem.manager.getCharge(worn));

        // taking a chestplate off gives nothing back
        ItemStack other = attached(100.0);
        player.equipStack(EquipmentSlot.CHEST, other);
        JetpackHandler.tick(player);
        player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
        JetpackHandler.tick(player);
        ctx.assertTrue(player.getEquippedStack(EquipmentSlot.CHEST).isEmpty(), "jetpack from a removed chestplate");
        ctx.complete();
    }

    private static ItemStack attached(double charge) {
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        JetpackHandler.setJetpackAttached(chest, true);
        ElectricItem.manager.charge(chest, charge, Integer.MAX_VALUE, true, false);
        return chest;
    }

    private static boolean matches(TestContext ctx, CraftingRecipeInput input) {
        return ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, ctx.getWorld())
                .filter(entry -> entry.value() instanceof JetpackAttachmentRecipe).isPresent();
    }

    private static CraftingRecipeInput grid(ItemStack... stacks) {
        List<ItemStack> list = new ArrayList<>(List.of(stacks));
        while (list.size() < 9) {
            list.add(ItemStack.EMPTY);
        }
        return CraftingRecipeInput.create(3, 3, list);
    }

    private static ItemStack jetpack(double charge) {
        ItemStack stack = new ItemStack(item("jetpack_electric"));
        ElectricItem.manager.charge(stack, charge, Integer.MAX_VALUE, true, false);
        return stack;
    }

    private static ItemStack plate() {
        return new ItemStack(IC2ItemVariants.get("crafting", "jetpack_attachment_plate"));
    }

    private static Item item(String name) {
        return Registries.ITEM.get(Identifier.of("ic2", name));
    }
}
