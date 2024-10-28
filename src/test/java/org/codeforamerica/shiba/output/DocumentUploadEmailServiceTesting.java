package org.codeforamerica.shiba.output;

import static org.codeforamerica.shiba.Program.CASH;
import static org.codeforamerica.shiba.Program.SNAP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.util.Arrays;
import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.application.Status;
import org.codeforamerica.shiba.application.parsers.ContactInfoParser;
import org.codeforamerica.shiba.application.parsers.EmailParser;
import org.codeforamerica.shiba.pages.DocRecommendationMessageService;
import org.codeforamerica.shiba.pages.DocRecommendationMessageService.DocumentRecommendation;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.emails.EmailClient;
import org.codeforamerica.shiba.pages.emails.EmailContentCreator;
import org.codeforamerica.shiba.pages.rest.CommunicationClient;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import com.google.gson.JsonObject;

@SpringBootTest
@ActiveProfiles("test")
public class DocumentUploadEmailServiceTesting {
	private final String CLIENT_EMAIL = "client@example.com";

	@Autowired
	private DocumentUploadEmailService documentUploadEmailService;

	@MockBean
	private CommunicationClient commHubEmailSendingClient;

	@MockBean
	private ApplicationRepository applicationRepository;

	@MockBean
	private DocRecommendationMessageService docRecommendationMessageService;

	@MockBean
	private EmailContentCreator emailContentCreator;

	@MockBean
	private MessageSource messageSource;

	@Captor
	private ArgumentCaptor<JsonObject> jsonCaptor;

	@Captor
	private ArgumentCaptor<String> applicationIdCaptor;

	@Captor
	private ArgumentCaptor<Status> statusCaptor;

	@Mock
	private ApplicationData applicationData;
	
	//private Application mockApp;
	

	private MockedStatic<EmailParser> emailParserMock;
	private MockedStatic<ContactInfoParser> contactInfoParserMock;

	@BeforeEach
	void setUp() {
		// Mock EmailParser
		emailParserMock = mockStatic(EmailParser.class);
		emailParserMock.when(() -> EmailParser.parse(any(ApplicationData.class))).thenReturn(Optional.of(CLIENT_EMAIL));

		// Mock ContactInfoParser for email opt-in
		contactInfoParserMock = mockStatic(ContactInfoParser.class);
		contactInfoParserMock.when(() -> ContactInfoParser.optedIntoEmailCommunications(any(ApplicationData.class)))
				.thenReturn(true);

		//create mock application
		 Application mockApp = mock(Application.class);
		when(mockApp.getApplicationData()).thenReturn(applicationData);
		when(mockApp.getId()).thenReturn("test-app-id");
		when(mockApp.getFlow()).thenReturn(FlowType.FULL);
		when(mockApp.getDocUploadEmailStatus()).thenReturn(null);

		// Mock ApplicationData behavior
		when(applicationData.getUploadedDocs()).thenReturn(Collections.emptyList());
		when(applicationData.getLocale()).thenReturn(Locale.ENGLISH);
		when(applicationData.getId()).thenReturn("test-app-id");
		
		//Now use the created mock directly to
		List<Application> mockApplications = Collections.singletonList(mockApp);
		when(applicationRepository.getApplicationsSubmittedBetweenTimestamps(any(), any()))
				.thenReturn(mockApplications);

		// Mock DocumentRecommendations
		when(docRecommendationMessageService.getConfirmationEmailDocumentRecommendations(any(ApplicationData.class),
				any(Locale.class)))
				.thenReturn(List.of(
						new DocRecommendationMessageService.DocumentRecommendation("Test Title", "Test Explanation")));

		// Mock EmailContentCreator
		when(emailContentCreator.createDocRecommendationEmail(any(ApplicationData.class)))
				.thenReturn("Test email content");

		// Mock MessageSource
		when(messageSource.getMessage(eq("email.document-recommendation-email-subject"), any(), any(Locale.class)))
				.thenReturn("[Action Required] Upload Documents To Your MNbenefits Application");
		
		when(applicationData.getUploadedDocs()).thenReturn(Collections.emptyList());

	}

	@Test
	void sendDocumentUploadEmailsTest() {
		// Execute service method
		documentUploadEmailService.sendDocumentUploadEmailReminders();

		verify(applicationRepository, times(1)).getApplicationsSubmittedBetweenTimestamps(any(), any());

		// Verify email sending
		verify(commHubEmailSendingClient, times(1))
		.sendEmailDataToCommhub(jsonCaptor.capture());

		//verify directly the status of the email sent SUccessfully
		verify(applicationRepository, times(1))
		.setDocUploadEmailStatus("test-app-id", Status.DELIVERED);

		// Verify email content
		JsonObject emailJson = jsonCaptor.getValue();
		assertEquals("[Action Required] Upload Documents To Your MNbenefits Application",
				emailJson.get("subject").getAsString());
		assertEquals("sender@email.org", emailJson.get("senderEmail").getAsString());
		assertEquals(CLIENT_EMAIL, emailJson.get("recepientEmail").getAsString());
		assertTrue(emailJson.get("emailContent").getAsString().contains("Test email content"));
		assertEquals("test-app-id", emailJson.get("applicationId").getAsString());

		verifyNoMoreInteractions(commHubEmailSendingClient);
	}

	@Test
	void shouldHandleEmailSendingFailure()throws Exception {
	
		doThrow(new RuntimeException("Email Sending Failed"))
			.when(commHubEmailSendingClient)
			.sendEmailDataToCommhub(any(JsonObject.class));
		
		try{
			documentUploadEmailService.sendDocumentUploadEmailReminders();
		}catch (RuntimeException e) {
			
		}
		
		// Assert- Verify FAILURE path status update
		verify(applicationRepository, times(1))
			.setDocUploadEmailStatus(eq("test-app-id"), eq(Status.DELIVERY_FAILED));
		
		verify(applicationRepository, times(0))
		.setDocUploadEmailStatus(eq("test-app-id"), eq(Status.DELIVERED));
	}
	
	
	@AfterEach
	void tearDown() {
		emailParserMock.close();
		contactInfoParserMock.close();
	}

}
