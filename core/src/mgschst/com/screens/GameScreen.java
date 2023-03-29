package mgschst.com.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import mgschst.com.EffectHandler;
import mgschst.com.MainMgschst;
import mgschst.com.connect.DatabaseHandler;
import mgschst.com.connect.TCPConnection;
import mgschst.com.dbObj.Building.Building;
import mgschst.com.dbObj.Card;
import mgschst.com.dbObj.Equipment.Armor;
import mgschst.com.dbObj.Equipment.Helmet;
import mgschst.com.dbObj.Equipment.Weapon;
import mgschst.com.dbObj.Objective;
import mgschst.com.dbObj.People.ObjectivePerson;
import mgschst.com.dbObj.People.Person;
import mgschst.com.dbObj.SecondPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import static mgschst.com.EffectHandler.getCard;

public class GameScreen implements Screen {
    final MainMgschst game;
    final OrthographicCamera camera;
    final Batch batch;
    Stage stage;
    Image background, chatSwitch;
    TextField chat;
    TextButton sendMessage, endTurnButton;
    VerticalGroup chatGroup;
    Label firstCardCounter, secondCardCounter, turnTimer, turnLabel;

    TCPConnection playerConn;
    Skin neonSkin = new Skin(Gdx.files.internal("Skins/neon/skin/neon-ui.json"));
    Connection conn = new DatabaseHandler().getConnection();

    String profilePicturePath = null;
    String cardPicturePath = null;
    String boardPicturePath = null;

    SecondPlayer secondPlayer;
    HorizontalGroup firstPlayerHand  = new HorizontalGroup(), secondPlayerHand = new HorizontalGroup();
    boolean myTurn = false;
    Stage cardStage = new Stage();
    Boolean isCardStageActive = false, isAttackActive = false;
    HorizontalGroup firstPlayerField = new HorizontalGroup(), secondPlayerField = new HorizontalGroup();
    HashMap<Integer, Card> firstPlayerActiveCards = new HashMap<>();
    HashMap<Integer, Card> secondPlayerActiveCards = new HashMap<>();
    static int lastPlayedCardID = 0;
    static int lastSecondPlayerPlayedCardID = 0;
    Label victoryPointsFirstPlayer, victoryPointsSecondPlayer;
    int victoryPoints = 0, completedObjectives = 0;
    VerticalGroup resourcesGroup = new VerticalGroup();
    int resourceMax = 5;
    Objective playerObjective = new Objective(0), enemyObjective = new Objective(0);
    Image enemyObjectiveImage, objectiveImage;
    Card objectiveCard;
    int objectiveCardID;

    Card currentAttacker;

    public GameScreen(final MainMgschst game) {
        PreparedStatement preparedStatement;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE nickname = ?");
            preparedStatement.setString(1, game.getCurrentUserName());
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            cardPicturePath = resultSet.getString("card_picture_path");
            boardPicturePath = resultSet.getString("board_picture_path");
            profilePicturePath = resultSet.getString("profile_picture_path");
        } catch (SQLException exception) { exception.printStackTrace(); }

        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        playerConn = game.getPlayerConnection();

        camera = game.getCamera();
        batch = game.batch;

        game.xScaler = stage.getWidth()/1920f;
        game.yScaler = stage.getHeight()/1080f;

        background = new Image(new Texture(Gdx.files.internal("UserInfo/Boards/" + boardPicturePath )));
        background.setPosition(0,0);
        stage.addActor(background);

        chatInitialize();
        firstPlayerInitialize();

