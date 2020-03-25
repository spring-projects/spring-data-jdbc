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
package org.springframework.data.relational.core.dialect;

import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.IdentifierProcessing.LetterCasing;
import org.springframework.data.relational.core.sql.IdentifierProcessing.Quoting;

/**
 * A SQL dialect for MySQL.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 1.1
 */
public class MySqlDialect extends AbstractDialect {

	/**
	 * Singleton instance.
	 */
	public static final MySqlDialect INSTANCE = new MySqlDialect();
	private IdentifierProcessing identifierProcessing;

	protected MySqlDialect() {
		this(IdentifierProcessing.create(new Quoting("`"), LetterCasing.LOWER_CASE));
	}

	public MySqlDialect(IdentifierProcessing identifierProcessing) {
		this.identifierProcessing = identifierProcessing;
	}

	private static final LimitClause LIMIT_CLAUSE = new LimitClause() {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.relational.core.dialect.LimitClause#getLimit(long)
		 */
		@Override
		public String getLimit(long limit) {
			return "LIMIT " + limit;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.relational.core.dialect.LimitClause#getOffset(long)
		 */
		@Override
		public String getOffset(long offset) {
			// Ugly but the official workaround for offset without limit
			// see: https://stackoverflow.com/a/271650
			return String.format("LIMIT %d, 18446744073709551615", offset);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.relational.core.dialect.LimitClause#getClause(long, long)
		 */
		@Override
		public String getLimitOffset(long limit, long offset) {

			// LIMIT {[offset,] row_count}
			return String.format("LIMIT %s, %s", offset, limit);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.relational.core.dialect.LimitClause#getClausePosition()
		 */
		@Override
		public Position getClausePosition() {
			return Position.AFTER_ORDER_BY;
		}
	};

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.relational.core.dialect.Dialect#limit()
	 */
	@Override
	public LimitClause limit() {
		return LIMIT_CLAUSE;
	}

	@Override
	public IdentifierProcessing getIdentifierProcessing() {
		return identifierProcessing;
	}
}
