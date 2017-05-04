package keboola.ftp.extractor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author David Esner
 */
public class Main {

	public static void main(String[] args) {
		try {
			Extractor.run(args);
		} catch (Exception e) {
			Logger.getAnonymousLogger().log(Level.SEVERE, "Extraction failed", e);
			System.exit(2);
		}

	}

}
