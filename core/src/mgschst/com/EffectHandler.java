package mgschst.com;

import mgschst.com.connect.DatabaseHandler;
import mgschst.com.dbObj.Card;
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

public class EffectHandler {
    static Connection conn = new DatabaseHandler().getConnection();
    static HashMap<Integer, String> effectMap;
    static {
        effectMap = new HashMap<>();
        fillMap();
    }

    public static void handEffect(int effectNumber, Card tempCard, final MainMgschst game){
        playEffect(effectMap.get(effectNumber).split(","), tempCard, game);
    }

    public static void fillMap(){
        effectMap.put(1, "spawn,bandit,2,Призыв 2 случайных бандитов");
        effectMap.put(2, "Призыв 2 случайных образцов русского вооружения");
        effectMap.put(3, "people,bandit,Вызов стандартного бандита");
        effectMap.put(4, "В сооружении можно починить снаряжение до максимальной прочности и снять отрицательные статусы");
        effectMap.put(5, "Установить цель по названию карты");
        effectMap.put(6, "Чинит снаряжение до максимальной прочности и снимает отрицательные статусы");
        effectMap.put(7, "Убивает цель атаки, если на ней нет брони");
        effectMap.put(8, "Отрицательный статус. Эффект снаряжения имеет шанс 25% не сработать");
        effectMap.put(9, "Оружие производит две атаки");
        effectMap.put(10, "Имеет 50% шанс дать дополнительное действие");
        effectMap.put(11, "Можно действовать лишь раз в два раунда");
        effectMap.put(14, "При атаке на вашего бойца, он будет атаковать первый");
        effectMap.put(15, "Выдача снаряжения российскому наёмнику");
        effectMap.put(16, "Ваш боец при атаке учитывает только защиту шлема противника и гарантированно попадает");
        effectMap.put(17, "При атаке на ваше сооружение один из атакующих умирает, теряя все снаряжение");
        effectMap.put(18, "Останавливает кровотечение");
        effectMap.put(19, "Лечит из ранен в цел");
        effectMap.put(20, "18 и 19 эффект");
        effectMap.put(21, "Отменяет перелом");
        effectMap.put(22, "75% шанс отменить ранение до конца раунда");
        effectMap.put(23, "В конце раунда боец умирает, если был ранен, или становится ранен, если был цел");
        effectMap.put(24, "Боец не может иметь статус здоровья лучше чем ранен");
        effectMap.put(25, "Боец не может использовать снайперские винтовки и оптические прицелы");
        effectMap.put(26, "Призыв 2 случайных российских наёмников");
        effectMap.put(27, "Призыв 2 случайных российских военных");
        effectMap.put(28, "Выдача снаряжения российскому военному");
        effectMap.put(29, "Задачи, связанные с обороной выполняются на 1 раунд быстрее");
        effectMap.put(30, "Задачи выполняются на 1 раунд быстрее");
        effectMap.put(31, "Атаки этого бойца не вызывают ответную атаку");
        effectMap.put(32, "Оружие бойца обязательно снайперское (РФ)");
        effectMap.put(33, "Добавляет бойцу одно дополнительное снаряжение");
        effectMap.put(34, "Атакованный не получает урон, но щит разрушается");
        effectMap.put(35, "Подавляет одного противника и с шансом 25% ранит его");
        effectMap.put(36, "Подавляет всех противников при обороне, или одного при атаке");
        effectMap.put(37, "Выдача 2 случайных медикаментов");
        effectMap.put(38, "Выдача 4 случайных медикаментов");
        effectMap.put(39, "Один человек становится неактивен. Вы получаете 2 карты случайного снаряжения");
        effectMap.put(40, "Два человека становятся неактивны. Вы получаете 6 карт случайного снаряжения. Шанс 25%, что боец с самой низкой защитой умрет");
        effectMap.put(41, "В конце раунда вы получаете 1 карту случайного снаряжения");
        effectMap.put(42, "Если у цели атаки - огнестрельное оружие, она атакует первой");
        effectMap.put(43, "Расширяет медблок на одно место");
        effectMap.put(44, "Восполняет действие \"Отстрелявшегося\"");
        effectMap.put(45, "50% шанс восполнить действие только что \"Отстрелявшегося\"");
        effectMap.put(46, "Вы знаете верхние 5 карт в колоде");
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
                    case "mercenary_eu" -> {
                        // пока что нет
                    }
                    case "army_ru" -> {
                        for (int i = 0; i < Integer.parseInt(commandList[2]); i++)
                            spawnPeople(new ArmyRu(), game);
                    }
                    case "army_eu" -> {
                        // пока что нет 2
                    }
                }
            }
            case "equip" -> {

            }
            case "objective" -> {

            }
            case "action" -> {

            }
            case "people" -> {
                switch (commandList[1]){
                    case "bandit" -> spawnPeople(new Bandit(4), game);
                    case "bandit_attack" -> spawnPeople(new Bandit(15), game);
                    case "bandit_defence" -> spawnPeople(new Bandit(16), game);
                }
            }
        }
    }

    public static void spawnPeople(Person person, final MainMgschst game){
        Card tempCard = getCardByID(person.getId());
        if (tempCard != null)
            tempCard.person = person;
        GameScreen currentScreen = (GameScreen) game.getScreen();
        currentScreen.spawnPeople(tempCard);
    }

    public static Card getCardByID(int id) {
        try {
            PreparedStatement cardPreparedStatement = conn.prepareStatement("SELECT * FROM cards WHERE card_id = ?");
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
        } catch (SQLException exception) { return null; }
    }
}


