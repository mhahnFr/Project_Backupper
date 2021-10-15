package hahn.backup.assistent;

import hahn.rmi.APIObject;
import hahn.rmi.RemoteExecution;

/**
 * Nur eine Testklasse.
 * @author Manuel Hahn
 * @since 11.04.2018
 */
@RemoteExecution
public class StringTest implements APIObject {
	private String data;
	
	@Override
	public void parseInData(String data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		return data;
	}

	@Override
	public byte[] convertToBytes() {
		return data.getBytes();
	}
}