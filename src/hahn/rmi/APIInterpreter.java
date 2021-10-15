package hahn.rmi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.HashMap;

import hahn.backup.assistent.AssistentServer;
import hahn.backup.assistent.Convertable;
import hahn.backup.gui.MainWindow;

/**
 * Interpretiert die Befehle, die über die Konsole kommen.
 * 
 * @author Manuel Hahn
 * @since 10.04.2018
 */
public class APIInterpreter implements Runnable, APIObject {
	/**
	 * Die Signalvariable, die mitteilt, ob dieser Thread beendet werden soll.
	 */
	private boolean terminate;
	/**
	 * Zeigt an, ob der Thread wirklich beendet wurde.
	 */
	private boolean hasTerminated;
	/**
	 * Ob die Ein- und Ausgaben in byte-Form via {@link Convertable} aktiviert sind.
	 */
	private boolean byteMode;
	/**
	 * Zeigt an, ob der Server vor Beginn des Interpretierens gestartet werden soll.
	 */
	/*@Deprecated
	private boolean serverStart;*/
	/**
	 * Eine Liste mit Klassen und deren Objekte, die statt neuer Instanzen der
	 * aufgeführten Klassen verwendet werden sollen.
	 */
	private HashMap<Class<?>, APIObject> noInstanceObjs;
	/**
	 * Eine Liste mit dem benutzerdefinierten Namen für die entsprechende Klasse.
	 */
	private HashMap<String, Class<?>> imported;
	/**
	 * Der Assistenzserver, falls er benutzt werden soll.
	 */
	/*@Deprecated
	private AssistentServer server;*/
	/**
	 * Der OutputStream, durch welchen sämtliche Ergebnisse, Warnungen usw. ausgegeben werden sollen.
	 */
	private PrintStream outputStream;
	/**
	 * Der unveränderliche Ausgangsstrom für die Serialation.
	 */
	private final ObjectOutputStream objectOut;
	/**
	 * Der InputStream, über welchen die Befehle für diesen Interpreter kommen.
	 */
	private InputStream inputStream;
	
	/**
	 * Initialisiert den Interpreter. Der {@code boolean} zeigt an, ob der Server
	 * gestartet werden soll wenn der APIInterpreter aktiviert wird.
	 * 
	 * @param server ob der Server gestartet werden soll
	 */
	/*public APIInterpreter(boolean server) {
		outputStream = System.out;
		inputStream = System.in;
		terminate = false;
		noInstanceObjs = new HashMap<>();
		imported = new HashMap<>();
		addAPIObject(this);
		serverStart = server;
	}*/

	/**
	 * Erzeugt den APIInterpreter mit den angegebenen Streams. Keiner dieser Streams darf
	 * {@code null} sein.
	 * 
	 * @param in der eingehende Datenstrom
	 * @param out der ausgehende Schreibstrom
	 * @param objectOut der ausgehende Datenstrom für Objekte
	 * @throws NullPointerException sollte einer der angegebenen Ströme {@code null} sein
	 */
	public APIInterpreter(InputStream in, PrintStream out, final ObjectOutputStream objectOut) {
		if(in == null) {
			throw new NullPointerException("The inputstream must not be null!");
		}
		if(out == null) {
			throw new NullPointerException("The outputstream must not be null!");
		}
		if(objectOut == null) {
			throw new NullPointerException("The objectoutputstream must not be null!");
		}
		outputStream = out;
		inputStream = in;
		this.objectOut = objectOut;
		terminate = false;
		noInstanceObjs = new HashMap<>();
		imported = new HashMap<>();
		addAPIObject(this);
	}
	
