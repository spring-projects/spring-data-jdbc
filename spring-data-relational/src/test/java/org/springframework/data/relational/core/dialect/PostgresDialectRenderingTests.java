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
package org.springframework.data.relational.core.dialect;

import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;

import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.NamingStrategies;
import org.springframework.data.relational.core.sql.render.SqlRenderer;

/**
 * Tests for {@link PostgresDialect}-specific rendering.
 *
 * @author Mark Paluch
 */
public class PostgresDialectRenderingTests {

	private final RenderContextFactory factory = new RenderContextFactory(PostgresDialect.INSTANCE);

	@Before
	public void before() throws Exception {
		factory.setNamingStrategy(NamingStrategies.asIs());
	}

	@Test // DATAJDBC-278
	public void shouldRenderSimpleSelect() {

		Table table = Table.create("foo");
		Select select = StatementBuilder.select(table.asterisk()).from(table).build();

		String sql = SqlRenderer.create(factory.createRenderContext()).render(select);

		assertThat(sql).isEqualTo("SELECT foo.* FROM foo");
	}

	@Test // DATAJDBC-278
	public void shouldApplyNamingStrategy() {

		factory.setNamingStrategy(NamingStrategies.toUpper());

		Table table = Table.create("foo");
		Select select = StatementBuilder.select(table.asterisk()).from(table).build();

		String sql = SqlRenderer.create(factory.createRenderContext()).render(select);

		assertThat(sql).isEqualTo("SELECT FOO.* FROM FOO");
	}

	@Test // DATAJDBC-278
	public void shouldRenderSelectWithLimit() {

		Table table = Table.create("foo");
		Select select = StatementBuilder.select(table.asterisk()).from(table).limit(10).build();

		String sql = SqlRenderer.create(factory.createRenderContext()).render(select);

		assertThat(sql).isEqualTo("SELECT foo.* FROM foo LIMIT 10");
	}

	@Test // DATAJDBC-278
	public void shouldRenderSelectWithOffset() {

		Table table = Table.create("foo");
		Select select = StatementBuilder.select(table.asterisk()).from(table).offset(10).build();

		String sql = SqlRenderer.create(factory.createRenderContext()).render(select);

		assertThat(sql).isEqualTo("SELECT foo.* FROM foo OFFSET 10");
	}

	@Test // DATAJDBC-278
	public void shouldRenderSelectWithLimitOffset() {

		Table table = Table.create("foo");
		Select select = StatementBuilder.select(table.asterisk()).from(table).limit(10).offset(20).build();

		String sql = SqlRenderer.create(factory.createRenderContext()).render(select);

		assertThat(sql).isEqualTo("SELECT foo.* FROM foo LIMIT 10 OFFSET 20");
	}
}
