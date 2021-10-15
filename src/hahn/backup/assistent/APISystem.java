package hahn.backup.assistent;

import hahn.rmi.APIObject;
import hahn.rmi.RemoteExecution;

/**
 * Diese Klasse bietet elemtare Systemfunktionen für die API an.
 * 
 * @author Manuel Hahn
 * @since 10.04.2018
 */
@RemoteExecution
public class APISystem implements APIObject {
	@Override
	public void parseInData(String data) {
		return;
	}
	
	/**
	 * Gibt Daten zur RAM-Benutzung aus.
	 */
	public String displayRAMStats() {
		Runtime runtime = Runtime.getRuntime();
		System.out.println("Total KB: " + runtime.totalMemory() / 1024);
		System.out.println("Used  KB: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024);
		return "Free  KB: " + runtime.freeMemory();
	}

	public String toUppercase(StringTest text) {
		return text.toString().toUpperCase();
	}

	@Override
	public byte[] convertToBytes() {
		return null;
	}
}