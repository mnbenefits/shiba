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

	private EmailJsonDataCreator emailJsonDataCreator;
	private static final String TEST_APP_ID = "12345";

	@BeforeEach
	void setUp() {
		routingDecisionService = mock(RoutingDecisionService.class);
		emailJsonDataCreator = new EmailJsonDataCreator(routingDecisionService);
	}

	@Test
	public void testCreateLaterDocsJsonObject() {
		// Mock Application and ApplicationData
		application = mock(Application.class);
		appData = mock(ApplicationData.class);

		when(application.getApplicationData()).thenReturn(appData);
		when(application.getId()).thenReturn(TEST_APP_ID);
		when(application.getCompletedAt()).thenReturn(ZonedDateTime.now());

		// Mock DocumentListParser behavior
		try (MockedStatic<DocumentListParser> mockedDocumentListParser = mockStatic(DocumentListParser.class)) {
			doc1 = mock(Document.class);
			doc2 = mock(Document.class);

			mockedDocumentListParser.when(() -> DocumentListParser.parse(appData))
					.thenReturn(Arrays.asList(doc1, doc2));

			// Mock RoutingDestination objects
			RoutingDestination countyDestination = mock(RoutingDestination.class);
			RoutingDestination tribalDestination = mock(TribalNationRoutingDestination.class);

			// Set up county destination
			when(countyDestination.getName()).thenReturn("Beltrami County");
			when(countyDestination.getPhoneNumber()).thenReturn("218-333-8300");

			// Set up tribal destination
			when(tribalDestination.getName()).thenReturn("Red Lake Nation");
			when(tribalDestination.getPhoneNumber()).thenReturn("218-679-3350");

			// Set up routing destinations
			when(routingDecisionService.getRoutingDestinations(appData, doc1)).thenReturn(List.of(countyDestination));
			when(routingDecisionService.getRoutingDestinations(appData, doc2)).thenReturn(List.of(tribalDestination));

			// Call the method to test
			Locale locale = new Locale("es");
			String recipientEmail = "test@ex.com";
			JsonObject result = emailJsonDataCreator.createLaterDocsJsonObject(application, recipientEmail, locale);

			// Assertions for JSON properties
			assertEquals("LATER_DOCS_CONFIRMATION", result.get("emailType").getAsString());
			assertEquals("test@ex.com", result.get("recepientEmail").getAsString());
			assertEquals("es", result.get("locale").getAsString());
			assertEquals("12345", result.get("applicationId").getAsString());

			// Verify the content of the routing destinations array
			JsonArray routingDestinationsArray = result.getAsJsonArray("routingDestinations");
			assertEquals(2, routingDestinationsArray.size());

			// Verify county destination

			JsonObject countyJson = routingDestinationsArray.get(0).getAsJsonObject();
			assertEquals("Beltrami County", countyJson.get("name").getAsString());
			assertEquals("218-333-8300", countyJson.get("phoneNumber").getAsString());
			assertEquals("COUNTY", countyJson.get("type").getAsString());

			JsonObject tribalJson = routingDestinationsArray.get(1).getAsJsonObject();
			assertEquals("Red Lake Nation", tribalJson.get("name").getAsString());
			assertEquals("218-679-3350", tribalJson.get("phoneNumber").getAsString());
			assertEquals("TRIBAL", tribalJson.get("type").getAsString());
		}
	}
}