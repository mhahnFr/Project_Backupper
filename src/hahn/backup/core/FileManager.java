package hahn.backup.core;

import hahn.backup.assistent.AssistentClient;
import hahn.backup.assistent.AssistentServer;
import hahn.backup.gui.MainWindow;
import hahn.rmi.APIInterpreter;
import hahn.rmi.APIObject;
import hahn.rmi.RemoteExecution;
import hahn.utils.ByteHelper;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

/**
 * Diese Klasse kümmert sich um das meiste, was mit Dateien zu tun hat.
 * 
 * @author Manuel Hahn
 * @since 19.10.2017
 */
public class FileManager implements APIObject {
	/**
	 * Der Schlüssel für die Ordner, die gesichert werden sollen.
	 * 
	 * @deprecated Ersetzt durch {@link #PROFILES_FOR_BACKUP_KEY}
	 */
	@Deprecated
	private static final String PROFILES_FOR_BACKUP_KEY_OLD = "hahn.projectBackupper.backUpProfiles";
	/**
	 * Der SChlüssel für die Backup-Profile.
	 */
	private static final String PROFILES_FOR_BACKUP_KEY		= "backupprofiles";
	/**
	 * Der Schlüssel für den Zeitintervall zwischen den Backups.
	 * 
	 * @deprecated Ersetzt durch {{@value #DELAY_BETWEEN_KEY}
	 */
	@Deprecated
	private static final String DELAY_BETWEEN_KEY_OLD 		= "hahn.projectBackupper.delay";
	/**
	 * Der Schlüssel für den Zeitintervall zwischen den Backups.
	 */
	private static final String DELAY_BETWEEN_KEY			= "delay";
	/**
	 * Der Schlüssel für die Unterdrückung der Warnungen bei nicht existierenden Ordnern.
	 * 
	 * @deprecated Ersetzt durch {@link #SUPPRESS_WARNINGS_KEY}
	 */
	@Deprecated
	private static final String SUPPRESS_WARNINGS_KEY_OLD 	= "hahn.projectBackupper.suppress";
	/**
	 * Der Schlüssel für die Unterdrückung von Warnungen.
	 */
	private static final String SUPPRESS_WARNINGS_KEY		= "suppresswarnings";
	/**
	 * Der Schlüssel für die x-Koordinate der Fensterposition.
	 */
	private static final String WINDOW_LOCATION_X_KEY	= "windowlocationx";
	/**
	 * Der Schlüssel für die y-Koordinate der Fensterposition.
	 */
	private static final String WINDOW_LOCATION_Y_KEY	= "windowlocationy";
	/**
	 * Der Schlüssel für die Fensterbreite.
	 */
	private static final String WINDOW_WIDTH_KEY		= "windowwidth";
	/**
	 * Der SChlüssel für die Fensterhöhe.
	 */
	private static final String WINDOW_HEIGHT_KEY		= "windowheight";
	/**
	 * Der Schlüssel für eine Einstellung, die die Version der Einstellungen erkennbar macht.
	 */
	private static final String IS_NEW_VERSION			= "isnewversion";
	/**
	 * Der Name des unsichtbaren Standardprofils für die Assistenzfunktion.
	 */
	public static final String STANDARD_PROFILE 		= "Standard-Profil";
	/**
	 * Die Einstellungen.
	 */
	private Preferences preferences;
	/**
	 * Eine Liste mit den Profilen für das Backup.
	 */
	private ArrayList<BackupProfile> profiles;
	/**
	 * Ob sich der Zeitintervall zwischen den Backups sich geändert hat.
	 */
	private volatile boolean needSaveD;
	/**
	 * Ob sich die Unterdrückung der Warnungen geändert hat.
	 */
	private volatile boolean needSaveS;
	/**
	 * Ob die Warnungen bei nicht existierenden Ordnern unterdrückt werden sollen.
	 */
	private boolean suppressNEWarnings;
	/**
	 * Der Zeitintervall zwischen den Backups.
	 */
	private int delay;
	/**
	 * Der Zeitpunkt, an welchem das letzte Backup gemacht wurde.
	 */
	private Date lastBackupDate;
	/**
	 * Der Dateiname, der im Warnungsdialog angezeigt werden soll.
	 * 
	 * @see #edtJO
	 */
	private String fileName;
	/**
	 * Der Punkt, an welchem sich das Backup gerade befindet.
	 */
	private BackupStatus status;
	/**
	 * Der Grund, der angezeigt werden soll, warum ein Fehler passiert ist.
	 * 
	 * @see #edtJO
	 */
	private String cause;
	/**
	 * Der Ordner für eine bestimmte Methode.
	 */
	private File lastDir;
	/**
	 * Zwischenspeicherung des Standardprofils.
	 * @see #getStandardProfile()
	 */
	private BackupProfile standard;
	/**
	 * Der Server, mit dem entfernt Methoden aufgerufen werden.
	 */
	private AssistentServer server;
	/**
	 * Der {@link APIInterpreter}, der ankommende Befehle erkennt und ausführt.
	 */
	private APIInterpreter apii;
	/**
	 * Der Thread, in dem der {@link #apii Interpreter} läuft.
	 */
	private Thread apiThread;
	/**
	 * Ein Runnable, das eine Warnung mit dem Text anzeigt.
	 * 
	 * @see #text
	 */
	private Runnable edtJO = () -> JOptionPane.showMessageDialog(null, "Konnte \"" + fileName + "\" nicht sichern.\n" 
			+ cause, "Problem beim Sichern einer Datei", JOptionPane.ERROR_MESSAGE);
	/**
	 * Ein Thread, mit welchem die Einstellungen gesichert werden.
	 */
	private Runnable settingsFlusher = () -> {
		System.out.println("Speicherung der Einstellungen");
		saveProfiles();
		if(needSaveD) {
			saveDelay();
			needSaveD = false;
		}
		if(needSaveS) {
			saveSuppress();
			needSaveS = false;
		}
		try {
			preferences.flush();
		} catch(BackingStoreException e) {
			System.err.println("Fehler aufgetreten: " + e.getMessage());
			e.printStackTrace();
			System.err.println("-------------------------------------");
		}
	};
	
