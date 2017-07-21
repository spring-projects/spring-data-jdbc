/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.jdbc.core;

import java.util.Map;

/**
 * Specifies a operations one can perform on a database, based on an <em>Domain Type</em>.
 *
 * @author Jens Schauder
 */
public interface JdbcEntityOperations {

	<T> void insert(T instance, Class<T> domainType, Map<String, Object> additionalParameter);

	<T> void update(T instance, Class<T> domainType);

	<T> void deleteById(Object id, Class<T> domainType);

	<T> void delete(T entity, Class<T> domainType);

	long count(Class<?> domainType);

	<T> T findById(Object id, Class<T> domainType);

	<T> Iterable<T> findAllById(Iterable<?> ids, Class<T> domainType);

	<T> Iterable<T> findAll(Class<T> domainType);

	<T> boolean existsById(Object id, Class<T> domainType);

	void deleteAll(Class<?> domainType);

	<T> void save(T instance, Class<T> domainType);
}
