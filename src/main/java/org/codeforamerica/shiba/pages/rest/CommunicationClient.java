package org.codeforamerica.shiba.pages.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommunicationClient{

	private RestTemplateBuilder commHubRestServiceBuilder;
	@Getter
	private RestTemplate commHubRestServiceTemplate;
	
	private String commHubUrl;
	private Boolean commHubTextEnabled;
	private String commHubEmailUrl;
	private Boolean commHubEmailEnabled;
	private String emailSender;
	
	public CommunicationClient(@Qualifier("commHubRestServiceTemplate") RestTemplateBuilder commHubRestServiceBuilder, 
			@Value("${comm-hub-text.url}") String commHubUrl,
			@Value("${comm-hub-text.enabled}") Boolean commHubTextEnabled,
			@Value("${comm-hub-email.url}") String commHubEmailUrl,
			@Value("${comm-hub-email.enabled}") Boolean commHubEmailEnabled,
			@Value("${comm-hub-email.delivery}") String emailSender) {
		super();
		this.commHubRestServiceBuilder = commHubRestServiceBuilder;
		this.commHubRestServiceTemplate = this.commHubRestServiceBuilder.build();
		this.commHubUrl = commHubUrl;
		this.commHubTextEnabled = commHubTextEnabled;
		this.commHubEmailUrl = commHubEmailUrl;
		this.commHubEmailEnabled = commHubEmailEnabled;
		this.emailSender = emailSender;
	}

	
	  @Retryable(
		      retryFor = {RestClientException.class},
		      maxAttempts = 3,
		      maxAttemptsExpression = "#{${comm-hub-text.max-attempts}}",
		      backoff = @Backoff(
		          delayExpression = "#{${comm-hub-text.delay}}",
		          multiplierExpression = "#{${comm-hub-text.multiplier}}",
		          maxDelayExpression = "#{${comm-hub-text.max-delay}}"
		      ),
		      listeners = {"commHubRetryListener"}
		  )


	public void send(JsonObject appJsonObject){
		List<String> retryCodes = new ArrayList<>();
		retryCodes.add("502");
		retryCodes.add("503");
		retryCodes.add("504");
		  
		if (!iscommHubTextEnabled()) {
			log.info("Post requests to comm-hub are disabled.");
			return;
		}

		try {
	      HttpHeaders headers = new HttpHeaders();
	      headers.setContentType(MediaType.APPLICATION_JSON);
	      
	      HttpEntity<String> entity = 
	            new HttpEntity<String>(appJsonObject.toString(), headers);
	        
	      ResponseEntity<String> responseEntityStr = commHubRestServiceTemplate.
	            postForEntity(commHubUrl, entity, String.class);
	      
	      log.info("send responseEntityStr Result = {}", responseEntityStr);
		}catch(RestClientException rce ) {
			Throwable t = rce.getMostSpecificCause();
			String name = t.getClass().getTypeName();
			log.info("Comm Hub Client Error Exception name: " + name + " - Most Specific Cause: " + t.getLocalizedMessage());
			log.error("Comm Hub Client Error: " + rce.getMessage() + " for JSON object: " + appJsonObject.toString(), rce);
			if(Stream.of(t.getLocalizedMessage()).anyMatch(retryCodes::contains)) {
				throw rce;
			}
			
		} catch(Exception e) {
			log.error("Comm Hub Error: " + e.getMessage() + " for JSON object: " + appJsonObject.toString(), e);
			throw e;
		}

	}
	  
	  public void sendEmailDataToCommhub(JsonObject appJsonObject){
			List<String> retryCodes = new ArrayList<>();
			retryCodes.add("502");
			retryCodes.add("503");
			retryCodes.add("504");
			  
			if (!isCommHubEmailEnabled()) {
				log.info("Post requests to comm-hub-email are disabled.");
				return;
			}
			log.info("Attempting to call email endpoint: {}", commHubEmailUrl);

			try {
		      HttpHeaders headers = new HttpHeaders();
		      headers.setContentType(MediaType.APPLICATION_JSON);
		      
		      HttpEntity<String> entity = 
		            new HttpEntity<String>(appJsonObject.toString(), headers);
		        
		      ResponseEntity<String> responseEntityStr = commHubRestServiceTemplate.
		            postForEntity(commHubEmailUrl, entity, String.class);
		      
		      log.info("sendEmailDataToCommhub responseEntityStr Result = {}", responseEntityStr);
			}catch(RestClientException rce ) {
				Throwable t = rce.getMostSpecificCause();
				String name = t.getClass().getTypeName();
				log.info("Comm Hub Client Error Exception name: " + name + " - Most Specific Cause: " + t.getLocalizedMessage());
				log.error("Comm Hub Client Error: " + rce.getMessage() + " for JSON object: " + appJsonObject.toString(), rce);
				if(Stream.of(t.getLocalizedMessage()).anyMatch(retryCodes::contains)) {
					throw rce;
				}
				
			} catch(Exception e) {
				log.error("Comm Hub Error: " + e.getMessage() + " for JSON object: " + appJsonObject.toString(), e);
				throw e;
			}

		}

	public boolean isCommHubEmailEnabled() {
		return commHubEmailEnabled && emailSender.equalsIgnoreCase("commhub");
	}


	public Boolean iscommHubTextEnabled() {
		return commHubTextEnabled;
	}

}
