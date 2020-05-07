package com.hansol.ismon.sync;

import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.sync.SyncVerticle;
import static io.vertx.ext.sync.Sync.*;

public class PingPong extends SyncVerticle {

	private static final String ADDRESS = "ping-address";

	public static void main(String[] args) {
		// verticle launcher
		Launcher.executeCommand("run", new String[]{PingPong.class.getName()});
	}

	@Suspendable
	@Override
	public void start(Promise<Void> startFuture) throws Exception {
		
		startFuture.complete();
		
		EventBus eb = vertx.eventBus();
		eb.consumer(ADDRESS).handler(msg -> msg.reply("pong"));

		for (int i=0; i < 10; i++) {
			System.out.println("Thread is " + Thread.currentThread());

			Message<String> reply = awaitResult(h -> eb.request(ADDRESS, "ping", h));

			System.out.println("got reply: " + reply.body());

			// Like Thread.sleep but doesn't block the OS thread
			Strand.sleep(1000);
		}
	}
}
