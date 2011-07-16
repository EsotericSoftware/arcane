package arcane;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import arcane.util.Preferences;

public class ArcaneTranslation extends Preferences{

	public ArcaneTranslation (String fileName) {
		try {
			FileInputStream stream = new FileInputStream(fileName);
			load(stream);
			stream.close();
		} catch (FileNotFoundException ex) {
		    	Arcane.getInstance().log("File not found: \"" + fileName + "\". UI will default to English.");
		} catch (IOException ex) {
		    	Arcane.getInstance().log("Error reading \"" + fileName + "\". UI will default to English.");
		}
	}

	public void load (FileInputStream stream) throws IOException {
		props.loadFromXML(stream);
	}

	public void store (FileOutputStream stream, String comments) throws IOException {
		props.storeToXML(stream, comments);
	}
}
