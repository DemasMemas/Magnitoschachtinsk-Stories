package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.Armor;
import mgschst.com.dbObj.Equipment.Weapon;

public class ObjectivePerson extends Person {
    public ObjectivePerson(int newID) {
        switch (newID){
            case 55 -> {
                id = 55;
                armor = new Armor(11, 4, "Бронежилет АТРАВМ", new int[]{});
                weapon = new Weapon(8, 2, "АКМ", new int[]{13});
            }
            case 56 -> {
                id = 56;
                armor = new Armor(0, 0, "Нет брони", new int[]{});
                weapon = new Weapon(0, 0, "Нет оружия", new int[]{});
            }
        }
    }
}
