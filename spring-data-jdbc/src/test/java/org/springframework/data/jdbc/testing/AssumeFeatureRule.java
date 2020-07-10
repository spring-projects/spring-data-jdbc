/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.jdbc.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class AssumeFeatureRule implements TestExecutionListener {

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {

		ApplicationContext applicationContext = testContext.getApplicationContext();
		TestDatabaseFeatures databaseFeatures = applicationContext.getBean(TestDatabaseFeatures.class);

		List<TestDatabaseFeatures.Feature> requiredFeatures = new ArrayList<>();

		RequiredFeature classAnnotation = testContext.getTestClass().getAnnotation(RequiredFeature.class);
		if (classAnnotation != null) {
			requiredFeatures.addAll(Arrays.asList(classAnnotation.value()));
		}

		RequiredFeature methodAnnotation = testContext.getTestMethod().getAnnotation(RequiredFeature.class);
		if (methodAnnotation != null) {
			requiredFeatures.addAll(Arrays.asList(methodAnnotation.value()));
		}

		for (TestDatabaseFeatures.Feature requiredFeature : requiredFeatures) {
			requiredFeature.test(databaseFeatures);
		}

	}
}
