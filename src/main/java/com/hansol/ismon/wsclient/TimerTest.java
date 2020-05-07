package com.hansol.ismon.wsclient;

import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;

public class TimerTest extends AbstractVerticle {
	
	private static ConcurrentHashMap<String,Integer> chMap = new ConcurrentHashMap<>();
	
	public static void main( String[] args ) throws Exception
	{
		// verticle launcher
		Launcher.executeCommand("run", new String[]{TimerTest.class.getName()});
	}
	
	@Override
	public void start() throws Exception {
		vertx.setPeriodic(200, tid -> {
			System.out.println("------------");
			if ( !chMap.isEmpty() ) {
				vertx.cancelTimer(tid);
				vertx.close();
			}
		});
		
		vertx.setTimer(2000, t -> {
			chMap.put("key", 1);
		});
	}
}
