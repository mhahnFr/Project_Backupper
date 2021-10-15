package hahn.backup.core;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Diese Klasserepräsentiert ein Datum; z. B. 11.12.2017 18:52:32
 * 
 * @author Manuel Hahn
 * @since 11.12.2017
 */
public class Date {
	private final int year;
	private final byte month;
	private final byte day;
	private final byte hour;
	private final byte minute;
	private final byte second;

	/**
	 * Erzeugt ein Datum mit dem angegebenen Zeitpunkt.
	 * 
	 * @param year das Jahr
	 * @param month der Monat
	 * @param day der Tag
	 * @param hour die Stunde
	 * @param minute die Minute
	 * @param second die Sekunde
	 */
	public Date(int year, byte month, byte day, byte hour, byte minute, byte second) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
	}
	
	/**
	 * Erzeugt ein Datum mit dem Zeitpunkt, an dem es erzeugt wurde.
	 */
	public Date() {
		Calendar calendar = Calendar.getInstance();
		year = calendar.get(Calendar.YEAR);
		month = (byte) (calendar.get(Calendar.MONTH) + 1);
		day = (byte) calendar.get(Calendar.DAY_OF_MONTH);
		hour = (byte) calendar.get(Calendar.HOUR_OF_DAY);
		minute = (byte) calendar.get(Calendar.MINUTE);
		second = (byte) calendar.get(Calendar.SECOND);
	}
	
	/**
	 * Gibt die Jahreszahl dieses Datums zurück.
	 * 
	 * @return die Jahreszahl dieses Datums
	 */
	public int getYear() {
		return year;
	}
	
	/**
	 * Gibt die Sekundenzahl dieses Datums zurück.
	 * 
	 * @return die Sekundenzahl dieses Datums
	 */
	public byte getSecond() {
		return second;
	}
	
	/**
	 * Gibt den Tag zurück.
	 * 
	 * @return die Zahl des Tages im Monat
	 */
	public byte getDay() {
		return day;
	}
	
	/**
	 * Gibt die Stunde dieses Datums zurück. Zugrunde gelegt wird dabei ein 24-Stundentag.
	 * 
	 * @return die Stunde des Tages
	 */
	public byte getHour() {
		return hour;
	}
	
	/**
	 * Gibt die Minute der Stunde zurück.
	 * 
	 * @return die Minute der Stunde
	 */
	public byte getMinute() {
		return minute;
	}
	
	/**
	 * Gibt den Monat dieses Datums zurück.
	 * 
	 * @return den Monat im Jahr
	 */
	public byte getMonth() {
		return month;
	}
	
	/**
	 * Gibt den vorhergehenden Tag um 00:00:00 zurück.
	 * 
	 * @return den Tag vor diesem
	 */
	public Date getPreviousDay() {
		byte nDay = (byte) (day - 1);
		int nYear = year;
		byte nMonth = month;
		if(nDay == 0) {
			nDay = 31;
			nMonth = (byte) (month - 1);
			switch(nMonth) {
			case 1:
			case 3:
			case 5:
			case 7:
			case 8:
			case 10:
			case 12:
				nDay = 31;
				break;
				
			case 4:
			case 6:
			case 9:
			case 11:
				nDay = 30;
				break;
				
			case 2:
				nDay = 28;
				break;
				
			case 0:
				nMonth = 12;
				nYear = year - 1;
				nDay = 31;
			}
		}
		return new Date(nYear, nMonth, nDay, (byte) 0, (byte) 0, (byte) 0);
	}
	
	/**
	 * Gibt einen Zeitstempel zurück. Der Zeitstempel ist nach diesem Schema 
	 * formattiert: JJJJ-MM-TT-HHMMSS
	 * 
	 * @return einen Zeitstempel
	 */
	public String toString() {
		String stamp = "";
		stamp += year;
		stamp += "-" + (month < 10 ? "0" + month : month);
		stamp += "-" + (day < 10 ? "0" + day : day);
		stamp += "-" + (hour < 10 ? "0" + hour : hour);
		stamp += "" + (minute < 10 ? "0" + minute : minute);
		stamp += "" + (second < 10 ? "0" + second : second);
		return stamp;
	}
	
	/**
	 * Überprüft, ob das angegebene Datum nach diesem liegt.
	 * 
	 * @param date das zu vergleichende Datum
	 * @return ob das angegebene Datum nach diesem liegt.
	 */
	public boolean isBefore(Date date) {
		if(date.year > year) {
			return true;
		} else if(date.year < year) {
			return false;
		}
		if(date.month > month) {
			return true;
		} else if(date.month < month) {
			return false;
		}
		if(date.day > day) {
			return true;
		} else if(date.day < day) {
			return false;
		}
		if(date.hour > hour) {
			return true;
		} else if(date.hour < hour) {
			return false;
		}
		if(date.minute > minute) {
			return true;
		} else if(date.minute < minute) {
			return false;
		}
		if(date.second > second) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + day;
		result = prime * result + hour;
		result = prime * result + minute;
		result = prime * result + month;
		result = prime * result + second;
		result = prime * result + year;
		return result;
	}
	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Date other = (Date) obj;
		if (day != other.day)
			return false;
		if (hour != other.hour)
			return false;
		if (minute != other.minute)
			return false;
		if (month != other.month)
			return false;
		if (second != other.second)
			return false;
		if (year != other.year)
			return false;
		return true;
	}

	/**
	 * Sortiert die angegebenen Daten aufsteigend. Je höher der Index, desto aktueller.
	 * Das erste Datum ist also das, welches am längsten vergangen ist.
	 * 
	 * @param dates die Daten, die sortiert werden sollen
	 * @return ein Array mit den angegebenen Daten, aufsteigend sortiert
	 */
	public static Date[] sort(Date[] dates) {
		ArrayList<Date> list = new ArrayList<>();
		Date last = null;
		boolean insert;
		for(Date d : dates) {
			insert = false;
			if(last == null) {
				list.add(d);
			} else {
				/*int index = list.indexOf(last);
				if(last.isBefore(d)) {
					list.add(index + 1, d);
				} else {
					list.add(index - 1, d);
				}*/
				for(int i = 0; i < list.size(); i++) {
					if(d.isBefore(list.get(i))) {
						list.add(i, d);
						insert = true;
						break;
					}
				}
				if(!insert) {
					list.add(d);
				}
			}
			last = d;
		}
		return list.toArray(new Date[list.size()]);
	}
	
	/**
	 * Liest aus dem angegebenen String ein Datum ein. Der Zeitstempel 
	 * muss von {@link #toString()} kommen.
	 * 
	 * @param timeStamp der Zeitstempel, der zu einem Datum eingelesen werden soll
	 * @return ein Datum, das den im Zeitstempel angegebenen Zeitpunkt repräsentiert
	 */
	public static Date parseDate(String timeStamp) {
		int year = Integer.parseInt(timeStamp.substring(0, 4));
		byte month = Byte.parseByte(timeStamp.substring(5, 7));
		byte day = Byte.parseByte(timeStamp.substring(8, 10));
		byte hour = Byte.parseByte(timeStamp.substring(11, 13));
		byte minute = Byte.parseByte(timeStamp.substring(13, 15));
		byte second = Byte.parseByte(timeStamp.substring(15));
		return new Date(year, month, day, hour, minute, second);
	}
}