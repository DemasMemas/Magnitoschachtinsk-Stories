package mgschst.com.dbObj.Building;

import mgschst.com.dbObj.Card;

import java.util.ArrayList;

public class Building {
    public int getId() {
        return id;
    }
    int id;
    int defenceBonus = 0;
    boolean health = true;
    ArrayList<Card> defenderList = new ArrayList<>();
    boolean alreadyUsed = false;
    boolean minedUp = false;
    public Building(int id){
        this.id = id;
        switch (id) {
            case 47 ->
                    defenceBonus = 1;
            case 48 -> // склад
                    defenceBonus = 2;
        }
    }
    public int getDefenceBonus() {
        return defenceBonus;
    }
    public void setDefenceBonus(int defenceBonus) {
        this.defenceBonus = defenceBonus;
    }
    public boolean isHealth() {
        return health;
    }
    public void setHealth(boolean health) {
        this.health = health;
    }
    public ArrayList<Card> getDefenderList() {
        return defenderList;
    }
    public void setDefenderList(ArrayList<Card> defenderList) {
        this.defenderList = defenderList;
    }
    public boolean isAlreadyUsed() {
        return alreadyUsed;
    }
    public void setAlreadyUsed(boolean alreadyUsed) {
        this.alreadyUsed = alreadyUsed;
    }
    public boolean isMinedUp() {
        return minedUp;
    }
    public void setMinedUp(boolean minedUp) {
        this.minedUp = minedUp;
    }
}
