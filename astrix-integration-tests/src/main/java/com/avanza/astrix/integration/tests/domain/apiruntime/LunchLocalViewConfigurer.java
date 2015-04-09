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
package com.avanza.astrix.integration.tests.domain.apiruntime;

import com.avanza.astrix.gs.localview.LocalViewConfigurer;
import com.avanza.astrix.gs.localview.LocalViewDefinition;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.j_spaces.core.client.SQLQuery;

public class LunchLocalViewConfigurer implements LocalViewConfigurer {

	@Override
	public void configure(LocalViewDefinition localView) {
		localView.addViewQuery(new SQLQuery<LunchRestaurant>());
		localView.setMaxDisconnectionDuration(1000);
	}
}
