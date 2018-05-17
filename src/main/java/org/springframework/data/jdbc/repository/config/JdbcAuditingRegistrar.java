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
package org.springframework.data.jdbc.repository.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.auditing.config.AuditingConfiguration;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.jdbc.domain.support.JdbcAuditingEventListener;

/**
 * {@link ImportBeanDefinitionRegistrar} which registers additional beans in order to enable auditing via the
 * {@link EnableJdbcAuditing} annotation.
 *
 * @see EnableJdbcAuditing
 * @author Kazuki Shimizu
 * @since 1.0
 */
class JdbcAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	/**
	 * {@inheritDoc}
	 * 
	 * @return return the {@link EnableJdbcAuditing}
	 * @see AuditingBeanDefinitionRegistrarSupport#getAnnotation()
	 */
	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJdbcAuditing.class;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return return "{@literal jdbcAuditingHandler}"
	 * @see AuditingBeanDefinitionRegistrarSupport#getAuditingHandlerBeanName()
	 */
	@Override
	protected String getAuditingHandlerBeanName() {
		return "jdbcAuditingHandler";
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport#getAuditHandlerBeanDefinitionBuilder(org.springframework.data.auditing.config.AuditingConfiguration)
	 */
	@Override
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(AuditingConfiguration configuration) {

		BeanDefinitionBuilder builder = super.getAuditHandlerBeanDefinitionBuilder(configuration);
		return builder.addConstructorArgReference("jdbcMappingContext");
	}

	/**
	 * Register the bean definition of {@link JdbcAuditingEventListener}. {@inheritDoc}
	 * 
	 * @see AuditingBeanDefinitionRegistrarSupport#registerAuditListenerBeanDefinition(BeanDefinition,
	 *      BeanDefinitionRegistry)
	 */
	@Override
	protected void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
			BeanDefinitionRegistry registry) {

		Class<?> listenerClass = JdbcAuditingEventListener.class;
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(listenerClass);
		builder.addPropertyValue("auditingHandler",
				ParsingUtils.getObjectFactoryBeanDefinition(getAuditingHandlerBeanName(), null));
		builder.setAutowireMode(AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
		registerInfrastructureBeanWithId(builder.getRawBeanDefinition(), listenerClass.getName(), registry);
	}

}
