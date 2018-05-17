/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jdbc.testing;

import java.util.Optional;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.DataAccessStrategy;
import org.springframework.data.jdbc.core.DefaultDataAccessStrategy;
import org.springframework.data.jdbc.core.SqlGeneratorSource;
import org.springframework.data.jdbc.core.mapping.model.ConversionCustomizer;
import org.springframework.data.jdbc.core.mapping.model.JdbcMappingContext;
import org.springframework.data.jdbc.core.mapping.model.NamingStrategy;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Infrastructure configuration for integration tests.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@Configuration
@ComponentScan // To pick up configuration classes (per activated profile)
public class TestConfiguration {

	@Autowired DataSource dataSource;
	@Autowired ApplicationEventPublisher publisher;
	@Autowired(required = false) SqlSessionFactory sqlSessionFactory;

	@Bean
	JdbcRepositoryFactory jdbcRepositoryFactory(DataAccessStrategy dataAccessStrategy) {

		NamedParameterJdbcOperations jdbcTemplate = namedParameterJdbcTemplate();

		final JdbcMappingContext context = new JdbcMappingContext(NamingStrategy.INSTANCE, jdbcTemplate, __ -> {});

		return new JdbcRepositoryFactory( //
				publisher, //
				context, //
				dataAccessStrategy //
		);
	}

	@Bean
	NamedParameterJdbcOperations namedParameterJdbcTemplate() {
		return new NamedParameterJdbcTemplate(dataSource);
	}

	@Bean
	PlatformTransactionManager transactionManager() {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	DataAccessStrategy defaultDataAccessStrategy(JdbcMappingContext context) {
		return new DefaultDataAccessStrategy(new SqlGeneratorSource(context), context);
	}

	@Bean
	JdbcMappingContext jdbcMappingContext(NamedParameterJdbcOperations template, Optional<NamingStrategy> namingStrategy,
			Optional<ConversionCustomizer> conversionCustomizer) {

		return new JdbcMappingContext( //
				namingStrategy.orElse(NamingStrategy.INSTANCE), //
				template, //
				conversionCustomizer.orElse(conversionService -> {}) //
		);
	}
}
