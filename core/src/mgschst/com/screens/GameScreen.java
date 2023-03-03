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
import com.badlogic.gdx.utils.ScreenUtils;
import mgschst.com.MainMgschst;
import mgschst.com.connect.DatabaseHandler;
import mgschst.com.connect.TCPConnection;
import mgschst.com.dbObj.Card;
import mgschst.com.dbObj.SecondPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

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
    HorizontalGroup firstPlayerHand = new HorizontalGroup();
    HorizontalGroup secondPlayerHand = new HorizontalGroup();

    boolean myTurn = false;
    Stage cardStage = new Stage();
    Boolean isCardStageActive = false;
    HorizontalGroup firstPlayerField = new HorizontalGroup();

    Label victoryPointsFirstPlayer, victoryPointsSecondPlayer;
    VerticalGroup resourcesGroup = new VerticalGroup();

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
            public void clicked(InputEvent event, float x, float y){ if (myTurn) endTurn(); }});
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
        game.setButtonPressed(false);
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.end();

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 60f));
        stage.draw();

        if (isCardStageActive) {
            cardStage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 60f));
            cardStage.draw();
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
        firstDeck.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                // показать верхние 5 карт, если разведцентр установлен
            }
        });
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
        addNewResource(5);
        stage.addActor(resourcesGroup);
    }
    public void secondPlayerInitialize(){
        // счетчик карт в колоде
        secondCardCounter = new Label("43", game.getMainLabelStyle());
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
        for (int i = 0; i < 7; i++)
            secondPlayerHand.addActor(new Image(new Texture(
                    Gdx.files.internal("UserInfo/Cards/GameCards/" + secondPlayer.cardPicturePath))));

        victoryPointsSecondPlayer = new Label("ПО: 0/5", game.getMainLabelStyle());
        victoryPointsSecondPlayer.setColor(Color.GOLDENROD);
        victoryPointsSecondPlayer.setPosition(15, 900);
        stage.addActor(victoryPointsSecondPlayer);
    }

    public void changeTimer(int time){
        if (time == 0 && myTurn) { endTurn(); return; }
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
                // проверка на то, чей ход
                if (!myTurn){
                    firstPlayerHand.removeActor(tempImage);
                    firstPlayerHand.addActor(tempImage);
                    return;
                }
                // Проверка на количество разыгранных карт
                if (firstPlayerField.getChildren().size >= 8 && (tempCard.type.equals("building") ||
                        tempCard.type.equals("people") || tempCard.type.equals("summon_people")))
                {useTurnLabel("Нельзя разыграть больше карт"); return;}

                // проверка на нахождение в игровой зоне (зона сдвинута на 565/70 из-за либгыдыха)
                if (tempImage.getX() + (tempImage.getWidth() / 2) < 1065 * game.xScaler &&
                        tempImage.getX() + (tempImage.getWidth() / 2) > -205 * game.xScaler &&
                        tempImage.getY() + (tempImage.getHeight() / 2) > 125 * game.yScaler &&
                        tempImage.getY() + (tempImage.getHeight() / 2) < 395 * game.yScaler){
                    playCard(tempCard, tempImage);
                    return;
                }

                // если поместить карту не в игровую зону
                firstPlayerHand.removeActor(tempImage);
                firstPlayerHand.addActor(tempImage);
            }
        });
        firstPlayerHand.addActor(tempImage);
    }

    public Card getCardByID(int id) {
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

        TextButton addButton = new TextButton("Разыграть", game.getTextButtonStyle());
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
        if (!checkPrice(tempCard, tempImage))
            return;
        if (tempCard.type.equals("building") || tempCard.type.equals("people")){
            // разместить карту на поле
            firstPlayerField.addActor(tempImage);
        }
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
                } else {
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
}

