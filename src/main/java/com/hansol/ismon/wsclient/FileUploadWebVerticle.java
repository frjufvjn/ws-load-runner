package com.hansol.ismon.wsclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class FileUploadWebVerticle extends AbstractVerticle {

	private final static Logger logger = LogManager.getLogger(FileUploadWebVerticle.class);

	public static void main( String[] args ) throws Exception
	{
		// verticle launcher
		Launcher.executeCommand("run", new String[]{
				FileUploadWebVerticle.class.getName()
		});
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		Router router = Router.router(vertx);

		// Enable multipart form data parsing
		router.route().handler(BodyHandler.create().setUploadsDirectory("uploads"));

		router.route("/").handler(routingContext -> {
			routingContext.response().putHeader("content-type", "text/html; charset=UTF-8").end(
					"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"
					+ "<form action=\"/form\" method=\"post\" enctype=\"multipart/form-data\" accept-charset=\"UTF-8\">\n" +
							"    <div>\n" +
							"        <label for=\"name\">Select a file:</label>\n" +
							"        <input type=\"file\" name=\"file\" />\n" +
							"    </div>\n" +
							"    <div class=\"button\">\n" +
							"        <button type=\"submit\">Send</button>\n" +
							"    </div>" +
							"</form>"
					);
		});

		
		
		// handle the form
		router.post("/form").handler(ctx -> {

			ctx.response().putHeader("Content-Type", "text/plain; charset=UTF-8");

			ctx.response().setChunked(true);

			for (FileUpload f : ctx.fileUploads()) {

				logger.info("f.uploadedFileName() : {}", f.uploadedFileName());
				logger.info("f.fileName() : {}", f.fileName());

				ctx.response().write("Filename: " + f.fileName());
				ctx.response().write("\n");
				ctx.response().write("Size: " + f.size());
			}

			ctx.response().end();
		});


		vertx.executeBlocking(blk -> {
			vertx.createHttpServer().requestHandler(router).listen(8080, l -> {
				if (l.succeeded()) blk.complete();
			});
		}, blkRes -> {
			if (blkRes.succeeded()) logger.info("server startup ... ");
		});
	}
}
