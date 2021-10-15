package hahn.utils.gui;

import java.awt.Color;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * Diese Klasse bietet die Grundlage für Labels, die selektiert werden können.
 * 
 * @author Manuel Hahn
 * @since 22.01.2018
 */
public abstract class SelectableLabel extends JPanel {
	private static final long serialVersionUID = -2358507279255848384L;
	/**
	 * Dies ist die {@link JCheckBox}, mit welcher dieses Label selektiert werden kann.
	 */
	protected JCheckBox checkBox;
	
	/**
	 * Erzeugt ein selektierbares Label mit dem angegebenen Text.
	 *  
	 * @param text der Text, der angezeigt werden soll
	 */
	protected SelectableLabel(String text) {
		checkBox = new JCheckBox(text);
	}
	
	/**
	 * Gibt den angezeigten Text zurück.
	 *  
	 * @return den auf diesem Label angezeigten Text
	 */
	public String getText() {
		return checkBox.getText();
	}
	
	/**
	 * Stellt ein, ob dieses Label selektiert sein soll oder nicht.
	 * Standardmäßig ist ein Label dieser Art nicht selektiert.
	 * 
	 * @param b ob dieses Label selektiert sein soll oder nicht
	 */
	public void setSelected(boolean b) {
		checkBox.setSelected(b);
	}
	
	/**
	 * Gibt zurück, ob dieses Label selektiert ist oder nicht.
	 * 
	 * @return ob dieses Label selektiert ist oder nicht
	 */
	public boolean isSelected() {
		return checkBox.isSelected();
	}
	
	public void setToolTipText(String text) {
		checkBox.setToolTipText(text);
	}
	
	public void setForeground(Color fg) {
		if(checkBox != null) {
			checkBox.setForeground(fg);
		}
	}
}