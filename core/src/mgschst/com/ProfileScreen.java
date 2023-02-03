package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class ProfileScreen implements Screen {
    final MainMgschst game;
    OrthographicCamera camera;
    Texture background;
    Texture profilePicture;
    Texture experienceBar;
    Texture experienceProgress;
    Texture dogtag;
    Texture rankIcon;
    TextButton exitGameButton;
    Image cardBox;

    ScrollPane cardPane;
    Table cardTable;
    Table cardContainerTable;

    ScrollPane boardPane;
    Table boardTable;
    Table boardContainerTable;

    ScrollPane avaPane;
    Table avaTable;
    Table avaContainerTable;

    ScrollPane deckPane;
    Table deckTable;
    Table deckContainerTable;

    Connection conn = new DatabaseHandler().getConnection();
    Skin neonSkin = new Skin(Gdx.files.internal("Skins/neon/skin/neon-ui.json"));

    String nickname;
    Integer level;
    Integer experience;
    Integer rating;
    Integer dogtags;
    String cardPicturePath;
    String boardPicturePath;
    String profilePicturePath;
    String rankName;
    Integer activeDeckID;
    HashMap<Integer, String> ranks = new HashMap<>();

    final Integer cardStyleAmount = 4;
    final Integer boardStyleAmount = 3;
    final Integer avaStyleAmount = 8;

    HashMap<String, UnlockCondition> conditions = new HashMap<>();

    public ProfileScreen(final MainMgschst game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);

        fillConditions();

        background = new Texture(Gdx.files.internal("ProfileAssets/profile_bg.jpg"));
        experienceBar = new Texture(Gdx.files.internal("Images/experience_bar.png"));
        experienceProgress = new Texture(Gdx.files.internal("Images/experience_progress.png"));
        dogtag = new Texture(Gdx.files.internal("Images/dogtag.png"));

        try {
            PreparedStatement preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE nickname = ?");
            preparedStatement.setString(1, game.getCurrentUserName());
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            profilePicture = new Texture(Gdx.files.internal("UserInfo/Avatars/" + resultSet.getString("profile_picture_path")));
            nickname = resultSet.getString("nickname");
            level = resultSet.getInt("level");
            experience = resultSet.getInt("experience");
            rating = resultSet.getInt("rating");
            dogtags = resultSet.getInt("dogtags");
            cardPicturePath = resultSet.getString("card_picture_path");
            boardPicturePath = resultSet.getString("board_picture_path");
            profilePicturePath = resultSet.getString("profile_picture_path");
            rankIcon = new Texture(Gdx.files.internal("UserInfo/Ranks/rank_" + ((rating - 99) / 100) + ".png"));
            activeDeckID = resultSet.getInt("active_deck");
            fillRanks();

            for (int i = 99; i <= 999; i += 100) {
                if (i >= rating) {
                    rankName = ranks.get(i);
                    break;
                }
            }
        } catch (SQLException throwables) {
            profilePicture = new Texture(Gdx.files.internal("UserInfo/Avatars/ava_0.png"));
            nickname = "Error on load";
            level = 0;
            experience = 0;
            rating = 0;
            dogtags = 0;
            cardPicturePath = "card_0.png";
            boardPicturePath = "board_0.png";
            profilePicturePath = "ava_0.png";
            rankIcon = new Texture(Gdx.files.internal("UserInfo/Ranks/rank_0.png"));
            rankName = "Error on load";
            activeDeckID = 0;
            throwables.printStackTrace();
        }

        game.stage = new Stage();
        Gdx.input.setInputProcessor(game.stage);

        exitGameButton = new TextButton("Выйти", game.getTextButtonStyle());
        game.stage.addActor(exitGameButton);
        exitGameButton.setPosition(game.stage.getWidth() / 2, game.stage.getHeight() - 550);

        exitGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dispose();
                    game.setScreen(new MainMenuScreen(game));
                }
            }
        });

        cardTable = new Table();
        fillCards();
        cardPane = new ScrollPane(cardTable, neonSkin);
        cardPane.setOverscroll(true, false);
        cardPane.setScrollingDisabled(false, true);
        cardContainerTable = new Table();
        cardContainerTable.add(cardPane).width(600f).height(310f);
        cardContainerTable.row();
        game.stage.addActor(cardContainerTable);
        cardContainerTable.setPosition(game.stage.getWidth() - 450, game.stage.getHeight() - 300);

        boardTable = new Table();
        fillBoards();
        boardPane = new ScrollPane(boardTable, neonSkin);
        boardPane.setOverscroll(true, false);
        boardPane.setScrollingDisabled(false, true);
        boardContainerTable = new Table();
        boardContainerTable.add(boardPane).width(600f).height(330f);
        boardContainerTable.row();
        game.stage.addActor(boardContainerTable);
        boardContainerTable.setPosition(game.stage.getWidth() - 450, game.stage.getHeight() - 655);

        avaTable = new Table();
        fillAvas();
        avaPane = new ScrollPane(avaTable, neonSkin);
        avaPane.setOverscroll(true, false);
        avaPane.setScrollingDisabled(false, true);
        avaContainerTable = new Table();
        avaContainerTable.add(avaPane).width(600f).height(160f);
        avaContainerTable.row();
        game.stage.addActor(avaContainerTable);
        avaContainerTable.setPosition(game.stage.getWidth() - 450, game.stage.getHeight() - 940);

        deckTable = new Table();
        fillDecks();
        deckPane = new ScrollPane(deckTable, neonSkin);
        deckPane.setOverscroll(false, true);
        deckPane.setScrollingDisabled(true, false);
        deckContainerTable = new Table();
        deckContainerTable.add(deckPane).width(700f).height(400f);
        deckContainerTable.row();
        game.stage.addActor(deckContainerTable);
        deckContainerTable.setPosition(350, game.stage.getHeight() - 660);

        cardBox = new Image(new Texture(Gdx.files.internal("ProfileAssets/cardbox.png")));
        cardBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final Dialog dialog = new Dialog("\n Подтвердить покупку? ", game.getDialogWindowStyle());
                dialog.getTitleLabel().setAlignment(Align.center);

                dialog.getContentTable().add(new Label("\n\nДля покупки 3 случайных карт\n        необходимо 100 жетонов",
                        game.getNormalLabelStyle()));

                TextButton yesButton = new TextButton("Да\n", game.getTextButtonStyle());
                yesButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        if (game.isButtonPressed()) {
                            game.setButtonPressed(true);

                            if (dogtags >= 100) {
                                try {
                                    PreparedStatement preparedStatement = conn.prepareStatement("UPDATE users SET dogtags = dogtags - 100 WHERE nickname = ?");
                                    preparedStatement.setString(1, game.getCurrentUserName());
                                    preparedStatement.executeUpdate();

                                    // выбор карт для выдачи
                                    preparedStatement = conn.prepareStatement("SELECT * FROM cards");
                                    ResultSet resultSet = preparedStatement.executeQuery();
                                    ArrayList<Integer> commonCards = new ArrayList<>();
                                    ArrayList<Integer> rareCards = new ArrayList<>();
                                    ArrayList<Integer> superRareCards = new ArrayList<>();
                                    while (resultSet.next()) {
                                        if (resultSet.getInt("rareness") == 1) {
                                            commonCards.add(resultSet.getInt(1));
                                        } else if (resultSet.getInt("rareness") == 2) {
                                            rareCards.add(resultSet.getInt(1));
                                        } else if (resultSet.getInt("rareness") == 3) {
                                            superRareCards.add(resultSet.getInt(1));
                                        }
                                    }

                                    ArrayList<Integer> resultCards = new ArrayList<>();
                                    for (int i = 0; i < 3; i++) {
                                        Random random = new Random();
                                        int chance = random.nextInt(100) + 1;
                                        if (chance <= 90) {
                                            resultCards.add(commonCards.get(random.nextInt(commonCards.size())));
                                        } else if (chance <= 99) {
                                            resultCards.add(rareCards.get(random.nextInt(rareCards.size())));
                                        } else {
                                            resultCards.add(superRareCards.get(random.nextInt(superRareCards.size())));
                                        }
                                    }
                                    // добавление карт в бд, продажа избытков
                                    preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE nickname = ?");
                                    preparedStatement.setString(1, game.getCurrentUserName());
                                    resultSet = preparedStatement.executeQuery();
                                    HashMap<Integer, Card> userCards = new HashMap<>();
                                    resultSet.next();
                                    for (String value : resultSet.getString("cards").split(","))
                                        userCards.put(Integer.parseInt(value.split(":")[0]),
                                                getUserCardByID(Integer.parseInt(value.split(":")[0]), Integer.parseInt(value.split(":")[1])));

                                    ArrayList<Integer> soldCards = new ArrayList<>();
                                    for (Integer number : resultCards) {
                                        if (userCards.get(number).current_amount + 1 > userCards.get(number).deck_limit) {
                                            // продать карту
                                            preparedStatement = conn.prepareStatement("UPDATE users SET dogtags = dogtags + ? WHERE nickname = ?");
                                            if (userCards.get(number).rareness == 1) {
                                                preparedStatement.setInt(1, 10);
                                            } else if (userCards.get(number).rareness == 2) {
                                                preparedStatement.setInt(1, 30);
                                            } else {
                                                preparedStatement.setInt(1, 50);
                                            }
                                            preparedStatement.setString(2, game.getCurrentUserName());
                                            preparedStatement.executeUpdate();
                                            soldCards.add(number);
                                        } else {
                                            // добавить карту
                                            Card tempCard = userCards.get(number);
                                            tempCard.current_amount += 1;
                                            userCards.replace(number, tempCard);
                                            soldCards.add(0);
                                        }
                                    }

                                    // обновить список карт у пользователя
                                    preparedStatement = conn.prepareStatement("UPDATE users SET cards = ? WHERE nickname = ?");
                                    StringBuilder newCards = new StringBuilder();
                                    for (Card tempCard : userCards.values())
                                        newCards.append(tempCard.card_id).append(":")
                                                .append(tempCard.current_amount).append(",");

                                    newCards.deleteCharAt(newCards.length() - 1);
                                    preparedStatement.setString(1, String.valueOf(newCards));
                                    preparedStatement.setString(2, game.getCurrentUserName());
                                    preparedStatement.executeUpdate();

                                    // визуальные приколы с открытием коробки
                                    openCrateDialog(resultCards.get(0), resultCards.get(1), resultCards.get(2), soldCards);
                                } catch (SQLException exception) {
                                    exception.printStackTrace();
                                }
                            } else {
                                errorPurchaseDialog();
                            }

                            dialog.hide();
                        }
                    }
                });
                TextButton noButton = new TextButton("Нет\n", game.getTextButtonStyle());
                noButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                        if (game.isButtonPressed()) {
                            game.setButtonPressed(true);
                            dialog.hide();
                        }
                    }
                });

                dialog.getButtonTable().add(yesButton);
                dialog.getButtonTable().add(noButton);

                dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));

                dialog.show(game.stage);
            }
        });
        cardBox.setPosition(game.stage.getWidth() / 2 - 200, game.stage.getHeight() - 900);
        game.stage.addActor(cardBox);
    }

    @Override
    public void render(float delta) {
        game.setButtonPressed(false);
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.draw(background, 0, 0);
        game.batch.draw(profilePicture, 100, game.stage.getHeight() - 228);
        game.mainFont.draw(game.batch, "Имя", 278, game.stage.getHeight() - 133);
        game.mainFont.draw(game.batch, nickname, 278, game.stage.getHeight() - 183);
        game.mainFont.draw(game.batch, "Уровень", 100, game.stage.getHeight() - 258);
        game.mainFont.draw(game.batch, level.toString(), 300, game.stage.getHeight() - 258);

        game.batch.draw(experienceBar, 100, game.stage.getHeight() - experienceBar.getHeight() - 300);
        game.batch.draw(experienceProgress, 103, game.stage.getHeight() - experienceProgress.getHeight() - 303, experienceProgress.getWidth() * ((float) experience / (level * 25)), experienceProgress.getHeight());
        game.mainFont.draw(game.batch, experience.toString(), 100, game.stage.getHeight() - 340);
        game.mainFont.draw(game.batch, String.valueOf(level * 25), 350, game.stage.getHeight() - 340);

        game.batch.draw(dogtag, 445, game.stage.getHeight() - dogtag.getHeight() - 254);
        game.mainFont.draw(game.batch, dogtags.toString(), 575, game.stage.getHeight() - dogtag.getHeight() - 200);

        game.batch.draw(rankIcon, 500, game.stage.getHeight() - 503);
        game.mainFont.draw(game.batch, "Ваш рейтинг: " + rating, 100, game.stage.getHeight() - 400);
        game.mainFont.draw(game.batch, rankName, 100, game.stage.getHeight() - 450);

        game.mainFont.draw(game.batch, "Рубашка карт", game.stage.getWidth() - 600, game.stage.getHeight() - 100);
        game.mainFont.draw(game.batch, "Игровое поле", game.stage.getWidth() - 600, game.stage.getHeight() - 455);
        game.mainFont.draw(game.batch, "Аватар", game.stage.getWidth() - 550, game.stage.getHeight() - 820);

        game.mainFont.draw(game.batch, "Колоды", 235, game.stage.getHeight() - 550);

        game.batch.end();

        game.stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 60f));
        game.stage.draw();
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
    public void hide() {
    }

    @Override
    public void show() {
    }

    @Override
    public void dispose() {
        background.dispose();
        game.stage.dispose();
    }

    public void fillRanks() {
        ranks.put(99, "Залётный I");
        ranks.put(199, "Залётный II");
        ranks.put(299, "Дикий I");
        ranks.put(399, "Дикий II");
        ranks.put(499, "Дикий III");
        ranks.put(599, "Заводской");
        ranks.put(699, "Живой куст");
        ranks.put(799, "Решала");
        ranks.put(899, "Торговец");
        ranks.put(999, "Смотритель");
    }

    public void fillCards() {
        for (int i = 0; i < cardStyleAmount; i++) {
            final Image tempImage = new Image(new Texture(Gdx.files.internal("UserInfo/Cards/card_" + i + ".png")));
            tempImage.setName("card_" + i + ".png");
            tempImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    startDialog(tempImage, "Рубаш");
                }
            });
            cardTable.add(tempImage);
        }
        cardTable.row();
        for (String name : cardTable.getCells().toString().substring(1, cardTable.getCells().toString().length() - 1).split(", ")) {
            if (cardPicturePath.equals(name)) {
                cardTable.add(new Label("Выбрано", game.getChosenLabelStyle()));
            } else {
                cardTable.add(new Label(conditions.get(name).userGet(), game.getSmallLabelStyle()));
            }
        }
    }

    public void fillBoards() {
        for (int i = 0; i < boardStyleAmount; i++) {
            final Image tempImage = new Image(new Texture(Gdx.files.internal("UserInfo/Boards/BoardProfile/board_" + i + ".png")));
            tempImage.setName("board_" + i + ".png");
            tempImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    startDialog(tempImage, "Дос");
                }
            });
            boardTable.add(tempImage);
        }
        boardTable.row();
        for (String name : boardTable.getCells().toString().substring(1, boardTable.getCells().toString().length() - 1).split(", ")) {
            if (boardPicturePath.equals(name)) {
                boardTable.add(new Label("Выбрано", game.getChosenLabelStyle()));
            } else {
                boardTable.add(new Label(conditions.get(name).userGet(), game.getSmallLabelStyle()));
            }
        }
    }

    public void fillAvas() {
        for (int i = 0; i < avaStyleAmount; i++) {
            final Image tempImage = new Image(new Texture(Gdx.files.internal("UserInfo/Avatars/ava_" + i + ".png")));
            tempImage.setName("ava_" + i + ".png");
            tempImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    startDialog(tempImage, "Аватар");
                }
            });
            avaTable.add(tempImage);
        }
        avaTable.row();
        for (String name : avaTable.getCells().toString().substring(1, avaTable.getCells().toString().length() - 1).split(", ")) {
            if (profilePicturePath.equals(name)) {
                avaTable.add(new Label("Выбрано", game.getChosenLabelStyle()));
            } else {
                avaTable.add(new Label(conditions.get(name).userGet(), game.getSmallLabelStyle()));
            }
        }
    }

    public void fillDecks() {
        try {
            PreparedStatement preparedStatement = conn.prepareStatement("SELECT * FROM decks WHERE nickname = ?");
            preparedStatement.setString(1, game.getCurrentUserName());
            final ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                final Label tempLabel;
                if (resultSet.getInt(1) == activeDeckID) {
                    tempLabel = new Label(resultSet.getString("name"), game.getChosenMainLabelStyle());
                } else {
                    tempLabel = new Label(resultSet.getString("name"), game.getMainLabelStyle());
                }
                tempLabel.setName(resultSet.getString("name") + " " + resultSet.getInt(1));
                tempLabel.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        try {
                            PreparedStatement preparedStatement = conn.prepareStatement("UPDATE users SET active_deck = ? WHERE nickname = ?");
                            preparedStatement.setInt(1, Integer.parseInt(tempLabel.getName().split(" ")[1]));
                            preparedStatement.setString(2, game.getCurrentUserName());
                            preparedStatement.executeUpdate();

                            dispose();
                            game.setScreen(new ProfileScreen(game));
                        } catch (SQLException exception) {
                            exception.printStackTrace();
                        }
                    }
                });
                deckTable.add(tempLabel);

                final Image editTempImage = new Image(new Texture(Gdx.files.internal("ProfileAssets/edit.png")));
                editTempImage.setName(resultSet.getString("name") + " " + resultSet.getInt(1));
                // открыть экран редактирования с id выбранной колоды
                editTempImage.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        dispose();
                        game.setScreen(new DeckBuildingScreen(game, Integer.parseInt(editTempImage.getName().split(" ")[1])));
                    }
                });
                deckTable.add(editTempImage);

                final Image deleteTempImage = new Image(new Texture(Gdx.files.internal("ProfileAssets/delete.png")));
                deleteTempImage.setName(resultSet.getString("name") + " " + resultSet.getInt(1));

                deleteTempImage.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        final Dialog dialog = new Dialog("\n Подтвердите удаление колоды ", game.getDialogWindowStyle());
                        dialog.getTitleLabel().setAlignment(Align.center);
                        dialog.getContentTable().add(new Label("\n\nВы хотите удалить колоду: " + deleteTempImage.getName().split(" ")[0],
                                game.getNormalLabelStyle()));

                        TextButton yesButton = new TextButton("Да\n", game.getTextButtonStyle());
                        yesButton.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                if (game.isButtonPressed()) {
                                    game.setButtonPressed(true);
                                    try {
                                        if (Integer.parseInt(deleteTempImage.getName().split(" ")[1]) == activeDeckID) {
                                            final Dialog dialog = new Dialog("\n Нельзя удалить активную колоду ", game.getDialogWindowStyle());
                                            dialog.getTitleLabel().setAlignment(Align.center);
                                            TextButton closeButton = new TextButton("Закрыть\n\n", game.getTextButtonStyle());
                                            closeButton.addListener(new ChangeListener() {
                                                @Override
                                                public void changed(ChangeEvent event, Actor actor) {
                                                    if (game.isButtonPressed()) {
                                                        game.setButtonPressed(true);
                                                        dialog.hide();
                                                    }
                                                }
                                            });
                                            dialog.getButtonTable().add(closeButton);
                                            dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));
                                            dialog.show(game.stage);
                                        } else {
                                            PreparedStatement preparedStatement = conn.prepareStatement("DELETE FROM decks WHERE deck_id = ?");
                                            preparedStatement.setInt(1, Integer.parseInt(deleteTempImage.getName().split(" ")[1]));
                                            preparedStatement.executeUpdate();
                                            dispose();
                                            game.setScreen(new ProfileScreen(game));
                                        }
                                    } catch (SQLException exception) {
                                        exception.printStackTrace();
                                    }
                                    dialog.hide();
                                }
                            }
                        });
                        TextButton noButton = new TextButton("Нет\n", game.getTextButtonStyle());
                        noButton.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                if (game.isButtonPressed()) {
                                    game.setButtonPressed(true);
                                    dialog.hide();
                                }
                            }
                        });

                        dialog.getButtonTable().add(yesButton);
                        dialog.getButtonTable().add(noButton);

                        dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));

                        dialog.show(game.stage);
                    }
                });

                deckTable.add(deleteTempImage);

                deckTable.row();
            }

            final Label tempLabel = new Label("Добавить новую", game.getMainLabelStyle());
            tempLabel.setName("addNewDeck 0");
            tempLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    dispose();
                    game.setScreen(new DeckBuildingScreen(game, 0));
                }
            });
            deckTable.add(tempLabel);
            Image tempImage = new Image(new Texture(Gdx.files.internal("ProfileAssets/left_plus.png")));
            tempImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    dispose();
                    game.setScreen(new DeckBuildingScreen(game, 0));
                }
            });
            deckTable.add(tempImage);
            tempImage = new Image(new Texture(Gdx.files.internal("ProfileAssets/right_plus.png")));
            tempImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    dispose();
                    game.setScreen(new DeckBuildingScreen(game, 0));
                }
            });
            deckTable.add(tempImage);


        } catch (SQLException exception) {
            exception.printStackTrace();
        }

    }

    public void fillConditions() {
        UnlockCondition tempUnlockCondition = new UnlockCondition("free", "profile");
        conditions.put("ava_0.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("rank", 2, "profile");
        conditions.put("ava_1.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("level", 2, "profile");
        conditions.put("ava_2.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("rank", 7, "profile");
        conditions.put("ava_3.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("buy", 100, "profile");
        conditions.put("ava_4.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("buy", 1000, "profile");
        conditions.put("ava_5.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("level", 4, "profile");
        conditions.put("ava_6.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("buy", 500, "profile");
        conditions.put("ava_7.png", tempUnlockCondition);

        tempUnlockCondition = new UnlockCondition("free", "board");
        conditions.put("board_0.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("level", 2, "board");
        conditions.put("board_1.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("buy", 100, "board");
        conditions.put("board_2.png", tempUnlockCondition);

        tempUnlockCondition = new UnlockCondition("free", "card");
        conditions.put("card_0.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("level", 2, "card");
        conditions.put("card_1.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("buy", 100, "card");
        conditions.put("card_2.png", tempUnlockCondition);
        tempUnlockCondition = new UnlockCondition("rank", 7, "card");
        conditions.put("card_3.png", tempUnlockCondition);
    }

    public void startDialog(Image tempImage, String word) {
        final Image currentImage = tempImage;
        String title = word.length() == 6 ? "\nСМЕНИТЬ " + word.toUpperCase() + "?" : "\nСМЕНИТЬ " + word.toUpperCase() + "КУ?";
        final Dialog dialog = new Dialog(title, game.getDialogWindowStyle());

        dialog.getTitleLabel().setAlignment(Align.center);
        if (word.length() == 3) {
            dialog.getContentTable().add(new Label("\n\n Для покупки этой\n" + word + "ки необходимо:",
                    game.getNormalLabelStyle()));
        } else if (word.length() == 5) {
            dialog.getContentTable().add(new Label("\n\n    Для покупки этой\n" + word + "ки необходимо:",
                    game.getNormalLabelStyle()));
        } else {
            dialog.getContentTable().add(new Label("\n\n   Для покупки этого\n" + word + "а необходимо:",
                    game.getNormalLabelStyle()));
        }

        dialog.getContentTable().row();
        dialog.getContentTable().add(new Label(conditions.get(tempImage.getName()).userGet(),
                game.getNormalLabelStyle()));

        TextButton yesButton = new TextButton("Да\n", game.getTextButtonStyle());
        yesButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);

                    if (!(currentImage.getName().equals(profilePicturePath)
                            || currentImage.getName().equals(cardPicturePath)
                            || currentImage.getName().equals(boardPicturePath))) {
                        try {
                            UnlockCondition tempUnlockCondition = conditions.get(currentImage.getName());
                            String tempType = tempUnlockCondition.getType();
                            int tempCost = tempUnlockCondition.getCost();
                            String tempProductType = tempUnlockCondition.getProductType();

                            switch (tempType) {
                                case "rank":
                                    tempCost = tempCost * 100 - 1;
                                    if (tempCost <= rating) {
                                        PreparedStatement preparedStatement;
                                        if (tempProductType.equals("card")) {
                                            preparedStatement = conn.prepareStatement("UPDATE users SET card_picture_path = ? WHERE nickname = ?");
                                        } else if (tempProductType.equals("board")) {
                                            preparedStatement = conn.prepareStatement("UPDATE users SET board_picture_path = ? WHERE nickname = ?");
                                        } else {
                                            preparedStatement = conn.prepareStatement("UPDATE users SET profile_picture_path = ? WHERE nickname = ?");
                                        }
                                        preparedStatement.setString(1, currentImage.getName());
                                        preparedStatement.setString(2, game.getCurrentUserName());
                                        preparedStatement.executeUpdate();

                                        showSuccessfulPurchaseDialog();
                                    } else {
                                        errorPurchaseDialog();
                                    }
                                    break;
                                case "level":
                                    if (tempCost <= level) {
                                        PreparedStatement preparedStatement;
                                        if (tempProductType.equals("card")) {
                                            preparedStatement = conn.prepareStatement("UPDATE users SET card_picture_path = ? WHERE nickname = ?");
                                        } else if (tempProductType.equals("board")) {
                                            preparedStatement = conn.prepareStatement("UPDATE users SET board_picture_path = ? WHERE nickname = ?");
                                        } else {
                                            preparedStatement = conn.prepareStatement("UPDATE users SET profile_picture_path = ? WHERE nickname = ?");
                                        }
                                        preparedStatement.setString(1, currentImage.getName());
                                        preparedStatement.setString(2, game.getCurrentUserName());
                                        preparedStatement.executeUpdate();

                                        showSuccessfulPurchaseDialog();
                                    } else {
                                        errorPurchaseDialog();
                                    }
                                    break;
                                case "buy":
                                    if (tempCost <= dogtags) {
                                        PreparedStatement preparedStatement;
                                        if (tempProductType.equals("card")) {
                                            preparedStatement = conn.prepareStatement("UPDATE users SET card_picture_path = ?, dogtags = ? WHERE nickname = ?");
                                        } else if (tempProductType.equals("board")) {
                                            preparedStatement = conn.prepareStatement("UPDATE users SET board_picture_path = ?, dogtags = ? WHERE nickname = ?");
                                        } else {
                                            preparedStatement = conn.prepareStatement("UPDATE users SET profile_picture_path = ?, dogtags = ? WHERE nickname = ?");
                                        }
                                        preparedStatement.setString(1, currentImage.getName());
                                        preparedStatement.setInt(2, dogtags - tempCost);
                                        preparedStatement.setString(3, game.getCurrentUserName());
                                        preparedStatement.executeUpdate();

                                        showSuccessfulPurchaseDialog();
                                    } else {
                                        errorPurchaseDialog();
                                    }
                                    break;
                                default:
                                    PreparedStatement preparedStatement;
                                    if (tempProductType.equals("card")) {
                                        preparedStatement = conn.prepareStatement("UPDATE users SET card_picture_path = ? WHERE nickname = ?");
                                    } else if (tempProductType.equals("board")) {
                                        preparedStatement = conn.prepareStatement("UPDATE users SET board_picture_path = ? WHERE nickname = ?");
                                    } else {
                                        preparedStatement = conn.prepareStatement("UPDATE users SET profile_picture_path = ? WHERE nickname = ?");
                                    }
                                    preparedStatement.setString(1, currentImage.getName());
                                    preparedStatement.setString(2, game.getCurrentUserName());
                                    preparedStatement.executeUpdate();
                                    showSuccessfulPurchaseDialog();
                                    break;
                            }
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    } else {
                        showSuccessfulPurchaseDialog();
                    }

                    dialog.hide();
                }
            }
        });
        TextButton noButton = new TextButton("Нет\n", game.getTextButtonStyle());
        noButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dialog.hide();
                }
            }
        });

        dialog.getButtonTable().add(yesButton);
        dialog.getButtonTable().add(noButton);

        dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));

        dialog.show(game.stage);
    }

    public void showSuccessfulPurchaseDialog() {
        purchaseDialog(true);
    }

    public void errorPurchaseDialog() {
        purchaseDialog(false);
    }

    public void purchaseDialog(boolean success) {
        final boolean tempSuccess = success;
        String title = tempSuccess ? "\n Успешная покупка " : "\n Не выполнены условия ";
        final Dialog dialog = new Dialog(title, game.getDialogWindowStyle());
        dialog.getTitleLabel().setAlignment(Align.center);
        TextButton closeButton = new TextButton("Закрыть\n\n", game.getTextButtonStyle());
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dialog.hide();
                    if (tempSuccess) {
                        dispose();
                        game.setScreen(new ProfileScreen(game));
                    }
                }
            }
        });

        dialog.getButtonTable().add(closeButton);
        dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));

        dialog.show(game.stage);
    }

    public Card getCardByID(int id) {
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
                    cardResultSet.getInt("stealth"));
        } catch (SQLException exception) {
            return null;
        }
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

    public void openCrateDialog(int firstCardID, int secondCardID, int thirdCardID, ArrayList<Integer> soldCards) {
        final Dialog dialog = new Dialog("\n\n\n\n\n Нажмите чтобы открыть ", game.getDialogWindowStyle());
        dialog.getTitleLabel().setAlignment(Align.center);

        final ArrayList<Integer> IDList = new ArrayList<>();
        IDList.add(firstCardID);
        IDList.add(secondCardID);
        IDList.add(thirdCardID);
        final ArrayList<Integer> tempSoldCards = new ArrayList<>(soldCards);

        final Image[] crateImage = {new Image(new Texture(Gdx.files.internal("ProfileAssets/сlosedCrateNoLight.png")))};
        crateImage[0].addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // звуковые эффекты открытия ящика
                Sound crateSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/crateSound.mp3"));
                crateSound.play(1.0f);
                Sound beerSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/beerSound.mp3"));
                beerSound.play(1.0f);
                // смена фона на открытый ящик, очистить надпись
                dialog.getContentTable().removeActor(crateImage[0]);
                dialog.getTitleLabel().setText("");
                dialog.setBackground(new TextureRegionDrawable
                        (new Texture(Gdx.files.internal("Images/open_crate_dialog_bg2.png"))));

                dialog.getContentTable().add(new Label(" ", game.getMainLabelStyle()));

                Image firstCard = new Image(new Texture(Gdx.files.internal("Cards/origs/card_" + IDList.get(0) + ".png")));
                Image secondCard = new Image(new Texture(Gdx.files.internal("Cards/origs/card_" + IDList.get(1) + ".png")));
                Image thirdCard = new Image(new Texture(Gdx.files.internal("Cards/origs/card_" + IDList.get(2) + ".png")));

                firstCard.setColor(firstCard.getColor().r, firstCard.getColor().g, firstCard.getColor().b, 0);
                secondCard.setColor(secondCard.getColor().r, secondCard.getColor().g, secondCard.getColor().b, 0);
                thirdCard.setColor(thirdCard.getColor().r, thirdCard.getColor().g, thirdCard.getColor().b, 0);

                firstCard.addAction(createAlphaAction());
                secondCard.addAction(createAlphaAction());
                thirdCard.addAction(createAlphaAction());

                dialog.getContentTable().add(firstCard);
                dialog.getContentTable().add(secondCard);
                dialog.getContentTable().add(thirdCard);
                dialog.getContentTable().add(new Label("  ", game.getMainLabelStyle()));

                dialog.getContentTable().row();
                dialog.getContentTable().add(new Label("  ", game.getMainLabelStyle()));
                dialog.getContentTable().add(new Label("  ", game.getMainLabelStyle()));

                int counter = 0;
                for (int i : tempSoldCards) {
                    String text;
                    counter += 1;
                    if (i > 0) {
                        text = "Продано за " +
                                (getCardByID(i).rareness == 1 ? 10 : getCardByID(i).rareness == 2 ? 30 : 100)
                                + " жетонов";
                    } else {
                        text = switch (counter) {
                            case 1 -> getCardByID(firstCardID).name;
                            case 2 -> getCardByID(secondCardID).name;
                            default -> getCardByID(thirdCardID).name;
                        };

                    }
                    Label tempLabel = new Label(text, game.getNormalLabelStyle());
                    tempLabel.setColor(tempLabel.getColor().r, tempLabel.getColor().g, tempLabel.getColor().b, 0);
                    tempLabel.addAction(createAlphaAction());
                    dialog.getContentTable().add(tempLabel);
                }
            }
        });
        dialog.getContentTable().add(crateImage[0]);

        TextButton closeButton = new TextButton("Закрыть\n\n", game.getTextButtonStyle());
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dialog.hide();
                    dispose();
                    game.setScreen(new ProfileScreen(game));
                }
            }
        });

        dialog.getButtonTable().add(closeButton);

        dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));

        dialog.show(game.stage);
    }

    public AlphaAction createAlphaAction() {
        AlphaAction tempAlphaAction = new AlphaAction();
        tempAlphaAction.setAlpha(1f);
        tempAlphaAction.setDuration(5f);
        return tempAlphaAction;
    }
}
