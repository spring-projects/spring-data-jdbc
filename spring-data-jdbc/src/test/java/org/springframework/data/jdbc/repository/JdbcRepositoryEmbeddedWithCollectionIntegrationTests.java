/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.data.jdbc.repository;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

import lombok.Data;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactory;
import org.springframework.data.jdbc.testing.TestConfiguration;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Very simple use cases for creation and usage of JdbcRepositories with test {@link Embedded} annotation in Entities.
 *
 * @author Bastian Wilhelm
 */
@ContextConfiguration
@Transactional
public class JdbcRepositoryEmbeddedWithCollectionIntegrationTests {

	@Configuration
	@Import(TestConfiguration.class)
	static class Config {

		@Autowired JdbcRepositoryFactory factory;

		@Bean
		Class<?> testClass() {
			return JdbcRepositoryEmbeddedWithCollectionIntegrationTests.class;
		}

		@Bean
		DummyEntityRepository dummyEntityRepository() {
			return factory.getRepository(DummyEntityRepository.class);
		}

	}

	@ClassRule public static final SpringClassRule classRule = new SpringClassRule();
	@Rule public SpringMethodRule methodRule = new SpringMethodRule();

	@Autowired NamedParameterJdbcTemplate template;
	@Autowired DummyEntityRepository repository;

	@Test // DATAJDBC-111
	public void savesAnEntity() {

		DummyEntity entity = repository.save(createDummyEntity());

		assertThat(JdbcTestUtils.countRowsInTableWhere((JdbcTemplate) template.getJdbcOperations(), "dummy_entity",
				"id = " + entity.getId())).isEqualTo(1);

		assertThat(JdbcTestUtils.countRowsInTableWhere((JdbcTemplate) template.getJdbcOperations(), "dummy_entity2",
				"id = " + entity.getId())).isEqualTo(2);
	}

	@Test // DATAJDBC-111
	public void saveAndLoadAnEntity() {

		DummyEntity entity = repository.save(createDummyEntity());

		assertThat(repository.findById(entity.getId())).hasValueSatisfying(it -> {
			assertThat(it.getId()).isEqualTo(entity.getId());
			assertThat(it.getEmbeddable().getTest()).isEqualTo(entity.getEmbeddable().getTest());
			assertThat(it.getEmbeddable().getList().size()).isEqualTo(entity.getEmbeddable().getList().size());
			assertThat(it.getEmbeddable().getList().get(0).getTest()).isEqualTo(entity.getEmbeddable().getList().get(0).getTest());
			assertThat(it.getEmbeddable().getList().get(1).getTest()).isEqualTo(entity.getEmbeddable().getList().get(1).getTest());
		});
	}

	@Test // DATAJDBC-111
	public void findAllFindsAllEntities() {

		DummyEntity entity = repository.save(createDummyEntity());
		DummyEntity other = repository.save(createDummyEntity());

		Iterable<DummyEntity> all = repository.findAll();

		assertThat(all)//
				.extracting(DummyEntity::getId)//
				.containsExactlyInAnyOrder(entity.getId(), other.getId());
	}

	@Test // DATAJDBC-111
	public void findByIdReturnsEmptyWhenNoneFound() {

		// NOT saving anything, so DB is empty
		assertThat(repository.findById(-1L)).isEmpty();
	}

	@Test // DATAJDBC-111
	public void update() {

		DummyEntity entity = repository.save(createDummyEntity());

		entity.getEmbeddable().setTest("something else");
		entity.getEmbeddable().getList().get(0).setTest("another");
		DummyEntity saved = repository.save(entity);

		assertThat(repository.findById(entity.getId())).hasValueSatisfying(it -> {
			assertThat(it.getId()).isEqualTo(saved.getId());
			assertThat(it.getEmbeddable().getTest()).isEqualTo(saved.getEmbeddable().getTest());
			assertThat(it.getEmbeddable().getList().size()).isEqualTo(saved.getEmbeddable().getList().size());
			assertThat(it.getEmbeddable().getList().get(0).getTest()).isEqualTo(saved.getEmbeddable().getList().get(0).getTest());
			assertThat(it.getEmbeddable().getList().get(1).getTest()).isEqualTo(saved.getEmbeddable().getList().get(1).getTest());
		});
	}

