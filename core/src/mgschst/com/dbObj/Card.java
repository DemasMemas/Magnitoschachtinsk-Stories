package mgschst.com.dbObj;

import mgschst.com.dbObj.Building.Building;
import mgschst.com.dbObj.People.Person;

public class Card {
    public int card_id;
    public String name;
    public String image_path;
    public String type;
    public String description;
    public int deck_limit;
    public String cost_type;
    public int health_status;
    public String effects;
    public int price;
    public int rareness;
    public int attack;
    public int defence;
    public int stealth;
    public int current_amount;
    public Person person;
    public Building building;

    public Card(int card_id, String name, String image_path, String type, String description,
                int deck_limit, String cost_type, int health_status,
                String effects, int price, int rareness, int attack, int defence, int stealth) {
        this.card_id = card_id;
        this.name = name;
        this.image_path = image_path;
        this.type = type;
        this.description = description;
        this.deck_limit = deck_limit;
        this.cost_type = cost_type;
        this.health_status = health_status;
        this.effects = effects;
        this.price = price;
        this.rareness = rareness;
        this.attack = attack;
        this.defence = defence;
        this.stealth = stealth;
    }

    public Card(int card_id, String name, String image_path, String type, String description,
                int deck_limit, String cost_type, int health_status,
                String effects, int price, int rareness, int attack, int defence, int stealth,
                int current_amount) {
        this.card_id = card_id;
        this.name = name;
        this.image_path = image_path;
        this.type = type;
        this.description = description;
        this.deck_limit = deck_limit;
        this.cost_type = cost_type;
        this.health_status = health_status;
        this.effects = effects;
        this.price = price;
        this.rareness = rareness;
        this.attack = attack;
        this.defence = defence;
        this.stealth = stealth;
        this.current_amount = current_amount;
    }

    @Override
    public String toString(){
        return card_id + " " + name + " " + current_amount + "/" + deck_limit;
    }

    public String getPersonCard(){
        return card_id + " " + person.getArmorString() + " " + person.getWeaponString() + " " + person.getHelmetString();
        //+ " " + person.getWeaponString() + " " + person.firstEquipString() + " " + person.secondEquipString() + " ";
    }
    public String getBuildingCard(){
        return String.valueOf(card_id);
    }
}
