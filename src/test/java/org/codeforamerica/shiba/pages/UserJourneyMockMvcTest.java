package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.codeforamerica.shiba.testutilities.FormPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class UserJourneyMockMvcTest extends AbstractShibaMockMvcTest {

  @MockitoBean
  private WicRecommendationService wicRecommendationService;
	
  @BeforeEach
  protected void setUp() throws Exception {
    super.setUp();
    mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
    postExpectingSuccess("identifyCountyBeforeApplying", "county", "Hennepin");
    postExpectingSuccess("writtenLanguage", Map.of("writtenLanguage", List.of("ENGLISH")));
    postExpectingSuccess("spokenLanguage", Map.of("spokenLanguage", List.of("ENGLISH")));
  }

  @Test
  void healthcareCoverageDoesNotDisplayOnSuccessPageWhenClientAlreadyHasHealthcare()
      throws Exception {
    var successPage = nonExpeditedFlowToSuccessPage(true, true, true, true);
	// We expect to see the WIC recommendation because the pregnancy question response was "YES"
	when(wicRecommendationService.showWicMessage(any())).thenReturn(true);	  
    assertThat(successPage.getElementById("showRecommendationLink")).isNotNull();
    var recommendationsPage = new FormPage(getPage("recommendations"));
    assertThat(recommendationsPage.getElementById("recommendWIC")).isNotNull();
    // The application has CCAP so we expect to see the child care recommendation
    assertThat(recommendationsPage.getElementById("childCare")).isNotNull();
    // The applicant said "YES" to health care so the health care recommendation should not be displayed
    assertThat(recommendationsPage.getElementById("healthcareCoverage")).isNull();
  }

  @Test
  void healthcareCoverageDisplaysOnRecommendationsPageWhenClientDoesNotHaveHealthcare() throws Exception {
    var successPage = nonExpeditedFlowToSuccessPage(false, false, false, false);
	// We do not expect to see the WIC recommendation because hasHousehold is false thus
	// the pregnancy question response was "NO" and no children under 5.
	when(wicRecommendationService.showWicMessage(any())).thenReturn(false);	  
    assertThat(successPage.getElementById("showRecommendationLink")).isNotNull();
    var recommendationsPage = new FormPage(getPage("recommendations"));
    assertThat(recommendationsPage.getElementById("recommendWIC")).isNull();
    // The application has CCAP so we expect to see the child care recommendation
    assertThat(recommendationsPage.getElementById("childCare")).isNotNull();
    // The applicant said "NO" to health care so the health care recommendation should be displayed
    assertThat(recommendationsPage.getElementById("healthcareCoverage")).isNotNull();
  }

  @Test
  void userCanCompleteTheNonExpeditedHouseholdFlowWithNoEmployment() throws Exception {
    nonExpeditedFlowToSuccessPage(true, false);
  }

  @Test
  void userCanCompleteTheExpeditedFlowWithoutBeingExpedited() throws Exception {
    completeFlowFromLandingPageThroughReviewInfo("SNAP", "CCAP");

    FormPage reviewInfoPage = new FormPage(getPage("reviewInfo"));
    reviewInfoPage
        .assertLinkWithTextHasCorrectUrl("Submit an incomplete application now with only the above information.",
            "/pages/doYouNeedHelpImmediately");

    postExpectingRedirect("doYouNeedHelpImmediately",
        "needHelpImmediately",
        "true",
        "addHouseholdMembersExpedited");
    postExpectingRedirect("addHouseholdMembersExpedited", "addHouseholdMembers", "false",
        "expeditedIncome");
    postExpectingRedirect("expeditedIncome", "moneyMadeLast30Days", "123", "expeditedHasSavings");
    
    postExpectingRedirect("expeditedHasSavings", "haveSavings", "true", "liquidAssets");
    postExpectingRedirect("liquidAssets", "liquidAssets", "1233", "expeditedExpenses");
    postExpectingRedirect("expeditedExpenses", "payRentOrMortgage", "true",
        "expeditedExpensesAmount");
    postExpectingRedirect("expeditedExpensesAmount", "homeExpensesAmount", "333",
        "expeditedUtilityPayments");
    postExpectingRedirect("expeditedUtilityPayments", "payForUtilities", "COOLING",
        "expeditedMigrantFarmWorker");
    postExpectingRedirect("expeditedMigrantFarmWorker",
        "migrantOrSeasonalFarmWorker",
        "false",
        "snapExpeditedDetermination");
    FormPage page = new FormPage(getPage("snapExpeditedDetermination"));
    assertThat(page.getElementsByTag("p").get(0).text()).isEqualTo(
        "An eligibility worker will contact you within 5-7 days to review your application.");
    assertNavigationRedirectsToCorrectNextPage("snapExpeditedDetermination", "legalStuff");
    page = new FormPage(getPage("legalStuff"));
    assertThat(page.getTitle()).isEqualTo("Legal Stuff");
    assertThat(page.getElementById("ccap-legal")).isNotNull();
  }

  @Test
  void partialFlow() throws Exception {
    getToDocumentUploadScreen();
    completeDocumentUploadFlow();
    FormPage page = new FormPage(getPage("success"));
    assertThat(page.getLinksContainingText("Download your application")).hasSizeGreaterThan(0);
    assertThat(page.getLinksContainingText("Download your application").get(0).attr("href"))
        .isEqualTo("/download");
  }

  @Test
  void shouldHandleDeletionOfLastHouseholdMember() throws Exception {
    completeFlowFromLandingPageThroughReviewInfo("CCAP");

    // Add and delete one household member
    postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "true");
    fillOutHousemateInfo("EA");
    deleteOnlyHouseholdMember();

    // When we "hit back", we should be redirected to reviewInfo
    getWithQueryParamAndExpectRedirect("householdDeleteWarningPage", "iterationIndex", "0",
        "reviewInfo");
  }

  @Test
  void shouldValidateContactInfoEmailEvenIfEmailNotSelected() throws Exception {
    completeFlowFromLandingPageThroughContactInfo("CCAP");
    // Submitting an invalid email address should keep you on the same page
    postExpectingFailure("contactInfo", Map.of(
        "phoneNumber", List.of("7234567890"),
        "email", List.of("example.com"),
        "phoneOrEmail", List.of("TEXT")
    ));
  }

  @Test
  void shouldNotShowValidationWarningWhenPressingBackOnFormWithNotEmptyValidationCondition()
      throws Exception {
    getToPersonalInfoScreen("CCAP");
    postExpectingSuccess("personalInfo", Map.of(
        "firstName", List.of("defaultFirstName"),
        "lastName", List.of("defaultLastName"),
        "dateOfBirth", List.of("01", "12", "1928")
    ));
    assertFalse(new FormPage(getPage("personalInfo")).hasInputError());
  }

  @Test
  void shouldNotAllowNonPrintableUnicode() throws Exception {
    getToPersonalInfoScreen("CCAP");
    postExpectingSuccess("personalInfo", Map.of(
        // unicode null
        "firstName", List.of("Amanda" + "\u0000" + "🙏"),
        // \n is another unicode ctrl character, but that one we want to keep
        "lastName", List.of("Sm\nith"),
        "dateOfBirth", List.of("01", "12", "1928")
    ));

    // remove the null character
    String firstName = applicationData.getPagesData()
        .safeGetPageInputValue("personalInfo", "firstName").get(0);
    assertThat(firstName).isEqualTo("Amanda");

    // We should remove the \n character
    String lastName = applicationData.getPagesData()
        .safeGetPageInputValue("personalInfo", "lastName").get(0);
    assertThat(lastName).isEqualTo("Smith");
  }
  
  @Test
  void shouldNotAllowEmojis() throws Exception {
    getToPersonalInfoScreen("CCAP");
    postExpectingSuccess("personalInfo", Map.of(
        // unicode null
        "firstName", List.of("Am♛an⭐da🙏"),
        // \n is another unicode ctrl character, but that one we want to keep
        "lastName", List.of("Sm🔥it✅h"),
        "dateOfBirth", List.of("01", "12", "1928")
    ));

    // remove the null character
    String firstName = applicationData.getPagesData()
        .safeGetPageInputValue("personalInfo", "firstName").get(0);
    assertThat(firstName).isEqualTo("Amanda");

    // We should keep the \n character
    String lastName = applicationData.getPagesData()
        .safeGetPageInputValue("personalInfo", "lastName").get(0);
    assertThat(lastName).isEqualTo("Smith");
  }

  /**
   * This test verifies that we save the option that the user selected on the outOfStateAddressNotice page
   * @throws Exception
   */
  @Test
  void shouldSaveSelectedOutOfStateAddressOption() throws Exception {
	getToOutOfStateAddressNotice("CASH");
	
    postExpectingSuccess("outOfStateAddressNotice", Map.of("selectedOutOfStateAddressOption", List.of("CONTINUE")));
    String selectedOption = applicationData.getPagesData()
        .safeGetPageInputValue("outOfStateAddressNotice", "selectedOutOfStateAddressOption").get(0);
    assertThat(selectedOption).isEqualTo("CONTINUE");

    postExpectingSuccess("outOfStateAddressNotice", Map.of("selectedOutOfStateAddressOption", List.of("EDIT")));
    selectedOption = applicationData.getPagesData()
        .safeGetPageInputValue("outOfStateAddressNotice", "selectedOutOfStateAddressOption").get(0);
    assertThat(selectedOption).isEqualTo("EDIT");

    postExpectingSuccess("outOfStateAddressNotice", Map.of("selectedOutOfStateAddressOption", List.of("QUIT")));
    selectedOption = applicationData.getPagesData()
        .safeGetPageInputValue("outOfStateAddressNotice", "selectedOutOfStateAddressOption").get(0);
    assertThat(selectedOption).isEqualTo("QUIT");
  }

  /**
   * These test cases verify the page navigation within the Emergency Type section of MNbenefits.
   * That is, it verifies the flow from the choosePrograms page to the introBasicInfo page based
   * on the programs (EA, SNAP) and emergencyType choice (either EVICTION_NOTICE or OTHER_EMERGENCY) that is made.
   * 
   * @param programs  - a list of programs, must include EA, SNAP is optional
   * @param emergencyType - can be either EVICTION_NOTICE or OTHER_EMERGENCY
   * @throws Exception
   */
  @ParameterizedTest
  @CsvSource(value = {
		  "EA, EVICTION_NOTICE",       // choosePrograms > emergencyType > introBasicInfo
		  "EA, OTHER_EMERGENCY",       // choosePrograms > emergencyType > otherEmergency > introBasicInfo
		  "SNAP;EA, EVICTION_NOTICE",  // choosePrograms > emergencyType > expeditedNotice > introBasicInfo
		  "SNAP;EA, OTHER_EMERGENCY"   // choosePrograms > emergencyType > otherEmergency > expeditedNotice > introBasicInfo
		  })
  void shouldNavigateEmergencyTypeFlow(String programs, String emergencyType) throws Exception {
	  List<String> programsList = new ArrayList<String>(Arrays.asList(programs.split(";")));
	  postExpectingSuccess("identifyCountyBeforeApplying", "county", List.of("Chisago"));
	  postExpectingRedirect("choosePrograms", "programs", programsList, "emergencyType");
	  if (programsList.contains("SNAP")) {
		  if (emergencyType.equals("OTHER_EMERGENCY")) {
			  postExpectingRedirect("emergencyType", "emergencyType", emergencyType, "otherEmergency");
			  assertNavigationRedirectsToCorrectNextPage("otherEmergency", "expeditedNotice");
		  } else {
			  postExpectingRedirect("emergencyType", "emergencyType", emergencyType, "expeditedNotice");
		  }
		  assertNavigationRedirectsToCorrectNextPage("expeditedNotice", "introBasicInfo");
	  } else {
		  if (emergencyType.equals("OTHER_EMERGENCY")) {
			  postExpectingRedirect("emergencyType", "emergencyType", emergencyType, "otherEmergency");
			  assertNavigationRedirectsToCorrectNextPage("otherEmergency", "introBasicInfo");
		  } else {
			  assertNavigationRedirectsToCorrectNextPage("emergencyType", "introBasicInfo");
		  }
	  }
  }
  
  /**
   * These test cases verify the page navigation within the Emergency Type section of MNbenefits.
   * That is, it verifies the flow from the choosePrograms page to the introBasicInfo page based
   * on the programs (EA, SNAP, CERTAIN_POPS) and emergencyType choice (either EVICTION_NOTICE or OTHER_EMERGENCY) that is made.
   * 
   * @param programs  - a list of programs, must include EA and CERTAIN_POPS, SNAP is optional
   * @param emergencyType - can be either EVICTION_NOTICE or OTHER_EMERGENCY
   * @throws Exception
   */
  @ParameterizedTest
  @CsvSource(value = {
		  "EA;CERTAIN_POPS, EVICTION_NOTICE",      // choosePrograms > emergencyType > basicCriteria > certainPopsConfirm > introBasicInfo
		  "EA;CERTAIN_POPS, OTHER_EMERGENCY",      // choosePrograms > emergencyType > otherEmergency > basicCriteria > certainPopsConfirm > introBasicInfo
		  "SNAP;EA;CERTAIN_POPS, EVICTION_NOTICE", // choosePrograms > emergnecyType > basicCriteria > certainPopsConfirm > expeditedNotice > introBasicInfo
		  "SNAP;EA;CERTAIN_POPS, OTHER_EMERGENCY"  // choosePrograms > emergencyType > otherEmergency > basicCriteria > certainPopsConfirm > expeditedNotice > introBasicInfo
		  })
  void shouldNavigateEmergencyTypeFlowWithCertainPops(String programs, String emergencyType) throws Exception {
	  List<String> programsList = new ArrayList<String>(Arrays.asList(programs.split(";")));
	  // Use Chisago County to enable Certain Pops.
	  postExpectingSuccess("identifyCountyBeforeApplying", "county", List.of("Chisago"));
	  postExpectingRedirect("choosePrograms", "programs", programsList, "emergencyType");
	  if (emergencyType.equals("OTHER_EMERGENCY")) {
		  postExpectingRedirect("emergencyType", "emergencyType", emergencyType, "otherEmergency");
		  postExpectingRedirect("otherEmergency", "otherEmergency", "a different emergency", "basicCriteria");
	  } else {
		  postExpectingRedirect("emergencyType", "emergencyType", emergencyType, "basicCriteria");
	  }
	  // Certain Pops basic criteria needs to be something other than "NONE", use "SIXTY_FIVE_OR_OLDER"
	  postExpectingRedirect("basicCriteria", "basicCriteria", "SIXTY_FIVE_OR_OLDER", "certainPopsConfirm");
	  if (programsList.contains("SNAP")) {
		  assertNavigationRedirectsToCorrectNextPage("certainPopsConfirm", "expeditedNotice");
		  assertNavigationRedirectsToCorrectNextPage("expeditedNotice", "introBasicInfo");
	  } else {
		  assertNavigationRedirectsToCorrectNextPage("certainPopsConfirm", "introBasicInfo");
	  }
  }

  /**
   * These test cases verify the page navigation within the Personal Details
   * section of MNbenefits. Test cases are limited to applications with a single
   * program selection.
   * 
   * @param program  - a single program
   * @param addChild - when "false" the test runs as an applicant-only
   *                 application, when "true" the test run with one child added to
   *                 the household but with "None" selected for the child's
   *                 program selection
   * @throws Exception
   */
  @ParameterizedTest
  @CsvSource(value = { "SNAP, false", "SNAP, true", "CASH, false", "CASH, true", "EA, false", "EA, true",
		  "GRH, false", "GRH, true", "CCAP, false", "CCAP, true", "CERTAIN_POPS, false", "CERTAIN_POPS, true" })
  void shouldNavigatePersonalDetailsFlow(String program, String addChild) throws Exception {
	  String[] programs = { program };

	  // Use Chisago County to enable Certain Pops.
	  postExpectingSuccess("identifyCountyBeforeApplying", "county", List.of("Chisago"));

	  // navigation from choosePrograms to introBasicInfo
	  switch (program) {
		  case "SNAP": {
			  postExpectingRedirect("choosePrograms", "programs", Arrays.stream(programs).toList(), "expeditedNotice");
			  assertNavigationRedirectsToCorrectNextPage("expeditedNotice", "introBasicInfo");
			  break;
		  }
		  case "CERTAIN_POPS": {
			  postExpectingRedirect("choosePrograms", "programs", Arrays.stream(programs).toList(), "basicCriteria");
			  postExpectingRedirect("basicCriteria", "basicCriteria", "SIXTY_FIVE_OR_OLDER", "certainPopsConfirm");
			  assertNavigationRedirectsToCorrectNextPage("certainPopsConfirm", "introBasicInfo");
			  break;
		  }
		  case "EA": {  // choosePrograms > emergencyType > otherEmergency > introBasicInfo
			  postExpectingRedirect("choosePrograms", "programs", Arrays.stream(programs).toList(), "emergencyType");
			  postExpectingRedirect("emergencyType", "emergencyType", "OTHER_EMERGENCY", "otherEmergency");
			  assertNavigationRedirectsToCorrectNextPage("otherEmergency", "introBasicInfo");
			  break;
		  }
		  default: {
			  postExpectingRedirect("choosePrograms", "programs", Arrays.stream(programs).toList(), "introBasicInfo");
		  }
	  }

	  fillInPersonalInfoAndContactInfoAndAddress();

	  // navigation from addHouseholdMembers to housingSubsidy
	  switch (addChild) {
		  case "false": { // applicant-only case
			  switch (program) {
				  case "CCAP": {
					  postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "false", "addChildrenConfirmation");
					  assertNavigationRedirectsToCorrectNextPageWithOption("addChildrenConfirmation", "false",
							  "introPersonalDetails");
					  break;
				  }
				  default: {
					  postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "false", "introPersonalDetails");
					  break;
				  }
			  }
			  switch (program) {
				  case "CERTAIN_POPS": {
					  // will not navigate to housingSubsidy when only progam is Certain Pops
					  assertNavigationRedirectsToCorrectNextPage("introPersonalDetails", "livingSituation");
					  break;
				  }
				  default: {
					  assertNavigationRedirectsToCorrectNextPage("introPersonalDetails", "housingSubsidy");
				  }
			  }
		  }
		  default: { // applicant with one child in household case
			  postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
			  assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
			  Map<String, List<String>> householdMemberInfo = new HashMap<>();
			  householdMemberInfo.put("firstName", List.of("childFirstName"));
			  householdMemberInfo.put("lastName", List.of("childLastName"));
			  householdMemberInfo.put("programs", List.of("None"));
			  householdMemberInfo.put("relationship", List.of("child"));
			  householdMemberInfo.put("dateOfBirth", List.of("09", "14", "2000"));
			  householdMemberInfo.put("ssn", List.of("987654321"));
			  householdMemberInfo.put("maritalStatus", List.of("Never married"));
			  householdMemberInfo.put("sex", List.of("Male"));
			  householdMemberInfo.put("livedInMnWholeLife", List.of("Yes"));
			  postExpectingRedirect("householdMemberInfo", householdMemberInfo, "householdList");
	
			  // The flow to the introPersonalDetails page varies based on program selection
			  switch (program) {
				  case "CCAP": {
					  assertNavigationRedirectsToCorrectNextPage("householdList", "childrenInNeedOfCare");
					  postExpectingRedirect("childrenInNeedOfCare", "whoNeedsChildCare", "childFirstName childLastName",
							  "doYouHaveChildCareProvider");
					  postExpectingRedirect("doYouHaveChildCareProvider", "hasChildCareProvider", "false", "whoHasParentNotAtHome");
					  postExpectingRedirect("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome", "NONE_OF_THE_ABOVE",
							  "childCareMentalHealth");
					  break;
				  }
				  case "SNAP": {
					  assertNavigationRedirectsToCorrectNextPage("householdList", "preparingMealsTogether");
					  postExpectingRedirect("preparingMealsTogether", "preparingMealsTogether", "true", "housingSubsidy");
					  break;
				  }
				  case "CERTAIN_POPS": {
					  assertNavigationRedirectsToCorrectNextPage("householdList", "livingSituation");
					  break;
				  }
				  default: {
					  assertNavigationRedirectsToCorrectNextPage("householdList", "housingSubsidy");
					  break;
				  }
			  }
		  }
	  }

	  // navigation from housingSubsidy to goingToSchool
	  switch (program) {
		  case "GRH", "CCAP": {
			  postExpectingRedirect("housingSubsidy", "hasHousingSubsidy", "false", "livingSituation");
			  postExpectingRedirect("livingSituation", "livingSituation",
					  "PAYING_FOR_HOUSING_WITH_RENT_LEASE_OR_MORTGAGE", "goingToSchool");
			  break;
		  }
		  case "CERTAIN_POPS": {
			  postExpectingRedirect("livingSituation", "livingSituation",
					  "PAYING_FOR_HOUSING_WITH_RENT_LEASE_OR_MORTGAGE", "goingToSchool");
			  break;
		  }
		  default: {
			  postExpectingRedirect("housingSubsidy", "hasHousingSubsidy", "false", "goingToSchool");
		  }
	  }

	  // navigation from goingToSchool to introIncome
	  postExpectingRedirect("goingToSchool", "goingToSchool", "false", "pregnant");
	  postExpectingRedirect("pregnant", "isPregnant", "false", "migrantFarmWorker");
	  postExpectingRedirect("migrantFarmWorker", "migrantOrSeasonalFarmWorker", "false", "usCitizen");
	  postExpectingRedirect("usCitizen", "isUsCitizen", "true", "disability");
	  postExpectingRedirect("disability", "hasDisability", "false", "workChanges");
	  postExpectingRedirect("workChanges", "workChanges", "GO_ON_STRIKE", "tribalNationMember");
	  postExpectingRedirect("tribalNationMember", "isTribalNationMember", "false", "introIncome");
  }

  protected void completeFlowFromReviewInfoToDisability(String... applicantPrograms)
      throws Exception {
    postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");
    if (Arrays.stream(applicantPrograms)
        .anyMatch(program -> program.equals("CCAP") || program.equals("GRH"))) {
      assertNavigationRedirectsToCorrectNextPage("addHouseholdMembers", "introPersonalDetails");
      assertNavigationRedirectsToCorrectNextPage("introPersonalDetails", "livingSituation");
      postExpectingRedirect("livingSituation", "livingSituation", "UNKNOWN", "goingToSchool");
    } else {
      assertNavigationRedirectsToCorrectNextPage("addHouseholdMembers", "goingToSchool");
    }

    postExpectingRedirect("goingToSchool", "goingToSchool", "true", "pregnant");
    postExpectingRedirect("pregnant", "isPregnant", "false", "migrantFarmWorker");
    postExpectingRedirect("migrantFarmWorker", "migrantOrSeasonalFarmWorker", "false", "usCitizen");
    postExpectingRedirect("usCitizen", "isUsCitizen", "true", "disability");
    postExpectingRedirect("disability", "hasDisability", "false", "workChanges");
  }
}
