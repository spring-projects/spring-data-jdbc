/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.jdbc.core.conversion;

import org.springframework.data.jdbc.core.conversion.DbAction.Delete;
import org.springframework.data.jdbc.core.conversion.DbAction.DeleteAll;
import org.springframework.data.jdbc.core.conversion.DbAction.Insert;
import org.springframework.data.jdbc.core.conversion.DbAction.Update;

/**
 * @author Jens Schauder
 */
public interface Interpreter {

	<T>void interpret(Update<T> update);
	<T>void interpret(Insert<T> insert);

	<T> void interpret(Delete<T> delete);
	<T> void interpret(DeleteAll<T> delete);
}
