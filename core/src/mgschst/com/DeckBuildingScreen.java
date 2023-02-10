package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class DeckBuildingScreen implements Screen {
    final MainMgschst game;
    Integer deckID;
    final OrthographicCamera camera;
    final Batch batch;
    Stage stage;
    Texture background;

    TextField deckNameField;
    TextButton saveButton;
    TextButton exitButton;
    Label deckNameLabel;
    Label objectiveCountLabel;
    Label rareCardsCountLabel;
    Label superRareCardsCountLabel;
    Label peopleCardsCountLabel;
    Label buildingsCardsCountLabel;
    Label deckSizeCountLabel;

    ScrollPane cardPane;
    Table cardTable;
    Table cardContainerTable;

    Connection conn = new DatabaseHandler().getConnection();
    Skin neonSkin = new Skin(Gdx.files.internal("Skins/neon/skin/neon-ui.json"));

    Stage cardStage = new Stage();
    Boolean isCardStageActive = false;

    HashMap<Integer, Card> userCards = new HashMap<>();
    Integer cardAmount;
    HashMap<Integer, Card> deckCards = new HashMap<>();
    Integer deckSize = 0;
    Integer objectiveAmount = 0;
    Integer rareCardsAmount = 0;
    Integer superRareCardsAmount = 0;
    Integer peopleCardAmount = 0;
    Integer buildingCardAmount = 0;

    public DeckBuildingScreen(final MainMgschst game, int newDeckID) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        camera = game.getCamera();
        batch = game.batch;

        background = new Texture(Gdx.files.internal("DeckAssets/deck_bg.jpg"));

        deckID = newDeckID;

        // считывание карт пользователя для учета  общей статистики каждой карты
        try {
            PreparedStatement preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE nickname = ?");
            preparedStatement.setString(1, game.getCurrentUserName());
            ResultSet resultSet = preparedStatement.executeQuery();

            resultSet.next();
            for (String value : resultSet.getString("cards").split(","))
                userCards.put(Integer.parseInt(value.split(":")[0]),
                        getUserCardByID(Integer.parseInt(value.split(":")[0]), Integer.parseInt(value.split(":")[1])));
            cardAmount = userCards.size();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (newDeckID > 0) {
            try {
                PreparedStatement preparedStatement = conn.prepareStatement("SELECT * FROM decks WHERE deck_id = ?");
                preparedStatement.setInt(1, deckID);
                ResultSet resultSet = preparedStatement.executeQuery();
                resultSet.next();

                deckNameField = new TextField(resultSet.getString("name"), game.getTextFieldStyle());
                deckNameField.setDisabled(true);

                // считывание колоды по DeckID
                for (String value : resultSet.getString("cards").split(","))
                    deckCards.put(Integer.parseInt(value.split(":")[0]),
                            getUserCardByID(Integer.parseInt(value.split(":")[0]),
                                    Integer.parseInt(value.split(":")[1])));
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        } else {
            deckNameField = new TextField("", game.getTextFieldStyle());
            // создание пустого объекта колоды
            for (int value = 1; value < cardAmount + 1; value++)
                deckCards.put(value, getUserCardByID(value, 0));
        }

        // заполнение статистики текущей колоды
        for (Card deckCard : deckCards.values()) {
            deckSize += deckCard.current_amount;
            if (deckCard.rareness == 2) rareCardsAmount += deckCard.current_amount;
            if (deckCard.rareness == 3) superRareCardsAmount += deckCard.current_amount;
            if (deckCard.type.equals("people")) peopleCardAmount += deckCard.current_amount;
            if (deckCard.type.equals("building")) buildingCardAmount += deckCard.current_amount;
            if (deckCard.type.equals("objective")) objectiveAmount += deckCard.current_amount;
        }

        objectiveCountLabel = new Label("Целей в колоде:\n" + objectiveAmount + "/7", game.getMainLabelStyle());
        objectiveCountLabel.setPosition(1600, 730);
        objectiveCountLabel.setWidth(300);
        objectiveCountLabel.setAlignment(Align.center);
        objectiveCountLabel.setWrap(true);
        stage.addActor(objectiveCountLabel);
        rareCardsCountLabel = new Label("Редких карт:\n" + rareCardsAmount + "/15", game.getMainLabelStyle());
        rareCardsCountLabel.setPosition(1600, 275);
        rareCardsCountLabel.setWidth(300);
        rareCardsCountLabel.setAlignment(Align.center);
        rareCardsCountLabel.setWrap(true);
        stage.addActor(rareCardsCountLabel);
        superRareCardsCountLabel = new Label("Очень редких карт: " + superRareCardsAmount + "/5", game.getMainLabelStyle());
        superRareCardsCountLabel.setPosition(1600, 155);
        superRareCardsCountLabel.setWidth(300);
        superRareCardsCountLabel.setAlignment(Align.center);
        superRareCardsCountLabel.setWrap(true);
        stage.addActor(superRareCardsCountLabel);
        peopleCardsCountLabel = new Label("Карт людей: 5/" + peopleCardAmount + "/10", game.getMainLabelStyle());
        peopleCardsCountLabel.setPosition(1600, 610);
        peopleCardsCountLabel.setWidth(300);
        peopleCardsCountLabel.setAlignment(Align.center);
        peopleCardsCountLabel.setWrap(true);
        stage.addActor(peopleCardsCountLabel);
        buildingsCardsCountLabel = new Label("Карт построек: 3/" + buildingCardAmount + "/10", game.getMainLabelStyle());
        buildingsCardsCountLabel.setPosition(1600, 455);
        buildingsCardsCountLabel.setWidth(300);
        buildingsCardsCountLabel.setAlignment(Align.center);
        buildingsCardsCountLabel.setWrap(true);
        stage.addActor(buildingsCardsCountLabel);
        deckSizeCountLabel = new Label("Карт в колоде:\n" + deckSize + "/50", game.getMainLabelStyle());
        deckSizeCountLabel.setPosition(1600, 900);
        deckSizeCountLabel.setWidth(300);
        deckSizeCountLabel.setAlignment(Align.center);
        deckSizeCountLabel.setWrap(true);
        stage.addActor(deckSizeCountLabel);

        deckNameField.setMessageText("Введите имя колоды");
        deckNameField.setMaxLength(16);
        deckNameField.setWidth(600f);
        deckNameField.setPosition(330, stage.getHeight() - 70);
        stage.addActor(deckNameField);

        deckNameLabel = new Label("Имя колоды:", game.getMainLabelStyle());
        deckNameLabel.setPosition(50, stage.getHeight() - 70);
        stage.addActor(deckNameLabel);

        exitButton = new TextButton("Выйти без сохранения", game.getTextButtonStyle());
        exitButton.setPosition(50, stage.getHeight() - 1060);
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new ProfileScreen(game));
            }
        });
        stage.addActor(exitButton);

        // переписать принцип работы
        saveButton = new TextButton("Сохранить", game.getTextButtonStyle());
        saveButton.setPosition(stage.getWidth() - saveButton.getWidth() - 50, stage.getHeight() - 1060);
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    if (!(deckSize != 50 || objectiveAmount != 7 || superRareCardsAmount > 5 || rareCardsAmount > 15 ||
                            peopleCardAmount > 10 || peopleCardAmount < 5 || buildingCardAmount > 10
                            || buildingCardAmount < 3 || deckNameField.getText().trim().equals("")
                            || deckNameField.getText().contains(";") || deckNameField.getText().contains(" "))) {
                        StringBuilder newCards = new StringBuilder();
                        for (Card tempCard : deckCards.values())
                            newCards.append(tempCard.card_id).append(":")
                                    .append(tempCard.current_amount).append(",");
                        newCards.deleteCharAt(newCards.length() - 1);

                        try {
                            PreparedStatement preparedStatement;
                            if (deckID > 0) {
                                preparedStatement = conn.prepareStatement("UPDATE decks SET cards = ? WHERE deck_id = ?");
                                preparedStatement.setString(1, String.valueOf(newCards));
                                preparedStatement.setInt(2, deckID);
                            } else {
                                preparedStatement = conn.prepareStatement("INSERT INTO decks (nickname, name, cards) VALUES (?, ?, ?)");
                                preparedStatement.setString(1, game.getCurrentUserName());
                                preparedStatement.setString(2, deckNameField.getText().trim());
                                preparedStatement.setString(3, String.valueOf(newCards));
                            }
                            preparedStatement.executeUpdate();
                            game.setScreen(new ProfileScreen(game));
                        } catch (Exception e) {
                            e.printStackTrace();
                            errorPurchaseDialog();
                        }
                    } else {
                        errorPurchaseDialog();
                    }
                }
            }
        });
        stage.addActor(saveButton);

        cardTable = new Table();
        fillCards();
        cardPane = new ScrollPane(cardTable, neonSkin);
        cardPane.setOverscroll(false, true);
        cardPane.setScrollingDisabled(true, false);
        cardContainerTable = new Table();
        cardContainerTable.add(cardPane);
        stage.addActor(cardContainerTable);
        cardContainerTable.setPosition(0, 100);
        cardContainerTable.setSize(1700, 900);
    }

    @Override
    public void render(float delta) {
        game.setButtonPressed(false);
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(background, 0, 0);
        batch.end();

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 60f));
        stage.draw();

        if (isCardStageActive) {
            cardStage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 60f));
            cardStage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() { dispose();
    }

    @Override
    public void show() {

    }

    @Override
    public void dispose() {
        background.dispose();
        stage.dispose();
    }

    public void fillCards() {
        for (int i = 0; i < (cardAmount / 7 + (cardAmount % 7 == 0 ? 0 : 1)); i++) {
            int j = 1;
            cardTable.row();
            while (j % 8 != 0) {
                try {
                    final Image tempImage = new Image(new Texture(Gdx.files.internal("Cards/card_" + (j + (i * 7)) + ".png")));
                    tempImage.setName(j + (i * 7) + "");
                    int finalJ = j;
                    int finalI = i;
                    tempImage.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            // Открыть сведения о карте в новом Stage
                            fillCurrentCardStage(getCardByID(finalJ + (finalI * 7)));
                            Gdx.input.setInputProcessor(cardStage);
                            isCardStageActive = true;
                        }
                    });
                    cardTable.add(tempImage);
                    j++;
                } catch (Exception e) {
                    break;
                }
            }
            j = 1;
            cardTable.row();
            while (j % 8 != 0) {
                try {
                    Label tempLabel = new Label(userCards.get((j + (i * 7))).name, game.getSmallLabelStyle());
                    tempLabel.setName(j + (i * 7) + "");
                    cardTable.add(tempLabel);
                    j++;
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    public Card getCardByID(int id) {
        PreparedStatement cardPreparedStatement;
        try {
            cardPreparedStatement = conn.prepareStatement("SELECT * FROM cards WHERE card_id = ?");
            cardPreparedStatement.setInt(1, id);
            ResultSet cardResultSet = cardPreparedStatement.executeQuery();
            cardResultSet.next();
            return new Card(cardResultSet.getInt(1),
                    cardResultSet.getString(2),
                    cardResultSet.getString(3),
                    cardResultSet.getString(4),
                    cardResultSet.getString(5),
                    cardResultSet.getInt(6),
                    cardResultSet.getString(7),
                    cardResultSet.getString(8),
                    cardResultSet.getInt(9),
                    cardResultSet.getInt(10),
                    cardResultSet.getInt(11),
                    cardResultSet.getInt(12),
                    cardResultSet.getInt(13),
                    cardResultSet.getInt(14),
                    cardResultSet.getInt(15));
        } catch (SQLException exception) {
            return null;
        }
    }

    public void fillCurrentCardStage(Card currentCard) {
        currentCard = userCards.get(currentCard.card_id);
        Card finalCurrentCard = currentCard;
        cardStage = new Stage();
        changeUIVisibilityIfCardStage(false);

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
        cardDesc.setPosition(475, 650);
        cardDesc.setWidth(1000);
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
        cardPrice.setWidth(400);
        cardPrice.setWrap(true);
        cardPrice.setAlignment(Align.center);
        cardStage.addActor(cardPrice);

        TextButton closeButton = new TextButton("Закрыть", game.getTextButtonStyle());
        closeButton.setPosition(160, 250);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                changeUIVisibilityIfCardStage(true);
                isCardStageActive = false;
                Gdx.input.setInputProcessor(stage);
                cardStage.dispose();
            }
        });
        cardStage.addActor(closeButton);

        TextButton addButton = new TextButton("Добавить " +
                deckCards.get(currentCard.card_id).current_amount +
                "/" + currentCard.current_amount,
                game.getTextButtonStyle());
        addButton.setPosition(575, 450);
        addButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (deckCards.get(finalCurrentCard.card_id).current_amount < finalCurrentCard.current_amount) {
                    Card tempCard = deckCards.get(finalCurrentCard.card_id);
                    tempCard.current_amount += 1;
                    deckCards.replace(finalCurrentCard.card_id, tempCard);
                    deckSize += 1;
                    if (finalCurrentCard.rareness == 2) rareCardsAmount++;
                    if (finalCurrentCard.rareness == 3) superRareCardsAmount++;
                    if (finalCurrentCard.type.equals("people")) peopleCardAmount++;
                    if (finalCurrentCard.type.equals("building")) buildingCardAmount++;
                    if (finalCurrentCard.type.equals("objective")) objectiveAmount++;

                    objectiveCountLabel.setText("Целей в колоде:\n" + objectiveAmount + "/7");
                    rareCardsCountLabel.setText("Редких карт:\n" + rareCardsAmount + "/15");
                    superRareCardsCountLabel.setText("Очень редких карт: " + superRareCardsAmount + "/5");
                    peopleCardsCountLabel.setText("Карт людей: 5/" + peopleCardAmount + "/10");
                    buildingsCardsCountLabel.setText("Карт построек: 3/" + buildingCardAmount + "/10");
                    deckSizeCountLabel.setText("Карт в колоде:\n" + deckSize + "/50");

                    addButton.setText("Добавить " +
                            deckCards.get(finalCurrentCard.card_id).current_amount +
                            "/" + finalCurrentCard.current_amount);
                }
            }
        });
        cardStage.addActor(addButton);

        TextButton minusButton = new TextButton("Убрать из колоды", game.getTextButtonStyle());
        minusButton.setPosition(975, 450);
        minusButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (deckCards.get(finalCurrentCard.card_id).current_amount > 0) {
                    Card tempCard = deckCards.get(finalCurrentCard.card_id);
                    tempCard.current_amount -= 1;
                    deckCards.replace(finalCurrentCard.card_id, tempCard);
                    deckSize -= 1;
                    if (finalCurrentCard.rareness == 2) rareCardsAmount--;
                    if (finalCurrentCard.rareness == 3) superRareCardsAmount--;
                    if (finalCurrentCard.type.equals("people")) peopleCardAmount--;
                    if (finalCurrentCard.type.equals("building")) buildingCardAmount--;
                    if (finalCurrentCard.type.equals("objective")) objectiveAmount--;

                    objectiveCountLabel.setText("Целей в колоде:\n" + objectiveAmount + "/7");
                    rareCardsCountLabel.setText("Редких карт:\n" + rareCardsAmount + "/15");
                    superRareCardsCountLabel.setText("Очень редких карт: " + superRareCardsAmount + "/5");
                    peopleCardsCountLabel.setText("Карт людей: 5/" + peopleCardAmount + "/10");
                    buildingsCardsCountLabel.setText("Карт построек: 3/" + buildingCardAmount + "/10");
                    deckSizeCountLabel.setText("Карт в колоде:\n" + deckSize + "/50");

                    addButton.setText("Добавить " +
                            deckCards.get(finalCurrentCard.card_id).current_amount +
                            "/" + finalCurrentCard.current_amount);
                }
            }
        });
        cardStage.addActor(minusButton);

        // покупка карточек
        TextButton buyButton = new TextButton("Купить:\n" + (currentCard.rareness == 1 ? 50 :
                currentCard.rareness == 2 ? 200 : 500) + " жетонов", game.getTextButtonStyle());
        final Integer price = currentCard.rareness == 1 ? 50 : currentCard.rareness == 2 ? 200 : 500;
        buyButton.setPosition(775, 300);
        buyButton.align(1);
        buyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (finalCurrentCard.current_amount < finalCurrentCard.deck_limit) {
                    Dialog dialog = new Dialog("\nКупить карточку?", game.getDialogWindowStyle());
                    dialog.getTitleLabel().setAlignment(Align.center);
                    dialog.getContentTable().add(new Label("\n\n     Для покупки этой\nкарточки необходимо:\n          "
                            + price + " жетонов", game.getNormalLabelStyle()));

                    TextButton yesButton = new TextButton("Да\n", game.getTextButtonStyle());
                    yesButton.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            try {
                                PreparedStatement preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE nickname = ?");
                                preparedStatement.setString(1, game.getCurrentUserName());
                                ResultSet resultSet = preparedStatement.executeQuery();
                                resultSet.next();
                                if (price <= resultSet.getInt("dogtags")) {
                                    preparedStatement = conn.prepareStatement("UPDATE users SET cards = ?, dogtags = dogtags - ? WHERE nickname = ?");

                                    Card tempCard = userCards.get(finalCurrentCard.card_id);
                                    tempCard.current_amount += 1;
                                    userCards.replace(finalCurrentCard.card_id, tempCard);

                                    StringBuilder newCards = new StringBuilder();
                                    for (Card tempTempCard : userCards.values())
                                        newCards.append(tempTempCard.card_id).append(":")
                                                .append(tempTempCard.current_amount).append(",");

                                    preparedStatement.setString(1, String.valueOf(newCards));
                                    preparedStatement.setInt(2, price);
                                    preparedStatement.setString(3, game.getCurrentUserName());
                                    preparedStatement.executeUpdate();

                                    showSuccessfulPurchaseDialog();
                                    addButton.setText("Добавить " +
                                            deckCards.get(finalCurrentCard.card_id).current_amount +
                                            "/" + finalCurrentCard.current_amount);
                                } else {
                                    errorPurchaseDialog();
                                }
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                            }
                            dialog.hide();
                        }
                    });

                    TextButton noButton = new TextButton("Нет\n", game.getTextButtonStyle());
                    noButton.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            dialog.hide();
                        }
                    });

                    dialog.getButtonTable().add(yesButton);
                    dialog.getButtonTable().add(noButton);

                    dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));

                    dialog.show(cardStage);
                } else {
                    errorLimitDialog();
                }
            }
        });
        cardStage.addActor(buyButton);
    }

    public void changeUIVisibilityIfCardStage(boolean status) {
        deckNameField.setVisible(status);
        saveButton.setVisible(status);
        exitButton.setVisible(status);
        deckNameLabel.setVisible(status);
        cardPane.setVisible(status);
        cardTable.setVisible(status);
        cardContainerTable.setVisible(status);
    }

    public Card getUserCardByID(int id, int currentAmount) {
        PreparedStatement cardPreparedStatement;
        try {
            cardPreparedStatement = conn.prepareStatement("SELECT * FROM cards WHERE card_id = ?");
            cardPreparedStatement.setInt(1, id);
            ResultSet cardResultSet = cardPreparedStatement.executeQuery();
            cardResultSet.next();
            return new Card(cardResultSet.getInt("card_id"),
                    cardResultSet.getString("name"),
                    cardResultSet.getString("image_path"),
                    cardResultSet.getString("type"),
                    cardResultSet.getString("description"),
                    cardResultSet.getInt("deck_limit"),
                    cardResultSet.getString("statuses"),
                    cardResultSet.getString("cost_type"),
                    cardResultSet.getInt("health_status"),
                    cardResultSet.getInt("effect_number"),
                    cardResultSet.getInt("price"),
                    cardResultSet.getInt("rareness"),
                    cardResultSet.getInt("attack"),
                    cardResultSet.getInt("defence"),
                    cardResultSet.getInt("stealth"),
                    currentAmount);
        } catch (SQLException exception) {
            return null;
        }
    }

    public void showSuccessfulPurchaseDialog() {
        purchaseDialog("\n Успешная покупка ");
    }

    public void errorPurchaseDialog() {
        purchaseDialog("\n Не выполнены условия ");
    }

    public void errorLimitDialog() {
        purchaseDialog("\n У вас уже максимум карт ");
    }

    public void purchaseDialog(String newTitle) {
        Dialog dialog = new Dialog(newTitle, game.getDialogWindowStyle());
        dialog.getTitleLabel().setAlignment(Align.center);
        TextButton closeButton = new TextButton("Закрыть\n\n", game.getTextButtonStyle());
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });

        dialog.getButtonTable().add(closeButton);
        dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));

        if (isCardStageActive) {
            dialog.show(cardStage);
        } else {
            dialog.show(stage);
        }
    }
}
