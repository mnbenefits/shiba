package org.codeforamerica.shiba.pages.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.codeforamerica.shiba.TribalNationRoutingDestination;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.DocumentListParser;
import org.codeforamerica.shiba.mnit.RoutingDestination;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.pages.RoutingDecisionService;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/*Test class for isolating the JSON object creation process used in generating data for 
LaterDocs and HealthCare Renewal communications via the Commhub*/
@ExtendWith(MockitoExtension.class)
public class EmailJsonDataCreatorTest {

	@Mock
	private RoutingDecisionService routingDecisionService;

	@Mock
	private Application application;

	@Mock
	private ApplicationData appData;

	@Mock
	private Document doc1, doc2;

	@Mock
	private PageData matchInfoPage;

	@Mock
	private PageData healthcareRenewalMatchInfoPage;

	private EmailJsonDataCreator emailJsonDataCreator;
	private static final String TEST_APP_ID = "12345";

	// Define Common test parameters
	private RoutingDestination countyDestination;
	private RoutingDestination tribalDestination;
	private String recepientEmail;
	private Locale locale;
	private ZonedDateTime completedAt;

	@BeforeEach
	void setUp() {
		routingDecisionService = mock(RoutingDecisionService.class);
		emailJsonDataCreator = new EmailJsonDataCreator(routingDecisionService);

		// Setup basic application data
		completedAt = ZonedDateTime.now();
		when(application.getApplicationData()).thenReturn(appData);
		when(application.getId()).thenReturn(TEST_APP_ID);
		when(application.getCompletedAt()).thenReturn(completedAt);

		// Mock RoutingDestination common objects
		countyDestination = mock(RoutingDestination.class);
		tribalDestination = mock(TribalNationRoutingDestination.class);

		// Set up county destination
		when(countyDestination.getName()).thenReturn("Beltrami County");
		when(countyDestination.getPhoneNumber()).thenReturn("218-333-8300");

		// Set up tribal destination
		when(tribalDestination.getName()).thenReturn("Red Lake Nation");
		when(tribalDestination.getPhoneNumber()).thenReturn("218-679-3350");

		// Common test values
		recepientEmail = "test@ex.com";
		locale = Locale.forLanguageTag("en");
	}

	/* HelperMethod1: Sets up document parser and routing destinations */
	private void setupDocumentParser() {
		doc1 = mock(Document.class);
		doc2 = mock(Document.class);

		when(routingDecisionService.getRoutingDestinations(appData, doc1)).thenReturn(List.of(countyDestination));
		when(routingDecisionService.getRoutingDestinations(appData, doc2)).thenReturn(List.of(tribalDestination));
	}

	/* HelperMethod2: Verifies common JSON properties that both email types share */
	private void verifyCommonJsonProperties(String expectedEmailType, JsonObject result) {
		assertEquals(expectedEmailType, result.get("emailType").getAsString());
		assertEquals(recepientEmail, result.get("recepientEmail").getAsString());
		assertEquals("en", result.get("locale").getAsString());
		assertEquals(TEST_APP_ID, result.get("applicationId").getAsString());
	}

	/* HelperMethod3: Verifies the routing destinations array content */
	private void verifyRoutingDestinations(JsonArray routingDestinationsArray) {
		assertEquals(2, routingDestinationsArray.size());

		// Verify county destination
		JsonObject countyJson = routingDestinationsArray.get(0).getAsJsonObject();
		assertEquals("Beltrami County", countyJson.get("name").getAsString());
		assertEquals("218-333-8300", countyJson.get("phoneNumber").getAsString());
		assertEquals("COUNTY", countyJson.get("type").getAsString());

		// Verify tribal destination
		JsonObject tribalJson = routingDestinationsArray.get(1).getAsJsonObject();
		assertEquals("Red Lake Nation", tribalJson.get("name").getAsString());
		assertEquals("218-679-3350", tribalJson.get("phoneNumber").getAsString());
		assertEquals("TRIBAL", tribalJson.get("type").getAsString());
	}

	@Test
	public void testCreateLaterDocsJsonObject() {
		try (MockedStatic<DocumentListParser> mockedDocumentListParser = mockStatic(DocumentListParser.class)) {
			setupDocumentParser();
			mockedDocumentListParser.when(() -> DocumentListParser.parse(appData))
					.thenReturn(Arrays.asList(doc1, doc2));

			// Mock InputData for first and last names
			InputData firstNameData = mock(InputData.class);
			InputData lastNameData = mock(InputData.class);

			// Setup the getValue behavior
			when(firstNameData.getValue(0)).thenReturn("John");
			when(lastNameData.getValue(0)).thenReturn("Doe");

			// Setup the page data returns
			when(matchInfoPage.get("firstName")).thenReturn(firstNameData);
			when(matchInfoPage.get("lastName")).thenReturn(lastNameData);
			when(appData.getPageData("matchInfo")).thenReturn(matchInfoPage);

			JsonObject result = emailJsonDataCreator.createLaterDocsJsonObject(application, recepientEmail, locale);

			verifyCommonJsonProperties("LATER_DOCS_CONFIRMATION", result);
			verifyRoutingDestinations(result.getAsJsonArray("routingDestinations"));
			assertEquals("John", result.get("firstName").getAsString());
			assertEquals("Doe", result.get("lastName").getAsString());
		}
	}

	@Test
	public void testCreateHealthcareRenewalJsonObject() {
		try (MockedStatic<DocumentListParser> mockedDocumentListParser = mockStatic(DocumentListParser.class)) {
			setupDocumentParser();
			mockedDocumentListParser.when(() -> DocumentListParser.parse(appData))
					.thenReturn(Arrays.asList(doc1, doc2));

			// Mock InputData for first and last names
			InputData firstNameData = mock(InputData.class);
			InputData lastNameData = mock(InputData.class);

			// Setup the getValue behavior
			when(firstNameData.getValue(0)).thenReturn("Jane");
			when(lastNameData.getValue(0)).thenReturn("Smith");

			// Setup the page data returns
			when(healthcareRenewalMatchInfoPage.get("firstName")).thenReturn(firstNameData);
			when(healthcareRenewalMatchInfoPage.get("lastName")).thenReturn(lastNameData);
			when(appData.getPageData("healthcareRenewalMatchInfo")).thenReturn(healthcareRenewalMatchInfoPage);

			JsonObject result = emailJsonDataCreator.createHealthcareRenewalJsonObject(application, recepientEmail,
					locale);

			verifyCommonJsonProperties("HEALTHCARE_RENEWAL", result);
			verifyRoutingDestinations(result.getAsJsonArray("routingDestinations"));

			assertEquals("Jane", result.get("firstName").getAsString());
			assertEquals("Smith", result.get("lastName").getAsString());
		}
	}
}