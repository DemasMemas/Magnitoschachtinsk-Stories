package mgschst.com.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import mgschst.com.MainMgschst;
import mgschst.com.UnlockCondition;
import mgschst.com.connect.DatabaseHandler;
import mgschst.com.dbObj.Card;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static mgschst.com.EffectHandler.makeNewCard;
import static mgschst.com.screens.DeckBuildingScreen.getCard;

public class ProfileScreen implements Screen {
    final MainMgschst game;
    final OrthographicCamera camera;
    final Batch batch;
    Stage stage;
    Image background;
    Image profilePicture;
    Image experienceBar;
    Image experienceProgress;
    Image dogtag;
    Image rankIcon;
    TextButton exitButton;
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
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        camera = game.getCamera();
        batch = game.batch;

        fillConditions();

        game.xScaler = stage.getWidth()/1920f;
        game.yScaler = stage.getHeight()/1080f;

        try {
            PreparedStatement preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE nickname = ?");
            preparedStatement.setString(1, game.getCurrentUserName());
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            profilePicture = new Image(new Texture(Gdx.files.internal("UserInfo/Avatars/" + resultSet.getString("profile_picture_path"))));
            nickname = resultSet.getString("nickname");
            level = resultSet.getInt("level");
            experience = resultSet.getInt("experience");
            rating = resultSet.getInt("rating");
            dogtags = resultSet.getInt("dogtags");
            cardPicturePath = resultSet.getString("card_picture_path");
            boardPicturePath = resultSet.getString("board_picture_path");
            profilePicturePath = resultSet.getString("profile_picture_path");
            rankIcon = new Image(new Texture(Gdx.files.internal("UserInfo/Ranks/rank_" + ((rating - 99) / 100) + ".png")));
            activeDeckID = resultSet.getInt("active_deck");
            fillRanks();

            for (int i = 99; i <= 999; i += 100) {
                if (i >= rating) {
                    rankName = ranks.get(i);
                    break;
                }
            }
        } catch (SQLException throwables) {
            profilePicture = new Image(new Texture(Gdx.files.internal("UserInfo/Avatars/ava_0.png")));
            nickname = "Error on load";
            level = 0;
            experience = 0;
            rating = 0;
            dogtags = 0;
            cardPicturePath = "card_0.png";
            boardPicturePath = "board_0.png";
            profilePicturePath = "ava_0.png";
            rankIcon = new Image(new Texture(Gdx.files.internal("UserInfo/Ranks/rank_0.png")));
            rankName = "Error on load";
            activeDeckID = 0;
            throwables.printStackTrace();
        }

        background = new Image(new Texture(Gdx.files.internal("ProfileAssets/profile_bg.jpg")));
        background.setPosition(0,0);
        experienceBar = new Image(new Texture(Gdx.files.internal("Images/experience_bar.png")));
        experienceBar.setPosition(100, 1080 - experienceBar.getHeight() - 300);
        experienceProgress = new Image(new Texture(Gdx.files.internal("Images/experience_progress.png")));
        experienceProgress.setPosition(103, 1080 - experienceProgress.getHeight() - 303);
        experienceProgress.setBounds(103, 1080 - experienceProgress.getHeight() - 303, experienceProgress.getWidth() * ((float) experience / (level * 25)), experienceProgress.getHeight());
        dogtag = new Image(new Texture(Gdx.files.internal("Images/dogtag.png")));
        dogtag.setPosition(445, 1080 - dogtag.getHeight() - 254);
        profilePicture.setPosition(100, 1080 - 228);
        rankIcon.setPosition(500, 1080 - 503);

        stage.addActor(background);
        stage.addActor(experienceBar);
        stage.addActor(experienceProgress);
        stage.addActor(dogtag);
        stage.addActor(profilePicture);
        stage.addActor(rankIcon);

