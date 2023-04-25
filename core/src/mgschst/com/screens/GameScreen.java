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
import mgschst.com.dbObj.Equipment.*;
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
    Boolean isCardStageActive = false, isAttackActive = false, isDefenceActive = false, repairActive = false,
            changeEquipActive = false, performingPersonAction = false, performingBuildingAction = false;
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

    Card currentAttacker, currentDefender, currentEquip, currentAction;
    Image attackerImage;

    public int medBaySize = 1; int currentPeopleCountInMedBay = 0;
    int baseRaidCardCounter = 0;

    Stage statusStage = new Stage();
    boolean isStatusStageActive = false;

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
        if (isStatusStageActive){
            statusStage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 60f));
            statusStage.draw();
        }
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

        playerConn.sendString("takeCardWithType," + game.getCurrentGameID() + "," +
                game.getCurrentUserName() + ",objective");
        playerConn.sendString("takeCardWithType," + game.getCurrentGameID() + "," +
                game.getCurrentUserName() + ",objective");
        playerConn.sendString("takeCardWithType," + game.getCurrentGameID() + "," +
                game.getCurrentUserName() + ",people");
        playerConn.sendString("takeCardWithType," + game.getCurrentGameID() + "," +
                game.getCurrentUserName() + ",people");
        playerConn.sendString("takeCardWithType," + game.getCurrentGameID() + "," +
                game.getCurrentUserName() + ",building");
        playerConn.sendString("takeCard," + game.getCurrentGameID() + "," + game.getCurrentUserName());
        playerConn.sendString("takeCard," + game.getCurrentGameID() + "," + game.getCurrentUserName());

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

        // Проверка на наличие людей на поле
        if (((tempCard.type.equals("equip_weapon")||tempCard.type.equals("equip_helmet")||
                tempCard.type.equals("equip_armor")||tempCard.type.equals("equip_heal")||
                tempCard.type.equals("equip_add")) || (tempCard.type.equals("action") && tempCard.card_id != 20))&&
                firstPlayerActiveCards.values().stream().noneMatch(x -> x.type.equals("people"))){
            useTurnLabel("Нет активных людей для использования");return false;}

        // Проверка на наличие достаточного количества людей на поле
        if (tempCard.card_id == 46 && firstPlayerActiveCards.values().stream().filter(x -> x.type.equals("people")
                && x.person.getFoughtStatus().equals("0")).count() < 2){
            useTurnLabel("Нет активных людей для использования");return false;}

        // Проверка на наличие зданий на поле
        if (tempCard.card_id == 20 &&
                firstPlayerActiveCards.values().stream().noneMatch(x -> x.type.equals("building"))){
            useTurnLabel("Нет активных зданий для использования");return false;}

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
                currentCard.type.equals("equip_helmet") || currentCard.type.equals("equip_heal")
        || currentCard.type.equals("equip_add")){
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
        // убрать карту из руки
        firstPlayerHand.removeActor(tempImage);
        tempImage.getListeners().clear();
        if ((tempCard.type.equals("equip_weapon")||tempCard.type.equals("equip_helmet")||
                tempCard.type.equals("equip_armor")||tempCard.type.equals("equip_heal")||
                tempCard.type.equals("equip_add"))){
            changeEquip(tempCard);
            return;
        } else if (tempCard.type.equals("action")){
            performAction(tempCard);
            return;
        }
        else {
            if (tempCard.effects != null)
                for(String effect:tempCard.effects.split(","))
                    EffectHandler.handEffect(Integer.parseInt(effect), tempCard, game);
        }
        endTurn();
    }

    public void performAction(Card tempCard){
        if (tempCard.card_id != 20) performingPersonAction = true;
        else performingBuildingAction = true;
        currentAction = tempCard;
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
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                if (card.person.getStatuses().contains("1") || card.person.isDefender() || !card.person.isNotWounded()
                || card.person.getFoughtStatus().equals("1")){
                    statusStage = new Stage();
                    Image bg = new Image(new Texture(Gdx.files.internal("Cards/statusIcons/status_bg.png")));
                    bg.setPosition(360, 475);
                    statusStage.addActor(bg);

                    Table statusTable = new Table();
                    statusTable.row();
                    statusTable.setPosition(360, 475);
                    statusTable.center();
                    statusTable.setBounds(380, 495, 560, 360);
                    if (card.person.isInMedBay()){
                        statusTable.add(new Label("В медблоке ", game.getNormalLabelStyle()));
                        statusTable.add(new Image(new Texture(Gdx.files.internal("Cards/statusIcons/medbay.png"))));
                        statusTable.row();
                    }
                    if (!card.person.isNotWounded()){
                        statusTable.add(new Label("Ранен ", game.getNormalLabelStyle()));
                        statusTable.add(new Image(new Texture(Gdx.files.internal("Cards/statusIcons/wounded.png"))));
                        statusTable.row();
                    }
                    if (card.person.getFoughtStatus().equals("1")){
                        statusTable.add(new Label("Отстрелялся ", game.getNormalLabelStyle()));
                        statusTable.add(new Image(new Texture(Gdx.files.internal("Cards/statusIcons/exhaust.png"))));
                        statusTable.row();
                    }
                    if (card.person.isDefender()){
                        statusTable.add(new Label("В обороне ", game.getNormalLabelStyle()));
                        statusTable.add(new Image(new Texture(Gdx.files.internal("Cards/statusIcons/defender.png"))));
                        statusTable.row();
                    }
                    if (card.person.isBleeding()){
                        statusTable.add(new Label("Кровоточит ", game.getNormalLabelStyle()));
                        statusTable.add(new Image(new Texture(Gdx.files.internal("Cards/statusIcons/bleeding.png"))));
                        statusTable.row();
                    }
                    if (!card.person.isNotFractured()){
                        statusTable.add(new Label("Перелом ", game.getNormalLabelStyle()));
                        statusTable.add(new Image(new Texture(Gdx.files.internal("Cards/statusIcons/fractured.png"))));
                        statusTable.row();
                    }
                    if (card.person.isOnPainkillers()){
                        statusTable.add(new Label("Принял обезболивающее ", game.getNormalLabelStyle()));
                        statusTable.add(new Image(new Texture(Gdx.files.internal("Cards/statusIcons/painKillers.png"))));
                        statusTable.row();
                    }
                    if (card.person.isHitInHead()){
                        statusTable.add(new Label("Прицелился в голову ", game.getNormalLabelStyle()));
                        statusTable.add(new Image(new Texture(Gdx.files.internal("Cards/statusIcons/hitInHead.png"))));
                        statusTable.row();
                    }
                    if (card.person.isDoingAnAmbush()){
                        statusTable.add(new Label("Находится в засаде ", game.getNormalLabelStyle()));
                        statusTable.add(new Image(new Texture(Gdx.files.internal("Cards/statusIcons/ambush.png"))));
                        statusTable.row();
                    }
                    statusStage.addActor(statusTable);

                    isStatusStageActive = true;
                }
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor){
                isStatusStageActive = false;
            }
            @Override
            public void clicked(InputEvent event, float x, float y){
                if (!repairActive && !changeEquipActive && !performingPersonAction) fillPersonCardStage(card, tempImage);
                else {
                    Person tempPerson = card.person;
                    if (repairActive){
                        tempPerson.getHelmet().setDefence(tempPerson.getHelmet().getMaxDefence());
                        tempPerson.getArmor().setDefence(tempPerson.getArmor().getMaxDefence());
                        ArrayList<Integer> tempList = new ArrayList<>();
                        for (int i:tempPerson.getArmor().getEffect()) if (i != 12) tempList.add(i);
                        tempPerson.getArmor().setEffect(tempList.stream().mapToInt(i -> i).toArray());
                        tempList.clear();
                        for (int i:tempPerson.getHelmet().getEffect()) if (i != 12) tempList.add(i);
                        tempPerson.getHelmet().setEffect(tempList.stream().mapToInt(i -> i).toArray());
                        tempList.clear();
                        for (int i:tempPerson.getWeapon().getEffectList()) if (i != 12) tempList.add(i);
                        tempPerson.getWeapon().setEffectList(tempList.stream().mapToInt(i -> i).toArray());

                        if (tempPerson.getFirstAddEquip().getId() == 38)
                            tempPerson.setFirstAddEquip(new AdditionalEquipment(39, "Глушитель"));
                        if (tempPerson.getSecondAddEquip().getId() == 38)
                            tempPerson.setSecondAddEquip(new AdditionalEquipment(39, "Глушитель"));
                    } else if (changeEquipActive){
                        switch (currentEquip.type){
                            case "equip_weapon" -> {
                                if (currentEquip.card_id == 52 || currentEquip.card_id == 51)
                                    takeCardNotFromDeck(tempPerson.getWeapon().getId());
                                if (tempPerson.getHelmet().getId() == 29 && (currentEquip.card_id == 30 ||
                                        currentEquip.card_id == 31)) {
                                    useTurnLabel("Оружие не совместимо с шлемом бойца (Спецназ)");
                                    return;
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
                                if (currentEquip.card_id == 29 && (tempPerson.getWeapon().getId() == 30 ||
                                        tempPerson.getWeapon().getId() == 31)) {
                                    useTurnLabel("Шлем не совместим с оружем бойца (Снайперское)");
                                    return;
                                }
                                tempPerson.setHelmet(setNewProtectionEquip());
                            }
                            case "equip_armor" -> tempPerson.setArmor(setNewProtectionEquip());
                            case "equip_heal" -> {
                                switch (currentEquip.card_id){
                                    case 21 -> tempPerson.setBleeding(false);
                                    case 22 -> {
                                        if (tempPerson.isNotFractured()) tempPerson.setHealth(true);
                                    }
                                    case 23 -> {
                                        if (tempPerson.isNotFractured()) tempPerson.setHealth(true);
                                        tempPerson.setBleeding(false);
                                    }
                                    case 24 -> tempPerson.setFractured(false);
                                    case 25 -> tempPerson.setOnPainkillers(true);
                                }
                                playerConn.sendString("updateStatuses," + game.getCurrentGameID() +
                                        "," + game.getCurrentUserName() + ",secondPlayer:" + tempImage.getName() + ":"
                                        + tempPerson.getStatuses());
                            }
                            case "equip_add" -> {
                                if (tempPerson.getFirstAddEquip().getId() == 0)
                                    tempPerson.setFirstAddEquip(new AdditionalEquipment(currentEquip.card_id, currentEquip.name));
                                else if (tempPerson.getSecondAddEquip().getId() == 0)
                                    tempPerson.setSecondAddEquip(new AdditionalEquipment(currentEquip.card_id, currentEquip.name));
                                else tempPerson.setFirstAddEquip(new AdditionalEquipment(currentEquip.card_id, currentEquip.name));
                            }
                        }
                    } else {
                        switch (currentAction.card_id){
                            case 18 -> tempPerson.setDoingAnAmbush(true);
                            case 19 -> tempPerson.setHitInHead(true);
                            case 49 -> {
                                if (tempPerson.getArmor().getId() != 14) tempPerson.setFought(false);
                                else {
                                    useTurnLabel("Слишком тяжелая броня");
                                    return;
                                }
                            }
                            case 45 -> {
                                if (tempPerson.getFoughtStatus().equals("0")){
                                    tempPerson.setFought(true);
                                    Random random  = new Random();
                                    int number = random.nextInt(58);
                                    while (number == 0 || !getCardByID(number).type.contains("equip_"))
                                        number = random.nextInt(58);
                                    takeCardNotFromDeck(number);
                                    number = random.nextInt(58);
                                    while (number == 0 || !getCardByID(number).type.contains("equip_"))
                                        number = random.nextInt(58);
                                    takeCardNotFromDeck(number);
                                }
                                else {
                                    useTurnLabel("Этот человек уже отстрелялся");
                                    return;
                                }
                            }
                            case 46 -> {
                                if (tempPerson.getFoughtStatus().equals("0")){
                                    tempPerson.setFought(true);
                                    baseRaidCardCounter++;
                                    if (baseRaidCardCounter == 2){
                                        baseRaidCardCounter = 0;
                                        if (new Random().nextInt(101) >= 90) killEnemy(tempImage);
                                        // дать четыре хорошие карты
                                        Random random  = new Random();
                                        int number = random.nextInt(58);
                                        while (number == 0 || !(getCardByID(number).type.contains("equip_") && getCardByID(number).rareness < 2))
                                            number = random.nextInt(58);
                                        takeCardNotFromDeck(number);
                                        number = random.nextInt(58);
                                        while (number == 0 || !(getCardByID(number).type.contains("equip_") && getCardByID(number).rareness < 2))
                                            number = random.nextInt(58);
                                        takeCardNotFromDeck(number);
                                        number = random.nextInt(58);
                                        while (number == 0 || !(getCardByID(number).type.contains("equip_") && getCardByID(number).rareness < 3))
                                            number = random.nextInt(58);
                                        takeCardNotFromDeck(number);
                                        number = random.nextInt(58);
                                        while (number == 0 || !(getCardByID(number).type.contains("equip_") && getCardByID(number).rareness < 3))
                                            number = random.nextInt(58);
                                        takeCardNotFromDeck(number);
                                    } else {
                                        useTurnLabel("Выберите еще одного человека");
                                        return;
                                    }
                                }
                                else {
                                    useTurnLabel("Этот человек уже отстрелялся");
                                    return;
                                }
                            }
                        }
                        playerConn.sendString("updateStatuses," + game.getCurrentGameID() +
                                "," + game.getCurrentUserName() + ",secondPlayer:" + tempImage.getName() + ":"
                                + tempPerson.getStatuses());
                    }
                    performingPersonAction = false;
                    changeEquipActive = false;
                    repairActive = false;
                    card.person = tempPerson;
                    playerConn.sendString("changeEnemyPersonStatus," + game.getCurrentGameID() + ","
                            + game.getCurrentUserName() + "," + tempImage.getName() + "," + card.getPersonCard());
                    endTurn();
                }
            }
        });
    }

    private ProtectionEquip setNewProtectionEquip() {
        ProtectionEquip tempEquip;
        try { tempEquip = new ProtectionEquip(currentEquip.card_id, currentEquip.defence,
                currentEquip.name, Arrays.stream(currentEquip.effects.split(",")).mapToInt(Integer::parseInt).toArray());
        } catch (Exception e){
            tempEquip = new ProtectionEquip(currentEquip.card_id, currentEquip.defence,
                    currentEquip.name, new int[]{});
        }
        return tempEquip;
    }

    public void spawnBuilding(Card card){
        if (card.card_id == 50) medBaySize++;
        Image tempImage = addToPlayedCards(card);
        playerConn.sendString("playCard," + game.getCurrentGameID() + "," + game.getCurrentUserName() + "," +
                "building," + card.getBuildingCard());
        tempImage.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                if (isDefenceActive) defend(card, tempImage);
                else if(performingBuildingAction){
                    if (currentAction.card_id == 20){
                        card.building.setMinedUp(true);
                        playerConn.sendString("updateMinedUp," + game.getCurrentGameID() +
                                "," + game.getCurrentUserName() + ",secondPlayer:" + tempImage.getName() + ":1");
                        performingBuildingAction = false;
                        endTurn();
                    }
                }
                else fillBuildingStage(card);
            }
        });
    }
    public Image addToPlayedCards(Card card){
        // разместить карту на поле
        firstPlayerActiveCards.put(lastPlayedCardID, card);
        Image tempImage = new Image(new Texture(Gdx.files.internal("Cards/inGame/" + card.image_path)));
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
                if (tempCard.person == null) {if (isAttackActive) attack(tempCard, tempImage);}
                else if (isAttackActive && !tempCard.person.isInMedBay()) attack(tempCard, tempImage);
                else if (tempCard.person.isInMedBay()) useTurnLabel("Цель в медблоке!");
                else startCardStage(tempCard, 600);
            }
        });
        secondPlayerField.addActor(tempImage);
    }

    public ProtectionEquip setProtectionEquip(String[] equipInfo){
        ProtectionEquip equip;
        try { equip = new ProtectionEquip(Integer.parseInt(equipInfo[0]), Integer.parseInt(equipInfo[1]),
                equipInfo[2], Arrays.stream(equipInfo[3].split(":")).mapToInt(Integer::parseInt).toArray());
        } catch (Exception e){
            equip = new ProtectionEquip(Integer.parseInt(equipInfo[0]), Integer.parseInt(equipInfo[1]), equipInfo[2], new int[]{});
        }
        return equip;
    }
    
    public Person createEnemyPerson(String[] info){
        Person tempPerson = new Person();

        String[] armorInfo = info[1].split(";");
        tempPerson.setArmor(setProtectionEquip(armorInfo));

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
        tempPerson.setHelmet(setProtectionEquip(helmetInfo));

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

        if (tempCard.person != null){
            if (secondPlayerField.getChildren().contains(tempImage, true)){
                tempCard.person.setDoingAnAmbush(false);
                playerConn.sendString("updateStatuses," + game.getCurrentGameID() +
                        "," + game.getCurrentUserName() + ",firstPlayer:" + tempImage.getName() + ":"
                        + tempCard.person.getStatuses());
            }
            if (firstPlayerField.getChildren().contains(tempImage, true)){
                playerConn.sendString("updateStatuses," + game.getCurrentGameID() +
                        "," + game.getCurrentUserName() + ",secondPlayer:" + tempImage.getName() + ":"
                        + tempCard.person.getStatuses());
            }
            if (firstPlayerField.getChildren().contains(attackerImage, true)){
                currentAttacker.person.setHitInHead(false);
                playerConn.sendString("updateStatuses," + game.getCurrentGameID() +
                        "," + game.getCurrentUserName() + ",secondPlayer:" + attackerImage.getName() + ":"
                        + currentAttacker.person.getStatuses());
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
                            try {
                                Image defenderImage = (Image) Arrays.stream(secondPlayerField.getChildren().items)
                                        .filter(x -> x.getName().equals(String.valueOf(i))).findFirst().get();
                                attackPeople(card, defenderImage, new Random());
                            } catch (Exception ignored){}
                        }
                        if (!firstPlayerActiveCards.containsValue(currentAttacker)) return;
                    }
                }
            } else if (tempCard.building.isMinedUp()){
                killEnemy(attackerImage);
                tempCard.building.setMinedUp(false);
                playerConn.sendString("updateMinedUp," + game.getCurrentGameID() +
                        "," + game.getCurrentUserName() + ",firstPlayer:" + tempImage.getName() + ":0");
            } else changeBuildingHealthStatus(tempCard, tempImage);
        } catch (Exception ignored){
            if (tempCard.building.getDefenderList().size() == 0)
                changeBuildingHealthStatus(tempCard, tempImage);
        }
    }

    public void updateMinedUp(String info){
        String[] splittedInfo = info.split(":");
        if (splittedInfo[0].equals("firstPlayer"))firstPlayerActiveCards.get(Integer.parseInt(splittedInfo[1]))
                .building.setMinedUp(Integer.parseInt(splittedInfo[2]) == 1);
        else secondPlayerActiveCards.get(Integer.parseInt(splittedInfo[1]))
                .building.setMinedUp(Integer.parseInt(splittedInfo[2]) == 1);
    }
    public void changeBuildingHealthStatus(Card tempCard, Image tempImage){
        if (tempCard.building.isHealth()) tempCard.building.setHealth(false);
        else killEnemy(tempImage);
    }
    public void attackPeople(Card tempCard, Image tempImage, Random random){
        if (!tempCard.person.isDefender() && !(currentAttacker.person.getWeapon().getId() == 51)
                && !(currentAttacker.person.getWeapon().getId() == 52) && !tempCard.person.isDoingAnAmbush()){
            // атаковать
            boolean noCounterAttack = addEquipCheck(tempCard, tempImage, random);
            attackEnemy(tempCard, tempImage, random, currentAttacker.person.getWeapon());
            if ((secondPlayerField.getChildren().contains(tempImage, true) ||
                    firstPlayerField.getChildren().contains(tempImage, true)) && noCounterAttack){
                // обратная атака
                attackEnemy(currentAttacker, attackerImage, random, tempCard.person.getWeapon());
            }
        } else {
            if (tempCard.person.isDoingAnAmbush()) useTurnLabel("Засада!");
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
                    if (tempCard.person.isNotWounded() && random.nextBoolean()) tempCard.person.setHealth(false);
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
                        if (tempCard.person.isNotWounded() && random.nextBoolean()) tempCard.person.setHealth(false);
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
            if (currentAttacker.person.getWeapon().equals(attackerWeapon) && currentAttacker.person.isHitInHead()) hitToHead = true;
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

    public ProtectionEquip attackProtectionEquip(Card tempCard, Weapon attackerWeapon, Image tempImage, String equipType){
        ProtectionEquip defenderEquip = new ProtectionEquip(999, 0, "0", new int[]{});
        if (equipType.equals("armor")) defenderEquip = tempCard.person.getArmor();
        else if (equipType.equals("helmet")) defenderEquip = tempCard.person.getHelmet();
        if (currentAttacker.person.getWeapon().equals(attackerWeapon)){
            if (tempCard.person.isDefender()) defenderEquip.setDefence(defenderEquip.getDefence() + tempCard.defence);
            if (defenderEquip.getDefence() >= attackerWeapon.getAttack() + currentAttacker.attack){
                defenderEquip.setDefence(defenderEquip.getDefence() - attackerWeapon.getAttack() - currentAttacker.attack);
                if (tempCard.person.isDefender()){
                    if (defenderEquip.getDefence() >= tempCard.defence)
                        defenderEquip.setDefence(defenderEquip.getDefence() - tempCard.defence);
                    else defenderEquip.setDefence(0);
                }
            } else {
                defenderEquip.setDefence(0);
                if (tempCard.person.isOnPainkillers() && new Random().nextInt(101) >= 25) return new ProtectionEquip(999, 0, "0", new int[]{});
                if (tempCard.person.isNotWounded()) checkOnInjuries (tempCard);
                else {killEnemy(tempImage);return new ProtectionEquip(999, 0, "0", new int[]{});}
            }
        } else {
            if (defenderEquip.getDefence() >= attackerWeapon.getAttack())
                defenderEquip.setDefence(defenderEquip.getDefence() - attackerWeapon.getAttack());
            else {
                defenderEquip.setDefence(0);
                if (tempCard.person.isNotWounded()) checkOnInjuries (tempCard);
                else {killEnemy(tempImage);return new ProtectionEquip(999, 0, "0", new int[]{});}
            }
        }
        return defenderEquip;
    }

    public void checkOnInjuries(Card tempCard){
        Random random = new Random();
        tempCard.person.setHealth(false);
        int i = random.nextInt(10);
        if (i == 8) tempCard.person.setFractured(true);
        else if (i == 9) tempCard.person.setBleeding(true);
    }

    public void attackArmor(Card tempCard, Weapon attackerWeapon, Image tempImage){
        tempCard.person.setArmor(attackProtectionEquip(tempCard, attackerWeapon, tempImage, "armor"));
        if (tempCard.person.getArmor().getId() != 999){
            if (firstPlayerActiveCards.containsValue(tempCard))
                playerConn.sendString("changeEnemyPersonStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                        "," + tempImage.getName() + "," + tempCard.getPersonCard());
            else if (secondPlayerActiveCards.containsValue(tempCard))
                playerConn.sendString("changeAllyPersonStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                        "," + tempImage.getName() + "," + tempCard.getPersonCard());
        }
    }
    public void attackHead(Card tempCard, Weapon attackerWeapon, Image tempImage){
        tempCard.person.setHelmet(attackProtectionEquip(tempCard, attackerWeapon, tempImage, "helmet"));
        if (tempCard.person.getHelmet().getId() != 999) {
            if (firstPlayerActiveCards.containsValue(tempCard))
                playerConn.sendString("changeEnemyPersonStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                        "," + tempImage.getName() + "," + tempCard.getPersonCard());
            else if (secondPlayerActiveCards.containsValue(tempCard))
                playerConn.sendString("changeAllyPersonStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                        "," + tempImage.getName() + "," + tempCard.getPersonCard());
        }
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
        repairActive = false; changeEquipActive = false; currentEquip = null; currentPeopleCountInMedBay = 0;
        baseRaidCardCounter = 0; currentAction = null; performingPersonAction = false; performingBuildingAction = false;
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
                if (myTurn && card.person.getFoughtStatus().equals("0") && !card.person.isInMedBay()
                        && !secondPlayerActiveCards.isEmpty()){
                    isAttackActive = true;
                    isDefenceActive = false;
                    attackerImage = tempImage;
                    currentAttacker = card;
                } else useTurnLabel("Данный боец не может атаковать");
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
                if (myTurn && !card.person.isInMedBay() && firstPlayerActiveCards.values().stream().anyMatch(x -> x.type.equals("building")
                        || x.type.equals("objective")) && card.person.getFoughtStatus().equals("0")){
                    isDefenceActive = true;
                    isAttackActive = false;
                    currentDefender = card;
                } else useTurnLabel("Данный боец не может оборонять");
            }
        });
        cardStage.addActor(defendButton);

        // кнопка для ухода в медблок
        TextButton medBayButton = new TextButton("Уйти в медблок", game.getTextButtonStyle());
        medBayButton.setPosition(800, 150);
        medBayButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (myTurn){
                    if (medBaySize > currentPeopleCountInMedBay && !card.person.isInMedBay()){
                        currentPeopleCountInMedBay++;
                        card.person.setInMedBay(true);
                        playerConn.sendString("updateStatuses," + game.getCurrentGameID() +
                                "," + game.getCurrentUserName() + ",secondPlayer:" + tempImage.getName() + ":"
                                + card.person.getStatuses());
                        endTurn();
                    } else useTurnLabel("Медблок полон");
                } else useTurnLabel("Не ваш ход");
                isCardStageActive = false;
                Gdx.input.setInputProcessor(stage);
            }
        });
        cardStage.addActor(medBayButton);

        Table gameGroup = new Table();
        gameGroup.pad(10).defaults().expandX().space(4);
        Table gameContainerTable = new Table();
        ScrollPane gameScrollPane = new ScrollPane(gameGroup, neonSkin);
        gameScrollPane.setOverscroll(false, true);
        gameScrollPane.setScrollingDisabled(true, false);

        gameGroup.row();
        // надписи о вооружении бойца
        Label weaponLabel = new Label("Оружие: " + card.person.getWeaponString().split(";")[2] +
                " Урон: " + card.person.getWeaponString().split(";")[1], game.getMainLabelStyle());
        weaponLabel.setColor(Color.GOLDENROD);
        weaponLabel.setAlignment(Align.center);
        weaponLabel.setWrap(true);
        gameGroup.add(weaponLabel).expandX().prefWidth(1015 * game.xScaler);

        if (card.person.getWeaponString().split(";").length > 3){
            gameGroup.row();
            Label weaponEffectLabel = new Label("Эффекты: ", game.getMainLabelStyle());
            StringBuilder effects = new StringBuilder();
            for (String effect:card.person.getWeaponString().split(";")[3].split(":"))
                effects.append(EffectHandler.effectMap.get(Integer.parseInt(effect))).append(", ");
            setEffectLabel(gameGroup, weaponEffectLabel, effects);
        }

        gameGroup.row();
        // надписи о броне бойца
        Label armorLabel = new Label("Броня: " + card.person.getArmorString().split(";")[2] +
                " Защита: " + card.person.getArmorString().split(";")[1], game.getMainLabelStyle());
        armorLabel.setColor(Color.GOLDENROD);
        armorLabel.setAlignment(Align.center);
        armorLabel.setWrap(true);
        gameGroup.add(armorLabel).expandX().prefWidth(1015 * game.xScaler);

        if (card.person.getArmorString().split(";").length > 3){
            gameGroup.row();
            Label armorEffectLabel = new Label("Эффекты: ", game.getMainLabelStyle());
            StringBuilder effects = new StringBuilder();
            for (String effect:card.person.getArmorString().split(";")[3].split(":"))
                effects.append(EffectHandler.effectMap.get(Integer.parseInt(effect))).append(", ");
            setEffectLabel(gameGroup, armorEffectLabel, effects);
        }

        gameGroup.row();
        // надписи о шлеме бойца
        Label helmetLabel = new Label("Шлем: " + card.person.getHelmetString().split(";")[2] +
                " Защита: " + card.person.getHelmetString().split(";")[1], game.getMainLabelStyle());
        helmetLabel.setColor(Color.GOLDENROD);
        helmetLabel.setAlignment(Align.center);
        helmetLabel.setWrap(true);
        gameGroup.add(helmetLabel).expandX().prefWidth(1015 * game.xScaler);

        if (card.person.getHelmetString().split(";").length > 3){
            gameGroup.row();
            Label helmetEffectLabel = new Label("Эффекты: ", game.getMainLabelStyle());
            StringBuilder effects = new StringBuilder();
            for (String effect:card.person.getHelmetString().split(";")[3].split(":"))
                effects.append(EffectHandler.effectMap.get(Integer.parseInt(effect))).append(", ");
            setEffectLabel(gameGroup, helmetEffectLabel, effects);
        }

        gameGroup.row();
        // надписи о доп снаряжении бойца
        Label firstAddEquipLabel = new Label("Снаряжение: " +
                card.person.firstEquipString().split(";")[1], game.getMainLabelStyle());
        firstAddEquipLabel.setColor(Color.GOLDENROD);
        firstAddEquipLabel.setAlignment(Align.center);
        firstAddEquipLabel.setWrap(true);
        gameGroup.add(firstAddEquipLabel).expandX().prefWidth(1015 * game.xScaler);

        if (Integer.parseInt(card.person.firstEquipString().split(";")[0]) != 0){
            gameGroup.row();
            Label equipEffectLabel = new Label("Описание: " +
                    getCardByID(Integer.parseInt(card.person.firstEquipString().split(";")[0])).description,
                    game.getMainLabelStyle());
            equipEffectLabel.setAlignment(Align.center);
            equipEffectLabel.setWrap(true);
            gameGroup.add(equipEffectLabel).expandX().prefWidth(1015 * game.xScaler);
        }

        gameGroup.row();
        Label secondAddEquipLabel = new Label("Снаряжение: " +
                card.person.secondEquipString().split(";")[1], game.getMainLabelStyle());
        secondAddEquipLabel.setColor(Color.GOLDENROD);
        secondAddEquipLabel.setAlignment(Align.center);
        secondAddEquipLabel.setWrap(true);
        gameGroup.add(secondAddEquipLabel).expandX().prefWidth(1015 * game.xScaler);

        if (Integer.parseInt(card.person.secondEquipString().split(";")[0]) != 0){
            gameGroup.row();
            Label equipEffectLabel = new Label("Описание: " +
                    getCardByID(Integer.parseInt(card.person.secondEquipString().split(";")[0])).description,
                    game.getMainLabelStyle());
            equipEffectLabel.setAlignment(Align.center);
            equipEffectLabel.setWrap(true);
            gameGroup.add(equipEffectLabel).expandX().prefWidth(1015 * game.xScaler);
        }

        gameContainerTable.add(gameScrollPane).expand().fill();
        gameContainerTable.setPosition(475 * game.xScaler, 300 * game.yScaler);
        gameContainerTable.setSize(1015 * game.xScaler, 600 * game.yScaler);
        cardStage.addActor(gameContainerTable);
        for (Actor actor:cardStage.getActors()) {
            actor.scaleBy(game.xScaler - 1,  game.yScaler - 1);
            actor.setPosition(actor.getX() * game.xScaler, actor.getY() * game.yScaler);
        }
    }

    private void setEffectLabel(Table gameGroup, Label effectLabel, StringBuilder effects) {
        effects.delete(effects.length() - 2, effects.length() - 1);
        effectLabel.setText("Эффекты: " + effects);
        effectLabel.setAlignment(Align.center);
        effectLabel.setWrap(true);
        gameGroup.add(effectLabel).expandX().prefWidth(1015 * game.xScaler);
    }
    public void takeCardNotFromDeck(int id){
        if (firstPlayerHand.getChildren().items.length < 9){
            takeCard(id);
            playerConn.sendString("takeCardNotFromDeck," + game.getCurrentGameID() + "," + game.getCurrentUserName());
        } else useTurnLabel("Кончилось место в руке");
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
                    if (!firstPlayerField.getChildren().contains(objectiveCardImage, true)){
                        playerObjective = new Objective(0);
                        stage.getActors().removeValue(objectiveImage, true);
                        failedObjective("normal");
                        objectiveEnded = true;
                    }
                    if (objectiveCard.building.getDefenderList().size() >= 3)
                        objectiveEnded = endOfExpensiveCaseObjective();
                    if (objectiveCard.building.getDefenderList().size() >= 1) {
                        playerObjective.setDuration(playerObjective.getDuration() - 1);
                        if (objectiveCard.building.getDefenderList().stream().anyMatch(x -> x.card_id == 35 ||
                                x.card_id == 36 ||  x.card_id == 37))
                            playerObjective.setDuration(playerObjective.getDuration() - 1);
                        if (playerObjective.getDuration() <= 0)
                            objectiveEnded = endOfExpensiveCaseObjective();
                    }
                    if (objectiveCard.building.getDefenderList().size() == 0) playerObjective.setDuration(2);
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
        ArrayList<Integer> killList = new ArrayList<>();
        for (Card card:firstPlayerActiveCards.values()){
            int cardID = 0;
            for (int i:firstPlayerActiveCards.keySet()) if (firstPlayerActiveCards.get(i).equals(card)) {cardID = i;break;}
            try {Person tempPerson = card.person;
                tempPerson.setFought(false);
                if (tempPerson.isInMedBay()){
                    tempPerson.setInMedBay(false);
                    tempPerson.setBleeding(false);
                    tempPerson.setFractured(false);
                    tempPerson.setHealth(true);
                }
                tempPerson.setOnPainkillers(false);
                if (tempPerson.isBleeding()){
                    if (tempPerson.isNotWounded()) tempPerson.setHealth(false);
                    else killList.add(cardID);
                    tempPerson.setBleeding(false);
                }
                card.person = tempPerson;
                if (!killList.contains(cardID)) playerConn.sendString("updateStatuses," + game.getCurrentGameID() +
                        "," + game.getCurrentUserName() + ",secondPlayer:" + cardID + ":" + tempPerson.getStatuses());
            } catch (Exception ignored){}
            try {card.building.setAlreadyUsed(false);
                if (card.card_id == 47){
                    Random random  = new Random();
                    int number = random.nextInt(58);
                    while (number == 0 || !getCardByID(number).type.contains("equip_"))
                        number = random.nextInt(58);
                    takeCardNotFromDeck(number);
                }
                if (card.card_id == 53){
                    Random random  = new Random();
                    int number = random.nextInt(58);
                    while (number == 0 || !getCardByID(number).type.equals("objective"))
                        number = random.nextInt(58);
                    takeCardNotFromDeck(number);
                }
            } catch (Exception ignored){}
        }
        for (int i:killList) {
            try {killEnemy((Image) firstPlayerField.getChildren().get(i));
            } catch (Exception ignored){}
        }
    }

    public void updateStatuses(String info){
        String[] splittedInfo = info.split(":");
        Card tempCard;
        if (splittedInfo[0].equals("firstPlayer")) tempCard = firstPlayerActiveCards.get(Integer.parseInt(splittedInfo[1]));
        else tempCard = secondPlayerActiveCards.get(Integer.parseInt(splittedInfo[1]));
        ArrayList<Integer> statusList = new ArrayList<>();
        statusList.add(Integer.parseInt(splittedInfo[2]));
        statusList.add(Integer.parseInt(splittedInfo[3]));
        statusList.add(Integer.parseInt(splittedInfo[4]));
        statusList.add(Integer.parseInt(splittedInfo[5]));
        statusList.add(Integer.parseInt(splittedInfo[6]));
        statusList.add(Integer.parseInt(splittedInfo[7]));
        tempCard.person.setStatuses(statusList);
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
            if (firstPlayerActiveCards.get(id).card_id == 50 && medBaySize > 1) medBaySize--;
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
        changePersonStatus(id, info, secondPlayerActiveCards);
    }
    public void changeAllyPersonStatus(int id, String info){
        changePersonStatus(id, info, firstPlayerActiveCards);
    }
    private void changePersonStatus(int id, String info, HashMap<Integer, Card> playerCards) {
        String[] splittedInfo = info.split(" . ");
        Card tempCard = playerCards.get(id);
        Person tempPerson = createEnemyPerson(splittedInfo);
        tempCard.person.setHealth(tempPerson.isNotWounded());
        tempCard.person.setWeapon(tempPerson.getWeapon());
        tempCard.person.setHelmet(tempPerson.getHelmet());
        tempCard.person.setArmor(tempPerson.getArmor());
        tempCard.person.setFirstAddEquip(tempPerson.getSecondAddEquip());
        tempCard.person.setSecondAddEquip(tempPerson.getFirstAddEquip());
        playerCards.replace(id, tempCard);
    }

    public void victory(){
        playerConn.sendString("endGame," + game.getCurrentGameID() + "," + game.getCurrentUserName()
                + ",victory," + victoryPoints + "," + firstPlayerField.getChildren().size);
    }
    public void lose(){
        playerConn.sendString("endGame," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                ",lose," + victoryPoints + "," + firstPlayerField.getChildren().size);
    }

    public void sendEndStatus(){
        playerConn.sendString("endStatus," + game.getCurrentGameID() + "," + game.getCurrentUserName() +
                "," + completedObjectives + "," + victoryPoints + "," + firstPlayerField.getChildren().size);
    }
    public void startEndScreen(String screenStatus){
        isCardStageActive = false;
        isStatusStageActive = false;
        Image endScreen = new Image(new Texture(Gdx.files.internal("Images/endGame_bg.jpg")));
        endScreen.setPosition(0,0);
        stage.addActor(endScreen);
        Image dogtags = new Image(new Texture(Gdx.files.internal("Images/dogtag.png")));
        dogtags.setPosition(900, 540);
        stage.addActor(dogtags);
        Image exp = new Image(new Texture(Gdx.files.internal("Images/experience.png")));
        exp.setPosition(1300, 540);
        stage.addActor(exp);
        Image rating = new Image(new Texture(Gdx.files.internal("Images/rating.png")));
        rating.setPosition(400, 540);
        stage.addActor(rating);
        TextButton exitButton = new TextButton("Вернуться в меню", game.getTextButtonStyle());
        exitButton.setPosition(775, 150);
        exitButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                game.setScreen(new MainMenuScreen(game));
            }
        });
        stage.addActor(exitButton);

        Label newExp = new Label("", game.getMainLabelStyle());
        newExp.setPosition(1340, 600);
        stage.addActor(newExp);
        Label newDogtags = new Label("", game.getMainLabelStyle());
        newDogtags.setPosition(925, 575);
        stage.addActor(newDogtags);
        try {
            PreparedStatement preparedStatement = conn.prepareStatement("SELECT rating FROM users WHERE nickname=?");
            preparedStatement.setString(1, game.getCurrentUserName());
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            Label newRating = new Label(resultSet.getString("rating"), game.getMainLabelStyle());
            newRating.setPosition(430, 575);
            stage.addActor(newRating);
        } catch (SQLException e) {e.printStackTrace();}
        switch (screenStatus){
            case "draw" -> {
                newExp.setText("+2");
                newDogtags.setText("+10");
            }
            case "victory" -> {
                newExp.setText("+3");
                newDogtags.setText("+15");
            }
            case "lose" -> {
                newExp.setText("+1");
                newDogtags.setText("+5");
            }
        }
    }
}