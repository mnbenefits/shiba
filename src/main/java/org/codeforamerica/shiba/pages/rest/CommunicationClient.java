package org.codeforamerica.shiba.pages.rest;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommunicationClient{

	private RestTemplateBuilder commHubRestServiceBuilder;
	
	private Clock clock;
	private String commHubUrl;
	private Boolean enabled;
	
	public CommunicationClient(@Qualifier("commHubRestServiceTemplate") RestTemplateBuilder commHubRestServiceBuilder, 
			Clock clock, 
			@Value("${comm-hub.url}") String commHubUrl,
			@Value("${comm-hub.enabled}") Boolean enabled) {
		super();
		this.commHubRestServiceBuilder = commHubRestServiceBuilder;
		this.clock = clock;
		this.commHubUrl = commHubUrl;
		this.enabled = enabled;
	}

	/**
	 * This method composes the REST request with the given Json object and posts to comm-hub
	 *  
	 */
	  @Retryable(
		      retryFor = {RestClientException.class},
		      maxAttempts = 3,
		      maxAttemptsExpression = "#{${comm-hub.max-attempts}}",
		      backoff = @Backoff(
		          delayExpression = "#{${comm-hub.delay}}",
		          multiplierExpression = "#{${comm-hub.multiplier}}",
		          maxDelayExpression = "#{${comm-hub.max-delay}}"
		      ),
		      listeners = {"commHubRetryListener"}
		  )

	public void send(JsonObject appJsonObject){
		
		if (!isEnabled()) {
			log.info("Post requests to comm-hub are disabled.");
			return;
		}

		try {
	      RestTemplate rt = commHubRestServiceBuilder.build();
	      
	      HttpHeaders headers = new HttpHeaders();
	      headers.setContentType(MediaType.APPLICATION_JSON);
	      
	      HttpEntity<String> entity = 
	            new HttpEntity<String>(appJsonObject.toString(), headers);
	        
		ResponseEntity<String> responseEntityStr = rt.
	            postForEntity(commHubUrl, entity, String.class);
	      
	      log.info("responseEntityStr Result = {}", responseEntityStr);
		}catch(RestClientException rce ) {
			Throwable t = rce.getMostSpecificCause();
			String name = t.getClass().getTypeName();
			//TODO modify the logged error after we see what kind of errors happen in production
			log.info("Comm Hub Client Error Exception name: " + name + " - Most Specific Cause: " + t.getLocalizedMessage());
			log.error("Comm Hub Client Error: " + rce.getMessage() + " for JSON object: " + appJsonObject.toString(), rce);
			t.getLocalizedMessage().contains("500");
			if(!t.getLocalizedMessage().contains("500")) {
				throw rce;
			}
			
		} catch(Exception e) {
			log.error("Comm Hub Error: " + e.getMessage() + " for JSON object: " + appJsonObject.toString(), e);
			throw e;
		}

	}

	//@Override
	public Boolean isEnabled() {
		return enabled;
	}

}
