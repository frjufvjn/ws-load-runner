package com.hansol.pom.db.sql;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Guice;
import com.hansol.ismon.data.VertxSqlConnectionFactory;
import com.hansol.pom.db.query.QueryModule;
import com.hansol.pom.db.query.QueryServices;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

public class SqlInnerServices {

	private final Logger logger = LogManager.getLogger(SqlInnerServices.class);

	private static final QueryServices queryServices = Guice.createInjector(new QueryModule()).getInstance(QueryServices.class);

	/**
	 * @description vertx sql client select
	 * @param params
	 * @param aHandler
	 */
	public void sqlRead(JsonObject params, Handler<AsyncResult<List<JsonObject>>> aHandler) {
		try {
			Map<String, Object> queryInfo = queryServices.getQuery(params);

			VertxSqlConnectionFactory.getClient().getConnection(conn -> {
				if (conn.failed()) aHandler.handle(Future.failedFuture(conn.cause()));

				try ( final SQLConnection connection = conn.result() ) {
					String sql = (String) queryInfo.get("sql");
					JsonArray param = (JsonArray)queryInfo.get("sqlParam");

					connection.queryWithParams(
							sql, 
							param, 
							ar -> {
						if(ar.succeeded()) {
							aHandler.handle(Future.succeededFuture(ar.result().getRows()));
						}
						else {
							logger.error(ar.cause().getMessage());
							aHandler.handle(Future.failedFuture(ar.cause()));
						}
					});
				}
			});

		} catch (Exception e) {
			aHandler.handle(Future.failedFuture(e));
		}
	}

	/**
	 * @description vertx sql client INSERT, UPDATE, DELETE
	 * @param params
	 * @param aHandler
	 */
	public void sqlCUD(JsonObject params, Handler<AsyncResult<Integer>> aHandler) {
		try {
			Map<String, Object> queryInfo = queryServices.getQuery(params);

			VertxSqlConnectionFactory.getClient().getConnection(conn -> {
				if (conn.failed()) aHandler.handle(Future.failedFuture(conn.cause()));

				try ( final SQLConnection connection = conn.result() ) {
					String sql = (String) queryInfo.get("sql");
					JsonArray param = (JsonArray)queryInfo.get("sqlParam");

					connection.updateWithParams(sql, param, ar -> {
						if(ar.succeeded()) {
							aHandler.handle(Future.succeededFuture( ar.result().getUpdated() ) );
						}
						else {
							logger.error(ar.cause().getMessage());
							aHandler.handle(Future.failedFuture(ar.cause()));
						}
					});
				}
			});

		} catch (Exception e) {
			aHandler.handle(Future.failedFuture(e));
		}
	}

	/**
	 * @description vertx sql client Batch Mode INSERT, UPDATE, DELETE
	 * @param params
	 * @param aHandler
	 */
	public void sqlCUDBatch(JsonObject params, Handler<AsyncResult<Integer>> aHandler) {
		try {
			Map<String, Object> queryInfo = queryServices.getQueryBatch(params);

			VertxSqlConnectionFactory.getClient().getConnection(conn -> {
				if (conn.failed()) aHandler.handle(Future.failedFuture(conn.cause()));

				try ( final SQLConnection connection = conn.result() ) {
					String sql = (String) queryInfo.get("sql");

					@SuppressWarnings("unchecked")
					List<JsonArray> batch = (List<JsonArray>) queryInfo.get("batchParam");

					connection.batchWithParams(sql, batch, ar -> {
						if(ar.succeeded()) {
							aHandler.handle(Future.succeededFuture() );
						}
						else {
							logger.error(ar.cause().getMessage());
							aHandler.handle(Future.failedFuture(ar.cause()));
						}
					});
				}
			});

		} catch (Exception e) {
			aHandler.handle(Future.failedFuture(e));
		}
	}
}
