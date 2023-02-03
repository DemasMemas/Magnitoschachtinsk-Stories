package mgschst.com;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

import java.util.Random;

public class MainMgschst extends Game {
    public SpriteBatch batch;
    public BitmapFont mainFont;
    public BitmapFont chosenFont;
    public BitmapFont smallFont;
    public BitmapFont normalFont;
    public Stage stage;
    private TextButton.TextButtonStyle textButtonStyle;
    private TextField.TextFieldStyle textFieldStyle;
    private Label.LabelStyle chosenLabelStyle;
    private Label.LabelStyle smallLabelStyle;
    private Label.LabelStyle normalLabelStyle;
    private Label.LabelStyle mainLabelStyle;
    private Label.LabelStyle chosenMainLabelStyle;
    private Window.WindowStyle dialogWindowStyle;
    private boolean buttonIsPressed;
    private String currentUserName;
    private Music menuMusic;
    private final Random random = new Random();


    @Override
    public void create() {
        batch = new SpriteBatch();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Fonts/jura.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789][_!$%#@|\\/?-+=()*&.;:,{}\"´`'<>";
        parameter.size = 40;
        parameter.borderWidth = 5;
        mainFont = generator.generateFont(parameter);
        generator.dispose();

        generator = new FreeTypeFontGenerator(Gdx.files.internal("Fonts/jura.ttf"));
        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789][_!$%#@|\\/?-+=()*&.;:,{}\"´`'<>";
        parameter.size = 16;
        parameter.borderWidth = 2;
        parameter.color = new Color(0, 1, 0, 1);
        chosenFont = generator.generateFont(parameter);
        generator.dispose();

        generator = new FreeTypeFontGenerator(Gdx.files.internal("Fonts/jura.ttf"));
        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789][_!$%#@|\\/?-+=()*&.;:,{}\"´`'<>";
        parameter.size = 14;
        parameter.borderWidth = 2;
        parameter.color = new Color(1, 1, 1, 1);
        smallFont = generator.generateFont(parameter);
        generator.dispose();

        generator = new FreeTypeFontGenerator(Gdx.files.internal("Fonts/jura.ttf"));
        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789][_!$%#@|\\/?-+=()*&.;:,{}\"´`'<>";
        parameter.size = 24;
        parameter.borderWidth = 2;
        parameter.color = new Color(1, 1, 1, 1);
        normalFont = generator.generateFont(parameter);
        generator.dispose();

        textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = mainFont;

        textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = mainFont;
        textFieldStyle.fontColor = mainFont.getColor();

        chosenLabelStyle = new Label.LabelStyle();
        chosenLabelStyle.font = chosenFont;
        chosenLabelStyle.fontColor = chosenFont.getColor();

        smallLabelStyle = new Label.LabelStyle();
        smallLabelStyle.font = smallFont;
        smallLabelStyle.fontColor = smallFont.getColor();

        normalLabelStyle = new Label.LabelStyle();
        normalLabelStyle.font = normalFont;
        normalLabelStyle.fontColor = normalFont.getColor();

        mainLabelStyle = new Label.LabelStyle();
        mainLabelStyle.font = mainFont;
        mainLabelStyle.fontColor = chosenFont.getColor();

        chosenMainLabelStyle = new Label.LabelStyle();
        chosenMainLabelStyle.font = mainFont;
        chosenMainLabelStyle.fontColor = new Color(0, 1, 0, 1);

        dialogWindowStyle = new Window.WindowStyle();
        dialogWindowStyle.titleFont = mainFont;

        playMenuMusic();

        this.setScreen(new AuthorizationScreen(this));
    }

    @Override
    public void render() {
        super.render();
        if (!menuMusic.isPlaying()) {
            playMenuMusic();
        }
    }

    @Override public void dispose() { }

    public TextButton.TextButtonStyle getTextButtonStyle() {
        return textButtonStyle;
    }

    public TextField.TextFieldStyle getTextFieldStyle() {
        return textFieldStyle;
    }

    public Label.LabelStyle getChosenLabelStyle() {
        return chosenLabelStyle;
    }

    public Label.LabelStyle getSmallLabelStyle() {
        return smallLabelStyle;
    }

    public Label.LabelStyle getNormalLabelStyle() {
        return normalLabelStyle;
    }

    public Label.LabelStyle getMainLabelStyle() {
        return mainLabelStyle;
    }

    public Label.LabelStyle getChosenMainLabelStyle() {
        return chosenMainLabelStyle;
    }

    public Window.WindowStyle getDialogWindowStyle() {
        return dialogWindowStyle;
    }

    public boolean isButtonPressed() {
        return !buttonIsPressed;
    }

    public void setButtonPressed(boolean buttonIsPressed) {
        this.buttonIsPressed = buttonIsPressed;
    }

    public void setCurrentUserName(String newName) {
        this.currentUserName = newName;
    }

    public String getCurrentUserName() {
        return this.currentUserName;
    }

    private void playMenuMusic() {
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal
                ("Music/menuMusic" + random.nextInt(7) + ".mp3"));
        menuMusic.play();
        menuMusic.setVolume(0.15f);
    }
}