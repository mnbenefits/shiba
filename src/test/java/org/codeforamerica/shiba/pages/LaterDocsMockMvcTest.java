package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.Document.CAF;
import static org.codeforamerica.shiba.testutilities.TestUtils.getAbsoluteFilepathString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.ServicingAgencyMap;
import org.codeforamerica.shiba.TribalNation;
import org.codeforamerica.shiba.TribalNationRoutingDestination;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.mnit.CountyRoutingDestination;
import org.codeforamerica.shiba.mnit.RoutingDestination;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

public class LaterDocsMockMvcTest extends AbstractShibaMockMvcTest {

	private static final String UPLOADED_JPG_FILE_NAME = "shiba+file.jpg";

	@Autowired
	private RoutingDecisionService routingDecisionService;

	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	@Autowired
	private ServicingAgencyMap<CountyRoutingDestination> countyMap;

	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	@Autowired
	private ServicingAgencyMap<TribalNationRoutingDestination> tribalNationMap;

	@BeforeEach
	protected void setUp() throws Exception {
		super.setUp();
		applicationData.setFlow(FlowType.LATER_DOCS);
		new TestApplicationDataBuilder(applicationData);
		mockMvc.perform(get("/pages/identifyCountyOrTribalNation").session(session)); // start timer

	}

