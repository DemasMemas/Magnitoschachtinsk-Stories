package mgschst.com;

public class Card {
    int card_id;
    String name;
    String image_path;
    String type;
    String description;
    int deck_limit;
    String statuses;
    String cost_type;
    int health_status;
    int effect_number;
    int price;
    int rareness;
    int attack;
    int defence;
    int stealth;
    int current_amount;

    public Card(int card_id, String name, String image_path, String type, String description,
                int deck_limit, String statuses, String cost_type, int health_status,
                int effect_number, int price, int rareness, int attack, int defence, int stealth) {
        this.card_id = card_id;
        this.name = name;
        this.image_path = image_path;
        this.type = type;
        this.description = description;
        this.deck_limit = deck_limit;
        this.statuses = statuses;
        this.cost_type = cost_type;
        this.health_status = health_status;
        this.effect_number = effect_number;
        this.price = price;
        this.rareness = rareness;
        this.attack = attack;
        this.defence = defence;
        this.stealth = stealth;
    }

    public Card(int card_id, String name, String image_path, String type, String description,
                int deck_limit, String statuses, String cost_type, int health_status,
                int effect_number, int price, int rareness, int attack, int defence, int stealth,
                int current_amount) {
        this.card_id = card_id;
        this.name = name;
        this.image_path = image_path;
        this.type = type;
        this.description = description;
        this.deck_limit = deck_limit;
        this.statuses = statuses;
        this.cost_type = cost_type;
        this.health_status = health_status;
        this.effect_number = effect_number;
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
}
