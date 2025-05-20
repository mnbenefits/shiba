package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.County.Beltrami;
import static org.codeforamerica.shiba.Program.CASH;
import static org.codeforamerica.shiba.Program.CCAP;
import static org.codeforamerica.shiba.Program.EA;
import static org.codeforamerica.shiba.Program.GRH;
import static org.codeforamerica.shiba.Program.SNAP;
import static org.codeforamerica.shiba.TribalNation.OtherFederallyRecognizedTribe;
import static org.codeforamerica.shiba.TribalNation.RedLakeNation;
import static org.codeforamerica.shiba.TribalNation.WhiteEarthNation;
import static org.codeforamerica.shiba.output.Document.CAF;
import static org.codeforamerica.shiba.output.Document.UPLOADED_DOC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.ServicingAgencyMap;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.mnit.CountyRoutingDestination;
import org.codeforamerica.shiba.mnit.RoutingDestination;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.pages.enrichment.Address;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.codeforamerica.shiba.testutilities.FormPage;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

public class TribalNationsMockMvcTest extends AbstractShibaMockMvcTest {

  @Autowired
  private RoutingDecisionService routingDecisionService;
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private ServicingAgencyMap<CountyRoutingDestination> countyMap;

  @BeforeEach
  protected void setUp() throws Exception {
    super.setUp();
    new TestApplicationDataBuilder(applicationData)
        .withPageData("identifyCounty", "county", "Hennepin");
    mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
    postExpectingSuccess("writtenLanguage", Map.of("writtenLanguage", List.of("ENGLISH")));
    postExpectingSuccess("spokenLanguage", Map.of("spokenLanguage", List.of("ENGLISH")));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "Bois Forte,Hennepin",
      "Fond Du Lac,Hennepin",
      "Grand Portage,Hennepin",
      "Leech Lake,Hennepin",
      "Mille Lacs Band of Ojibwe,Hennepin",
      "White Earth Nation,Hennepin",
      "Bois Forte,Anoka",
      "Fond Du Lac,Anoka",
      "Grand Portage,Anoka",
      "Leech Lake,Anoka",
      "Mille Lacs Band of Ojibwe,Anoka",
      "White Earth Nation,Anoka",
      "Bois Forte,Ramsey",
      "Fond Du Lac,Ramsey",
      "Grand Portage,Ramsey",
      "Leech Lake,Ramsey",
      "Mille Lacs Band of Ojibwe,Ramsey",
      "White Earth Nation,Ramsey",
      "Mille Lacs Band of Ojibwe,Aitkin",
      "Mille Lacs Band of Ojibwe,Benton",
      "Mille Lacs Band of Ojibwe,Chisago",
      "Mille Lacs Band of Ojibwe,CrowWing",
      "Mille Lacs Band of Ojibwe,Kanabec",
      "Mille Lacs Band of Ojibwe,Morrison",
      "Mille Lacs Band of Ojibwe,MilleLacs",
      "Mille Lacs Band of Ojibwe,Pine"
  })
  void shouldAddTribalTanfAndRouteCAFToMilleLacsAndCCAPToCounty(
      String tribalNation, String county)
      throws Exception {
	fillOutPersonalInfo();
    addHouseholdMembersWithProgram("CCAP");
    goThroughLongTribalTanfFlow(tribalNation, county, "true", CCAP);
    assertRoutingDestinationIsCorrectForDocument(CAF, "Mille Lacs Band of Ojibwe");
    String countyName = County.getForName(county).toString();
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, "Mille Lacs Band of Ojibwe",
    		countyName);
    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, countyName);
  }

	/**
	 * This test verifies that the routing destination is White Earth Nation when
	 * the applicant is a tribal member of White Earth Nation and resides in one of
	 * the counties serviced by WEN.
	 * 
	 * @param county
	 * @param program
	 * @throws Exception
	 */
	@ParameterizedTest
	@CsvSource(value = { "Becker, SNAP", "Becker, EA", "Becker, CASH", "Becker, CCAP", "Becker, GRH",
			"Mahnomen, SNAP", "Mahnomen, EA", "Mahnomen, CASH", "Mahnomen, CCAP", "Mahnomen, GRH",
			"Clearwater, SNAP", "Clearwater, EA", "Clearwater, CASH", "Clearwater, CCAP", "Clearwater, GRH" })
	void routeWhiteEarthApplicationsToWhiteEarthOnly(String county, String program) throws Exception {
	    addHouseholdMembersWithProgram(program);
	    goThroughNationOfResidence(county, "White Earth Nation", new String[]{program});

    Document expectedDocType;
    if (program.equals("CCAP")) {
    	expectedDocType = Document.CCAP;
        assertRoutingDestinationIsCorrectForDocument(Document.CCAP, WhiteEarthNation.toString());
    } else {
    	expectedDocType = Document.CAF;
    	assertRoutingDestinationIsCorrectForDocument(CAF, WhiteEarthNation.toString());
    }
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, WhiteEarthNation.toString());

    var routingDestinations = routingDecisionService.getRoutingDestinations(applicationData, expectedDocType);
    RoutingDestination routingDestination = routingDestinations.get(0);
    assertThat(routingDestination.getDhsProviderId()).isEqualTo("A086642300");
    assertThat(routingDestination.getEmail()).isEqualTo("mnbenefits@state.mn.us");
    assertThat(routingDestination.getPhoneNumber()).isEqualTo("218-935-2359");
  }

  /**
   * This test verifies that the routing destination will include White Earth Nation when
   * the applicant is not a tribal member but rather a lineal descendant of White Earth Nation and
   * resides in one of the counties serviced by WEN.
   * @param county
   * @throws Exception
   */
  @ParameterizedTest
  @CsvSource(value = {"Becker", "Mahnomen", "Clearwater"})
  void shouldRouteWhiteEarthLinealDescendantApplicationsToWhiteEarth(String county) throws Exception {
		when(featureFlagConfiguration.get("WEN-lineal-descendant")).thenReturn(FeatureFlag.ON); 
	    postExpectingRedirect("identifyCountyBeforeApplying", "county", county, "prepareToApply");
	    postExpectingRedirect("choosePrograms", "programs", "CCAP, SNAP", "introBasicInfo");
	    postExpectingRedirect("tribalNationMember", "isTribalNationMember", "false", "linealDescendantWEN");
	    postExpectingRedirect("linealDescendantWEN", "linealDescendantWEN", "true", "introIncome");

	    assertRoutingDestinationIsCorrectForDocument(CAF, WhiteEarthNation.toString());
	    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, WhiteEarthNation.toString());

	    var routingDestinations = routingDecisionService.getRoutingDestinations(applicationData, CAF);
	    RoutingDestination routingDestination = routingDestinations.get(0);
	    assertThat(routingDestination.getDhsProviderId()).isEqualTo("A086642300");
	    assertThat(routingDestination.getEmail()).isEqualTo("mnbenefits@state.mn.us");
	    assertThat(routingDestination.getPhoneNumber()).isEqualTo("218-935-2359");
  }

  /**
   * This test verifies input requirements that allow the linealDescendantWhiteEarthNation page to be displayed,
   * i.e., county of residence: Becker, Clearwater, Mahnomen
   *       tribal member: No
   * @param county
   * @throws Exception
   */
  @ParameterizedTest
  @CsvSource(value = {"Becker", "Mahnomen", "Clearwater"})
  void shouldShowLinealDescendantWENPageWhenNotTribalMember(String county) throws Exception {
	when(featureFlagConfiguration.get("WEN-lineal-descendant")).thenReturn(FeatureFlag.ON); 
	// The expected sequence of pages is illustrated below, the test will post data for the pages
	// that contain data that affects navigation to the linealDescendantWEN page.

    // identifyCountyBeforeApplying
    postExpectingRedirect("identifyCountyBeforeApplying", "county", county, "prepareToApply");
    // tribalNationMember
    postExpectingRedirect("tribalNationMember", "isTribalNationMember", "false", "linealDescendantWEN");
    // linealDescendantWEN
    postExpectingRedirect("linealDescendantWEN", "linealDescendantWEN", "true", "introIncome");
  }

  /**
   * This test is a variation of test shouldShowLinealDescendantWENPageWhenNotTribalMember, whose
   * purpose is to verify that the alternative "household version" of the linealDescendantWEN page
   * header is displayed when the application has a household member.
   * @throws Exception
   */
  @Test
  void shouldShowHouseholdVersionOfLinealDescendantWENPage() throws Exception {
	when(featureFlagConfiguration.get("WEN-lineal-descendant")).thenReturn(FeatureFlag.ON); 
	// This test posts data for the pages that contain data that affects navigation
	// to the linealDescendantWEN page and the text displayed on the page.

    // identifyCountyBeforeApplying
    postExpectingRedirect("identifyCountyBeforeApplying", "county", "Becker", "prepareToApply");
    // add a household
    this.addHouseholdMembersWithProgram(SNAP);
    // tribalNationMember
    var nextPage = postAndFollowRedirect("tribalNationMember", "isTribalNationMember", "false");
    // verify the page header text is present
    assertThat(nextPage.getElementTextById("page-header")).isEqualTo("Is anyone in your household a lineal descendant of the White Earth Nation?");
    // verify that the help text (in the reveal) is present
    assertThat(nextPage.getElementTextById("reveal-content")).isEqualTo("A lineal descendant is anyone who can trace their ancestry directly to a White Earth Nation tribal member.");
  }

  /**
   * This test verifies that the linealDescendantWEN page is not displayed when the county of residence
   * is Becker, Clearwater or Mahnomen but tribal membership is Yes
   * @param county
   * @throws Exception
   */
  @ParameterizedTest
  @CsvSource(value = {"Becker", "Mahnomen", "Clearwater"})
  void shouldNotShowLinealDescendantWENPageWhenTribalMember(String county) throws Exception {
	when(featureFlagConfiguration.get("WEN-lineal-descendant")).thenReturn(FeatureFlag.ON); 
	// The expected sequence of pages illustrated below, this test will post data for the pages
	// that contain data that affects navigation to the linealDescendantWEN page.

    // identifyCountyBeforeApplying
    postExpectingRedirect("identifyCountyBeforeApplying", "county", county, "prepareToApply");
    // choosePrograms
    postExpectingRedirect("choosePrograms", "programs", "CASH", "introBasicInfo");
    // addHouseholdMembers
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "false", "introPersonalDetails");
    // tribalNationMember
    postExpectingRedirect("tribalNationMember", "isTribalNationMember", "true", "selectTheTribe");
    // selectTheTribe
    postExpectingRedirect("selectTheTribe","selectedTribe", "White Earth Nation", "nationsBoundary");
    // nationsBoundary
	postExpectingRedirect("nationsBoundary", "livingInNationBoundary", "true", "nationOfResidence");
	// nationOfResidence
	postExpectingRedirect("nationOfResidence", "selectedNationOfResidence", "White Earth Nation", "introIncome");
  }

  /**
   * This test verifies that the routing destination is the county of residence when
   * the applicant is a tribal member of White Earth Nation but does not resides in one of
   * the counties serviced by WEN.
   * 
   * @param county
   * @param program
   * @throws Exception
   */
  @ParameterizedTest
  @CsvSource(value = { "Nobles, SNAP", "Nobles, EA", "Nobles, CASH", "Nobles, CCAP", "Nobles, GRH",
		  "Scott, SNAP", "Scott, EA", "Scott, CASH", "Scott, CCAP", "Scott, GRH",
		  "Meeker, SNAP", "Meeker, EA", "Meeker, CASH", "Meeker, CCAP", "Meeker, GRH" })
  void routeWhiteEarthApplicationsToCountyOnly(String county, String program) throws Exception {
    addHouseholdMembersWithProgram(program);
    goThroughNationOfResidence(county, "White Earth Nation", new String[]{program});

    Document expectedDocType;
    if (program.equals("CCAP")) {
    	expectedDocType = Document.CCAP;
        assertRoutingDestinationIsCorrectForDocument(Document.CCAP, county);
    } else {
    	expectedDocType = Document.CAF;
    	assertRoutingDestinationIsCorrectForDocument(CAF, county);
    }
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, county);
  }

  @ParameterizedTest
  @CsvSource(value = {
      "Hennepin,true,Hennepin;Mille Lacs Band of Ojibwe", // separate multiple destinations with ";"
      "Ramsey,true,Ramsey;Mille Lacs Band of Ojibwe",
      "Anoka,true,Anoka;Mille Lacs Band of Ojibwe",
      "Hennepin,false,Hennepin",
      "Ramsey,false,Ramsey",
      "Anoka,false,Anoka",
  })
  void routeUrbanWhiteEarthApplicationsForOnlyEaAndTribalTanf(String county,
      String applyForTribalTANF,
      String destinationsNames) throws Exception {
	String[] destinationsNamesArray = destinationsNames.split(";");
 
    addHouseholdMembersWithProgram("EA");

    goThroughLongTribalTanfFlow(WhiteEarthNation.toString(), county, applyForTribalTANF, EA);

    assertRoutingDestinationIsCorrectForDocument(CAF, destinationsNamesArray);
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, destinationsNamesArray);
  }


  @Test
  void clientsFromOtherFederallyRecognizedNationsShouldBeAbleToApplyForTribalTanfAndRouteToRedLake()
      throws Exception {

    addHouseholdMembersWithProgram("EA");
    goThroughLongTribalTanfFlow(OtherFederallyRecognizedTribe.toString(), Beltrami.toString(), "true", EA);
    assertRoutingDestinationIsCorrectForDocument(CAF, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, RedLakeNation.toString());

  }

  @Test
  void clientsFromOtherFederallyRecognizedNationsShouldBeRoutedToRedLakeEvenIfTheyAreNotApplyingForTanf()
      throws Exception {
    addHouseholdMembersWithProgram("EA");
    goThroughLongTribalTanfFlow(OtherFederallyRecognizedTribe.toString(), Beltrami.toString(), "false", EA);
    assertRoutingDestinationIsCorrectForDocument(CAF, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, RedLakeNation.toString());

  }

  @Test
  void clientsFromOtherFederallyRecognizedNationsShouldBeAbleToApplyForMFIPAndRouteToCounty()
      throws Exception {
	applicationData.setFlow(FlowType.FULL);
    addHouseholdMembersWithProgram("EA");
    goThroughLongTribalTanfFlow(OtherFederallyRecognizedTribe.toString(), Beltrami.toString(), "false", EA);
    assertRoutingDestinationIsCorrectForDocument(CAF, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, RedLakeNation.toString());
  }

  @ParameterizedTest
  @CsvSource(value = {
      "Red Lake Nation,Hennepin",
      "Shakopee Mdewakanton,Hennepin"
  })
  void shouldGetBootedFromTheFlowAndSentToCountyIfLivingOutsideOfNationBoundary(String nationName,
      String county)
      throws Exception {
    addHouseholdMembersWithProgram("EA");

    getToPersonalInfoScreen(CCAP, SNAP, CASH, EA);

    addAddressInGivenCounty("Hennepin");
    postExpectingRedirect("tribalNationMember",
        "isTribalNationMember",
        "true",
        "selectTheTribe");
    postExpectingRedirect("selectTheTribe", "selectedTribe", nationName, "nationsBoundary");
    postExpectingRedirect("nationsBoundary",
        "livingInNationBoundary",
        "false",
        "introIncome");

    assertRoutingDestinationIsCorrectForDocument(CAF, county);
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, county);
    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, county);
  }

  @ParameterizedTest
  @CsvSource(value = {
      "Prairie Island,Hennepin,true",
      "Shakopee Mdewakanton,Hennepin,true",
      "Lower Sioux,Ramsey,true",
      "Upper Sioux,Ramsey,true"
  })

  /**
   * This test is verifying that Red Lake Nation and county will get a copy of the application documents when:
   *  - the county of residence is Clearwater
   *  - the selected programs include SNAP, CASH, CCAP and EA (GRH is not include in this test)
   *    Note: SNAP, EA, CCAP, TANF -> RLN and CASH, GRH -> Clearwater
   *  - tribal membership is Red Lake Nation
   *  - applicant says Yes to living in Tribal Nations boundary 
   *  - selects Red Lake Nation as the resident Tribal Nation 
   *  - the response to "do you want to apply for Tribal TANF" is "Yes"
   * 
   * Note: In order to get the applyForTribalTANF page when the Tribal Nation is Red Lake Nation the
   * following conditions must also be met:
   *  - someone in the household must be pregnant or less than age 18
   *  - the county of residence must be either Beltrami or Clearwater
   * @throws Exception
   */
  @Test
  void redLakeApplicationsWithoutGrhGetSentToRedLake() throws Exception {
    addHouseholdMembersWithProgram("EA");
    String county = "Clearwater";
    goThroughLongTribalTanfFlow(RedLakeNation.toString(), county, "true", CCAP, SNAP, CASH, EA);

    assertRoutingDestinationIsCorrectForDocument(CAF, RedLakeNation.toString(),county);
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, RedLakeNation.toString(),county);
    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, RedLakeNation.toString());
  }

  /**
   * This test is verifying that only Red Lake Nation will get a copy of the application documents when:
   *  - the selected programs include only SNAP
   *  - tribal membership is Red Lake Nation
   *  - living in Red Lake Nation boundaries
   *  - the response to "do you want to apply for Tribal TANF" is "No"
   * 
   * Note: In order to get the applyForTribalTANF page when the Tribal Nation is Red Lake Nation the
   * following conditions must also be met:
   *  - someone in the household must be pregnant or less than age 18
   *  - the county of residence must be either Beltrami or Clearwater
   * @throws Exception
   */
  @Test
  void redLakeApplicationsWithOnlySnapGetSentToRedLake() throws Exception {
    fillOutPersonalInfo();
    addHouseholdMembersWithProgram(SNAP);
    goThroughLongTribalTanfFlow(RedLakeNation.toString(), "Clearwater", "false", SNAP);

    assertRoutingDestinationIsCorrectForDocument(CAF, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, RedLakeNation.toString());
  }

  /**
   * This test is verifying that both Red Lake Nation and Red Lake County will get a copy of
   * the application documents when:
   *  - the selected programs include only GRH (which is processed by the county)
   *  - tribal membership is Red Lake Nation
   *  - the response to "do you want to apply for Tribal TANF" is "Yes" (which is processed by the tribe)
   * 
   * Note: In order to get the applyForTribalTANF page when the Tribal Nation is Red Lake Nation the
   * following conditions must also be met:
   *  - someone in the household must be pregnant or less than age 18
   *  - the county of residence must be either Beltrami or Clearwater
   * @throws Exception
   */
  @Test
  void redLakeApplicationsWithGrhAndTribalTanfGetSentToRedLakeAndCounty() throws Exception {
    fillOutPersonalInfo();
    addHouseholdMembersWithProgram("GRH");

    String county = "Clearwater";
    goThroughLongTribalTanfFlow(RedLakeNation.toString(), county, "true", GRH);
    assertRoutingDestinationIsCorrectForDocument(CAF, county, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, county,
        RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(Document.XML, county, RedLakeNation.toString());
  }

  /**
   * This test is verifying that both Red Lake Nation and Clearwater County will get a copy of
   * the application documents when:
   *  - the county of Residence is Clearwater County
   *  - the selected programs include both GRH and SNAP
   *  - they live within the boundaries of Red Lake Nation
   *  - tribal membership is Red Lake Nation
   *  - the response to "do you want to apply for Tribal TANF" is "Yes"
   * 
   * Note: In order to get the applyForTribalTANF page when the Tribal Nation is Red Lake Nation the
   * following conditions must also be met:
   *  - someone in the household must be pregnant or less than age 18
   *  - the county of residence must be either Beltrami or Clearwater
   * @throws Exception
   */
	@Test
	void redLakeApplicationsWithGrhAndSnapAndTribalTanfGetSentToRedLakeAndCounty() throws Exception {
		fillOutPersonalInfo();
		addHouseholdMembersWithProgram(SNAP); // adds a spouse and a child with program SNAP

		County countyOfResidence = County.Clearwater;
		goThroughLongTribalTanfFlow(RedLakeNation.toString(), countyOfResidence.toString(), "true", GRH);
		// The 3 documents that could be generated with the given inputs are: CAF, UPLOADED_DOC, XML
		assertRoutingDestinationIsCorrectForDocument(CAF, RedLakeNation.toString(), countyOfResidence.toString());
		assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, countyOfResidence.toString(), RedLakeNation.toString(),
				countyOfResidence.toString());
		assertRoutingDestinationIsCorrectForDocument(Document.XML, countyOfResidence.toString(), RedLakeNation.toString(),
				countyOfResidence.toString());
	}

  /**
   * This test is verifying that both Red Lake Nation and Clearwater County will get a copy of
   * the application documents when:
   *  - the county of Residence is Clearwater County
   *  - the selected programs include both GRH and CCAP (GRH to county, CCAP to RLN)  
   *  - they live within the boundaries of Red Lake Nation
   *  - tribal membership is Red Lake Nation
   *  - the response to "do you want to apply for Tribal TANF" is "No"
   * 
   * Note: In order to get the applyForTribalTANF page when the Tribal Nation is Red Lake Nation the
   * following conditions must also be met:
   *  - someone in the household must be pregnant or less than age 18
   *  - the county of residence must be either Beltrami or Clearwater
   * @throws Exception
   */
  @Test
  void redLakeApplicationsWithOnlyGrhAndCcapGetSentToRedLakeAndCounty() throws Exception {
    fillOutPersonalInfo();
    addHouseholdMembersWithProgram(CCAP);

    String county = "Clearwater";
    goThroughLongTribalTanfFlow(RedLakeNation.toString(), county, "false", GRH);

    assertRoutingDestinationIsCorrectForDocument(CAF, county); // CAF w\GRH to county
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, county,
        RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(Document.XML, county, RedLakeNation.toString());
    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, RedLakeNation.toString()); // CCAP to RLN
  }

  @Test
  void redLakeApplicationsWithGrhOnlyGetSentToCounty() throws Exception {
    addHouseholdMembersWithProgram("GRH");
    getToPersonalInfoScreen(GRH);
    fillOutPersonalInfo();
    String county = "Anoka";
    addAddressInGivenCounty(county);
    postExpectingRedirect("tribalNationMember",
        "isTribalNationMember",
        "true",
        "selectTheTribe");

    postExpectingRedirect("selectTheTribe", "selectedTribe", RedLakeNation.toString(),
        "nationsBoundary");
    postExpectingRedirect("nationsBoundary",
        "livingInNationBoundary",
        "false",
        "introIncome");

    assertRoutingDestinationIsCorrectForDocument(CAF, county);
    assertRoutingDestinationIsCorrectForDocument(UPLOADED_DOC, county);
    assertRoutingDestinationIsCorrectForDocument(Document.CCAP, county);
  }

	/**
	 * This test verifies: 
	 * 1) if "true" is selected on the tribalNationMember page then the next page is selectTheTribe 
	 * 2) the exact content of the select list on the selectTheTribe page 
	 * 3) that regardless of the selectedTribe input value the next page is always nationsBoundary
	 * 
	 * @throws Exception
	 */
	@Test
	void selectTheTribeFlowsToNationsBoundary() throws Exception {
		// start with the tribalNationMember page, we do not need preceding inputs
		// tribalNationMember
		var nextPage = postAndFollowRedirect("tribalNationMember", "isTribalNationMember", "true");
		assertThat(nextPage.getTitle()).isEqualTo("Select a Tribal Nation");
		List<String> selectedTribeOptions = nextPage.getElementById("selectedTribe").getElementsByTag("option")
				.eachText();
		assertThat(selectedTribeOptions).containsExactly("Select a Tribal Nation", "Bois Forte", "Fond Du Lac",
				"Grand Portage", "Leech Lake", "Lower Sioux", "Mille Lacs Band of Ojibwe", "Prairie Island",
				"Red Lake Nation", "Shakopee Mdewakanton", "Upper Sioux", "White Earth Nation",
				"Other federally recognized tribe");

		// selectTheTribe
		nextPage = postAndFollowRedirect("selectTheTribe", "selectedTribe", "any selected tribe");
		assertThat(nextPage.getTitle()).isEqualTo("Nations Boundary");
	}
	
	/**
	 * Tests when "Yes" is selected on Nations Boundary page, the next page will be the nationOfResidence page.
	 * @throws Exception
	 */
	@Test
	void whenLivesInNationsBoundaryNextPageIsNationOfResidence() throws Exception {
		// start with the tribalNationMember page, we do not need preceding inputs
		var nextPage = postAndFollowRedirect("tribalNationMember", "isTribalNationMember", "true");
		nextPage = postAndFollowRedirect("selectTheTribe", "selectedTribe", "any selected tribe");
		assertThat(nextPage.getTitle()).isEqualTo("Nations Boundary");
		nextPage = postAndFollowRedirect("nationsBoundary", "livingInNationBoundary", "true");
		assertThat(nextPage.getTitle()).isEqualTo("Tribal Nation of residence");
	}

	/**
	 * This test verifies the exact content of the select list on the
	 * nationOfResidence page
	 * 
	 * @throws Exception
	 */
	@Test
	void nationOfResidenceOptions() throws Exception {
		var page = new FormPage(getPage("nationOfResidence"));
		assertThat(page.getTitle()).isEqualTo("Tribal Nation of residence");
		List<String> selectedNationOfResidenceOptions = page.getElementById("selectedNationOfResidence")
				.getElementsByTag("option").eachText();
		assertThat(selectedNationOfResidenceOptions).containsExactly("Select a Tribal Nation", "Bois Forte",
				"Fond Du Lac", "Grand Portage", "Leech Lake", "Lower Sioux", "Mille Lacs Band of Ojibwe",
				"Prairie Island", "Red Lake Nation", "Shakopee Mdewakanton", "Upper Sioux", "White Earth Nation");
	}


	/**
	 * This test verifies correct next page flow from the nationOfResidence page.
	 * When the household includes someone who is pregnant and/or a child under age 18
	 * AND
	 * 1) a household member is a member of the Mille Lacs Band of Ojibwe 
	 *    AND the household lives in one of (Aitkin, Benton, Chisago, Crow Wing, Kanabec, Mille Lacs, Morrison, Pine) counties
	 * 2) OR a household member is a member of any of (Leech Lake, White Earth, Bois Forte, Grand Portage, Fond du Lac, 
	 *    Mille Lacs Band of Ojibwe) AND the household lives in one of (Anoka, Hennepin, Ramsey)
	 * 3) OR a household member is a member of any Tribal Nation other than Leech Lake AND the household lives in Beltrami County
	 * 4) OR they indicate they live within the boundaries of Red Lake Nation 
	 *    AND the county of residence is Beltrami or Clearwater County (member of any Tribal Nation)
	 * THEN the next page is applyForTribalTANF 
	 * 5) OTHERWISE the next page is introIncome
	 * 
	 * @throws Exception
	 */
	@ParameterizedTest
	@CsvSource(value = {
			// Test cases when the household includes someone who is pregnant and/or a child under age 18
			// case 1
			"Aitkin, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF",
			"Benton, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF",
			"Chisago, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF",
			"CrowWing, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF",
			"Kanabec, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF",
			"Morrison, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF",
			"MilleLacs, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF", 
			"Pine, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF",
			// case 2
			"Anoka, Bois Forte, true, apply for Tribal TANF",
			"Hennepin, Fond Du Lac, true, apply for Tribal TANF",
			"Ramsey, Grand Portage, true, apply for Tribal TANF",
			"Anoka, Leech Lake, true, apply for Tribal TANF",
			"Hennepin, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF",
			"Ramsey, White Earth Nation, true, apply for Tribal TANF",
			// case 3
			"Beltrami, Bois Forte, true, apply for Tribal TANF",
			"Beltrami, Fond Du Lac, true, apply for Tribal TANF",
			"Beltrami, Grand Portage, true, apply for Tribal TANF",
			"Beltrami, Mille Lacs Band of Ojibwe, true, apply for Tribal TANF",
			"Beltrami, White Earth Nation, true, apply for Tribal TANF",
			"Beltrami, Lower Sioux, true, apply for Tribal TANF",
			"Beltrami, Upper Sioux, true, apply for Tribal TANF",
			"Beltrami, Prairie Island, true, apply for Tribal TANF",
			"Beltrami, Shakopee Mdewakanton, true, apply for Tribal TANF",
 			// case 4
			"Beltrami, Red Lake Nation, true, apply for Tribal TANF",
			"Clearwater, Red Lake Nation, true, apply for Tribal TANF",
			// case 5
			"Clay, Shakopee Mdewakanton, true, Intro: Income",
			// Repeat test cases for household that DOES NOT include someone who is pregnant and/or a child under age 18
			// case 1
			"Aitkin, Mille Lacs Band of Ojibwe, false, Intro: Income",
			"Benton, Mille Lacs Band of Ojibwe, false, Intro: Income",
			"Chisago, Mille Lacs Band of Ojibwe, false, Intro: Income",
			"Crow Wing, Mille Lacs Band of Ojibwe, false, Intro: Income",
			"Kanabec, Mille Lacs Band of Ojibwe, false, Intro: Income",
			"Morrison, Mille Lacs Band of Ojibwe, false, Intro: Income",
			"Mille Lacs, Mille Lacs Band of Ojibwe, false, Intro: Income",
			"Pine, Mille Lacs Band of Ojibwe, false, Intro: Income",
			// case 2
			"Anoka, Bois Forte, false, Intro: Income",
			"Hennepin, Fond Du Lac, false, Intro: Income",
			"Ramsey, Grand Portage, false, Intro: Income",
			"Anoka, Leech Lake, false, Intro: Income",
			"Hennepin, Mille Lacs Band of Ojibwe, false, Intro: Income",
			"Ramsey, White Earth Nation, false, Intro: Income",
			// case 3
			"Beltrami, Bois Forte, false, Intro: Income",
			"Beltrami, Fond Du Lac, false, Intro: Income",
			"Beltrami, Grand Portage, false, Intro: Income",
			"Beltrami, Mille Lacs Band of Ojibwe, false, Intro: Income",
			"Beltrami, White Earth Nation, false, Intro: Income",
			"Beltrami, Lower Sioux, false, Intro: Income",
			"Beltrami, Upper Sioux, false, Intro: Income",
			"Beltrami, Prairie Island, false, Intro: Income",
			"Beltrami, Shakopee Mdewakanton, false, Intro: Income",
				// case 4
			"Beltrami, Red Lake Nation, false, Intro: Income",
			"Clearwater, Red Lake Nation, false, Intro: Income",
			// case 5
			"Clay, Shakopee Mdewakanton, false, Intro: Income" })
	void nationOfResidenceNavigatesToCorrectNextPage(String county, String tribe, String pregnantOrChildUnderAge18,
			String nextPageTitle) throws Exception {
		postExpectingRedirect("identifyCountyBeforeApplying", "county", county, "prepareToApply");
		postExpectingRedirect("choosePrograms", "programs", "SNAP", "expeditedNotice");

		FormPage nextPage;
		if (Boolean.valueOf(pregnantOrChildUnderAge18)) {
			// addHouseholdMember
			nextPage = postAndFollowRedirect("addHouseholdMembers", "addHouseholdMembers", "false");
			assertThat(nextPage.getTitle()).isEqualTo("Intro: Personal Details");
			// pregnant
			nextPage = postAndFollowRedirect("pregnant", "isPregnant", "true");
			assertThat(nextPage.getTitle()).isEqualTo("Expedited Migrant Farm Worker, 1 person");

		} else {
			// addHouseholdMember
			nextPage = postAndFollowRedirect("addHouseholdMembers", "addHouseholdMembers", "true");
			assertThat(nextPage.getTitle()).isEqualTo("Start Household");
			// pregnant
			nextPage = postAndFollowRedirect("pregnant", "isPregnant", "false");
			assertThat(nextPage.getTitle()).isEqualTo("Expedited Migrant Farm Worker, Household");
		}

		// tribalNationMember
		nextPage = postAndFollowRedirect("tribalNationMember", "isTribalNationMember", "true");
		assertThat(nextPage.getTitle()).isEqualTo("Select a Tribal Nation");

		// selectTheTribe
		nextPage = postAndFollowRedirect("selectTheTribe", "selectedTribe", tribe);
		assertThat(nextPage.getTitle()).isEqualTo("Nations Boundary");

		// nationsBoundary
		nextPage = postAndFollowRedirect("nationsBoundary", "livingInNationBoundary", "true");
		assertThat(nextPage.getTitle()).isEqualTo("Tribal Nation of residence");

		// nationOfResidence
		nextPage = postAndFollowRedirect("nationOfResidence", "selectedNationOfResidence", tribe);
		assertThat(nextPage.getTitle()).isEqualTo(nextPageTitle);
	}	
	
	/**
	 * This test verifies correct next page flow from the nationsBoundary page. When
	 * the household includes someone who is pregnant and/or a child under age 18
	 * AND a household member is a member of any Tribal Nation other than Leech Lake AND the household lives in Beltrami County THEN the next page is
	 * applyForTribalTANF 
	 * 
	 * @throws Exception
	 */
	@ParameterizedTest
	@CsvSource(value = {		
			//Test cases when the household lives in Beltrami County AND member of TribalNations
			  "Beltrami, Bois Forte,  apply for Tribal TANF", 
			  "Beltrami, Fond Du Lac,  apply for Tribal TANF",
			  "Beltrami, Grand Portage,  apply for Tribal TANF",
			  "Beltrami, Mille Lacs Band of Ojibwe,  apply for Tribal TANF",
			  "Beltrami, White Earth Nation,  apply for Tribal TANF",
			  "Beltrami, Lower Sioux,  apply for Tribal TANF",
			  "Beltrami, Upper Sioux,  apply for Tribal TANF",
			  "Beltrami, Prairie Island,  apply for Tribal TANF",
			  "Beltrami, Shakopee Mdewakanton,  apply for Tribal TANF",
			  "Beltrami, Red Lake Nation,  apply for Tribal TANF",
			  //if Leech Lake then navigation goes to page introIncome
			  "Beltrami, Leech Lake,  Intro: Income"
	})
	void nationsBoundaryNavigatesToCorrectNextPage(String county, String tribe, String nextPageTitle)
			throws Exception {
		postExpectingRedirect("identifyCountyBeforeApplying", "county", county, "prepareToApply");
		postExpectingRedirect("choosePrograms", "programs", "SNAP", "expeditedNotice");

		FormPage nextPage;
		//when the household includes someone who is pregnant
		nextPage = postAndFollowRedirect("addHouseholdMembers", "addHouseholdMembers", "false");
		nextPage = postAndFollowRedirect("pregnant", "isPregnant", "true");

		nextPage = postAndFollowRedirect("tribalNationMember", "isTribalNationMember", "true");
		nextPage = postAndFollowRedirect("selectTheTribe", "selectedTribe", tribe);
		nextPage = postAndFollowRedirect("nationsBoundary", "livingInNationBoundary", "false");
			
		assertThat(nextPage.getTitle()).isEqualTo(nextPageTitle);
			
	}
	
	/**
	 * When the county is a Certain_Pops pilot country (Chisago and Mille Lacs are also CP pilots, but exclude them from this test because
	 * they are MLBO rural counties)
	 * AND the Certain_Pops program is selected 
	 * AND Certain_Pops feature flag is on 
	 * AND Applicant is a member of Mille Lacs Band of Ojibwe
	 * AND "No" is selected on nationsBoundary page
	 * then the next page is medicalCareMilestone
	 * @param county
	 * @throws Exception
	 */
	@ParameterizedTest
	@CsvSource(value = {
			"Mower",
			"Polk"
	})
	public void whenCertainPopsNextPageIsMedicalCareMilestone(String county) throws Exception {
		when(featureFlagConfiguration.get("certain-pops")).thenReturn(FeatureFlag.ON);
		postExpectingRedirect("identifyCountyBeforeApplying", "county", county, "prepareToApply");
		postExpectingRedirect("choosePrograms", "programs", "CERTAIN_POPS", "basicCriteria");
		postExpectingRedirect("basicCriteria", "basicCriteria", List.of("SIXTY_FIVE_OR_OLDER"),
				"certainPopsConfirm");
	    fillOutPersonalInfo();
	    postExpectingRedirect("pregnant", "isPregnant", "true", "whoIsPregnant");
	    postExpectingRedirect("whoIsPregnant", "whoIsPregnant", "Dwight Schrute", "migrantFarmWorker");
		var nextPage = postAndFollowRedirect("tribalNationMember", "isTribalNationMember", "true");
		nextPage = postAndFollowRedirect("selectTheTribe", "selectedTribe", "Mille Lacs Band of Ojibwe");
		assertThat(nextPage.getTitle()).isEqualTo("Nations Boundary");
		nextPage = postAndFollowRedirect("nationsBoundary", "livingInNationBoundary", "false");
		assertThat(nextPage.getTitle()).isEqualTo("Medical Care Milestone");
		
	}
	
	/**
	 * When "No" is the response to the nationsBoundary page
	 * AND the condition for applyForTribalTANF as the next page are not met
	 * AND the condition for medicalCareMilestone as the next page is not met
	 * THEN the next page is intoIncome
	 * @throws Exception
	 */
	@Test
	public void whenNotTribalTanfOrCertainPopsTheNextPageIsIntroIncome() throws Exception {
		postExpectingRedirect("identifyCountyBeforeApplying", "county", "Norman", "prepareToApply");
		postExpectingRedirect("choosePrograms", "programs", "SNAP", "expeditedNotice");
	    fillOutPersonalInfo();
		var nextPage = postAndFollowRedirect("tribalNationMember", "isTribalNationMember", "true");
		nextPage = postAndFollowRedirect("selectTheTribe", "selectedTribe", "Mille Lacs Band of Ojibwe");
		assertThat(nextPage.getTitle()).isEqualTo("Nations Boundary");
		nextPage = postAndFollowRedirect("nationsBoundary", "livingInNationBoundary", "false");
		assertThat(nextPage.getTitle()).isEqualTo("Intro: Income");
	}

	/**
	 * Test for Minnesota Chippewa Tribes.
	 * When "No" is selected on nationsBoundary page 
	 * AND when the applicant is (an urban tribe member AND the county is an urban county) 
	 * AND applicant is pregnant
	 * then the next page is applyForTribalTANF.
	 * @throws Exception 
	 */
	@ParameterizedTest
	@CsvSource(value = {
			"Anoka, Bois Forte",
			"Hennepin, Bois Forte",
			"Ramsey, Bois Forte",
			"Anoka, Leech Lake",
			"Hennepin, Leech Lake",
			"Ramsey, Leech Lake",
			"Anoka, Mille Lacs Band of Ojibwe",
			"Hennepin, Mille Lacs Band of Ojibwe",
			"Ramsey, Mille Lacs Band of Ojibwe",
			"Anoka, White Earth Nation",
			"Hennepin, White Earth Nation",
			"Ramsey, White Earth Nation",
			"Anoka, Bois Forte",
			"Hennepin, Bois Forte",
			"Ramsey, Bois Forte",
			"Anoka, Grand Portage",
			"Hennepin, Grand Portage",
			"Ramsey, Grand Portage" })
	public void whenUrbanNotInTribalBoundaryNextPageIsApplyForTribalTANF(String urbanCounty, String tribalNation) throws Exception {
		postExpectingRedirect("identifyCountyBeforeApplying", "county", urbanCounty, "prepareToApply");
	    fillOutPersonalInfo();
	    postExpectingRedirect("pregnant", "isPregnant", "true", "whoIsPregnant");
	    postExpectingRedirect("whoIsPregnant", "whoIsPregnant", "Dwight Schrute", "migrantFarmWorker");
		var nextPage = postAndFollowRedirect("tribalNationMember", "isTribalNationMember", "true");
		nextPage = postAndFollowRedirect("selectTheTribe", "selectedTribe", tribalNation);
		assertThat(nextPage.getTitle()).isEqualTo("Nations Boundary");
		nextPage = postAndFollowRedirect("nationsBoundary", "livingInNationBoundary", "false");
		assertThat(nextPage.getTitle()).isEqualTo("apply for Tribal TANF");
	}
	
	/**
	 * Test for Mille Lacs Band of Ojibwe.
	 * When "No" is selected on nationsBoundary page 
	 * AND when the applicant is a Mille Lacs Band of Ojibwe tribe member AND the county is a rural MLBO county) 
	 * AND applicant is pregnant
	 * then the next page is applyForTribalTANF.
	 * @throws Exception 
	 */
	@ParameterizedTest
	@CsvSource(value = {
			"Aitkin", "Benton", "Chisago", "CrowWing", "Kanabec", "MilleLacs", "Morrison", "Pine" 		
	})
	public void whenMilleLacsIsRuralNotInTribalBoundaryNextPageIsApplyForTribalTANF(String ruralCounty) throws Exception {
		postExpectingRedirect("identifyCountyBeforeApplying", "county", ruralCounty, "prepareToApply");
	    fillOutPersonalInfo();
	    addHouseholdMembersWithProgram(CCAP);
	    postExpectingRedirect("pregnant", "isPregnant", "true", "whoIsPregnant");
	    postExpectingRedirect("whoIsPregnant", "whoIsPregnant", "Dwight Schrute", "migrantFarmWorker");
		var nextPage = postAndFollowRedirect("tribalNationMember", "isTribalNationMember", "true");
		nextPage = postAndFollowRedirect("selectTheTribe", "selectedTribe", "Mille Lacs Band of Ojibwe");
		assertThat(nextPage.getTitle()).isEqualTo("Nations Boundary");
		nextPage = postAndFollowRedirect("nationsBoundary", "livingInNationBoundary", "false");
		assertThat(nextPage.getTitle()).isEqualTo("apply for Tribal TANF");
	}
	
	
  private void goThroughLongTribalTanfFlow(String nationName, String county,
      String applyForTribalTanf,
      String... programs) throws Exception {
    getToPersonalInfoScreen(programs);
    addAddressInGivenCounty(county);
    postExpectingRedirect("pregnant", "isPregnant", "true", "whoIsPregnant");
    postExpectingRedirect("whoIsPregnant", "whoIsPregnant", "Dwight Schrute", "migrantFarmWorker");

	postExpectingRedirect("tribalNationMember", "isTribalNationMember", "true", "selectTheTribe");
	postExpectingRedirect("selectTheTribe", "selectedTribe", nationName, "nationsBoundary");
	postExpectingRedirect("nationsBoundary", "livingInNationBoundary", "true", "nationOfResidence");
	postExpectingRedirect("nationOfResidence", "selectedNationOfResidence", nationName, "applyForTribalTANF");
	postExpectingRedirect("applyForTribalTANF", "applyForTribalTANF", applyForTribalTanf,
			applyForTribalTanf.equals("true") ? "tribalTANFConfirmation" : "introIncome");
  }

  private void assertRoutingDestinationIsCorrectForDocument(Document doc,
      String... expectedNames) {
    List<RoutingDestination> routingDestinations = routingDecisionService.getRoutingDestinations(
        applicationData, doc);
    List<String> routingDestinationNames = routingDestinations.stream()
        .map(RoutingDestination::getName)
        .collect(Collectors.toList());
    assertThat(routingDestinationNames).containsOnly(expectedNames);
  }

  private void addAddressInGivenCounty(String county) throws Exception {
    new TestApplicationDataBuilder(applicationData)
        .withPageData("identifyCounty", "county", county);
    fillOutPersonalInfo();
    fillOutContactInfo();

    when(locationClient.validateAddress(any())).thenReturn(
        Optional.of(
            new Address("testStreet", "testCity", "someState", "testZipCode", "", county)));
    postExpectingSuccess("homeAddress", Map.of(
        "streetAddress", List.of("originalStreetAddress"),
        "apartmentNumber", List.of("originalApt"),
        "city", List.of("originalCity"),
        "zipCode", List.of("54321"),
        "state", List.of("MN"),
        "sameMailingAddress", List.of()
    ));
    postExpectingSuccess("verifyHomeAddress", "useEnrichedAddress", "true");

    postExpectingSuccess("identifyCountyBeforeApplying", "county", county);
    when(locationClient.validateAddress(any())).thenReturn(
        Optional.of(new Address("smarty street", "City", "CA", "03104", "", county))
    );
    postExpectingSuccess("mailingAddress", Map.of(
        "streetAddress", List.of("someStreetAddress"),
        "apartmentNumber", List.of("someApartmentNumber"),
        "city", List.of("someCity"),
        "zipCode", List.of("12345"),
        "state", List.of("IL"),
        "sameMailingAddress", List.of()
    ));

    postExpectingSuccess("verifyMailingAddress", "useEnrichedAddress", "true");

    var returnPage = new FormPage(getPage("reviewInfo"));
    assertThat(returnPage.getElementTextById("mailingAddress-address_street")).isEqualTo(
        "smarty street");
  }

  private void goThroughNationOfResidence(String county, String tribalNation, String[] programs)
      throws Exception {
    getToPersonalInfoScreen(programs);
    addAddressInGivenCounty(county);
    postExpectingSuccess("identifyCountyBeforeApplying", "county", county);
    postExpectingRedirect("tribalNationMember", "isTribalNationMember", "true", "selectTheTribe");
	postExpectingRedirect("selectTheTribe", "selectedTribe", tribalNation, "nationsBoundary");
    postExpectingRedirect("nationsBoundary", "livingInNationBoundary", "true", "nationOfResidence");
    postExpectingRedirect("nationOfResidence", "selectedNationOfResidence", tribalNation, "introIncome");
    
  }
}
