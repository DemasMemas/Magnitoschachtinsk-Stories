package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class GameMatchingScreen implements Screen, TCPConnectionListener {
    final MainMgschst game;
    OrthographicCamera camera;
    Texture background;


    VerticalGroup gameGroup;
    static TCPConnection currentPlayerConnection;

    public GameMatchingScreen(final MainMgschst game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);

        background = new Texture(Gdx.files.internal("MenuAssets/main_menu_bg.jpg"));

        game.stage = new Stage();
        Gdx.input.setInputProcessor(game.stage);

        try {
            currentPlayerConnection = new TCPConnection(this, "localhost", 8080);
        } catch (IOException e) {
            e.printStackTrace();
        }

        currentPlayerConnection.sendString("getGames");

        gameGroup = new VerticalGroup();
        gameGroup.setPosition(350, 650);
        game.stage.addActor(gameGroup);
    }

    @Override
    public void render(float delta) {
        game.setButtonPressed(false);
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.draw(background, 0, 0);
        game.batch.end();

        game.stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 60f));
        game.stage.draw();
    }

    public void setUpGameList(ArrayList<String> gameList) {
        gameGroup = new VerticalGroup();
        gameGroup.setPosition(350, 650);
        game.stage.addActor(gameGroup);
        int counter = 0;
        String tempFirstName = "";
        String tempSecondName = "";
        for (String value : gameList) {
            counter++;
            if (counter == 1) {
                tempFirstName = value;
            }
            if (counter == 2) {
                tempSecondName = value;
            }
            if (counter == 3) {
                Label tempLabel = new Label("", game.getMainLabelStyle());
                tempLabel.setAlignment(Align.center);
                // присоединение к игре невозможно, если она уже идёт
                if (!tempSecondName.equals(" ")) {
                    tempLabel.setText(tempFirstName + " против " + tempSecondName);
                } else {
                    tempLabel.setText(tempFirstName + " ожидает соперника");
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
                                            "," + tempTextField.getText().trim() + "," + Integer.parseInt(value));
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
                            dialog.show(game.stage);
                        }
                    });
                }
                gameGroup.addActor(tempLabel);
                counter = 0;
            }
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
    public void hide() {

    }

    @Override
    public void show() {
    }

    @Override
    public void dispose() {
        game.stage.dispose();
        background.dispose();
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {
    }

    @Override
    public void onReceiveString(TCPConnection tcpConnection, String value) {
        Gdx.app.postRunnable(() -> {
            // обработка обратной связи, взаимодействие с объектом GameScreen
            ArrayList<String> commandList = new ArrayList<>();
            Collections.addAll(commandList, value.split(","));
            switch (commandList.get(0)) {
                case "games" -> {
                    commandList.remove(0);
                    setUpGameList(commandList);
                }
                case "writeChatMsg" -> System.out.println(commandList.get(1) + ": " + commandList.get(2));
                case "acceptJoin" -> {
                    game.setScreen(new GameScreen(game, currentPlayerConnection));
                    dispose();
                }
            }
        });
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        printMessage("Connection close" + game.getCurrentUserName());
    }

    @Override
    public void onException(TCPConnection tcpConnection, IOException e) {
        printMessage("Connection exception: " + e);
    }

    // отправка справочной информации на сервер
    private synchronized void printMessage(String message) {
        currentPlayerConnection.sendString(message);
    }
}
