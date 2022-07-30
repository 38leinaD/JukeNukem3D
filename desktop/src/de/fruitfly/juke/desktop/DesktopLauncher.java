package de.fruitfly.juke.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import de.fruitfly.juke.JukeNukem3D;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		//config.width = 1024;
		//config.height = 768;
		new Lwjgl3Application(new JukeNukem3D(), config);
	}
}
