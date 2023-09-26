package org.codeforamerica.shiba.mnit;

import static org.codeforamerica.shiba.County.Hennepin;
import static org.codeforamerica.shiba.County.Olmsted;
import static org.codeforamerica.shiba.application.FlowType.FULL;
import static org.codeforamerica.shiba.application.Status.DELIVERED;
import static org.codeforamerica.shiba.application.Status.SENDING;
import static org.codeforamerica.shiba.output.Document.CAF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.ws.test.client.RequestMatchers.connectionTo;
import static org.springframework.ws.test.client.RequestMatchers.xpath;
import static org.springframework.ws.test.client.ResponseCreators.withException;
import static org.springframework.ws.test.client.ResponseCreators.withSoapEnvelope;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import javax.xml.transform.dom.DOMResult;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationStatus;
import org.codeforamerica.shiba.application.ApplicationStatusRepository;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.output.ApplicationFile;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.WebServiceTransportException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.namespace.SimpleNamespaceContext;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPException;

@SpringBootTest
@ActiveProfiles("test")
class CommHubbServiceTest {

  private final Map<String, String> namespaceMapping = Map.of(
      "ns2", "http://docs.oasis-open.org/ns/cmis/messaging/200908/",
      "ns3", "http://docs.oasis-open.org/ns/cmis/core/200908/",
      "cmism", "http://docs.oasis-open.org/cmis/CMIS/v1.1/errata01/os/schema/CMIS-Messaging.xsd");
  private final String fileContent = "fileContent";
  private final String fileName = "fileName";
  private final String filenetIdd = "idd_some-filenet-idd";
  private final StringSource successResponse = new StringSource("" +
      "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/'>" +
      "<soapenv:Body>" +
      "<b:createDocumentResponse xmlns:b='http://docs.oasis-open.org/ns/cmis/messaging/200908/'>" +
      "<b:objectId>" + filenetIdd + "</b:objectId>" +
      "<b:extension si:nil='true' xmlns:si='http://www.w3.org/2001/XMLSchema-instance'/>" +
      "</b:createDocumentResponse>" +
      "</soapenv:Body>" +
      "</soapenv:Envelope>"
  );
  private final String routerResponse = "{\n \"message\" : \"Success\" \n}";
  @Autowired
  @Qualifier("filenetWebServiceTemplate")
  private WebServiceTemplate webServiceTemplate;
  @Value("${comm-hub.url}")
  private String url;
  @Value("${comm-hub.enabled}")
  private String enabled;
  @Value("${mnit-filenet.sftp-upload-url}")
  private String sftpUploadUrl;
  private MockWebServiceServer mockWebServiceServer;
  @MockBean
  private RestTemplate restTemplate;
  
  private Application application;
  private CountyRoutingDestination olmsted;
  private CountyRoutingDestination hennepin;
  private String applicationId;

  @BeforeEach
  void setUp() {
    mockWebServiceServer = MockWebServiceServer.createServer(webServiceTemplate);
    applicationId = "someId";
    application = Application.builder()
            .id("someId")
            .flow(FULL)
            .build();

    String routerRequest = String.format("%s/%s", sftpUploadUrl, filenetIdd);
    Mockito.when(restTemplate.getForObject(routerRequest, String.class)).thenReturn(routerResponse);
  }

  @Test
  void sendObjectToCommHubSuccess() {
	  mockWebServiceServer.expect(connectionTo(url))
      .andRespond(withSoapEnvelope(successResponse));

	  String routerRequest = String.format("%s/%s", sftpUploadUrl, filenetIdd);
	  Mockito.when(restTemplate.getForObject(routerRequest, String.class)).thenReturn(routerResponse);
	
	  verify(applicationStatusRepository).createOrUpdate(applicationId, Document.CAF, olmsted.getName(),
	      DELIVERED, fileName);
	
	  mockWebServiceServer.verify();
  }
  
  @Test
  void sendObjectToCommHubExpectedRetryThenSuccess() {
	  mockWebServiceServer.expect(connectionTo(url))
      .andRespond(withException(new RuntimeException(new SOAPException("initial failure"))));

	  RuntimeException exceptionToSend = new RuntimeException(
	      mock(SoapFaultClientException.class));
	  mockWebServiceServer.expect(connectionTo(url))
	      .andRespond(withException(exceptionToSend));
	
	  mockWebServiceServer.verify();
  }

}
