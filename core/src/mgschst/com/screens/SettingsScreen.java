package mgschst.com.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import mgschst.com.MainMgschst;

import java.io.*;

public class SettingsScreen implements Screen {
    final MainMgschst game;
    OrthographicCamera camera;
    final Batch batch;
    Image background;
    Stage stage;
    Skin neonSkin = new Skin(Gdx.files.internal("Skins/neon/skin/neon-ui.json"));

    Float musicVol = 1f, soundVol = 1f;

    public SettingsScreen(final MainMgschst game) {
        this.game = game;
        camera = game.getCamera();
        batch = game.batch;
    }

    @Override
    public void render(float delta) {
        DeckBuildingScreen.renderScreen(game, camera, batch, stage);
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

        if (!(stage.getWidth() == 0 || stage.getHeight() == 0)){
            game.xScaler = stage.getWidth()/1920f;
            game.yScaler = stage.getHeight()/1080f;
        }

        stage.getViewport().setScreenBounds(0, 0, (int) (1920 * game.xScaler), (int) (1080 * game.yScaler));

        for (Actor actor:stage.getActors()) {
            actor.scaleBy(game.xScaler - 1,  game.yScaler - 1);
            actor.setPosition(actor.getX() * game.xScaler, actor.getY() * game.yScaler);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    public void createStage() {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(System.getProperty("user.dir")
                + "\\core\\src\\mgschst\\com\\config\\audioCFG.txt").getPath()))) {
            String line; float sV = 1f, mV = 1f;
            while ((line = reader.readLine()) != null) {
                switch (line.split(":")[0]) {
                    case "musicVolume" -> mV = Float.parseFloat(line.split(":")[1]);
                    case "soundVolume" -> sV = Float.parseFloat(line.split(":")[1]);
                }
            }
            musicVol = mV; soundVol = sV;
        } catch (IOException e) {e.printStackTrace();}

        background = new Image(new Texture(Gdx.files.internal("MenuAssets/main_menu_bg.jpg")));
        background.setPosition(0,0);
        stage.addActor(background);

        Image musicIco = new Image(new Texture(Gdx.files.internal("MenuAssets/musicICO.png")));
        musicIco.setPosition(650, 775);
        stage.addActor(musicIco);
        Label musicLabel = new Label("Громкость музыки", game.getMainLabelStyle());
        musicLabel.setWidth(500 * game.xScaler); musicLabel.setAlignment(Align.center);
        musicLabel.setPosition(100, 850);
        stage.addActor(musicLabel);
        Slider musicSlider = new Slider(0, 100, 1, false, neonSkin);
        musicSlider.setPosition(100, 800);
        musicSlider.setValue(musicVol * 100);
        musicSlider.setSize(500 * game.xScaler, 50);
        stage.addActor(musicSlider);

        Image soundIco = new Image(new Texture(Gdx.files.internal("MenuAssets/soundICO.png")));
        soundIco.setPosition(650, 625);
        stage.addActor(soundIco);
        Label soundLabel = new Label("Громкость звуков", game.getMainLabelStyle());
        soundLabel.setWidth(500 * game.xScaler); soundLabel.setAlignment(Align.center);
        soundLabel.setPosition(100, 700);
        stage.addActor(soundLabel);
        Slider soundSlider = new Slider(0, 100, 1, false, neonSkin);
        soundSlider.setPosition(100, 650);
        soundSlider.setValue(soundVol * 100);
        soundSlider.setSize(500 * game.xScaler, 50);
        stage.addActor(soundSlider);

        TextButton saveButton = new TextButton("Сохранить", game.getTextButtonStyle());
        saveButton.setPosition(800, 200);
        saveButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                musicVol = musicSlider.getValue() / 100;
                soundVol = soundSlider.getValue() / 100;
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
                        System.getProperty("user.dir") + "\\core\\src\\mgschst\\com\\config\\audioCFG.txt")
                        .getPath()))) {
                    writer.write("musicVolume:" + musicVol + "\n");
                    writer.write("soundVolume:" + soundVol + "\n");
                }
                catch (IOException e) {e.printStackTrace();}
                game.updateVolume(musicVol, soundVol);
            }
        });
        stage.addActor(saveButton);

        TextButton menuButton = new TextButton("Вернуться в меню", game.getTextButtonStyle());
        menuButton.setPosition(725, 125);
        menuButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        stage.addActor(menuButton);
    }
}
