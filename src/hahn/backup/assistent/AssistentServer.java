package hahn.backup.assistent;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

/**
 * Ein Server für die Assistenzfunktion.
 * 
 * @author Manuel Hahn
 * @since 16.05.2018
 */
public class AssistentServer {
	/**
	 * Der Socket der Verbindung.
	 */
	private Socket socket;
	/**
	 * Der Socket dieses Servers.
	 */
	private ServerSocket serverSocket;
	/**
	 * Der zentrale ausgehende Stream.
	 */
	private ObjectOutputStream serialOut;
	
	/**
	 * Öffnet einen Server auf dem angegebenen Port.
	 * 
	 * @param port die Nummer des zu verwendenden Ports
	 * @throws IOException sollte ein Fehler beim Aufsetzen des Server passieren
	 */
	public AssistentServer(int port) throws IOException {
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		serverSocket = factory.createServerSocket(port);
	}
	
	/**
	 * Gibt zurück, ob der Server mit einem Client verbunden ist.
	 * 
	 * @return ob der Server mit einem Client verbunden ist
	 */
	public boolean hasClient() {
		return socket != null && 
				!socket.isClosed() && 
				socket.isConnected() && 
				socket.isBound();
	}
	
	/**
	 * Gibt den Socket, der die aktuelle Verbindung repräsentiert, zurück. Sollte bisher noch
	 * keine Verbindung zustande gekommen sein, wird {@code null} zurückgegeben.
	 * 
	 * @return den Socket der aktuellen Verbindung
	 */
	public final Socket getSocket() {
		return socket;
	}
	
	/**
	 * Stellt eine Verbindung mit dem erstbesten Client her. Diese Methode blockiert
	 * bis eine Verbindung erfolgreich aufgebaut wurde.
	 * 
	 * @throws IOException sollte irgendein Handshakingfehler auftreten
	 */
	public void establishConnection() throws IOException {
		if(hasClient()) {
			socket.close();
		}
		socket = serverSocket.accept();
		serialOut = new ObjectOutputStream(socket.getOutputStream());
		serialOut.flush();
	}
	
	/**
	 * Gibt einen bereits initialisierten ObjectOutputStream zurück.
	 * 
	 * @return den ObjectOutputStream
	 */
	public ObjectOutputStream getObjectOutputStream() {
		return serialOut;
	}
}