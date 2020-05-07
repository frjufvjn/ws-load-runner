package com.hansol.pom.db.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hansol.ismon.data.MyBatisConnectionFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @author PJW
 *
 */
public class QueryServiceImp implements QueryServices {

	private final Logger logger = LogManager.getLogger(QueryServiceImp.class);

	/**
	 * @description sql name property name
	 */
	private final String SQL_NAME = "sqlName";

	/**
	 * @description batch sql param key
	 * */
	private final String BATCH_PARAM_KEY = "arr";

	/**
	 * @description default sql query
	 */
	private final String DEFAULT_SQL = "SELECT 1";

	/**
	 * @description Get query and bind param from request parameter
	 * @param reqData
	 * @return map {sqlString, sqlParam}
	 * */
	@Override
	public Map<String, Object> getQuery(Map<String, Object> reqData) throws Exception {

		Map<String,Object> result = new LinkedHashMap<String,Object>();
		String queryString = DEFAULT_SQL;
		JsonArray jsonParam = new JsonArray();

		// MyBatis session open
		try ( final SqlSession sqlsession = MyBatisConnectionFactory.getSqlSessionFactory().openSession(); ) {
			String sqlName = reqData.get(SQL_NAME).toString();
			reqData.remove(SQL_NAME);

			// Get BoudSql
			BoundSql boundSql = sqlsession.getConfiguration()
					.getMappedStatement(sqlName) // MyBatis SQL ID
					.getSqlSource()
					.getBoundSql(reqData)
					;

			queryString = boundSql.getSql();

			// get Parameter
			List<ParameterMapping> paramMapping = boundSql.getParameterMappings();
			for ( ParameterMapping mapping : paramMapping ) {
				String key = mapping.getProperty();
				Object value = ((Map<String,Object>)reqData).get(key);

				jsonParam.add(value);
			}

			result.put("sql", queryString);
			result.put("sqlParam", jsonParam);
		}

		return result;
	}

	/**
	 * @description Get query and bind param from routingContext
	 * @param ctx
	 * @return map {sqlString, sqlParam}
	 * */
	@Override
	public Map<String, Object> getQuery(RoutingContext ctx) throws Exception {
		return getQueryFromParam(getParamToMapFromCtx(ctx));
	}


	/**
	 * @description Get query and bind param from jsonobject
	 * @param reqData
	 * @return map {sqlString, sqlParam}
	 * */
	@Override
	public Map<String, Object> getQuery(JsonObject params) throws Exception {
		return getQueryFromParam(getParamToMap(params));
	}

	/**
	 * @description Common method (get query and bind param from request map)
	 * @param reqData
	 * @return map {sqlString, sqlParam}
	 */
	private Map<String, Object> getQueryFromParam(Map<String,Object> reqData) {
		Map<String,Object> result = new LinkedHashMap<String,Object>();
		String queryString = DEFAULT_SQL;
		JsonArray jsonParam = new JsonArray();

		// sqlname validation
		if ( !reqData.containsKey(SQL_NAME) ) {
			throw new IllegalArgumentException("The sqlName parameter is empty or the service can not be found in the service list (mybatis mapper).");
		}

		// MyBatis open session
		try ( final SqlSession sqlsession = MyBatisConnectionFactory.getSqlSessionFactory().openSession(); ) {
			String sqlName = reqData.get(SQL_NAME).toString();
			reqData.remove(SQL_NAME);

			// Get BoudSql
			BoundSql boundSql = sqlsession.getConfiguration()
					.getMappedStatement(sqlName) // MyBatis SQL ID
					.getSqlSource()
					.getBoundSql(reqData)
					;

			queryString = boundSql.getSql();
//			logger.info("[QUERY INFO-----------------------------------------------------------------");
//			logger.info("\tquery : {}", queryString);

			// get Parameter
			List<ParameterMapping> paramMapping = boundSql.getParameterMappings();

			// sql bind parameter validation
			if ( paramMapping.size() != reqData.size() ) {
				throw new IllegalArgumentException("The number of sql parameter is insufficient or exceeded.");
			}

//			logger.info("\tquery param :");
			for ( ParameterMapping mapping : paramMapping ) {
				String key = mapping.getProperty();
				Object value = ((Map<String,Object>)reqData).get(key);

				if ( !reqData.containsKey(key) ) {
					throw new IllegalArgumentException("The requested sql parameter does not match the parameter of mybatis mapper.");
				}

				logger.debug("\t\tkey: {}, value: {}", key, value);

				jsonParam.add(value);
			}
//			logger.info("QUERY INFO-----------------------------------------------------------------]");
			result.put("sql", queryString);
			result.put("sqlParam", jsonParam);
		}

		return result;
	}

