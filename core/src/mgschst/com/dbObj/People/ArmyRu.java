package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.AdditionalEquipment;
import mgschst.com.dbObj.Equipment.ProtectionEquip;
import mgschst.com.dbObj.Equipment.Weapon;

import java.util.Random;

public class ArmyRu extends Person{
    Random random = new Random();
    public ArmyRu(int id){
        this.id = id;
        randomizeArmor();
        if (id != 37) {
            weapon = new Weapon(8, 2, "АКМ", new int[]{13});
            if (id == 36){
                switch (random.nextInt(2)){
                    case 0 -> helmet = new ProtectionEquip(29, 5, "Шлем спецназа", new int[]{25});
                    case 1 -> helmet = new ProtectionEquip(57, 5, "Шлем Жерло-5", new int[]{});
                }
                firstAddEquip = new AdditionalEquipment(40, "Штурмовой щит");
                secondAddEquip = randomizeAddEquip();
            } else {
                switch (random.nextInt(2)){
                    case 0 -> helmet = new ProtectionEquip(27,2,"Общевойсковой шлем", new int[]{});
                    case 1 -> helmet = new ProtectionEquip(28, 3, "Штурмовой шлем", new int[]{});
                }
                firstAddEquip = randomizeAddEquip();
            }
        }else {
            switch (random.nextInt(2)){
                case 0 -> weapon = new Weapon(30, 4, "Болтовая винтовка Мосина", new int[]{12,7});
                case 1 -> weapon = new Weapon(31, 4, "Болтовая винтовка СВ-98", new int[]{7});
            }
        }
    }

    public void randomizeArmor(){
        switch (random.nextInt(4)){
            case 0 -> armor = new ProtectionEquip(11, 4, "Бронежилет АТРАВМ", new int[]{});
            case 1 -> armor = new ProtectionEquip(12, 4, "Плитник \"Защитник\"", new int[]{8});
            case 2 -> armor = new ProtectionEquip(13, 5, "Бронежилет \"АТАКА\"", new int[]{});
            case 3 -> armor = new ProtectionEquip(14, 6, "Бронежилет \"Крепость\"", new int[]{10});
        }
    }
    public AdditionalEquipment randomizeAddEquip(){
        AdditionalEquipment equipment = new AdditionalEquipment(0, "");
        switch (random.nextInt(4)){
            case 0 -> equipment = new AdditionalEquipment(38, "Старый глушитель");
            case 1 -> equipment = new AdditionalEquipment(39, "Глушитель");
            case 2 -> equipment = new AdditionalEquipment(41, "Осколочная граната");
            case 3 -> equipment = new AdditionalEquipment(42, "Светошумовая граната");
        }
        return equipment;
    }
}
