package org.codeforamerica.shiba.pages;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

public class GoogleAnalyticsTest extends AbstractShibaMockMvcTest {

	@BeforeEach
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * Test Google Analytics on landing page and on outOfStateAddressNotice page
	 */
	@ParameterizedTest
	@CsvSource(value = {
            // The whitespace and commas are removed from the test strings that we pass to the test
			"true, (window,document,'script','dataLayer','GTM-A1B2C3D4'), <noscript><iframe src=\"https://www.googletagmanager.com/ns.html?id=GTM-T7N8SWFD\"",
			"false, <!--GoogleTagManager--><!--EndGoogleTagManager-->, <!--GoogleTagManager(noscript)--><noscript></noscript><!--EndGoogleTagManager(noscript)-->"
	})
	void googleAnalyticsSnippetsShouldBeManagedBySwitch(String gaSwitch, String gaTestString1, String gaTestString2)
			throws Exception {
		System.setProperty("mnb-enable-google-analytics", gaSwitch);
		System.setProperty("google-tag-manager-id", "GTM-A1B2C3D4");  // made up Google tag manager ID
		ResultActions resultActions = mockMvc.perform(get("/pages/landing"));
		MockHttpServletResponse mockHttpServletResponse = resultActions.andReturn().getResponse();

		// Remove whitespace to simplify string comparison
		String pageContent = StringUtils.deleteWhitespace(mockHttpServletResponse.getContentAsString());

		assert (pageContent.contains(gaTestString1));
		// Navigate to the outOfStateAddressNotice page
		mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
		postExpectingSuccess("identifyCountyBeforeApplying", "county", "Hennepin");
		postExpectingSuccess("writtenLanguage",	Map.of("writtenLanguage", List.of("ENGLISH")));
		postExpectingSuccess("spokenLanguage",	Map.of("spokenLanguage", List.of("ENGLISH")));
		postExpectingSuccess("choosePrograms", "programs", List.of("CASH"));
		postExpectingSuccess("personalInfo",
				Map.of("firstName", List.of("Jane"), "lastName", List.of("Doe"), "otherName", List.of(""),
						"dateOfBirth", List.of("01", "12", "1981"), "ssn", List.of("123456789"), "maritalStatus",
						List.of("NEVER_MARRIED"), "sex", List.of("FEMALE"), "livedInMnWholeLife", List.of("true")));
		postExpectingSuccess("homeAddress",
				Map.of("streetAddress", List.of("someStreetAddress"), "apartmentNumber", List.of("someApartmentNumber"),
						"city", List.of("someCity"), "zipCode", List.of("12345"), "state", List.of("WI")));
		resultActions = getPageExpectingSuccess("outOfStateAddressNotice");
		mockHttpServletResponse = resultActions.andReturn().getResponse();

		// Remove whitespace to simplify string comparison
		pageContent = StringUtils.deleteWhitespace(mockHttpServletResponse.getContentAsString());

		assert (pageContent.contains(gaTestString2));
	}
	
}