        exitButton = new TextButton("??????????", game.getTextButtonStyle());
        stage.addActor(exitButton);
        exitButton.setPosition(960, 1080 - 550);

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenuScreen(game));
            }
        });

        cardTable = new Table();
        fillCards();
        cardPane = new ScrollPane(cardTable, neonSkin);
        cardPane.setOverscroll(true, false);
        cardPane.setScrollingDisabled(false, true);
        cardContainerTable = new Table();
        cardContainerTable.add(cardPane).width(600f * game.xScaler).height(310f * game.yScaler);
        cardContainerTable.row();
        stage.addActor(cardContainerTable);
        cardContainerTable.setPosition(1920 - 450, 1080 - 300);

        boardTable = new Table();
        fillBoards();
        boardPane = new ScrollPane(boardTable, neonSkin);
        boardPane.setOverscroll(true, false);
        boardPane.setScrollingDisabled(false, true);
        boardContainerTable = new Table();
        boardContainerTable.add(boardPane).width(600f * game.xScaler).height(330f * game.yScaler);
        boardContainerTable.row();
        stage.addActor(boardContainerTable);
        boardContainerTable.setPosition(1920 - 450, 1080 - 655);

        avaTable = new Table();
        fillAvas();
        avaPane = new ScrollPane(avaTable, neonSkin);
        avaPane.setOverscroll(true, false);
        avaPane.setScrollingDisabled(false, true);
        avaContainerTable = new Table();
        avaContainerTable.add(avaPane).width(600f * game.xScaler).height(160f * game.yScaler);
        avaContainerTable.row();
        stage.addActor(avaContainerTable);
        avaContainerTable.setPosition(1920 - 450, 1080 - 940);

        deckTable = new Table();
        fillDecks();
        deckPane = new ScrollPane(deckTable, neonSkin);
        deckPane.setOverscroll(false, true);
        deckPane.setScrollingDisabled(true, false);
        deckContainerTable = new Table();
        deckContainerTable.add(deckPane).width(700f * game.xScaler).height(400f * game.yScaler);
        deckContainerTable.row();
        stage.addActor(deckContainerTable);
        deckContainerTable.setPosition(350, 1080 - 660);

        cardBox = new Image(new Texture(Gdx.files.internal("ProfileAssets/cardbox.png")));
        cardBox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final Dialog dialog = new Dialog("\n ?????????????????????? ??????????????? ", game.getDialogWindowStyle());
                dialog.getTitleLabel().setAlignment(Align.center);

                dialog.getContentTable().add(new Label("\n\n?????? ?????????????? 3 ?????????????????? ????????\n        ???????????????????? 100 ??????????????",
                        game.getNormalLabelStyle()));

                TextButton yesButton = new TextButton("????\n", game.getTextButtonStyle());
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

                                    // ?????????? ???????? ?????? ????????????
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
                                    // ???????????????????? ???????? ?? ????, ?????????????? ????????????????
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
                                            // ?????????????? ??????????
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
                                            // ???????????????? ??????????
                                            Card tempCard = userCards.get(number);
                                            tempCard.current_amount += 1;
                                            userCards.replace(number, tempCard);
                                            soldCards.add(0);
                                        }
                                    }

                                    // ???????????????? ???????????? ???????? ?? ????????????????????????
                                    preparedStatement = conn.prepareStatement("UPDATE users SET cards = ? WHERE nickname = ?");
                                    StringBuilder newCards = new StringBuilder();
                                    for (Card tempCard : userCards.values())
                                        newCards.append(tempCard.card_id).append(":")
                                                .append(tempCard.current_amount).append(",");

                                    newCards.deleteCharAt(newCards.length() - 1);
                                    preparedStatement.setString(1, String.valueOf(newCards));
                                    preparedStatement.setString(2, game.getCurrentUserName());
                                    preparedStatement.executeUpdate();

                                    // ???????????????????? ?????????????? ?? ?????????????????? ??????????????
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
                TextButton noButton = new TextButton("??????\n", game.getTextButtonStyle());
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
                dialog.scaleBy(game.xScaler - 1, game.yScaler - 1);
                dialog.show(stage);
            }
        });
        cardBox.setPosition(860 - 200, 1080 - 900);
        stage.addActor(cardBox);

        Label tempLabel = new Label("??????", game.getMainLabelStyle());
        tempLabel.setPosition(278, 1080 - 158);
        stage.addActor(tempLabel);

        tempLabel = new Label(nickname, game.getMainLabelStyle());
        tempLabel.setPosition(278, 1080 - 208);
        stage.addActor(tempLabel);

        tempLabel = new Label("??????????????", game.getMainLabelStyle());
        tempLabel.setPosition(100, 1080 - 283);
        stage.addActor(tempLabel);

        tempLabel = new Label(level.toString(), game.getMainLabelStyle());
        tempLabel.setPosition(300, 1080 - 283);
        stage.addActor(tempLabel);

        tempLabel = new Label(experience.toString(), game.getMainLabelStyle());
        tempLabel.setPosition(100, 1080 - 365);
        stage.addActor(tempLabel);

        tempLabel = new Label(String.valueOf(level * 25), game.getMainLabelStyle());
        tempLabel.setPosition(350, 1080 - 365);
        stage.addActor(tempLabel);

        tempLabel = new Label(dogtags.toString(), game.getMainLabelStyle());
        tempLabel.setPosition(575, 1080 - dogtag.getHeight() - 225);
        stage.addActor(tempLabel);

        tempLabel = new Label("?????? ??????????????: " + rating, game.getMainLabelStyle());
        tempLabel.setPosition(100, 1080 - 425);
        stage.addActor(tempLabel);

        tempLabel = new Label(rankName, game.getMainLabelStyle());
        tempLabel.setPosition(100, 1080 - 475);
        stage.addActor(tempLabel);

        tempLabel = new Label("?????????????? ????????", game.getMainLabelStyle());
        tempLabel.setPosition(1920 - 600, 1080 - 125);
        stage.addActor(tempLabel);

        tempLabel = new Label("?????????????? ????????", game.getMainLabelStyle());
        tempLabel.setPosition(1920 - 600, 1080 - 480);
        stage.addActor(tempLabel);

        tempLabel = new Label("????????????", game.getMainLabelStyle());
        tempLabel.setPosition(1920 - 550, 1080 - 845);
        stage.addActor(tempLabel);

        tempLabel = new Label("????????????", game.getMainLabelStyle());
        tempLabel.setPosition(235, 1080 - 575);
        stage.addActor(tempLabel);

        for (Actor actor:stage.getActors()) {
            actor.scaleBy(game.xScaler - 1,  game.yScaler - 1);
            actor.setPosition(actor.getX() * game.xScaler, actor.getY() * game.yScaler);
        }
    }

    @Override
    public void render(float delta) {
        DeckBuildingScreen.renderScreen(game, camera, batch, stage);
    }

    @Override
    public void resize(int width, int height) { }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() {dispose(); }

    @Override
    public void show() { }

    @Override
    public void dispose() {
        stage.dispose();
    }

    public void fillRanks() {
        ranks.put(99, "???????????????? I");
        ranks.put(199, "???????????????? II");
        ranks.put(299, "?????????? I");
        ranks.put(399, "?????????? II");
        ranks.put(499, "?????????? III");
        ranks.put(599, "??????????????????");
        ranks.put(699, "?????????? ????????");
        ranks.put(799, "????????????");
        ranks.put(899, "????????????????");
        ranks.put(999, "????????????????????");
    }

    public void fillCards() {
        for (int i = 0; i < cardStyleAmount; i++) {
            final Image tempImage = new Image(new Texture(Gdx.files.internal("UserInfo/Cards/card_" + i + ".png")));
            tempImage.setName("card_" + i + ".png");
            tempImage.setSize(tempImage.getWidth() * game.xScaler, tempImage.getHeight() * game.yScaler);
            tempImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    startDialog(tempImage, "??????????");
                }
            });
            cardTable.add(tempImage);
        }
        makeTextRowInBuyTable(cardTable, cardPicturePath);
    }

    private void makeTextRowInBuyTable(Table table, String picturePath) {
        table.row();
        for (String name : table.getCells().toString().substring(1, table.getCells().toString().length() - 1).split(", ")) {
            if (picturePath.equals(name)) {
                table.add(new Label("??????????????", game.getChosenLabelStyle()));
            } else {
                table.add(new Label(conditions.get(name).userGet(), game.getSmallLabelStyle()));
            }
        }
    }

    public void fillBoards() {
        // ???????????? ?????????? - 533 / 300
        for (int i = 0; i < boardStyleAmount; i++) {
            final Image tempImage = new Image(new Texture(Gdx.files.internal("UserInfo/Boards/BoardProfile/board_" + i + ".png")));
            tempImage.setName("board_" + i + ".png");
            tempImage.setSize(tempImage.getWidth() * game.xScaler, tempImage.getHeight() * game.yScaler);
            tempImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    startDialog(tempImage, "??????");
                }
            });
            boardTable.add(tempImage);
        }
        makeTextRowInBuyTable(boardTable, boardPicturePath);
    }

    public void fillAvas() {
        for (int i = 0; i < avaStyleAmount; i++) {
            final Image tempImage = new Image(new Texture(Gdx.files.internal("UserInfo/Avatars/ava_" + i + ".png")));
            tempImage.setName("ava_" + i + ".png");
            tempImage.setSize(tempImage.getWidth() * game.xScaler, tempImage.getHeight() * game.yScaler);
            tempImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    startDialog(tempImage, "????????????");
                }
            });
            avaTable.add(tempImage);
        }
        makeTextRowInBuyTable(avaTable, profilePicturePath);
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

                            game.setScreen(new ProfileScreen(game));
                        } catch (SQLException exception) {
                            exception.printStackTrace();
                        }
                    }
                });
                deckTable.add(tempLabel);

                final Image editTempImage = new Image(new Texture(Gdx.files.internal("ProfileAssets/edit.png")));
                editTempImage.setName(resultSet.getString("name") + " " + resultSet.getInt(1));
                editTempImage.setSize(editTempImage.getWidth() * game.xScaler, editTempImage.getHeight() * game.yScaler);
                // ?????????????? ?????????? ???????????????????????????? ?? id ?????????????????? ????????????
                editTempImage.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        game.setScreen(new DeckBuildingScreen(game, Integer.parseInt(editTempImage.getName().split(" ")[1])));
                    }
                });
                deckTable.add(editTempImage);

                final Image deleteTempImage = new Image(new Texture(Gdx.files.internal("ProfileAssets/delete.png")));
                deleteTempImage.setName(resultSet.getString("name") + " " + resultSet.getInt(1));
                deleteTempImage.setSize(deleteTempImage.getWidth() * game.xScaler, deleteTempImage.getHeight() * game.yScaler);
                deleteTempImage.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        final Dialog dialog = new Dialog("\n ?????????????????????? ???????????????? ???????????? ", game.getDialogWindowStyle());
                        dialog.getTitleLabel().setAlignment(Align.center);
                        dialog.getContentTable().add(new Label("\n\n???? ???????????? ?????????????? ????????????: " + deleteTempImage.getName().split(" ")[0],
                                game.getNormalLabelStyle()));

                        TextButton yesButton = new TextButton("????\n", game.getTextButtonStyle());
                        yesButton.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                if (game.isButtonPressed()) {
                                    game.setButtonPressed(true);
                                    try {
                                        if (Integer.parseInt(deleteTempImage.getName().split(" ")[1]) == activeDeckID) {
                                            final Dialog dialog = new Dialog("\n ???????????? ?????????????? ???????????????? ???????????? ", game.getDialogWindowStyle());
                                            dialog.getTitleLabel().setAlignment(Align.center);
                                            TextButton closeButton = new TextButton("??????????????\n\n", game.getTextButtonStyle());
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
                                            dialog.show(stage);
                                        } else {
                                            PreparedStatement preparedStatement = conn.prepareStatement("DELETE FROM decks WHERE deck_id = ?");
                                            preparedStatement.setInt(1, Integer.parseInt(deleteTempImage.getName().split(" ")[1]));
                                            preparedStatement.executeUpdate();
                                            game.setScreen(new ProfileScreen(game));
                                        }
                                    } catch (SQLException exception) {
                                        exception.printStackTrace();
                                    }
                                    dialog.hide();
                                }
                            }
                        });
                        endDialogCfg(dialog, yesButton);
                        dialog.scaleBy(game.xScaler - 1, game.yScaler - 1);
                        dialog.show(stage);
                    }
                });

                deckTable.add(deleteTempImage);

                deckTable.row();
            }

            final Label tempLabel = new Label("???????????????? ??????????", game.getMainLabelStyle());
            tempLabel.setName("addNewDeck 0");
            tempLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.setScreen(new DeckBuildingScreen(game, 0));
                }
            });
            deckTable.add(tempLabel);
            Image tempImage = new Image(new Texture(Gdx.files.internal("ProfileAssets/left_plus.png")));
            addDeckBuildingListenerToImage(tempImage);
            tempImage = new Image(new Texture(Gdx.files.internal("ProfileAssets/right_plus.png")));
            addDeckBuildingListenerToImage(tempImage);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

    }

    private void addDeckBuildingListenerToImage(Image tempImage) {
        tempImage.setSize(tempImage.getWidth() * game.xScaler, tempImage.getHeight() * game.yScaler);
        tempImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y)
            { game.setScreen(new DeckBuildingScreen(game, 0)); }
        });
        deckTable.add(tempImage);
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
        String title = word.length() == 6 ? "\n?????????????? " + word.toUpperCase() + "?" : "\n?????????????? " + word.toUpperCase() + "?????";
        final Dialog dialog = new Dialog(title, game.getDialogWindowStyle());

        dialog.getTitleLabel().setAlignment(Align.center);
        if (word.length() == 3) {
            dialog.getContentTable().add(new Label("\n\n ?????? ?????????????? ????????\n" + word + "???? ????????????????????:",
                    game.getNormalLabelStyle()));
        } else if (word.length() == 5) {
            dialog.getContentTable().add(new Label("\n\n    ?????? ?????????????? ????????\n" + word + "???? ????????????????????:",
                    game.getNormalLabelStyle()));
        } else {
            dialog.getContentTable().add(new Label("\n\n   ?????? ?????????????? ??????????\n" + word + "?? ????????????????????:",
                    game.getNormalLabelStyle()));
        }

        dialog.getContentTable().row();
        dialog.getContentTable().add(new Label(conditions.get(tempImage.getName()).userGet(),
                game.getNormalLabelStyle()));

        TextButton yesButton = new TextButton("????\n", game.getTextButtonStyle());
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
                                case "rank" -> {
                                    tempCost = tempCost * 100 - 1;
                                    noPayBuyChange(tempCost, tempProductType, rating, currentImage);
                                }
                                case "level" -> noPayBuyChange(tempCost, tempProductType, level, currentImage);
                                case "buy" -> {
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
                                }
                                default -> updateOnSuccessfulPurchase(tempProductType, currentImage);
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
        endDialogCfg(dialog, yesButton);

        dialog.show(stage);
    }

    private void updateOnSuccessfulPurchase(String tempProductType, Image currentImage) throws SQLException {
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
    }

    private void noPayBuyChange(int tempCost, String tempProductType, Integer rating, Image currentImage) throws SQLException {
        if (tempCost <= rating) updateOnSuccessfulPurchase(tempProductType, currentImage);
        else errorPurchaseDialog();
    }

    private void endDialogCfg(Dialog dialog, TextButton yesButton) {
        TextButton noButton = new TextButton("??????\n", game.getTextButtonStyle());
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
    }

    public void showSuccessfulPurchaseDialog() {
        purchaseDialog(true);
    }

    public void errorPurchaseDialog() {
        purchaseDialog(false);
    }

    public void purchaseDialog(boolean success) {
        final boolean tempSuccess = success;
        String title = tempSuccess ? "\n ???????????????? ?????????????? " : "\n ???? ?????????????????? ?????????????? ";
        final Dialog dialog = new Dialog(title, game.getDialogWindowStyle());
        dialog.getTitleLabel().setAlignment(Align.center);
        TextButton closeButton = new TextButton("??????????????\n\n", game.getTextButtonStyle());
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dialog.hide();
                    if (tempSuccess) {
                        game.setScreen(new ProfileScreen(game));
                    }
                }
            }
        });

        dialog.getButtonTable().add(closeButton);
        dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));
        dialog.scaleBy(game.xScaler - 1, game.yScaler - 1);
        dialog.show(stage);
    }

    public Card getCardByID(int id) {
        PreparedStatement cardPreparedStatement;
        try {
            cardPreparedStatement = conn.prepareStatement("SELECT * FROM cards WHERE card_id = ?");
            return makeNewCard(id, cardPreparedStatement);
        } catch (SQLException exception) {
            return null;
        }
    }

    public Card getUserCardByID(int id, int currentAmount) {
        return getCard(id, currentAmount, conn);
    }

    public void openCrateDialog(int firstCardID, int secondCardID, int thirdCardID, ArrayList<Integer> soldCards) {
        final Dialog dialog = new Dialog("\n\n\n\n\n ?????????????? ?????????? ?????????????? ", game.getDialogWindowStyle());
        dialog.getTitleLabel().setAlignment(Align.center);

        final ArrayList<Integer> IDList = new ArrayList<>();
        IDList.add(firstCardID);
        IDList.add(secondCardID);
        IDList.add(thirdCardID);
        final ArrayList<Integer> tempSoldCards = new ArrayList<>(soldCards);

        final Image[] crateImage = {new Image(new Texture(Gdx.files.internal("ProfileAssets/??losedCrateNoLight.png")))};
        crateImage[0].addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // ???????????????? ?????????????? ???????????????? ??????????
                Sound crateSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/crateSound.mp3"));
                crateSound.play(1.0f);
                Sound beerSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/beerSound.mp3"));
                beerSound.play(1.0f);
                // ?????????? ???????? ???? ???????????????? ????????, ???????????????? ??????????????
                dialog.getContentTable().removeActor(crateImage[0]);
                dialog.getTitleLabel().setText("");
                dialog.setBackground(new TextureRegionDrawable
                        (new Texture(Gdx.files.internal("Images/open_crate_dialog_bg2.png"))));

                dialog.getContentTable().add(new Label(" ", game.getMainLabelStyle()));

                Image firstCard = new Image(new Texture(Gdx.files.internal("Cards/origs/card_" + IDList.get(0) + ".png")));
                Image secondCard = new Image(new Texture(Gdx.files.internal("Cards/origs/card_" + IDList.get(1) + ".png")));
                Image thirdCard = new Image(new Texture(Gdx.files.internal("Cards/origs/card_" + IDList.get(2) + ".png")));

                firstCard.scaleBy(game.xScaler - 1, game.yScaler - 1);
                secondCard.scaleBy(game.xScaler - 1, game.yScaler - 1);
                thirdCard.scaleBy(game.xScaler - 1, game.yScaler - 1);

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
                        text = "?????????????? ???? " +
                                (getCardByID(i).rareness == 1 ? 10 : getCardByID(i).rareness == 2 ? 30 : 100)
                                + " ??????????????";
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

        TextButton closeButton = new TextButton("??????????????\n\n", game.getTextButtonStyle());
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dialog.hide();
                    game.setScreen(new ProfileScreen(game));
                }
            }
        });

        dialog.getButtonTable().add(closeButton);

        dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));

        dialog.scaleBy(game.xScaler - 1, game.yScaler - 1);
        dialog.show(stage);
    }

    public AlphaAction createAlphaAction() {
        AlphaAction tempAlphaAction = new AlphaAction();
        tempAlphaAction.setAlpha(1f);
        tempAlphaAction.setDuration(5f);
        return tempAlphaAction;
    }
}
