/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.jdbc.core.convert;

import lombok.RequiredArgsConstructor;

import java.sql.ResultSet;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.convert.ObjectPath;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor
public class NewJdbcConverterImpl implements NewJdbcConverter {

	private static final ObjectPath<RelationalPersistentEntity<?>> ROOT_PATH = //
			ObjectPath.init(RelationalPersistentEntity::getTableName);

	private final RelationalMappingContext context;
	private final NewRelationResolver relationResolver;
	private final EntityInstantiators instantiators;
	private final CustomConversions conversions = new JdbcCustomConversions();
	private final ConversionService conversionService = new DefaultConversionService();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.EntityReader#read(java.lang.Class, java.lang.Object)
	 */
	@Override
	public <R> R read(Class<R> type, ResultSet source) {
		return read(ClassTypeInformation.from(type), ResultSetAccessor.of(source));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jdbc.core.convert.NewJdbcConverter#getConversionService()
	 */
	@Override
	public ConversionService getConversionService() {
		return conversionService;
	}

	private <R> R read(TypeInformation<R> type, ResultSetAccessor source) {
		return read(type, source, ROOT_PATH);
	}

	@SuppressWarnings("unchecked")
	private <R> R read(TypeInformation<R> type, ResultSetAccessor source,
			ObjectPath<RelationalPersistentEntity<?>> path) {

		RelationalPersistentEntity<R> entity = (RelationalPersistentEntity<R>) context.getRequiredPersistentEntity(type);

		return read(entity, source, path);
	}

	private <R extends Object> R read(RelationalPersistentEntity<R> entity, ResultSetAccessor source,
			ObjectPath<RelationalPersistentEntity<?>> path) {

		EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
		ResultSetPropertyValueProvider delegate = new ResultSetPropertyValueProvider(source, path);

		R bean = instantiator.createInstance(entity, getParameterProvider(entity, delegate, path));

		if (entity.requiresPropertyPopulation()) {
			populateProperties(entity, source, path, bean);
		}

		return bean;
	}

	private <S, E extends RelationalPersistentEntity<S>> S populateProperties(RelationalPersistentEntity<S> entity,
			ResultSetAccessor resultSet, ObjectPath<RelationalPersistentEntity<?>> path, S instance) {

		PersistentPropertyAccessor<S> accessor = new ConvertingPropertyAccessor<>(entity.getPropertyAccessor(instance),
				conversionService);

		// Make sure id property is set before all other properties

		IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(instance);
		Object rawId = identifierAccessor.getIdentifier();
		rawId = rawId == null ? readAndPopulateIdentifier(accessor, resultSet, entity, path) : rawId;

		ObjectPath<RelationalPersistentEntity<?>> currentPath = path.push(accessor.getBean(), entity, rawId);

		readProperties(entity, accessor, resultSet, currentPath);

		return accessor.getBean();
	}

	private void readProperties(RelationalPersistentEntity<?> entity, //
			PersistentPropertyAccessor<?> accessor, //
			ResultSetAccessor resultSet, //
			ObjectPath<RelationalPersistentEntity<?>> path) {

		for (RelationalPersistentProperty property : entity) {

			if (property.isAssociation() && !entity.isConstructorArgument(property)) {

				// handle associations;
				continue;
			}

			// We skip the id property since it was already set

			if (entity.isIdProperty(property)) {
				continue;
			}

			if (entity.isConstructorArgument(property)) {
				continue;
			}

			if (property.isCollectionLike()) {

				accessor.setProperty(property, readCollection(property, resultSet, path));
				continue;
			}

			ResultSetPropertyValueProvider valueProvider = new ResultSetPropertyValueProvider(resultSet, path);

			accessor.setProperty(property, valueProvider.getPropertyValue(property));
		}
	}

	/**
	 * @param property
	 * @param resultSet
	 * @param path
	 * @return
	 */
	private Object readCollection(RelationalPersistentProperty property, ResultSetAccessor resultSet,
			ObjectPath<RelationalPersistentEntity<?>> path) {

		return relationResolver.findAllByPath(path, property);
	}

	/**
	 * Reads the identifier from either the bean backing the {@link PersistentPropertyAccessor} or the source document in
	 * case the identifier has not be populated yet. In this case the identifier is set on the bean for further reference.
	 *
	 * @param accessor must not be {@literal null}.
	 * @param document must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @param path
	 * @param evaluator
	 * @return
	 */
	private <S> Object readAndPopulateIdentifier(PersistentPropertyAccessor<?> accessor, ResultSetAccessor resultSet,
			RelationalPersistentEntity<?> entity, ObjectPath<RelationalPersistentEntity<?>> path) {

		Object rawId = resultSet.getValue(entity.getIdProperty());

		if (rawId == null) {
			return rawId;
		}

		RelationalPersistentProperty idProperty = entity.getRequiredIdProperty();

		if (idProperty.isImmutable() && entity.isConstructorArgument(idProperty)) {
			return rawId;
		}

		accessor.setProperty(idProperty, postProcessValue(rawId, idProperty.getTypeInformation(), path));

		return rawId;
	}

	private <S> ParameterValueProvider<RelationalPersistentProperty> getParameterProvider(
			RelationalPersistentEntity<S> entity, //
			PropertyValueProvider<RelationalPersistentProperty> valueProvider, //
			ObjectPath<RelationalPersistentEntity<?>> path) {

		return new PersistentEntityParameterValueProvider<>(entity, valueProvider, path.getCurrentObject());
	}

	/**
	 * Checks whether we have a custom conversion for the given simple object. Converts the given value if so, applies
	 * {@link Enum} handling or returns the value as is.
	 *
	 * @param value
	 * @param target must not be {@literal null}.
	 * @return
	 */
	@Nullable
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getPotentiallyConvertedSimpleRead(@Nullable Object value, @Nullable Class<?> target) {

		if (value == null || target == null || ClassUtils.isAssignableValue(target, value)) {
			return value;
		}

		if (conversions.hasCustomReadTarget(value.getClass(), target)) {
			return conversionService.convert(value, target);
		}

		if (Enum.class.isAssignableFrom(target)) {
			return Enum.valueOf((Class<Enum>) target, value.toString());
		}

		return conversionService.convert(value, target);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	<T> T postProcessValue(Object value, TypeInformation<?> type, ObjectPath<RelationalPersistentEntity<?>> path) {

		Class<?> rawType = type.getType();

		if (conversions.hasCustomReadTarget(value.getClass(), rawType)) {
			return (T) conversionService.convert(value, rawType);
		} else {
			return (T) getPotentiallyConvertedSimpleRead(value, rawType);
		}
	}

	@RequiredArgsConstructor
	class ResultSetPropertyValueProvider implements PropertyValueProvider<RelationalPersistentProperty> {

		private final ResultSetAccessor resultSet;
		private final ObjectPath<RelationalPersistentEntity<?>> path;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.model.PropertyValueProvider#getPropertyValue(org.springframework.data.mapping.PersistentProperty)
		 */
		@Override
		@SuppressWarnings("unchecked")
		public <T> T getPropertyValue(RelationalPersistentProperty property) {

			if (property.isCollectionLike()) {
				return (T) readCollection(property, resultSet, path);
			}

			if (property.isEmbedded() || property.isEntity()) {
				return (T) read(property.getTypeInformation(), resultSet.withContext(property, path), path);
			}

			return (T) postProcessValue(resultSet.getValue(property), property.getTypeInformation(), path);
		}
	}
}
