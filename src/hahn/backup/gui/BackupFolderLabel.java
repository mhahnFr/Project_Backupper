package hahn.backup.gui;

import hahn.utils.gui.SelectableLabel;

import java.awt.BorderLayout;

import javax.swing.JComboBox;
import javax.swing.border.EtchedBorder;

/**
 * Diese Klasse ist ein spezielles Label, das selektiert werden kann.
 * 
 * @author Manuel Hahn
 * @since 31.10.2017
 */
public class BackupFolderLabel extends SelectableLabel {
	private static final long serialVersionUID = 7854193662453797978L;
	/**
	 * Diese {@link JComboBox} bietet die Möglichkeit, den hier angezeigten Ordner
	 * mit einem anderen Text zu verknüpfen.
	 */
	private JComboBox<String> comboBox;
	
	/**
	 * Erzeugt ein selektierbares Label mit dem angegebenen Text. Unterscheidet sich zu anderen 
	 * durch seine Spezialfunktionen.
	 * 
	 * @param directory der Ordner, der angezeigt werden soll
	 * @param select die Ordner, aus welchem auswählbar ist, in welches dieser Ordner gehört
	 */
	public BackupFolderLabel(String directory, String... select) {
		super(directory);
		comboBox = new JComboBox<>(select);
		comboBox.setSelectedIndex(0);
		setLayout(new BorderLayout());
		add(checkBox, BorderLayout.CENTER);
		add(comboBox, BorderLayout.EAST);
		setBorder(new EtchedBorder());
	}
	
	/**
	 * Sorgt dafür, das der übergeben {@link String} in der ComboBox selektiert wird.
	 * 
	 * @param toSelect der zu selektierende {@link String}
	 */
	public void setSelectedString(String toSelect) {
		comboBox.setSelectedItem(toSelect);
	}
	
	/**
	 * Selektiert den String mit der angegebenen Nummer in der ComboBox.
	 * 
	 * @param index der Index, dessen String angezeigt werden soll
	 */
	public void setSelectedString(int index) {
		comboBox.setSelectedIndex(index);
	}
	
	/**
	 * Gibt den zuletzt ausgewählten String der ComboBox zurück.
	 * 
	 * @return den selektierten String
	 */
	public String getSelectedString() {
		return (String) comboBox.getSelectedItem();
	}
}