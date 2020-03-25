package com.hansol.ismon.wsclient;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.Router;

public class WebsocketVerticle extends AbstractVerticle {

	public static void main( String[] args ) throws Exception
	{
		// verticle launcher
		Launcher.executeCommand("run", new String[]{WebsocketVerticle.class.getName()});
	}

	private LocalMap<String,String> wsSessions = null;
	private static final int maxSession = 2;

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		wsSessions = vertx.sharedData().getLocalMap("WEBSOCKET_CHANNEL");

		final HttpServer httpServer = vertx.createHttpServer();

		final Router router = Router.router(vertx);
		httpServer.requestHandler(router); // (router::accept); jw version up edit

		// jw 파일 다운로드 
		router.get("/download").handler(ctx -> {
			HttpServerResponse response = ctx.request().response();
			response.setChunked(true);
			Pump.pump(ctx.request(), response).start();

			ctx.request().response().putHeader("Content-Type", "application/octet-stream; charset=UTF-8");
			ctx.request().response().putHeader("Content-Disposition", "attachment; filename=\"sys-mon-kebhana-2020-03-22.tar.gz\"");
			ctx.request().response().sendFile("C:\\doc\\IS-MON\\하나은행\\서버의소스백업\\sys-mon-kebhana-2020-03-22.tar.gz", ar -> {
				// response.end();
				System.out.println("file download end :" + ar.succeeded());
			});

			// ctx.request().endHandler(v -> response.end());
		});


		/**
		 * @description 
		 * 	- 웹 화면에서 웹소켓으로 커넥션 갯수를 제한 하기 위한 테스트 코드임
		 * 	- vertx core 3.8.3 필요 (이전의 ServerWebSocket.reject()는 정상적으로 사용이 불가능)
		 * 	 curl -o dd.tar.gz localhost:18080/download
		 * */
		httpServer.websocketHandler(ws -> {

			Promise<Integer> promise = Promise.promise();
			Future<Integer> fut = promise.future(); // Future.future();

			ws.setHandshake(fut);
			authorization(ar -> {
				System.out.println(">" + ws.textHandlerID());
				if (ar.succeeded()) {
					// Terminate the handshake with the status code 101 (Switching Protocol)
					// Reject the handshake with 401 (Unauthorized)
					// fut.complete(ar.succeeded() ? 101 : 401);

					// fut.complete(101);
					promise.complete(101);
				} else {
					// Will send a 500 error
					// fut.fail(ar.cause());

					// fut.complete(401);
					promise.complete(401);
				}
			});

			ws.closeHandler(ch -> {
				System.out.println("Close Handler event, remove wsSession map...");
				if (wsSessions.containsKey(ws.textHandlerID())) {
					wsSessions.remove(ws.textHandlerID());
				}
			});

			ws.frameHandler(wsFrame -> {
				switch(ws.path()) {
				case "/sys-mon":
					if ( wsFrame.isText() ) {

						if ( !wsSessions.containsKey(ws.textHandlerID()) ) {
							wsSessions.put(ws.textHandlerID(), "ee");
							ws.writeFinalTextFrame("Service Registration Success");
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
					System.out.println("startup...");
					startPromise.complete();
				} else {
					startPromise.fail("verticle start fail");
				}
			});
		});

	}

	private void authorization(Handler<AsyncResult<JsonObject>> ar) {
		System.out.println("wsSessions.size() : " + wsSessions.size());
		if ( wsSessions.size() >= maxSession ) {
			System.out.println("max over...");
			ar.handle(Future.failedFuture("custom error") );
		} else {
			ar.handle(Future.succeededFuture());
		} 
	}
}
