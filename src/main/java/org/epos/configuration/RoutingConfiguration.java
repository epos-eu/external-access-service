/*******************************************************************************
 * Copyright 2021 EPOS ERIC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.epos.configuration;

import java.util.Optional;

import org.epos.router_framework.RpcRouter;
import org.epos.router_framework.RpcRouterBuilder;
import org.epos.router_framework.domain.Actor;
import org.epos.router_framework.domain.BuiltInActorType;
import org.epos.router_framework.types.ServiceType;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingConfiguration {
	
	@Value("${router.num-of-publishers}")
	private int numOfPublishers;
	
	@Value("${router.num-of-consumers}")
	private int numOfConsumers;
	
    @Bean
    public RpcRouter router() 
    {
        Optional<RpcRouter> router = RpcRouterBuilder.instance(Actor.getInstance(BuiltInActorType.TCS_CONNECTOR.verbLabel()).get())
    			.addServiceSupport(ServiceType.EXTERNAL, Actor.getInstance(BuiltInActorType.CONVERTER.verbLabel()).get())
    			.setNumberOfPublishers(numOfPublishers)
    			.setNumberOfConsumers(numOfConsumers)
    			.setRoutingKeyPrefix("externalaccess")
    			.build();
        
        return router.orElseThrow(() -> new BeanInitializationException(
        		"Router instance for External Access Service component could not be instantiated"));
    }

}