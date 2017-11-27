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
package org.springframework.data.jdbc.repository.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.mapping.model.ConversionCustomizer;
import org.springframework.data.jdbc.mapping.model.DefaultNamingStrategy;
import org.springframework.data.jdbc.mapping.model.JdbcMappingContext;
import org.springframework.data.jdbc.mapping.model.NamingStrategy;

/**
 * Beans that must be registered for Spring Data JDBC to work.
 * 
 * @author Greg Turnquist
 */
@Configuration
public class JdbcConfiguration {

	@Bean
	JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy,
										  Optional<ConversionCustomizer> conversionCustomizer) {

		return new JdbcMappingContext(
				namingStrategy.orElse(new DefaultNamingStrategy()),
				conversionCustomizer.orElse(conversionService -> {}));
	}
}
