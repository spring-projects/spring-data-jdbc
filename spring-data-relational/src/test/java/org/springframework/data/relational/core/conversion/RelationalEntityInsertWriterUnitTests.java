/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.data.relational.core.conversion;

import static org.assertj.core.api.Assertions.*;

import lombok.RequiredArgsConstructor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.conversion.AggregateChange.Kind;
import org.springframework.data.relational.core.conversion.DbAction.InsertRoot;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;

/**
 * Unit tests for the {@link RelationalEntityInsertWriter}
 *
 * @author Thomas Lang
 */
@RunWith(MockitoJUnitRunner.class)
public class RelationalEntityInsertWriterUnitTests {

	public static final long SOME_ENTITY_ID = 23L;
	RelationalEntityInsertWriter converter = new RelationalEntityInsertWriter(new RelationalMappingContext());

	@Test // DATAJDBC-112
	public void newEntityGetsConvertedToOneInsert() {

		SingleReferenceEntity entity = new SingleReferenceEntity(null);
		AggregateChange<SingleReferenceEntity> aggregateChange = //
				new AggregateChange(Kind.SAVE, SingleReferenceEntity.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, DbAction::getEntityType, DbActionTestSupport::extractPath,
						DbActionTestSupport::actualEntityType, DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(InsertRoot.class, SingleReferenceEntity.class, "", SingleReferenceEntity.class, false) //
				);
	}

	@Test // DATAJDBC-282
	public void existingEntityGetsNotConvertedToDeletePlusUpdate() {

		SingleReferenceEntity entity = new SingleReferenceEntity(SOME_ENTITY_ID);

		AggregateChange<SingleReferenceEntity> aggregateChange = //
				new AggregateChange(Kind.SAVE, SingleReferenceEntity.class, entity);

		converter.write(entity, aggregateChange);

		assertThat(aggregateChange.getActions()) //
				.extracting(DbAction::getClass, DbAction::getEntityType, DbActionTestSupport::extractPath,
						DbActionTestSupport::actualEntityType, DbActionTestSupport::isWithDependsOn) //
				.containsExactly( //
						tuple(InsertRoot.class, SingleReferenceEntity.class, "", SingleReferenceEntity.class, false) //
				);

	}

	@RequiredArgsConstructor
	static class SingleReferenceEntity {

		@Id final Long id;
		Element other;
		// should not trigger own Dbaction
		String name;
	}

	@RequiredArgsConstructor
	private static class Element {
		@Id final Long id;
	}
}
