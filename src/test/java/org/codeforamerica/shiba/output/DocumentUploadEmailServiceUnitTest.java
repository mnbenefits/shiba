package org.codeforamerica.shiba.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.application.Status;
import org.codeforamerica.shiba.application.parsers.ContactInfoParser;
import org.codeforamerica.shiba.application.parsers.EmailParser;
import org.codeforamerica.shiba.pages.DocRecommendationMessageService;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.emails.EmailContentCreator;
import org.codeforamerica.shiba.pages.rest.CommunicationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.google.gson.JsonObject;

//this is a unit test for the new feature of sending a jsonobject to commshub
@SpringBootTest
@ActiveProfiles("test")
public class DocumentUploadEmailServiceUnitTest {
	private final String CLIENT_EMAIL = "client@example.com";

	@Autowired
	private DocumentUploadEmailService documentUploadEmailService;

	@MockitoBean
	private CommunicationClient commHubEmailSendingClient;

	@MockitoBean
	private ApplicationRepository applicationRepository;

	@MockitoBean
	private DocRecommendationMessageService docRecommendationMessageService;

	@MockitoBean
	private EmailContentCreator emailContentCreator;

	@MockitoBean
	private MessageSource messageSource;

	@Captor
	private ArgumentCaptor<JsonObject> jsonCaptor;

	@Captor
	private ArgumentCaptor<String> applicationIdCaptor;

	@Captor
	private ArgumentCaptor<Status> statusCaptor;

	@Mock
	private ApplicationData applicationData;
	
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
		
		List<Application> mockApplications = Collections.singletonList(mockApp);
		when(applicationRepository.getApplicationsSubmittedBetweenTimestamps(any(), any()))
				.thenReturn(mockApplications);

		when(docRecommendationMessageService.getConfirmationEmailDocumentRecommendations(any(ApplicationData.class),
				any(Locale.class)))
				.thenReturn(List.of(
						new DocRecommendationMessageService.DocumentRecommendation("Test Title", "Test Explanation")));

		when(emailContentCreator.createDocRecommendationEmail(any(ApplicationData.class)))
				.thenReturn("Test email content");

		when(messageSource.getMessage(eq("email.document-recommendation-email-subject"), any(), any(Locale.class)))
				.thenReturn("[Action Required] Upload Documents To Your MNbenefits Application");
		
		when(applicationData.getUploadedDocs()).thenReturn(Collections.emptyList());

	}

	@Test
	void sendDocumentUploadEmailsTest() {
		documentUploadEmailService.sendDocumentUploadEmailReminders();

		verify(applicationRepository, times(1)).getApplicationsSubmittedBetweenTimestamps(any(), any());

		verify(commHubEmailSendingClient, times(1))
		.sendEmailDataToCommhub(jsonCaptor.capture());

		//verify directly the status of the email sent Successfully
		verify(applicationRepository, times(1))
		.setDocUploadEmailStatus("test-app-id", Status.DELIVERED);

		// Verify email content
		JsonObject emailJson = jsonCaptor.getValue();
		assertEquals("DOCUMENT_UPLOAD_REMINDER",
				emailJson.get("emailType").getAsString());
		assertEquals("[Action Required] Upload Documents To Your MNbenefits Application",
				emailJson.get("subject").getAsString());
		assertEquals("sender@email.org", emailJson.get("senderEmail").getAsString());
		assertEquals(CLIENT_EMAIL, emailJson.get("recepientEmail").getAsString());
		assertTrue(emailJson.get("emailContent").getAsString().contains("Test email content"));
		assertEquals("test-app-id", emailJson.get("applicationId").getAsString());

		verifyNoMoreInteractions(commHubEmailSendingClient);
	}

	@Disabled("Test disabled for now to be fixed at a later time")
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
