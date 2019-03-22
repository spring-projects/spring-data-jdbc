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
package org.springframework.data.jdbc.core;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jdbc.core.PropertyPathUtils.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.convert.JdbcIdentifierBuilder;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.domain.Identifier;

/**
 * Unit tests for the {@link JdbcIdentifierBuilder}.
 *
 * @author Jens Schauder
 */
public class JdbcIdentifierBuilderUnitTests {

	JdbcMappingContext context = new JdbcMappingContext();

	@Test // DATAJDBC-326
	public void parametersWithPropertyKeysUseTheParentPropertyJdbcType() {

		Identifier identifier = JdbcIdentifierBuilder.forBackReferences(getPath("child"), "eins").build();

		assertThat(identifier.getParts()) //
				.extracting("name", "value", "targetType") //
				.containsExactly( //
				tuple("dummy_entity", "eins", UUID.class) //
		);
	}

	@Test // DATAJDBC-326
	public void qualifiersForMaps() {

		PersistentPropertyPath<RelationalPersistentProperty> path = getPath("children");

		Identifier identifier = JdbcIdentifierBuilder //
				.forBackReferences(path, "parent-eins") //
				.withQualifier(path, "map-key-eins") //
				.build();

		assertThat(identifier.getParts()) //
				.extracting("name", "value", "targetType") //
				.containsExactlyInAnyOrder( //
				tuple("dummy_entity", "parent-eins", UUID.class), //
				tuple("dummy_entity_key", "map-key-eins", String.class) //
		);
	}

	@Test // DATAJDBC-326
	public void qualifiersForLists() {

		PersistentPropertyPath<RelationalPersistentProperty> path = getPath("moreChildren");

		Identifier identifier = JdbcIdentifierBuilder //
				.forBackReferences(path, "parent-eins") //
				.withQualifier(path, "list-index-eins") //
				.build();

		assertThat(identifier.getParts()) //
				.extracting("name", "value", "targetType") //
				.containsExactlyInAnyOrder( //
				tuple("dummy_entity", "parent-eins", UUID.class), //
				tuple("dummy_entity_key", "list-index-eins", Integer.class) //
		);
	}

	@Test // DATAJDBC-326
	public void backreferenceAcrossEmbeddable() {

		Identifier identifier = JdbcIdentifierBuilder //
				.forBackReferences(getPath("embeddable.child"), "parent-eins") //
				.build();

		assertThat(identifier.getParts()) //
				.extracting("name", "value", "targetType") //
				.containsExactly( //
				tuple("embeddable", "parent-eins", UUID.class) //
		);
	}

	@NotNull
	private PersistentPropertyPath<RelationalPersistentProperty> getPath(String dotPath) {
		return toPath(dotPath, DummyEntity.class, context);
	}

	@SuppressWarnings("unused")
	static class DummyEntity {

		@Id UUID id;
		String one;
		Long two;
		Child child;

		Map<String, Child> children;

		List<Child> moreChildren;

		Embeddable embeddable;
	}

	@SuppressWarnings("unused")
	static class Embeddable {
		Child child;
	}

	static class Child {}
}
