package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.Armor;
import mgschst.com.dbObj.Equipment.Helmet;
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
                    case 0 -> helmet = new Helmet(29, 5, "Шлем спецназа", new int[]{25});
                    case 1 -> helmet = new Helmet(57, 5, "Шлем Жерло-5", new int[]{});
                }
                // выдать два дополнительных снаряжения
                // добавить эффект 30
            } else {
                switch (random.nextInt(2)){
                    case 0 -> helmet = new Helmet(27,2,"Общевойсковой шлем", new int[]{});
                    case 1 -> helmet = new Helmet(28, 3, "Штурмовой шлем", new int[]{});
                }
                 // выдать одно дополнительное снаряжение
                if (id == 35) {
                    // добавить эффект 29
                }
            }
        }else {
            // выдать снайперское оружие
            switch (random.nextInt(2)){
                case 0 -> weapon = new Weapon(30, 4, "Болтовая винтовка Мосина", new int[]{12,7});
                case 1 -> weapon = new Weapon(31, 4, "Болтовая винтовка СВ-98", new int[]{7});
            }
            // добавить эффект 31
        }
    }

    public void randomizeArmor(){
        switch (random.nextInt(4)){
            case 0 -> armor = new Armor(11, 4, "Бронежилет АТРАВМ", new int[]{});
            case 1 -> armor = new Armor(12, 4, "Плитник \"Защитник\"", new int[]{8});
            case 2 -> armor = new Armor(13, 5, "Бронежилет \"АТАКА\"", new int[]{});
            case 3 -> armor = new Armor(14, 6, "Бронежилет \"Крепость\"", new int[]{10});
        }
    }
}