	/**
	 * Die wichtigste Methode. Interpretiert das angegebene Kommando. Gibt zurück, ob
	 * das Kommando ausgeführt wurde oder nicht. Dieser Wert zeigt nicht an, ob das Kommando 
	 * fehlerfrei ausgeführt wurde!
	 * 
	 * @param command das Kommando
	 * @return ob das Kommando ausgeführt wurde
	 */
	public boolean interprateCommand(String command) {
		try {
			if(command.equals("")) {
				return false;
			}
			if(command.startsWith("import")) {
				int is = command.indexOf(' ');
				int is2 = command.indexOf(' ', is + 1);
				String as;
				if(is2 == -1) {
					is2 = command.length();
					as = command.substring(command.lastIndexOf('.') + 1);
				} else {
					as = command.substring(is2 + 1);
				}
				String cl = command.substring(is + 1, is2);
				imported.put(as, Class.forName(cl));
				return true;
			}
			int indexdp = command.indexOf(':');
			if(indexdp == -1) {
				outputStream.println("Methoden werden mit einen Doppelpunkt aufgerufen:");
				outputStream.println("Klasse:Methode(Klasse argument)");
				outputStream.println("Zurzeit ist nur 1 Argument übertragbar!");
				outputStream.flush();
				return false;
			}
			String c = command.substring(0, indexdp);
			Class<?> clas = getClassByString(c);
			int indexob = command.indexOf('(');
			int indexlob = command.length() - 1;
			String m = command.substring(indexdp + 1, indexob);
			String args = command.substring(indexob + 1, indexlob);
			Object argClassObject = null;
			if(!args.equals("")) {
				int indexs = args.indexOf(' ');
				String argsC = args.substring(0, indexs);
				String tsArg = args.substring(indexs + 1);
				Class<?> ca = getClassByString(argsC);
				if(ca.equals(String.class)) {
					argClassObject = new String(tsArg);
				} else if(ca.equals(Boolean.class)) {
					argClassObject = Boolean.parseBoolean(tsArg);
				} else {
					if(byteMode) {
						try {
							argClassObject = ca.getDeclaredConstructor(byte[].class).newInstance(tsArg.getBytes());
						} catch(NoSuchMethodException e) {
							argClassObject = ca.getDeclaredConstructor().newInstance();
							((Convertable) argClassObject).parseInData(tsArg.getBytes());
						}
					} else {
						try {
							argClassObject = ca.getDeclaredConstructor(String.class).newInstance(tsArg);
						} catch(NoSuchMethodException e) {
							argClassObject = ca.getDeclaredConstructor().newInstance();
							((APIObject) argClassObject).parseInData(tsArg);
						}
					}
				}
			}
			APIObject classObject = noInstanceObjs.get(clas);
			if(classObject == null) {
				classObject = (APIObject) clas.getDeclaredConstructor().newInstance();
			}
			Object[] acos = null;
			if(argClassObject != null) {
				acos = new Object[] {argClassObject};
			}
			// FIXME Server hier raus!!!
			classObject.findAndInvokeMethod(/*server*/objectOut, outputStream, m, byteMode, acos);
		} catch (IOException e) {
			System.err.println("Fehler aufgetreten: " + e.getMessage());
			e.printStackTrace();
			System.err.println("-------------------------------------");
		} catch (ClassNotFoundException e) {
			System.err.println("Fehler: Angegebene Klasse existiert nicht!");
		} catch (SecurityException e) {
			System.err.println("Fehler: Methode ist nicht öffentlich!");
		} catch (IllegalAccessException e) {
			System.err.println("Fehler: Es konnte nicht auf die Methode zugegriffen werden.");
		} catch (IllegalArgumentException e) {
			System.err.println("Fehlerhafte Argumente.");
		} catch (InstantiationException e) {
			System.err.println("Konnte kein Objekt der angegebene Klasse erzeugen!");
		} catch (NoSuchMethodException e) {
			System.err.println("Fehler: Methode existiert nicht in der angegebenen Klasse!");
		} catch (InvocationTargetException e) {
			System.err.println("Methode konnte nicht ausgeführt werden!");
		} catch (ClassCastException e) {
			System.err.println("Klasse implementiert das benötigte Interface nicht!");
		} catch (Exception e) {
			System.err.println("Unbekannter Fehler aufgetreten!");
			System.err.println("Fehler aufgetreten: " + e.getMessage());
			e.printStackTrace();
			System.err.println("-------------------------------------");
		}
		return true;
	}
	
