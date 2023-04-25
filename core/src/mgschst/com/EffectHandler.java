package mgschst.com;

import mgschst.com.connect.DatabaseHandler;
import mgschst.com.dbObj.Building.Building;
import mgschst.com.dbObj.Card;
import mgschst.com.dbObj.Objective;
import mgschst.com.dbObj.People.ArmyRu;
import mgschst.com.dbObj.People.Bandit;
import mgschst.com.dbObj.People.MercRu;
import mgschst.com.dbObj.People.Person;
import mgschst.com.screens.GameScreen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;

public class EffectHandler {
    static Connection conn = new DatabaseHandler().getConnection();
    public static HashMap<Integer, String> effectMap;
    static {
        effectMap = new HashMap<>();
        fillMap();
    }

    public static void handEffect(int effectNumber, Card tempCard, final MainMgschst game){
        playEffect(effectMap.get(effectNumber).split(","), tempCard, game);
    }

    public static void fillMap(){
        effectMap.put(1, "spawn,bandit,2,Призыв 2 случайных бандитов");
        effectMap.put(2, "summon_equip,weapon,2,Призыв 2 случайных образцов русского вооружения");
        effectMap.put(3, "people,bandit,Вызов стандартного бандита");
        effectMap.put(4, "people,bandit_attack,Вызов бандита-налётчика");
        effectMap.put(5, "people,bandit_defence,Вызов бандита-патрульного");
        effectMap.put(6, "building");
        effectMap.put(7, "Оружие с 75% шансом стреляет в голову");
        effectMap.put(8, "Имеет 50% шанс дать дополнительное действие");
        effectMap.put(9, "people,mercenary_ru,Выдача снаряжения российскому наёмнику");
        effectMap.put(10, "Отстрелявшийся боец не может вернуться в бой в этом раунде");
        effectMap.put(11, "Убивает цель атаки, если на ней нет брони");
        effectMap.put(12, "Поломано. Эффект снаряжения имеет шанс 25% не сработать");
        effectMap.put(13, "Оружие производит две атаки");
        effectMap.put(14, "При атаке на вашего бойца, он будет атаковать первый");
        effectMap.put(15, "objective");
        effectMap.put(16, "Ваш боец при атаке учитывает только защиту шлема противника и гарантированно попадает");
        effectMap.put(17, "При атаке на ваше сооружение один из атакующих умирает, теряя все снаряжение");
        effectMap.put(22, "75% шанс отменить ранение до конца раунда");
        effectMap.put(23, "В конце раунда боец умирает, если был ранен, или становится ранен, если был цел");
        effectMap.put(24, "Боец не может иметь статус здоровья лучше чем ранен");
        effectMap.put(25, "Боец не может использовать снайперские винтовки");
        effectMap.put(26, "spawn,mercenary_ru,2,Призыв 2 случайных российских наёмников");
        effectMap.put(27, "spawn,army_ru,2,Призыв 2 случайных российских военных");
        effectMap.put(28, "Выдача снаряжения российскому военному");
        effectMap.put(29, "Задачи, связанные с обороной выполняются на 1 раунд быстрее");
        effectMap.put(30, "Задачи выполняются на 1 раунд быстрее");
        effectMap.put(31, "Атаки этого бойца не вызывают ответную атаку");
        effectMap.put(32, "people,army_sniper_ru,Вызов снайпера РФ)");
        effectMap.put(33, "people,army_spec-ops_ru,Вызов бойца спецназа РФ");
        effectMap.put(34, "people,army_patrol_ru,Вызов патрульного военного РФ");
        effectMap.put(35, "people,army_ru,Вызов военного РФ");
        effectMap.put(37, "summon_heal,any,2,Выдача 2 случайных медикаментов");
        effectMap.put(38, "summon_heal,good,4,Выдача 4 случайных медикаментов");
        effectMap.put(39, "Один человек становится неактивен. Вы получаете 2 карты случайного снаряжения");
        effectMap.put(40, "Два человека становятся неактивны. Вы получаете 3 карты случайного хорошего снаряжения. Шанс 25%, что боец с самой низкой защитой умрет");
        effectMap.put(42, "Если у цели атаки - огнестрельное оружие, она атакует первой");
        effectMap.put(44, "Восполняет действие \"Отстрелявшегося\"");
    }

