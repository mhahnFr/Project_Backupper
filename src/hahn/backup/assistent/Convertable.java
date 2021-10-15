package hahn.backup.assistent;

/**
 * Wenn man dieses Interface implementiert, kann ein Objekt der implementierenden Klasse automatsich
 * in byte-Form gespeichert werden.
 * 
 * @author Manuel Hahn
 * @since 10.07.2017
 */
public interface Convertable {
	
	/**
	 * Diese Methode konvertiert dieses Objekt zu einem Array mit bytes.
	 * 
	 * @return die aus allen Feldern der Methode generierten bytes
	 */
	byte[] convertToBytes();
	
	/**
	 * Liest die Daten ein, die die Klasse über {@link Convertable#convertToBytes()} ausgibt.
	 * 
	 * @param data die einzulesenen bytes
	 */
	default void parseInData(byte[] data) {
		throw new NoImplementationException("Nicht implementiert!");
	}
}