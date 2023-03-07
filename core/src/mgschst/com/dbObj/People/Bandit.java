package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.Armor;

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
    }

    public Bandit(int id){
        this.id = id;
        randomizeArmor();
    }

    public void randomizeArmor(){
        switch (random.nextInt(2)){
            case 0 -> armor = new Armor(9, 2, "Полицейский бронежилет", null);
            case 1 -> armor = new Armor(10, 3, "Бронежилет \"Пресса\"", null);
        }
    }
}
