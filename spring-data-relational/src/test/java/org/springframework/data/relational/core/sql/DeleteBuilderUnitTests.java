/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.data.relational.core.sql;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * Unit tests for {@link DeleteBuilder}.
 *
 * @author Mark Paluch
 */
public class DeleteBuilderUnitTests {

	@Test // DATAJDBC-335
	public void simpleDelete() {

		DeleteBuilder builder = StatementBuilder.delete();

		Table table = SQL.table("mytable");
		Column foo = table.column("foo");
		Column bar = table.column("bar");

		Delete delete = builder.from(table).where(foo.isEqualTo(bar)).build();

		CapturingVisitor visitor = new CapturingVisitor();
		delete.visit(visitor);

		assertThat(visitor.enter).containsSequence(new From(table), table, new Where(foo.isEqualTo(bar)),
				foo.isEqualTo(bar), foo, table, bar, table);

		assertThat(delete.toString()).isEqualTo("DELETE FROM mytable WHERE mytable.foo = mytable.bar");
	}
}