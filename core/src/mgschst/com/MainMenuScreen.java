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

public class MainMenuScreen implements Screen {
    final MainMgschst game;
    OrthographicCamera camera;
    final Batch batch;
    Texture background;
    Stage stage;

    TextButton createGameButton;
    TextButton joinGameButton;
    TextButton openProfileButton;
    TextButton exitGameButton;

    Label greetLabel1;
    Label greetLabel2;

    private final TCPConnection connection;

    public MainMenuScreen(final MainMgschst game) {
        this.game = game;
        camera = game.getCamera();
        batch = game.batch;
        connection = game.getPlayerConnection();
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
        stage = new Stage();
        stage.getViewport().setCamera(camera);
        createStage();
        Gdx.input.setInputProcessor(stage);
        stage.getViewport().setScreenBounds(0, 0, 1920, 1080);
    }

    @Override
    public void dispose() {
        background.dispose();
        stage.dispose();
    }

    public void createStage() {
        background = new Texture(Gdx.files.internal("MenuAssets/main_menu_bg.jpg"));

        createGameButton = new TextButton("Создать игру", game.getTextButtonStyle());
        createGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // диалог о создании игры
                final Dialog dialog = new Dialog("\n Введите пароль ", game.getDialogWindowStyle());
                dialog.getTitleLabel().setAlignment(Align.center);
                dialog.getContentTable().add(new Label("\n", game.getMainLabelStyle()));
                dialog.getContentTable().row();

                TextField tempTextField = new TextField("", game.getTextFieldStyle());
                tempTextField.setMessageText("Введите пароль для комнаты: ");
                tempTextField.setAlignment(Align.center);
                dialog.getContentTable().add(tempTextField);
                dialog.getContentTable().getCell(tempTextField).width(800f);

                TextButton startButton = new TextButton("Создать\n", game.getTextButtonStyle());
                startButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        dialog.hide();
                        if (tempTextField.getText().trim().length() != 0)
                            connection.sendString("createGame," + game.getCurrentUserName() + "," + tempTextField.getText().trim());
                        else
                            connection.sendString("createGame," + game.getCurrentUserName() + ",noPassword");
                        game.setScreen(new GameScreen(game));
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
        stage.addActor(createGameButton);
        createGameButton.setPosition(100, 1080 - 400);

        joinGameButton = new TextButton("Присоединиться к игре", game.getTextButtonStyle());
        joinGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameMatchingScreen(game));
            }
        });
        stage.addActor(joinGameButton);
        joinGameButton.setPosition(100, 1080 - 450);

        openProfileButton = new TextButton("Профиль", game.getTextButtonStyle());
        openProfileButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new ProfileScreen(game));
            }
        });
        stage.addActor(openProfileButton);
        openProfileButton.setPosition(100, 1080 - 500);

        exitGameButton = new TextButton("Выйти", game.getTextButtonStyle());
        exitGameButton.setPosition(100, 1080 - 600);
        exitGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                connection.disconnect();
                game.dispose();
                Gdx.app.exit();
            }
        });
        stage.addActor(exitGameButton);

        greetLabel1 = new Label("Добро пожаловать в Магнитошахтинск", game.getMainLabelStyle());
        greetLabel1.setPosition(100, 1080 - 200);
        stage.addActor(greetLabel1);
        greetLabel2 = new Label("Приготовьтесь к худшему...", game.getMainLabelStyle());
        greetLabel2.setPosition(100, 1080 - 250);
        stage.addActor(greetLabel2);
    }
}
