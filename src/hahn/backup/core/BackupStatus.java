package hahn.backup.core;

/**
 * Dieses Enum zählt auf, welche Stadien ein Backup durchläuft.
 * 
 * @author Manuel Hahn
 * @since 20.03.2018
 */
public enum BackupStatus {
	/**
	 * Bedeutet, dass das Backup gerade (auf die Festplatte) geschrieben wird.
	 */
	WRITING,
	/**
	 * Bedeutet, dass gerade alte Backups gelöscht werden.
	 */
	DELETING,
	/**
	 * Aufräumarbeiten nach einem Backup.
	 */
	CLEANING_UP,
	/**
	 * Das Backup wurde abgebrochen.
	 */
	ABORTED
}