package ic2.api.reactor;

import ic2.api.info.ILocatable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;

public interface IReactor extends ILocatable {
    BlockEntity getCoreTe();
    int getHeat();
    void setHeat(int heat);
    int addHeat(int amount);
    int getMaxHeat();
    void setMaxHeat(int maxHeat);
    void addEmitHeat(int heat);
    float getHeatEffectModifier();
    void setHeatEffectModifier(float modifier);
    float getReactorEnergyOutput();
    double getReactorEUEnergyOutput();
    float addOutput(float energy);
    ItemStack getItemAt(int x, int y);
    void setItemAt(int x, int y, ItemStack stack);
    void explode();
    int getTickRate();
    boolean produceEnergy();
    boolean isFluidCooled();
}
