package mgschst.com;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import mgschst.com.MainMgschst;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		Graphics.DisplayMode primaryMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
		config.setFullscreenMode(primaryMode);
		config.setTitle("Magnitoschachtinsk Stories");
		config.setWindowIcon( Files.FileType.Internal, "Images/icon.png");
		new Lwjgl3Application(new MainMgschst(), config);
	}
}
