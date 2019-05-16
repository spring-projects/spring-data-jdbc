/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jdbc.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.EntityRowMapper;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.mybatis.support.MybatisContext;
import org.springframework.data.jdbc.mybatis.support.MybatisQuery;
import org.springframework.data.jdbc.mybatis.support.MybatisQueryMethod;
import org.springframework.data.jdbc.mybatis.support.MybatisRepositoryQuery;
import org.springframework.data.jdbc.repository.QueryMappingConfiguration;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * {@link QueryLookupStrategy} for JDBC repositories. Currently only supports annotated queries.
 *
 * @author Jens Schauder
 * @author Kazuki Shimizu
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Maciej Walkowiak
 */
class JdbcQueryLookupStrategy implements QueryLookupStrategy {

    private final ApplicationEventPublisher publisher;
    private final RelationalMappingContext context;
    private final JdbcConverter converter;
    private final DataAccessStrategy accessStrategy;
    private final QueryMappingConfiguration queryMappingConfiguration;
    private final NamedParameterJdbcOperations operations;

    private MybatisContext mybatisContent;

    /**
     * Creates a new {@link JdbcQueryLookupStrategy} for the given {@link RelationalMappingContext},
     * {@link DataAccessStrategy} and {@link QueryMappingConfiguration}.
     *
     * @param publisher                 must not be {@literal null}.
     * @param context                   must not be {@literal null}.
     * @param converter                 must not be {@literal null}.
     * @param accessStrategy            must not be {@literal null}.
     * @param queryMappingConfiguration must not be {@literal null}.
     */
    JdbcQueryLookupStrategy(ApplicationEventPublisher publisher, RelationalMappingContext context,
                            JdbcConverter converter, DataAccessStrategy accessStrategy, QueryMappingConfiguration queryMappingConfiguration,
                            NamedParameterJdbcOperations operations) {

        Assert.notNull(publisher, "Publisher must not be null!");
        Assert.notNull(context, "RelationalMappingContext must not be null!");
        Assert.notNull(converter, "RelationalConverter must not be null!");
        Assert.notNull(accessStrategy, "DataAccessStrategy must not be null!");
        Assert.notNull(queryMappingConfiguration, "RowMapperMap must not be null!");

        this.publisher = publisher;
        this.context = context;
        this.converter = converter;
        this.accessStrategy = accessStrategy;
        this.queryMappingConfiguration = queryMappingConfiguration;
        this.operations = operations;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
     */
    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata repositoryMetadata,
                                        ProjectionFactory projectionFactory, NamedQueries namedQueries) {


        if (method.isAnnotationPresent(MybatisQuery.class)) {
            return createMybatisRepositoryQuery(method, repositoryMetadata, projectionFactory);
        } else {
            JdbcQueryMethod queryMethod = new JdbcQueryMethod(method, repositoryMetadata, projectionFactory);

            RowMapper<?> mapper = queryMethod.isModifyingQuery() ? null : createMapper(queryMethod);

            return new JdbcRepositoryQuery(publisher, context, queryMethod, operations, mapper);
        }

    }

    private RepositoryQuery createMybatisRepositoryQuery(Method method, RepositoryMetadata repositoryMetadata, ProjectionFactory projectionFactory) {
        SqlSessionTemplate sqlSession = mybatisContent.getSqlSessionTemplate();
        if (sqlSession == null) {
            throw new IllegalStateException(String.format("You have annotated @MybatisQuery on method:%s ,but no org.mybatis.spring.SqlSessionTemplate provided.", method.getName()));
        }
        Object mapper = sqlSession.getMapper(method.getDeclaringClass());
        MybatisQueryMethod mybatisQueryMethod = new MybatisQueryMethod(method, repositoryMetadata, projectionFactory, mapper);
        return new MybatisRepositoryQuery(publisher, context, mybatisQueryMethod);
    }

    private RowMapper<?> createMapper(JdbcQueryMethod queryMethod) {

        Class<?> returnedObjectType = queryMethod.getReturnedObjectType();

        RelationalPersistentEntity<?> persistentEntity = context.getPersistentEntity(returnedObjectType);

        if (persistentEntity == null) {
            return SingleColumnRowMapper.newInstance(returnedObjectType, converter.getConversionService());
        }

        return determineDefaultMapper(queryMethod);
    }

    private RowMapper<?> determineDefaultMapper(JdbcQueryMethod queryMethod) {

        Class<?> domainType = queryMethod.getReturnedObjectType();
        RowMapper<?> configuredQueryMapper = queryMappingConfiguration.getRowMapper(domainType);

        if (configuredQueryMapper != null)
            return configuredQueryMapper;

        EntityRowMapper<?> defaultEntityRowMapper = new EntityRowMapper<>( //
                context.getRequiredPersistentEntity(domainType), //
                //
                converter, //
                accessStrategy);

        return defaultEntityRowMapper;
    }

    public void setMybatisContext(MybatisContext mybatisContent) {
        this.mybatisContent = mybatisContent;
    }
}
