package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.Armor;
import mgschst.com.dbObj.Equipment.Helmet;
import mgschst.com.dbObj.Equipment.Weapon;

import java.util.Random;

public class MercRu extends Person{
    Random random = new Random();
    public MercRu(){
        id = 17;
        randomizeArmor();
        randomizeWeapon();
        randomizeHelmet();
    }

    public void randomizeArmor(){
        switch (random.nextInt(2)){
            case 0 -> armor = new Armor(11, 4, "Бронежилет АТРАВМ", new int[]{});
            case 1 -> armor = new Armor(12, 4, "Плитник \"Защитник\"", new int[]{8});
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
            case 0 -> helmet = new Helmet(27,2,"Общевойсковой шлем", new int[]{});
            case 1 -> helmet = new Helmet(28, 3, "Штурмовой шлем", new int[]{});
        }
    }
}
