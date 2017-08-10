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

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.mapping.model.DefaultNamingStrategy;
import org.springframework.data.jdbc.mapping.model.JdbcMappingContext;
import org.springframework.data.jdbc.mapping.model.JdbcPersistentEntity;
import org.springframework.data.jdbc.mapping.model.NamingStrategy;
import org.springframework.data.mapping.PropertyPath;

/**
 * Unit tests for the {@link SqlGenerator}.
 * 
 * @author Jens Schauder
 */
public class SqlGeneratorUnitTests {

	private NamingStrategy namingStrategy;
	private JdbcMappingContext context;
	private JdbcPersistentEntity<?> persistentEntity;
	private SqlGenerator sqlGenerator;

	@Before
	public void setUp() {

		this.namingStrategy = new DefaultNamingStrategy();
		this.context = new JdbcMappingContext(namingStrategy);
		this.persistentEntity = context.getRequiredPersistentEntity(DummyEntity.class);
		this.sqlGenerator = new SqlGenerator(context, persistentEntity, new SqlGeneratorSource(context));
	}

	@Test // DATAJDBC-112
	public void findOne() {

		String sql = sqlGenerator.getFindOne();

		SoftAssertions softAssertions = new SoftAssertions();
		softAssertions.assertThat(sql) //
				.startsWith("SELECT") //
				.contains("DummyEntity.id AS id,") //
				.contains("DummyEntity.name AS name,") //
				.contains("ref.l1id AS ref_l1id") //
				.contains("ref.content AS ref_content").contains(" FROM DummyEntity");
		softAssertions.assertAll();
	}

	@Test // DATAJDBC-112
	public void cascadingDeleteFirstLevel() {

		String sql = sqlGenerator.createDeleteByPath(PropertyPath.from("ref", DummyEntity.class));

		assertThat(sql).isEqualTo("DELETE FROM ReferencedEntity WHERE DummyEntity = :rootId");
	}

	@Test // DATAJDBC-112
	public void cascadingDeleteAllSecondLevel() {

		String sql = sqlGenerator.createDeleteByPath(PropertyPath.from("ref.further", DummyEntity.class));

		assertThat(sql).isEqualTo(
				"DELETE FROM SecondLevelReferencedEntity WHERE ReferencedEntity IN (SELECT l1id FROM ReferencedEntity WHERE DummyEntity = :rootId)");
	}

	@Test // DATAJDBC-112
	public void deleteAll() {

		String sql = sqlGenerator.createDeleteAllSql(null);

		assertThat(sql).isEqualTo("DELETE FROM DummyEntity");
	}

	@Test // DATAJDBC-112
	public void cascadingDeleteAllFirstLevel() {

		String sql = sqlGenerator.createDeleteAllSql(PropertyPath.from("ref", DummyEntity.class));

		assertThat(sql).isEqualTo("DELETE FROM ReferencedEntity WHERE DummyEntity IS NOT NULL");
	}

	@Test // DATAJDBC-112
	public void cascadingDeleteSecondLevel() {

		String sql = sqlGenerator.createDeleteAllSql(PropertyPath.from("ref.further", DummyEntity.class));

		assertThat(sql).isEqualTo(
				"DELETE FROM SecondLevelReferencedEntity WHERE ReferencedEntity IN (SELECT l1id FROM ReferencedEntity WHERE DummyEntity IS NOT NULL)");
	}

	static class DummyEntity {

		@Id Long id;
		String name;
		ReferencedEntity ref;
	}

	static class ReferencedEntity {

		@Id Long l1id;
		String content;
		SecondLevelReferencedEntity further;
	}

	static class SecondLevelReferencedEntity {

		@Id Long l2id;
		String something;
	}
}