	/**
	 * Initialisiert die Einstellungen, Listen...
	 */
	public FileManager() {
		preferences = Preferences.userNodeForPackage(getClass());
		boolean isNew = preferences.getBoolean(IS_NEW_VERSION, false);
		if(!isNew) {
			Preferences old = Preferences.userRoot();
			preferences.putByteArray(PROFILES_FOR_BACKUP_KEY, old.getByteArray(PROFILES_FOR_BACKUP_KEY_OLD, null));
			old.remove(PROFILES_FOR_BACKUP_KEY_OLD);
			preferences.putInt(DELAY_BETWEEN_KEY, old.getInt(DELAY_BETWEEN_KEY_OLD, 1000));
			old.remove(DELAY_BETWEEN_KEY_OLD);
			preferences.putBoolean(SUPPRESS_WARNINGS_KEY, old.getBoolean(SUPPRESS_WARNINGS_KEY_OLD, false));
			old.remove(SUPPRESS_WARNINGS_KEY_OLD);
			isNew = true;
			preferences.putBoolean(IS_NEW_VERSION, isNew);
		}
		profiles = new ArrayList<>();
		byte[] profiles = preferences.getByteArray(PROFILES_FOR_BACKUP_KEY, null);
		delay = preferences.getInt(DELAY_BETWEEN_KEY, 1);
		if(profiles != null) {
			readProfiles(profiles);
		}
		if(!MainWindow.assistent) {
			for(BackupProfile profile : this.profiles) {
				if(profile.getName().equalsIgnoreCase(STANDARD_PROFILE)) {
					continue;
				}
				HashMap<Date, File> lastBD = getLastBackups(profile.getLocationForBackups());
				if(!lastBD.isEmpty()) {
					Date[] lasts = Date.sort(lastBD.keySet().toArray(new Date[lastBD.size()]));
					profile.setPreviousBackupDate(lasts[lasts.length - 1]);
				}
			}
		}
	}
	
	/**
	 * Macht die entsprechend gekennzeichneten Methoden und Klassen entfernt ausführbar.
	 */
	public void startServer() {
		apiThread = new Thread(() -> settleServer());
		apiThread.setName("ServerThread");
		apiThread.start();
	}
	
	/**
	 * Setzt den Server auf.
	 */
	private void settleServer() {
		try {
			server = new AssistentServer(9090);
			server.establishConnection();
			final Socket s = server.getSocket();
			apii = new APIInterpreter(
					s.getInputStream(), new PrintStream(s.getOutputStream()), server.getObjectOutputStream());
			apii.run();
		} catch (IOException e) {
			System.err.println("Fehler aufgetreten: " + e.getMessage());
			if(MainWindow.VERBOSE) {
				e.printStackTrace();
				System.err.println("-------------------------------------");
			}
		}
	}
	
