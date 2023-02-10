package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthorizationScreen implements Screen {
    final MainMgschst game;
    final OrthographicCamera camera;
    final Batch batch;
    Sprite background;
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

        background = new Sprite(new Texture(Gdx.files.internal("AuthorizationAssets/authorization_bg.jpg")));
        background.setBounds(0, 0, 1920 * game.xScaler, 1080 * game.yScaler);

        loginButton = new TextButton("Войти", game.getTextButtonStyle());
        stage.addActor(loginButton);
        loginButton.setPosition(stage.getWidth()/2 - 68, stage.getHeight() - 700);

        registerButton = new TextButton("Зарегистрироваться", game.getTextButtonStyle());
        stage.addActor(registerButton);
        registerButton.setPosition(stage.getWidth()/2 - 236, stage.getHeight() - 750);

        exitGameButton = new TextButton("Выйти", game.getTextButtonStyle());
        stage.addActor(exitGameButton);
        exitGameButton.setPosition(stage.getWidth()/2 - 68, stage.getHeight() - 850);

        loginField = new TextField("", game.getTextFieldStyle());
        stage.addActor(loginField);
        loginField.setPosition(stage.getWidth()/2 - 175, stage.getHeight() - 550);
        loginField.setWidth(400f);
        loginField.setMessageText("Введите логин...");
        loginField.setMaxLength(16);

        passwordField = new TextField("", game.getTextFieldStyle());
        stage.addActor(passwordField);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        passwordField.setPosition(stage.getWidth()/2 - 175, stage.getHeight() - 600);
        passwordField.setWidth(400f);
        passwordField.setMessageText("Введите пароль..");
        passwordField.setMaxLength(20);

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
    }

    @Override
    public void show() { }

    @Override
    public void render(float delta) {
        game.setButtonPressed(false);
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        background.draw(game.batch);
        game.mainFont.draw(batch, "Магнитошахтинск ждёт", stage.getWidth()/2 - 255, stage.getHeight() - 400);
        batch.end();

        stage.draw();
    }

    @Override public void resize(int width, int height) {
        game.xScaler = 1920f/width;
        game.yScaler = 1080f/height;
    }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
