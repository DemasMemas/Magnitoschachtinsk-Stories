package mgschst.com.dbObj.Equipment;

public class Weapon {
    private final int id;
    private final int attack;
    private final String name;
    private int[] effectList;

    public Weapon(int id, int attack, String name, int[] effectList) {
        this.id = id;
        this.attack = attack;
        this.name = name;
        this.effectList = effectList;
    }

    public int getAttack() {
        return attack;
    }

    public String getName() {
        return name;
    }

    public int[] getEffectList() {
        return effectList;
    }

    public void setEffectList(int[] effectList) {
        this.effectList = effectList;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder effects = new StringBuilder();
        for (int i : getEffectList())
            effects.append(i).append(":");
        if (effects.length() > 0) effects.deleteCharAt(effects.length() - 1);
        return getId() + ";" + getAttack() + ";" + getName() + ";" + effects;
    }
}
