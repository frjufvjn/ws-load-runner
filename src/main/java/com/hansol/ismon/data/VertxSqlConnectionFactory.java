package com.hansol.ismon.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.ibatis.io.Resources;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

public class VertxSqlConnectionFactory {

	private static JDBCClient jdbcClient;
	private static Properties prop = new Properties();

	static {
		try {
			final String confPath = System.getProperty("app.home") == null ? 
					String.join(File.separator, System.getProperty("user.dir"), "config", "db.properties") 
					: String.join(File.separator, System.getProperty("app.home"), "config", "db.properties");
			prop.load(new FileReader(confPath));

			Vertx vertx = Vertx.vertx();
			
			// c3p0 (default)
//			jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
//					.put("url", prop.getProperty("db.url"))
//					.put("driver_class", prop.getProperty("db.driver"))
//					.put("max_pool_size", Integer.parseInt(prop.getProperty("db.max_pool_size")))
//					.put("user", prop.getProperty("db.username"))
//					.put("password", prop.getProperty("db.password"))
//					.put("max_idle_time", Integer.parseInt(prop.getProperty("db.max_idle_time")))
//					.put("min_pool_size", Integer.parseInt(prop.getProperty("db.min_pool_size")))
//					.put("acquire_retry_attempts", Integer.parseInt(prop.getProperty("db.acquire_retry_attempts")))
//					);
			// HikariCP
			jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
					.put("provider_class", "io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider")
					.put("jdbcUrl", prop.getProperty("db.url"))
					.put("driverClassName", prop.getProperty("db.driver"))
					.put("maximumPoolSize", Integer.parseInt(prop.getProperty("db.max_pool_size")))
					.put("username", prop.getProperty("db.username"))
					.put("password", prop.getProperty("db.password"))
					.put("idleTimeout", 600000)
					.put("minimumIdle", Integer.parseInt(prop.getProperty("db.min_pool_size")))
					// .put("acquire_retry_attempts", Integer.parseInt(prop.getProperty("db.acquire_retry_attempts")))
					);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static JDBCClient getClient() {
		return jdbcClient;
	}
}