	/**
	 * Gibt die Liste mit den Profilen für Backups zurück.
	 * 
	 * @return die Liste mit den BackupProfilen
	 */
	public BackupProfile[] getProfiles() {
		return profiles.toArray(new BackupProfile[profiles.size()]);
	}

	/**
	 * Fügt das angegebene Profil der Liste hinzu.
	 * 
	 * @param p das zu speichernde Profil
	 */
	public void addProfile(BackupProfile p) {
		profiles.add(p);
	}
	
	/**
	 * Löscht das angegebene Profil aus der Liste.
	 * 
	 * @param p das zu löschende Profil
	 */
	public void deleteProfile(BackupProfile p) {
		profiles.remove(p);
	}
	
	/**
	 * Setzt, ob die Warnungen bei nicht existierenden Ordnern ignoriert werden sollen oder nicht.
	 * 
	 * @param suppress ob die Warnungen unterdrückt werden sollen
	 */
	public void setSuppressNEWarnings(boolean suppress) {
		suppressNEWarnings = suppress;
		needSaveS = true;
	}
	
	/**
	 * Gibt den Status des Backups zurück. Wenn gerade nichts getan wird, wird {@code null} zurückgegeben.
	 * 
	 * @return den Punkt, an welchem sich das Backup gerade befindet
	 */
	public BackupStatus getStatus() {
		return status;
	}
	
	/**
	 * Teilt dem FileManager mit, dass der Vorgang abgebrochen wird.
	 */
	public void willAbort() {
		if(status == BackupStatus.WRITING) {
			status = BackupStatus.ABORTED;
		}
	}
	
	/**
	 * Gibt zurück, ob der erste Pfad dem Originalpfad entspricht. Vergleicht die beiden angegebenen Pfade,
	 * geht dabei davon aus, dass der erste Pfad ein Backuppfad ist und der zweite Pfad der Originalpfad.
	 * 
	 * @param backuppedFilePath der Pfad in einem Backup
	 * @param originalFilePath der Originalpfad
	 * @return ob davon ausgegangen werden kann, dass die gleiche Datei gemeint ist
	 */
	@SuppressWarnings("unused")
	private boolean checkEquality(String backuppedFilePath, String originalFilePath, BackupProfile profile) {
		try {
			backuppedFilePath = 
					backuppedFilePath.substring(profile.getLocationForBackups().getAbsolutePath().length() + 18, backuppedFilePath.length());
			originalFilePath = originalFilePath.substring(originalFilePath.length() - backuppedFilePath.length());
		} catch(IndexOutOfBoundsException e) {
			if(MainWindow.VERBOSE) {
				System.err.println("checkEquality(String, String) nicht vergleichbar!");
			}
		}
		return backuppedFilePath.equals(originalFilePath);
	}
	
	/**
	 * Gibt eine Liste mit den Ordnern der letzten Backups, verknüpft mit den Daten,
	 * für den angegebenen Ordner zurück.
	 * 
	 * @param backupDir der Ordner mit den Backups
	 * @return eine Liste mit den letzten Backups und den zugehörigen Daten
	 */
	private HashMap<Date, File> getLastBackups(File backupDir) {
		File[] dirs = backupDir.listFiles(file -> file.isDirectory());
		HashMap<Date, File> lastBD = new HashMap<>();
		if(dirs.length > 0) {
			for(File dir : dirs) {
				try {
					lastBD.put(Date.parseDate(dir.getName()), dir);
				} catch(NumberFormatException | IndexOutOfBoundsException e) {
					System.out.println("Kein Backup-Ordner: " + dir.getName());
				}
			}
		}
		return lastBD;
	}
	
