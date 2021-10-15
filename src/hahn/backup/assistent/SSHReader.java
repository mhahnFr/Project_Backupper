package hahn.backup.assistent;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Liest die Konsole.
 * 
 * @author Manuel Hahn
 * @since 10.04.2018
 */
public class SSHReader implements Closeable, AutoCloseable {
	/**
	 * Der zentrale Reader.
	 */
	private BufferedReader reader;
	
	/**
	 * Initialisiert den Reader.
	 */
	public SSHReader(InputStream is) {
		InputStreamReader isr;
		if(is == null) {
			isr = new InputStreamReader(System.in);
		} else {
			isr = new InputStreamReader(is);
		}
		reader = new BufferedReader(isr);
	}
	
	/**
	 * Liest eine eingegebene Zeile auf der Konsole ein.
	 * 
	 * @return die eingelesene Zeile
	 * @throws IOException sollte dabei etwas schiefgehen
	 */
	public String getCommand() throws IOException {
		return reader.readLine();
	}
	
	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			System.err.println("Fehler aufgetreten: " + e.getMessage());
			e.printStackTrace();
			System.err.println("-------------------------------------");
		}
	}
}