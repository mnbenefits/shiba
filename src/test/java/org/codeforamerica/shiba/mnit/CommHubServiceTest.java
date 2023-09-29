package org.codeforamerica.shiba.mnit;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.codeforamerica.shiba.pages.rest.CommunicationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonObject;

import wiremock.com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Creating another test class from ground up.
 * We need to rename or remove one of these.
 *
 */
@SpringBootTest
@ActiveProfiles("test")
public class CommHubServiceTest {
	
	/*
	 * sendObjectToCommHubExpectedRetriesThenFailure
All responses returned by the /mnb-confirmation request are HTTP 503 (Service Unavailable)
Test that goes over the process for sending an object to comm-hub and the scenario is present 
which requires multiple retries to send the object again, ultimately ending with the object not being able to be sent
Mock responses that force the retry logic continue until it doesn't anymore and then ensure the end
 response lines up with what it should do
Currently there is no retry logic or a limit on how much it should retry, it is expected for there to be a retry 
feature, but the end response is currently unknown at the time of writing this test
	 */
	
	@Autowired
	private CommunicationClient communicationClient;

	// TODO: delete this commented-out code.
	// We need to use the same exact RestTemplate object that the CommunicationsClient uses
	// so let just get it from the Communications client.
    //@Autowired
    //@Qualifier("commHubRestServiceTemplate")
    //private RestTemplateBuilder restTemplateBuilder;
    
	@MockBean
	private Clock clock;
    
    private MockRestServiceServer mockServer;
    @Value("${comm-hub.url}")
    private String commHubURL;
    
    private String successString;
    private String serverError;

    @BeforeEach
    public void init() {
    	System.out.println("------ CommHubServiceTest init() restTemplate is null: " + (communicationClient.getCommHubRestServiceTemplate() == null));
        mockServer = MockRestServiceServer.bindTo(communicationClient.getCommHubRestServiceTemplate()).build();
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
//        successString = "<200 OK OK,[content-length:\"0\", date:\"" + clock.instant() + "\", "
//        		+ "set-cookie:\"368b5561d23651fde798b1d0eaf2ba27=b60509dc8d49ec3360adcc3a4555d26b; "
//        		+ "path=/; HttpOnly; Secure; SameSite=None\"]>";
        successString = "thank you";
        serverError = "SERVER ERROR";
        System.out.println("==== commHubURL: " + commHubURL);
        System.out.println(successString);
    }
    
	@Test
	public void sendObjectToCommHubSuccess() {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> entity = new HttpEntity<String>("some JSON String", headers);
		mockServer.expect(requestTo(commHubURL))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withStatus(HttpStatus.OK)
					.contentType(MediaType.APPLICATION_JSON)
					.body(successString));
		System.out.println(">>> commHubURL: " + commHubURL);
		ResponseEntity<String> response = communicationClient.getCommHubRestServiceTemplate().postForEntity(commHubURL, entity, String.class);
		System.out.println("++++ " + response.toString());
		mockServer.verify();
	}
    
	@Test
	public void sendObjectToCommHubExpectedRetriesThenFailure() {

		JsonObject appJsonObject = new JsonObject();
		appJsonObject.addProperty("whatever", "whatever");
		mockServer.expect(requestTo(commHubURL))
		.andExpect(method(HttpMethod.POST))
		.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
				.contentType(MediaType.APPLICATION_JSON)
				.body(serverError));

		communicationClient.send(appJsonObject);

		mockServer.expect(ExpectedCount.times(3), requestTo(""))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		mockServer.verify();

	}
    
    
    /*
     * This test verifies correct retry logic when the response from the comm-hub / is an HTTP 500 (Internal Server Error).
     * In this case there should not be any retires.
     */
    @Test                                                                                          
    public void sendObjectToCommHubErrorResponseHandling() {
    	// Create a mock web server to respond to our comm-hub request(s)
        mockServer
        .expect(
            requestTo(commHubURL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body("<500 INTERNAL SERVER ERROR"));
        
        // The request pay load doesn't really matter because we expect the HTTP 500 response
        JsonObject request = new JsonObject();
        communicationClient.send(request);
    }

}
