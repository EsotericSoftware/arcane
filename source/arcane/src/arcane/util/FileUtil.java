
package arcane.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides file utility methods.
 */
public class FileUtil {
	private FileUtil () {
	}

	/**
	 * A {@link FileFilter} that accepts only directories.
	 */
	static public FileFilter directoryFilter = new FileFilter() {
		public boolean accept (File file) {
			return file.isDirectory();
		}
	};

	/**
	 * Returns a {@link FilenameFilter} that accepts only filenames that end with the specified string.
	 */
	static public FilenameFilter filenameEndsWith (final String endsWith) {
		return new FilenameFilter() {
			public boolean accept (File dir, String filename) {
				return filename.toLowerCase().endsWith(endsWith);
			}
		};
	}

	static public javax.swing.filechooser.FileFilter chooserFileEndsWith (final String description, final String endsWith) {
		return new javax.swing.filechooser.FileFilter() {
			public boolean accept (File file) {
				if (file.isDirectory()) return true;
				return file.getName().toLowerCase().endsWith(endsWith);
			}

			public String getDescription () {
				return description;
			}
		};
	}

	/**
	 * Copies one file to another.
	 */
	static public void copyFile (File in, File out) throws IOException {
		FileChannel sourceChannel = new FileInputStream(in).getChannel();
		FileChannel destinationChannel = new FileOutputStream(out).getChannel();
		sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
		sourceChannel.close();
		destinationChannel.close();
	}

	/**
	 * Deletes a directory and all files and directories it contains.
	 */
	static public boolean deleteDirectory (File file) {
		if (file.exists()) {
			File[] files = file.listFiles();
			for (int i = 0, n = files.length; i < n; i++) {
				if (files[i].isDirectory())
					deleteDirectory(files[i]);
				else
					files[i].delete();
			}
		}
		return file.delete();
	}

	static public String getExtension (String fileName) {
		Pattern pattern = Pattern.compile(".+\\.([^\\.\\/]+)$");
		Matcher matcher = pattern.matcher(new File(fileName).getName());
		if (!matcher.matches()) return null;
		return matcher.group(1);
	}
}