	@Test // DATAJDBC-111
	public void updateMany() {

		DummyEntity entity = repository.save(createDummyEntity());
		DummyEntity other = repository.save(createDummyEntity());

		entity.getEmbeddable().setTest("something else");
		other.getEmbeddable().setTest("others Name");

		entity.getEmbeddable().getList().get(0).setTest("else");
		other.getEmbeddable().getList().get(0).setTest("Name");

		repository.saveAll(asList(entity, other));

		assertThat(repository.findAll()) //
				.extracting(d -> d.getEmbeddable().getTest()) //
				.containsExactlyInAnyOrder(entity.getEmbeddable().getTest(), other.getEmbeddable().getTest());

		assertThat(repository.findAll()) //
				.extracting(d -> d.getEmbeddable().getList().get(0).getTest()) //
				.containsExactlyInAnyOrder(entity.getEmbeddable().getList().get(0).getTest(), other.getEmbeddable().getList().get(0).getTest());
	}

	@Test // DATAJDBC-111
	public void deleteById() {

		DummyEntity one = repository.save(createDummyEntity());
		DummyEntity two = repository.save(createDummyEntity());
		DummyEntity three = repository.save(createDummyEntity());

		repository.deleteById(two.getId());

		assertThat(repository.findAll()) //
				.extracting(DummyEntity::getId) //
				.containsExactlyInAnyOrder(one.getId(), three.getId());
	}

	@Test // DATAJDBC-111
	public void deleteByEntity() {
		DummyEntity one = repository.save(createDummyEntity());
		DummyEntity two = repository.save(createDummyEntity());
		DummyEntity three = repository.save(createDummyEntity());

		repository.delete(one);

		assertThat(repository.findAll()) //
				.extracting(DummyEntity::getId) //
				.containsExactlyInAnyOrder(two.getId(), three.getId());
	}

	@Test // DATAJDBC-111
	public void deleteByList() {

		DummyEntity one = repository.save(createDummyEntity());
		DummyEntity two = repository.save(createDummyEntity());
		DummyEntity three = repository.save(createDummyEntity());

		repository.deleteAll(asList(one, three));

		assertThat(repository.findAll()) //
				.extracting(DummyEntity::getId) //
				.containsExactlyInAnyOrder(two.getId());
	}

	@Test // DATAJDBC-111
	public void deleteAll() {

		repository.save(createDummyEntity());
		repository.save(createDummyEntity());
		repository.save(createDummyEntity());

		assertThat(repository.findAll()).isNotEmpty();

		repository.deleteAll();

		assertThat(repository.findAll()).isEmpty();
	}

	private static DummyEntity createDummyEntity() {
		DummyEntity entity = new DummyEntity();
		entity.setTest("root");

		final Embeddable embeddable = new Embeddable();
		embeddable.setTest("embedded");

		final DummyEntity2 dummyEntity21 = new DummyEntity2();
		dummyEntity21.setTest("entity1");

		final DummyEntity2 dummyEntity22 = new DummyEntity2();
		dummyEntity22.setTest("entity2");

		embeddable.getList().add(dummyEntity21);
		embeddable.getList().add(dummyEntity22);

		entity.setEmbeddable(embeddable);

		return entity;
	}

	interface DummyEntityRepository extends CrudRepository<DummyEntity, Long> {}

	@Data
	private static class DummyEntity {
		@Id Long id;

		String test;

		@Embedded("prefix_")
		Embeddable embeddable;
	}

	@Data
	private static class Embeddable {
		@Column(value = "id", keyColumn = "key")
		List<DummyEntity2> list = new ArrayList<>();

		String test;
	}

	@Data
	private static class DummyEntity2 {
		String test;
	}
}
