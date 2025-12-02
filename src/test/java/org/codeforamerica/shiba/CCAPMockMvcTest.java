package org.codeforamerica.shiba;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ccap")
public class CCAPMockMvcTest extends AbstractShibaMockMvcTest {

  @BeforeEach
  protected void setUp() throws Exception {
    super.setUp();
    mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
    postExpectingSuccess("identifyCountyBeforeApplying", "county", "Hennepin");
    postExpectingSuccess("writtenLanguage", Map.of("writtenLanguage", List.of("ENGLISH")));
    postExpectingSuccess("spokenLanguage", Map.of("spokenLanguage", List.of("ENGLISH")));
  }
  
  
  @Test
  void verifyUnearnedIncomeFlow() throws Exception {
	  completeFlowFromLandingPageThroughReviewInfo("SNAP");
	  postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
	  assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
	  fillOutSpouseInfo("SNAP");
	  
	  finishAddingHouseholdMembers("preparingMealsTogether");
	  postExpectingNextPageTitle("preparingMealsTogether", "isPreparingMealsTogether", "true",
		        "Buying and cooking food");
	   postExpectingNextPageTitle("buyOrCookFood", "isDisabledToBuyOrCookFood", "false",
	            "Housing subsidy");
	  postExpectingNextPageTitle("housingSubsidy", "hasHousingSubsidy", "false",
	            "Going to school");
	  postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Pregnant");
	  completeFlowFromIsPregnantThroughTribalNations(true, "SNAP");
	  assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
	  postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Income Up Next");
	  assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
	  postExpectingNextPageTitle("unearnedIncome", "unearnedIncome", List.of("UNEMPLOYMENT", "WORKERS_COMPENSATION"), "Unearned Income Source");
	  postExpectingRedirect("unemploymentIncomeSource", "monthlyIncomeUnemployment", List.of("Dwight Schrute applicant"), "workersCompIncomeSource");
	  postExpectingRedirect("workersCompIncomeSource", "monthlyIncomeWorkersComp", List.of("Dwight Schrute applicant"), "otherUnearnedIncome");
	  postExpectingNextPageTitle("otherUnearnedIncome", "otherUnearnedIncome", List.of("INSURANCE_PAYMENTS"), "Insurance payments");
	  postExpectingRedirect("insurancePaymentsIncomeSource", "monthlyIncomeInsurancePayments", List.of("Dwight Schrute applicant"), "futureIncome");
  }
  
	@Test
	void verifyotherUnearnedIncomeFlow() throws Exception {

		when(featureFlagConfiguration.get("child-care")).thenReturn(FeatureFlag.ON);

		// Initial add info for user
		completeFlowFromLandingPageThroughReviewInfo("CCAP");

		// Add a spouse NOT SURE
		postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
		assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
		fillOutHousemateInfo("CCAP");

		getNavigationPageWithQueryParamAndExpectRedirect("householdList", "option", "1", "householdMemberInfo");

		// Add child (NONE for programs)

		fillOutHousemateInfo("NONE");
		finishAddingHouseholdMembers("childrenInNeedOfCare");

		// Children in need of care flow
		assertCorrectPageTitle("childrenInNeedOfCare", "Who are the children in need of care?");
		postExpectingRedirect("childrenInNeedOfCare", "whoNeedsChildCare", List.of("child name"),
				"doYouHaveChildCareProvider");

		assertNavigationRedirectsToCorrectNextPage("childrenInNeedOfCare", "doYouHaveChildCareProvider");
		// Say no provider, no parent not at home
		postExpectingRedirect("doYouHaveChildCareProvider", "hasChildCareProvider", "false", "whoHasParentNotAtHome");
		// Mental Health
		postExpectingRedirect("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome", List.of("NONE_OF_THE_ABOVE"),
				"childCareMentalHealth");
		
		postExpectingRedirect("childCareMentalHealth", "childCareMentalHealth", "false",
				"housingSubsidy");

		// Minimal path through housing/school
		postExpectingRedirect("housingSubsidy", "hasHousingSubsidy", "false", "livingSituation");
		postExpectingRedirect("livingSituation", "livingSituation", "UNKNOWN", "goingToSchool");
		postExpectingRedirect("goingToSchool", "goingToSchool", "false", "pregnant");
		completeFlowFromIsPregnantThroughTribalNations(false, "CCAP", "CCAP", "NONE");

		// Income section
		assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
		postExpectingRedirect("employmentStatus", "areYouWorking", "false", "jobSearch");
		postExpectingRedirect("jobSearch", "currentlyLookingForJob", "false", "incomeUpNext");

		// Unearned income → None
		assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
		postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED", "otherUnearnedIncome");

		// Select 4 other unearned income sources
		postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome",
				// List.of("INSURANCE_PAYMENTS", "TRUST_MONEY", "INTEREST_DIVIDENDS",
				// "HEALTH_CARE_REIMBURSEMENT"),
				List.of("INSURANCE_PAYMENTS"), "insurancePaymentsIncomeSource");

