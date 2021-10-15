package hahn.backup.gui;

import hahn.utils.gui.SelectableLabel;

/**
 * In diesem Label werden Ordner angezeigt, in die eine Backup gemacht werden soll.
 * 
 * @author Manuel Hahn
 * @since 22.01.2018
 */
public class BFolderLabel extends SelectableLabel {
	private static final long serialVersionUID = -102518316084799554L;

	/**
	 * Erzeugt dieses Label.
	 * 
	 * @param directory der Ordnername, der angezeigt werden soll
	 */
	public BFolderLabel(String directory) {
		super(directory);
	}
}