	/**
	 * @description get query and batch params from routingContext
	 * @param ctx
	 * @return map {sqlString, batchParam}
	 * */
	@Override
	public Map<String, Object> getQueryWithoutParam(RoutingContext ctx) throws Exception {

		if ( !ctx.getBodyAsJson().containsKey(SQL_NAME) ) {
			throw new IllegalArgumentException("The sqlName parameter is empty or the service can not be found in the service list (mybatis mapper).");
		}

		String sqlName = ctx.getBodyAsJson().getString(SQL_NAME);

		JsonArray arr = ctx.getBodyAsJson().getJsonArray("arr");
		if ( arr.size() <= 0 ) {
			throw new IllegalArgumentException("array size is zero");
		}

		Map<String,Object> result = new LinkedHashMap<String,Object>();
		String queryString = DEFAULT_SQL;

		Map<String,Object> reqData = getListParamToMap(arr);

		try ( final SqlSession sqlsession = MyBatisConnectionFactory.getSqlSessionFactory().openSession(); ) {

			// Get BoudSql
			BoundSql boundSql = sqlsession.getConfiguration()
					.getMappedStatement(sqlName) // MyBatis SQL ID
					.getSqlSource()
					.getBoundSql(reqData)
					;

			queryString = boundSql.getSql();

			result.put("sql", queryString);
			result.put("batchParam", getBatchParam(ctx));
		}

		return result;
	}
	
	@Override
	public Map<String, Object> getQueryBatch(JsonObject params) throws Exception {
		if (!params.containsKey(SQL_NAME)) {
			throw new IllegalArgumentException("The sqlName parameter is empty or the service can not be found in the service list (mybatis mapper).");
		}
		String sqlName = params.getString(SQL_NAME);
		JsonArray arr = params.getJsonArray("arr");
		if ( arr.size() <= 0 ) {
			throw new IllegalArgumentException("array size is zero");
		}
		
		Map<String,Object> result = new LinkedHashMap<String,Object>();
		String queryString = DEFAULT_SQL;
		
		Map<String,Object> reqData = getListParamToMap(arr);

		try ( final SqlSession sqlsession = MyBatisConnectionFactory.getSqlSessionFactory().openSession(); ) {

			// Get BoudSql
			BoundSql boundSql = sqlsession.getConfiguration()
					.getMappedStatement(sqlName) // MyBatis SQL ID
					.getSqlSource()
					.getBoundSql(reqData)
					;

			queryString = boundSql.getSql();
			
			result.put("sql", queryString);
			result.put("batchParam", getBatchParam(arr));
		}

		return result;
	}

	/**
	 * @param ctx
	 * @return
	 */
	private Map<String,Object> getParamToMapFromCtx(RoutingContext ctx) {
		Map<String,Object> map = new LinkedHashMap<String,Object>();
		ctx.getBodyAsJson().forEach(param -> {
			map.put(param.getKey(), param.getValue());
		});
		return map;
	}

	private Map<String,Object> getParamToMap(JsonObject params) {
		Map<String,Object> map = new LinkedHashMap<String,Object>();
		params.forEach(param -> {
			map.put(param.getKey(), param.getValue());
		});
		return map;
	}

	/**
	 * @param arr
	 * @return
	 */
	private Map<String,Object> getListParamToMap(JsonArray arr) {
		// get only first row
		JsonObject firstObjFromArr = arr.getJsonObject(0);
		Map<String,Object> map = new LinkedHashMap<String,Object>();
		firstObjFromArr.forEach(param -> {
			map.put(param.getKey(), param.getValue());
		});
		return map;
	}


	/**
	 * @param ctx
	 */
	private List<JsonArray> getBatchParam(RoutingContext ctx) {
		List<JsonArray> batch = new ArrayList<JsonArray>();

		ctx.getBodyAsJson().getJsonArray(BATCH_PARAM_KEY)
		.stream().forEach(row -> {
			JsonObject obj = (JsonObject) row;
			JsonArray innerArr = new JsonArray();
			obj.fieldNames().forEach(key -> {
				innerArr.add(obj.getValue(key));
			});
			batch.add(innerArr);
		});
		return batch;
	}
	
	private List<JsonArray> getBatchParam(JsonArray arr) {
		List<JsonArray> batch = new ArrayList<JsonArray>();
		arr.stream().forEach(row -> {
			JsonObject obj = (JsonObject) row;
			JsonArray innerArr = new JsonArray();
			obj.fieldNames().forEach(key -> {
				innerArr.add(obj.getValue(key));
			});
			batch.add(innerArr);
		});
		return batch;
	}
}
