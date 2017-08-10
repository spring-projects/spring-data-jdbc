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
package org.springframework.data.jdbc.mapping.model;

import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.ClassUtils;

/**
 * Meta data about a property to be used by repository implementations.
 *
 * @author Jens Schauder
 * @since 2.0
 */
public class BasicJdbcPersistentProperty extends AnnotationBasedPersistentProperty<JdbcPersistentProperty>
		implements JdbcPersistentProperty {

	private static final Map<Class<?>, Class<?>> javaToDbType = new LinkedHashMap<>();
	private final JdbcMappingContext context;

	static {
		javaToDbType.put(Enum.class, String.class);
		javaToDbType.put(ZonedDateTime.class, String.class);
		javaToDbType.put(Temporal.class, Date.class);
	}

	/**
	 * Creates a new {@link AnnotationBasedPersistentProperty}.
	 * 
	 * @param property must not be {@literal null}.
	 * @param owner must not be {@literal null}.
	 * @param simpleTypeHolder must not be {@literal null}.
	 * @param context
	 */
	public BasicJdbcPersistentProperty(Property property, PersistentEntity<?, JdbcPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder, JdbcMappingContext context) {
		super(property, owner, simpleTypeHolder);
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#createAssociation()
	 */
	@Override
	protected Association<JdbcPersistentProperty> createAssociation() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jdbc.mapping.model.JdbcPersistentProperty#getColumnName()
	 */
	public String getColumnName() {
		return this.context.getNamingStrategy().getColumnName(this);
	}

	/**
	 * The type to be used to store this property in the database.
	 *
	 * @return a {@link Class} that is suitable for usage with JDBC drivers
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Class getColumnType() {

		Class columnType = columnTypeIfEntity(getType());

		return columnType == null ? columnTypeForNonEntity(getType()) : columnType;
	}

	private Class columnTypeIfEntity(Class type) {

		JdbcPersistentEntity<?> persistentEntity = context.getPersistentEntity(type);

		if (persistentEntity == null) {
			return null;
		}

		JdbcPersistentProperty idProperty = persistentEntity.getIdProperty();

		if (idProperty == null) {
			return null;
		}
		return idProperty.getColumnType();
	}

	private Class columnTypeForNonEntity(Class type) {

		return javaToDbType.entrySet().stream() //
				.filter(e -> e.getKey().isAssignableFrom(type)) //
				.map(e -> (Class) e.getValue()) //
				.findFirst() //
				.orElseGet(() -> ClassUtils.resolvePrimitiveIfNecessary(type));
	}
}
