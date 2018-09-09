package org.jurassicraft.server.util;
import java.util.Timer;

public class QueryData {
	
	private long time;
	private int area;
	private String ID;
	private Timer timer;
	
	public QueryData(long time, int area) {
		this.time = time;
		this.area = area;
	}
	
	public void setTime(long time) {
		this.time = time;
	}
	
	public void setArea(int area) {
		this.area = area;
	}
	
	public long getTime() {
		return this.time;
	}
	
	public int getArea() {
		return this.area;
	}
	
	public String getID() {
		return this.ID;
	}
	
	public void setID(String ID) {
		this.ID = ID;
	}
	
	public Timer getTimer() {
		return this.timer;
	}
	
	public void setTimer(Timer timer) {
		this.timer = timer;
	}
}