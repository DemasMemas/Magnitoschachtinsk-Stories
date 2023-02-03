package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
    OrthographicCamera camera;
    Texture background;

    TextButton loginButton;
    TextButton registerButton;
    TextButton exitGameButton;

    TextField loginField;
    TextField passwordField;

    Connection conn = new DatabaseHandler().getConnection();

    public AuthorizationScreen(final MainMgschst game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);

        background = new Texture(Gdx.files.internal("AuthorizationAssets/authorization_bg.jpg"));

        game.stage = new Stage();
        Gdx.input.setInputProcessor(game.stage);

        loginButton = new TextButton("Войти", game.getTextButtonStyle());
        game.stage.addActor(loginButton);
        loginButton.setPosition(game.stage.getWidth()/2 - 68, game.stage.getHeight() - 700);

        registerButton = new TextButton("Зарегистрироваться", game.getTextButtonStyle());
        game.stage.addActor(registerButton);
        registerButton.setPosition(game.stage.getWidth()/2 - 236, game.stage.getHeight() - 750);

        exitGameButton = new TextButton("Выйти", game.getTextButtonStyle());
        game.stage.addActor(exitGameButton);
        exitGameButton.setPosition(game.stage.getWidth()/2 - 68, game.stage.getHeight() - 850);

        loginField = new TextField("", game.getTextFieldStyle());
        game.stage.addActor(loginField);
        loginField.setPosition(game.stage.getWidth()/2 - 175, game.stage.getHeight() - 550);
        loginField.setWidth(400f);
        loginField.setMessageText("Введите логин...");
        loginField.setMaxLength(16);

        passwordField = new TextField("", game.getTextFieldStyle());
        game.stage.addActor(passwordField);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        passwordField.setPosition(game.stage.getWidth()/2 - 175, game.stage.getHeight() - 600);
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

                            dispose();
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

                                dispose();
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
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dispose();
                    game.batch.dispose();
                    game.mainFont.dispose();
                    game.dispose(); } }});
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        game.setButtonPressed(false);
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.draw(background, 0, 0);
        game.mainFont.draw(game.batch, "Магнитошахтинск ждёт", game.stage.getWidth()/2 - 255, game.stage.getHeight() - 400);
        game.batch.end();

        game.stage.draw();
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        background.dispose();
        game.stage.dispose();
    }
}
