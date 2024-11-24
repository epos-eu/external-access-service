package org.epos.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ScheduledRuntimes {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledRuntimes.class);

	@PostConstruct
	public void onStartup() {
		LOGGER.info("[External Service Startup] -----------------------------------------------");
		LOGGER.info("[StartUp Task - Resources] Updating facets information");
		connectionsUpdater();
		LOGGER.info("[StartUp Task - Resources] Done");
		LOGGER.info("[External Service Startup Completed] -----------------------------------------------");
	}

	@Scheduled(fixedRate = 60000, initialDelay = 0)
	@Async
	public void connectionsUpdater() {
		LOGGER.info("[Scheduled Task - Resources] Updating resources information");
        DatabaseConnections.getInstance();
        LOGGER.info("[Scheduled Task - Resources] Resources successfully updated");
	}

	private String getSubString(final String input, char characterStart, char characterEnd) {
		if(input == null) {
			return null;
		}

		final int indexOfAt = input.indexOf(characterStart);
		if(input.isEmpty() || indexOfAt < 0 || indexOfAt > input.length()-1) {
			return null;
		}

		String suffix = input.substring(indexOfAt + 1);

		final int indexOfDot = suffix.indexOf(characterEnd);

		if(indexOfDot < 1) {
			return null;
		}

		return suffix.substring(0, indexOfDot);
	}

}
