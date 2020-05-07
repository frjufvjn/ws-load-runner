package com.hansol.ismon.wsclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class FileUploadVerticle extends AbstractVerticle {

	private final static Logger logger = LogManager.getLogger(FileUploadVerticle.class);

	public static void main( String[] args ) throws Exception
	{
		// verticle launcher
		Launcher.executeCommand("run", new String[]{FileUploadVerticle.class.getName()});
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		final Router router = Router.router(vertx);
		router.route().handler(StaticHandler.create().setCachingEnabled(false));

		final HttpServer httpServer = vertx.createHttpServer();

		httpServer.requestHandler(router);

		vertx.executeBlocking(blk -> {
			httpServer.listen(8080, l -> {
				if (l.succeeded()) blk.complete();
			});
		}, blkRes -> {
			if (blkRes.succeeded()) logger.info("server startup ... ");
		});
		
		Route fileRouter = router.post("/form");
		fileRouter.handler(ctx -> {
			HttpServerRequest req = ctx.request();
			req.setExpectMultipart(true);
			req.uploadHandler(upload -> {
				upload.exceptionHandler(cause -> {
					logger.error(cause.getMessage());
					req.response().setChunked(true).end("Upload failed");
				});

				upload.endHandler(v -> {
					req.response()
						.setChunked(true)
						.putHeader("Content-Type", "text/plain; charset=UTF-8")
						.end("Successfully uploaded to " + upload.filename());
				});
				// FIXME - Potential security exploit! In a real system you must check this filename
				// to make sure you're not saving to a place where you don't want!
				// Or better still, just use Vert.x-Web which controls the upload area.
				upload.streamToFileSystem("uploads/" + upload.filename());

			});
		});
	}
}
