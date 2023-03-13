package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.Armor;
import mgschst.com.dbObj.Equipment.Weapon;

import java.util.Random;

public class ArmyRu extends Person{
    Random random = new Random();
    public ArmyRu(int id){
        this.id = id;
        randomizeArmor();
        if (id != 37) {
            // рандом оружие
            weapon = new Weapon(8, 2, "АКМ", new int[]{13});
            if (id == 36){
                // выдать стандартный шлем
                // выдать два дополнительных снаряжения
                // добавить эффект 30
            } else {
                // выдать штурмовой шлем
                 // выдать одно дополнительное снаряжение
                 if (id == 35) {
                    // добавить эффект 29
                 }
            }
        }else {
            // выдать снайперское оружие
            switch (random.nextInt(2)){
                case 0 -> weapon = new Weapon(30, 4, "Болтовая винтовка Мосина", new int[]{12});
                case 1 -> weapon = new Weapon(31, 4, "Болтовая винтовка СВ-98", null);
            }
            // не выдавать шлем
            // добавить эффект 31
        }
    }

    public void randomizeArmor(){
        switch (random.nextInt(4)){
            case 0 -> armor = new Armor(11, 4, "Бронежилет АТРАВМ", null);
            case 1 -> armor = new Armor(12, 4, "Плитник \"Защитник\"", new int[]{8});
            case 2 -> armor = new Armor(13, 5, "Бронежилет \"АТАКА\"", null);
            case 3 -> armor = new Armor(14, 6, "Бронежилет \"Крепость\"", new int[]{9});
        }
    }
}