	/**
	 * Die zentrale Backuproutine. Sichert ein Kopie der angegebenen Ordnern in dem Ordner, in das 
	 * die Backups geschrieben werden sollen, und verlinkt Dateien, die sich nicht geändert haben, 
	 * unter den Backups zueinander. Hinterher wird aufgeräumt.
	 * 
	 * @return gibt zurück, ob alles geklappt hat
	 */
	public boolean backupAll() {
		status = BackupStatus.WRITING;
		lastBackupDate = new Date();
		//File[] backupFolders = new File[profiles.size() - 1];
		//int counter = 0;
		int ppp = 100 / profiles.size();
		for(BackupProfile profile : profiles) {
			if(profile.getName().equalsIgnoreCase(STANDARD_PROFILE)) {
				MainWindow.addPBarValue(ppp);
				continue;
			}
			/*if(!profile.hasOldFiles()) {
				fillLastBackupList(profile);
			}*/
			File backupDes = new File(profile.getLocationForBackups() + File.separator + lastBackupDate.toString());
			/*if(profile.needsAssistent()) {
				try {
					profile.getClient().getObjectInputStream().skipBytes(profile.getClient().getObjectInputStream().available());
				} catch (IOException e) {
					System.err.println("Fehler aufgetreten: " + e.getMessage());
					e.printStackTrace();
					System.err.println("-------------------------------------");
				}
			}*/
			//backupFolders[counter] = backupDes;
			/*if(profile.needsAssistent()) {
				//AssistentClient client = profile.getClient();
				profile.getClient().getPrintStream().println("BackupProfile:getLocationForBackups()");
				//client.getPrintStream().println("FileManager:getStandardProfile()");
				try {
					backupDes = (File) profile.getClient().getObjectInputStream().readObject();
					//backupDes = ((BackupProfile) client.getObjectInputStream().readObject()).getLocationForBackups();
				} catch (ClassNotFoundException | IOException e) {
					System.err.println("Fehler aufgetreten: " + e.getMessage());
					if(MainWindow.VERBOSE) {
						e.printStackTrace();
						System.err.println("-------------------------------------");
					}
				}
				backupDes = new File(backupDes.getAbsolutePath() + File.separator + lastBackupDate.toString());
				
			}*/
			backupDes.mkdir();
			if(profile.needsAssistent()) {
				if(!profile.isClientConnected()) {
					MainWindow.addPBarValue(ppp);
					continue;
				}
				backupDes = new File(profile.getLocationForBackupsAPOV().getAbsolutePath() + 
						File.separator + lastBackupDate.toString());
			}
			copyFiles(profile.getFolders(), backupDes, profile);
			MainWindow.addPBarValue(ppp);
			//counter++;
		}
		/*File backupDestination = new File(backupOnDisk.getAbsolutePath() + File.separator + lastBackupDate.toString());
		backupDestination.mkdir();
		ArrayList<File> listToBackup = backupDirectories;
		copyFiles(listToBackup.toArray(new File[listToBackup.size()]), backupDestination);*/
		/*counter = 0;
		for(File folder : backupFolders) {
			if(folder.list().length == 0) {
				counter++;
			}
		}
		if(counter == backupFolders.length) {
			return true;
		}*/
		return cleanUp();
	}
	
	/**
	 * Löscht das neueste Backup, wenn der Schreibvorgang abgebrochen wurde.
	 * 
	 * @param list eine Liste mit den letzten Backups und deren Zeitpunkten
	 */
	private void deleteNewestBackup(HashMap<Date, File> list) {
		status = BackupStatus.DELETING;
		File lastBackup = list.get(lastBackupDate);
		if(lastBackup != null) {
			deleteOldDirectory(lastBackup);
		}
	}
	
