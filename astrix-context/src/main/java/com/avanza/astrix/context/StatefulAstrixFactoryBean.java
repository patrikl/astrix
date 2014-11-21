/*
 * Copyright 2014-2015 Avanza Bank AB
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
package com.avanza.astrix.context;

import java.lang.reflect.Proxy;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
final class StatefulAstrixFactoryBean<T> implements AstrixFactoryBeanPlugin<T>, AstrixDecorator, AstrixEventBusAware, AstrixBeanStateWorkerAware {

	private final AstrixFactoryBeanPlugin<T> targetFactory;
	private AstrixEventBus eventBus;
	private AstrixBeanStateWorker beanStateWorker;
	
	public StatefulAstrixFactoryBean(AstrixFactoryBeanPlugin<T> targetFactory) {
		if (!targetFactory.getBeanType().isInterface()) {
			throw new IllegalArgumentException("Can only create stateful Astrix beans if bean is exported using an interface." +
											   " targetBeanType=" + targetFactory.getBeanType().getName() + 
											   " beanFactoryType=" + targetFactory.getClass().getName());
		}
		this.targetFactory = targetFactory;
	}

	@Override
	public T create(String optionalQualifier) {
		StatefulAstrixBean<T> handler = new StatefulAstrixBean<>(targetFactory, optionalQualifier, eventBus);
		try {
			handler.bind();
		} catch (Exception e) {
		}
		beanStateWorker.add(handler);
		return targetFactory.getBeanType().cast(Proxy.newProxyInstance(targetFactory.getBeanType().getClassLoader(), new Class<?>[]{targetFactory.getBeanType()}, handler));
	}

	@Override
	public Class<T> getBeanType() {
		return targetFactory.getBeanType();
	}

	@Override
	public Object getTarget() {
		return targetFactory;
	}

	@Override
	public void setEventBus(AstrixEventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void setBeanStateWorker(AstrixBeanStateWorker worker) {
		this.beanStateWorker = worker;
	}

}