		// Verify first income source page is reached
		assertCorrectPageTitle("insurancePaymentsIncomeSource", "Insurance payments");

		// Continue flow to futureIncome

		postExpectingRedirect("insurancePaymentsIncomeSource", "monthlyIncomeInsurancePayments",
				List.of("Dwight Schrute applicant"), "futureIncome");

		assertCorrectPageTitle("futureIncome", "Future Income");

	}
  

  @Test
  void verifyFlowWhenNoOneHasSelectedCCAPInHousehold() throws Exception {
    completeFlowFromLandingPageThroughReviewInfo("SNAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("EA");
    finishAddingHouseholdMembers("preparingMealsTogether");
    postExpectingNextPageTitle("preparingMealsTogether", "isPreparingMealsTogether", "false",
        "Buying and cooking food");
    postExpectingNextPageTitle("buyOrCookFood", "isDisabledToBuyOrCookFood", "false",
            "Housing subsidy");   
    postExpectingNextPageTitle("housingSubsidy", "hasHousingSubsidy", "false",
            "Going to school");
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Pregnant");
    completeFlowFromIsPregnantThroughTribalNations(true, "SNAP");
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Income Up Next");
    assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
    postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED","otherUnearnedIncome");
    postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED","futureIncome");
    fillAdditionalIncomeInfo("SNAP");
   //applicationData.getPagesData().getPage("otherUnearnedIncome").get("otherUnearnedIncome").getValue();
    postExpectingRedirect("supportAndCare", "supportAndCare", "false", "childCareCosts"); 
    postExpectingRedirect("childCareCosts", "childCareCosts", "false", "assets"); 
    postExpectingSuccess("assets", "assets", "NONE");
    assertNavigationRedirectsToCorrectNextPage("assets", "soldAssets");
    assertPageDoesNotHaveElementWithId("legalStuff", "ccap-legal");
  }
  
  @Test
  void verifyFlowWhenOtherUnearnedIncomeIsSelectedForSNAPHousehold() throws Exception {
    completeFlowFromLandingPageThroughReviewInfo("SNAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("EA");
    finishAddingHouseholdMembers("preparingMealsTogether");
    postExpectingNextPageTitle("preparingMealsTogether", "isPreparingMealsTogether", "false",
        "Buying and cooking food");
    postExpectingNextPageTitle("buyOrCookFood", "isDisabledToBuyOrCookFood", "false",
            "Housing subsidy");    
    postExpectingNextPageTitle("housingSubsidy", "hasHousingSubsidy", "false",
            "Going to school");
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Pregnant");
    completeFlowFromIsPregnantThroughTribalNations(true, "SNAP");
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Income Up Next");
    assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
    postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED","otherUnearnedIncome");
    postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", List.of("ANNUITY_PAYMENTS"),"annuityIncomeSource");
    
    postExpectingRedirect("annuityIncomeSource", Map.of("monthlyIncomeAnnuityPayments",  List.of("Dwight Schrute applicant"), "annuityPaymentsAmount", List.of("100", "")), "futureIncome");
    
	// Verify that annuity was selected on otherUnearnedIncome page
	List<String> otherUnearnedIncomeSelections = (List<String>) applicationData.getPagesData()
			.getPage("otherUnearnedIncome").get("otherUnearnedIncome").getValue();
	assertThat(otherUnearnedIncomeSelections).contains("ANNUITY_PAYMENTS");

	// Verify that the household member was selected
	List<String> monthlyIncomeAnnuityPayment = (List<String>) applicationData.getPagesData()
			.getPage("annuityIncomeSource").get("monthlyIncomeAnnuityPayments").getValue();
	assertThat(monthlyIncomeAnnuityPayment).contains("Dwight Schrute applicant");
	// Verify the annuity amount was saved correctly
	List<String> annuityPaymentsAmount = (List<String>) applicationData.getPagesData().getPage("annuityIncomeSource")
			.get("annuityPaymentsAmount").getValue();
	assertThat(annuityPaymentsAmount).isEqualTo(List.of("100", ""));
    fillAdditionalIncomeInfo("SNAP");
   //applicationData.getPagesData().getPage("otherUnearnedIncome").get("otherUnearnedIncome").getValue();
    postExpectingRedirect("supportAndCare", "supportAndCare", "false", "childCareCosts");
    postExpectingRedirect("childCareCosts", "childCareCosts", "false", "assets");
    postExpectingSuccess("assets", "assets", "VEHICLE");
    assertNavigationRedirectsToCorrectNextPage("assets", "soldAssets");
    assertPageDoesNotHaveElementWithId("legalStuff", "ccap-legal");
  }

  @Test
  void verifyFlowWhenLiveAloneApplicantHasNotSelectedCCAP() throws Exception {
    completeFlowFromLandingPageThroughReviewInfo("SNAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "false",
        "introPersonalDetails");
    postExpectingRedirect("introPersonalDetails", "housingSubsidy");
    postExpectingRedirect("housingSubsidy", "goingToSchool");
    postExpectingRedirect("goingToSchool", "goingToSchool", "true", "pregnant");
    completeFlowFromIsPregnantThroughTribalNations(false, "SNAP");
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Income Up Next");
    assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
    postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED",
        "otherUnearnedIncome");
    postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED",
            "futureIncome");
    fillAdditionalIncomeInfo("SNAP");
    postExpectingRedirect("supportAndCare", "supportAndCare", "false", "childCareCosts");
    postExpectingRedirect("childCareCosts", "childCareCosts", "false", "assets");
    postExpectingSuccess("assets", "assets", "VEHICLE");
    assertNavigationRedirectsToCorrectNextPage("assets", "soldAssets");
    assertPageDoesNotHaveElementWithId("legalStuff", "ccap-legal");
  }

  @Test
  void verifyDrugFelonyQuestionIsDisplayedWhenAnythingOtherThanCCAPOnlyIsSelected() throws Exception {
    completeFlowFromLandingPageThroughReviewInfo("CCAP", "SNAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true",
            "startHousehold");
    fillOutHousemateInfo("EA");
    assertPageHasElementWithId("legalStuff", "drugFelony1");
  }

  @Test
  void verifyDrugFelonyQuestionIsNotDisplayedWhenCCAPOnlyIsSelected() throws Exception {
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true",
            "startHousehold");
    fillOutHousemateInfo("EA");
    assertPageDoesNotHaveElementWithId("legalStuff", "drugFelony1");
  }

  @Test
  void verifyFlowWhenLiveAloneApplicantSelectedCCAP() throws Exception {
    // Applicant lives alone and choose CCAP
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "false",
        "addChildrenConfirmation");
    assertNavigationRedirectsToCorrectNextPageWithOption("addChildrenConfirmation","false","introPersonalDetails");
    postExpectingRedirect("introPersonalDetails", "housingSubsidy");
    postExpectingRedirect("housingSubsidy", "livingSituation");
    postExpectingRedirect("livingSituation", "goingToSchool");
    postExpectingRedirect("goingToSchool", "goingToSchool", "true", "pregnant");
    postExpectingRedirect("pregnant", "isPregnant", "true", "migrantFarmWorker");
    postExpectingRedirect("migrantFarmWorker", "migrantOrSeasonalFarmWorker", "true", "citizenship");
    postExpectingRedirect("citizenship", "citizenshipStatus", "BIRTH_RIGHT", "tribalNationMember");
    postExpectingRedirect("tribalNationMember", "isTribalNationMember", "false", "introIncome");
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingRedirect("employmentStatus", "areYouWorking", "false", "jobSearch");
    postExpectingRedirect("jobSearch", "currentlyLookingForJob", "true", "incomeUpNext");
    fillUnearnedIncomeToLegalStuffCCAP("CCAP");
  }

  @Test
  void verifyFlowWhenApplicantSelectedCCAPAndHouseholdMemberDidNot() throws Exception {
    // Applicant selected CCAP for themselves and did not choose any program (i.e., None) for the household member
	when(featureFlagConfiguration.get("child-care")).thenReturn(FeatureFlag.ON); 
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("NONE");
    finishAddingHouseholdMembers("childrenInNeedOfCare");
    assertCorrectPageTitle("childrenInNeedOfCare", "Who are the children in need of care?");
    postExpectingRedirect("childrenInNeedOfCare", "whoNeedsChildCare", List.of("child name"), "doYouHaveChildCareProvider");
    postExpectingRedirect("doYouHaveChildCareProvider", "hasChildCareProvider", "false", "whoHasParentNotAtHome");
    postExpectingRedirect("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome", List.of("NONE_OF_THE_ABOVE"), "childCareMentalHealth");
    postExpectingRedirect("childCareMentalHealth", "childCareMentalHealth", "false", "housingSubsidy");
    postExpectingRedirect("housingSubsidy", "livingSituation");
    postExpectingRedirect("livingSituation", "goingToSchool");
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Who is going to school?");
    completeFlowFromIsPregnantThroughTribalNations(true, "CCAP", "NONE");
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Job Search");
    postExpectingNextPageTitle("jobSearch", "currentlyLookingForJob", "true",
        "Who is looking for a job");
    fillUnearnedIncomeToLegalStuffCCAP("CCAP", "NONE");
  }
  

  @Test
  void verifyChildCareProviderAndChildSupportFlow() throws Exception {
	when(featureFlagConfiguration.get("child-care")).thenReturn(FeatureFlag.ON);
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("EA");
    finishAddingHouseholdMembers("childrenInNeedOfCare");
    assertCorrectPageTitle("childrenInNeedOfCare", "Who are the children in need of care?");
    postExpectingRedirect("childrenInNeedOfCare", "whoNeedsChildCare", List.of("child name"), "doYouHaveChildCareProvider");
    postExpectingRedirect("doYouHaveChildCareProvider", "hasChildCareProvider", "true", "childCareProviderInfo");
    fillOutProviderInformation();
    String householdMemberId = getFirstHouseholdMemberId();
    postExpectingNextPageTitle("childrenAtThisProvider",
        "childrenNames",
        List.of("householdMemberFirstName householdMemberLastName" + householdMemberId),
        "Child Care Provider List"
    );
    getNavigationPageWithQueryParamAndExpectRedirect("childCareProviderList", "option", "1",
            "whoHasParentNotAtHome");
    postExpectingNextPageTitle("whoHasParentNotAtHome",
            "whoHasAParentNotLivingAtHome",
            List.of("householdMemberFirstName householdMemberLastName" + householdMemberId),
            "Name of parent outside home");
    postExpectingNextPageTitle("parentNotAtHomeNames",
            Map.of("whatAreTheParentsNames", List.of("My Parent", "Default's Parent"),
                "childIdMap", List.of("applicant", householdMemberId)
            ),
            "Child support payments");
    postExpectingNextPageTitle("childCareChildSupport",
            Map.of("whoReceivesChildSupportPayments", List.of("householdMemberFirstName householdMemberLastName" + householdMemberId)),
            "Mental health needs & child care");
    
    
    postExpectingRedirect("childCareMentalHealth", "childCareMentalHealth", "false",
			"housingSubsidy");
    postExpectingRedirect("housingSubsidy", "livingSituation");
    postExpectingRedirect("livingSituation", "goingToSchool");
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Who is going to school?");
    completeFlowFromIsPregnantThroughTribalNations(true, "CCAP", "EA");
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Job Search");
    postExpectingNextPageTitle("jobSearch", "currentlyLookingForJob", "true",
        "Who is looking for a job");
    fillUnearnedIncomeToLegalStuffCCAP("CCAP", "EA");
  }
  
  @Test
  void verifySchoolInformationFlow() throws Exception {
	when(featureFlagConfiguration.get("child-care")).thenReturn(FeatureFlag.ON);
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("CCAP");
    finishAddingHouseholdMembers("childrenInNeedOfCare");
    assertCorrectPageTitle("childrenInNeedOfCare", "Who are the children in need of care?");
    postExpectingRedirect("childrenInNeedOfCare", "whoNeedsChildCare", List.of("child name"), "doYouHaveChildCareProvider");
    postExpectingRedirect("doYouHaveChildCareProvider", "hasChildCareProvider", "true", "childCareProviderInfo");
    fillOutProviderInformation();
    String householdMemberId = getFirstHouseholdMemberId();
    postExpectingNextPageTitle("childrenAtThisProvider",
        "childrenNames",
        List.of("householdMemberFirstName householdMemberLastName" + householdMemberId),
        "Child Care Provider List"
    );
    getNavigationPageWithQueryParamAndExpectRedirect("childCareProviderList", "option", "1",
            "whoHasParentNotAtHome");
    postExpectingNextPageTitle("whoHasParentNotAtHome",
            "whoHasAParentNotLivingAtHome",
            List.of("householdMemberFirstName householdMemberLastName" + householdMemberId),
            "Name of parent outside home");
    postExpectingNextPageTitle("parentNotAtHomeNames",
            Map.of("whatAreTheParentsNames", List.of("My Parent", "Default's Parent"),
                "childIdMap", List.of("applicant", householdMemberId)
            ),
            "Child support payments");
    postExpectingNextPageTitle("childCareChildSupport",
            Map.of("whoReceivesChildSupportPayments", List.of("householdMemberFirstName householdMemberLastName" + householdMemberId)),
            "Mental health needs & child care");
    postExpectingRedirect("childCareMentalHealth", "childCareMentalHealth", "false",
			"housingSubsidy");
    postExpectingRedirect("housingSubsidy", "livingSituation");
    postExpectingRedirect("livingSituation", "goingToSchool");
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Who is going to school?");
    postExpectingNextPageTitle("whoIsGoingToSchool", "whoIsGoingToSchool", List.of("child name"), "School details");
    postExpectingNextPageTitle("schoolDetails", "schoolName", List.of("child school"), "School grade level");
    postExpectingNextPageTitle("schoolGrade", "schoolGrade", List.of("Pre-K"), "School start date");
    postExpectingNextPageTitle("schoolStartDate", "schoolStartDate", List.of("01","01","2024"), "Pregnant");
  }

  @Test
  void verifyFlowWhenOnlyHouseholdMemberSelectedCCAP() throws Exception {
    selectPrograms("NONE");
    assertPageHasWarningMessage("choosePrograms",
        "You will be asked to share some information about yourself even though you're only applying for others.");

    completeFlowFromLandingPageThroughReviewInfo("NONE");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("CCAP");

    // Don't select any children in need of care, should get redirected to preparing meals together
    assertCorrectPageTitle("childrenInNeedOfCare", "Who are the children in need of care?");
    postExpectingNextPageTitle("childrenInNeedOfCare", "Mental health needs & child care");

    // Go back to childrenInNeedOfCare and select someone this time, but don't select anyone having a parent not at home
    String householdMemberId = getFirstHouseholdMemberId();
    postExpectingNextPageTitle("childrenInNeedOfCare",
        "whoNeedsChildCare",
        List.of("defaultFirstName defaultLastName applicant",
            "householdMemberFirstName householdMemberLastName" + householdMemberId),
        "Do you have a child care provider?"
    );
    postExpectingRedirect("doYouHaveChildCareProvider", "hasChildCareProvider", "false", "whoHasParentNotAtHome");
    postExpectingNextPageTitle("whoHasParentNotAtHome",
        "whoHasAParentNotLivingAtHome",
        List.of("NONE_OF_THE_ABOVE"),
        "Mental health needs & child care");


    // Go back and select someone having a parent not at home
    postExpectingNextPageTitle("whoHasParentNotAtHome",
        "whoHasAParentNotLivingAtHome",
        List.of("defaultFirstName defaultLastName applicant"),
        "Name of parent outside home");
    postExpectingNextPageTitle("parentNotAtHomeNames",
        Map.of("whatAreTheParentsNames", List.of("My Parent", "Default's Parent"),
            "childIdMap", List.of("applicant", householdMemberId)
        ),
        "Child support payments");
    postExpectingNextPageTitle("childCareChildSupport",
            Map.of("whoReceivesChildSupportPayments", List.of("defaultFirstName defaultLastName applicant")),
            "Mental health needs & child care");
    postExpectingRedirect("childCareMentalHealth", "childCareMentalHealth", "false", "housingSubsidy");

    postExpectingRedirect("housingSubsidy", "hasHousingSubsidy", "true", "livingSituation");
    postExpectingRedirect("livingSituation", "livingSituation", "UNKNOWN", "goingToSchool");
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Who is going to school?");
    //postExpectingRedirect("whoIsGoingToSchool", "pregnant"); // no one is going to school
    completeFlowFromIsPregnantThroughTribalNations(true, "CCAP", "NONE");
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Job Search");
    postExpectingNextPageTitle("jobSearch", "currentlyLookingForJob", "true",
        "Who is looking for a job");
    fillUnearnedIncomeToLegalStuffCCAP("CCAP", "NONE");
  }
  
  

  private void fillUnearnedIncomeToLegalStuffCCAP(String... Programs) throws Exception {
    assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
    postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED",
        "otherUnearnedIncome");
    postExpectingRedirect("otherUnearnedIncome",
        "otherUnearnedIncome",
        "NO_OTHER_UNEARNED_INCOME_SELECTED",
        "futureIncome");
    fillAdditionalIncomeInfo(Programs);
    if (Arrays.stream(Programs).allMatch(p -> p.equals("CCAP") || p.equals("NONE"))
    	    && Arrays.asList(Programs).contains("CCAP")) {
		postExpectingRedirect("supportAndCare", "supportAndCare", "false", "assets");
	} else {
		postExpectingRedirect("supportAndCare", "supportAndCare", "false", "childCareCosts");
		postExpectingRedirect("childCareCosts", "childCareCosts", "false", "assets");
	}
    postExpectingSuccess("assets", "assets", "NONE");
    assertNavigationRedirectsToCorrectNextPage("assets", "soldAssets");
    assertPageHasElementWithId("legalStuff", "ccap-legal");
  }
  
  private void fillOutProviderInformation() throws Exception {
	    postExpectingSuccess("childCareProviderInfo", Map.of(
	        "childCareProviderName", List.of("ProviderName"),
	        "phoneNumber", List.of("6523325454"),
	        "streetAddress", List.of("someStreet"),
	        "city", List.of("someCity"),
	        "zipCode", List.of("12345"),
	        "state", List.of("MN")
	    ));
	  }
  
  // test case: CCAP Applicant only and the ChildCareMentalHealth pages are skipped
  @Test
  void verifyChildCareMentalHealthFlowApplicantOnlyAndNoTochildCareMentalHealth() throws Exception {
	when(featureFlagConfiguration.get("child-care")).thenReturn(FeatureFlag.ON); 
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "false", "addChildrenConfirmation");
    assertNavigationRedirectsToCorrectNextPageWithOption("addChildrenConfirmation","false","introPersonalDetails");
    assertNavigationRedirectsToCorrectNextPage("introPersonalDetails", "housingSubsidy"); 
  }
  
  //test case: CCAP with Household and No to the ChildCareMentalHealth
  @Test
  void verifyChildCareMentalHealthFlowWithHouseholdAndNoTochildCareMentalHealth() throws Exception {
	when(featureFlagConfiguration.get("child-care")).thenReturn(FeatureFlag.ON); 
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
	postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
	assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
	fillOutHousemateInfo("CCAP");
	finishAddingHouseholdMembers("childrenInNeedOfCare");
	postExpectingRedirect("childrenInNeedOfCare", "whoNeedsChildCare", List.of("child name"),
			"doYouHaveChildCareProvider");
	postExpectingRedirect("doYouHaveChildCareProvider", "hasChildCareProvider", "false", "whoHasParentNotAtHome");

	postExpectingRedirect("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome", List.of("NONE_OF_THE_ABOVE"),
			"childCareMentalHealth");
    assertCorrectPageTitle("childCareMentalHealth", "Mental health needs & child care");
    postExpectingRedirect("childCareMentalHealth", "childCareMentalHealth", "false",
            "housingSubsidy");
    assertCorrectPageTitle("housingSubsidy", "Housing subsidy");
    
  }
   
  //test case: CCAP Household and Yes to the ChildCareMentalHealth
  @Test
  void verifyChildCareMentalHealthFlowWithHouseholdAndYesTochildCareMentalHealth() throws Exception {
	when(featureFlagConfiguration.get("child-care")).thenReturn(FeatureFlag.ON); 
    completeFlowFromLandingPageThroughReviewInfo("CCAP");
	postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
	assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
	fillOutHousemateInfo("CCAP");
	finishAddingHouseholdMembers("childrenInNeedOfCare");
	postExpectingRedirect("childrenInNeedOfCare", "whoNeedsChildCare", List.of("child name"),
			"doYouHaveChildCareProvider");
	postExpectingRedirect("doYouHaveChildCareProvider", "hasChildCareProvider", "false", "whoHasParentNotAtHome");

	postExpectingRedirect("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome", List.of("NONE_OF_THE_ABOVE"),
			"childCareMentalHealth");
    assertCorrectPageTitle("childCareMentalHealth", "Mental health needs & child care");
    // We will skip the whoNeedsChildCareForMentalHealth page.  The skipCondition evaluates to "true" because
    // We added one household member and then we put one child in the "whoNeedsChildCare" list so all thats left is the applicant.
    postExpectingRedirect("childCareMentalHealth", "childCareMentalHealth", "true",
            "childCareMentalHealthTimes");
    assertCorrectPageTitle("childCareMentalHealthTimes", "Time needed each week");
    postExpectingRedirect("childCareMentalHealthTimes", "childCareMentalHealthHours", "8",
            "housingSubsidy");
    assertCorrectPageTitle("housingSubsidy", "Housing subsidy");   
  }
  
}
