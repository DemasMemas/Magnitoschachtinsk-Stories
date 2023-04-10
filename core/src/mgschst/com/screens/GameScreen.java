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
import mgschst.com.dbObj.Equipment.AdditionalEquipment;
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
import java.util.*;

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
    Boolean isCardStageActive = false, isAttackActive = false, isDefenceActive = false, repairActive = false, changeEquipActive = false;
    HorizontalGroup firstPlayerField = new HorizontalGroup(), secondPlayerField = new HorizontalGroup();
    HashMap<Integer, Card> firstPlayerActiveCards = new HashMap<>();
    HashMap<Integer, Card> secondPlayerActiveCards = new HashMap<>();
    static int lastPlayedCardID = 0;
    static int lastSecondPlayerPlayedCardID = 0;
    Label victoryPointsFirstPlayer, victoryPointsSecondPlayer;
    int victoryPoints = 0, completedObjectives = 0;
    VerticalGroup resourcesGroup = new VerticalGroup();
    int resourceMax = 5, soldCards = 0;
    Objective playerObjective = new Objective(0), enemyObjective = new Objective(0);
    Image enemyObjectiveImage, objectiveImage, objectiveCardImage;
    Card objectiveCard;
    int objectiveCardID;

    Card currentAttacker, currentDefender, currentEquip;
    Image attackerImage;

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
        firstPlayerField.center();
        firstPlayerField.space(10f);
        firstPlayerField.pad(10f);
        stage.addActor(firstPlayerField);
        stage.addActor(firstPlayerHand);
        secondPlayerHand.setPosition(700, 975);
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
        firstPlayerHand.setPosition(1000, 70);
        firstPlayerHand.space(-35f);
        firstPlayerHand.center();

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
        secondPlayerHand.center();

        secondPlayerField.setPosition(360, 595);
        secondPlayerField.setWidth(1270 * game.xScaler);
        secondPlayerField.setHeight(270 * game.xScaler);
        secondPlayerField.center();
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
        if (myTurn){
            playerConn.sendString("changeTurn," + game.getCurrentGameID());
            useTurnLabel("Ход игрока: " + secondPlayer.nickname);
            myTurn = false;
        }
    }

    public void takeTurn(){
        // взятие хода текущим игроком
        if (!myTurn) {
            useTurnLabel("Ход игрока: " + game.getCurrentUserName());
            myTurn = true;
        }
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
            // проверка на нахождение в игровой зоне (зона сдвинута на 1000/70 из-за либгыдыха)
            return tempImage.getX() + (tempImage.getWidth() / 2) < 635 * game.xScaler &&
                    tempImage.getX() + (tempImage.getWidth() / 2) > -640 * game.xScaler &&
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

        if (currentCard.type.equals("equip_weapon") || currentCard.type.equals("equip_armor") ||
                currentCard.type.equals("equip_helmet") || currentCard.type.equals("equip_heal")){
            TextButton sellButton = new TextButton("Продать карту (Продайте две, чтобы получить поддержку)", game.getTextButtonStyle());
            sellButton.setPosition(125, 150);
            sellButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    isCardStageActive = false;
                    Gdx.input.setInputProcessor(stage);
                    cardStage.dispose();
                    if (++soldCards == 2){
                        soldCards = 0;
                        addMoneyResource();
                    } else useTurnLabel("Продайте еще одну карту, чтобы получить поддержку");
                    playerConn.sendString("cardFromHand," + game.getCurrentGameID() + "," + game.getCurrentUserName());
                    firstPlayerHand.removeActor(tempImage);
                }
            });
            cardStage.addActor(sellButton);
        }

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
        if ((tempCard.type.equals("equip_weapon")||tempCard.type.equals("equip_helmet")||
                tempCard.type.equals("equip_armor")||tempCard.type.equals("equip_heal")||
                tempCard.type.equals("equip_add"))){
            EffectHandler.handEffect(36, tempCard, game);
        } else {
            if (tempCard.effects != null)
                for(String effect:tempCard.effects.split(","))
                    EffectHandler.handEffect(Integer.parseInt(effect), tempCard, game);
        }
        // убрать карту из руки
        firstPlayerHand.removeActor(tempImage);
        tempImage.getListeners().clear();
        endTurn();
    }

    public void changeEquip(Card tempCard){
        changeEquipActive = true;
        currentEquip = tempCard;
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

    public void addMoneyResource(){
        Image tempImage = new Image(new Texture(Gdx.files.internal("DeckAssets/ResIco/any.png")));
        tempImage.setName("any");
        resourcesGroup.addActor(tempImage);
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
                if (!repairActive && !changeEquipActive) fillPersonCardStage(card, tempImage);
                else {
                    Person tempPerson = card.person;
                    if (repairActive){
                        tempPerson.getHelmet().setDefence(tempPerson.getHelmet().getMaxDefence());
                        tempPerson.getArmor().setDefence(tempPerson.getArmor().getMaxDefence());
                        ArrayList<Integer> tempList = new ArrayList<>();
                        for (int i:tempPerson.getArmor().getEffect()) if (i != 12) tempList.add(i);
                        tempPerson.getArmor().setEffect(tempList.stream().mapToInt(i -> i).toArray());
                        tempList = new ArrayList<>();
                        for (int i:tempPerson.getHelmet().getEffect()) if (i != 12) tempList.add(i);
                        tempPerson.getHelmet().setEffect(tempList.stream().mapToInt(i -> i).toArray());
                        tempList = new ArrayList<>();
                        for (int i:tempPerson.getWeapon().getEffectList()) if (i != 12) tempList.add(i);
                        tempPerson.getWeapon().setEffectList(tempList.stream().mapToInt(i -> i).toArray());

                        if (tempPerson.getFirstAddEquip().getId() == 38)
                            tempPerson.setFirstAddEquip(new AdditionalEquipment(39, "Глушитель"));
                        if (tempPerson.getSecondAddEquip().getId() == 38)
                            tempPerson.setSecondAddEquip(new AdditionalEquipment(39, "Глушитель"));
                    } else {
                        switch (currentEquip.type){
                            case "equip_weapon" -> {
                                if (currentEquip.card_id == 52){
                                    // вернуть в руку оружие бойца
                                }
                                Weapon tempWeapon;
                                try { tempWeapon = new Weapon(currentEquip.card_id, currentEquip.attack,
                                        currentEquip.name, Arrays.stream(currentEquip.effects.split(",")).mapToInt(Integer::parseInt).toArray());
                                } catch (Exception e){
                                    tempWeapon = new Weapon(currentEquip.card_id, currentEquip.attack,
                                            currentEquip.name, new int[]{});
                                }
                                tempPerson.setWeapon(tempWeapon);
                            }
                            case "equip_helmet" -> {
                                Helmet tempHelmet;
                                try { tempHelmet = new Helmet(currentEquip.card_id, currentEquip.defence,
                                        currentEquip.name, Arrays.stream(currentEquip.effects.split(",")).mapToInt(Integer::parseInt).toArray());
                                } catch (Exception e){
                                    tempHelmet = new Helmet(currentEquip.card_id, currentEquip.defence,
                                            currentEquip.name, new int[]{});
                                }
                                tempPerson.setHelmet(tempHelmet);
                            }
                            case "equip_armor" -> {
                                Armor tempArmor;
                                try { tempArmor = new Armor(currentEquip.card_id, currentEquip.defence,
                                        currentEquip.name, Arrays.stream(currentEquip.effects.split(",")).mapToInt(Integer::parseInt).toArray());
                                } catch (Exception e){
                                    tempArmor = new Armor(currentEquip.card_id, currentEquip.defence,
                                            currentEquip.name, new int[]{});
                                }
                                tempPerson.setArmor(tempArmor);
                            }
                            case "equip_heal" -> {
                                // после статусов
                            }
                            case "equip_add" -> {
                                if (tempPerson.getFirstAddEquip().getId() == 0)
                                    tempPerson.setFirstAddEquip(new AdditionalEquipment(currentEquip.card_id, currentEquip.name));
                                else if (tempPerson.getSecondAddEquip().getId() == 0)
                                    tempPerson.setSecondAddEquip(new AdditionalEquipment(currentEquip.card_id, currentEquip.name));
                                else tempPerson.setFirstAddEquip(new AdditionalEquipment(currentEquip.card_id, currentEquip.name));
                            }
                        }
                    }
                    changeEquipActive = false;
                    repairActive = false;
                    card.person = tempPerson;
                    playerConn.sendString("changeEnemyPersonStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                            "," + tempImage.getName() + "," + card.getPersonCard());
                    endTurn();
                }
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
                if (isDefenceActive) defend(card, tempImage);
                else fillBuildingStage(card);
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

    public void fillBuildingStage(Card tempCard){
        startCardStage(tempCard, 800);
        Label cardDesc = new Label(tempCard.description, game.getMainLabelStyle());
        cardDesc.setPosition(475, 550);
        cardDesc.setWidth(1000 * game.xScaler);
        cardDesc.setWrap(true);
        cardDesc.setAlignment(Align.center);
        cardStage.addActor(cardDesc);
        if (tempCard.card_id == 5){
            TextButton repairButton = new TextButton("Починить снаряжение у бойца", game.getTextButtonStyle());
            repairButton.setPosition(600, 250);
            repairButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    isCardStageActive = false;
                    Gdx.input.setInputProcessor(stage);
                    if (!tempCard.building.isAlreadyUsed() && firstPlayerActiveCards.values().stream()
                            .anyMatch(x -> x.type.equals("people"))){
                        tempCard.building.setAlreadyUsed(true);
                        repairActive = true;
                    } else useTurnLabel("Уже использован или нет доступных людей");
                }
            });
            cardStage.addActor(repairButton);
        }
    }

    public void defend(Card tempCard, Image tempImage){
        if (currentDefender.person.isDefender()) changeDefender(currentDefender, firstPlayerActiveCards);
        Building tempBuilding = tempCard.building;
        tempBuilding.getDefenderList().add(currentDefender);
        sendDefendersUpdate(tempImage.getName(), tempBuilding, firstPlayerActiveCards);
        checkFiredUp(new Random(), currentDefender);
        currentDefender.person.setDefender(true);
        isDefenceActive = false;
        currentDefender = null;
        endTurn();
    }

    public void sendDefendersUpdate(String buildingID, Building tempBuilding, HashMap<Integer, Card> playerCards){
        StringBuilder defenders = new StringBuilder();
        for (int i:playerCards.keySet())
            if (tempBuilding.getDefenderList().contains(playerCards.get(i)))
                defenders.append(i).append(";");
        if (defenders.length() > 0) defenders.deleteCharAt(defenders.length() - 1);
        else defenders.append("000");
        playerConn.sendString("updateDefenders," + game.getCurrentGameID() + ","
                + game.getCurrentUserName() + "," + buildingID + "," + defenders + "," +
                (playerCards.equals(firstPlayerActiveCards) ? 1 : 2));
    }

    public void updateDefenders(int id, String info, int player){
        HashMap<Integer, Card> playerCards = player == 2 ? firstPlayerActiveCards : secondPlayerActiveCards;
        ArrayList<Card> newDefenderList = new ArrayList<>();
        ArrayList<Card> oldDefenderList = new ArrayList<>(playerCards.get(id).building.getDefenderList());
        if (!info.equals("000"))
            for (String i:info.split(";"))
                newDefenderList.add(playerCards.get(Integer.parseInt(i)));
        playerCards.get(id).building.setDefenderList(newDefenderList);
        for (Card card:newDefenderList) if (!oldDefenderList.contains(card)) card.person.setDefender(true);
        for (Card card:oldDefenderList) if (!newDefenderList.contains(card)) card.person.setDefender(false);
    }

    private void checkFiredUp(Random random, Card currentPerson) {
        try {
            if (!(firstPlayerActiveCards.containsValue(currentPerson) || secondPlayerActiveCards.containsValue(currentPerson))) return;
            if (!(Arrays.stream(currentPerson.person.getArmor().getEffect()).filter(x -> x == 8).findAny().isPresent() &&
                    random.nextBoolean())){
                boolean firedUp = true;
                for (Card card:firstPlayerActiveCards.values())
                    if (card.card_id == 48 && random.nextBoolean()){firedUp = false; break;}
                if (firedUp) currentPerson.person.setFought(true);
            }
            if (Arrays.stream(currentPerson.person.getArmor().getEffect()).filter(x -> x == 10).findAny().isPresent())
                currentPerson.person.setFought(true);
        } catch (NullPointerException e) {System.out.println("NullPointerException");}
    }

    public void placeEnemyCard(String cardType, String cardInfo){
        String[] info = cardInfo.split(" . ");
        Card tempCard = getCardByID(Integer.parseInt(info[0]));
        if (cardType.equals("people")){
            tempCard.person = createEnemyPerson(info);
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
    
    public Person createEnemyPerson(String[] info){
        Person tempPerson = new Person();

        String[] armorInfo = info[1].split(";");
        Armor tempArmor;
        try { tempArmor = new Armor(Integer.parseInt(armorInfo[0]), Integer.parseInt(armorInfo[1]),
                    armorInfo[2], Arrays.stream(armorInfo[3].split(":")).mapToInt(Integer::parseInt).toArray());
        } catch (Exception e){
            tempArmor = new Armor(Integer.parseInt(armorInfo[0]), Integer.parseInt(armorInfo[1]), armorInfo[2], new int[]{});
        }
        tempPerson.setArmor(tempArmor);

        String[] weaponInfo = info[2].split(";");
        Weapon tempWeapon;
        try { tempWeapon = new Weapon(Integer.parseInt(weaponInfo[0]), Integer.parseInt(weaponInfo[1]),
                weaponInfo[2], Arrays.stream(weaponInfo[3].split(":")).mapToInt(Integer::parseInt).toArray());
        } catch (Exception e){
            tempWeapon = new Weapon(Integer.parseInt(weaponInfo[0]), Integer.parseInt(weaponInfo[1]),
                    weaponInfo[2], new int[]{});
        }
        tempPerson.setWeapon(tempWeapon);

        String[] helmetInfo = info[3].split(";");
        Helmet tempHelmet;
        try { tempHelmet = new Helmet(Integer.parseInt(helmetInfo[0]), Integer.parseInt(helmetInfo[1]),
                helmetInfo[2], Arrays.stream(helmetInfo[3].split(":")).mapToInt(Integer::parseInt).toArray());
        } catch (Exception e){
            tempHelmet = new Helmet(Integer.parseInt(helmetInfo[0]), Integer.parseInt(helmetInfo[1]),
                    helmetInfo[2], new int[]{});
        }
        tempPerson.setHelmet(tempHelmet);

        String[] addEquipInfo = info[4].split(";");
        AdditionalEquipment tempFirstEquip;
        tempFirstEquip = new AdditionalEquipment(Integer.parseInt(addEquipInfo[0]), addEquipInfo[1]);
        tempPerson.setFirstAddEquip(tempFirstEquip);

        addEquipInfo = info[5].split(";");
        AdditionalEquipment tempSecondEquip;
        tempSecondEquip = new AdditionalEquipment(Integer.parseInt(addEquipInfo[0]), addEquipInfo[1]);
        tempPerson.setSecondAddEquip(tempSecondEquip);

        tempPerson.setHealth(info[6].equals("1"));

        return tempPerson;
    }

    public void changeDefender(Card personCard, HashMap<Integer, Card> playerCards){
        try {
            String buildingID = ""; Building tempBuilding = new Building(0);
            for (int i:playerCards.keySet()) {
                try {
                    if (playerCards.get(i).building.getDefenderList().contains(personCard)) {
                        buildingID = String.valueOf(i);
                        tempBuilding = playerCards.get(i).building;
                        break;
                    }
                } catch (Exception ignored) {}
            }
            playerCards.values().stream().filter(card -> card.type.equals("building")
                    || card.type.equals("objective")).forEach(building -> building.building.getDefenderList()
                    .removeIf(defender -> defender.equals(personCard)));
            sendDefendersUpdate(buildingID, tempBuilding, playerCards);
        } catch (Exception ignored){}
    }

    public void attack(Card tempCard, Image tempImage){
        if (currentAttacker.person.isDefender()){
            changeDefender(currentAttacker, firstPlayerActiveCards);
            currentAttacker.person.setDefender(false);
        }
        Random random = new Random();
        if (tempCard.type.equals("people")) attackPeople(tempCard, tempImage, random);
        else {
            if (tempCard.type.equals("building")) attackBuilding(tempCard, tempImage);
            else if (tempCard.type.equals("objective")){
                if (tempCard.card_id == 54) attackBuilding(tempCard, tempImage);
                else attackPeople(tempCard, tempImage, random);
            }
        }

        checkFiredUp(random, currentAttacker);
        attackerImage = null;
        isAttackActive = false;
        currentAttacker = null;
        endTurn();
    }
    public void attackBuilding(Card tempCard, Image tempImage){
        try {
            Building tempBuilding = tempCard.building;
            if (tempBuilding.getDefenderList().size() > 0){
                for (Card card:tempBuilding.getDefenderList()){
                    for (int i:secondPlayerActiveCards.keySet()){
                        if (secondPlayerActiveCards.get(i).equals(card)){
                            Image defenderImage = (Image) Arrays.stream(secondPlayerField.getChildren().items)
                                    .filter(x -> x.getName().equals(String.valueOf(i))).findFirst().get();
                            attackPeople(card, defenderImage, new Random());
                        }
                        if (!firstPlayerActiveCards.containsValue(currentAttacker)) return;
                    }
                }
            } else changeBuildingHealthStatus(tempCard, tempImage);
        } catch (Exception ignored){
            if (tempCard.building.getDefenderList().size() == 0)
                changeBuildingHealthStatus(tempCard, tempImage);
        }
    }
    public void changeBuildingHealthStatus(Card tempCard, Image tempImage){
        if (tempCard.building.isHealth()) tempCard.building.setHealth(false);
        else killEnemy(tempImage);
    }
    public void attackPeople(Card tempCard, Image tempImage, Random random){
        if (!tempCard.person.isDefender() && !(currentAttacker.person.getWeapon().getId() == 51)
                && !(currentAttacker.person.getWeapon().getId() == 52)){
            // атаковать
            boolean noCounterAttack = addEquipCheck(tempCard, tempImage, random);
            attackEnemy(tempCard, tempImage, random, currentAttacker.person.getWeapon());

            if ((secondPlayerField.getChildren().contains(tempImage, true) ||
                    firstPlayerField.getChildren().contains(tempImage, true)) && noCounterAttack){
                // обратная атака
                attackEnemy(currentAttacker, attackerImage, random, tempCard.person.getWeapon());
            }
        } else {
            // обратная атака
            attackEnemy(currentAttacker, attackerImage, random, tempCard.person.getWeapon());
            if (secondPlayerField.getChildren().contains(attackerImage, true) ||
                    firstPlayerField.getChildren().contains(attackerImage, true)){
                // атаковать
                attackEnemy(tempCard, tempImage, random, currentAttacker.person.getWeapon());
            }
        }
    }

    public boolean addEquipCheck(Card tempCard, Image tempImage, Random random){
        boolean noCounterAttack = true;
        if (currentAttacker.person.getFirstAddEquip().getId() == 39 ||
                (currentAttacker.person.getFirstAddEquip().getId() == 38 && random.nextBoolean())
                || currentAttacker.person.getFirstAddEquip().getId() == 41
                || currentAttacker.person.getFirstAddEquip().getId() == 42){
            noCounterAttack = false;
            if (currentAttacker.person.getFirstAddEquip().getId() == 41
                    || currentAttacker.person.getFirstAddEquip().getId() == 42){
                if (currentAttacker.person.getFirstAddEquip().getId() == 41){
                    if (tempCard.person.isHealth() && random.nextBoolean()) tempCard.person.setHealth(false);
                    else killEnemy(tempImage);
                }
                currentAttacker.person.setFirstAddEquip(new AdditionalEquipment(0, "Нет снаряжения"));
            }
        }else {
            if (currentAttacker.person.getSecondAddEquip().getId() == 39 ||
                    (currentAttacker.person.getSecondAddEquip().getId() == 38 && random.nextBoolean())
                    || currentAttacker.person.getSecondAddEquip().getId() == 41
                    || currentAttacker.person.getSecondAddEquip().getId() == 42){
                noCounterAttack = false;
                if (currentAttacker.person.getSecondAddEquip().getId() == 41
                        || currentAttacker.person.getSecondAddEquip().getId() == 42){
                    if (currentAttacker.person.getSecondAddEquip().getId() == 41){
                        if (tempCard.person.isHealth() && random.nextBoolean()) tempCard.person.setHealth(false);
                        else killEnemy(tempImage);
                    }
                    currentAttacker.person.setSecondAddEquip(new AdditionalEquipment(0, "Нет снаряжения"));
                }
            }
        }
        return noCounterAttack;
    }
    public void attackEnemy(Card tempCard, Image tempImage, Random random, Weapon attackerWeapon){
        boolean broken = false;
        ArrayList<Integer> effectList = new ArrayList<>();
        try {
            for(int effectNumb:attackerWeapon.getEffectList())
                effectList.add(effectNumb);
            if (effectList.contains(12)){ effectList.remove((Integer) 12);
                if (random.nextInt(101) >= 75) broken = true;}
        } catch (Exception ignored){}
        if (broken){
            if (checkOnAttackerShield(tempCard)) return;
            if (random.nextInt(101) >= 75) attackHead(tempCard, attackerWeapon, tempImage);
            else attackArmor(tempCard, attackerWeapon, tempImage);
        } else {
            int attackCounter = 1;
            if (effectList.contains(13)) attackCounter += 1;
            boolean hitToHead = effectList.contains(7) && random.nextInt(101) >= 25;
            boolean instantKill = effectList.contains(11);

            for (int i = 0; i<attackCounter;i++) {
                if (firstPlayerActiveCards.containsValue(tempCard) || secondPlayerActiveCards.containsValue(tempCard)) {
                    if (checkOnAttackerShield(tempCard)) return;
                    if (!instantKill) {
                        if (hitToHead || random.nextInt(101) >= 75) attackHead(tempCard, attackerWeapon, tempImage);
                        else attackArmor(tempCard, attackerWeapon, tempImage);
                    } else {
                        if (hitToHead || random.nextInt(101) >= 75) {
                            if (attackerWeapon.getAttack() > tempCard.person.getHelmet().getDefence())
                                killEnemy(tempImage);
                            else attackHead(tempCard, attackerWeapon, tempImage);
                        } else {
                            if (attackerWeapon.getAttack() > tempCard.person.getArmor().getDefence())
                                killEnemy(tempImage);
                            else attackArmor(tempCard, attackerWeapon, tempImage);
                        }
                    }
                } else {break;}
            }
        }
    }

    private boolean checkOnAttackerShield(Card tempCard) {
        if (tempCard.equals(currentAttacker) && (currentAttacker.person.getFirstAddEquip().getId() == 40
                || currentAttacker.person.getSecondAddEquip().getId() == 40)){
            if (currentAttacker.person.getFirstAddEquip().getId() == 40)
                currentAttacker.person.setFirstAddEquip(new AdditionalEquipment(0, "Нет снаряжения"));
            else currentAttacker.person.setSecondAddEquip(new AdditionalEquipment(0, "Нет снаряжения"));
            return true;
        }
        return false;
    }

    public void attackArmor(Card tempCard, Weapon attackerWeapon, Image tempImage){
        Armor defenderArmor = tempCard.person.getArmor();
        if (tempCard.person.isDefender())
            defenderArmor.setDefence(defenderArmor.getDefence() + tempCard.defence);
        if (defenderArmor.getDefence() >= attackerWeapon.getAttack() + currentAttacker.attack){
            defenderArmor.setDefence(defenderArmor.getDefence() - attackerWeapon.getAttack());
            if (defenderArmor.getDefence() >= tempCard.defence && tempCard.person.isDefender())
                defenderArmor.setDefence(defenderArmor.getDefence() - tempCard.defence);
            else defenderArmor.setDefence(0);
        }
        else {
            defenderArmor.setDefence(0);
            if (tempCard.person.isHealth()) tempCard.person.setHealth(false);
            else {killEnemy(tempImage);return;}
        }
        tempCard.person.setArmor(defenderArmor);
        if (firstPlayerActiveCards.containsValue(tempCard))
            playerConn.sendString("changeEnemyPersonStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                    "," + tempImage.getName() + "," + tempCard.getPersonCard());
        else if (secondPlayerActiveCards.containsValue(tempCard))
            playerConn.sendString("changeAllyPersonStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                "," + tempImage.getName() + "," + tempCard.getPersonCard());
    }
    public void attackHead(Card tempCard, Weapon attackerWeapon, Image tempImage){
        Helmet defenderHelmet = tempCard.person.getHelmet();
        if (tempCard.person.isDefender())
            defenderHelmet.setDefence(defenderHelmet.getDefence() + tempCard.defence);
        if (defenderHelmet.getDefence() >= attackerWeapon.getAttack() + currentAttacker.attack){
            defenderHelmet.setDefence(defenderHelmet.getDefence() - attackerWeapon.getAttack());
            if (defenderHelmet.getDefence() >= tempCard.defence && tempCard.person.isDefender())
                defenderHelmet.setDefence(defenderHelmet.getDefence() - tempCard.defence);
            else defenderHelmet.setDefence(0);
        }
        else {
            defenderHelmet.setDefence(0);
            if (tempCard.person.isHealth()) tempCard.person.setHealth(false);
            else {killEnemy(tempImage);return;}
        }
        tempCard.person.setHelmet(defenderHelmet);
        if (firstPlayerActiveCards.containsValue(tempCard))
            playerConn.sendString("changeEnemyPersonStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                    "," + tempImage.getName() + "," + tempCard.getPersonCard());
        else if (secondPlayerActiveCards.containsValue(tempCard))
            playerConn.sendString("changeAllyPersonStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                "," + tempImage.getName() + "," + tempCard.getPersonCard());
    }

    public void killEnemy(Image tempImage){
        if (secondPlayerField.getChildren().contains(tempImage,true)){
            killSomeone(tempImage, secondPlayerActiveCards, secondPlayerField);
            playerConn.sendString("removeKilledAlly," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                    "," + tempImage.getName());
        } else if (firstPlayerField.getChildren().contains(tempImage,true)){
            killSomeone(tempImage, firstPlayerActiveCards, firstPlayerField);
            playerConn.sendString("removeKilledEnemy," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                    "," + tempImage.getName());
        }
    }

    private void killSomeone(Image tempImage, HashMap<Integer, Card> playerActiveCards, HorizontalGroup playerField) {
        try {
            if (playerActiveCards.get(Integer.parseInt(tempImage.getName())).person.isDefender()){
                for (Card card: playerActiveCards.values()){
                    try {
                        if (card.building.getDefenderList().contains
                                (playerActiveCards.get(Integer.parseInt(tempImage.getName()))))
                            changeDefender(playerActiveCards.get(Integer.parseInt(tempImage.getName())), playerActiveCards);
                    } catch (Exception ignored){}
                }
            }
        } catch (Exception ignored){}
        playerActiveCards.remove(Integer.parseInt(tempImage.getName()));
        playerField.removeActor(tempImage);
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
        isAttackActive = false; isDefenceActive = false; currentDefender = null; currentAttacker = null;
        repairActive = false; changeEquipActive = false; currentEquip = null;
    }

    public void fillPersonCardStage(Card card, Image tempImage){
        startCardStage(card, 900);

        TextButton attackButton = new TextButton("Атаковать", game.getTextButtonStyle());
        attackButton.setPosition(675, 200);
        attackButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isCardStageActive = false;
                Gdx.input.setInputProcessor(stage);
                if (myTurn && !secondPlayerActiveCards.isEmpty() && card.person.getFoughtStatus().equals("0")){
                    isAttackActive = true;
                    isDefenceActive = false;
                    attackerImage = tempImage;
                    currentAttacker = card;
                }
            }
        });
        cardStage.addActor(attackButton);

        TextButton defendButton = new TextButton("Оборонять", game.getTextButtonStyle());
        defendButton.setPosition(1025, 200);
        defendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isCardStageActive = false;
                Gdx.input.setInputProcessor(stage);
                if (myTurn && firstPlayerActiveCards.values().stream().anyMatch(x -> x.type.equals("building")
                        || x.type.equals("objective")) && card.person.getFoughtStatus().equals("0")){
                    isDefenceActive = true;
                    isAttackActive = false;
                    currentDefender = card;
                }
            }
        });
        cardStage.addActor(defendButton);

        // кнопка для ухода в медблок
        TextButton medBayButton = new TextButton("Уйти в медблок", game.getTextButtonStyle());
        medBayButton.setPosition(800, 150);
        medBayButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

            }
        });
        cardStage.addActor(medBayButton);

        VerticalGroup gameGroup;
        Table gameContainerTable;
        ScrollPane gameScrollPane;
        gameGroup = new VerticalGroup();
        gameGroup.pad(30f);

        // надписи о вооружении бойца
        Label weaponLabel = new Label("Оружие: " + card.person.getWeaponString().split(";")[2] +
                " Урон: " + card.person.getWeaponString().split(";")[1], game.getMainLabelStyle());
        weaponLabel.setColor(Color.GOLDENROD);
        gameGroup.addActor(weaponLabel);

        if (card.person.getWeaponString().split(";").length > 3){
            Label weaponEffectLabel = new Label("Эффекты: ", game.getMainLabelStyle());
            StringBuilder effects = new StringBuilder();
            for (String effect:card.person.getWeaponString().split(";")[3].split(":"))
                effects.append(EffectHandler.effectMap.get(Integer.parseInt(effect))).append(", ");
            effects.delete(effects.length() - 2, effects.length() - 1);
            weaponEffectLabel.setText("Эффекты: " + effects);
            gameGroup.addActor(weaponEffectLabel);
        }

        // надписи о броне бойца
        Label armorLabel = new Label("Броня: " + card.person.getArmorString().split(";")[2] +
                " Защита: " + card.person.getArmorString().split(";")[1], game.getMainLabelStyle());
        armorLabel.setColor(Color.GOLDENROD);
        gameGroup.addActor(armorLabel);

        if (card.person.getArmorString().split(";").length > 3){
            Label armorEffectLabel = new Label("Эффекты: ", game.getMainLabelStyle());
            StringBuilder effects = new StringBuilder();
            for (String effect:card.person.getArmorString().split(";")[3].split(":"))
                effects.append(EffectHandler.effectMap.get(Integer.parseInt(effect))).append(", ");
            effects.delete(effects.length() - 2, effects.length() - 1);
            armorEffectLabel.setText("Эффекты: " + effects);
            gameGroup.addActor(armorEffectLabel);
        }

        // надписи о шлеме бойца
        Label helmetLabel = new Label("Шлем: " + card.person.getHelmetString().split(";")[2] +
                " Защита: " + card.person.getHelmetString().split(";")[1], game.getMainLabelStyle());
        helmetLabel.setColor(Color.GOLDENROD);
        gameGroup.addActor(helmetLabel);

        if (card.person.getHelmetString().split(";").length > 3){
            Label helmetEffectLabel = new Label("Эффекты: ", game.getMainLabelStyle());
            StringBuilder effects = new StringBuilder();
            for (String effect:card.person.getHelmetString().split(";")[3].split(":"))
                effects.append(EffectHandler.effectMap.get(Integer.parseInt(effect))).append(", ");
            effects.delete(effects.length() - 2, effects.length() - 1);
            helmetEffectLabel.setText("Эффекты: " + effects);
            gameGroup.addActor(helmetEffectLabel);
        }

        // надписи о доп снаряжении бойца
        Label firstAddEquipLabel = new Label("Снаряжение: " +
                card.person.firstEquipString().split(";")[1], game.getMainLabelStyle());
        firstAddEquipLabel.setColor(Color.GOLDENROD);
        gameGroup.addActor(firstAddEquipLabel);

        if (Integer.parseInt(card.person.firstEquipString().split(";")[0]) != 0){
            Label equipEffectLabel = new Label("Описание: " +
                    getCardByID(Integer.parseInt(card.person.firstEquipString().split(";")[0])).description,
                    game.getMainLabelStyle());
            gameGroup.addActor(equipEffectLabel);
        }

        Label secondAddEquipLabel = new Label("Снаряжение: " +
                card.person.secondEquipString().split(";")[1], game.getMainLabelStyle());
        secondAddEquipLabel.setColor(Color.GOLDENROD);
        gameGroup.addActor(secondAddEquipLabel);

        if (Integer.parseInt(card.person.secondEquipString().split(";")[0]) != 0){
            Label equipEffectLabel = new Label("Описание: " +
                    getCardByID(Integer.parseInt(card.person.secondEquipString().split(";")[0])).description,
                    game.getMainLabelStyle());
            equipEffectLabel.setWrap(true);
            gameGroup.addActor(equipEffectLabel);
        }

        for(Actor actor:gameGroup.getChildren()) {
            ((Label) actor).setAlignment(Align.center);
            actor.setWidth(1000 * game.xScaler);
        }
        gameScrollPane = new ScrollPane(gameGroup, neonSkin);
        gameScrollPane.setOverscroll(false, true);
        gameScrollPane.setScrollingDisabled(true, false);
        gameContainerTable = new Table();
        gameContainerTable.add(gameScrollPane);
        gameContainerTable.setPosition(475 * game.xScaler, 375 * game.yScaler);
        gameContainerTable.setSize(1000 * game.xScaler, 600 * game.yScaler);
        cardStage.addActor(gameContainerTable);

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
                objectiveCardImage = addToPlayedCards(objectiveCard);
                playerConn.sendString("playCard," + game.getCurrentGameID() + "," + game.getCurrentUserName() + "," +
                        "building," + objectiveCard.getBuildingCard());
                objectiveCardImage.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y){
                        if (isDefenceActive) defend(objectiveCard, objectiveCardImage);
                        else fillBuildingStage(objectiveCard);
                    }
                });
                objectiveCardID = lastPlayedCardID - 1;
            }
            case 55 -> {
                objectiveCard.person = new ObjectivePerson(55);
                objectiveCardImage = addToPlayedCards(objectiveCard);
                objectiveCardID = lastPlayedCardID - 1;
                playerConn.sendString("playCard," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                        ",person," + objectiveCard.getPersonCard());
                objectiveCardImage.addListener(new ClickListener(){
                    public void clicked(InputEvent event, float x, float y){
                        if (isAttackActive) attack(objectiveCard, objectiveCardImage);
                        else startCardStage(objectiveCard, 600);
                    }
                });
            }
            case 56 -> {
                objectiveCard.person = new ObjectivePerson(56);
                objectiveCardImage = addToPlayedCards(objectiveCard);
                playerConn.sendString("playCard," + game.getCurrentGameID() + "," + game.getCurrentUserName() + "," +
                        "person," + objectiveCard.getPersonCard());
                objectiveCardImage.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y){
                        fillPersonCardStage(objectiveCard, objectiveCardImage);
                    }
                });
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

    private void fillObjectiveStage(Card objectiveCard, Image tempImage, Objective objective) {
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

                tempLabel = new Label("Раундов осталось: " + objective.getDuration(), game.getMainLabelStyle());
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
            boolean objectiveEnded = false;
            switch (playerObjective.getId()){
                case 6 -> {
                    int peopleCounter = 0;
                    for (Card card:firstPlayerActiveCards.values())
                        if (card.type.equals("people")) peopleCounter++;
                    playerObjective = new Objective(0);
                    stage.getActors().removeValue(objectiveImage, true);
                    if (peopleCounter >= 3) successfulObjective("easy");
                    else failedObjective("easy");
                    objectiveEnded = true;
                }
                case 54 -> {
                    if (objectiveCard.building.getDefenderList().size() >= 3)
                        objectiveEnded = endOfExpensiveCaseObjective();
                    if (objectiveCard.building.getDefenderList().size() >= 1) {
                        playerObjective.setDuration(playerObjective.getDuration() - 1);
                        if (playerObjective.getDuration() == 0)
                            objectiveEnded = endOfExpensiveCaseObjective();
                    }
                    if (objectiveCard.building.getDefenderList().size() == 0) playerObjective.setDuration(2);
                    if (!firstPlayerField.getChildren().contains(objectiveCardImage, true)){
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                        failedObjective("normal");
                        objectiveEnded = true;
                    }
                }
                case 55 -> {
                    if (firstPlayerActiveCards.containsValue(objectiveCard))
                        playerObjective.setDuration(playerObjective.getDuration() - 1);
                    else {
                        successfulObjective("easy");
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                        objectiveEnded = true;
                    }
                    if (playerObjective.getDuration() == 0 && firstPlayerActiveCards.containsValue(objectiveCard)) {
                        killEnemy(objectiveCardImage);
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                        failedObjective("easy");
                        objectiveEnded = true;
                    }
                }
                case 56 -> {
                    if (firstPlayerActiveCards.containsValue(objectiveCard))
                        playerObjective.setDuration(playerObjective.getDuration() - 1);
                    else {
                        failedObjective("hard");
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                        objectiveEnded = true;
                    }
                    // медблок
                    for (Card card:firstPlayerActiveCards.values())
                        if (card.card_id == 50 && card.building.getDefenderList().contains(objectiveCard)){
                            playerObjective.setDuration(playerObjective.getDuration() - 1);
                            break;
                        }

                    if (firstPlayerActiveCards.containsValue(objectiveCard) && playerObjective.getDuration() <= 0){
                        successfulObjective("hard");
                        killEnemy(objectiveCardImage);
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                        objectiveEnded = true;
                    }
                }
            }
            if (objectiveEnded) playerConn.sendString("updateEnemyVictoryPoints," + game.getCurrentGameID() + ","
                    + game.getCurrentUserName() + "," + victoryPoints + "," + "objectiveEnded");
            else playerConn.sendString("updateEnemyVictoryPoints," + game.getCurrentGameID() + ","
                    + game.getCurrentUserName() + "," + victoryPoints + "," + "objectiveNotEnded");
            playerConn.sendString("updateEnemyObjectiveDuration," + game.getCurrentGameID() + ","
                    + game.getCurrentUserName() + "," + playerObjective.getDuration());
        }
    }

    private boolean endOfExpensiveCaseObjective() {
        for (Card card : objectiveCard.building.getDefenderList())
            card.person.setDefender(false);
        successfulObjective("normal");
        killEnemy(objectiveCardImage);
        playerObjective = new Objective(0);
        stage.getActors().removeValue(objectiveImage, true);
        return true;
    }

    public void checkCardEffects(){
        for (Card card:firstPlayerActiveCards.values()){
            try {card.person.setFought(false);
            } catch (Exception ignored){}
            try {card.building.setAlreadyUsed(false);
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

    public void removeKilledAlly(int id){
        try{
            firstPlayerActiveCards.remove(id);
            firstPlayerField.removeActor(Arrays.stream(firstPlayerField.getChildren().toArray())
                    .filter(x -> x.getName().equals(String.valueOf(id))).findFirst().get());
        } catch (NoSuchElementException e){System.out.println("No such element error");}

    }
    public void removeKilledEnemy(int id){
        try{
            secondPlayerActiveCards.remove(id);
            secondPlayerField.removeActor(Arrays.stream(secondPlayerField.getChildren().toArray())
                .filter(x -> x.getName().equals(String.valueOf(id))).findFirst().get());
        } catch (NoSuchElementException e){System.out.println("No such element error");}
    }
    public void updateEnemyObjectiveDuration(int duration){
        enemyObjective.setDuration(duration);
    }
    public void changeEnemyPersonStatus(int id, String info){
        String[] splittedInfo = info.split(" . ");
        Card tempCard = secondPlayerActiveCards.get(id);
        tempCard.person = createEnemyPerson(splittedInfo);
        secondPlayerActiveCards.replace(id, tempCard);
    }
    public void changeAllyPersonStatus(int id, String info){
        String[] splittedInfo = info.split(" . ");
        Card tempCard = firstPlayerActiveCards.get(id);
        tempCard.person = createEnemyPerson(splittedInfo);
        firstPlayerActiveCards.replace(id, tempCard);
    }
    public void victory(){
        playerConn.sendString("endGame," + game.getCurrentGameID());
    }
    public void lose(){
        playerConn.sendString("endGame," + game.getCurrentGameID());
    }
    public void draw(){
        playerConn.sendString("endGame," + game.getCurrentGameID());
    }
}

