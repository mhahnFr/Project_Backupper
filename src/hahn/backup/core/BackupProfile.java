package hahn.backup.core;

import java.io.File;
import java.lang.Thread.State;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.swing.Timer;

import hahn.backup.assistent.AssistentClient;
import hahn.backup.gui.MainWindow;
import hahn.rmi.APIObject;
import hahn.rmi.RemoteExecution;
import hahn.utils.ByteHelper;

/**
 * Objekte dieser Klasse sind Profile, also die Details wie ein Backup
 * erstellt werden soll.
 * 
 * @author Manuel Hahn
 * @since 17.04.2018
 */
public class BackupProfile implements APIObject {
	/**
	 * Der Name des Profils.
	 */
	private String name;
	/**
	 * Die Liste mit sämtlichen Ordnern, die diesem Profil zugewiesen sind.
	 */
	private ArrayList<File> folders;
	/**
	 * Der Ort, in das dieses Profil gesichert werden soll.
	 */
	private File location;
	/**
	 * Repräsentiert den Blickpunkt des Assistenten auf den {@link #location BackupOrdner}.
	 */
	private File locationAPOV;
	/**
	 * Zeigt an, ob die Assistenzfunktion für Backups mit diesem Profil nötig ist.
	 */
	private boolean needAssistent;
	/**
	 * Die IP-Adresse des zu benutzenden Servers.
	 */
	private String ipAddress;
	/**
	 * Der Zeitpunkt des Backups vor dem zuletzt erstellten.
	 */
	private Date previousBackupDate;
	/**
	 * Der bestimmte Client, mit dem sich das Programm verbinden muss, um Backups
	 * erstellen zu können.
	 */
	private AssistentClient client;
	/**
	 * Der Thread, in dem der {@link #clientSettler} läuft.
	 */
	private Thread settleThread;
	/**
	 * Das Runnable mit der Verbindungsroutine.
	 */
	private Runnable clientSettler = new Runnable() {
		@Override
		public void run() {
			Timer timer = new Timer(5000, null);
			timer.addActionListener(e -> {
				if(!settle()) {
					timer.restart();
				}
			});
			timer.setRepeats(false);
			if(!settle()) {
				timer.start();
			}
		}
		
		/**
		 * Verbindet den Client mit dem gespeicherten Server.
		 * 
		 * @return ob die Verbindung steht
		 */
		private boolean settle() {
			try {
				InetAddress ipa = InetAddress.getByName(ipAddress);
				if(!ipa.isReachable(1000)) {
					return false;
				}
				client.establishConnection();
				var out = client.getPrintStream();
				out.println("import hahn.backup.core.BackupProfile");
				out.println("import hahn.backup.core.FileManager");
				out.println("import hahn.backup.assistent.APIInterpreter");
				out.println("import java.lang.String");
				out.println("hahn.backup.assistent.APIInterpreter:setByteModeEnabled(java.lang.Boolean true)");
				out.println("BackupProfile:getFolders()");
				File[] files = (File[]) client.getObjectInputStream().readObject();
				for(File file : files) {
					if(!isFolderOnList(file)) {
						addFolder(file);
					}
				}
				out.println("BackupProfile:getLocationForBackups()");
				locationAPOV = (File) client.getObjectInputStream().readObject();
				return true;
			} catch(ConnectException e) {
				System.err.println("Verbindungsprobleme: Erneuter Versuch in 5 Sekunden.");
				if(MainWindow.VERBOSE) {
					System.err.println("Fehler aufgetreten: " + e.getMessage());
					e.printStackTrace();
					System.err.println("-------------------------------------");
				}
			} catch(Exception e) {
				System.err.println("Fehler aufgetreten: " + e.getMessage());
				e.printStackTrace();
				System.err.println("-------------------------------------");
			}
			return false;
		}
	};
	
	/**
	 * Erzeugt ein Profil mit dem angegebenen Namen, der kann nicht 
	 * mehr verändert werden!
	 * 
	 * @param name der unveränderliche Name dieses Profils
	 */
	public BackupProfile(String name) {
		this();
		this.name = name;
	}
	
	/**
	 * Erzeugt benötigte Strukturen innerhalb eines Profils, die immer gleich sind.
	 */
	private BackupProfile() {
		folders = new ArrayList<>();
	}
	
