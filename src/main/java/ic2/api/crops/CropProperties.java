package ic2.api.crops;

public class CropProperties {
    private final int tier;
    private final int chemistry;
    private final int consumable;
    private final int defensive;
    private final int colorful;
    private final int weed;

    public CropProperties(int tier, int chemistry, int consumable, int defensive, int colorful, int weed) {
        this.tier = tier;
        this.chemistry = chemistry;
        this.consumable = consumable;
        this.defensive = defensive;
        this.colorful = colorful;
        this.weed = weed;
    }

    public int getTier() { return tier; }
    public int getChemistry() { return chemistry; }
    public int getConsumable() { return consumable; }
    public int getDefensive() { return defensive; }
    public int getColorful() { return colorful; }
    public int getWeed() { return weed; }
    public int[] getAllProperties() { return new int[]{getChemistry(), getConsumable(), getDefensive(), getColorful(), getWeed()}; }
}
