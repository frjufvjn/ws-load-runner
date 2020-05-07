package com.hansol.pom.db.sql;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class SqlInnerModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(SqlInnerServices.class).in(Scopes.SINGLETON);
	}
}
