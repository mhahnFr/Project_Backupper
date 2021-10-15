package hahn.utils;

/**
 * Mit dieser Klasse können zwei beliebige Objekte miteinander verknüpft werden.
 * 
 * @author Manuel Hahn
 * @since 20.12.2017
 */
public class Pair<T, O> {
	private T object1;
	private O object2;
	
	/**
	 * Verbindet die beiden angegebenen Objekte miteinander. Sollten bereits
	 * zwei Objekte miteinander verbunden worden sein, werden sie überschrieben.
	 * 
	 * @param object1 das erste Objekt
	 * @param object2 das zweite Objekt
	 */
	public void link(T object1, O object2) {
		this.object1 = object1;
		this.object2 = object2;
	}
	
	/**
	 * Gibt das erste Objekt zurück.
	 * 
	 * @return das erste Objekt vom Typ T
	 */
	public T getT() {
		return object1;
	}
	
	/**
	 * Gibt das zweite Objekt zurück.
	 * 
	 * @return das zweite Objekt vom Typ O
	 */
	public O getO() {
		return object2;
	}
	
	/**
	 * Gibt das mit dem angegebenen Objekt verbundene zurück. Sollte der 
	 * angegebene Wert nicht einem der beiden gespeicherten entsprechen sollte,
	 * wird null zurückgegeben.
	 * 
	 * @param linked das Objekt, dessen zugehöriges Objekt zurückgegeben werden soll
	 * @return das zum angegebenen Objekt verlinkte
	 */
	public Object getThePairedValue(Object linked) {
		if(linked.equals(object1)) {
			return object2;
		} else if(linked.equals(object2)) {
			return object1;
		}
		return null;
	}
	
	public String toString() {
		return object1.toString() + " <-> " + object2.toString();
	}
}