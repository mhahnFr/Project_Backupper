package hahn.rmi;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import hahn.backup.assistent.Convertable;
import hahn.backup.assistent.NoImplementationException;
import hahn.rmi.RemoteExecution;
import hahn.utils.ByteHelper;

/**
 * Diese Klasse ist ein ausschließliches Interface mit Methoden,
 * die alle API-Klassen implementieren müssen.
 * 
 * @author Manuel Hahn
 * @since 10.04.2018
 */
public interface APIObject extends Convertable {
	/**
	 * Findet und führt die angegebene Methode in dieser Klasse aus.
	 * 
	 * @param server der Server mit dem ObjectOutputStream
	 * @param out der {@link PrintStream} für die Ausgabe
	 * @param method die Methode die ausgeführt werden soll
	 * @param perByte ob das eventuelle Ergebnis möglichst als byte-Array ausgegebenen werden soll
	 * @param arg das optionale Argument für die auszuführende Methode
	 * @throws NoSuchMethodException sollte die Methode nicht existieren
	 * @throws SecurityException sollte es nicht erlaubt sein, sie auszuführen
	 * @throws IllegalAccessException darf sie nicht aufgerufen werden, also sie ist nicht protected oder public
	 * @throws IllegalArgumentException sollte die Methode Argumente erfordern
	 * @throws InvocationTargetException sollte dieses Objekt die Methode aus einem Grund nicht ausführen können
	 * @throws IOException sollte das optionale Ergebnis nicht in den Stream geschrieben werden können
	 */
	default void findAndInvokeMethod(/*AssistentServer server*/final ObjectOutputStream stream, PrintStream out, String method, boolean perByte, Object[] arg) 
			throws NoSuchMethodException,
			SecurityException, 
			IllegalAccessException, 
			IllegalArgumentException, 
			InvocationTargetException,
			IOException {
		if(out == null) {
			out = System.out;
		}
		Method m;
		Object returned;
		Class<?> thisClass = getClass();
		boolean annotated = thisClass.isAnnotationPresent(RemoteExecution.class);
		if(arg != null && arg.length > 0) {
			Class<?>[] argClass = new Class<?>[arg.length];
			for(int i = 0; i < argClass.length; i++) {
				Class<?> c = arg[i].getClass();
				if(c.equals(Boolean.class)) {
					c = Boolean.TYPE;
				}
				argClass[i] = c;
			}
			m = getClass().getMethod(method, argClass);
			if(!annotated && !m.isAnnotationPresent(RemoteExecution.class)) {
				throw new IllegalAccessException("Zugriff auf die angegebene Methode ist nicht erlaubt!");
			}
			returned = m.invoke(this, arg);
		} else {
			m = getClass().getMethod(method);
			if(!annotated && !m.isAnnotationPresent(RemoteExecution.class)) {
				throw new IllegalAccessException("Zugriff auf die angegebene Methode ist nicht erlaubt!");
			}
			returned = m.invoke(this);
		}
		if(!m.getReturnType().equals(Void.TYPE)) {
			if(perByte && returned instanceof Convertable) {
				final byte[] r = ((Convertable) returned).convertToBytes();
				byte[] count = ByteHelper.intToBytes(r.length);
				out.write(count, 0, count.length);
				out.write(r, 0, r.length);
			} else if(perByte && returned instanceof Serializable) {
				//server.getObjectOutputStream().writeObject(returned);
				//server.getObjectOutputStream().flush();
				stream.writeObject(returned);
				stream.flush();
			} else {
				out.println(returned);
			}
			out.flush();
		}
	}
	
	/**
	 * Liest die Daten ein, die die Klasse über {@link APIObject#toString()} ausgibt.
	 * 
	 * @param data die Daten, die als Text vorliegen
	 */
	default void parseInData(String data) {
		throw new NoImplementationException("Methode ist nicht implementiert!");
	}
}