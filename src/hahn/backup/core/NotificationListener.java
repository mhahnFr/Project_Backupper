package hahn.backup.core;

/**
 * Dieses Interface bietet nur eine Methode, damit Klassen, die sich gegenseitig aufrufen müssen
 * keine Probleme mit komplizierten Rückkopplungen kriegen.
 * 
 * @author Manuel Hahn
 * @since 31.01.2018
 */
public interface NotificationListener {
	/**
	 * Dies ist die Methode, die Rückkopplungen ohne Probleme ermöglicht. Sie
	 * kann, muss aber nicht, benötigte Informationen zurückgeben und zur Bearbeitung
	 * Objekte erhalten.
	 * 
	 * @param objects beliebige benötigte Objekte
	 * @return das Ergebnis (wenn vorhanden)
	 */
	Object notify(Object... objects);
}