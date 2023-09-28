package org.codeforamerica.shiba.mnit;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;
import wiremock.org.apache.hc.core5.http.ContentType;
import wiremock.org.hamcrest.Matcher;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
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
    @Qualifier("commHubWebServiceTemplate")
    private RestTemplate restTemplate;
    
	  @MockBean
private Clock clock;
    
    private MockRestServiceServer mockServer;
    private ObjectMapper mapper = new ObjectMapper();
    @Value("${comm-hub.url}")
    private String commHubURL;
    
    private String successString;

    @BeforeEach
    public void init() {
    	//restTemplate = new RestTemplate(); THIS IS ALREADY AUTOWIRED, NO NEED TO CREATE A NEW ONE
        //mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        successString = "<200 OK OK,[content-length:\"0\", date:\"" + clock.instant() + "\", "
        		+ "set-cookie:\"368b5561d23651fde798b1d0eaf2ba27=b60509dc8d49ec3360adcc3a4555d26b; "
        		+ "path=/; HttpOnly; Secure; SameSite=None\"]>";
        System.out.println("==== commHubURL: " + commHubURL);
        System.out.println(successString);
    }
    
    @Test                                                                                          
    public void sendObjectToCommHubExpectedRetriesThenFailure() {   
// From https://www.baeldung.com/spring-mock-rest-template
    	
//    	Matcher<? super String> m;
//        mockServer.expect(ExpectedCount.once(), 
//        
//          requestTo(m))//new URI("http://localhost:8080/employee/E001")))
//          .andExpect(method(HttpMethod.GET))
//          .andRespond(withStatus(HttpStatus.OK)
//          .contentType(MediaType.APPLICATION_JSON)
//          .body("test")
//        );                                   
//        mockServer.verify();
        //Assertions.assertEquals(emp, employee);       
    	
 // From https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/client/MockRestServiceServer.html
//    	
//    	//restTemplate.
//    	MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
//
//    	server.expect(manyTimes(), requestTo("/hotels/42")).andExpect(method(HttpMethod.GET))
//    		.andRespond(withSuccess("{ \"id\" : \"42\", \"name\" : \"Holiday Inn\"}", MediaType.APPLICATION_JSON));
//    	 server.verify();
    	

        mockServer
            .expect(
                requestTo(commHubURL))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(successString));
        mockServer.verify();
        


     // verifyMockServerRequest("GET", "/albums/test/photos", 1);
    	
    	
    }
    

}
