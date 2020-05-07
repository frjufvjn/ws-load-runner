package com.hansol.ismon.wsclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class SessionVerticle extends AbstractVerticle {

	private final Logger logger = LogManager.getLogger(SessionVerticle.class);
	private static final int httpPort = 8080;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main( String[] args ) throws Exception
	{
		/**
		 * @description verticle launcher
		 * */
		// Launcher.executeCommand("run", new String[]{SessionVerticle.class.getName()});

		/**
		 * @description vertx deploy
		 * */
		// Vertx.vertx().deployVerticle(SessionVerticle.class.getName(), new DeploymentOptions().setInstances(4));
		Vertx.vertx().deployVerticle(SessionVerticle.class.getName());
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		// log4j2 Setting
		System.setProperty("vertx.logger-delegate-factory-class-name", Log4j2LogDelegateFactory.class.getName());

		vertx.executeBlocking(promise -> {
			logger.info("Start Main SessionVerticle... ");
			startupHttpServer(promise);
		}, result -> {
			if (result.failed()) logger.error(result.cause().getMessage());
		});
	}

	private void startupHttpServer(Promise<Object> promise) {
		Router router = Router.router(vertx);

		// router.route().handler(CookieHandler.create());
		router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx, "h-session", 5000)).setSessionTimeout(5000));

		router.route().handler(routingContext -> {
			logger.info("call...");
			Session session = routingContext.session();

			Integer cnt = session.get("hitcount");
			cnt = (cnt == null ? 0 : cnt) + 1;

			session.put("hitcount", cnt);

			routingContext.response().putHeader("content-type", "text/html")
			.end("<html><body><h1>Hitcount: " + cnt + "</h1></body></html>");
		});

		vertx.createHttpServer().requestHandler(router).listen(httpPort, res -> {
			if ( res.succeeded() ) {
				logger.info("Vertx HTTP Server Startup successfully. on : {}", httpPort);
				promise.complete();
			} else {
				promise.fail(res.cause());
			}
		});
	}
}
