package org.codeforamerica.shiba.pages.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommunicationClient implements RestClient {
	
	private Boolean enabled;
	private String comHubURL;
	
	public CommunicationClient(
			@Value("${comm-hub.enabled}") String enabled,
			@Value("${comm-hub.url}") String comHubURL) {
		this.enabled = Boolean.valueOf(enabled);
		this.comHubURL = comHubURL;		
	}
	
	/**
	 * This method composes the REST request with the given Json object and posts to comm-hub
	 */
	  @Retryable(
		      retryFor = {Exception.class},
		      maxAttempts = 6,
		      maxAttemptsExpression = "#{${comm-hub.max-attempts}}",
		      backoff = @Backoff(
		          delayExpression = "#{${comm-hub.delay}}",
		          multiplierExpression = "#{${comm-hub.multiplier}}",
		          maxDelayExpression = "#{${comm-hub.max-delay}}"
		      ),
		      listeners = {"commHubRetryListener"}
		  )
	@Override
	public void send(JsonObject appJsonObject) {
		
		if (!isEnabled()) {
			log.info("Post requests to comm-hub are disabled.");
			return;
		}
		
		try {
	      RestTemplate rt = new RestTemplate();
	      
	      HttpHeaders headers = new HttpHeaders();
	      headers.setContentType(MediaType.APPLICATION_JSON);
	      
	      HttpEntity<String> entity = 
	            new HttpEntity<String>(appJsonObject.toString(), headers);
	      
	      ResponseEntity<String> responseEntityStr = rt.
	            postForEntity(comHubURL, entity, String.class);
	      
	      log.info("responseEntityStr Result = {}", responseEntityStr);
		} catch(Exception e) {
			log.error("Comm Hub Error: " + e.getMessage(), e);
		}

	}

	@Override
	public Boolean isEnabled() {
		return enabled;
	}
	


}