	/**
	 * Erzeugt ein Profile mit den angegebenen Informationen.
	 * 
	 * @param bytes die zu interpretierenden bytes
	 */
	public BackupProfile(byte[] bytes) {
		this();
		int byteCount = Integer.BYTES;
		int length = ByteHelper.bytesToInt(bytes);
		name = new String(ByteHelper.subBytes(bytes, byteCount, byteCount += length));
		needAssistent = bytes[byteCount] == 1 ? true : false;
		byteCount++;
		if(needAssistent) {
			length = ByteHelper.bytesToInt(ByteHelper.subBytes(bytes, byteCount, byteCount += Integer.BYTES));
			ipAddress = new String(ByteHelper.subBytes(bytes, byteCount, byteCount += length));
		}
		length = ByteHelper.bytesToInt(ByteHelper.subBytes(bytes, byteCount, byteCount += Integer.BYTES));
		location = new File(new String(ByteHelper.subBytes(bytes, byteCount, byteCount += length)));
		int count = ByteHelper.bytesToInt(ByteHelper.subBytes(bytes, byteCount, byteCount += Integer.BYTES));
		for(int i = 0; i < count; i++) {
			length = ByteHelper.bytesToInt(ByteHelper.subBytes(bytes, byteCount, byteCount += Integer.BYTES));
			folders.add(new File(new String(ByteHelper.subBytes(bytes, byteCount, byteCount += length))));
		}
		if(needAssistent && !MainWindow.assistent) {
			settleClient();
		}
	}
	
	/**
	 * Gibt den Namen dieses Profils zurück.
	 * 
	 * @return den Name dieses Profils
	 */
	public String getName() {
		return name;
	}

	/**
	 * Fügt diesem Profil ein zu sichernden Ordner hinzu.
	 * 
	 * @param folder der Ordner, der mit diesem Profil gesichert werden soll
	 */
	public void addFolder(File folder) {
		folders.add(folder);
		
		// Raus bei assistent!
		if(client != null && client.isConnected()) {
			client.getPrintStream().println("BackupProfile:addFolder(String " + folder.getAbsolutePath() + ")");
		}
	}
	
	/**
	 * Fügt den angegebenen Ordner auf die Liste der zu sichernden.
	 * 
	 * @param folder der zu sichernde Ordner
	 */
	@RemoteExecution
	public void addFolder(String folder) {
		addFolder(new File(folder));
	}
	
	/**
	 * Gibt die Ordner dieses Profils zurück.
	 * 
	 * @return die Ordner dieses Profils
	 */
	@RemoteExecution
	public File[] getFolders() {
		return folders.toArray(new File[folders.size()]);
	}
	
	/**
	 * Gibt die Anzahl an Ordnern für das Backup zurück.
	 * 
	 * @return die Anzahl an registrierten Ordnern
	 */
	public int getCount() {
		return folders.size();
	}
	
	/**
	 * Gibt zurück, ob für Backups mit diesem Profil der Assistent bemüht werden muss.
	 * 
	 * @return ob die Assistenzfunktion benötigt wird
	 */
	public boolean needsAssistent() {
		if(needAssistent) {
			if(!isClientConnected()) {
				if(settleThread != null &&  !settleThread.isAlive()) {
					settling();
				}
			}
		}
		return needAssistent;
	}
	
