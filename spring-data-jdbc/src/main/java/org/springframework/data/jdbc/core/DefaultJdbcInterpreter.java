/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.data.jdbc.core;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Map;

import org.springframework.data.jdbc.core.convert.JdbcIdentifierBuilder;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.conversion.DbAction;
import org.springframework.data.relational.core.conversion.DbAction.Delete;
import org.springframework.data.relational.core.conversion.DbAction.DeleteAll;
import org.springframework.data.relational.core.conversion.DbAction.DeleteAllRoot;
import org.springframework.data.relational.core.conversion.DbAction.DeleteRoot;
import org.springframework.data.relational.core.conversion.DbAction.Insert;
import org.springframework.data.relational.core.conversion.DbAction.InsertRoot;
import org.springframework.data.relational.core.conversion.DbAction.Merge;
import org.springframework.data.relational.core.conversion.DbAction.Update;
import org.springframework.data.relational.core.conversion.DbAction.UpdateRoot;
import org.springframework.data.relational.core.conversion.Interpreter;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.domain.Identifier;
import org.springframework.lang.Nullable;

/**
 * {@link Interpreter} for {@link DbAction}s using a {@link DataAccessStrategy} for performing actual database
 * interactions.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 */
@RequiredArgsConstructor
class DefaultJdbcInterpreter implements Interpreter {

	private final RelationalMappingContext context;
	private final DataAccessStrategy accessStrategy;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.conversion.Interpreter#interpret(org.springframework.data.relational.core.conversion.DbAction.Insert)
	 */
	@Override
	public <T> void interpret(Insert<T> insert) {

		Object id = accessStrategy.insert(insert.getEntity(), insert.getEntityType(), getParentKeys(insert));

		insert.setGeneratedId(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.conversion.Interpreter#interpret(org.springframework.data.relational.core.conversion.DbAction.InsertRoot)
	 */
	@Override
	public <T> void interpret(InsertRoot<T> insert) {

		Object id = accessStrategy.insert(insert.getEntity(), insert.getEntityType(), Collections.emptyMap());
		insert.setGeneratedId(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.conversion.Interpreter#interpret(org.springframework.data.relational.core.conversion.DbAction.Update)
	 */
	@Override
	public <T> void interpret(Update<T> update) {
		accessStrategy.update(update.getEntity(), update.getEntityType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.conversion.Interpreter#interpret(org.springframework.data.relational.core.conversion.DbAction.UpdateRoot)
	 */
	@Override
	public <T> void interpret(UpdateRoot<T> update) {
		accessStrategy.update(update.getEntity(), update.getEntityType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.conversion.Interpreter#interpret(org.springframework.data.relational.core.conversion.DbAction.Merge)
	 */
	@Override
	public <T> void interpret(Merge<T> merge) {

		// temporary implementation
		if (!accessStrategy.update(merge.getEntity(), merge.getEntityType())) {
			accessStrategy.insert(merge.getEntity(), merge.getEntityType(), getParentKeys(merge));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.conversion.Interpreter#interpret(org.springframework.data.relational.core.conversion.DbAction.Delete)
	 */
	@Override
	public <T> void interpret(Delete<T> delete) {
		accessStrategy.delete(delete.getRootId(), delete.getPropertyPath());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.conversion.Interpreter#interpret(org.springframework.data.relational.core.conversion.DbAction.DeleteRoot)
	 */
	@Override
	public <T> void interpret(DeleteRoot<T> delete) {
		accessStrategy.delete(delete.getRootId(), delete.getEntityType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.conversion.Interpreter#interpret(org.springframework.data.relational.core.conversion.DbAction.DeleteAll)
	 */
	@Override
	public <T> void interpret(DeleteAll<T> delete) {
		accessStrategy.deleteAll(delete.getPropertyPath());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.conversion.Interpreter#interpret(org.springframework.data.relational.core.conversion.DbAction.DeleteAllRoot)
	 */
	@Override
	public <T> void interpret(DeleteAllRoot<T> deleteAllRoot) {
		accessStrategy.deleteAll(deleteAllRoot.getEntityType());
	}

	private Identifier getParentKeys(DbAction.WithDependingOn<?> action) {

		DbAction.WithEntity<?> dependingOn = action.getDependingOn();

		RelationalPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(dependingOn.getEntityType());

		Object id = getIdFromEntityDependingOn(dependingOn, persistentEntity);
		JdbcIdentifierBuilder identifier = JdbcIdentifierBuilder //
				.forBackReferences(action.getPropertyPath(), id);

		for (Map.Entry<PersistentPropertyPath<RelationalPersistentProperty>, Object> qualifier : action.getQualifiers()
				.entrySet()) {
			identifier = identifier.withQualifier(qualifier.getKey(), qualifier.getValue());
		}

		return identifier.build();
	}

	@Nullable
	private Object getIdFromEntityDependingOn(DbAction.WithEntity<?> dependingOn,
			RelationalPersistentEntity<?> persistentEntity) {

		Object entity = dependingOn.getEntity();

		if (dependingOn instanceof DbAction.WithGeneratedId) {

			Object generatedId = ((DbAction.WithGeneratedId<?>) dependingOn).getGeneratedId();

			if (generatedId != null) {
				return generatedId;
			}
		}

		return persistentEntity.getIdentifierAccessor(entity).getIdentifier();
	}

}
