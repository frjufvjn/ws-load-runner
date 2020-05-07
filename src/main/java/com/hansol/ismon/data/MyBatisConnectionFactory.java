package com.hansol.ismon.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class MyBatisConnectionFactory {

	private static SqlSessionFactory sqlSessionFactory;
	private static Properties prop = new Properties();
	static {
		try {
			String resource = "config/mybatis-conf.xml";
			Reader reader = Resources.getResourceAsReader(resource);


			final String propertyPath = System.getProperty("app.home") == null ? 
					String.join(File.separator, System.getProperty("user.dir"), "config", "db.properties") 
					: String.join(File.separator, System.getProperty("app.home"), "config", "db.properties");
			Properties getProp = new Properties();
			getProp.load(new FileReader(propertyPath));
			prop.setProperty("driver", getProp.getProperty("db.driver"));
			prop.setProperty("url", getProp.getProperty("db.url"));
			prop.setProperty("username", getProp.getProperty("db.username"));
			prop.setProperty("password", getProp.getProperty("db.password"));

			if (sqlSessionFactory == null) {
				sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, prop);
			}
		}
		catch (FileNotFoundException fileNotFoundException) {
			fileNotFoundException.printStackTrace();
		}
		catch (IOException iOException) {
			iOException.printStackTrace();
		}
	}

	public static SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}
}
