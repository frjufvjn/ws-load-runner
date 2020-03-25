package com.hansol.ismon.wsclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.Router;

public class WebsocketVerticle extends AbstractVerticle {

	public static void main( String[] args ) throws Exception
	{
		// verticle launcher
		Launcher.executeCommand("run", new String[]{WebsocketVerticle.class.getName()});
	}

	private final Logger logger = LogManager.getLogger(WebsocketVerticle.class);
	private LocalMap<String,String> wsSessions = null;
	private static final int maxSession = 2;

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		// log4j2 Setting
		System.setProperty("vertx.logger-delegate-factory-class-name", Log4j2LogDelegateFactory.class.getName());

		wsSessions = vertx.sharedData().getLocalMap("WEBSOCKET_CHANNEL");

		/**
		 * @description 
		 * 	1. Sec-WebSocket-Protocol header, sub protocol 을 추가 시킴
		 * 	2. 인증 용도로 클라이언트에서 호출하는 방법 
		 * 		- var subProtocol = ["strstr","hash"]; // 첫번째값은 sub protocol로 정의 되어 있어야 하며 / 두번째 값을 해시로 취득하여 인증에 사용할 수 있도록 한다. 
		 * 		- new WebSocket("ws://localhost:18080/sys-mon", subProtocol);
		 * */
		HttpServerOptions httpServerOption = new HttpServerOptions().setWebsocketSubProtocols("strstr");
		final HttpServer httpServer = vertx.createHttpServer(httpServerOption);

		final Router router = Router.router(vertx);
		httpServer.requestHandler(router); // (router::accept); jw version up edit





		/**
		 * @description 
		 * 	파일 다운로드 테스트
		 * 	curl -o dd.tar.gz localhost:18080/download
		 * */
		router.get("/download").handler(ctx -> {
			HttpServerResponse response = ctx.request().response();
			response.setChunked(true);
			Pump.pump(ctx.request(), response).start();

			response.putHeader("Content-Type", "application/octet-stream; charset=UTF-8");
			response.putHeader("Content-Disposition", "attachment; filename=\"sys-mon-kebhana-2020-03-22.tar.gz\"");
			response.sendFile("C:\\doc\\IS-MON\\하나은행\\서버의소스백업\\sys-mon-kebhana-2020-03-22.tar.gz", ar -> {
				// response.end();
				logger.info("file download end : {}", ar.succeeded());
			});

			// ctx.request().endHandler(v -> response.end());
		});





		/**
		 * @description 
		 * 	- 웹 화면에서 웹소켓으로 커넥션 갯수를 제한 하기 위한 테스트 코드임
		 * 	- vertx core 3.8.3 필요 (이전의 ServerWebSocket.reject()는 정상적으로 사용이 불가능)
		 * */

		httpServer.websocketHandler(ws -> {

			Promise<Integer> promise = Promise.promise();
			Future<Integer> fut = promise.future(); // Future.future();

			ws.setHandshake(fut);
			authorization(ws.headers(), ar -> {
				logger.info("-----------------------------------------------------------------Print Header");
				ws.headers().forEach(item -> {
					logger.info("key : {}, value : {}", item.getKey(), item.getValue());
				});
				logger.info("-----------------------------------------------------------------");
				logger.info("ws handler id : {}", ws.textHandlerID());
				if (ar.succeeded()) {
					// Terminate the handshake with the status code 101 (Switching Protocol)
					// Reject the handshake with 401 (Unauthorized)
					// fut.complete(ar.succeeded() ? 101 : 401);

					// fut.complete(101);
					logger.info("Auth pass --> Switching Protocol");
					promise.complete(101);
				} else {
					// Will send a 500 error
					// fut.fail(ar.cause());

					// fut.complete(401);
					logger.info("Auth failed");
					promise.complete(401);
				}
			});

			ws.closeHandler(ch -> {
				logger.info("Close Handler event...");
				if (wsSessions.containsKey(ws.textHandlerID())) {
					logger.info("Close Handler event, remove wsSession map...");
					wsSessions.remove(ws.textHandlerID());
				}
			});

			ws.frameHandler(wsFrame -> {
				switch(ws.path()) {
				case "/sys-mon":
					if ( wsFrame.isText() ) {

						if ( !wsSessions.containsKey(ws.textHandlerID()) ) {
							wsSessions.put(ws.textHandlerID(), "ee");

							ws.writeFinalTextFrame(new JsonObject()
									.put("ResultCode", "OK")
									.put("ResultMessage", "Service Registration Success")
									.encode());
							logger.info("Regi Success...");
						}

					}
					break;
				}
			});
		});

		vertx.runOnContext(r -> {
			vertx.executeBlocking(bl -> {
				httpServer.listen(18080, ar -> {
					bl.complete();
				});
			}, res -> {
				if (res.succeeded()) {
					logger.info("startup...");
					startPromise.complete();
				} else {
					logger.error("verticle start fail");
					startPromise.fail("verticle start fail");
				}
			});
		});

	}

	private void authorization(MultiMap map, Handler<AsyncResult<JsonObject>> ar) {
		logger.info("current wsSessions.size (before regi) : {}", wsSessions.size());
		if ( wsSessions.size() >= maxSession ) {
			logger.info("max over.. ({})", maxSession);
			ar.handle(Future.failedFuture("custom error") );
		} else {
			// ar.handle(Future.succeededFuture());
			if (map.contains("Sec-WebSocket-Protocol")) {

				String[] arr = map.get("Sec-WebSocket-Protocol").split(",");
				if ("hash".equals(arr[1].trim())) {
					ar.handle(Future.succeededFuture());
				} else {
					ar.handle(Future.failedFuture("custom error") );
				}
			} else {
				ar.handle(Future.failedFuture("custom error") );
			}
		}
	}
}
