package hahn.backup.assistent;

/**
 * Diese Exception zeigt an, dass der Ort, an dem sie geworfen wurde, 
 * nicht korrekt implementiert ist.
 * 
 * @author Manuel Hahn
 * @since 22.05.2018
 */
public class NoImplementationException extends RuntimeException {

	private static final long serialVersionUID = 582980933331562346L;

	/**
	 * Wirft eine normale Exception.
	 */
	public NoImplementationException() {
		super();
	}
	
	/**
	 * Wirft eine normale Exception mit der angegebenen Nachricht.
	 * 
	 * @param message die Nachricht
	 */
	public NoImplementationException(String message) {
		super(message);
	}
	
	/**
	 * Wirft eine normale Exception mit dem angegebenen Throwable.
	 * 
	 * @param throwable das Throwable
	 */
	public NoImplementationException(Throwable throwable) {
		super(throwable);
	}
	
	/**
	 * Wirft eine normale Exception mit der angegebenen Nachricht und dem 
	 * angegebenen Throwable.
	 * 
	 * @param message die Nachricht
	 * @param throwable das Throwable
	 */
	public NoImplementationException(String message, Throwable throwable) {
		super(message, throwable);
	}
}