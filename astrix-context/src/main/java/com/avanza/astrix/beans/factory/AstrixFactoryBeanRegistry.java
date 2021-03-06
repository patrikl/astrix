/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.beans.factory;

import java.util.Set;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AstrixFactoryBeanRegistry {

	/**
	 * @param beanKey
	 * @throws MissingBeanProviderException this registry doesn't contain a factory for the requested bean
	 * @return
	 */
	<T> StandardFactoryBean<T> getFactoryBean(AstrixBeanKey<T> beanKey);
	
	/**
	 * Invoked before requesting a factory from the registry. Allows the registry
	 * to bind multiple keys to the same factory and thereby the same instance at runtime. <p>
	 * 
	 * @param beanKey
	 * @return
	 */
	<T> AstrixBeanKey<? extends T> resolveBean(AstrixBeanKey<T> beanKey);
	
	<T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type);

}