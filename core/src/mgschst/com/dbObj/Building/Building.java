package mgschst.com.dbObj.Building;


import mgschst.com.dbObj.Equipment.Weapon;

public class Building {
    public int getId() {
        return id;
    }
    int id;
    int attackBonus;
    int defenceBonus;
    Weapon builtInWeapon;
    boolean health = true;
    public Building(int id){
        this.id = id;
        attackBonus = 0;
        defenceBonus = 0;
        switch (id) {
            case 5 -> {
                // верстак, добавить возможность чинить
            }
            case 47 -> {
                defenceBonus = 1;
                // налетная, добавить выдачу снаряжения
            }
            case 48 -> // склад
                    defenceBonus = 2;
            case 53-> {
                // разведцентр, добавить выдачу целей
            }
            case 50-> {
                // медблок, расширить на 1
            }
            case 54 -> {
                // ценный груз, цель
            }
        }
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    public void setAttackBonus(int attackBonus) {
        this.attackBonus = attackBonus;
    }

    public int getDefenceBonus() {
        return defenceBonus;
    }

    public void setDefenceBonus(int defenceBonus) {
        this.defenceBonus = defenceBonus;
    }

    public boolean isHealth() {
        return health;
    }

    public void setHealth(boolean health) {
        this.health = health;
    }
}