	/**
	 * Gibt zurück, ob der {@link AssistentClient Client} verbunden ist, sollte
	 * dieses Profil einen Assistenten benötigen.
	 * 
	 * @return ob der {@link AssistentClient Client} verbunden ist
	 */
	public boolean isClientConnected() {
		if(client.isConnected()) {
			String answer, call = "antwort";
			try {
				client.getPrintStream().println("APIInterpreter:echo(String " + call + ")");
				answer = (String) client.getObjectInputStream().readObject();
			} catch(Exception e) {
				return false;
			}
			if(answer.equals(call)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Stellt ein, ob der Assistent für Backups mit diesem Profil benötigt wird.
	 * 
	 * @param needsAssistent ob der Assistent benötigt wird
	 */
	public void setNeedsAssistent(boolean needsAssistent) {
		needAssistent = needsAssistent;
		if(!needAssistent) {
			ipAddress = null;
			if(settleThread != null && settleThread.getState() != State.TERMINATED) {
				settleThread.interrupt();
			}
			if(true/*verbunden*/) {
				// Trennen
			}
			client = null;
			settleThread = null;
			locationAPOV = null;
		}
	}
	
	/**
	 * Setzt den Client auf, so wird er bereit, sich zu verbinden.
	 */
	private void settleClient() {
		if(settleThread != null && settleThread.getState() != State.TERMINATED) {
			settleThread.interrupt();
		}
		client = new AssistentClient(ipAddress, 9096);
		settling();
	}
	
	/**
	 * Startet den Thread, der den {@link AssistentClient Client} verbindet.
	 */
	private void settling() {
		settleThread = new Thread(clientSettler);
		settleThread.start();
	}
	
	/**
	 * Setzt die IP-Adresse des zu verwendenden Servers.
	 * 
	 * @param ipAddress die IP-Adresse
	 */
	public void setIPAddress(String ipAddress) {
		this.ipAddress = ipAddress;
		if(client == null || !client.isConnected()) {
			settleClient();
		}
	}
	
	/**
	 * Gibt die IP-Adresse des zu verwendenden Servvers zurück.
	 * 
	 * @return die IP-Adresse des benötigten Servers
	 */
	public String getIPAddress() {
		return ipAddress;
	}
	
	/**
	 * Entfernt einen Order, der gesichert werden sollte.
	 * 
	 * @param folder der Ordner, der nicht mehr gesichert werden soll
	 */
	public void deleteFolder(File folder) {
		folders.remove(folder);
	}
	
	/**
	 * Löscht den angegebenen Ordner von der Liste der zu sichernden Dateien.
	 * 
	 * @param folder der zu löschende Ordner
	 */
	@RemoteExecution
	public void deleteFolder(String folder) {
		deleteFolder(new File(folder));
	}
	
	/**
	 * Setzt den Ort, in welches Backups von diesem Profil geschrieben werden.
	 * 
	 * @param location der Ort, in welches die Backups geschrieben werden sollen
	 */
	public void setLocationForBackup(File location) {
		this.location = location;
	}
	
	/**
	 * Gibt den Ordner zurück, in welchen die Backups dieses Profils 
	 * geschrieben werden sollen.
	 * 
	 * @return den Ordner für die Backzps dieses Profils
	 */
	@RemoteExecution
	public File getLocationForBackups() {
		return location;
	}
	
	/**
	 * Gibt die Nummer des angegebenen Ordners in der Liste zurück. Sollte der
	 * Ordner nicht in der Liste vorhanden sein, wird -1 zurückgegeben.
	 * 
	 * @param folder der zu suchende Ordner
	 * @return die Nummer des Ordners
	 */
	public int indexOfFolder(File folder) {
		return folders.indexOf(folder);
	}
	
	/**
	 * Gibt zurück, ob der angegebene Ordner auf der Liste der zu sichernden 
	 * Ordnern ist.
	 * 
	 * @param folder der zu suchende Ordner
	 * @return ob der angegebene Ordner auf der Liste ist
	 */
	public boolean isFolderOnList(File folder) {
		return folders.contains(folder);
	}
	
	/**
	 * Gibt sämtliche Informationen dieses Profiles als byte-Array zurück.
	 * 
	 * @return ein byte-Array mit sämtlichen Informationen
	 */
	public byte[] convertToBytes() {
		ArrayList<byte[]> bytes = new ArrayList<>();
		byte[] n = name.getBytes();
		bytes.add(ByteHelper.intToBytes(n.length));
		int length = Integer.BYTES;
		bytes.add(n);
		length += n.length;
		n = new byte[] {(byte) (needAssistent ? 1 : 0)};
		length++;
		bytes.add(n);
		if(needAssistent) {
			n = ipAddress.getBytes();
			bytes.add(ByteHelper.intToBytes(n.length));
			length += Integer.BYTES;
			bytes.add(n);
			length += n.length;
		}
		n = location.getAbsolutePath().getBytes();
		bytes.add(ByteHelper.intToBytes(n.length));
		length += Integer.BYTES;
		bytes.add(n);
		length += n.length;
		bytes.add(ByteHelper.intToBytes(folders.size()));
		length += Integer.BYTES;
		for(File f : folders) {
			n = f.getAbsolutePath().getBytes();
			bytes.add(ByteHelper.intToBytes(n.length));
			length += Integer.BYTES;
			bytes.add(n);
			length += n.length;
		}
		byte[] toReturn = new byte[length];
		int byteCount = 0;
		for(byte[] bts : bytes) {
			for(byte b : bts) {
				toReturn[byteCount] = b;
				byteCount++;
			}
		}
		return toReturn;
	}

	/**
	 * Schließt den {@link AssistentClient}.
	 */
	public void closeClient() {
		if(needAssistent && client != null) {
			client.close();
		}
	}
	
	/**
	 * Setzt den Zeitpunkt des Backups vor dem zuletzt erstellten.
	 * 
	 * @param previous der genaue Zeitpunkt
	 */
	public void setPreviousBackupDate(Date previous) {
		previousBackupDate = previous;
	}
	
	/**
	 * Gibt den Zeitpunkt des Backups vor dem zuletzt erstellten zurück.
	 * 
	 * @return den Zeitpunkt
	 */
	public Date getPreviousBackupDate() {
		return previousBackupDate;
	}
	
	/**
	 * Gibt den Ordner für Backups zurück, aus der Sicht des Assistenten. Dieser Ordner
	 * existiert nur, wenn bereits ein Assistent verbunden wurde. Das kann mit der Methode
	 * {@link #needsAssistent()} überprüft werden. Existiert der Ordner nicht, wird {@code null}
	 * zurückgegeben.
	 * 
	 * @return den Backupordner aus der Sicht des Assistenten
	 * @see #needsAssistent()
	 */
	public File getLocationForBackupsAPOV() {
		return locationAPOV;
	}
	
	public String toString() {
		return "Name: " + name + "; Backupfolder: " + location + "; needsAssistent: " + needAssistent + 
				"; IP-Address: " + ipAddress;
	}
	
	/**
	 * Gibt den Client für den Assistenzserver zurück.
	 * 
	 * @return den Client
	 */
	public AssistentClient getClient() {
		return client;
	}
}