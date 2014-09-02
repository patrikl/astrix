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
package se.avanzabank.asterix.versioning.plugin;

import org.kohsuke.MetaInfServices;

import se.avanzabank.asterix.context.AsterixVersioningPlugin;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.provider.versioning.AsterixVersioned;

@MetaInfServices(AsterixVersioningPlugin.class)
public class JacksonVersioningPlugin implements AsterixVersioningPlugin {
	@Override
	public AsterixObjectSerializer create(Class<?> astrixApiDescriptorHolder) {
		if (astrixApiDescriptorHolder.isAnnotationPresent(AsterixVersioned.class)) {
			AsterixVersioned versioningInfo = astrixApiDescriptorHolder.getAnnotation(AsterixVersioned.class);
			return new VersionJacksonAsterixObjectSerializer(versioningInfo);
		}
		return new AsterixObjectSerializer.NoVersioningSupport();
	}

}