	/**
	 * Räumt nach einem Backup auf. Löscht zu alte Backups.
	 * 
	 * @return ob das Aufräumen erfolgreich war oder nicht
	 */
	public boolean cleanUp() {
		for(BackupProfile profile : profiles) {
			if(profile.getName().equalsIgnoreCase(STANDARD_PROFILE)) {
				continue;
			}
			HashMap<Date, File> lastBD = getLastBackups(profile.getLocationForBackups());
			if(status == BackupStatus.ABORTED) {
				deleteNewestBackup(lastBD);
				continue;
			}
			status = BackupStatus.DELETING;
			File backupFolder = lastBD.get(lastBackupDate);
			if(backupFolder == null) {
				continue;
			}
			if(backupFolder.list().length == 0) {
				boolean deleted = backupFolder.delete();
				if(MainWindow.VERBOSE) {
					System.out.println("Leerer Backup-Ordner gelöscht: " + deleted);
					System.out.println("Ordner: " + backupFolder);
				}
				continue;
			}
			profile.setPreviousBackupDate(lastBackupDate);
			if(!lastBD.isEmpty() && lastBackupDate != null) {
			//Date[] lasts = Date.sort(lastBD.keySet().toArray(new Date[lastBD.size()]));
				Date[] lasts = lastBD.keySet().toArray(new Date[lastBD.size()]);
				ArrayList<Date> beforeTodays = new ArrayList<>();
				Date today = new Date(lastBackupDate.getYear(), lastBackupDate.getMonth(), lastBackupDate.getDay(),
						(byte) 0, (byte) 0, (byte) 0);
				for(Date date : lasts) {
					if(date.isBefore(today)) {
						beforeTodays.add(date);
					}
				}
				Date[] bTodays = Date.sort(beforeTodays.toArray(new Date[beforeTodays.size()]));
				// Löschschleife
				for(int i = 0; i < bTodays.length - 1; i++) {
					deleteOldDirectory(lastBD.get(bTodays[i]));
				}
				status = BackupStatus.CLEANING_UP;
			// -------------------
			// Funktioniert nur, wenn ein oder mehrere Backups von gestern existieren
			/*Date yesterday = lastBackupDate.getPreviousDay();
			Date lastYesterday = new Date(yesterday.getYear(), yesterday.getMonth(), yesterday.getDay(), 
					(byte) 23, (byte) 59, (byte) 59);
			ArrayList<Date> yesterdays = new ArrayList<>();
			for(int i = 0; i < lasts.length; i++) {
				if(lasts[i].isBefore(yesterday)) {
					deleteOldDirectory(lastBD.get(lasts[i]));
				} else if(lasts[i].isBefore(lastYesterday)) {
					yesterdays.add(lasts[i]);
				}
			}
			lasts = Date.sort(yesterdays.toArray(new Date[yesterdays.size()]));
			Date lastStanding = lasts[lasts.length - 1];
			for(int i = 0; i < lasts.length - 1; i++) {
				if(lasts[i].isBefore(lastStanding)) {
					deleteOldDirectory(lastBD.get(lasts[i]));
				}
			}*/
			}
		}
		//MainWindow.setPBarValue(100);
		status = null;
		return true;
	}
	
	/**
	 * Löscht den angegebenen Ordner. Der Ordner wird geleert, sollten sich noch Dateien
	 * in dem angegebenen Ordner befinden.
	 * 
	 * @param directory der zu löschende Ordner
	 */
	private void deleteOldDirectory(File directory) {
		for(File file : directory.listFiles()) {
			if(file.isDirectory()) {
				deleteOldDirectory(file);
			} else {
				file.delete();
			}
		}
		directory.delete();
	}
	
	/**
	 * Kürzt den Originalordner um den zweiten Ordner nach folgendem Schema:
	 * {@code makeIndepend(/Users/admin/Documents/etc, /Users/admin)} gibt zurück: {@code Documents/etc}.
	 * 
	 * @param orig der Ordner, der gekürzt werden soll
	 * @param bl die Kürzungsdefinition
	 * @return den um bl gekürzten orig
	 */
	private String makeIndepend(File orig, File bl) {
		return orig.getAbsolutePath().substring(bl.getAbsolutePath().length());
	}
		
	/**
	 * Dreht die Zeichen des angegebenen Strings um. Beispiel: Aus {@code Documents}
	 * wird {@code stnemucoD}.
	 * 
	 * @param toFlip der umzukehrende String
	 * @return den umgedrehten String
	 */
	@RemoteExecution
	public String flipString(String toFlip) {
		char[] cs = toFlip.toCharArray();
		char[] flipCs = new char[cs.length];
		for(int oi = cs.length - 1, fi = 0; fi < flipCs.length; oi--, fi++) {
			flipCs[fi] = cs[oi];
		}
		return new String(flipCs);
	}
	
	/**
	 * Gibt zurück, ob das angegebene {@link File} ein Ordner ist oder nicht.
	 * 
	 * @param file die zu prüfende Datei
	 * @return ob die Datei ein Ordner ist
	 */
	@RemoteExecution
	public boolean isDirectoryStringFile(String file) {
		return new File(file).isDirectory();
	}
	
	/**
	 * Gibt das letzte bekannte Backup der angegebenen Datei des angegebenen Profils zurück. Die angegebene 
	 * Datei muss eine bereits im Backup befindliche Datei sein. Nur der Zeitstempel des Pfades wird getauscht.
	 * 
	 * @param file die Datei, dessen letztes Backup gesucht wird
	 * @param profile das {@link BackupProfile Profil}, aus dem die Datei stammt
	 * @return das letzte bekannte Backup der gesuchten Datei
	 */
	private File getLastBackupOf(String file, BackupProfile profile) {
		String oldBackupFolder = profile.getLocationForBackups().getAbsolutePath() + File.separator 
				+ profile.getPreviousBackupDate() + File.separator;
		int cutLength = oldBackupFolder.length();
		if(profile.needsAssistent()) {
			cutLength = (profile.getLocationForBackupsAPOV().getAbsolutePath() + File.separator
					+ profile.getPreviousBackupDate() + File.separator).length();
		}
		file = file.substring(cutLength);
		return new File(oldBackupFolder + file);
	}
	
