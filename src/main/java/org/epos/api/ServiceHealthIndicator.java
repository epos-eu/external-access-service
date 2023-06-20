package org.epos.api;

import org.epos.router_framework.RpcRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ServiceHealthIndicator implements HealthIndicator{
	
	@Autowired
	private RpcRouter router;

	@Override
	public Health health() {
		try {
			if(router.doHealthCheck()) {
				return Health.up().withDetail("RabbitMQ Connection", "Available").build();
			} else {
				return Health.down().withDetail("RabbitMQ Connection", "Not Available").build();
			}
		}catch(Exception e) {
			return Health.down().withDetail("RabbitMQ Connection", "Not Available").build();
		}
	}
}