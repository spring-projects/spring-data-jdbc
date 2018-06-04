/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.jdbc.core.r2dbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.util.Assert;

/**
 * @author Mark Paluch
 */
class IterableUtils {

	static <T> Collection<T> toCollection(Iterable<T> iterable) {

		Assert.notNull(iterable, "Iterable must not be null!");

		if (iterable instanceof Collection) {
			return (Collection<T>) iterable;
		}

		List<T> result = new ArrayList<>();

		for (T element : iterable) {
			result.add(element);
		}

		return result;
	}

	static <T> List<T> toList(Iterable<T> iterable) {

		Assert.notNull(iterable, "Iterable must not be null!");

		if (iterable instanceof List) {
			return (List<T>) iterable;
		}

		List<T> result = new ArrayList<>();

		for (T element : iterable) {
			result.add(element);
		}

		return result;
	}
}