	/**
	 * Kopiert oder verlinkt die Dateien aus dem Array in den angegebenen Ordner. Verlinkt eine Datei, 
	 * wenn sie sich in der Liste der zuletzt gesicherten Dateien befindet.
	 * 
	 * @param files die Dateien, die nach backupDir sollen
	 * @param backupDir der Ort, an den die Dateien hinkopiert oder verlinkt werden sollen
	 */
	private void copyFiles(File[] files, File backupDir, BackupProfile profile) {
		for(File file : files) {
			if(!profile.needsAssistent()) {
				if(!file.exists()) {
					continue;
				}
			}
			long lastEdit = file.lastModified();
			String guessedBackupFile = backupDir.getAbsolutePath() + File.separator + file.getName();
			File listLE = getLastBackupOf(guessedBackupFile, profile);
			boolean saved = listLE != null;
			File backuppedFile = null;
			String absolutePath = file.getAbsolutePath();
			boolean isDir = file.isDirectory();
			try {
				AssistentClient client = null;
				PrintStream ps = null;
				if(profile.needsAssistent()) {
					client = profile.getClient();
					ps = client.getPrintStream();
					ps.println("FileManager:getLastModifiedByString(String " + absolutePath + ")");
					lastEdit = (Long) client.getObjectInputStream().readObject();
					ps.println("FileManager:isDirectoryStringFile(String " + absolutePath + ")");
					isDir = (Boolean) client.getObjectInputStream().readObject();
				}
				if(saved && !isDir && lastEdit <= listLE.lastModified()) {
					// Verlinken
					Path newLink;
					if(profile.needsAssistent()) {
						File loc = profile.getLocationForBackups();
						newLink = new File(loc.getAbsolutePath() + File.separator + 
								makeIndepend(backupDir, profile.getLocationForBackupsAPOV()) + 
								File.separator + file.getName()).toPath();
					} else {
						newLink = new File(guessedBackupFile).toPath();
					}
					backuppedFile = Files.createLink(newLink, listLE.toPath()).toFile();
					if(MainWindow.VERBOSE) {
						System.out.println("Linked file:   " + backuppedFile);
					}
				} else { 
					// Kopieren
					if(profile.needsAssistent()) {
						byte[] buffer = new byte[Integer.BYTES];
						ps.println("FileManager:getStandardProfile()");
						client.getSocketInputStream().read(buffer);
						buffer = new byte[ByteHelper.bytesToInt(buffer)];
						client.getSocketInputStream().read(buffer);
						BackupProfile assistentStandard = new BackupProfile(buffer);
						ps.println("FileManager:setCurrentDirectory"
								+ "(String " + makeIndepend(backupDir, assistentStandard.getLocationForBackups()) + ")");
						ps.println("FileManager:copyFile(String " + absolutePath + ")");
						backuppedFile = (File) client.getObjectInputStream().readObject();
					} else {
						// TODO Aliasse können nicht korrekt gesichert werden
						Path filePath = file.toPath();
						if(Files.isSymbolicLink(filePath)) {
							Files.createSymbolicLink(new File(guessedBackupFile).toPath(), filePath.toRealPath());
						} else {
						backuppedFile = Files.copy(filePath, new File(guessedBackupFile).toPath(), 
								StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS).toFile();
						}
					}
					if(isDir) {
						File[] dirFiles = file.listFiles();
						if(profile.needsAssistent()) {
							ps.println("FileManager:listFilesByString(String " + absolutePath + ")");
							dirFiles = (File[]) client.getObjectInputStream().readObject();
						}
						copyFiles(dirFiles, backuppedFile, profile);
						if(MainWindow.VERBOSE) {
							System.out.println("Copied folder: " + backuppedFile);
						}
					} else {
						if(MainWindow.VERBOSE) {
							System.out.println("Copied file:   " + backuppedFile);
						}
					}
					/*if(profile.needsAssistent()) {
						backuppedFile = new File(profile.getLocationForBackups().getAbsolutePath() + File.separator + 
								makeIndepend(backupDir, profile.getLocationForBackupsAPOV()) + 
								File.separator + file.getName());
					}*/
				}
			} catch(Exception e) {
				System.err.println("Fehler aufgetreten: " + e.getMessage());
				e.printStackTrace();
				System.err.println("-------------------------------------");
				if(!suppressNEWarnings) {
					fileName = file.getPath();
					String c = e.getMessage();
					cause = c == null ? "Unbekannter Fehler aufgetreten!" : e.getLocalizedMessage();
					if(e instanceof NoSuchFileException) {
						cause = "Datei/Ordner nicht gefunden!";
					}
					EventQueue.invokeLater(edtJO);
				}
			}
		}
	}
	
