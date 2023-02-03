package mgschst.com;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;

public class MainMenuScreen implements Screen {
    final MainMgschst game;
    OrthographicCamera camera;
    Texture background;

    TextButton createGameButton;
    TextButton joinGameButton;
    TextButton openProfileButton;
    TextButton exitGameButton;

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
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dispose();
                    game.batch.dispose();
                    game.mainFont.dispose();
                    game.dispose();} }});

        createGameButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    System.out.println("Game created"); } }});

        joinGameButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    System.out.println("Game joined"); } }});

        openProfileButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (game.isButtonPressed()) {
                    game.setButtonPressed(true);
                    dispose();
                    game.setScreen(new ProfileScreen(game)); } }});
    }

    @Override
    public void render(float delta) {
        game.setButtonPressed(false);
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.draw(background, 0, 0);
        game.mainFont.draw(game.batch, "Добро пожаловать в Магнитошахтинск", 100, game.stage.getHeight() - 200);
        game.mainFont.draw(game.batch, "Приготовьтесь к худшему...", 100, game.stage.getHeight() - 250);
        game.batch.end();

        game.stage.draw();
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void show() {

    }

    @Override
    public void dispose() {
        background.dispose();
        game.stage.dispose();
    }
}
