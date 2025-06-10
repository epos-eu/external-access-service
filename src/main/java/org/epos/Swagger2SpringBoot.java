package org.epos;


import org.epos.configuration.LocalDateConverter;
import org.epos.configuration.LocalDateTimeConverter;
import org.epos.router_framework.RpcRouter;
import org.epos.router_framework.exception.RoutingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = { "org.epos", "org.epos.api" , "org.epos.configuration"})
public class Swagger2SpringBoot implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(Swagger2SpringBoot.class);
	
	@Autowired
	private RpcRouter router;
	
	@Override
	public void run(String... arg0) throws Exception {
		if (arg0.length > 0 && arg0[0].equals("exitcode")) {
			throw new ExitException();
		}

		try {
			router.init(System.getenv("BROKER_HOST"), System.getenv("BROKER_VHOST"), System.getenv("BROKER_USERNAME"),
					System.getenv("BROKER_PASSWORD"));
		} catch (RoutingException e) {
			LOGGER.error("A problem was encountered whilst initialising the routing framework.", e);
		}
	}

	public static void main(String[] args) throws Exception {
		new SpringApplication(Swagger2SpringBoot.class).run(args);
	}

	@Configuration
	static class CustomDateConfig implements WebMvcConfigurer {
		@Override
		public void addFormatters(FormatterRegistry registry) {
			registry.addConverter(new LocalDateConverter("yyyy-MM-dd"));
			registry.addConverter(new LocalDateTimeConverter("yyyy-MM-dd'T'HH:mm:ss.SSS"));
		}
	}

	class ExitException extends RuntimeException implements ExitCodeGenerator {
		private static final long serialVersionUID = 1L;

		@Override
		public int getExitCode() {
			return 10;
		}

	}
}
