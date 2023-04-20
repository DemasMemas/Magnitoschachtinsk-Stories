package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.AdditionalEquipment;
import mgschst.com.dbObj.Equipment.ProtectionEquip;
import mgschst.com.dbObj.Equipment.Weapon;

import java.util.Random;

public class MercRu extends Person{
    Random random = new Random();
    public MercRu(){
        id = 17;
        randomizeArmor();
        randomizeWeapon();
        randomizeHelmet();
        randomizeAddEquip();
    }

    public void randomizeArmor(){
        switch (random.nextInt(2)){
            case 0 -> armor = new ProtectionEquip(11, 4, "Бронежилет АТРАВМ", new int[]{});
            case 1 -> armor = new ProtectionEquip(12, 4, "Плитник \"Защитник\"", new int[]{8});
        }
    }
    public void randomizeWeapon(){
        switch (random.nextInt(2)){
            case 0 -> weapon = new Weapon(30, 4, "Болтовая винтовка Мосина", new int[]{12,7});
            case 1 -> weapon = new Weapon(8, 2, "АКМ", new int[]{13});
        }
    }

    public void randomizeHelmet(){
        switch (random.nextInt(2)){
            case 0 -> helmet = new ProtectionEquip(27,2,"Общевойсковой шлем", new int[]{});
            case 1 -> helmet = new ProtectionEquip(28, 3, "Штурмовой шлем", new int[]{});
        }
    }

    public void randomizeAddEquip(){
        switch (random.nextInt(5)){
            case 0 -> firstAddEquip = new AdditionalEquipment(38, "Старый глушитель");
            case 1 -> firstAddEquip = new AdditionalEquipment(39, "Глушитель");
            case 2 -> firstAddEquip = new AdditionalEquipment(41, "Осколочная граната");
            case 3 -> firstAddEquip = new AdditionalEquipment(42, "Светошумовая граната");
            case 4 -> firstAddEquip = new AdditionalEquipment(40, "Штурмовой щит");
        }
    }
}
