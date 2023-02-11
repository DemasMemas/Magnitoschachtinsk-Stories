package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthorizationScreen implements Screen {
    final MainMgschst game;
    final OrthographicCamera camera;
    final Batch batch;
    Image background;
    Stage stage;

    TextButton loginButton;
    TextButton registerButton;
    TextButton exitGameButton;

    TextField loginField;
    TextField passwordField;

    Connection conn = new DatabaseHandler().getConnection();

    public AuthorizationScreen(final MainMgschst game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        camera = game.getCamera();
        batch = game.batch;

        game.xScaler = stage.getWidth()/1920f;
        game.yScaler = stage.getHeight()/1080f;

        background = new Image(new Texture(Gdx.files.internal("AuthorizationAssets/authorization_bg.jpg")));
        background.setPosition(0,0);
        stage.addActor(background);

        loginButton = new TextButton("Войти", game.getTextButtonStyle());
        stage.addActor(loginButton);
        loginButton.setPosition(960 - 68, 1080 - 700);
        loginButton.align(Align.center);

        registerButton = new TextButton("Зарегистрироваться", game.getTextButtonStyle());
        stage.addActor(registerButton);
        registerButton.setPosition(960 - 236, 1080 - 750);
        registerButton.align(Align.center);

        exitGameButton = new TextButton("Выйти", game.getTextButtonStyle());
        stage.addActor(exitGameButton);
        exitGameButton.setPosition(960 - 68, 1080 - 850);
        exitGameButton.align(Align.center);

        loginField = new TextField("", game.getTextFieldStyle());
        stage.addActor(loginField);
        loginField.setPosition(960 - 185, 1080 - 550);
        loginField.setWidth(400f * game.xScaler);
        loginField.setMessageText("Введите логин...");
        loginField.setMaxLength(16);
        loginField.setAlignment(Align.center);

        passwordField = new TextField("", game.getTextFieldStyle());
        stage.addActor(passwordField);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        passwordField.setPosition(960 - 185, 1080 - 600);
        passwordField.setWidth(400f * game.xScaler);
        passwordField.setMessageText("Введите пароль..");
        passwordField.setMaxLength(20);
        passwordField.setAlignment(Align.center);

        loginButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);

                    try {
                        PreparedStatement preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE nickname = ? AND password = ?");
                        preparedStatement.setString(1, loginField.getText().trim());
                        preparedStatement.setString(2, passwordField.getText().trim());
                        ResultSet resultSet = preparedStatement.executeQuery();
                        if (resultSet.next()){
                            game.setCurrentUserName(loginField.getText().trim());

                            game.setScreen(new MainMenuScreen(game));
                        } else {
                            loginField.setText("");
                            passwordField.setText("");
                            loginField.setMessageText("Неверные данные");
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                } }});

        registerButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    if (loginField.getText().trim().length() > 0 && passwordField.getText().trim().length() > 0){
                        try {
                            PreparedStatement preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE nickname = ?");
                            preparedStatement.setString(1, loginField.getText().trim());
                            ResultSet resultSet = preparedStatement.executeQuery();
                            if (resultSet.next()){
                                loginField.setText("");
                                loginField.setMessageText("Логин занят");

                            } else {
                                game.setCurrentUserName(loginField.getText().trim());

                                preparedStatement = conn.prepareStatement("INSERT INTO users (nickname,password) VALUES(?,?)");
                                preparedStatement.setString(1, loginField.getText().trim());
                                preparedStatement.setString(2, passwordField.getText().trim());
                                preparedStatement.executeUpdate();

                                game.setScreen(new MainMenuScreen(game));
                            }
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    } else {
                        loginField.setMessageText("Не введены данные");
                    }
                } }});

        exitGameButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.getPlayerConnection().disconnect();
                game.dispose();
                Gdx.app.exit(); }});

        Label mainLabel = new Label("Магнитошахтинск ждёт", game.getMainLabelStyle());
        mainLabel.setPosition(960 - 255, 1080 - 400);
        mainLabel.setAlignment(Align.center);
        stage.addActor(mainLabel);

        for (Actor actor:stage.getActors()) {
            actor.scaleBy(game.xScaler - 1,  game.yScaler - 1);
            actor.setPosition(actor.getX() * game.xScaler, actor.getY() * game.yScaler);
        }
    }

    @Override
    public void show() { }

    @Override
    public void render(float delta) {
        game.setButtonPressed(false);
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        stage.draw();
    }

    @Override public void resize(int width, int height) {    }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
