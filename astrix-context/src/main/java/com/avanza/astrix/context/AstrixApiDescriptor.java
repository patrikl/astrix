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

import java.lang.annotation.Annotation;

import com.avanza.astrix.provider.core.AstrixServiceRegistryApi;
import com.avanza.astrix.provider.versioning.AstrixVersioned;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public abstract class AstrixApiDescriptor {

	public static AstrixApiDescriptor create(Class<?> descriptorHolder) {
		return new AnnotationApiDescriptor(descriptorHolder);
	}
	
	public static AstrixApiDescriptor simple(String name) {
		return new SimpleApiDescriptor(name);
	}

	public abstract boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

	public abstract <T extends Annotation> T getAnnotation(Class<T> annotationClass);
	
	public abstract String getName();
	
	public abstract Class<?> getDescriptorClass();
	
	@Override
	public final String toString() {
		return getName();
	}
	
	/**
	 * Whether this api uses the service registry or not.
	 * @return
	 */
	public abstract boolean usesServiceRegistry();
	
	public abstract boolean isVersioned();
	
	private static class SimpleApiDescriptor extends AstrixApiDescriptor {

		private String name;
		private AstrixFactoryBeanPlugin<?> factory;
		
		public SimpleApiDescriptor(String name) {
			this.name = name;
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			return false;
		}

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Class<?> getDescriptorClass() {
			return null;
		}

		@Override
		public boolean usesServiceRegistry() {
			return false;
		}

		@Override
		public boolean isVersioned() {
			return false;
		}

		public AstrixFactoryBeanPlugin<?> getFactory() {
			return factory;
		}
		
	}
	
	private static class AnnotationApiDescriptor extends AstrixApiDescriptor {
		private Class<?> descriptorHolder;

		private AnnotationApiDescriptor(Class<?> annotationHolder) {
			this.descriptorHolder = annotationHolder;
		}
		
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			return descriptorHolder.isAnnotationPresent(annotationClass);
		}

		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			return descriptorHolder.getAnnotation(annotationClass);
		}
		
		public String getName() {
			return this.descriptorHolder.getName();
		}

		public Class<?> getDescriptorClass() {
			return descriptorHolder;
		}
		
		/**
		 * Whether this api uses the service registry or not.
		 * @return
		 */
		public boolean usesServiceRegistry() {
			return descriptorHolder.isAnnotationPresent(AstrixServiceRegistryApi.class);
		}
		
		public boolean isVersioned() {
			return descriptorHolder.isAnnotationPresent(AstrixVersioned.class);
		}
		
	}

}