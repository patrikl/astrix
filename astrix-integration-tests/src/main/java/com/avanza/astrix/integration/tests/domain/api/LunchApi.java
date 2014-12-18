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
package com.avanza.astrix.integration.tests.domain.api;

import com.avanza.astrix.integration.tests.common.Ping;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.versioning.Versioned;

@Versioned
public interface LunchApi {
	
	@Service
	LunchService lunchService();
	
	@AstrixQualifier("lunch-ping")
	@Service
	Ping ping();
	
	@Library
	LunchUtil lunchUtil();
	
	@Library
	LunchStatistics lunchStatistics();
}