package hahn.backup.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.border.EtchedBorder;

import hahn.backup.core.BackupProfile;
import hahn.utils.gui.SelectableLabel;

/**
 * Dieses Label ist für Profile gedacht. Ein Profil kann bearbeitet werden, 
 * ein ActionListener kann für diesen Zweck dem Knopf zugewiesen werden.
 * 
 * @author Manuel Hahn
 * @since 12.04.2018
 */
public class ProfileListLabel extends SelectableLabel {
	private static final long serialVersionUID = -4481796376049740197L;
	/**
	 * Das BackupProfile, das mit diesem Label verknüpft ist.
	 */
	protected final BackupProfile p;
	/**
	 * Ein Knopf.
	 */
	protected final JButton text;

	/**
	 * Erzeugt ein auswählbares Label mit dem angegebenen Text und einem 
	 * Knopf mit der Beschriftung 'Bearbeiten...'.
	 * 
	 * @param profile das BackupProfile, das mit diesem Label verknüpft werden soll
	 */
	public ProfileListLabel(BackupProfile profile) {
		super(profile.getName());
		p = profile;
		setLayout(new BorderLayout());
		text = new JButton("Bearbeiten...");
		add(checkBox, BorderLayout.CENTER);
		add(text, BorderLayout.EAST);
		setBorder(new EtchedBorder());
	}
	
	/**
	 * Erezugt ein Label mit dem angegebenen Profil, auf den Knopf kommt die 
	 * angegebene Beschriftung.
	 * 
	 * @param profile das anzuzeigende Profil
	 * @param buttonText der Text, der auf dem Knopf stehen soll
	 */
	public ProfileListLabel(BackupProfile profile, String buttonText) {
		this(profile);
		text.setText(buttonText);
	}
	
	/**
	 * Erzeugt ein Label mit dem angegebenen Profil, auf dem Knopf steht der angegebene Text,
	 * der Listener wird direkt dem Knopf hinzugefügt.
	 * 
	 * @param profile das anzuzeigende Profil
	 * @param buttonText der Text, der auf dem Knopf stehen soll
	 * @param listener der Listener für Aktionen des Knopfes
	 */
	public ProfileListLabel(BackupProfile profile, String buttonText, ActionListener listener) {
		this(profile, buttonText);
		text.addActionListener(listener);
	}
	
	/**
	 * Erzeugt ein Label mit dem angegebenen Profil, der angegebene Listener wird
	 * direkt dem Knopf hinzugefügt.
	 * 
	 * @param profile das anzuzeigende Profil
	 * @param listener der Listener für Aktionen des Knopfes
	 */
	public ProfileListLabel(BackupProfile profile, ActionListener listener) {
		this(profile);
		text.addActionListener(listener);
	}

	/**
	 * Gibt das BackupProfile, das mit diesem Label verknüpft ist, zurück.
	 * 
	 * @return das mit diesem Label verknüpfte BackupProfile
	 */
	public BackupProfile getProfile() {
		return p;
	}
	
	/**
	 * Fügt dem Knopf einen ActionListener hinzu.
	 * 
	 * @param listener der Listener
	 */
	public void addActionListener(ActionListener listener) {
		text.addActionListener(listener);
	}
	
	/**
	 * Löscht den angegebenen ActionListener vom Knopf.
	 * 
	 * @param listener der zu entfernende Listener
	 */
	public void removeActionListener(ActionListener listener) {
		text.removeActionListener(listener);
	}
	
	/**
	 * Ändert den auf dem Knopf angezeigten Text auf den angegebenen Text.
	 * 
	 * @param text der neue, anzuzeigende Text
	 */
	public void setButtonText(String text) {
		this.text.setText(text);
	}
}