package mgschst.com.dbObj.Equipment;

public class ProtectionEquip {
    private final int id;
    private int defence;
    private final int maxDefence;
    private final String name;
    private int[] effectList;

    public ProtectionEquip(int id, int defence, String name, int[] effect) {
        this.id = id;
        this.maxDefence = defence;
        this.defence = defence;
        this.name = name;
        this.effectList = effect;
    }

    public int getDefence() {
        return defence;
    }

    public void setDefence(int newDefence) {
        this.defence = newDefence;
    }

    public String getName() {
        return name;
    }

    public int[] getEffect() {
        return effectList;
    }

    public void setEffect(int[] effect) {
        this.effectList = effect;
    }

    public int getId() {
        return id;
    }

    public int getMaxDefence() {
        return maxDefence;
    }

    @Override
    public String toString() {
        StringBuilder effects = new StringBuilder();
        for (int i : getEffect())
            effects.append(i).append(":");
        if (effects.length() > 0) effects.deleteCharAt(effects.length() - 1);
        return getId() + ";" + getDefence() + ";" + getName() + ";" + effects;
    }
}