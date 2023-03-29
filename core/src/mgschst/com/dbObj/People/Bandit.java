package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.Armor;
import mgschst.com.dbObj.Equipment.Helmet;
import mgschst.com.dbObj.Equipment.Weapon;

import java.util.Random;

public class Bandit extends Person{
    Random random = new Random();
    public Bandit(){
        switch (random.nextInt(3)){
            case 0 -> id = 4;
            case 1 -> id = 15;
            case 2 -> id = 16;
        }
        randomizeArmor();
        randomizeWeapon();
    }

    public Bandit(int id){
        this.id = id;
        randomizeArmor();
        randomizeWeapon();
        randomizeHelmet();
    }

    public void randomizeArmor(){
        switch (random.nextInt(2)){
            case 0 -> armor = new Armor(9, 2, "Полицейский бронежилет", new int[]{});
            case 1 -> armor = new Armor(10, 3, "Бронежилет \"Пресса\"", new int[]{});
        }
    }

    public void randomizeWeapon(){
        switch (random.nextInt(3)){
            case 0 -> weapon = new Weapon(1, 1, "ПМ", new int[]{});
            case 1 -> weapon = new Weapon(7, 2, "Обрез двустволки", new int[]{11});
            case 2 -> weapon = new Weapon(8, 2, "АКМ", new int[]{12,13});
        }
    }

    public void randomizeHelmet(){
        switch (random.nextInt(8)){
            case 7 -> helmet = new Helmet(27,2,"Общевойсковой шлем", new int[]{});
            case 5,6 -> helmet = new Helmet(26, 1, "Полицейский шлем", new int[]{});
        }
    }
}