        endTurnButton = new TextButton("Закончить ход", game.getTextButtonStyle());
        endTurnButton.setDisabled(true);
        endTurnButton.setWidth(endTurnButton.getWidth() * game.xScaler);
        endTurnButton.setPosition(25, 525);
        endTurnButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                if (myTurn) {
                    endTurn();
                    setPlayerEndedTurn();
                }
            }});
        stage.addActor(endTurnButton);

        turnTimer = new Label("60 секунд...", game.getMainLabelStyle());
        turnTimer.setPosition(25, 485);
        turnTimer.setWidth(endTurnButton.getWidth());
        turnTimer.setAlignment(Align.center);
        stage.addActor(turnTimer);

        turnLabel = new Label("", game.getMainLabelStyle());

        firstPlayerField.setPosition(360, 195);
        firstPlayerField.setWidth(1270 * game.xScaler);
        firstPlayerField.setHeight(270 * game.xScaler);
        firstPlayerField.align(Align.center);
        firstPlayerField.space(10f);
        firstPlayerField.pad(10f);
        stage.addActor(firstPlayerField);
        stage.addActor(firstPlayerHand);
        secondPlayerHand.setPosition(175, 975);
        stage.addActor(secondPlayerHand);

        for (Actor actor:stage.getActors()) {
            actor.scaleBy(game.xScaler - 1,  game.yScaler - 1);
            actor.setPosition(actor.getX() * game.xScaler, actor.getY() * game.yScaler);
        }
    }

    @Override
    public void render(float delta) {
        DeckBuildingScreen.renderScreenWithCardStage(game, camera, batch, stage, isCardStageActive, cardStage);
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() {}
    @Override public void hide() { dispose();}
    @Override public void show() { }

    @Override
    public void dispose() {
        stage.dispose();
        game.setCurrentGameID(0);
    }

    public void changeLabel(String text){
        StringBuilder tempSB = new StringBuilder(text);
        while (tempSB.length() > 33){
            chatGroup.addActor( new Label(tempSB.substring(0, 33), game.getMainLabelStyle()));
            tempSB.delete(0, 33); }
        chatGroup.addActor( new Label(tempSB, game.getMainLabelStyle()));
    }
    public void openMenu(){ Gdx.app.postRunnable(() -> game.setScreen(new MainMenuScreen(game))); }
    public void startGame(String secondPlayerNick){
        secondPlayer = SecondPlayer.getPlayerByNick(secondPlayerNick);
        secondPlayerInitialize();
    }
    public void chatInitialize(){
        chatGroup = new VerticalGroup();
        chatGroup.columnAlign(Align.left);
        chatGroup.pad(25f,35f,35f,25f).space(5f);
        ScrollPane chatScrollPane = new ScrollPane(chatGroup, neonSkin);
        chatScrollPane.setOverscroll(false, true);
        chatScrollPane.setScrollingDisabled(true, false);
        Table chatContainerTable = new Table();
        chatContainerTable.add(chatScrollPane);
        stage.addActor(chatContainerTable);
        chatContainerTable.setPosition(25 * game.xScaler, 200 * game.yScaler);
        chatContainerTable.setSize(1000 * game.xScaler, 750 * game.yScaler);
        chatContainerTable.setVisible(false);
        chatContainerTable.setBackground(new TextureRegionDrawable
                (new Texture(Gdx.files.internal("Images/open_crate_dialog_bg.png"))));

        chatSwitch = new Image(new Texture(Gdx.files.internal("Images/chat.png")));
        chatSwitch.setPosition(25,950);
        chatSwitch.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                sendMessage.setVisible(!chat.isVisible());
                chatContainerTable.setVisible(!chat.isVisible());
                chat.setVisible(!chat.isVisible());
                chatContainerTable.toFront();
                chat.toFront();
                sendMessage.toFront();
            }});
        stage.addActor(chatSwitch);

        chat = new TextField("", game.getTextFieldStyle());
        chat.setMessageText("Введите сообщение:");
        chat.setPosition(25, 160);
        chat.setWidth(1000f * game.xScaler);
        chat.setVisible(false);
        stage.addActor(chat);

        sendMessage = new TextButton("Отправить сообщение", game.getTextButtonStyle());
        sendMessage.setVisible(false);
        stage.addActor(sendMessage);
        sendMessage.setPosition(25, 100);

        sendMessage.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                if (!chat.getText().equals(""))
                    playerConn.sendString("chatMsg," + game.getCurrentUserName() + ","
                            + game.getCurrentGameID() + "," + chat.getText().replace(",", "```"));
                chat.setText("");
            }
        });
    }
    public void firstPlayerInitialize(){
        // счетчик карт в колоде
        firstCardCounter = new Label("50", game.getMainLabelStyle());
        firstCardCounter.setPosition(25, 285);
        firstCardCounter.setAlignment(Align.center);
        firstCardCounter.setWidth(150f * game.xScaler);
        firstCardCounter.setWrap(true);
        // рубашка колоды первого игрока
        Image firstDeck = new Image(new Texture(Gdx.files.internal
                ("UserInfo/Cards/GameCards/" + cardPicturePath)));
        firstDeck.setPosition(25, 200);
        stage.addActor(firstDeck);
        stage.addActor(firstCardCounter);
        Image firstAvatar = new Image(new Texture(Gdx.files.internal
                ("UserInfo/Avatars/" + profilePicturePath)));
        firstAvatar.setPosition(36, 50);
        stage.addActor(firstAvatar);
        Label firstName = new Label(game.getCurrentUserName(), game.getMainLabelStyle());
        firstName.setPosition(175, 90);
        stage.addActor(firstName);
        firstPlayerHand.setPosition(565, 70);
        firstPlayerHand.space(-35f);

        victoryPointsFirstPlayer = new Label("ПО: 0/5", game.getMainLabelStyle());
        victoryPointsFirstPlayer.setColor(Color.GOLDENROD);
        victoryPointsFirstPlayer.setPosition(1750, 50);
        stage.addActor(victoryPointsFirstPlayer);

        resourcesGroup.setPosition(1685, 500);
        resourcesGroup.space(5f);
        addNewResource(resourceMax);
        stage.addActor(resourcesGroup);
    }
    public void secondPlayerInitialize(){
        // счетчик карт в колоде
        secondCardCounter = new Label("50", game.getMainLabelStyle());
        secondCardCounter.setPosition(1740, 755);
        secondCardCounter.setAlignment(Align.center);
        secondCardCounter.setWidth(150f * game.xScaler);
        secondCardCounter.setWrap(true);
        // рубашка колоды первого игрока
        Image secondDeck = new Image(new Texture(Gdx.files.internal
                ("UserInfo/Cards/GameCards/" + secondPlayer.cardPicturePath)));
        secondDeck.setPosition(1740, 670);
        secondDeck.addListener(new ClickListener(){
            public void clicked(InputEvent event, float x, float y){
                // показать верхние 5 карт, если разведцентр установлен
            }
        });
        stage.addActor(secondDeck);
        stage.addActor(secondCardCounter);
        Image secondAvatar = new Image(new Texture(Gdx.files.internal
                ("UserInfo/Avatars/" + secondPlayer.profilePicturePath)));
        secondAvatar.setPosition(1750, 900);
        stage.addActor(secondAvatar);
        Label secondName = new Label(secondPlayer.nickname, game.getMainLabelStyle());
        secondName.setPosition(1350, 940);
        secondName.setWidth(24*16f * game.xScaler);
        secondName.setAlignment(Align.right);
        stage.addActor(secondName);

        secondPlayerHand.space(-40f);

        secondPlayerField.setPosition(360, 595);
        secondPlayerField.setWidth(1270 * game.xScaler);
        secondPlayerField.setHeight(270 * game.xScaler);
        secondPlayerField.align(Align.center);
        secondPlayerField.space(10f);
        secondPlayerField.pad(10f);
        stage.addActor(secondPlayerField);

        victoryPointsSecondPlayer = new Label("ПО: 0/5", game.getMainLabelStyle());
        victoryPointsSecondPlayer.setColor(Color.GOLDENROD);
        victoryPointsSecondPlayer.setPosition(15, 900);
        stage.addActor(victoryPointsSecondPlayer);
    }

    public void changeTimer(int time){
        if (time == 0 && myTurn) { endTurn(); setPlayerEndedTurn(); return; }
        String timer = String.valueOf(time);
        if (timer.startsWith("1") && timer.length() == 2){
            turnTimer.setText(time + " секунд...");
        } else if (timer.endsWith("1")){
            turnTimer.setText(time + " секунда...");
        } else if (timer.endsWith("2") || timer.endsWith("3") || timer.endsWith("4")){
            turnTimer.setText(time + " секунды...");
        } else { turnTimer.setText(time + " секунд..."); }
    }

    public void endTurn(){
        // передача хода другому игроку
        playerConn.sendString("changeTurn," + game.getCurrentGameID());
        useTurnLabel("Ход игрока: " + secondPlayer.nickname);
        myTurn = false;
    }

    public void takeTurn(){
        // взятие хода текущим игроком
        useTurnLabel("Ход игрока: " + game.getCurrentUserName());
        myTurn = true;
    }

    public void showFirstPlayer(String player){
        turnLabel.setPosition(800, 700);
        turnLabel.setWidth(300f * game.xScaler);
        turnLabel.setAlignment(Align.center);
        stage.addActor(turnLabel);
        useTurnLabel("Игру начинает: " + player);

        for (int i = 0; i < 7; i++)
            playerConn.sendString("takeCard," + game.getCurrentGameID() + ","+game.getCurrentUserName());

        if (player.equals(game.getCurrentUserName())) takeTurn();
    }

    public AlphaAction createAlphaAction() {
        AlphaAction tempAlphaAction = new AlphaAction();
        tempAlphaAction.setAlpha(0f);
        tempAlphaAction.setDuration(2f);
        return tempAlphaAction;
    }

    public void takeCard(int cardID){
        firstCardCounter.setText(Integer.parseInt(String.valueOf(firstCardCounter.getText())) - 1);
        // добавить карту в руку
        Card tempCard = getCardByID(cardID);
        Image tempImage = new Image(new Texture(
                Gdx.files.internal("Cards/inGame/" + tempCard.image_path)));
        tempImage.setName("");
        tempImage.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                // открыть описание карты
                if (tempImage.getName().equals("")) fillCurrentCardStage(tempCard, tempImage);
            }
        });
        tempImage.addListener(new DragListener(){
            public void dragStart(InputEvent event, float x, float y, int pointer) {
                tempImage.setName("isDragged"); }
            public void drag(InputEvent event, float x, float y, int pointer) {
                tempImage.moveBy(x - tempImage.getWidth() / 2, y - tempImage.getHeight() / 2); }
            public void dragStop(InputEvent event, float x, float y, int pointer) {
                tempImage.setName("");
                if (canCardBePlaced(tempCard, tempImage)) playCard(tempCard, tempImage);
                else returnCardInHand(tempImage);
            }
        });
        firstPlayerHand.addActor(tempImage);
    }
    public boolean canCardBePlaced(Card tempCard, Image tempImage){
        if (canCardBePlacedFromHand(tempCard)){
            // проверка на нахождение в игровой зоне (зона сдвинута на 565/70 из-за либгыдыха)
            return tempImage.getX() + (tempImage.getWidth() / 2) < 1065 * game.xScaler &&
                    tempImage.getX() + (tempImage.getWidth() / 2) > -205 * game.xScaler &&
                    tempImage.getY() + (tempImage.getHeight() / 2) > 125 * game.yScaler &&
                    tempImage.getY() + (tempImage.getHeight() / 2) < 395 * game.yScaler;
        } else return false;
    }
    public boolean canCardBePlacedFromHand(Card tempCard) {
        // проверка на то, чей ход
        if (!myTurn){return false;}

        // Проверка на количество разыгранных карт
        if (firstPlayerField.getChildren().size >= 8 &&
                ((tempCard.type.equals("building") || tempCard.type.equals("people") || tempCard.type.equals("summon_people"))
                        ||(tempCard.type.equals("objective")&&tempCard.card_id != 6)))
        {useTurnLabel("Нельзя разыграть больше карт");return false;}

        // Проверка на наличие людей на карте
        if ((tempCard.type.equals("equip_weapon")||tempCard.type.equals("equip_helmet")||
                tempCard.type.equals("equip_armor")||tempCard.type.equals("equip_heal")||
                tempCard.type.equals("equip_add"))&&
                firstPlayerActiveCards.values().stream().noneMatch(x -> x.type.equals("people"))){
            useTurnLabel("Нет активных людей для использования");return false;}

        // Проверка на наличие активной цели
        if (tempCard.type.equals("objective")&&!playerObjective.equals(0)){
            useTurnLabel("У вас уже есть активная цель");return false;}

        return true;
    }
    public void returnCardInHand(Image tempImage){
        firstPlayerHand.removeActor(tempImage);
        firstPlayerHand.addActor(tempImage);
    }
    public Card getCardByID(int id) {
        return getCard(id, conn);
    }

    public void useTurnLabel(String text){
        turnLabel.setText(text);
        turnLabel.getColor().a = 1;
        turnLabel.clearActions();
        turnLabel.addAction(createAlphaAction());
    }

    public void fillCurrentCardStage(Card currentCard, Image tempImage) {
        startCardStage(currentCard, 800);

        Label cardDesc = new Label(currentCard.description, game.getMainLabelStyle());
        cardDesc.setPosition(475, 550);
        addCardPriceAndDescOnCardStage(currentCard, cardDesc, game, cardStage);

        TextButton addButton = new TextButton("Разыграть", game.getTextButtonStyle());
        addButton.setPosition(860, 250);
        addButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isCardStageActive = false;
                Gdx.input.setInputProcessor(stage);
                cardStage.dispose();
                if (canCardBePlacedFromHand(currentCard)) playCard(currentCard, tempImage);
            }
        });
        cardStage.addActor(addButton);

        for (Actor actor:cardStage.getActors()) {
            actor.scaleBy(game.xScaler - 1,  game.yScaler - 1);
            actor.setPosition(actor.getX() * game.xScaler, actor.getY() * game.yScaler);
        }
    }

    static void addCardPriceAndDescOnCardStage(Card currentCard, Label cardDesc, MainMgschst game, Stage cardStage) {
        cardDesc.setWidth(1000 * game.xScaler);
        cardDesc.setWrap(true);
        cardDesc.setAlignment(Align.center);
        cardStage.addActor(cardDesc);

        Label cardPrice = new Label("", game.getMainLabelStyle());
        switch (currentCard.cost_type) {
            case "free" -> cardPrice.setText("Бесплатно");
            case "prapor" -> cardPrice.setText("Прапор: " + currentCard.price + " поддержк" + (currentCard.price == 1 ? "а" : "и"));
            case "mechanic" -> cardPrice.setText("Механик: " + currentCard.price + " поддержк" + (currentCard.price == 1 ? "а" : "и"));
            case "therapist" -> cardPrice.setText("Терапевт:  " + currentCard.price + " поддержк" + (currentCard.price == 1 ? "а" : "и"));
            case "any" -> cardPrice.setText("Любая поддержка:  " + currentCard.price);
        }
        cardPrice.setPosition(50, 950);
        cardPrice.setWidth(400 * game.xScaler);
        cardPrice.setWrap(true);
        cardPrice.setAlignment(Align.center);
        cardStage.addActor(cardPrice);
    }

    private void startCardStage(Card currentCard, float cardNameHeight) {
        isCardStageActive = true;
        cardStage = new Stage();
        Gdx.input.setInputProcessor(cardStage);

        fillCardStageBasis(currentCard, cardStage, game, cardNameHeight);

        TextButton closeButton = new TextButton("Закрыть", game.getTextButtonStyle());
        closeButton.setPosition(160, 250);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isCardStageActive = false;
                Gdx.input.setInputProcessor(stage);
                cardStage.dispose();
            }
        });
        cardStage.addActor(closeButton);
    }

    static void fillCardStageBasis(Card currentCard, Stage cardStage, MainMgschst game, float cardNameHeight) {
        Image bg = new Image(new Texture(Gdx.files.internal("DeckAssets/card_info_bg.png")));
        bg.setPosition(25, 125);
        cardStage.addActor(bg);

        Image cardImage = new Image(new Texture(Gdx.files.internal("Cards/origs/" + currentCard.image_path)));
        cardImage.setPosition(50, 325);
        cardStage.addActor(cardImage);

        Label cardName = new Label(currentCard.name, game.getMainLabelStyle());
        cardName.setPosition(975 - (currentCard.name.length() * 12), cardNameHeight);
        cardStage.addActor(cardName);
    }

    public void playCard(Card tempCard, Image tempImage){
        if (!checkPrice(tempCard, tempImage)) return;
        playerConn.sendString("cardFromHand," + game.getCurrentGameID() + "," + game.getCurrentUserName());
        if (tempCard.effects != null)
            for(String effect:tempCard.effects.split(","))
                EffectHandler.handEffect(Integer.parseInt(effect), tempCard, game);
        // убрать карту из руки
        firstPlayerHand.removeActor(tempImage);
        tempImage.getListeners().clear();
        endTurn();
    }

    public void addNewResource(int times){
        Random random = new Random();
        for (int i = 0; i < times; i++){
            String type = "";
            switch (random.nextInt(6)){
                case 0, 5 -> type = "any";
                case 1 -> type = "prapor";
                case 2 -> type = "peacekeeper";
                case 3 -> type = "therapist";
                case 4 -> type = "mechanic";
            }
            Image tempImage = new Image(new Texture(Gdx.files.internal("DeckAssets/ResIco/" + type + ".png")));
            tempImage.setName(type);
            resourcesGroup.addActor(tempImage);
        }
    }

    public boolean checkPrice(Card tempCard, Image tempImage) {
        if (!tempCard.cost_type.equals("free")) {
            int counter = 0;
            if (tempCard.cost_type.equals("any"))
                counter = resourcesGroup.getChildren().size;
            else {
                for (Actor tempActor : resourcesGroup.getChildren())
                    if (tempActor.getName().equals(tempCard.cost_type) ||
                            tempActor.getName().equals("any")) counter += 1;
            }
            if (counter >= tempCard.price) {
                counter = tempCard.price;
                ArrayList<Actor> deleteList = new ArrayList<>();
                if (tempCard.cost_type.equals("any")) {
                    for (Actor tempActor : resourcesGroup.getChildren().items) {
                        if (counter == 0) break;
                        try {
                            if (!tempActor.getName().equals(tempCard.cost_type)) {
                                counter -= 1;
                                deleteList.add(tempActor);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    for (Actor tempActor : deleteList)
                        resourcesGroup.removeActor(tempActor, true);
                    deleteList.clear();
                    deleteSameResource(tempCard, counter, deleteList);
                } else {
                    counter = deleteSameResource(tempCard, counter, deleteList);
                    deleteList.clear();
                    for (Actor tempActor : resourcesGroup.getChildren().items) {
                        if (counter == 0) break;
                        try {
                            if (tempActor.getName().equals("any")) {
                                counter -= 1;
                                deleteList.add(tempActor);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    for (Actor tempActor : deleteList)
                        resourcesGroup.removeActor(tempActor, true);
                }
                deleteList.clear();
            } else {
                firstPlayerHand.removeActor(tempImage);
                firstPlayerHand.addActor(tempImage);
                useTurnLabel("Недостаточно поддержки");
                return false;
            }
        }
        return true;
    }

    private int deleteSameResource(Card tempCard, int counter, ArrayList<Actor> deleteList) {
        for (Actor tempActor : resourcesGroup.getChildren().items) {
            if (counter == 0) break;
            try {
                if (tempActor.getName().equals(tempCard.cost_type)) {
                    counter -= 1;
                    deleteList.add(tempActor);
                }
            } catch (Exception ignored) {
            }
        }
        for (Actor tempActor : deleteList)
            resourcesGroup.removeActor(tempActor, true);
        return counter;
    }
    public void spawnPeople(Card card){
        Image tempImage = addToPlayedCards(card);
        playerConn.sendString("playCard," + game.getCurrentGameID() + "," + game.getCurrentUserName() + "," +
                "person," + card.getPersonCard());
        tempImage.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                fillPersonCardStage(card);
            }
        });
    }
    public void spawnBuilding(Card card){
        Image tempImage = addToPlayedCards(card);
        playerConn.sendString("playCard," + game.getCurrentGameID() + "," + game.getCurrentUserName() + "," +
                "building," + card.getBuildingCard());
        tempImage.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){

            }
        });
    }
    public Image addToPlayedCards(Card card){
        // разместить карту на поле
        firstPlayerActiveCards.put(lastPlayedCardID, card);
        Image tempImage = new Image(new Texture(
                Gdx.files.internal("Cards/inGame/" + card.image_path)));
        tempImage.setName(String.valueOf(lastPlayedCardID));
        lastPlayedCardID++;
        firstPlayerField.addActor(tempImage);
        return tempImage;
    }
    public void placeEnemyCard(String cardType, String cardInfo){
        String[] info = cardInfo.split(" ");
        Card tempCard = getCardByID(Integer.parseInt(info[0]));
        if (cardType.equals("people")){
            Person tempPerson = new Person();

            String[] armorInfo = info[1].split(";");
            Armor tempArmor;
            if (armorInfo.length == 3)
                tempArmor = new Armor(Integer.parseInt(armorInfo[0]), Integer.parseInt(armorInfo[1]),
                        armorInfo[2], null);
            else
                tempArmor = new Armor(Integer.parseInt(armorInfo[0]), Integer.parseInt(armorInfo[1]),
                        armorInfo[2], Arrays.stream(armorInfo[3].split(";")).mapToInt(Integer::parseInt).toArray());
            tempPerson.setArmor(tempArmor);

            String[] weaponInfo = info[1].split(";");
            Weapon tempWeapon;
            if (armorInfo.length == 3)
                tempWeapon = new Weapon(Integer.parseInt(weaponInfo[0]), Integer.parseInt(weaponInfo[1]),
                        weaponInfo[2], null);
            else
                tempWeapon = new Weapon(Integer.parseInt(weaponInfo[0]), Integer.parseInt(weaponInfo[1]),
                        weaponInfo[2], Arrays.stream(weaponInfo[3].split(";")).mapToInt(Integer::parseInt).toArray());
            tempPerson.setWeapon(tempWeapon);

            // set everything else later
            tempCard.person = tempPerson;
        } else tempCard.building = new Building(Integer.parseInt(info[0]));

        secondPlayerActiveCards.put(lastSecondPlayerPlayedCardID, tempCard);

        Image tempImage = new Image(new Texture(
                Gdx.files.internal("Cards/inGame/" + tempCard.image_path)));
        tempImage.setName(String.valueOf(lastSecondPlayerPlayedCardID));
        lastSecondPlayerPlayedCardID++;
        tempImage.addListener(new ClickListener(){
            public void clicked(InputEvent event, float x, float y){
                if (isAttackActive) attack(tempCard, tempImage);
                else startCardStage(tempCard, 600);
            }
        });
        secondPlayerField.addActor(tempImage);
    }

    public void attack(Card tempCard, Image tempImage){
        Random random = new Random();
        Weapon attackerWeapon = currentAttacker.person.getWeapon();
        boolean broken = false;
        ArrayList<Integer> effectList = new ArrayList<>();
        try {
            for(int effectNumb:attackerWeapon.getEffectList())
                effectList.add(effectNumb);
            if (effectList.contains(12) && random.nextInt(101) >= 75)
                broken = true;
        } catch (Exception ignored){}
        if (broken){
            if (random.nextInt(101) >= 75) attackHead(tempCard, attackerWeapon, tempImage);
            else attackArmor(tempCard, attackerWeapon, tempImage);
        } else {
            boolean hitToHead = effectList.contains(7) && random.nextInt(101) >= 25;
            if (effectList.contains(11) && !hitToHead){
                if (random.nextInt(101) >= 75){
                     if(tempCard.person.getHelmet().getDefence() == 0) killEnemy(tempImage);
                     else attackHead(tempCard, attackerWeapon, tempImage);
                }
                else {
                    if (tempCard.person.getArmor().getDefence() == 0) killEnemy(tempImage);
                    else attackArmor(tempCard, attackerWeapon, tempImage);
                }
            }else{
                if (effectList.contains(11) && hitToHead) {
                    if (tempCard.person.getHelmet().getDefence() == 0) killEnemy(tempImage);
                    else attackHead(tempCard, attackerWeapon, tempImage);
                }
            }
            if (effectList.contains(13)){
                if (!hitToHead){
                    for (int i = 0; i<2;i++){
                        if (random.nextInt(101) >= 75) attackHead(tempCard, attackerWeapon, tempImage);
                        else attackArmor(tempCard, attackerWeapon, tempImage);
                    }
                } else for (int i = 0; i<2;i++) attackHead(tempCard, attackerWeapon, tempImage);
            }
        }
        if (!(Arrays.stream(currentAttacker.person.getArmor().getEffect()).filter(x -> x == 8).findAny().isPresent() &&
        random.nextBoolean())){
            boolean firedUp = true;
            for (Card card:firstPlayerActiveCards.values())
                if (card.card_id == 48 && random.nextBoolean()){firedUp = false; break;}
            if (firedUp) currentAttacker.person.setFought(true);
        }
        if (Arrays.stream(currentAttacker.person.getArmor().getEffect()).filter(x -> x == 10).findAny().isPresent())
            currentAttacker.person.setFought(true);
        isAttackActive = false;
        currentAttacker = null;
        endTurn();
    }
    public void attackArmor(Card tempCard, Weapon attackerWeapon, Image tempImage){
        Armor defenderArmor = tempCard.person.getArmor();
        if (defenderArmor.getDefence() >= attackerWeapon.getAttack())
            defenderArmor.setDefence(defenderArmor.getDefence() - attackerWeapon.getAttack());
        else {
            defenderArmor.setDefence(0);
            if (tempCard.person.isHealth()) tempCard.person.setHealth(false);
            else killEnemy(tempImage);
        }
        tempCard.person.setArmor(defenderArmor);
    }
    public void attackHead(Card tempCard, Weapon attackerWeapon, Image tempImage){
        Helmet defenderHelmet = tempCard.person.getHelmet();
        if (defenderHelmet.getDefence() >= attackerWeapon.getAttack())
            defenderHelmet.setDefence(defenderHelmet.getDefence() - attackerWeapon.getAttack());
        else {
            defenderHelmet.setDefence(0);
            if (tempCard.person.isHealth()) tempCard.person.setHealth(false);
            else killEnemy(tempImage);
        }
        tempCard.person.setHelmet(defenderHelmet);
    }

    public void killEnemy(Image tempImage){
        if (secondPlayerField.getChildren().contains(tempImage,true)){
            secondPlayerActiveCards.remove(Integer.parseInt(tempImage.getName()));
            secondPlayerField.removeActor(tempImage);
        } else {
            // В случае, если нужно убить цель на своей игровой половине
            firstPlayerActiveCards.remove(Integer.parseInt(tempImage.getName()));
            firstPlayerField.removeActor(tempImage);
        }

    }
    public void dropCardFromEnemyHand(){
        secondPlayerHand.removeActorAt(0, true);
    }
    public void setPlayerEndedTurn(){
        playerConn.sendString("endTurn," + game.getCurrentGameID() + "," + game.getCurrentUserName());
    }
    public void enemyTakeCard(){
        secondPlayerHand.addActor(new Image(new Texture(
                Gdx.files.internal("UserInfo/Cards/GameCards/" + secondPlayer.cardPicturePath))));
        secondCardCounter.setText(Integer.parseInt(String.valueOf(secondCardCounter.getText())) - 1);
    }
    public void updateResources(){
        resourcesGroup.clear();
        addNewResource(resourceMax);
    }
    public void checkRoundEndStatus(){
        checkCardEffects();
        checkObjective();
    }

    public void fillPersonCardStage(Card card){
        startCardStage(card, 900);
        // кнопка атаки
        TextButton attackButton = new TextButton("Атаковать", game.getTextButtonStyle());
        attackButton.setPosition(675, 200);
        attackButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isCardStageActive = false;
                Gdx.input.setInputProcessor(stage);
                if (myTurn && !secondPlayerActiveCards.isEmpty() && card.person.getFoughtStatus().equals("0")){
                    isAttackActive = true;
                    currentAttacker = card;
                }
            }
        });
        cardStage.addActor(attackButton);
        // кнопка обороны
        TextButton defendButton = new TextButton("Оборонять", game.getTextButtonStyle());
        defendButton.setPosition(1025, 200);
        defendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

            }
        });
        cardStage.addActor(defendButton);
        // кнопка создания группы/присоединения к существующей группе
        TextButton groupButton = new TextButton("Собрать группу", game.getTextButtonStyle());
        groupButton.setPosition(600, 150);
        groupButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

            }
        });
        cardStage.addActor(groupButton);
        // кнопка для ухода в медблок
        TextButton medBayButton = new TextButton("Уйти в медблок", game.getTextButtonStyle());
        medBayButton.setPosition(975, 150);
        medBayButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

            }
        });
        cardStage.addActor(medBayButton);

        // надпись о состоянии группы
        Label groupLabel = new Label("Не в группе", game.getMainLabelStyle());
        groupLabel.setPosition(50, 205);
        groupLabel.setWidth(400 * game.xScaler);
        groupLabel.setAlignment(Align.center);
        cardStage.addActor(groupLabel);

        // надписи о вооружении бойца
        Label weaponLabel = new Label("Оружие: " + card.person.getWeaponString().split(";")[2] +
                " Урон: " + card.person.getWeaponString().split(";")[1], game.getMainLabelStyle());
        weaponLabel.setColor(Color.GOLDENROD);
        weaponLabel.setPosition(500, 850);
        weaponLabel.setWidth(1000 * game.xScaler);
        weaponLabel.setAlignment(Align.center);
        cardStage.addActor(weaponLabel);

        int yUp = 0;
        if (card.person.getWeaponString().split(";").length > 3){
            Label weaponEffectLabel = new Label("Эффекты: ", game.getMainLabelStyle());
            StringBuilder effects = new StringBuilder();
            for (String effect:card.person.getWeaponString().split(";")[3].split(":"))
                effects.append(EffectHandler.effectMap.get(Integer.parseInt(effect))).append(", ");
            effects.delete(effects.length() - 2, effects.length() - 1);
            weaponEffectLabel.setText("Эффекты: " + effects);
            weaponEffectLabel.setPosition(500, 800);
            weaponEffectLabel.setWidth(1000 * game.xScaler);
            weaponEffectLabel.setWrap(true);
            weaponEffectLabel.setAlignment(Align.top);
            cardStage.addActor(weaponEffectLabel);
            yUp += 50 * ((weaponEffectLabel.getText().length / 40) + 1);
        }

        // надписи о броне бойца
        Label armorLabel = new Label("Броня: " + card.person.getArmorString().split(";")[2] +
                " Защита: " + card.person.getArmorString().split(";")[1], game.getMainLabelStyle());
        armorLabel.setColor(Color.GOLDENROD);
        armorLabel.setPosition(500, 800 - yUp);
        armorLabel.setWidth(1000 * game.xScaler);
        armorLabel.setAlignment(Align.center);
        cardStage.addActor(armorLabel);

        if (card.person.getArmorString().split(";").length > 3){
            Label armorEffectLabel = new Label("Эффекты: ", game.getMainLabelStyle());
            StringBuilder effects = new StringBuilder();
            for (String effect:card.person.getArmorString().split(";")[3].split(":"))
                effects.append(EffectHandler.effectMap.get(Integer.parseInt(effect))).append(", ");
            effects.delete(effects.length() - 2, effects.length() - 1);
            armorEffectLabel.setText("Эффекты: " + effects);
            armorEffectLabel.setPosition(500, 750 - yUp);
            armorEffectLabel.setWidth(1000 * game.xScaler);
            armorEffectLabel.setWrap(true);
            armorEffectLabel.setAlignment(Align.top);
            cardStage.addActor(armorEffectLabel);
            yUp += 50 * ((armorEffectLabel.getText().length / 40) + 1);
        }

        // надписи о шлеме бойца
        Label helmetLabel = new Label("Шлем: " + card.person.getHelmetString().split(";")[2] +
                " Защита: " + card.person.getHelmetString().split(";")[1], game.getMainLabelStyle());
        helmetLabel.setColor(Color.GOLDENROD);
        helmetLabel.setPosition(500, 750 - yUp);
        helmetLabel.setWidth(1000 * game.xScaler);
        helmetLabel.setAlignment(Align.center);
        cardStage.addActor(helmetLabel);

        if (card.person.getHelmetString().split(";").length > 3){
            Label helmetEffectLabel = new Label("Эффекты: ", game.getMainLabelStyle());
            StringBuilder effects = new StringBuilder();
            for (String effect:card.person.getHelmetString().split(";")[3].split(":"))
                effects.append(EffectHandler.effectMap.get(Integer.parseInt(effect))).append(", ");
            effects.delete(effects.length() - 2, effects.length() - 1);
            helmetEffectLabel.setText("Эффекты: " + effects);
            helmetEffectLabel.setPosition(500, 700 - yUp);
            helmetEffectLabel.setWidth(1000 * game.xScaler);
            helmetEffectLabel.setWrap(true);
            helmetEffectLabel.setAlignment(Align.top);
            cardStage.addActor(helmetEffectLabel);
            yUp += 50 * ((helmetEffectLabel.getText().length / 40) + 1);
        }

        for (Actor actor:cardStage.getActors()) {
            actor.scaleBy(game.xScaler - 1,  game.yScaler - 1);
            actor.setPosition(actor.getX() * game.xScaler, actor.getY() * game.yScaler);
        }
    }

    public void setNewObjective(Objective objective){
        playerObjective = objective;
        objectiveCard = getCardByID(objective.getId());
        objectiveImage = new Image(new Texture(Gdx.files.internal("Cards/inGame/" +
                objectiveCard.image_path)));
        objectiveImage.setPosition(1735, 225);
        fillObjectiveStage(objectiveCard, objectiveImage, objective);
        playerConn.sendString("newObjective," + game.getCurrentGameID()
                + "," + game.getCurrentUserName() + "," + objective.getId());
        switch (playerObjective.getId()){
            case 54 -> {
                objectiveCard.building = new Building(54);
                spawnBuilding(objectiveCard);
                objectiveCardID = lastPlayedCardID - 1;
            }
            case 55 -> {
                objectiveCard.person = new ObjectivePerson(55);
                Image tempImage = addToPlayedCards(objectiveCard);
                objectiveCardID = lastPlayedCardID - 1;
                playerConn.sendString("playCard," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                        ",person," + objectiveCard.getPersonCard());
                tempImage.addListener(new ClickListener(){
                    public void clicked(InputEvent event, float x, float y){
                        if (isAttackActive) attack(objectiveCard, tempImage);
                        else startCardStage(objectiveCard, 600);
                    }
                });
            }
            case 56 -> {
                objectiveCard.person = new ObjectivePerson(56);
                spawnPeople(objectiveCard);
            }
        }
    }

    public void setNewEnemyObjective(int id){
        enemyObjective = new Objective(id);
        Card objectiveCard = getCardByID(enemyObjective.getId());
        enemyObjectiveImage = new Image(new Texture(Gdx.files.internal("Cards/inGame/" +
                objectiveCard.image_path)));
        enemyObjectiveImage.setPosition(25, 620);
        fillObjectiveStage(objectiveCard, enemyObjectiveImage, enemyObjective);
    }

    private void fillObjectiveStage(Card objectiveCard, Image tempImage, Objective enemyObjective) {
        stage.addActor(tempImage);
        tempImage.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startCardStage(objectiveCard, 900);
                Label tempLabel = new Label(objectiveCard.description, game.getMainLabelStyle());
                tempLabel.setWidth(1000 * game.xScaler);
                tempLabel.setWrap(true);
                tempLabel.setAlignment(Align.center);
                tempLabel.setPosition(475, 550);
                cardStage.addActor(tempLabel);

                tempLabel = new Label("Раундов осталось: " + enemyObjective.getDuration(), game.getMainLabelStyle());
                tempLabel.setWidth(500 * game.xScaler);
                tempLabel.setWrap(true);
                tempLabel.setAlignment(Align.center);
                tempLabel.setPosition(725, 250);
                cardStage.addActor(tempLabel);
            }
        });
    }

    public void checkObjective(){
        if (!playerObjective.equals(0)){
            switch (playerObjective.getId()){
                case 6 -> {
                    int peopleCounter = 0;
                    for (Card card:firstPlayerActiveCards.values())
                        if (card.type.equals("people")) peopleCounter++;
                    playerObjective = new Objective(0);
                    stage.getActors().removeValue(objectiveImage, true);
                    if (peopleCounter >= 3) successfulObjective("easy");
                    else failedObjective("easy");
                }
                case 54 -> {

                }
                case 55 -> {
                    if (firstPlayerActiveCards.containsValue(objectiveCard)) playerObjective.setDuration(playerObjective.getDuration() - 1);
                    else {
                        successfulObjective("easy");
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                    }
                    if (playerObjective.getDuration() == 0 && firstPlayerActiveCards.containsValue(objectiveCard)) {
                        firstPlayerActiveCards.remove(objectiveCardID);
                        firstPlayerField.removeActorAt(objectiveCardID, true);
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                        failedObjective("easy");
                    }
                }
                case 56 -> {
                    if (firstPlayerActiveCards.containsValue(objectiveCard)) playerObjective.setDuration(playerObjective.getDuration() - 1);
                    else {
                        failedObjective("hard");
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                    }
                    // добавить пункт про помещение в медблок
                    if (firstPlayerActiveCards.containsValue(objectiveCard) && playerObjective.getDuration() == 0){
                        successfulObjective("hard");
                        firstPlayerActiveCards.remove(objectiveCardID);
                        firstPlayerField.removeActorAt(objectiveCardID, true);
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                    }
                }
            }
            playerConn.sendString("updateEnemyVictoryPoints," + game.getCurrentGameID() + ","
                    + game.getCurrentUserName() + "," + victoryPoints + "," + "objectiveEnded");
        }
    }

    public void checkCardEffects(){
        for (Card card:firstPlayerActiveCards.values()){
            try {card.person.setFought(false);
            } catch (Exception ignored){}
        }
    }

    public void successfulObjective(String toughness){
        switch (toughness){
            case "easy" -> victoryPoints += 1;
            case "normal" -> victoryPoints += 2;
            case "hard" -> victoryPoints += 3;
        }
        victoryPointsFirstPlayer.setText("ПО: " + victoryPoints + "/5");
        completedObjectives++;
        useTurnLabel("Цель выполнена");
        if (victoryPoints >= 5 || completedObjectives >= 3) victory();
    }

    public void failedObjective(String toughness){
        switch (toughness){
            case "normal" -> victoryPoints -= 1;
            case "hard" -> victoryPoints -= 2;
        }
        victoryPointsFirstPlayer.setText("ПО: " + victoryPoints + "/5");
        useTurnLabel("Цель провалена");
        if (victoryPoints <= -3) lose();
    }

    public void updateEnemyVictoryPoints(int points, String objectiveEnd){
        victoryPointsSecondPlayer.setText("ПО: " + points + "/5");
        if (objectiveEnd.equals("objectiveEnded")) {
            enemyObjective = new Objective(0);
            stage.getActors().removeValue(enemyObjectiveImage, true);
        }
    }

    public void victory(){

    }
    public void lose(){

    }
    public void draw(){

    }
}

