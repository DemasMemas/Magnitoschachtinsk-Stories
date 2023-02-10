package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameScreen implements Screen {
    final MainMgschst game;
    final OrthographicCamera camera;
    final Batch batch;
    Stage stage;
    Image background;


    TextField chat;
    TextButton exitButton;
    TextButton sendMessage;

    TCPConnection playerConn;

    public GameScreen(final MainMgschst game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        playerConn = game.getPlayerConnection();

        camera = game.getCamera();
        batch = game.batch;

        background = new Image(new Texture(Gdx.files.internal("MenuAssets/main_menu_bg.jpg")));
        background.setPosition(0,0);
        stage.addActor(background);

        chat = new TextField("TempLabel", game.getTextFieldStyle());
        chat.setPosition(100, 200);
        chat.setWidth(1000f);
        stage.addActor(chat);

        exitButton = new TextButton("Выйти", game.getTextButtonStyle());
        stage.addActor(exitButton);
        exitButton.setPosition(stage.getWidth() / 2, stage.getHeight() - 550);

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getPlayerConnection().disconnect();
                game.recreatePlayerConnection();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        sendMessage = new TextButton("Отправить сообщение", game.getTextButtonStyle());
        stage.addActor(sendMessage);
        sendMessage.setPosition(100, 140);

        sendMessage.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // изменить на нормальный чат
                playerConn.sendString("chatMsg," + game.getCurrentUserName() + ","
                        + game.getCurrentGameID() + "," + chat.getText());
            }
        });

        game.xScaler = stage.getWidth()/1920f;
        game.yScaler = stage.getHeight()/1080f;
        for (Actor actor:stage.getActors()) {
            actor.scaleBy(game.xScaler - 1,  game.yScaler - 1);
            actor.setPosition(actor.getX() * game.xScaler, actor.getY() * game.yScaler);
        }
    }

    @Override
    public void render(float delta) {
        game.setButtonPressed(false);
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.end();

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 60f));
        stage.draw();
    }

    @Override public void resize(int width, int height) {
    }
    @Override public void pause() { }
    @Override public void resume() {}
    @Override public void hide() { dispose();}
    @Override public void show() {    }

    @Override
    public void dispose() {
        stage.dispose();
        game.setCurrentGameID(0);
    }

    public void changeLabel(String text){ chat.setText(text); }
    public void openMenu(){ Gdx.app.postRunnable(() -> game.setScreen(new MainMenuScreen(game))); }
}
