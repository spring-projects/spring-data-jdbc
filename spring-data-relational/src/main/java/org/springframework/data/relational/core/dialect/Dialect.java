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

import org.springframework.data.relational.core.sql.render.SelectRenderContext;
import org.springframework.data.relational.domain.IdentifierProcessing;
import org.springframework.data.relational.domain.SqlIdentifier;

/**
 * Represents a dialect that is implemented by a particular database. Please note that not all features are supported by
 * all vendors. Dialects typically express this with feature flags. Methods for unsupported functionality may throw
 * {@link UnsupportedOperationException}.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 1.1
 */
public interface Dialect {

	/**
	 * Return the {@link LimitClause} used by this dialect.
	 *
	 * @return the {@link LimitClause} used by this dialect.
	 */
	LimitClause limit();

	/**
	 * Returns the array support object that describes how array-typed columns are supported by this dialect.
	 *
	 * @return the array support object that describes how array-typed columns are supported by this dialect.
	 */
	default ArrayColumns getArraySupport() {
		return ArrayColumns.Unsupported.INSTANCE;
	}

	/**
	 * Obtain the {@link SelectRenderContext}.
	 *
	 * @return the {@link SelectRenderContext}.
	 */
	SelectRenderContext getSelectContext();

	/**
	 * Returns the {@link IdentifierProcessing} used for processing {@link SqlIdentifier} when converting them to SQL snippets or parameter names.
	 * 
	 * @return the {@link IdentifierProcessing}. Guaranteed to be not {@literal null}.
	 * @since 2.0
	 */
	default IdentifierProcessing getIdentifierProcessing() {
		return IdentifierProcessing.ANSI;
	}
}
