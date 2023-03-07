package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.*;

public class Person {
    public int getId() {
        return id;
    }

    int id;
    Armor armor;
    Helmet helmet;
    Weapon weapon;
    AdditionalEquipment firstAddEquip;
    AdditionalEquipment secondAddEquip;
}
