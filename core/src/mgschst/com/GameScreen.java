package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameScreen implements Screen {
    final MainMgschst game;
    OrthographicCamera camera;
    Texture background;



    TCPConnection currentPlayerConnection;

    public GameScreen(final MainMgschst game, TCPConnection playerConnection) {
        this.game = game;
        this.currentPlayerConnection = playerConnection;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);

        background = new Texture(Gdx.files.internal("MenuAssets/main_menu_bg.jpg"));

        game.stage = new Stage();
        Gdx.input.setInputProcessor(game.stage);
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

        game.stage.draw();
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() {

    }
    @Override public void resume() {

    }
    @Override public void hide() {

    }
    @Override public void show() { }

    @Override
    public void dispose() {
        game.stage.dispose();
        background.dispose();
    }
}
