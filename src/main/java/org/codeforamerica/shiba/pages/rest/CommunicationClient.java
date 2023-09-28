package org.codeforamerica.shiba.pages.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.SocketTimeoutException;

import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

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
	@Override
	public void send(JsonObject appJsonObject){
		
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

	@Override
	public Boolean isEnabled() {
		return enabled;
	}

}