    public static void playEffect(String[] commandList, Card tempCard, final MainMgschst game){
        switch (commandList[0]){
            case "spawn" -> {
                switch (commandList[1]){
                    case "bandit" -> {
                        for (int i = 0; i < Integer.parseInt(commandList[2]); i++)
                            spawnPeople(new Bandit(), game);
                    }
                    case "mercenary_ru" -> {
                        for (int i = 0; i < Integer.parseInt(commandList[2]); i++)
                            spawnPeople(new MercRu(), game);
                    }
                    case "army_ru" -> {
                        for (int i = 0; i < Integer.parseInt(commandList[2]); i++){
                            int id = 0;
                            Random random = new Random();
                            switch (random.nextInt(4)){
                                case 0 -> id = 34;
                                case 1 -> id = 35;
                                case 2 -> id = 36;
                                case 3 -> id = 37;
                            }
                            spawnPeople(new ArmyRu(id), game);
                        }
                    }
                }
            }
            case "objective" -> {
                GameScreen currentScreen = (GameScreen) game.getScreen();
                currentScreen.setNewObjective(new Objective(tempCard.card_id));
            }
            case "building" -> spawnBuilding(new Building(tempCard.card_id), game);
            case "people" -> {
                switch (commandList[1]){
                    case "bandit" -> spawnPeople(new Bandit(4), game);
                    case "bandit_attack" -> spawnPeople(new Bandit(16), game);
                    case "bandit_defence" -> spawnPeople(new Bandit(15), game);
                    case "mercenary_ru" -> spawnPeople(new MercRu(), game);
                    case "army_sniper_ru" -> spawnPeople(new ArmyRu(37), game);
                    case "army_spec-ops_ru" -> spawnPeople(new ArmyRu(36), game);
                    case "army_patrol_ru" -> spawnPeople(new ArmyRu(35), game);
                    case "army_ru" -> spawnPeople(new ArmyRu(34), game);
                }
            }
            case "summon_heal" -> {
                Random random  = new Random();
                int number = random.nextInt(58);
                int minimumRareness = 1;
                if (commandList[1].equals("good")) minimumRareness = 2;
                for (int i = 0; i < Integer.parseInt(commandList[2]);i++){
                    while (number == 0 || !(getCardByID(number).type.contains("equip_heal") && getCardByID(number).rareness >= minimumRareness))
                        number = random.nextInt(58);
                    takeCardNotFromDeck(number, game);
                }
            }
            case "summon_equip" -> {
                Random random  = new Random();
                int number = random.nextInt(58);
                for (int i = 0; i < Integer.parseInt(commandList[2]);i++){
                    while (number == 0 || !getCardByID(number).type.contains("equip_" + commandList[1]))
                        number = random.nextInt(58);
                    takeCardNotFromDeck(number, game);
                }
            }
        }
    }
    public static void takeCardNotFromDeck(int id, final MainMgschst game){
        GameScreen currentScreen = (GameScreen) game.getScreen();
        currentScreen.takeCardNotFromDeck(id);
    }
    public static void spawnPeople(Person person, final MainMgschst game){
        Card tempCard = getCardByID(person.getId());
        if (tempCard != null) tempCard.person = person;
        GameScreen currentScreen = (GameScreen) game.getScreen();
        currentScreen.spawnPeople(tempCard);
    }

    public static void spawnBuilding(Building building, final MainMgschst game){
        Card tempCard = getCardByID(building.getId());
        if (tempCard != null) tempCard.building = building;
        GameScreen currentScreen = (GameScreen) game.getScreen();
        currentScreen.spawnBuilding(tempCard);
    }

    public static Card getCardByID(int id) {
        return getCard(id, conn);
    }

    public static Card getCard(int id, Connection conn) {
        try {
            PreparedStatement cardPreparedStatement = conn.prepareStatement("SELECT * FROM cards WHERE card_id = ?");
            return makeNewCard(id, cardPreparedStatement);
        } catch (SQLException exception) { return null; }
    }

    public static Card makeNewCard(int id, PreparedStatement cardPreparedStatement) throws SQLException {
        cardPreparedStatement.setInt(1, id);
        ResultSet cardResultSet = cardPreparedStatement.executeQuery();
        cardResultSet.next();
        return new Card(cardResultSet.getInt("card_id"),
                cardResultSet.getString("name"),
                cardResultSet.getString("image_path"),
                cardResultSet.getString("type"),
                cardResultSet.getString("description"),
                cardResultSet.getInt("deck_limit"),
                cardResultSet.getString("cost_type"),
                cardResultSet.getInt("health_status"),
                cardResultSet.getString("effects"),
                cardResultSet.getInt("price"),
                cardResultSet.getInt("rareness"),
                cardResultSet.getInt("attack"),
                cardResultSet.getInt("defence"),
                cardResultSet.getInt("stealth"));
    }
}


