package org.codeforamerica.shiba.pages.rest;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommHubRestServiceTemplateConfiguration {

	@Bean
	public RestTemplateBuilder commHubRestServiceTemplate(RestTemplateBuilderConfigurer configurer,
			@Value("${comm-hub.timeout-seconds}") long timeoutSeconds) {
		System.out.println("========== CommHubRestServiceTemplateConfiguration commHubWebServiceTemplate ========");//TODO emj delete
		System.out.println("==========  timeoutSeconds: " + timeoutSeconds + " ========");
		return configurer.configure(new RestTemplateBuilder())
				.setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
				.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
	}

}
