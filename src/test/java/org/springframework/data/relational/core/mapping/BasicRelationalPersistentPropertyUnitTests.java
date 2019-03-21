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
package org.springframework.data.relational.core.mapping;

import static org.assertj.core.api.Assertions.*;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.Test;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.relational.core.mapping.BasicRelationalPersistentProperty;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Unit tests for the {@link BasicRelationalPersistentProperty}.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 */
public class BasicRelationalPersistentPropertyUnitTests {

	RelationalMappingContext context = new RelationalMappingContext();

	@Test // DATAJDBC-104
	public void enumGetsStoredAsString() {

		RelationalPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DummyEntity.class);

		persistentEntity.doWithProperties((PropertyHandler<RelationalPersistentProperty>) p -> {
			switch (p.getName()) {
				case "someEnum":
					assertThat(p.getColumnType()).isEqualTo(String.class);
					break;
				case "localDateTime":
					assertThat(p.getColumnType()).isEqualTo(Date.class);
					break;
				case "zonedDateTime":
					assertThat(p.getColumnType()).isEqualTo(String.class);
					break;
				default:
			}
		});
	}

	@Test // DATAJDBC-106
	public void detectsAnnotatedColumnName() {

		RelationalPersistentEntity<?> entity = context.getRequiredPersistentEntity(DummyEntity.class);

		assertThat(entity.getRequiredPersistentProperty("name").getColumnName()).isEqualTo("dummy_name");
		assertThat(entity.getRequiredPersistentProperty("localDateTime").getColumnName())
				.isEqualTo("dummy_last_updated_at");
	}

	@Data
	private static class DummyEntity {

		private final SomeEnum someEnum;
		private final LocalDateTime localDateTime;
		private final ZonedDateTime zonedDateTime;

		// DATACMNS-106

		private @Column("dummy_name") String name;

		@Column("dummy_last_updated_at")
		public LocalDateTime getLocalDateTime() {
			return localDateTime;
		}
	}

	private enum SomeEnum {
		ALPHA;
	}
}
