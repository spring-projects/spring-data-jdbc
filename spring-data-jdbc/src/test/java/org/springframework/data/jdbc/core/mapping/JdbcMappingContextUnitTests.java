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
package org.springframework.data.jdbc.core.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * @author Oliver Drotbohm
 */
public class JdbcMappingContextUnitTests {

	public static final NamingStrategy X_APPENDING_NAMINGSTRATEGY = new NamingStrategy() {

		@Override
		public String getColumnName(RelationalPersistentProperty property) {
			return NamingStrategy.super.getColumnName(property) + "x";
		}
	};

	@Test
	public void appliesNamingStrategy() {

		JdbcMappingContext context = new JdbcMappingContext(X_APPENDING_NAMINGSTRATEGY);
		RelationalPersistentEntity<?> entity = context.getPersistentEntity(Sample.class);
		RelationalPersistentProperty property = entity.getRequiredPersistentProperty("name");

		assertThat(property.getColumnName()).isEqualTo("namex");
	}

	static class Sample {
		String name;
	}
}
