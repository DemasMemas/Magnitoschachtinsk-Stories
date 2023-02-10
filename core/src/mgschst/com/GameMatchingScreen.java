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
import java.util.ArrayList;

public class GameMatchingScreen implements Screen {
    final MainMgschst game;
    final OrthographicCamera camera;
    final Batch batch;
    Stage stage;

    Texture background;

    TextButton exitButton;
    TextButton refreshButton;

    VerticalGroup gameGroup;
    Table gameContainerTable;
    ScrollPane gameScrollPane;
    Skin neonSkin = new Skin(Gdx.files.internal("Skins/neon/skin/neon-ui.json"));
    Connection conn = new DatabaseHandler().getConnection();

    TCPConnection currentPlayerConnection;

    public GameMatchingScreen(final MainMgschst game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        camera = game.getCamera();
        batch = game.batch;;

        background = new Texture(Gdx.files.internal("MenuAssets/game_matching_bg.jpg"));

        currentPlayerConnection = game.getPlayerConnection();
        currentPlayerConnection.sendString("getGames");


        exitButton = new TextButton("Выйти", game.getTextButtonStyle());
        stage.addActor(exitButton);
        exitButton.setPosition(100, 100);

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenuScreen(game));
            }
        });

        refreshButton = new TextButton("Обновить", game.getTextButtonStyle());
        stage.addActor(refreshButton);
        refreshButton.setPosition(500, 100);

        refreshButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    currentPlayerConnection.sendString("getGames");
                }
            }
        });

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
    }

    public void setUpGameList(ArrayList<String> gameList) {
        stage.getActors().removeValue(gameContainerTable, true);
        gameGroup = new VerticalGroup();
        gameGroup.pad(25f);
        gameGroup.addActor(new Label("Список текущих игр", game.getMainLabelStyle()));
        gameGroup.addActor(new Label("--------------------------", game.getMainLabelStyle()));

        int counter = 0;
        String tempFirstName = "";
        String tempSecondName = "";
        int tempGameID = 0;

        for (String value : gameList) {
            counter++;
            if (counter == 1) {
                tempFirstName = value;
            }
            if (counter == 2) {
                tempSecondName = value;
            }
            if (counter == 3) {
                tempGameID = Integer.parseInt(value);
            }
            if (counter == 4) {
                Label tempLabel = new Label("", game.getMainLabelStyle());
                tempLabel.setAlignment(Align.center);
                // присоединение к игре невозможно, если она уже идёт
                if (!tempSecondName.equals(" ")) {
                    tempLabel.setText(tempFirstName + "\nпротив\n" + tempSecondName);
                } else {
                    try {
                        PreparedStatement preparedStatement = conn.prepareStatement("SELECT level, rating FROM users WHERE nickname = ?");
                        preparedStatement.setString(1, tempFirstName);
                        ResultSet resultSet = preparedStatement.executeQuery();
                        resultSet.next();
                        if (Integer.parseInt(value) == 0) {
                            tempLabel.setText(tempFirstName + "\nожидает соперника.\nБез пароля.\nУровень: " +
                                    resultSet.getInt("level") + "   Рейтинг: " + resultSet.getInt("rating"));
                        } else {
                            tempLabel.setText(tempFirstName + "\nожидает соперника\nУровень: " +
                                    resultSet.getInt("level") + "   Рейтинг: " + resultSet.getInt("rating"));
                        }
                    } catch (SQLException e) {
                        System.out.println("Жеееесть SQL недоступен");
                    }


                    int finalTempGameID = tempGameID;
                    tempLabel.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            // диалог о присоединении к игре
                            final Dialog dialog = new Dialog("\n Введите пароль ", game.getDialogWindowStyle());
                            dialog.getTitleLabel().setAlignment(Align.center);
                            dialog.getContentTable().add(new Label("\n", game.getMainLabelStyle()));
                            dialog.getContentTable().row();
                            TextField tempTextField = new TextField("", game.getTextFieldStyle());
                            tempTextField.setMessageText("Введите пароль для комнаты: ");
                            tempTextField.setAlignment(Align.center);
                            dialog.getContentTable().add(tempTextField);
                            dialog.getContentTable().getCell(tempTextField).width(800f);

                            TextButton startButton = new TextButton("Присоединиться\n", game.getTextButtonStyle());
                            startButton.addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                    dialog.hide();
                                    currentPlayerConnection.sendString("joinGame," + game.getCurrentUserName() +
                                            "," + tempTextField.getText().trim() + "," + finalTempGameID);
                                }
                            });
                            dialog.getButtonTable().add(startButton);

                            TextButton closeDialogButton = new TextButton("Закрыть\n", game.getTextButtonStyle());
                            closeDialogButton.addListener(new ClickListener() {
                                @Override
                                public void clicked(InputEvent event, float x, float y) {
                                    dialog.hide();
                                }
                            });
                            dialog.getButtonTable().add(closeDialogButton);
                            dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));
                            dialog.show(stage);
                        }
                    });
                }
                gameGroup.addActor(tempLabel);
                gameGroup.addActor(new Label("--------------------------", game.getMainLabelStyle()));
                counter = 0;
            }
        }
        gameScrollPane = new ScrollPane(gameGroup, neonSkin);
        gameScrollPane.setOverscroll(false, true);
        gameScrollPane.setScrollingDisabled(true, false);
        gameContainerTable = new Table();
        gameContainerTable.add(gameScrollPane);
        stage.addActor(gameContainerTable);
        gameContainerTable.setPosition(25, 200);
        gameContainerTable.setSize(800, 800);
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
        dispose();
    }

    @Override
    public void show() {
    }

    @Override
    public void dispose() {
        background.dispose();
        stage.dispose();
    }
}
