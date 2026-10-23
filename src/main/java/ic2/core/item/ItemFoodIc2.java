package ic2.core.item;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class ItemFoodIc2 extends ItemIC2 {
    public ItemFoodIc2(String name, int nutrition, float saturation){
        super(name, new Settings().food(new FoodComponent.Builder().nutrition(nutrition).saturationModifier(saturation).statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200), 1.0f).alwaysEdible().build()));
    }

    public ItemFoodIc2(String name, int nutrition, float saturation, StatusEffectInstance effect, float probability){
        super(name, new Settings().food(createFood(nutrition, saturation, effect, probability, true)));
    }

    private static FoodComponent createFood(int nutrition, float saturation, StatusEffectInstance effect, float probability, boolean alwaysEdible){
        FoodComponent.Builder builder = new FoodComponent.Builder().nutrition(nutrition).saturationModifier(saturation);
        if (effect != null) builder.statusEffect(effect, probability);
        if (alwaysEdible) builder.alwaysEdible();
        return builder.build();
    }
}
