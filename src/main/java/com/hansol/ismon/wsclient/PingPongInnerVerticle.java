package com.hansol.ismon.wsclient;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;

public class PingPongInnerVerticle extends AbstractVerticle {
	
	public static void main(String[] args) {
		// verticle launcher
		Launcher.executeCommand("run", new String[]{PingPongInnerVerticle.class.getName()});
	}
	
	@Override
	public void start(Future<Void> startFuture) throws Exception {
		
		
		
		vertx.eventBus().consumer("addr", msg -> {
			msg.reply("Got the " + msg.body() + ", reply pong...");
		});
		
		vertx.setPeriodic(1000, pid -> {
			vertx.eventBus().request("addr", "Hello", reply -> {
				System.out.println(reply.result().body().toString());
			});
		});
	}
}