	/**
	 * Gibt alle Dateien zurück, die in dem gesuchten Ordner sind.
	 * 
	 * @param path der Pfad zum Ordner
	 * @return alle Dateien und Ordner, die sich im gesuchten Ordner befinden
	 */
	@RemoteExecution
	public File[] listFilesByString(String path) {
		return new File(path).listFiles();
	}
	
	/**
	 * Gibt das letzte Änderungsdatum der gesuchten Datei zurück.
	 * 
	 * @param path der Pfad zu der Datei oder Ordner, dessen letztes Änderungsdatum gesucht wird
	 * @return das letzte Änderungsdatum der gesuchten Datei
	 */
	@RemoteExecution
	public long getLastModifiedByString(String path) {
		return new File(path).lastModified();
	}
	
	/**
	 * Setzt den Ordner für {@link #copyFile(String)}
	 * 
	 * @param dir der Ort
	 */
	@RemoteExecution
	public void setCurrentDirectory(String dir) {
		lastDir = new File(getStandardProfile().getLocationForBackups().getAbsolutePath() + File.separator + dir);
	}
	
	/**
	 * Kopiert die angegebene Datei an den Ort des Backups.
	 * 
	 * @param fileName die zu kopierende Datei
	 * @throws IOException sollte ein Fehler beim Kopieren auftreten
	 */
	@RemoteExecution
	public File copyFile(String fileName) throws IOException {
		File file = new File(fileName);
		return Files.copy(file.toPath(), new File(lastDir + File.separator + file.getName()).toPath(), 
				StandardCopyOption.COPY_ATTRIBUTES).toFile();
	}	
	
	/**
	 * Liest die Profile aus den Einstellungen ein.
	 * 
	 * @param bytes die zu interpretierenden bytes
	 */
	private void readProfiles(byte[] bytes) {
		final int count = ByteHelper.bytesToInt(bytes);
		int length, byteCount = Integer.BYTES;
		for(int i = 0; i < count; i++) {
			length = ByteHelper.bytesToInt(ByteHelper.subBytes(bytes, byteCount, byteCount += Integer.BYTES));
			profiles.add(new BackupProfile(ByteHelper.subBytes(bytes, byteCount, byteCount += length)));
		}
	}
	
	/**
	 * Ändert den zeitlichen Abstand zwischen den Backups. Der Abstand wird in 
	 * 
	 * @param newDelay
	 */
	public void delayChanged(int newDelay) {
		delay = newDelay;
		needSaveD = true;
	}
	
	/**
	 * Sichert die Einstellung, welcher zeitliche Abstand zwischen den Backups eingehalten werden soll.
	 */
	private void saveDelay() {
		preferences.putInt(DELAY_BETWEEN_KEY_OLD, delay);
	}
	
	/**
	 * Sichert die Einstellung, ob die Warnungen unterdrückt werden sollen oder nicht.
	 */
	private void saveSuppress() {
		preferences.putBoolean(SUPPRESS_WARNINGS_KEY_OLD, suppressNEWarnings);
	}
	
	/**
	 * Exportiert die Liste mit den für Backups registrierten Ordnern.
	 * 
	 * @param file die Datei, in welche die Ordner exportiert werden sollen
	 * @param p das Profil, dessen Ordner exportiert werden sollen
	 */
	public void exportFolders(File file, BackupProfile p) {
		if(!file.getName().endsWith(".txt")) {
			if(!file.getName().endsWith(".TXT")) {
				file = new File(file.getAbsolutePath() + ".txt");
			}
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			for(File toSave : p.getFolders()) {
				writer.write(toSave.getAbsolutePath());
				writer.newLine();
			}
		} catch (IOException e) {
			System.err.println("Fehler aufgetreten: " + e.getMessage());
			e.printStackTrace();
			System.err.println("-------------------------------------");
		}
	}
	
