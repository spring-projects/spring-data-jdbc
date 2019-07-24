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

import lombok.RequiredArgsConstructor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.convert.ObjectPath;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Drotbohm
 */
@RequiredArgsConstructor(staticName = "of")
public class ResultSetAccessor {

	private final ResultSet resultSet;

	public Object getValue(RelationalPersistentProperty property) {
		return getValue(property.getColumnName());
	}

	protected Object getValue(String columnName) {

		try {
			return resultSet.getObject(columnName);
		} catch (SQLException o_O) {

			throw new MappingException(String.format("Could not read value %s from result set containing columns: %s!",
					columnName, getColumnNames(resultSet)), o_O);
		}
	}

	private static List<String> getColumnNames(ResultSet resultSet) {

		try {

			ResultSetMetaData metaData = resultSet.getMetaData();
			int numberOfColumns = metaData.getColumnCount();
			List<String> columnNames = new ArrayList<>(numberOfColumns);

			for (int i = 0; i < numberOfColumns; i++) {
				columnNames.add(metaData.getCatalogName(i));
			}

			return columnNames;

		} catch (SQLException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	public ResultSetAccessor withContext(RelationalPersistentProperty contextualProperty,
			ObjectPath<? extends RelationalPersistentEntity<?>> path) {
		return new ContextualResultSetAccessor(resultSet, contextualProperty, path);
	}

	private static class ContextualResultSetAccessor extends ResultSetAccessor {

		private final RelationalPersistentProperty contextualProperty;
		private final ObjectPath<? extends RelationalPersistentEntity<?>> objectPath;

		/**
		 * @param contextualProperty
		 * @param objectPath
		 */
		public ContextualResultSetAccessor(ResultSet resultSet, RelationalPersistentProperty contextualProperty,
				ObjectPath<? extends RelationalPersistentEntity<?>> objectPath) {

			super(resultSet);

			this.contextualProperty = contextualProperty;
			this.objectPath = objectPath;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jdbc.core.convert.ResultSetAccessor#getValue(java.lang.String)
		 */
		@Override
		protected Object getValue(String columnName) {

			String embeddedPrefix = contextualProperty.getEmbeddedPrefix();

			String augmentedColumnName = StringUtils.hasText(embeddedPrefix) //
					? embeddedPrefix.concat("_").concat(columnName) //
					: contextualProperty.getName().concat("_").concat(columnName);

			return super.getValue(augmentedColumnName);
		}
	}
}