	@Override
	public void run() {
		/*if(serverStart) {
			startServer(true);
		}*/
		StringBuilder builder = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(inputStream);
		do {
			char c = '\n';
			try {
				c = (char) reader.read();
			} catch(IOException e) {
				System.err.println("Fehler aufgetreten: " + e.getMessage());
				if(MainWindow.VERBOSE) {
					e.printStackTrace();
					System.err.println("-------------------------------------");
				}
			}
			if(c == '\n') {
				interprateCommand(builder.toString());
				builder = new StringBuilder();
			} else if((short) c == -1) {
				/*if(serverStart) {
					startServer(false);
					reader = new InputStreamReader(inputStream);
				} else {*/
				return;
				//}
			} else {
				builder.append(c);
			}
		} while(!terminate);
		hasTerminated = true;
	}
	
	/**
	 * Gibt die Klasse zurück, die zu dem angegebenen Namen passt.
	 * 
	 * @param c der Name der Klasse
	 * @return die zugehörige Klasse
	 * @throws ClassNotFoundException sollte die Klasse nicht importiert worden sein und nicht existieren
	 */
	private Class<?> getClassByString(String c) throws ClassNotFoundException {
		Class<?> clas = imported.get(c);
		if(clas == null) {
			clas = Class.forName(c);
		}
		return clas;
	}
	
	/**
	 * Fügt ein konkretes Objekt der Liste der Klassen hinzu, die keine weiteren
	 * Instanzen haben sollen.
	 * 
	 * @param object das Objekt, das statt einer neuen Instanz der Klasse verwendet werden soll
	 */
	public void addAPIObject(APIObject object) {
		noInstanceObjs.put(object.getClass(), object);
	}
	
	/**
	 * Beendet das ganze Programm.
	 */
	@RemoteExecution
	public void exit() {
		System.exit(0);
	}
	
	/**
	 * Gibt zurück, ob dieses {@link Runnable} beendet ist oder nicht.
	 * 
	 * @return ob die Arbeitsmethode beendet ist oder nicht
	 */
	public boolean isTerminated() {
		return hasTerminated;
	}
	
	/**
	 * Beendet den API-Service.
	 */
	@RemoteExecution
	public void terminate() {
		System.out.println("Service wird beendet");
		terminate = true;
	}
	
	/**
	 * Gibt ganz einfach das angegebene Objekt zurück.
	 * 
	 * @param object das Objekt, das zurückkommen soll wie ein Echo
	 * @return das angegebene Objekt
	 */
	@RemoteExecution
	public String echo(String object) {
		return object;
	}
	
	/**
	 * Setzt, ob die zurückgegebenen Objekte im byte-Format zurückgegebenen werden sollen.
	 * 
	 * @param b ob der byte-Modus aktiviert werden soll
	 */
	@RemoteExecution
	public void setByteModeEnabled(boolean b) {
		byteMode = b;
	}

	@Override
	public byte[] convertToBytes() {
		return null;
	}

	/**
	 * Startet den Server für entfernte API-Benutzung.
	 * 
	 * @param newServer ob der Server erzeugt werden soll oder nicht
	 */
	/*public void startServer(boolean newServer) {
		try {
			if(newServer) {
				server = new AssistentServer(9096); 
			}
			server.establishConnection();
			final Socket socket = server.getSocket();
			outputStream = new PrintStream(socket.getOutputStream());
			inputStream = socket.getInputStream();
		} catch(IOException e) {
			System.err.println("Konnte Server nicht starten!");
			e.printStackTrace();
			System.err.println("-------------------------------------");
		}
	}*/
}