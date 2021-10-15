package hahn.backup.assistent;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

import hahn.backup.gui.MainWindow;

/**
 * Ein einfacher Client für den Assistenzserver.
 * 
 * @author Manuel Hahn
 * @since 23.05.2018
 */
public class AssistentClient implements AutoCloseable, Closeable {
	/**
	 * Die SocketFactory.
	 */
	private SocketFactory socketFactory;
	/**
	 * Der Socket des Clients.
	 */
	private Socket socket;
	/**
	 * Der zuletzt verwendete Host.
	 */
	private String host;
	/**
	 * Der zuletzt verwendete Port.
	 */
	private int port;
	/**
	 * Der zentrale eingehende ObjectStream.
	 */
	private ObjectInputStream serialIn;
	/**
	 * Der Haupteingangsdatenstrom.
	 */
	private InputStream mainIn;
	/**
	 * Der zentrale ausgehende PrintStream.
	 */
	private PrintStream commandOut;

	/**
	 * Erzeugt einen Client, der sich mit der Angabe des Hosts und des Ports verbinden kann.
	 */
	public AssistentClient() {
		socketFactory = SocketFactory.getDefault();
	}
	
	/**
	 * Erzeugt einen Client, der sich sofort über {@link #establishConnection()} verbinden kann.
	 * 
	 * @param host der zu verwendende Host
	 * @param port der zu verwendende Port
	 */
	public AssistentClient(String host, int port) {
		this();
		this.host = host;
		this.port = port;
	}
	
	/**
	 * Verbindet sich mit dem angegebenen Host auf dem angegebenen Port. Diese
	 * Methode blockiert solange, bis eine Verbindung zustande kommt. Der Host 
	 * und der Port werden gespeichert, sollte {@link #establishConnection()}
	 * aufgerufen werden, werden die hier übergebenen Argumente verwendet.
	 * 
	 * @param host der Host
	 * @param port der Port, auf dem der Server angesprochen werden soll
	 * @throws UnknownHostException sollte der Host nicht bekannt sein
	 * @throws IOException sollte beim Handshaking ein Fehler passieren
	 */
	public void establishConnection(String host, int port) throws UnknownHostException, IOException {
		if(host != null && !host.equals("") && this.host != host) {
			this.host = host;
		}
		if(port != 0 && this.port != port) {
			this.port = port;
		}
		try {
			establishConnection();
		} catch(IllegalAccessException e) {
			System.err.println("Eigentlich nicht möglich! Fehlerhafte Argumente!");
			e.printStackTrace();
			System.err.println("-------------------------------------");
		}
	}
	
	/**
	 * Verbindet sich mit dem zuletzt übergebenen Host auf dem zuletzt übergebenen 
	 * Port. Diese Methode bricht ab, sollte bisher kein Host und kein Port übergeben
	 * worden sein. Sie blockiert solange, bis eine Verbindung aufgebaut wird.
	 * 
	 * @throws UnknownHostException sollte der Host unbekannt sein
	 * @throws IOException sollte die Verbindung nicht aufgebaut werden können
	 * @throws IllegalAccessException sollte kein Host und kein Port bisher übergeben worden sein
	 */
	public void establishConnection() throws UnknownHostException, IOException, IllegalAccessException {
		var exc = new IllegalAccessException("Fehlende Argumente! Zuerst AssistentClient#establ"
				+ "ishConnection(String, int) aufrufen!");
		if(host == null || host.equals("")) {
			throw exc;
		}
		if(port == 0) {
			throw exc;
		}
		if(isConnected()) {
			socket.close();
		}
		socket = socketFactory.createSocket(host, port);
		commandOut = new PrintStream(socket.getOutputStream());
		mainIn = socket.getInputStream();
		serialIn = new ObjectInputStream(mainIn);
	}
	
	/**
	 * Gibt zurück, ob dieser Client mit einem Server verbunden ist oder nicht.
	 * 
	 * @return ob eine Verbindung steht
	 */
	public boolean isConnected() {
		return socket != null && 
				!socket.isClosed() && 
				socket.isConnected() && 
				socket.isBound();
	}

	/**
	 * Gibt den ausgehenden PrintStream zurück.
	 * 
	 * @return den ausgehenden PrintStream
	 */
	public PrintStream getPrintStream() {
		return commandOut;
	}
	
	/**
	 * Gibt den eingehenden ObjectInputStream zurück. Es sollte dieser verwendet werden 
	 * und kein neuer initialisiert werden, um Headerprobleme zu vermeiden.
	 * 
	 * @return den initialisierten ObjectOutputStream
	 */
	public ObjectInputStream getObjectInputStream() {
		return serialIn;
	}
	
	/**
	 * Gibt den Socket, der die aktuelle Verbindung repräsentiert, zurück. Sollte 
	 * bisher noch keine Verbindung zustande gekommen sein, wird {@code null} 
	 * zurückgegeben.
	 * 
	 * @return den Socket der aktuellen Verbindung
	 */
	public final Socket getSocket() {
		return socket;
	}
	
	/**
	 * Gibt den rohen {@link InputStream} zurück.
	 * 
	 * @return den ersten {@link InputStream}
	 */
	public final InputStream getSocketInputStream() {
		return mainIn;
	}
	
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Fehler aufgetreten:" + e.getMessage());
			if(MainWindow.VERBOSE) {
				e.printStackTrace();
				System.err.println("------------------------------------");
			}
		}
	}
	
	/**
	 * Gibt zurück, ob alle Streams dieses Clients betriebsbereit sind.
	 * 
	 * @return ob alle Streams vorhanden sind
	 */
	public boolean hasStreams() {
		return commandOut != null && serialIn != null && mainIn != null;
	}
}