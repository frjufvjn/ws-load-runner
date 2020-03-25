package com.hansol.ismon.wsclient;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Log4j2LogDelegateFactory;

public class MainVerticle extends AbstractVerticle {

	private final Logger logger = LogManager.getLogger(MainVerticle.class);

	public static void main( String[] args ) throws Exception
	{
		// verticle launcher
		Launcher.executeCommand("run", new String[]{MainVerticle.class.getName()});
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		
		// log4j2 Setting
		System.setProperty("vertx.logger-delegate-factory-class-name", Log4j2LogDelegateFactory.class.getName());
		
		logger.info("Start Main Verticle");
		
		final String appHome = System.getenv("APP_HOME") != null ? System.getenv("APP_HOME") : System.getProperty("user.dir");
		final String confPath = String.join(File.separator, appHome, "config", "config.yaml");

		logger.info("confPath : " + confPath);

		ConfigRetrieverOptions options = new ConfigRetrieverOptions()
				.setScanPeriod(60_000L)
				.addStore(new ConfigStoreOptions()
						.setType("file")
						.setFormat("yaml")
						.setConfig(new JsonObject().put("path", confPath)));

		ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

		retriever.getConfig(this::startup);

	}

	private void startup(final AsyncResult<JsonObject> ar) {
		if (ar.succeeded()) {

			/**
			 * @description IS-MON Agent Mode 서버에 가상의 Device를 다수로 만들어 Connection을 생성하여 가혹한 성능 테스트를 실시함.
			 * 	- numOfVirtualDevices : 가상 Device의 갯수
			 * 	- sendDelayMills : Device별로 Send할 간격 (ms)
			 * */
			logger.info(ar.result().encodePrettily() );
			final int numOfVirtualDevices = ar.result().getJsonObject("control-value").getInteger("numOfVirtualDevices");
			final long sendDelayMills = ar.result().getJsonObject("control-value").getLong("sendDelayMills");

			final String serverHost = ar.result().getJsonObject("connect-info").getString("host");
			final int serverPort = ar.result().getJsonObject("connect-info").getInteger("port");
			final String uri = ar.result().getJsonObject("connect-info").getString("uripath");

			final String payload = ar.result().getJsonObject("payload").getString("str");

			final JsonArray arr = ar.result().getJsonArray("variable-input-sample-list");
			arr.stream().forEach(item -> {

				HttpClient wsClient = vertx.createHttpClient();
				WebSocketConnectOptions options = new WebSocketConnectOptions()
						.setHost(serverHost)
						.setPort(serverPort)
						.setURI(uri);

				wsClient.webSocket(options, conn -> {
					if (conn.succeeded()) {
						WebSocket ws = conn.result();

						ws.exceptionHandler(ex -> {
							ex.printStackTrace();
						});

						ws.closeHandler(ch -> {
							logger.info("closeHandler event...");
						});

						ws.handler(data -> {
							logger.info("Received data : {}", data.toString("ISO-8859-1"));
						});



						vertx.setPeriodic(sendDelayMills, p -> {
							String sendPayload = new JsonObject(payload).put("deviceid", item.toString()).encode();
							ws.writeFinalTextFrame(sendPayload);
						});

					} else {
						logger.error("websocket connect failed : {}", conn.cause().getMessage());
						wsClient.close();
					}
				});
			});

			logger.info("loop Async Request End");
		} else {
			logger.error(ar.cause().getMessage());
		}
	}
}
