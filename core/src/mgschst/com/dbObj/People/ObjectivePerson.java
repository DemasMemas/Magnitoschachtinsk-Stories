package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.ProtectionEquip;
import mgschst.com.dbObj.Equipment.Weapon;

public class ObjectivePerson extends Person {
    public ObjectivePerson(int newID) {
        switch (newID){
            case 55 -> {
                id = 55;
                armor = new ProtectionEquip(11, 4, "Бронежилет АТРАВМ", new int[]{});
                weapon = new Weapon(8, 2, "АКМ", new int[]{13});
                helmet = new ProtectionEquip(27, 2, "Общевойсковой шлем", new int[]{});
            }
            case 56 -> id = 56;
        }
    }
}
