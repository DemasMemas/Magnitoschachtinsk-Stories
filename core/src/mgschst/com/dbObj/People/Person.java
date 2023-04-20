package mgschst.com.dbObj.People;

import mgschst.com.dbObj.Equipment.*;

import java.util.ArrayList;

public class Person {
    public int getId() {
        return id;
    }
    int id;
    ProtectionEquip armor = new ProtectionEquip(0, 0, "Нет брони", new int[]{});
    ProtectionEquip helmet = new ProtectionEquip(0, 0, "Нет шлема", new int[]{});
    Weapon weapon = new Weapon(0, 0, "Нет оружия", new int[]{});
    AdditionalEquipment firstAddEquip = new AdditionalEquipment(0, "Нет снаряжения");
    AdditionalEquipment secondAddEquip = new AdditionalEquipment(0, "Нет снаряжения");
    boolean fought = false, health = true, defender = false;
    boolean inMedBay = false, doingAnAmbush = false, hitInHead = false, onPainkillers = false;
    boolean bleeding = false, fractured = false;
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

    public void setArmor(ProtectionEquip armor) {
        this.armor = armor;
    }

    public void setHelmet(ProtectionEquip helmet) {
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
    public boolean isNotWounded() {
        return health;
    }
    public void setHealth(boolean health) {
        this.health = health;
    }
    public ProtectionEquip getArmor() {
        return armor;
    }
    public ProtectionEquip getHelmet() {
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
    public boolean isInMedBay() {
        return inMedBay;
    }
    public void setInMedBay(boolean inMedBay) {
        this.inMedBay = inMedBay;
    }
    public boolean isDoingAnAmbush() {
        return doingAnAmbush;
    }
    public void setDoingAnAmbush(boolean doingAnAmbush) {
        this.doingAnAmbush = doingAnAmbush;
    }
    public boolean isHitInHead() {
        return hitInHead;
    }
    public void setHitInHead(boolean hitInHead) {
        this.hitInHead = hitInHead;
    }
    public boolean isOnPainkillers() {
        return onPainkillers;
    }
    public void setOnPainkillers(boolean onPainkillers) {
        this.onPainkillers = onPainkillers;
    }
    public boolean isBleeding() {
        return bleeding;
    }
    public void setBleeding(boolean bleeding) {
        this.bleeding = bleeding;
    }
    public boolean isNotFractured() {
        return !fractured;
    }
    public void setFractured(boolean fractured) {
        this.fractured = fractured;
    }
    public String getStatuses(){
        return (inMedBay ? "1":"0") + ":" + (doingAnAmbush ? "1":"0") + ":" + (hitInHead ? "1":"0") + ":"
                + (onPainkillers ? "1":"0") + ":" + (bleeding ? "1":"0") + ":" + (fractured ? "1":"0");
    }
    public void setStatuses(ArrayList<Integer> statuses){
        inMedBay = statuses.get(0) == 1;
        doingAnAmbush = statuses.get(1) == 1;
        hitInHead = statuses.get(2) == 1;
        onPainkillers = statuses.get(3) == 1;
        bleeding = statuses.get(4) == 1;
        fractured = statuses.get(5) == 1;
    }
}
