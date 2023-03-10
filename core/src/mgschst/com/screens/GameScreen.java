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
import mgschst.com.dbObj.Equipment.Weapon;
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

    Image background;
    Image chatSwitch;
    TextField chat;
    TextButton sendMessage;
    VerticalGroup chatGroup;

    Label firstCardCounter, secondCardCounter, turnTimer, turnLabel;
    TextButton endTurnButton;

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
    Boolean isCardStageActive = false;
    HorizontalGroup firstPlayerField = new HorizontalGroup(), secondPlayerField = new HorizontalGroup();
    HashMap<Integer, Card> firstPlayerActiveCards = new HashMap<>();
    HashMap<Integer, Card> secondPlayerActiveCards = new HashMap<>();
    static int lastPlayedCardID = 0;
    public static ArrayList<Integer> globalEffects = new ArrayList<>();

    Label victoryPointsFirstPlayer, victoryPointsSecondPlayer;
    VerticalGroup resourcesGroup = new VerticalGroup();
    int resourceMax = 5;

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

        endTurnButton = new TextButton("?????????????????? ??????", game.getTextButtonStyle());
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

        turnTimer = new Label("60 ????????????...", game.getMainLabelStyle());
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
        chat.setMessageText("?????????????? ??????????????????:");
        chat.setPosition(25, 160);
        chat.setWidth(1000f * game.xScaler);
        chat.setVisible(false);
        stage.addActor(chat);

        sendMessage = new TextButton("?????????????????? ??????????????????", game.getTextButtonStyle());
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
        // ?????????????? ???????? ?? ????????????
        firstCardCounter = new Label("50", game.getMainLabelStyle());
        firstCardCounter.setPosition(25, 285);
        firstCardCounter.setAlignment(Align.center);
        firstCardCounter.setWidth(150f * game.xScaler);
        firstCardCounter.setWrap(true);
        // ?????????????? ???????????? ?????????????? ????????????
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

        victoryPointsFirstPlayer = new Label("????: 0/5", game.getMainLabelStyle());
        victoryPointsFirstPlayer.setColor(Color.GOLDENROD);
        victoryPointsFirstPlayer.setPosition(1750, 50);
        stage.addActor(victoryPointsFirstPlayer);

        resourcesGroup.setPosition(1685, 500);
        resourcesGroup.space(5f);
        addNewResource(resourceMax);
        stage.addActor(resourcesGroup);
    }
    public void secondPlayerInitialize(){
        // ?????????????? ???????? ?? ????????????
        secondCardCounter = new Label("50", game.getMainLabelStyle());
        secondCardCounter.setPosition(1740, 755);
        secondCardCounter.setAlignment(Align.center);
        secondCardCounter.setWidth(150f * game.xScaler);
        secondCardCounter.setWrap(true);
        // ?????????????? ???????????? ?????????????? ????????????
        Image secondDeck = new Image(new Texture(Gdx.files.internal
                ("UserInfo/Cards/GameCards/" + secondPlayer.cardPicturePath)));
        secondDeck.setPosition(1740, 670);
        secondDeck.addListener(new ClickListener(){
            public void clicked(InputEvent event, float x, float y){
                // ???????????????? ?????????????? 5 ????????, ???????? ?????????????????????? ????????????????????
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

        victoryPointsSecondPlayer = new Label("????: 0/5", game.getMainLabelStyle());
        victoryPointsSecondPlayer.setColor(Color.GOLDENROD);
        victoryPointsSecondPlayer.setPosition(15, 900);
        stage.addActor(victoryPointsSecondPlayer);
    }

    public void changeTimer(int time){
        if (time == 0 && myTurn) { endTurn(); setPlayerEndedTurn(); return; }
        String timer = String.valueOf(time);
        if (timer.startsWith("1") && timer.length() == 2){
            turnTimer.setText(time + " ????????????...");
        } else if (timer.endsWith("1")){
            turnTimer.setText(time + " ??????????????...");
        } else if (timer.endsWith("2") || timer.endsWith("3") || timer.endsWith("4")){
            turnTimer.setText(time + " ??????????????...");
        } else { turnTimer.setText(time + " ????????????..."); }
    }

    public void endTurn(){
        // ???????????????? ???????? ?????????????? ????????????
        playerConn.sendString("changeTurn," + game.getCurrentGameID());
        useTurnLabel("?????? ????????????: " + secondPlayer.nickname);
        myTurn = false;
    }

    public void takeTurn(){
        // ???????????? ???????? ?????????????? ??????????????
        useTurnLabel("?????? ????????????: " + game.getCurrentUserName());
        myTurn = true;
    }

    public void showFirstPlayer(String player){
        turnLabel.setPosition(800, 700);
        turnLabel.setWidth(300f * game.xScaler);
        turnLabel.setAlignment(Align.center);
        stage.addActor(turnLabel);
        useTurnLabel("???????? ????????????????: " + player);

        for (int i = 0; i < 7; i++)
            playerConn.sendString("takeCard," + game.getCurrentGameID() + ","+game.getCurrentUserName());

        if (player.equals(game.getCurrentUserName()))
            takeTurn();
    }

    public AlphaAction createAlphaAction() {
        AlphaAction tempAlphaAction = new AlphaAction();
        tempAlphaAction.setAlpha(0f);
        tempAlphaAction.setDuration(2f);
        return tempAlphaAction;
    }

    public void takeCard(int cardID){
        firstCardCounter.setText(Integer.parseInt(String.valueOf(firstCardCounter.getText())) - 1);
        // ???????????????? ?????????? ?? ????????
        Card tempCard = getCardByID(cardID);
        Image tempImage = new Image(new Texture(
                Gdx.files.internal("Cards/inGame/" + tempCard.image_path)));
        tempImage.setName("");
        tempImage.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                // ?????????????? ???????????????? ??????????
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
                // ???????????????? ???? ????, ?????? ??????
                if (!myTurn){returnCardInHand(tempImage);return;}

                // ???????????????? ???? ???????????????????? ?????????????????????? ????????
                if (firstPlayerField.getChildren().size >= 8 && (tempCard.type.equals("building") ||
                        tempCard.type.equals("people") || tempCard.type.equals("summon_people")))
                {useTurnLabel("???????????? ?????????????????? ???????????? ????????");
                    returnCardInHand(tempImage); return;}

                // ???????????????? ???? ?????????????? ?????????? ???? ??????????
                if ((tempCard.type.equals("equip_weapon")||tempCard.type.equals("equip_helmet")||
                        tempCard.type.equals("equip_armor")||tempCard.type.equals("equip_heal")||
                        tempCard.type.equals("equip_add"))&&
                        firstPlayerActiveCards.values().stream()
                                .map(a -> a.type.equals("people")).findAny().isEmpty()){
                    useTurnLabel("?????? ???????????????? ?????????? ?????? ??????????????????????????");
                    returnCardInHand(tempImage); return;}

                // ???????????????? ???? ???????????????????? ?? ?????????????? ???????? (???????? ???????????????? ???? 565/70 ????-???? ??????????????????)
                if (tempImage.getX() + (tempImage.getWidth() / 2) < 1065 * game.xScaler &&
                        tempImage.getX() + (tempImage.getWidth() / 2) > -205 * game.xScaler &&
                        tempImage.getY() + (tempImage.getHeight() / 2) > 125 * game.yScaler &&
                        tempImage.getY() + (tempImage.getHeight() / 2) < 395 * game.yScaler){
                    playCard(tempCard, tempImage);
                    return;
                }

                // ???????? ?????????????????? ?????????? ???? ?? ?????????????? ????????
                returnCardInHand(tempImage);
            }
        });
        firstPlayerHand.addActor(tempImage);
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
        isCardStageActive = true;
        cardStage = new Stage();
        Gdx.input.setInputProcessor(cardStage);

        Image bg = new Image(new Texture(Gdx.files.internal("DeckAssets/card_info_bg.png")));
        bg.setPosition(25, 125);
        cardStage.addActor(bg);

        Image cardImage = new Image(new Texture(Gdx.files.internal("Cards/origs/" + currentCard.image_path)));
        cardImage.setPosition(50, 325);
        cardStage.addActor(cardImage);

        Label cardName = new Label(currentCard.name, game.getMainLabelStyle());
        cardName.setPosition(950 - (currentCard.name.length() * 12), 800);
        cardStage.addActor(cardName);

        Label cardDesc = new Label(currentCard.description, game.getMainLabelStyle());
        cardDesc.setPosition(475, 550);
        cardDesc.setWidth(1000 * game.xScaler);
        cardDesc.setWrap(true);
        cardDesc.setAlignment(Align.center);
        cardStage.addActor(cardDesc);

        Label cardPrice = new Label("", game.getMainLabelStyle());
        switch (currentCard.cost_type) {
            case "free" -> cardPrice.setText("??????????????????");
            case "prapor" -> cardPrice.setText("????????????: " + currentCard.price + " ????????????????" + (currentCard.price == 1 ? "??" : "??"));
            case "mechanic" -> cardPrice.setText("??????????????: " + currentCard.price + " ????????????????" + (currentCard.price == 1 ? "??" : "??"));
            case "therapist" -> cardPrice.setText("????????????????:  " + currentCard.price + " ????????????????" + (currentCard.price == 1 ? "??" : "??"));
            case "any" -> cardPrice.setText("?????????? ??????????????????:  " + currentCard.price);
        }
        cardPrice.setPosition(50, 950);
        cardPrice.setWidth(400 * game.xScaler);
        cardPrice.setWrap(true);
        cardPrice.setAlignment(Align.center);
        cardStage.addActor(cardPrice);

        TextButton closeButton = new TextButton("??????????????", game.getTextButtonStyle());
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

        TextButton addButton = new TextButton("??????????????????", game.getTextButtonStyle());
        addButton.setPosition(860, 250);
        addButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                isCardStageActive = false;
                Gdx.input.setInputProcessor(stage);
                cardStage.dispose();
                playCard(currentCard, tempImage);
            }
        });
        cardStage.addActor(addButton);

        for (Actor actor:cardStage.getActors()) {
            actor.scaleBy(game.xScaler - 1,  game.yScaler - 1);
            actor.setPosition(actor.getX() * game.xScaler, actor.getY() * game.yScaler);
        }
    }

    public void playCard(Card tempCard, Image tempImage){
        if (!checkPrice(tempCard, tempImage)) return;
        playerConn.sendString("cardFromHand," + game.getCurrentGameID() + "," + game.getCurrentUserName());
        if (tempCard.effects != null)
            for(String effect:tempCard.effects.split(","))
                EffectHandler.handEffect(Integer.parseInt(effect), tempCard, game);
        // ???????????? ?????????? ???? ????????
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
                useTurnLabel("???????????????????????? ??????????????????");
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
        // ???????????????????? ?????????? ???? ????????
        firstPlayerActiveCards.put(lastPlayedCardID, card);
        Image tempImage = new Image(new Texture(
                Gdx.files.internal("Cards/inGame/" + card.image_path)));
        tempImage.setName(String.valueOf(lastPlayedCardID));
        lastPlayedCardID++;
        // ???????????????? ???????????????????? ?? ?????????? ?? ???????? ???? ?????????????????????? ??????????????????????
        firstPlayerField.addActor(tempImage);
        return tempImage;
    }

    public void placeEnemyCard(String cardType, String cardInfo){
        String[] info = cardInfo.split(" ");
        Card tempCard = getCardByID(Integer.parseInt(info[0]));
        if (cardType.equals("people")){
            Person tempPerson = new Person();

            // setting armor
            String[] armorInfo = info[1].split(";");
            Armor tempArmor;
            if (armorInfo.length == 3)
                tempArmor = new Armor(Integer.parseInt(armorInfo[0]), Integer.parseInt(armorInfo[1]),
                        armorInfo[2], null);
            else
                tempArmor = new Armor(Integer.parseInt(armorInfo[0]), Integer.parseInt(armorInfo[1]),
                        armorInfo[2], Arrays.stream(armorInfo[3].split(";")).mapToInt(Integer::parseInt).toArray());
            tempPerson.setArmor(tempArmor);

            // setting weapon
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
        } else
            tempCard.building = new Building(Integer.parseInt(info[0]));

        secondPlayerActiveCards.put(lastPlayedCardID, tempCard);

        Image tempImage = new Image(new Texture(
                Gdx.files.internal("Cards/inGame/" + tempCard.image_path)));
        tempImage.setName(String.valueOf(lastPlayedCardID));
        lastPlayedCardID++;
        // set listener to image
        secondPlayerField.addActor(tempImage);
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
        // ???????????????? ???????????????? ???????? ???????? ?? ??????????
        // ?????????????????????? ??????????
    }
}

