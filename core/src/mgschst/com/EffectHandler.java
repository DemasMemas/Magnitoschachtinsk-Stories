package mgschst.com;

import java.util.HashMap;

public class EffectHandler {
    HashMap<Integer, String> effectMap;

    public EffectHandler(){
        this.effectMap = new HashMap<>();
        fillMap();
    }

    public void fillMap(){
        this.effectMap.put(1, "Призыв 2 случайных бандитов");
        this.effectMap.put(2, "Призыв 2 случайных образцов русского вооружения");
        this.effectMap.put(3, "Выдача снаряжения бандиту");
        this.effectMap.put(4, "В сооружении можно починить снаряжение до максимальной прочности и снять отрицательные статусы");
        this.effectMap.put(5, "Установить цель по названию карты");
        this.effectMap.put(6, "Чинит снаряжение до максимальной прочности и снимает отрицательные статусы");
        this.effectMap.put(7, "Убивает цель атаки, если на ней нет брони");
        this.effectMap.put(8, "Отрицательный статус. Эффект снаряжения имеет шанс 25% не сработать");
        this.effectMap.put(9, "Оружие производит две атаки");
        this.effectMap.put(10, "Имеет 50% шанс дать дополнительное действие");
        this.effectMap.put(11, "Можно действовать лишь раз в два раунда");
        this.effectMap.put(14, "При атаке на вашего бойца, он будет атаковать первый");
        this.effectMap.put(15, "Выдача снаряжения российскому наёмнику");
        this.effectMap.put(16, "Ваш боец при атаке учитывает только защиту шлема противника и гарантированно попадает");
        this.effectMap.put(17, "При атаке на ваше сооружение один из атакующих умирает, теряя все снаряжение");
        this.effectMap.put(18, "Останавливает кровотечение");
        this.effectMap.put(19, "Лечит из ранен в цел");
        this.effectMap.put(20, "18 и 19 эффект");
        this.effectMap.put(21, "Отменяет перелом");
        this.effectMap.put(22, "75% шанс отменить ранение до конца раунда");
        this.effectMap.put(23, "В конце раунда боец умирает, если был ранен, или становится ранен, если был цел");
        this.effectMap.put(24, "Боец не может иметь статус здоровья лучше чем ранен");
        this.effectMap.put(25, "Боец не может использовать снайперские винтовки и оптические прицелы");
        this.effectMap.put(26, "Призыв 2 случайных российских наёмников");
        this.effectMap.put(27, "Призыв 2 случайных российских военных");
        this.effectMap.put(28, "Выдача снаряжения российскому военному");
        this.effectMap.put(29, "Задачи, связанные с обороной выполняются на 1 раунд быстрее");
        this.effectMap.put(30, "Задачи выполняются на 1 раунд быстрее");
        this.effectMap.put(31, "Атаки этого бойца не вызывают ответную атаку");
        this.effectMap.put(32, "Оружие бойца обязательно снайперское (РФ)");
        this.effectMap.put(33, "Добавляет бойцу одно дополнительное снаряжение");
        this.effectMap.put(34, "Атакованный не получает урон, но щит разрушается");
        this.effectMap.put(35, "Подавляет одного противника и с шансом 25% ранит его");
        this.effectMap.put(36, "Подавляет всех противников при обороне, или одного при атаке");
        this.effectMap.put(37, "Выдача 2 случайных медикаментов");
        this.effectMap.put(38, "Выдача 4 случайных медикаментов");
        this.effectMap.put(39, "Один человек становится неактивен. Вы получаете 2 карты случайного снаряжения");
        this.effectMap.put(40, "Два человека становятся неактивны. Вы получаете 6 карт случайного снаряжения. Шанс 25%, что боец с самой низкой защитой умрет");
        this.effectMap.put(41, "В конце раунда вы получаете 1 карту случайного снаряжения");
        this.effectMap.put(42, "Если у цели атаки - огнестрельное оружие, она атакует первой");
        this.effectMap.put(43, "Расширяет медблок на одно место");
        this.effectMap.put(44, "Восполняет действие \"Отстрелявшегося\"");
        this.effectMap.put(45, "50% шанс восполнить действие только что \"Отстрелявшегося\"");
        this.effectMap.put(46, "Вы знаете верхние 5 карт в колоде");
    }
}


