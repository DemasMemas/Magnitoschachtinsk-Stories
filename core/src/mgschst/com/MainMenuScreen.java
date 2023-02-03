package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.IOException;

public class MainMenuScreen implements Screen, TCPConnectionListener  {
    final MainMgschst game;
    OrthographicCamera camera;
    Texture background;

    TextButton createGameButton;
    TextButton joinGameButton;
    TextButton openProfileButton;
    TextButton exitGameButton;

    Label greetLabel1;
    Label greetLabel2;

    private TCPConnection connection;

    public MainMenuScreen(final MainMgschst game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);

        background = new Texture(Gdx.files.internal("MenuAssets/main_menu_bg.jpg"));

        game.stage = new Stage();
        Gdx.input.setInputProcessor(game.stage);

        createGameButton = new TextButton("Создать игру", game.getTextButtonStyle());
        game.stage.addActor(createGameButton);
        createGameButton.setPosition(100, game.stage.getHeight() - 400);

        joinGameButton = new TextButton("Присоединиться к игре", game.getTextButtonStyle());
        game.stage.addActor(joinGameButton);
        joinGameButton.setPosition(100, game.stage.getHeight() - 450);

        openProfileButton = new TextButton("Профиль", game.getTextButtonStyle());
        game.stage.addActor(openProfileButton);
        openProfileButton.setPosition(100, game.stage.getHeight() - 500);

        exitGameButton = new TextButton("Выйти", game.getTextButtonStyle());
        game.stage.addActor(exitGameButton);
        exitGameButton.setPosition(100, game.stage.getHeight() - 600);

        exitGameButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                    game.dispose();
                Gdx.app.exit();} });

        createGameButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
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
                                game.setScreen(new GameScreen(game));
                                dispose();

                                try {
                                    connection = new TCPConnection(MainMenuScreen.this, "localhost", 8080);
                                    connection.sendString("createGame," + game.getCurrentUserName() + "," + tempTextField.getText().trim());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } }});

                    dialog.getButtonTable().add(startButton);

                    TextButton closeDialogButton = new TextButton("Закрыть\n", game.getTextButtonStyle());
                    closeDialogButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                                dialog.hide(); }});

                    dialog.getButtonTable().add(closeDialogButton);

                    dialog.background(new TextureRegionDrawable(new Texture(Gdx.files.internal("Images/dialog_bg.png"))));

                    dialog.show(game.stage);
                }});

        joinGameButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);

                    // новое окно с выбором доступной игры

                }}});

        openProfileButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dispose();
                    game.setScreen(new ProfileScreen(game)); } }});

        greetLabel1 = new Label("Добро пожаловать в Магнитошахтинск", game.getMainLabelStyle());
        greetLabel1.setPosition(100, 1080 - 200);
        game.stage.addActor(greetLabel1);
        greetLabel2 = new Label("Приготовьтесь к худшему...", game.getMainLabelStyle());
        greetLabel2.setPosition(100, 1080 - 250);
        game.stage.addActor(greetLabel2);
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

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void show() { }

    @Override
    public void dispose() {
        background.dispose();
        game.stage.dispose();
    }

    @Override public void onConnectionReady(TCPConnection tcpConnection) { }

    @Override
    public void onReceiveString(TCPConnection tcpConnection, String value) {
        // обработка обратной связи, взаимодействие с объектом GameScreen
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) { printMessage("Connection close" + game.getCurrentUserName()); }

    @Override
    public void onException(TCPConnection tcpConnection, IOException e) {
        printMessage("Connection exception: " + e);
    }

    // отправка справочной информации на сервер
    private synchronized void printMessage(String message) { connection.sendString(message); }
}
