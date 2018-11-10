/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.jdbc.repository.support;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.event.AfterLoadEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * A query to be executed based on a repository method, it's annotated SQL query and the arguments provided to the
 * method.
 *
 * @author Jens Schauder
 * @author Kazuki Shimizu
 * @author Oliver Gierke
 * @author Maciej Walkowiak
 */
class JdbcRepositoryQuery implements RepositoryQuery {

	private static final String PARAMETER_NEEDS_TO_BE_NAMED = "For queries with named parameters you need to provide names for method parameters. Use @Param for query method parameters, or when on Java 8+ use the javac flag -parameters.";

	private final ApplicationEventPublisher publisher;
	private final RelationalMappingContext context;
	private final JdbcQueryMethod queryMethod;
	private final NamedParameterJdbcOperations operations;
	private final Object mapper;

	/**
	 * Creates a new {@link JdbcRepositoryQuery} for the given {@link JdbcQueryMethod}, {@link RelationalMappingContext}
	 * and {@link RowMapper}.
	 *
	 * @param publisher must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 * @param queryMethod must not be {@literal null}.
	 * @param operations must not be {@literal null}.
	 * @param defaultRowMapper can be {@literal null} (only in case of a modifying query).
	 */
	JdbcRepositoryQuery(ApplicationEventPublisher publisher, RelationalMappingContext context, JdbcQueryMethod queryMethod, NamedParameterJdbcOperations operations,
			@Nullable Object defaultMapper) {

		Assert.notNull(publisher, "Publisher must not be null!");
		Assert.notNull(context, "Context must not be null!");
		Assert.notNull(queryMethod, "Query method must not be null!");
		Assert.notNull(operations, "NamedParameterJdbcOperations must not be null!");

		if (!queryMethod.isModifyingQuery()) {
			Assert.notNull(defaultMapper, "Mapper must not be null!");
		}

		this.publisher = publisher;
		this.context = context;
		this.queryMethod = queryMethod;
		this.operations = operations;
		
		RowMapper<?> queryRowMapper = createRowMapper(queryMethod); //use the RowMapper|ResultSetExtractor from Query annotation if set, else use default
		ResultSetExtractor<?> queryResultSetExtractor = createResultSetExtractor(queryMethod);
		if(queryRowMapper != null) {
			this.mapper = queryRowMapper;
		} else if(queryResultSetExtractor != null) {
			this.mapper = queryResultSetExtractor;
		} else this.mapper = defaultMapper;
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#execute(java.lang.Object[])
	 */
	@Override
	public Object execute(Object[] objects) {

		String query = determineQuery();
		MapSqlParameterSource parameters = bindParameters(objects);

		if (queryMethod.isModifyingQuery()) {

			int updatedCount = operations.update(query, parameters);
			Class<?> returnedObjectType = queryMethod.getReturnedObjectType();
			return (returnedObjectType == boolean.class || returnedObjectType == Boolean.class) ? updatedCount != 0
					: updatedCount;
		}

		if (queryMethod.isCollectionQuery() || queryMethod.isStreamQuery()) {
			List<?> result = null;
			if(this.mapper instanceof ResultSetExtractor) {
				result = (List<?>) operations.query(query, parameters, (ResultSetExtractor)this.mapper);
			} else {
				result = operations.query(query, parameters, (RowMapper)this.mapper);
			}
			publishAfterLoad(result);
			return result;
		}

		try {
			Object result = null;
			if(this.mapper instanceof ResultSetExtractor) {
				result =  operations.query(query,parameters, (ResultSetExtractor)this.mapper);
			} else {
				result = operations.queryForObject(query, parameters, (RowMapper)this.mapper);
			}
			publishAfterLoad(result);
			return result;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#getQueryMethod()
	 */
	@Override
	public JdbcQueryMethod getQueryMethod() {
		return queryMethod;
	}

	private String determineQuery() {

		String query = queryMethod.getAnnotatedQuery();

		if (StringUtils.isEmpty(query)) {
			throw new IllegalStateException(String.format("No query specified on %s", queryMethod.getName()));
		}

		return query;
	}

	private MapSqlParameterSource bindParameters(Object[] objects) {

		MapSqlParameterSource parameters = new MapSqlParameterSource();

		queryMethod.getParameters().getBindableParameters().forEach(p -> {

			String parameterName = p.getName().orElseThrow(() -> new IllegalStateException(PARAMETER_NEEDS_TO_BE_NAMED));
			parameters.addValue(parameterName, objects[p.getIndex()]);
		});

		return parameters;
	}

	@Nullable
	private static RowMapper<?> createRowMapper(JdbcQueryMethod queryMethod) {

		Class<?> rowMapperClass = queryMethod.getRowMapperClass();

		return rowMapperClass == null || rowMapperClass == RowMapper.class //
				? null //
				: (RowMapper<?>) BeanUtils.instantiateClass(rowMapperClass);
	}
	
	@Nullable
	private static ResultSetExtractor<?> createResultSetExtractor(JdbcQueryMethod queryMethod) {

		Class<?> resultSetExtractorClass = queryMethod.getResultSetExtractorClass();

		if(resultSetExtractorClass == null || resultSetExtractorClass == ResultSetExtractor.class) { //if it's the default - return null
			return null;
		} else {
			return (ResultSetExtractor<?>) BeanUtils.instantiateClass(resultSetExtractorClass);
		}
				
	}

	private <T> void publishAfterLoad(Iterable<T> all) {

		for (T e : all) {
			publishAfterLoad(e);
		}
	}

	private <T> void publishAfterLoad(@Nullable T entity) {

		if (entity != null && context.hasPersistentEntityFor(entity.getClass())) {

			RelationalPersistentEntity<?> e = context.getRequiredPersistentEntity(entity.getClass());
			Object identifier = e.getIdentifierAccessor(entity)
					     .getIdentifier();

			if (identifier != null) {
				publisher.publishEvent(new AfterLoadEvent(Identifier.of(identifier), entity));
			}
		}

	}
}