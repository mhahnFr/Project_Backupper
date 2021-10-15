package hahn.backup.core;

import java.io.File;
import java.net.URI;

public class BackupFile extends File {
	private static final long serialVersionUID = -3844093295746177383L;
	private int lastBYear;
	private int lastBMonth;
	private int lastBDay;
	private int lastBHour;
	private int lastBMinute;
	private int lastBSecond;

	public BackupFile(URI uri) {
		super(uri);
	}

	public BackupFile(String parent, String child) {
		super(parent, child);
	}

	public BackupFile(String pathname) {
		super(pathname);
	}

	public BackupFile(File parent, String child) {
		super(parent, child);
	}

	public void setLastBackedUp(int year, int month, int day, int hour, int minute, int second) {
		lastBYear = year;
		lastBMonth = month;
		lastBDay = day;
		lastBHour = hour;
		lastBMinute = minute;
		lastBSecond = second;
	}
	
	public int getLastBackupDay() {
		return lastBDay;
	}
	
	public int getLastBackupYear() {
		return lastBYear;
	}
	
	public int getLastBackupSecond() {
		return lastBSecond;
	}
	
	public int getLastBackupMonth() {
		return lastBMonth;
	}
	
	public int getLastBackupMinute() {
		return lastBMinute;
	}
	
	public int getLastBackupHour() {
		return lastBHour;
	}
	
	
}