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
package com.avanza.astrix.integration.tests;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.registry.ServiceRegistryClient;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryEntry;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.config.ConfigSource;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixApplication;
import com.avanza.astrix.provider.core.AstrixServiceExport;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.versioning.AstrixJsonApiMigration;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfig;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfigurer;
import com.avanza.astrix.provider.versioning.JacksonObjectMapperBuilder;
import com.avanza.astrix.provider.versioning.Versioned;
import com.avanza.astrix.serviceunit.ServiceAdministrator;
import com.avanza.astrix.spring.AstrixFrameworkBean;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.avanza.astrix.versioning.plugin.Jackson1ObjectSerializerConfigurer;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceBlueGreenDeployTest {

	private InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	static String ACCOUNT_PERFORMANCE_SUBSYSTEM = "account-performance-subsystem";
	
	private AstrixSettings accountPerformanceClientConfig = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		set(SERVICE_LEASE_RENEW_INTERVAL, 100);
		set(BEAN_BIND_ATTEMPT_INTERVAL, 100);
		set(SUBSYSTEM_NAME, "client-subsystem");
	}};
	
	private AstrixSettings feeder1clientConfig = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		set(SERVICE_LEASE_RENEW_INTERVAL, 100);
		set(BEAN_BIND_ATTEMPT_INTERVAL, 100);
		set(SUBSYSTEM_NAME, ACCOUNT_PERFORMANCE_SUBSYSTEM);
		set(APPLICATION_TAG, "1");
	}};
	
	private AstrixSettings feeder2clientConfig = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		set(SERVICE_LEASE_RENEW_INTERVAL, 100);
		set(BEAN_BIND_ATTEMPT_INTERVAL, 100);
		set(SUBSYSTEM_NAME, ACCOUNT_PERFORMANCE_SUBSYSTEM);
		set(APPLICATION_TAG, "2");
	}};
	
	private AstrixSettings server1Config = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		set(SERVICE_LEASE_RENEW_INTERVAL, 100);
		set(BEAN_BIND_ATTEMPT_INTERVAL, 100);
		set(APPLICATION_INSTANCE_ID, "server-1");
		set(SERVICE_ADMINISTRATOR_COMPONENT, AstrixServiceComponentNames.DIRECT);
		set(APPLICATION_TAG, "1");
	}};
	
	private AstrixSettings server2Config = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		set(SERVICE_LEASE_RENEW_INTERVAL, 100);
		set(BEAN_BIND_ATTEMPT_INTERVAL, 100);
		set(APPLICATION_INSTANCE_ID, "server-2");
		set(SERVICE_ADMINISTRATOR_COMPONENT, AstrixServiceComponentNames.DIRECT);
		set(APPLICATION_TAG, "2");
	}};
	
	private AstrixSettings feeder1Config = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		set(SERVICE_LEASE_RENEW_INTERVAL, 100);
		set(BEAN_BIND_ATTEMPT_INTERVAL, 100);
		set(APPLICATION_TAG, "1");
		set(SERVICE_ADMINISTRATOR_COMPONENT, AstrixServiceComponentNames.DIRECT);
		set(APPLICATION_INSTANCE_ID, "feeder-server-1");
	}};
	
	private AstrixSettings feeder2Config = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		set(SERVICE_LEASE_RENEW_INTERVAL, 100);
		set(BEAN_BIND_ATTEMPT_INTERVAL, 100);
		set(APPLICATION_TAG, "2");
		set(SERVICE_ADMINISTRATOR_COMPONENT, AstrixServiceComponentNames.DIRECT);
		set(APPLICATION_INSTANCE_ID, "feeder-server-2");
	}};
	

	private AstrixContext feeder1clientContext;
	private AstrixContext feeder2clientContext;
	private AstrixSpringApp server1 = new AstrixSpringApp(server1Config, AccountPerformanceAppConfig.class);
	private AstrixSpringApp server2 = new AstrixSpringApp(server2Config, AccountPerformanceAppConfig.class);
	private AstrixSpringApp feeder1 = new AstrixSpringApp(feeder1Config, FeederAppConfig.class);
	private AstrixSpringApp feeder2 = new AstrixSpringApp(feeder2Config, FeederAppConfig.class);
	private AstrixContext accountPerformanceClientContext;
	private AccountPerformance accountPerformance;

	static class AstrixSpringApp implements AutoCloseable {
		private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		
		public AstrixSpringApp(AstrixSettings astrixSettings, Class<?> configuration) {
			this.context.register(configuration);
			Map<String, Object> settings = new HashMap<>();
			settings.put("configSourceId", astrixSettings.getConfigSourceId());
			context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("astrixSettings", settings));
		}

		@Override
		public void close() throws Exception {
			this.context.close();
		}
		public void start() {
			this.context.refresh();
		}
	}
	
	@Before
	public void setup() throws Exception {
		this.feeder1clientContext = new AstrixConfigurer().setConfig(DynamicConfig.create(feeder1clientConfig)).configure();
		this.feeder2clientContext = new AstrixConfigurer().setConfig(DynamicConfig.create(feeder2clientConfig)).configure();
		this.accountPerformanceClientContext = new AstrixConfigurer().setConfig(DynamicConfig.create(accountPerformanceClientConfig)).configure();
	}
	
	@After
	public void after() throws Exception {
		AstrixTestUtil.closeSafe(feeder1clientContext);
		AstrixTestUtil.closeSafe(feeder2clientContext);
		AstrixTestUtil.closeSafe(accountPerformanceClientContext);
		AstrixTestUtil.closeSafe(server1);
		AstrixTestUtil.closeSafe(server2);
		AstrixTestUtil.closeSafe(feeder1);
		AstrixTestUtil.closeSafe(feeder2);
	}
	
	@Test
	public void registersServicesInZoneUsingSubsystemNameAndTag() throws Exception {
		feeder1.start();
		feeder1clientContext.waitForBean(FeederService.class, 1000);
		List<AstrixServiceRegistryEntry> providers = this.serviceRegistry.listServices(FeederService.class.getName(), null);
		assertEquals(1, providers.size());
		AstrixServiceRegistryEntry providerProperties = providers.get(0);
		assertEquals(ACCOUNT_PERFORMANCE_SUBSYSTEM + "#1", providerProperties.getServiceProperties().get(ServiceProperties.SERVICE_ZONE));
	}
	

	@Test
	public void blueGreenDeployTest() throws Exception {
		server1Config.set(AstrixSettings.PUBLISH_SERVICES, true);
		server1.start();
		
		server2Config.set(AstrixSettings.PUBLISH_SERVICES, false);
		server2.start();

		
		this.accountPerformance = accountPerformanceClientContext.waitForBean(AccountPerformance.class, 1000);

		String respondingAppIntance = accountPerformance.getAppInstanceId();
		assertEquals("server-1", respondingAppIntance);

		// Activate service-2
		ServiceAdministrator serviceInstance2Administrator = 
				feeder1clientContext.waitForBean(ServiceAdministrator.class, "server-2", 1000L);
		serviceInstance2Administrator.setPublishServices(true);
		
		ServiceAdministrator serviceInstance1Administrator = 
				feeder1clientContext.waitForBean(ServiceAdministrator.class, "server-1", 1000L);
		serviceInstance1Administrator.setPublishServices(false);
		
		ServiceRegistryClient serviceRegistryClient = feeder1clientContext.getBean(ServiceRegistryClient.class);
		List<ServiceProperties> serviceProperties = serviceRegistryClient.list(AstrixBeanKey.create(AccountPerformance.class));
		assertEquals(2, serviceProperties.size());
//		
		// Verify traffic eventually moves to instance-2
		assertEventually(new Probe() {
			private String lastReply;
			@Override
			public boolean isSatisfied() {
				return lastReply.startsWith("server-2");
			}
			@Override
			public void sample() {
				lastReply = accountPerformance.getAppInstanceId();
			}
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected reply from instance 1, but was " + lastReply);
			}
		});
	}
	
	@Test
	public void serviceGoesToInactiveStateWhenServerIsDeactivated() throws Exception {
		server1Config.set(AstrixSettings.PUBLISH_SERVICES, true);
		server1.start();

		this.accountPerformance = accountPerformanceClientContext.waitForBean(AccountPerformance.class, 1000);
		assertNotNull(accountPerformance.getAppInstanceId());
		
		ServiceAdministrator serviceInstance1Administrator = 
				feeder1clientContext.waitForBean(ServiceAdministrator.class, "server-1", 1000);
		serviceInstance1Administrator.setPublishServices(false);
		assertEventually(new Probe() {
			
			private boolean currentServiceState;

			@Override
			public void sample() {
				ServiceRegistryClient serviceRegistryClient = feeder1clientContext.getBean(ServiceRegistryClient.class);
				List<ServiceProperties> servicePropertyList = serviceRegistryClient.list(AstrixBeanKey.create(AccountPerformance.class));
				assertEquals("registered service count" + servicePropertyList, 1, servicePropertyList.size());
				ServiceProperties serviceProperties = servicePropertyList.get(0);
				currentServiceState = Boolean.valueOf(serviceProperties.getProperties().get(ServiceProperties.PUBLISHED));
			}
			
			@Override
			public boolean isSatisfied() {
				return currentServiceState == false;
			}
			
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected service to be non published, but it was published=" + currentServiceState);
			}
		});
	}
	
	@Test
	public void blueGreenDeployTest_TwoApplicationsInSameSubsystem() throws Exception {
		server1Config.set(AstrixSettings.PUBLISH_SERVICES, true);
		server1.start();
		feeder1.start();
		
		// Start isolated version of subsystem in state INACTIVE
		server2Config.set(AstrixSettings.PUBLISH_SERVICES, false);
		server2.start();
		feeder2.start();
		
		FeederService feeder1 = feeder1clientContext.waitForBean(FeederService.class, 1000);
		FeederService feeder2 = feeder2clientContext.waitForBean(FeederService.class, 1000);
		this.accountPerformance = accountPerformanceClientContext.waitForBean(AccountPerformance.class, 1000);

		assertEquals("feeder-server-1", feeder1.getAppInstanceId()); // Verify feeder1 talks to correct instance
		assertEquals("feeder-server-2", feeder2.getAppInstanceId());
		feeder1.setPerformance("21", 100);
		feeder2.setPerformance("21", 200);
		assertEquals("server-1", accountPerformance.getAppInstanceId()); // Verify server-1 is published
		assertEquals(Integer.valueOf(100), accountPerformance.getPerformance("21"));
		feeder1clientContext.waitForBean(ServiceAdministrator.class, "server-2", 1000).setPublishServices(true);
		feeder1clientContext.waitForBean(ServiceAdministrator.class, "server-1", 1000).setPublishServices(false);
		// Verify traffic eventually moves to instance-2
		assertEventually(new Probe() {
			Integer lastReply;
			@Override
			public boolean isSatisfied() {
				return Integer.valueOf(200).equals(lastReply);
			}
			@Override
			public void sample() {
				lastReply = accountPerformance.getPerformance("21");
			}
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected performance from server-2, but was : " + lastReply);
			}
		});
	}
	
	@Configuration
	public static class AccountPerformanceAppConfig {
		@Bean
		public static AstrixFrameworkBean astrix() {
			AstrixFrameworkBean astrix = new AstrixFrameworkBean();
			astrix.setSubsystem(ACCOUNT_PERFORMANCE_SUBSYSTEM);
			astrix.setApplicationDescriptor(AccountPerformanceApplicationDescriptor.class);
			return astrix;
		}
		
		@Bean
		public DynamicConfig config(Environment env) {
			String configSourceId = env.getProperty("configSourceId");
			ConfigSource configSource = GlobalConfigSourceRegistry.getConfigSource(configSourceId);
			return DynamicConfig.create(configSource);
		}
		
		@Bean
		public AccountPerformance accountPerformance(DynamicConfig config) {
			return new AccountPerformanceImpl(AstrixSettings.APPLICATION_INSTANCE_ID.getFrom(config).get());
		}
	}
	
	public static class DummyVersioningConfig implements AstrixObjectSerializerConfigurer {
		
	}
	
	@Configuration
	public static class FeederAppConfig {
		@Bean
		public static AstrixFrameworkBean astrix() {
			AstrixFrameworkBean astrix = new AstrixFrameworkBean();
			astrix.setSubsystem(ACCOUNT_PERFORMANCE_SUBSYSTEM);
			astrix.setApplicationDescriptor(FeederApplicationDescriptor.class);
			return astrix;
		}
		
		@Bean
		public DynamicConfig config(Environment env) {
			String configSourceId = env.getProperty("configSourceId");
			ConfigSource configSource = GlobalConfigSourceRegistry.getConfigSource(configSourceId);
			return DynamicConfig.create(configSource);
		}
		
		@Bean
		public FeederServiceImpl ping(AstrixContext astrix, DynamicConfig config) {
			return new FeederServiceImpl(astrix.getBean(AccountPerformanceInternal.class), AstrixSettings.APPLICATION_INSTANCE_ID.getFrom(config).get());
		}
	}

	@AstrixApplication(
		exportsRemoteServicesFor = AccountPerformaneApi.class,
		defaultServiceComponent = AstrixServiceComponentNames.DIRECT
	)
	public static class AccountPerformanceApplicationDescriptor {
	}

	@AstrixApplication(
		exportsRemoteServicesFor = FeederApi.class,
		defaultServiceComponent = AstrixServiceComponentNames.DIRECT
	)
	public static class FeederApplicationDescriptor {
	}
	
	@AstrixObjectSerializerConfig(
		objectSerializerConfigurer = AccountPerformanceVersioningConfig.class,
		version = 1
	)
	@AstrixApiProvider
	public static interface AccountPerformaneApi {
		@Versioned
		@Service
		AccountPerformance accountPerformance();
		
		@Service
		AccountPerformanceInternal accountPerformanceInternal();
	}
	
	public static class AccountPerformanceVersioningConfig implements Jackson1ObjectSerializerConfigurer {
		@Override
		public List<? extends AstrixJsonApiMigration> apiMigrations() {
			return Collections.emptyList();
		}
		@Override
		public void configure(JacksonObjectMapperBuilder objectMapperBuilder) {
		}
	}
	
	@AstrixApiProvider
	public interface FeederApi {
		@Service
		FeederService feederService();
	}
	
	public interface AccountPerformance {
		String getAppInstanceId();
		Integer getPerformance(String accountId);
	}
	
	public interface AccountPerformanceInternal {
		void setPerformance(String accountId, Integer performance);
	}

	public interface FeederService {
		String getAppInstanceId();
		void setPerformance(String accountId, Integer performance);
	}
	
	@AstrixServiceExport(FeederService.class)
	public static class FeederServiceImpl implements FeederService {
		private AccountPerformanceInternal accountPerformance;
		private String applicationInstanceId;
		
		public FeederServiceImpl(AccountPerformanceInternal updater, String applicationInstanceId) {
			this.accountPerformance = updater;
			this.applicationInstanceId = applicationInstanceId;
		}
		@Override
		public void setPerformance(String accountId, Integer performance) {
			this.accountPerformance.setPerformance(accountId, performance);
		}
		@Override
		public String getAppInstanceId() {
			return this.applicationInstanceId;
		}
	}
	
	@AstrixServiceExport({AccountPerformance.class, AccountPerformanceInternal.class})
	public static class AccountPerformanceImpl implements AccountPerformance, AccountPerformanceInternal {
		
		private String applicationInstanceId;
		private final ConcurrentMap<String, Integer> performanceByAccountId = new ConcurrentHashMap<>();
		
		public AccountPerformanceImpl(String applicationInstanceId) {
			this.applicationInstanceId = applicationInstanceId;
		}

		@Override
		public String getAppInstanceId() {
			return applicationInstanceId;
		}

		@Override
		public Integer getPerformance(String accountId) {
			return performanceByAccountId.get(accountId);
		}

		@Override
		public void setPerformance(String accountId, Integer performance) {
			this.performanceByAccountId.put(accountId, performance);
		}
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(10_000, 10).check(probe);
	}
	
}
