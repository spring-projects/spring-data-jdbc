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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.experimental.Wither;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.util.MethodInvocationRecorder;
import org.springframework.data.util.MethodInvocationRecorder.Recorded;

import com.mockrunner.mock.jdbc.MockResultSet;

/**
 * @author Oliver Drotbohm
 */
public class NewJdbcConverterUnitTests {

	JdbcMappingContext context = new JdbcMappingContext();
	NewJdbcConverter converter = new NewJdbcConverterImpl(context, mock(NewRelationResolver.class),
			new EntityInstantiators());

	@Test
	public void readsImmutable() {

		TestSpec<TrivialImmutable> spec = TestSpec.of(TrivialImmutable.class) //
				.andProperty(TrivialImmutable::getId, 42L) //
				.andProperty(TrivialImmutable::getName, "Dave");

		TrivialImmutable result = converter.read(TrivialImmutable.class, spec //
				.toResultSet());

		spec.verify(result);
	}

	@Test
	public void readsBeanWithPropertiesToPopulate() {

		TestSpec<Trivial> spec = TestSpec.of(Trivial.class) //
				.andProperty(Trivial::getId, 42L) //
				.andProperty(Trivial::getName, "Dave");

		Trivial trivial = converter.read(Trivial.class, spec.toResultSet());

		spec.verify(trivial);
	}

	@Test
	public void simpleOneToOneGetsProperlyExtracted() {

		TestSpec<OneToOne> spec = TestSpec.of(OneToOne.class) //
				.andProperty(OneToOne::getId, 42L) //
				.andProperty(OneToOne::getName, "Dave") //
				.andValue("child_id", 24L) //
				.andValue("child_name", "beta");

		OneToOne result = converter.read(OneToOne.class, spec.toResultSet());

		spec.verify(result);
		spec.verifyAdditionalValues(values -> {

			assertThat(result.child.getId()).isEqualTo(values.get("child_id"));
			assertThat(result.child.getName()).isEqualTo(values.get("child_name"));
		});
	}

	@Test
	public void simpleOneToOneImmutableGetsProperlyExtracted() {

		TestSpec<OneToOneImmutable> spec = TestSpec.of(OneToOneImmutable.class) //
				.andProperty(OneToOneImmutable::getId, 42L) //
				.andProperty(OneToOneImmutable::getName, "Dave") //
				.andValue("child_id", 24L) //
				.andValue("child_name", "beta");

		OneToOneImmutable result = converter.read(OneToOneImmutable.class, spec.toResultSet());

		spec.verify(result);
		spec.verifyAdditionalValues(values -> {

			assertThat(result.child.getId()).isEqualTo(values.get("child_id"));
			assertThat(result.child.getName()).isEqualTo(values.get("child_name"));
		});
	}

	@Wither
	@Getter
	@RequiredArgsConstructor
	static class TrivialImmutable {

		@Id private final Long id;
		private final String name;
	}

	@EqualsAndHashCode
	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	static class Trivial {

		@Id Long id;
		String name;
	}

	@Getter
	static class OneToOne {

		@Id Long id;
		String name;
		Trivial child;
	}

	@Getter
	@Wither
	@RequiredArgsConstructor
	static class OneToOneImmutable {

		private final @Id Long id;
		private final String name;
		private final TrivialImmutable child;
	}

	@RequiredArgsConstructor
	private static class TestSpec<T> {

		private final Class<T> type;
		private final Map<Function<T, ? extends Object>, Object> expected;
		private final Map<Function<T, ? extends Object>, String> columnNames;
		private final @Wither @Singular Map<String, Object> additionalValues;

		public static <T> TestSpec<T> of(Class<T> type) {
			return new TestSpec<>(type, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
		}

		public <S> TestSpec<T> andProperty(Function<T, S> extractor, S value) {

			Map<Function<T, ? extends Object>, Object> expected = new HashMap<>(this.expected);
			expected.put(extractor, value);

			return new TestSpec<>(type, expected, columnNames, additionalValues);
		}

		// public TestSpec<T> andProperty(Function<T, Object> extractor, String columnName, Object value) {
		//
		// Map<Function<T, ? extends Object>, Object> expected = new HashMap<>(this.expected);
		// expected.put(extractor, value);
		//
		// Map<Function<T, ? extends Object>, String> columnNames = new HashMap<>(this.columnNames);
		// columnNames.put(extractor, columnName);
		//
		// return new TestSpec<>(type, expected, columnNames, additionalValues);
		// }

		public TestSpec<T> andValue(String key, Object value) {

			Map<String, Object> additionalValues = new HashMap<>(this.additionalValues);
			additionalValues.put(key, value);

			return withAdditionalValues(additionalValues);
		}

		public void verify(T entity) {
			expected.forEach((function, value) -> {
				assertThat(function.apply(entity)).isEqualTo(value);
			});
		}

		public void verifyAdditionalValues(Consumer<Map<String, Object>> consumer) {
			consumer.accept(additionalValues);
		}

		@SneakyThrows
		public ResultSet toResultSet() {

			Recorded<T> recorded = MethodInvocationRecorder.forProxyOf(type);

			Map<String, Object> collect = expected.entrySet().stream() //
					.collect(Collectors.toMap(//
							e -> getColumn(e, recorded), //
							e -> e.getValue()));

			HashMap<String, Object> columns = new LinkedHashMap<>(collect);
			columns.putAll(additionalValues);

			MockResultSet set = new MockResultSet("result");

			set.addColumns(columns.keySet());
			set.addRow(columns);
			set.next();

			return set;
		}

		private String getColumn(Entry<Function<T, ? extends Object>, Object> entry, Recorded<T> recorded) {

			String string = columnNames.get(entry.getKey());

			if (string != null) {
				return string;
			}

			return recorded.record(entry.getKey()).getPropertyPath().orElse(null);
		}
	}
}
