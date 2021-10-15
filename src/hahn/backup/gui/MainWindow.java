package hahn.backup.gui;

import hahn.backup.assistent.AssistentClient;
import hahn.backup.core.BackupProfile;
import hahn.backup.core.FileManager;
import hahn.rmi.APIInterpreter;
import hahn.utils.gui.SelectableLabel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainWindow extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1301664179939733580L;
	/**
	 * Der ActionCommand fürs Backup erstellen.
	 */
	private static final String CREATE_BACKUP	 = "backing up";
	/**
	 * Der ActionCommand ein Backup abzubrechen.
	 */
	private static final String ABORT_BACKING_UP = "abort backup";
	/**
	 * Der ActionCommand die Einstellungen anzuzeigen.
	 */
	private static final String SETTINGS		 = "einstellungen";
	/**
	 * Der ActionCommand eine Sekunde herunterzuzählen.
	 */
	private static final String MINUS			 = "minus";
	/**
	 * Ein ActionCommand um den Fortschrittsbalken zu aktualisieren.
	 */
	private static final String UPDATE_PROGRESS	 = "pbar update";
	/**
	 * Der ActionCommand um die Einstellungen zu löschen.
	 */
	private static final String UNINSTALL		 = "uninstall";
	/**
	 * Die Hauptversion.
	 */
	private static final int VERSION  = 1;
	/**
	 * Die Unterversion.
	 */
	private static final int STEPPING = 0;
	/**
	 * Die Version des Updates.
	 */
	private static final int UPDATE   = 0;
	/**
	 * Ob der ausführliche Modus aktiviert ist oder nicht.
	 */
	public static boolean VERBOSE;
	/**
	 * Ob diese Instanz des Programms als Assistent für eine andere Instanz fungiert.
	 */
	public static boolean assistent;
	/**
	 * Dieses Label zeigt an, wie lange es noch dauert, bis das nächste Backup erstellt wird.
	 */
	private JLabel nextB;
	/**
	 * Der Knopf, mit dem sofort ein Backup erstellt werden kann.
	 */
	private JButton now;
	/**
	 * Das Fenster mit den Einstellungen.
	 */
	private JDialog settingsWindow;
	/**
	 * Dieses Label zeigt den Fortschritt an.
	 */
	private ProgressLabel progress;
	/**
	 * Der zentrale Fortschrittsbalken.
	 */
	private static JProgressBar pbar;
	/**
	 * Zeigt an, ob gerade ein Backup erstellt wird.
	 */
	private boolean backingUp;
	/**
	 * Ob die Timer schon einmal gestartet wurden, wichtig, um zu erkennen, 
	 * welche Methode zum Starten der Timer verwendet werden muss.
	 */
	private boolean timersStartedOnce;
	/**
	 * Der Timer, der das nächste Backup auslöst.
	 */
	private Timer nextBackup;
	/**
	 * Der Timer, der die Zeit herunterzählt.
	 */
	private Timer discountSecs;
	/**
	 * Dieser Timer aktualisiert den Fortschrittsbalken.
	 */
	private Timer updatePBar;
	/**
	 * Die Anzahl an Sekunden des Intervalls bis zum nächsten Backup.
	 */
	private int secondsToNext;
	/**
	 * Die nicht zu verändernde Anzahl an Sekunden bis zum nächsten Backup.
	 */
	private int delay;
	/**
	 * Die Anzahl an Stunden bis zum nächsten Backup.
	 */
	private int nochStunden;
	/**
	 * Die Anzahl an Minuten bis zum nächsten Backup.
	 */
	private int nochMinuten;
	/**
	 * Die Anzahl an Sekunden bis zum nächsten Backup.
	 */
	private int nochSekunden;
	/**
	 * Der genaue Zeitstempel, wann das Backup gestartet wurde, um die Zeit, 
	 * die es gedauert hat, das Backup zu erstellen, vom nächsten Intervall 
	 * abziehen zu können.
	 */
	private long startTime;
	/**
	 * Der zentrale FileManager.
	 */
	private FileManager fileManager;
	/**
	 * Der aktuelle Thread, der das Backup erstellt.
	 */
	private Thread currentBackupper;
	/**
	 * Das Runnable das das Backup erstellt.
	 */
	private final Runnable backupStarter = () -> {
		//stopFreeRAMTimer();
		fileManager.backupAll();
		//startFreeRAMTimer();
		try {
			EventQueue.invokeAndWait(() -> finish());
		} catch(InvocationTargetException | InterruptedException e) {
			System.err.println("Fehler aufgetreten: " + e.getMessage());
			e.printStackTrace();
			System.err.println("--------------------------------------");
		}
	};
	/**
	 * Das Runnable räumt auf, sollte das Backup abgebrochen worden sein.
	 */
	private final Runnable abortClearer = () -> {
		fileManager.cleanUp();
	};
	/**
	 * Der zentrale API-Interpreter, falls die Assistenzfunktion gebraucht wird.
	 */
	@SuppressWarnings("unused")
	private APIInterpreter apii;
	/**
	 * Der Thread, der die Assistenzfunktion ausführt.
	 */
	@SuppressWarnings("unused")
	private Thread apiThread;
	
	/**
	 * Erzeugt das Anwendungsfenster.
	 */
	public MainWindow(/*FileManager fileManager1, APIInterpreter apii, Thread apiThread*/) {
		setTitle("Project Back-Upper" + (assistent ? " ASSISTENT" : ""));
		if(Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
			Desktop.getDesktop().setPreferencesHandler(e -> displaySettings());
		}
		startTime = 0;
		/*this.apii = apii;
		this.apiThread = apiThread;*/
		nextB = new JLabel("Nächstes Backup in: ");
		now = new JButton("Backup jetzt erstellen");
		JButton settings = new JButton("Einstellungen");
		//if(fileManager1 == null) {
		fileManager = new FileManager();
		if(assistent) {
			fileManager.startServer();
		}
		/*} else {
			this.fileManager = fileManager1;
		}*/
		settings.addActionListener(this);
		settings.setActionCommand(SETTINGS);
		now.addActionListener(this);
		JButton execute = new JButton("Methode ausführen");
		execute.addActionListener(event -> {
			String methodName = JOptionPane.showInputDialog(this, "Name der Methode ohne Parameter eingeben:",
					"Methode ausführen", JOptionPane.QUESTION_MESSAGE);
			Method methode;
			try {
				methode = getClass().getMethod(methodName);
			} catch(NoSuchMethodException e) {
				JOptionPane.showMessageDialog(this, methodName + " existiert nicht oder nur mit Parametern!",
						"Ausführen von " + methodName, JOptionPane.ERROR_MESSAGE);
				return;
			}
			try {
				methode.invoke(this);
			} catch(Exception e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), e.getClass().getName(), JOptionPane.ERROR_MESSAGE);
			}
		});
		JButton uninstall = new JButton("Einstellungen löschen");
		uninstall.setActionCommand(UNINSTALL);
		uninstall.addActionListener(this);
		now.setActionCommand(CREATE_BACKUP);
		progress = new ProgressLabel(new ImageIcon("ajax_loader_gray_32.gif"), 
				UIManager.getIcon("OptionPane.errorIcon"), UIManager.getIcon("OptionPane.informationIcon"));
		pbar = new JProgressBar();
		setLayout(new GridLayout(6, 1));
		add(nextB);
		add(pbar);
		add(now);
		add(settings);
		//add(execute);
		add(uninstall);
		add(progress);
		if(!assistent) {
			secondsToNext = delay = this.fileManager.getDelay() * 60;
			nextBackup = new Timer(secondsToNext * 1000, this);
			nextBackup.setActionCommand(CREATE_BACKUP);
			discountSecs = new Timer(1000, this);
			discountSecs.setActionCommand(MINUS);
			updatePBar = new Timer(10, this);
			updatePBar.setActionCommand(UPDATE_PROGRESS);
		} else {
			now.setEnabled(false);
			nextB.setText("Assistent");
		}
		/*if(fileManager.isAssistent() == null) {
			int option = JOptionPane.showConfirmDialog(this, "Ist dieses Programm ein Assistent für ein weiteres?",
					"Assistentsmöglichkeit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			boolean assistent;
			switch(option) {
			case JOptionPane.YES_OPTION:
				assistent = true;
				break;
				
			case JOptionPane.NO_OPTION:
				assistent = false;
				break;
				
			default:
				assistent = false;
				System.exit(0);
				break;
			}
			fileManager.setIsAssistent(assistent);
		}*/
		final boolean hf = this.fileManager.hasFolders();
		if(hf && !assistent) {
			startTimers();
		}
		progress.setVisible(false);
		pbar.setVisible(false);
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		createSettings();
		if(!hf) {
			displaySettings();
		}/* else if(assistent) {
			apii = new APIInterpreter();
			apii.addAPIObject(fileManager);
			new Thread(apii).start();
		}*/
		/*addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent e) {
				startFreeRAMTimer();
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
				stopFreeRAMTimer();
			}
		});*/
	}
	
	/**
	 * Macht die Deinstallierungsmethode für den Nutzer erreichbar.
	 */
	public void uninstall() {
		int next = JOptionPane.showConfirmDialog(this, "Sollen die Einstellungen unwiderruflich gelöscht werden?\n"
				+ "Das Programm wird anschließend beendet.",
				"Zurücksetzen der Einstellungen", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		switch(next) {
		case JOptionPane.CANCEL_OPTION:
		case JOptionPane.CLOSED_OPTION:
			return;
		}
		fileManager.uninstall();
		System.exit(0);
	}
	
	public void actionPerformed(ActionEvent e) {
		switch(e.getActionCommand()) {
		case CREATE_BACKUP:
			startBackup();
			break;
			
		case ABORT_BACKING_UP:
			abortBackup(false);
			break;
			
		case SETTINGS:
			if(settingsWindow == null) {
				createSettings();
			}
			displaySettings();
			break;
			
		case MINUS:
			discount();
			break;
			
		case UPDATE_PROGRESS:
			//pbar.setValue((int) fileManager.getProgress());
			break;
			
		case UNINSTALL:
			uninstall();
			break;
		}
	}

	/**
	 * Zählt grafisch eine Sekunde herunter.
	 */
	private void discount() {
		final String part1 = "Nächstes Backup in: ";
		nochSekunden--;
		if(nochSekunden == -1) {
			nochMinuten--;
			nochSekunden = 59;
			if(nochMinuten == -1) {
				nochStunden--;
				nochMinuten = 59;
			}
		}
		String se = Integer.toString(nochSekunden),
				m = Integer.toString(nochMinuten),
				st = Integer.toString(nochStunden);
		se = nochSekunden < 10 ? "0" + se : se;
		m = nochMinuten < 10 ? "0" + m : m;
		st = nochStunden < 10 ? "0" + st : st;
		nextB.setText(part1 + st + ":" + m + ":" + se);
	}

	/**
	 * Erzeugt das Fenster für die Einstellungen.
	 */
	private void createSettings() {
		settingsWindow = new JDialog(this, true);
		settingsWindow.setTitle("Einstellungen");
		
		JPanel toFolder = null;
		if(assistent) {
			toFolder = new JPanel();
			toFolder.setLayout(new GridLayout(2, 1));
			JPanel name = new JPanel();
			name.setLayout(new BoxLayout(name, BoxLayout.X_AXIS));
			File ploc = fileManager.getStandardProfile().getLocationForBackups();
			JLabel toFolderName = new JLabel(ploc == null ? "Nicht zugewiesen" : ploc.getAbsolutePath());
			toFolderName.setFont(toFolderName.getFont().deriveFont(Font.BOLD));
			JLabel text = new JLabel("Ordner für Backups: ");
			name.add(text);
			name.add(toFolderName);
			JButton editToFolder = new JButton("Ändern");
			editToFolder.addActionListener(event -> {
				JFileChooser chooser = new JFileChooser();
				chooser.setMultiSelectionEnabled(false);
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setFileHidingEnabled(true);
				if(chooser.showOpenDialog(settingsWindow) == JFileChooser.APPROVE_OPTION) {
					File destination = chooser.getSelectedFile();
					fileManager.getStandardProfile().setLocationForBackup(destination);
					toFolderName.setText(destination.getPath());
				}
			});
			toFolder.add(name);
			toFolder.add(editToFolder);
			toFolder.setBorder(new EtchedBorder());
		}
		
		JPanel selfSpinner = new JPanel();
		selfSpinner.setLayout(new BorderLayout());
		final JButton minus = new JButton("-");
		final JButton plus = new JButton("+");
		final JTextField number = new JTextField(Integer.toString(delay / 60));
		number.setToolTipText("Anzahl der Minuten");
		minus.addActionListener(event -> {
			try {
				number.setText(Integer.toString(Integer.parseInt(number.getText()) - 1));
			} catch(NumberFormatException e) {
				System.err.println("Keine Zahl eingegeben!");
			}
			number.requestFocusInWindow();
		});
		plus.addActionListener(event -> {
			try {
				number.setText(Integer.toString(Integer.parseInt(number.getText()) + 1));
			} catch(NumberFormatException e) {
				System.err.println("Keine Zahl eingegeben!");
			}
			number.requestFocusInWindow();
		});
		selfSpinner.add(minus, BorderLayout.WEST);
		selfSpinner.add(plus, BorderLayout.EAST);
		selfSpinner.add(number, BorderLayout.CENTER);
		
		final JLabel besch = new JLabel("Zeitintervall in Minuten zwischen den Backups: ");
		JPanel sp = new JPanel();
		sp.setLayout(new BoxLayout(sp, BoxLayout.X_AXIS));
		sp.add(besch);
		sp.add(selfSpinner);
		sp.setBorder(new EtchedBorder());
		
		JPanel toSave = new JPanel();
		Border etched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		etched = BorderFactory.createTitledBorder(etched, "Ordner zum Backup", TitledBorder.LEFT, TitledBorder.TOP);
		toSave.setBorder(etched);
		JPanel folders = new JPanel();
		JButton add = new JButton("+");
		JButton delete = new JButton("-");
		/*JButton export = new JButton("Exportieren");
		export.addActionListener(event -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileHidingEnabled(true);
			if(chooser.showSaveDialog(settingsWindow) == JFileChooser.APPROVE_OPTION) {
				fileManager.exportFolders(chooser.getSelectedFile());
			}
		});
		JButton impor = new JButton("Impotieren");
		impor.addActionListener(event -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileHidingEnabled(true);
			chooser.setFileFilter(new FileNameExtensionFilter("Textdateien", "txt", "TXT"));
			if(chooser.showOpenDialog(settingsWindow) == JFileChooser.APPROVE_OPTION) {
				fileManager.importFolders(chooser.getSelectedFile());
			}
		});*/
		//if(labels == null) {
		ArrayList<SelectableLabel> labels = new ArrayList<>();
		//}
		folders.setLayout(new GridLayout(labels.size(), 1));
		/*for(SelectableLabel label : labels) {
			folders.add(label);
		}*/
		add.addActionListener(event -> {
			if(!assistent) {
				String namep = JOptionPane.showInputDialog(settingsWindow, "Geben Sie den Namen des Profils ein:",
						"Neues Profil", JOptionPane.QUESTION_MESSAGE);
				if(namep != null && !namep.equals("")) {
					BackupProfile profil = new BackupProfile(namep);
					SelectableLabel label = new ProfileListLabel(profil);
					labels.add(label);
					folders.add(label);
					fileManager.addProfile(profil);
					displayProfile(profil, settingsWindow);
				}
			} else {
				File[] fs = getUserFolder(settingsWindow);
				if(fs != null && fs.length != 0) {
					BackupProfile profile = fileManager.getStandardProfile();
					/*if(fileManager.getProfiles().length == 0) {
						profile = new BackupProfile(FileManager.STANDARD_PROFILE);
						profile.setLocationForBackup(new File(""));
						fileManager.addProfile(profile);
						apii.addAPIObject(profile);
					} else {
						profile = fileManager.getProfiles()[0];
					}*/
					for(File file : fs) {
						if(!profile.isFolderOnList(file)) {
							profile.addFolder(file);
							BackupFolderLabel newLabel = new BackupFolderLabel(file.getAbsolutePath(), profile.getName());
							labels.add(newLabel);
							folders.add(newLabel);
						}
					}
					settingsWindow.validate();
				}
			}
		});
		delete.addActionListener(event -> {
			Iterator<SelectableLabel> iterator = labels.iterator();
			SelectableLabel label;
			while(iterator.hasNext()) {
				label = (SelectableLabel) iterator.next();
				if(label.isSelected()) {
					folders.remove(label);
					if(assistent) {
						fileManager.getStandardProfile().deleteFolder(label.getText());
					} else {
						fileManager.deleteProfile(((ProfileListLabel) label).getProfile());
					}
					iterator.remove();
				}
			}
			settingsWindow.validate();
		});
		
		JCheckBox showWarnings = new JCheckBox("Warnungen über nicht existierende Ordner unterdrücken");
		JPanel checkbox = new JPanel();
		checkbox.setLayout(new BoxLayout(checkbox, BoxLayout.X_AXIS));
		checkbox.add(showWarnings);
		checkbox.setBorder(new EtchedBorder());

		JScrollPane scr = new JScrollPane(folders);
		scr.getVerticalScrollBar().setUnitIncrement(5);
		scr.getHorizontalScrollBar().setUnitIncrement(5);
		JPanel all = new JPanel();
		all.setLayout(new BorderLayout());
		all.add(scr, BorderLayout.CENTER);
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.add(delete);
		buttons.add(add);
		//buttons.add(impor);
		//buttons.add(export);
		all.add(buttons, BorderLayout.SOUTH);
		JPanel nextLayout = new JPanel();
		nextLayout.setLayout(new BoxLayout(nextLayout, BoxLayout.Y_AXIS));
		settingsWindow.setLayout(new BorderLayout());
		if(!assistent) {
			//nextLayout.add(toFolder);
			nextLayout.add(sp);
		} else {
			nextLayout.add(toFolder);
		}
		nextLayout.add(checkbox);
		settingsWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		settingsWindow.add(nextLayout, BorderLayout.NORTH);
		settingsWindow.add(all, BorderLayout.CENTER);
		settingsWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				int newDelay = Integer.parseInt(number.getText());
				if(newDelay <= 0) {
					newDelay = 1;
				}
				fileManager.delayChanged(newDelay);
				delay = newDelay * 60;
				if(fileManager.hasFolders() /*&& fileManager.getBackupDestination() != null*/ && !assistent) {
					startTimers();
				}
				fileManager.setSuppressNEWarnings(showWarnings.isSelected());
				fileManager.flushSettings();
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
				labels.clear();
				folders.removeAll();
				/*String bFolder = fileManager.getBackupDirectory().getName();
				for(File file : fileManager.getFoldersNeedingBackup()) {
					BackupFolderLabel next = new BackupFolderLabel(file.getAbsolutePath(), bFolder);
					String tooltip = file.getPath();
					if(!file.exists()) {
						next.setForeground(Color.RED);
						tooltip = "Ordner existiert nicht!";
						next.setSelected(true);
					}
					next.setToolTipText(tooltip);
					labels.add(next);
					folders.add(next);
				}*/
				if(assistent) {
					if(fileManager.getProfiles().length != 0) {
						BackupProfile profile = fileManager.getStandardProfile();
						for(File file : profile.getFolders()) {
							BackupFolderLabel next = new BackupFolderLabel(file.getAbsolutePath(), profile.getName());
							labels.add(next);
							folders.add(next);
						}
					}
				} else {
					for(BackupProfile profile : fileManager.getProfiles()) {
						ProfileListLabel next = new ProfileListLabel(profile);
						next.addActionListener(event -> displayProfile(profile, settingsWindow));
						labels.add(next);
						folders.add(next);
					}
				}
				settingsWindow.validate();
			}
		});
		settingsWindow.pack();
	}
	
	/**
	 * Zeigt das angegebene Profil an. Wenn das Fenster geschlossen wird, 
	 * wird das Profil gesichert.
	 * 
	 * @param profile das anzuzeigende Profil
	 * @param frame das assoziierte Fenster
	 */
	private void displayProfile(BackupProfile profile, JDialog frame) {
		JDialog profileWindow = new JDialog(frame, "Profil \"" + profile.getName() + "\"", true);
		profileWindow.setLayout(new BorderLayout());
		
		var around = new JPanel();
		around.setLayout(new BoxLayout(around, BoxLayout.Y_AXIS));
		var toFolder = new JPanel();
		toFolder.setLayout(new GridLayout(2, 1));
		JPanel name = new JPanel();
		name.setLayout(new BoxLayout(name, BoxLayout.X_AXIS));
		var ploc = profile.getLocationForBackups();
		JLabel toFolderName = new JLabel(ploc == null ? "Nicht zugewiesen" : ploc.getAbsolutePath());
		toFolderName.setFont(toFolderName.getFont().deriveFont(Font.BOLD));
		JLabel text = new JLabel("Ordner für Backups: ");
		name.add(text);
		name.add(toFolderName);
		JButton editToFolder = new JButton("Ändern");
		editToFolder.addActionListener(event -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setFileHidingEnabled(true);
			if(chooser.showOpenDialog(profileWindow) == JFileChooser.APPROVE_OPTION) {
				File destination = chooser.getSelectedFile();
				profile.setLocationForBackup(destination);
				toFolderName.setText(destination.getPath());
			}
		});
		toFolder.add(name);
		toFolder.add(editToFolder);
		toFolder.setBorder(new EtchedBorder());
		around.add(toFolder);
		
		var sshSettings = new JPanel();
		var hint = "Your-Computer-Name.local";
		var account = new JTextField(hint);
		account.setForeground(Color.GRAY);
		account.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				if(account.getText().equals(hint)) {
					account.setText("");
					account.setForeground(Color.BLACK);
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				var text = account.getText();
				if(text.equals(hint) || text.equals("")) {
					account.setText(hint);
					account.setForeground(Color.GRAY);
				}
			}			
		});
		var addressLabel = new JLabel("Computername oder IP-Adresse: ");
		sshSettings.setLayout(new GridLayout(4, 1));
		var war = new JLabel("Die zu sichernden Ordner müssen auf dem ");
		var war2 = new JLabel("entfernten Computer registriert werden!");
		war.setFont(war.getFont().deriveFont(Font.ITALIC));
		war2.setFont(war.getFont());
		sshSettings.add(war);
		sshSettings.add(war2);
		sshSettings.add(addressLabel/*, BorderLayout.WEST*/);
		sshSettings.add(account/*, BorderLayout.CENTER*/);
		JButton add = new JButton("+"), delete = new JButton("-");
		JButton export = new JButton("Exportieren"), impor = new JButton("Impotieren");
		JPanel all = new JPanel(), buttons = new JPanel(), mainFolders = new JPanel();
		var scr = new JScrollPane(mainFolders);
		
		var cbox = new JCheckBox("Benötigt Server");
		cbox.addItemListener(event -> {
			if(cbox.isSelected()) {
				around.add(sshSettings);
				all.remove(buttons);
				all.remove(scr);
				
			} else {
				around.remove(sshSettings);
				all.add(buttons, BorderLayout.SOUTH);
				all.add(scr, BorderLayout.CENTER);
			}
			profileWindow.validate();
		});
		around.add(cbox);
		profileWindow.add(around, BorderLayout.NORTH);
		
		if(profile.needsAssistent()) {
			cbox.setSelected(true);
			around.add(sshSettings);
			account.setText(profile.getIPAddress());
			account.setForeground(Color.BLACK);
		}
		
		ArrayList<BackupFolderLabel> labels = new ArrayList<>();
		Border etched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		etched = BorderFactory.createTitledBorder(etched, "Ordner für das Backup", TitledBorder.LEFT, TitledBorder.TOP);
		mainFolders.setLayout(new GridLayout(profile.getCount(), 1));
		mainFolders.setBorder(etched);
		for(File bf : profile.getFolders()) {
			BackupFolderLabel l = new BackupFolderLabel(bf.getAbsolutePath(), profile.getName());
			labels.add(l);
			mainFolders.add(l);
		}
		export.addActionListener(event -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileHidingEnabled(true);
			chooser.setSelectedFile(new File(chooser.getCurrentDirectory().getAbsolutePath() + 
					File.separatorChar + profile.getName()));
			if(chooser.showSaveDialog(profileWindow) == JFileChooser.APPROVE_OPTION) {
				fileManager.exportFolders(chooser.getSelectedFile(), profile);
			}
		});
		impor.addActionListener(event -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileHidingEnabled(true);
			chooser.setFileFilter(new FileNameExtensionFilter("Textdateien", "txt", "TXT"));
			if(chooser.showOpenDialog(profileWindow) == JFileChooser.APPROVE_OPTION) {
				fileManager.importFolders(chooser.getSelectedFile(), profile);
			}
		});
		add.addActionListener(event -> {
			/*JFileChooser chooser = new JFileChooser();
			chooser.setFileHidingEnabled(true);
			chooser.setMultiSelectionEnabled(true);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);*/
			//if(chooser.showOpenDialog(profileWindow) == JFileChooser.APPROVE_OPTION) {
			File[] folders = getUserFolder(profileWindow);
			if(folders != null && folders.length != 0) {
				for(File file : folders) {
					if(!profile.isFolderOnList(file)) {
						profile.addFolder(file);
						BackupFolderLabel newLabel = new BackupFolderLabel(file.getAbsolutePath(), profile.getName());
						labels.add(newLabel);
						mainFolders.add(newLabel);
					}
				}
				profileWindow.validate();
			}
		});
		delete.addActionListener(event -> {
			Iterator<BackupFolderLabel> iterator = labels.iterator();
			BackupFolderLabel label;
			while(iterator.hasNext()) {
				label = iterator.next();
				if(label.isSelected()) {
					mainFolders.remove(label);
					profile.deleteFolder(new File(label.getText()));
					iterator.remove();
				}
			}
			profileWindow.validate();
		});
		scr.getVerticalScrollBar().setUnitIncrement(5);
		scr.getHorizontalScrollBar().setUnitIncrement(5);
		all.setLayout(new BorderLayout());
		if(!cbox.isSelected()) {
			all.add(scr, BorderLayout.CENTER);
		}
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttons.add(delete);
		buttons.add(add);
		buttons.add(impor);
		buttons.add(export);
		if(!cbox.isSelected()) {
			all.add(buttons, BorderLayout.SOUTH);
		}
		profileWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				labels.clear();
				mainFolders.removeAll();
				for(File folder : profile.getFolders()) {
					BackupFolderLabel label = new BackupFolderLabel(folder.getAbsolutePath(), profile.getName());
					labels.add(label);
					mainFolders.add(label);
				}
				profileWindow.validate();
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				if(cbox.isSelected()) {
					var text = account.getText();
					if(!text.equals(hint) && !text.equals("")) {
						profile.setNeedsAssistent(true);
						profile.setIPAddress(text);
							/*JOptionPane.showMessageDialog(profileWindow, "Der Server \"" + text + "\" ist nicht erreichbar.\n"
									+ "Ist die eingegebene Addresse korrekt?\n"
									+ "Läuft der Server auf dem entfernten angegebenen Computer?",
									"Server nicht erreichbar", JOptionPane.WARNING_MESSAGE);
						}*/
					} else {
						profile.setNeedsAssistent(false);
					}
				} else {
					profile.setNeedsAssistent(false);
				}
			}
		});
		profileWindow.add(all, BorderLayout.CENTER);
		profileWindow.pack();
		profileWindow.setLocationRelativeTo(settingsWindow);
		profileWindow.setVisible(true);
	}
	
	/**
	 * Gibt mehrere vom Nutzer ausgewählte Ordner zurück.
	 * 
	 * @param parent das Fenster für die Modalität
	 * @return die Ordner, die der Nutzer ausgewählt hat
	 */
	private File[] getUserFolder(JDialog parent) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileHidingEnabled(true);
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if(chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFiles();
		}
		return null;
	}

	/**
	 * Zeigt das Fenster für die Einstellungen an.
	 */
	private void displaySettings() {
		if(!assistent) {
			stopGUI();
		}
		if(settingsWindow == null) {
			createSettings();
		}
		settingsWindow.setLocationRelativeTo(this);
		settingsWindow.setVisible(true);
	}

	/**
	 * Startet ein Backup. Ändert das GUI dahingehend.
	 */
	private void startBackup() {
		startTime = System.currentTimeMillis();
		now.setText("Backup abbrechen");
		now.setActionCommand(ABORT_BACKING_UP);
		progress.setVisible(true);
		progress.showWorkInProgress("Backup wird erstellt...");
		pbar.setValue(0);
		pbar.setVisible(true);
		backingUp = true;
		discountSecs.stop();
		nextBackup.stop();
		nextB.setText("Backup wird erstellt...");
		currentBackupper = new Thread(backupStarter);
		currentBackupper.setName("MainBackupThread");
		currentBackupper.start();
	}

	/**
	 * Bricht das gerade laufende Backup ab. Fragt den Nutzer, ob das Backup wirklich
	 * abgebrochen werden soll.
	 * 
	 * @param asked ob bereits gefragt wurde
	 */
	private void abortBackup(boolean asked) {
		if(!asked) {
			if(!askForAbort(true)) {
				return;
			}
		}
		if(currentBackupper != null) {
			currentBackupper.interrupt();
		}
		fileManager.willAbort();
		new Thread(abortClearer).start();
		finish();
	}

	/**
	 * Startet die Timer und setzt deren Zeiten.
	 */
	private void startTimers() {
		int toDelete = 0;
		if(startTime != 0) {
			toDelete = (int) (System.currentTimeMillis() - startTime);
			toDelete = toDelete < 0 ? 0 : toDelete;
			startTime = 0;
		}
		secondsToNext = delay - (toDelete / 1000);
		int oSecs = secondsToNext;
		nochSekunden = oSecs % 60;
		oSecs -= nochSekunden;
		int mins = oSecs / 60;
		nochMinuten = mins % 60;
		mins -= nochMinuten;
		nochStunden = mins / 60;
		
		int nd = delay * 1000;
		if(nd > 1) {
			nd -= toDelete;
		}
		nextBackup.setInitialDelay(nd);
		if(timersStartedOnce) {
			discountSecs.restart();
			nextBackup.restart();
		} else {
			discountSecs.start();
			nextBackup.start();
			timersStartedOnce = true;
		}
	}

	/**
	 * Ändert die Beschriftung der Knöpfe, setzt Zeiten zurück.
	 */
	private void stopGUI() {
		discountSecs.stop();
		nextBackup.stop();
		now.setText("Backup jetzt erstellen");
		now.setActionCommand(CREATE_BACKUP);
		progress.stop();
		pbar.setVisible(false);
		progress.setVisible(false);
		backingUp = false;
		secondsToNext = delay;
		nextBackup.setInitialDelay(secondsToNext * 1000);
	}

	/**
	 * Räumt das GUI nach einem Backup auf.
	 */
	private void finish() {
		stopGUI();
		startTimers();
	}

	public void dispose() {
		if(askForAbort(false)) {
			if(backingUp) {
				abortBackup(true);
			}
		} else {
			return;
		}
		super.dispose();
		System.exit(0);
	}

	/**
	 * Fragt den Nutzer, ob das gerade laufende Backup abgebrochen werden soll.
	 * 
	 * @return ob der Nutzer bestätigt hat, dass das Backup abgebrochen werden soll
	 */
	private boolean askForAbort(boolean abort) {
		if(backingUp) {
			String mainText, question, title;
			switch(fileManager.getStatus()) {
			case CLEANING_UP:
				mainText = "Aufräumarbeiten.";
				break;
				
			case DELETING:
				mainText = "Alte Backups werden gerade gelöscht.";
				break;
				
			case WRITING:
				mainText = "Backup wird gerade erstellt.";
				break;
				
			default:
				mainText = "Keine laufenden Aufgaben.";
				break;
			}
			if(abort) {
				question = "Wirklich abbrechen?";
				title = "Backup abbrechen";
			} else {
				question = "Wirklich beenden?";
				title = "Programm beenden";
			}
			int choice = JOptionPane.showConfirmDialog(this, mainText + " " + question,
					title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			switch(choice) {
			case JOptionPane.CANCEL_OPTION:
			case JOptionPane.CLOSED_OPTION:
				return false;
			}
			return true;
		}
		return true;
	}

	/*public static String getStandardPassword(String message) {
		var panel = new JPanel();
		panel.setLayout(new GridLayout(1, 2));
		panel.add(new JLabel(message));
		var pwf = new JPasswordField();
		panel.add(pwf);
		int option = JOptionPane.showOptionDialog(null, panel, "Passwort eingeben", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, null, JOptionPane.OK_OPTION);
		return option == JOptionPane.OK_OPTION ? new String(pwf.getPassword()) : null;
	}*/
	
	/**
	 * Schiebt die Fortschrittsanzeige um die angegebenen Prozent weiter.
	 * 
	 * @param value der Wert, der an den Fortschrittsbalken weitergegeben wird
	 */
	public static void addPBarValue(int value) {
		EventQueue.invokeLater(() -> pbar.setValue(pbar.getValue() + value));
	}

	/**
	 * Die zentrale Startmethode. Setzt das Look&Feel auf das native und startet das Programm.
	 * 
	 * @param args wird derzeit ignoriert
	 */
	public static void main(String[] args) {
		for(String argument: args) {
			switch(argument) {
			case "--version":
				System.out.println("Project-Backupper Version: " + VERSION + "." + STEPPING + "." + UPDATE);
				break;
				
			case "--verbose":
				VERBOSE = true;
				break;
				
			case "--assistent":
			case "-a":
				assistent = true;
				break;
				
			case "-test2":
				try (AssistentClient client = new AssistentClient()) {
					client.establishConnection("localhost", 9090);
					String command = JOptionPane.showInputDialog(null, "Methodenaufruf:", "API-Test", JOptionPane.PLAIN_MESSAGE);
					final PrintStream ps = client.getPrintStream();
					ps.println(command);
					ps.flush();
					final BufferedReader reader = new BufferedReader(new InputStreamReader(client.getSocketInputStream()));
					JOptionPane.showMessageDialog(null, reader.readLine(), "Antwort vom Server", JOptionPane.INFORMATION_MESSAGE);
					ps.println("hahn.rmi.APIInterpreter:exit()");
					ps.flush();
				} catch (IOException e) {
					System.err.println("Fehler aufgetreten: " + e.getMessage());
					e.printStackTrace();
					System.err.println("-------------------------------------");
				}
				System.exit(0);
				break;
				
			case "-test":
				try (AssistentClient client = new AssistentClient()) {
					client.establishConnection("localhost", 9096);
					String command = JOptionPane.showInputDialog(null, "Methodenaufruf:", "API-Test", JOptionPane.PLAIN_MESSAGE);
					final OutputStream os = client.getSocket().getOutputStream();
					command += '\n';
					final byte[] cmd = command.getBytes();
					os.write(cmd, 0, cmd.length);
					os.flush();
					StringBuilder builder = new StringBuilder();
					final BufferedReader reader = new BufferedReader(new InputStreamReader(client.getSocket().getInputStream()));
					do {
						builder.append((char) reader.read());
					} while(reader.ready());
					JOptionPane.showMessageDialog(null, builder.toString(), "Antwort vom Server", JOptionPane.INFORMATION_MESSAGE);
					final byte[] exitCmd = "hahn.backup.assistent.APIInterpreter:exit()\n".getBytes();
					os.write(exitCmd, 0, exitCmd.length);
					os.flush();
				} catch(Exception e) {
					e.printStackTrace();
				}
				System.exit(0);
				break;
				
			case "-h":
			case "--help":
			default:
				System.out.println("Project-Backupper Hilfe:");
				System.out.println("--version	Zeigt die Version des Programms an\n");
				System.out.println("--verbose	Aktiviert den ausführlichen Modus\n");
				System.out.println("--assistent");
				System.out.println("-a		Aktiviert den Assistenzmodus, nur zur API-Benutzung!\n");
				System.out.println("--window		Öffnet das Hauptfenster\n");
				System.out.println("--help");
				System.out.println("-h		Zeigt diese Hilfe an");
				break;
			}
		}
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println("Konnte Standard-Look&Feel nicht benutzen: " + e.getMessage());
			e.printStackTrace();
			System.err.println("-------------------------------------");
		}
		/*FileManager fileManager;
		APIInterpreter apii;
		Thread apiThread;*/
		//if(!VERBOSE) {
		VERBOSE = true;
		// Behalten!!! Nur zum Debuggen in einer IDE deaktivieren!
		/*if(!assistent) {
			File logFile = new File("Project-Backupper.log");
			try {
				PrintStream ps = new PrintStream(logFile);
				System.setErr(ps);
				System.setOut(ps);
			} catch (IOException e) {
				System.err.println("Fehler aufgetreten: " + e.getMessage());
				e.printStackTrace();
				System.err.println("-------------------------------------");
			}
		}*/
		/*if(assistent) {
			fileManager = new FileManager();
			//apii = new APIInterpreter(true);
			AssistentServer server = new AssistentServer(9696);
			apii.addAPIObject(fileManager);
			if(fileManager.hasFolders()) {
				apii.addAPIObject(fileManager.getStandardProfile());
			}
			apiThread = new Thread(apii);
			apiThread.setName("ServerThread");
			apiThread.start();
		} else {
			apii = null;
			fileManager = null;
			apiThread = null;
		}*/
		EventQueue.invokeLater(() -> new MainWindow(/*fileManager, apii, apiThread*/).setVisible(true));
	}
}