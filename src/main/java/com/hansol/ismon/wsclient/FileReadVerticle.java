package com.hansol.ismon.wsclient;

import java.io.File;
import java.io.FileFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Guice;
import com.hansol.pom.db.sql.SqlInnerModule;
import com.hansol.pom.db.sql.SqlInnerServices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;

public class FileReadVerticle extends AbstractVerticle {

	private final Logger logger = LogManager.getLogger(FileReadVerticle.class);
	private static final String targetPath = "C:/doc/IS-MON/Agent-Test";
	private static final SqlInnerServices sqlService = Guice.createInjector(new SqlInnerModule())
			.getInstance(SqlInnerServices.class);

	public static void main(String[] args) {
		// verticle launcher
		Launcher.executeCommand("run", new String[]{FileReadVerticle.class.getName()});
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		logger.info("start... file read verticle");

		vertx.executeBlocking(blk -> {
			File fl = new File(targetPath);
			File[] files = fl.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return file.isFile();
				}
			});
			blk.complete(files);

		}, resBlk -> {
			File[] files = (File[]) resBlk.result();

			// 파일 목록
			for (File file : files) {
				logger.info("- file : {}", file.getName());

				// 비동기 파일 오픈
				vertx.fileSystem().open(targetPath + "/" + file.getName(), new OpenOptions() , result -> {
					if ( result.succeeded() ) {
						AsyncFile asyncFile = result.result();

						// 라인별로 리드 
						RecordParser recordParser = RecordParser.newDelimited("\n", bufferedLine -> {
							// logger.info("{} - bufferedLine = {}",file.getName(), bufferedLine);
							String readLine = bufferedLine.toString();
							int findIdx = readLine.indexOf("SEND]");
							if ( findIdx != -1 ) {
								// logger.info("slice : {}", readLine.substring(findIdx+6));
								try {
									// logger.info("parsed : {}", new JsonObject(readLine.substring(findIdx+6)).encode());
									JsonObject obj = new JsonObject(readLine.substring(findIdx+6));
									final String currtime = obj.getString("executetime").substring(0, 19);
									final Double cpu = obj.getDouble("cpu");
									final Double rmem = obj.getDouble("rmem");
									final Double smem = obj.getDouble("smem");
									Double value1 = null, value2 = null, value3 = null;

									JsonArray list = obj.getJsonArray("diskinfo");
									for (Object ele : list) {
										JsonObject itemObj = ((JsonObject)ele);
										if ( "/".equals(itemObj.getString("path")) ) {
											value1 = itemObj.getDouble("usedPercent");
										}
										else if ( "/run".equals(itemObj.getString("path")) ) {
											value2 = itemObj.getDouble("usedPercent");
										}
										else if ( "/dev".equals(itemObj.getString("path")) ) {
											value3 = itemObj.getDouble("usedPercent");
										}
									}
									logger.info("currtime: {}, cpu: {}, rmem: {}, smem: {}, val1: {}, val2: {}, val3: {}", 
											currtime, cpu, rmem, smem, value1, value2, value3);

									JsonObject sqlParam = new JsonObject()
											.put("sqlName", "sql_insert_agent_log_save")
											.put("current_time", currtime)
											.put("cpu", cpu)
											.put("rmem", rmem)
											.put("smem", smem)
											.put("disk1_name", "/")
											.put("disk1_value", value1)
											.put("disk2_name", "/run")
											.put("disk2_value", value2)
											.put("disk3_name", "/dev")
											.put("disk3_value", value3)
											;

									sqlService.sqlCUD(sqlParam, ar -> {
										if (ar.succeeded()) logger.info("STAT SAVE SUCCESS");
										else logger.error(ar.cause());
									});

								} catch (Exception e) {
									logger.error("parsing error : {}", e.getMessage());
								}
							}
						});

						asyncFile.handler(recordParser)
						.endHandler(v -> {
							asyncFile.close();
							logger.info("{} Done", file.getName());
						});
					}
				});
			}
		});
	}
}
