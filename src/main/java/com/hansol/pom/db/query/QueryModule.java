package com.hansol.pom.db.query;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class QueryModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(QueryServices.class).to(QueryServiceImp.class)
		.in(Scopes.SINGLETON);
	}
}