	@Test
	void routeLaterDocsToCorrectCountyOnly() throws Exception {
		//postExpectingSuccess("readyToUploadDocuments");
		postExpectingSuccess("matchInfo", Map.of(
				"firstName", List.of("Dwight"), 
				"lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));
		postExpectingSuccess("identifyCounty", "county", "Aitkin");
		postExpectingSuccess("tribalNationMember", "isTribalNationMember", "false");
	
		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();
		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");
		assertThat(routingDecisionService.getRoutingDestinations(applicationData, CAF))
				.containsExactly(countyMap.get(County.getForName("Aitkin")));
	}
	
	@Test
	void routeLaterDocsToCorrectCountyOnlyEvenWhenIsTribalNationMemberButNotInSpecialScenarios() throws Exception {
		//postExpectingSuccess("readyToUploadDocuments");
		postExpectingSuccess("matchInfo", Map.of(
				"firstName", List.of("Dwight"), 
				"lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));
		postExpectingSuccess("identifyCounty", "county", "Aitkin");
		postExpectingSuccess("tribalNationMember", "isTribalNationMember", "True");
		postExpectingSuccess("selectTheTribe", "selectedTribe", "Red Lake Nation");		
	
		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();
		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");
		assertThat(routingDecisionService.getRoutingDestinations(applicationData, CAF))
				.containsExactly(countyMap.get(County.getForName("Aitkin")));
	}	

	@Test
	void routeLaterDocsToCorrectTribalNationOnly() throws Exception {
		postExpectingSuccess("matchInfo", Map.of(
				"firstName", List.of("Dwight"), 
				"lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));

		postExpectingSuccess("identifyCounty", "county", "Becker");
		postExpectingSuccess("tribalNationMember", "isTribalNationMember", "True");
		postExpectingSuccess("selectTheTribe", "selectedTribe", "White Earth Nation");
		
		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();

		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");
		
		//No county was selected, so the blank county defaults to Hennepin
		/*var countyServicingAgency = County.getForName("Hennepin");
		var countyRoutingDestination = countyMap.get(countyServicingAgency);*/

		var servicingAgency = TribalNation.getFromName("White Earth Nation");
		var tribalRoutingDestination = tribalNationMap.get(servicingAgency);
		List<RoutingDestination> routingDestinations = routingDecisionService.getRoutingDestinations(applicationData,
				CAF);

		assertThat(routingDestinations).containsExactly(tribalRoutingDestination);
	}
	
	@Test
	void routeLaterDocsToCorrectCountyAndTribalNation() throws Exception {
		
		postExpectingSuccess("matchInfo", Map.of(
				"firstName", List.of("Dwight"), 
				"lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));
		
		postExpectingSuccess("identifyCounty", Map.of(
						"county", List.of("Aitkin")));
		postExpectingSuccess("tribalNationMember", Map.of(
						"isTribalNationMember",List.of("True")));
		postExpectingSuccess("selectTheTribe", Map.of(
								"selectedTribe",List.of("Mille Lacs Band of Ojibwe")));				
						
		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();

		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");

		var countyServicingAgency = County.getForName("Aitkin");
		var countyRoutingDestination = countyMap.get(countyServicingAgency);

		var tribalServicingAgency = TribalNation.getFromName("Mille Lacs Band of Ojibwe");
		var tribalRoutingDestination = tribalNationMap.get(tribalServicingAgency);

		List<RoutingDestination> routingDestinations = routingDecisionService.getRoutingDestinations(applicationData,
				CAF);

		assertThat(routingDestinations).containsExactly(countyRoutingDestination, tribalRoutingDestination);
	}
	
	@Test
	void routeLaterDocsToCountyAndTribalNationInCasesofNationBoundary() throws Exception {
		
		postExpectingSuccess("matchInfo", Map.of(
				"firstName", List.of("Dwight"), 
				"lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));
		
		postExpectingSuccess("identifyCounty", Map.of(
						"county", List.of("Beltrami")));
		postExpectingSuccess("tribalNationMember", Map.of(
						"isTribalNationMember",List.of("True")));
		postExpectingSuccess("selectTheTribe", Map.of(
								"selectedTribe",List.of("Red Lake Nation")));				
						
		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();

		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");

		var countyServicingAgency = County.getForName("Beltrami");
		var countyRoutingDestination = countyMap.get(countyServicingAgency);

		var tribalServicingAgency = TribalNation.getFromName("Red Lake Nation");
		var tribalRoutingDestination = tribalNationMap.get(tribalServicingAgency);

		List<RoutingDestination> routingDestinations = routingDecisionService.getRoutingDestinations(applicationData,
				CAF);

		assertThat(routingDestinations).containsExactly(countyRoutingDestination, tribalRoutingDestination);
	}
	
  @ParameterizedTest
  @CsvSource({"Anoka, FondDuLac",
	  "Hennepin, FondDuLac",
	  "Ramsey, FondDuLac",
	  "Anoka, MilleLacsBandOfOjibwe",
	  "Hennepin, MilleLacsBandOfOjibwe",
	  "Ramsey, MilleLacsBandOfOjibwe",
	  "Anoka, GrandPortage",
	  "Hennepin, GrandPortage",
	  "Ramsey, GrandPortage",
	  "Anoka, BoisForte",
	  "Hennepin, BoisForte",
	  "Ramsey, BoisForte",
	  "Anoka, LeechLake",
	  "Hennepin, LeechLake",
	  "Ramsey, LeechLake"})
	void routeLaterDocsToUrbanCountyAndMillLacsTribalNation(String county, String tribalNation) throws Exception {
		
		postExpectingSuccess("matchInfo", Map.of(
				"firstName", List.of("Dwight"), 
				"lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));
		
		postExpectingSuccess("identifyCounty", Map.of(
						"county", List.of(county)));
		postExpectingSuccess("tribalNationMember", Map.of(
						"isTribalNationMember",List.of("True")));
		postExpectingSuccess("selectTheTribe", Map.of(
								"selectedTribe",List.of(tribalNation)));				
						
		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();

		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");

		var countyServicingAgency = County.getForName(county);
		var countyRoutingDestination = countyMap.get(countyServicingAgency);

		var tribalServicingAgency = TribalNation.getFromName("Mille Lacs Band of Ojibwe");
		var tribalRoutingDestination = tribalNationMap.get(tribalServicingAgency);

		List<RoutingDestination> routingDestinations = routingDecisionService.getRoutingDestinations(applicationData,
				CAF);

		assertThat(routingDestinations).containsExactly(countyRoutingDestination, tribalRoutingDestination);
	}
  
  @ParameterizedTest
  @CsvSource({"Aitkin",
	  "Benton",
	  "Chisago",
	  "Crow Wing",
	  "Kanabec",
	  "Mille Lacs",
	  "Morrison",
	  "Pine"})
	void routeLaterDocsToCountyAndMillLacsTribalNation(String county) throws Exception {
		
		postExpectingSuccess("matchInfo", Map.of(
				"firstName", List.of("Dwight"), 
				"lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));
		
		postExpectingSuccess("identifyCounty", Map.of(
						"county", List.of(county)));
		postExpectingSuccess("tribalNationMember", Map.of(
						"isTribalNationMember",List.of("True")));
		postExpectingSuccess("selectTheTribe", Map.of(
								"selectedTribe",List.of("MilleLacsBandOfOjibwe")));				
						
		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();

		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");

		var countyServicingAgency = County.getForName(county);
		var countyRoutingDestination = countyMap.get(countyServicingAgency);

		var tribalServicingAgency = TribalNation.getFromName("Mille Lacs Band of Ojibwe");
		var tribalRoutingDestination = tribalNationMap.get(tribalServicingAgency);

		List<RoutingDestination> routingDestinations = routingDecisionService.getRoutingDestinations(applicationData,
				CAF);

		assertThat(routingDestinations).containsExactly(countyRoutingDestination, tribalRoutingDestination);
	}

	@ParameterizedTest
	@CsvSource({ "BoisForte, true", "FondDuLac, true", "GrandPortage, true", "MilleLacsBandOfOjibwe, true",
			"LowerSioux, true", "PrairieIsland, true", "RedLakeNation, true", "ShakopeeMdewakanton, true",
			"UpperSioux, true", "OtherFederallyRecognizedTribe, true", "LeechLake, false", "WhiteEarthNation, false" })
	void routeLaterDocsToClearwaterAndRedLakeNation(String tribalNation, String routingToClearwaterAndRedLakeNation)
			throws Exception {
		postExpectingSuccess("matchInfo", Map.of("firstName", List.of("Dwight"), "lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));

		postExpectingSuccess("identifyCounty", Map.of("county", List.of("Clearwater")));
		postExpectingSuccess("tribalNationMember", Map.of("isTribalNationMember", List.of("True")));
		postExpectingSuccess("selectTheTribe", Map.of("selectedTribe", List.of(tribalNation)));

		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();

		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");

		var countyServicingAgency = County.getForName("Clearwater");
		var countyRoutingDestination = countyMap.get(countyServicingAgency);

		var tribalServicingAgency = TribalNation.getFromName("Red Lake Nation");
		var tribalRoutingDestination = tribalNationMap.get(tribalServicingAgency);

		List<RoutingDestination> routingDestinations = routingDecisionService.getRoutingDestinations(applicationData,
				Document.UPLOADED_DOC);

		if (Boolean.parseBoolean(routingToClearwaterAndRedLakeNation)) {
			assertThat(routingDestinations).containsExactly(countyRoutingDestination, tribalRoutingDestination);
		} else { // handle the two special cases
			if (tribalNation.equals("LeechLake")) {
				assertThat(routingDestinations).containsExactly(countyRoutingDestination);
			}
			if (tribalNation.equals("WhiteEarthNation")) {
				tribalServicingAgency = TribalNation.getFromName("White Earth Nation");
				tribalRoutingDestination = tribalNationMap.get(tribalServicingAgency);
				assertThat(routingDestinations).containsExactly(tribalRoutingDestination);
			}
		}
	}
	  
	@ParameterizedTest
	@CsvSource({ "BoisForte, true", "FondDuLac, true", "GrandPortage, true", "MilleLacsBandOfOjibwe, true",
			"LowerSioux, true", "PrairieIsland, true", "RedLakeNation, true", "ShakopeeMdewakanton, true",
			"UpperSioux, true", "WhiteEarthNation, true", "OtherFederallyRecognizedTribe, true", "LeechLake, false" })
	void routeLaterDocsToBeltramiAndRedLakeNation(String tribalNation, String routingToBeltramiAndRedLakeNation)
			throws Exception {
		postExpectingSuccess("matchInfo", Map.of("firstName", List.of("Dwight"), "lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));

		postExpectingSuccess("identifyCounty", Map.of("county", List.of("Beltrami")));
		postExpectingSuccess("tribalNationMember", Map.of("isTribalNationMember", List.of("True")));
		postExpectingSuccess("selectTheTribe", Map.of("selectedTribe", List.of(tribalNation)));

		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();

		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");

		var countyServicingAgency = County.getForName("Beltrami");
		var countyRoutingDestination = countyMap.get(countyServicingAgency);

		var tribalServicingAgency = TribalNation.getFromName("Red Lake Nation");
		var tribalRoutingDestination = tribalNationMap.get(tribalServicingAgency);

		List<RoutingDestination> routingDestinations = routingDecisionService.getRoutingDestinations(applicationData,
				Document.UPLOADED_DOC);

		if (Boolean.parseBoolean(routingToBeltramiAndRedLakeNation)) {
			assertThat(routingDestinations).containsExactly(countyRoutingDestination, tribalRoutingDestination);
		} else {
			assertThat(routingDestinations).containsExactly(countyRoutingDestination);
		}
	}
	  
	@ParameterizedTest
	@CsvSource({ "Becker, true", "Mahnomen, true", "Clearwater, true", "Norman, false", "Hennepin, false" })
	void routeLaterDocsToWhiteEarthNation(String county, String routingToWhiteEarthNation)
			throws Exception {
		postExpectingSuccess("matchInfo", Map.of("firstName", List.of("Dwight"), "lastName", List.of("Schrute"),
				"dateOfBirth", List.of("01", "12", "1928")));

		postExpectingSuccess("identifyCounty", Map.of("county", List.of(county)));
		postExpectingSuccess("tribalNationMember", Map.of("isTribalNationMember", List.of("True")));
		postExpectingSuccess("selectTheTribe", Map.of("selectedTribe", List.of("WhiteEarthNation")));

		clickContinueOnInfoPage("howToAddDocuments", "Continue", "uploadDocuments");
		completeLaterDocsUploadFlow();

		clickContinueOnInfoPage("uploadDocuments", "Submit my documents", "documentSubmitConfirmation");

		var countyServicingAgency = County.getForName(county);
		var countyRoutingDestination = countyMap.get(countyServicingAgency);

		var tribalServicingAgency = TribalNation.getFromName("White Earth Nation");
		var tribalRoutingDestination = tribalNationMap.get(tribalServicingAgency);

		List<RoutingDestination> routingDestinations = routingDecisionService.getRoutingDestinations(applicationData,
				Document.UPLOADED_DOC);

		if (Boolean.parseBoolean(routingToWhiteEarthNation)) {
			assertThat(routingDestinations).containsExactly(tribalRoutingDestination);
		} else {
			assertThat(routingDestinations).containsExactly(countyRoutingDestination);
		}
	}

	private void completeLaterDocsUploadFlow() throws Exception {
		String filePath = getAbsoluteFilepathString(UPLOADED_JPG_FILE_NAME);
		var jpgFile = new MockMultipartFile(UPLOADED_JPG_FILE_NAME, new FileInputStream(filePath));
		mockMvc.perform(multipart("/document-upload").file(jpgFile).session(session).with(csrf()));
	}

}