	/**
	 * Importiert die in der Datei angegebenen Ordner, sollten sie nicht bereits auf der Liste
	 * stehen.
	 * 
	 * @param file die Datei, aus der die Ordner importiert werden sollen
	 * @param p das Profil, in welches die Ordner importiert werden sollen
	 */
	public void importFolders(File file, BackupProfile p) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String read;
			File toAdd;
			while(reader.ready()) {
				read = reader.readLine();
				if(read != null && !read.equals("")) {
					toAdd = new File(read);
					if(toAdd.exists() && p.indexOfFolder(toAdd) == -1) {
						p.addFolder(toAdd);
					}
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("Kann eigentlich nicht passieren! Datei nicht gefunden.");
			e.printStackTrace();
			System.err.println("-------------------------------------");
		} catch (IOException e) {
			System.err.println("Fehler aufgetreten: " + e.getMessage());
			e.printStackTrace();
			System.err.println("-------------------------------------");
		}
	}
	
	/**
	 * Sorgt dafür, dass alle Einstellungen gesichert werden. Dazu wird ein neuer Thread gestartet,
	 * um den aufrufenden Thread nicht auszubremsen. Wenn das nicht erwünscht ist, sollten die 
	 * Sicherungsmethoden der Einstellungen direkt aufgerufen werden.
	 */
	public void flushSettings() {
		new Thread(settingsFlusher).start();
	}
	
	/**
	 * Gibt zurück, ob die Warnungen bei Fehlern beim Sichern von Dateien unterdrückt
	 * werden sollen oder nicht.
	 * 
	 * @return ob die Warnungen unterdrückt werden sollen
	 */
	public boolean getSuppressNEWarnings() {
		return suppressNEWarnings;
	}
	
	/**
	 * Gibt den zeitlichen Abstand zwischen den Backups zurück.
	 * 
	 * @return den Zeitabstand zwischen den Backups
	 */
	public int getDelay() {
		return delay;
	}
	
	/**
	 * Gibt das Profil mit dem Standardnamen zurück. Existiert es nicht, wird es erstellt.
	 * Existieren mehrere mit dem selben Namen, wird das erste gefundene zurückgegeben.
	 * 
	 * @return das Standardprofile
	 */
	@RemoteExecution
	public BackupProfile getStandardProfile() {
		if(standard == null) {
			for(BackupProfile profile : getProfiles()) {
				if(profile.getName().equals(STANDARD_PROFILE)) {
					standard = profile;
					break;
				}
			}
			if(standard == null) {
				standard = new BackupProfile(STANDARD_PROFILE);
				standard.setLocationForBackup(new File("/dev/null/"));
				addProfile(standard);
			}
		}
		return standard;
	}
	
	/**
	 * Sichert die Profile in den Einstellungen.
	 */
	private void saveProfiles() {
		ArrayList<byte[]> bytes = new ArrayList<>();
		bytes.add(ByteHelper.intToBytes(profiles.size()));
		int length = Integer.BYTES;
		for(BackupProfile profile : profiles) {
			byte[] p = profile.convertToBytes();
			bytes.add(ByteHelper.intToBytes(p.length));
			length += Integer.BYTES;
			bytes.add(p);
			length += p.length;
		}
		byte[] toSave = new byte[length];
		int byteCount = 0;
		for(byte[] bts : bytes) {
			for(byte b : bts) {
				toSave[byteCount] = b;
				byteCount++;
			}
		}
		preferences.putByteArray(PROFILES_FOR_BACKUP_KEY_OLD, toSave);
	}
	
	/**
	 * Gibt zurück, ob ein Backup erstellt werden kann oder nicht.
	 * 
	 * @return ob Ordner registriert sind
	 */
	@RemoteExecution
	public boolean hasFolders() {
		if(profiles != null) {
			if(profiles.size() == 0) {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	public void finalize() {
		flushSettings();
	}
	
	/**
	 * Deinstalliert die Einstellungen dieses Programms.
	 */
	public void uninstall() {
		preferences.remove(DELAY_BETWEEN_KEY_OLD);
		preferences.remove(SUPPRESS_WARNINGS_KEY_OLD);
		preferences.remove(PROFILES_FOR_BACKUP_KEY_OLD);
	}

	@Override
	public byte[] convertToBytes() {
		return null;
	}
}