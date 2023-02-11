package mgschst.com;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
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
    private final OrthographicCamera camera = new OrthographicCamera();
    private TCPConnection playerConnection;
    private int currentGameID;

    public float xScaler = 1;
    public float yScaler = 1;

    @Override
    public void create() {
        recreatePlayerConnection();
        camera.setToOrtho(false);
        batch = new SpriteBatch();

        String charactersFont = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789][_!$%#@|\\/?-+=()*&.;:,{}\"´`'<>";

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Fonts/jura.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = charactersFont;
        parameter.size = 40;
        parameter.borderWidth = 5;
        mainFont = generator.generateFont(parameter);

        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = charactersFont;
        parameter.size = 16;
        parameter.borderWidth = 2;
        parameter.color = new Color(0, 1, 0, 1);
        chosenFont = generator.generateFont(parameter);

        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = charactersFont;
        parameter.size = 14;
        parameter.borderWidth = 2;
        smallFont = generator.generateFont(parameter);

        parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters = charactersFont;
        parameter.size = 24;
        parameter.borderWidth = 2;
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

        AuthorizationScreen auS = new AuthorizationScreen(this);


        if (xScaler != 1 || yScaler != 1){
            float fontScaler = (xScaler + yScaler) / 2;
            generator = new FreeTypeFontGenerator(Gdx.files.internal("Fonts/jura.ttf"));
            parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.characters = charactersFont;
            parameter.size = (int) (40 * fontScaler);
            parameter.borderWidth = 5;
            mainFont = generator.generateFont(parameter);

            parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.characters = charactersFont;
            parameter.size = (int) (16 * fontScaler);
            parameter.borderWidth = 2;
            parameter.color = new Color(0, 1, 0, 1);
            chosenFont = generator.generateFont(parameter);

            parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.characters = charactersFont;
            parameter.size = (int) (14 * fontScaler);
            parameter.borderWidth = 2;
            smallFont = generator.generateFont(parameter);

            parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.characters = charactersFont;
            parameter.size = (int) (24 * fontScaler);
            parameter.borderWidth = 2;
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

            auS = new AuthorizationScreen(this);
        }

        this.setScreen(auS);
    }

    @Override
    public void render() {
        super.render();
        if (!menuMusic.isPlaying()) {
            playMenuMusic();
        }
    }

    @Override public void dispose() {
        menuMusic.dispose();
    }

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

    public OrthographicCamera getCamera() {
        return camera;
    }

    public TCPConnection getPlayerConnection(){ return playerConnection;}

    public int getCurrentGameID() { return currentGameID; }

    public void setCurrentGameID(int currentGameID) { this.currentGameID = currentGameID; }

    public void recreatePlayerConnection(){ try {
        playerConnection = new TCPConnection(new TCPConnectionHandler(this), "192.168.67.241", 8080);
    } catch (Exception e) {
        e.printStackTrace();
    }}

    private void playMenuMusic() {
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal
                ("Music/menuMusic" + random.nextInt(7) + ".mp3"));
        menuMusic.play();
        menuMusic.setVolume(0.15f);
    }
}