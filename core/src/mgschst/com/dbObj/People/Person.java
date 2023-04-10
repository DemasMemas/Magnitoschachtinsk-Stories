package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.*;

import java.util.ArrayList;

public class Person {
    public int getId() {
        return id;
    }
    int id;
    Armor armor = new Armor(0, 0, "Нет брони", new int[]{});
    Helmet helmet = new Helmet(0, 0, "Нет шлема", new int[]{});
    Weapon weapon = new Weapon(0, 0, "Нет оружия", new int[]{});
    AdditionalEquipment firstAddEquip = new AdditionalEquipment(0, "Нет снаряжения");
    AdditionalEquipment secondAddEquip = new AdditionalEquipment(0, "Нет снаряжения");
    ArrayList<Status> statusesList = new ArrayList<>();
    boolean fought = false;
    boolean health = true;
    boolean defender = false;
    public String getArmorString(){
        return armor.toString();
    }
    public String getHelmetString(){
        return helmet.toString();
    }
    public String getWeaponString(){
        return weapon.toString();
    }
    public String firstEquipString(){
        return firstAddEquip.toString();
    }
    public String secondEquipString(){
        return secondAddEquip.toString();
    }
    public String getFoughtStatus(){
        return fought ? "1":"0";
    }

    public void setArmor(Armor armor) {
        this.armor = armor;
    }

    public void setHelmet(Helmet helmet) {
        this.helmet = helmet;
    }

    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    public void setFirstAddEquip(AdditionalEquipment firstAddEquip) {
        this.firstAddEquip = firstAddEquip;
    }

    public void setSecondAddEquip(AdditionalEquipment secondAddEquip) {
        this.secondAddEquip = secondAddEquip;
    }

    public void setFought(boolean fought) {
        this.fought = fought;
    }
    public boolean isHealth() {
        return health;
    }
    public void setHealth(boolean health) {
        this.health = health;
    }
    public Armor getArmor() {
        return armor;
    }
    public Helmet getHelmet() {
        return helmet;
    }
    public Weapon getWeapon() {
        return weapon;
    }
    public AdditionalEquipment getFirstAddEquip() {
        return firstAddEquip;
    }
    public AdditionalEquipment getSecondAddEquip() {
        return secondAddEquip;
    }
    public boolean isDefender() {
        return defender;
    }
    public void setDefender(boolean defender) {
        this.defender = defender;
    }
    public ArrayList<Status> getStatusesList() {
        return statusesList;
    }
}
