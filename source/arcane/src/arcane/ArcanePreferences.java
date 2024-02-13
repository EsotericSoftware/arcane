
package arcane;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import arcane.util.Preferences;

public class ArcanePreferences extends Preferences {
	public String version;
	public boolean showLogOnError;
	public String imagesSuffix;
	public String imagesPath;
	public boolean scaleCardImageLarger;
	public boolean logFoundImages;
	public boolean logMissingImages;
	public CardImageType cardImageType;
	public CardFontSize fontSize;
	public CardFontFamily fontFamily;
	public String language;
	public String uiLanguage;
	public long rulesTimestamp;
	public long rulingsTimestamp;
	public String proxyHost;
	public String proxyPort;

	private List<SavePreferencesListener> saveListeners = new ArrayList<SavePreferencesListener>();
	private final String fileName;

	public ArcanePreferences (String fileName) {
		this.fileName = fileName;
		if (new File(fileName).exists()) {
			try {
				FileInputStream stream = new FileInputStream(fileName);
				load(stream);
				stream.close();
			} catch (FileNotFoundException ex) {
			    throw new ArcaneException("File not found: \"" + fileName + "\".", ex);
			} catch (IOException ex) {
				throw new ArcaneException("Error reading \"" + fileName + "\".", ex);
			}
		}

		version = get("version", Arcane.version);

		showLogOnError = getBoolean("show.log.on.error", true);

		imagesPath = get("card.images.path", "crops/");
		imagesSuffix = get("card.images.suffix", ".jpg");
		scaleCardImageLarger = getBoolean("card.images.scale.larger", false);
		logFoundImages = getBoolean("card.images.log.found", false);
		logMissingImages = getBoolean("card.images.log.missing", false);
		cardImageType = CardImageType.valueOf(get("card.images.type", "none"));

		fontSize = CardFontSize.valueOf(get("card.font.size", "small"));
		fontFamily = CardFontFamily.valueOf(get("card.font.family", "tahoma"));

		language = get("card.language", "English");
		uiLanguage = get("ui.language", "English");

		rulesTimestamp = getLong("timestamp.rules", 0);
		rulingsTimestamp = getLong("timestamp.rulings", 0);

		proxyHost = get("proxy.host", null);
		if (proxyHost != null && proxyHost.length() > 0) System.setProperty("http.proxyHost", proxyHost);
		proxyPort = get("proxy.port", null);
		if (proxyPort != null && proxyPort.length() > 0) System.setProperty("http.proxyPort", proxyPort);
	}

	public void save () {
		set("version", Arcane.version);

		set("show.log.on.error", showLogOnError);

		set("card.images.path", imagesPath);
		set("card.images.suffix", imagesSuffix);
		set("card.images.scale.larger", scaleCardImageLarger);
		set("card.images.log.found", logFoundImages);
		set("card.images.log.missing", logMissingImages);
		set("card.images.type", cardImageType);

		set("card.font.size", fontSize);
		set("card.font.family", fontFamily);

		set("card.language", language);
		set("ui.language", uiLanguage);

		set("timestamp.rules", rulesTimestamp);
		set("timestamp.rulings", rulingsTimestamp);

		if (proxyHost != null && proxyHost.length() > 0) set("proxy.host", proxyHost);
		else set("proxy.host", "");
		if (proxyPort != null && proxyPort.length() > 0 && proxyPort != "80") set("proxy.port", proxyPort);
		else set("proxy.port", "");

		for (SavePreferencesListener listeners : saveListeners)
			listeners.savePreferences();

		try {
			FileOutputStream stream = new FileOutputStream(fileName);
			store(stream, "Arcane v" + Arcane.version);
			stream.close();
		} catch (IOException ex) {
			throw new ArcaneException("Error saving \"" + fileName + "\".", ex);
		}
	}

	public void addSaveListener (SavePreferencesListener listener) {
		saveListeners.add(listener);
	}

	public boolean isEnglishLanguage () {
		return language.toLowerCase().equals("english");
	}

	static public enum CardImageType {
		none, local, wizards
	}

	static public enum CardFontSize {
		small, medium, large
	}

	static public enum CardFontFamily {
		tahoma, arial, verdana
	}
}
