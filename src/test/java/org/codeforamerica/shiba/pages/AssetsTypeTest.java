package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.codeforamerica.shiba.testutilities.FormPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.codeforamerica.shiba.testutilities.TestUtils.assertPdfFieldEquals;

public class AssetsTypeTest extends AbstractShibaMockMvcTest {

  @BeforeEach
  protected void setUp() throws Exception {
    super.setUp();
    mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
    postExpectingSuccess("identifyCountyBeforeApplying", "county", "Hennepin");
    postExpectingSuccess("writtenLanguage", Map.of("writtenLanguage", List.of("ENGLISH")));
    postExpectingSuccess("spokenLanguage", Map.of("spokenLanguage", List.of("ENGLISH")));
  }
  @Test
  void verifyAssetTypesForSNAP() throws Exception {
    completeFlowAssetsTypeAsPerProgram("SNAP","CASH","GRH");
    var page = new FormPage(getPage("assets"));
    assertThat(page.getOptionValues("assets")).containsOnly("CASH", "BANK_ACCOUNT", "ELECTRONIC_PAYMENT_CARD", "VEHICLE","STOCK_BOND","NONE");
  }
  
  @Test
  void verifyAssetHeaderTextForHousehold() throws Exception {
	  completeFlowAssetsTypeAsPerProgram("SNAP","CASH","GRH");
	  expectHeaderText("/pages/assets", "Does anyone in your household have any of these?");
  }
  
  @Test
  void verifyAssetHeaderTextForSingleApplicant() throws Exception {
	  completeFlowAssetsSingleApplicant("SNAP");
	  expectHeaderText("/pages/assets", "Do you have any of these assets?");
  }
  
  @Test
  void verifyPDfAssetValues() throws Exception {
	  completeFlowAssetsTypeAsPerProgram("SNAP","CASH","GRH");
	  postExpectingSuccess("assets", "assets", List.of("CASH", "BANK_ACCOUNT", "ELECTRONIC_PAYMENT_CARD"));
	  var caf = submitAndDownloadCaf();
	  assertPdfFieldEquals("HAVE_CASH", "Yes", caf);
	  assertPdfFieldEquals("HAVE_SAVINGS", "Yes", caf);
	  assertPdfFieldEquals("HAVE_PAYMENT_CARD", "Yes", caf);
	  postExpectingSuccess("assets", "assets", List.of("NONE"));
	  caf = submitAndDownloadCaf();
	  assertPdfFieldEquals("HAVE_CASH", "No", caf);
	  assertPdfFieldEquals("HAVE_SAVINGS", "No", caf);
	  assertPdfFieldEquals("HAVE_PAYMENT_CARD", "No", caf);
  }
  @Test
  void verifyAssetTypesForCCAP() throws Exception {
    completeFlowAssetsTypeAsPerProgram("CCAP");
    var page = new FormPage(getPage("assets"));
    assertThat(page.getOptionValues("assets")).containsOnly("CASH", "BANK_ACCOUNT", "ELECTRONIC_PAYMENT_CARD", "VEHICLE","STOCK_BOND","REAL_ESTATE","ONE_MILLION_ASSETS","NONE");
    
  }

  @Test
  void verifyAssetSourcePagesForAll() throws Exception {
    completeFlowAssetsTypeAsPerProgram("CCAP");
    var page = new FormPage(getPage("assets"));
    assertThat(page.getOptionValues("assets")).containsOnly("CASH", "BANK_ACCOUNT", "ELECTRONIC_PAYMENT_CARD", "VEHICLE","STOCK_BOND","REAL_ESTATE", "ONE_MILLION_ASSETS","NONE");
    postExpectingRedirect("assets", "assets", List.of("VEHICLE","STOCK_BOND","REAL_ESTATE", "ONE_MILLION_ASSETS"), "soldAssets");  
  }

  @Test
  void verifySavingsIfNoneForAssetsChose() throws Exception {
    completeFlowAssetsTypeAsPerProgram("CCAP");
    var page = new FormPage(getPage("assets"));
    assertThat(page.getOptionValues("assets")).containsOnly("CASH", "BANK_ACCOUNT", "ELECTRONIC_PAYMENT_CARD", "VEHICLE", "STOCK_BOND", "REAL_ESTATE",
          "ONE_MILLION_ASSETS", "NONE");
    postExpectingRedirect("assets", "assets", List.of("None"), "soldAssets");
  }

  private void completeFlowAssetsTypeAsPerProgram(String... programs) throws Exception {
    
    completeFlowFromLandingPageThroughReviewInfo(programs);
    postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "true", "startHousehold");
    assertNavigationRedirectsToCorrectNextPage("startHousehold", "householdMemberInfo");
    fillOutHousemateInfo("CCAP");
    // Don't select any children in need of care, should get redirected to preparing meals together
    assertCorrectPageTitle("childrenInNeedOfCare", "Who are the children in need of care?");
    if(Arrays.stream(programs).anyMatch(p -> p.equals("SNAP"))) {
        postExpectingRedirect("preparingMealsTogether", "isPreparingMealsTogether", "false",
                "buyOrCookFood");
        postExpectingRedirect("buyOrCookFood", "isDisabledToBuyOrCookFood", "false",
	            "housingSubsidy");   
    }else {
        postExpectingRedirect("preparingMealsTogether", "isPreparingMealsTogether", "false",
                "housingSubsidy");
    }
    postExpectingRedirect("housingSubsidy", "hasHousingSubsidy", "false", "livingSituation");
    if(Arrays.asList(programs).contains("GRH")) {
    	 postExpectingRedirect("housingProvider", "housingProvider", "false", "goingToSchool");
    }else {
    	postExpectingRedirect("livingSituation", "livingSituation", "UNKNOWN", "goingToSchool");
    }
    postExpectingNextPageTitle("goingToSchool", "goingToSchool", "true", "Who is going to school?");
    postExpectingRedirect("whoIsGoingToSchool", "pregnant"); // no one is going to school
    completeFlowFromIsPregnantThroughTribalNations(true, programs);
    assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
    if (!Arrays.asList(programs).contains("CCAP")) {
	    postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Employment in the past");
	    postExpectingNextPageTitle("pastEmployment", "wereYouEmployed", "false", "Principal Wage Earner");
	    postExpectingNextPageTitle("principalWageEarner", "principalWageEarner", "I want to talk with my worker first.", "Income Up Next");
    }else{
        postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Job Search");
        postExpectingNextPageTitle("jobSearch", "currentlyLookingForJob", "true", "Who is looking for a job");
    }    

    fillSupportAndCare(programs);
  }
  
  private void completeFlowAssetsSingleApplicant(String... programs) throws Exception {
	  completeFlowFromLandingPageThroughReviewInfo(programs);
	  postExpectingRedirect("addHouseholdMembers", "addHouseholdMembers", "false", "temporaryAbsence");
	  postExpectingRedirect("temporaryAbsence", "hasTemporaryAbsence", "false", "introPersonalDetails");
	  postExpectingRedirect("housingSubsidy", "hasHousingSubsidy", "false", "goingToSchool");
	  postExpectingNextPageTitle("goingToSchool", "goingToSchool", "false", "Pregnant");
	  completeFlowFromIsPregnantThroughTribalNations(false, programs);
	  assertNavigationRedirectsToCorrectNextPage("introIncome", "employmentStatus");
	  postExpectingNextPageTitle("employmentStatus", "areYouWorking", "false", "Employment in the past");
	  postExpectingNextPageTitle("pastEmployment", "wereYouEmployed", "false", "Principal Wage Earner");
	  postExpectingNextPageTitle("principalWageEarner", "principalWageEarner", "I want to talk with my worker first.", "Income Up Next");
	  // should navigate from the incomeUpNext page to the unearnedIncome page
	  assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
	  // enter "None" on the unearnedIncome page, should navigate to the otherUnearnedIncome page
	  postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED", "otherUnearnedIncome");
	  // enter "None" on the otherUnearnedIncome page, should navigate to the futureIncome page.
	  postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED", "advancedChildTaxCredit");
	  fillAdditionalIncomeInfo(programs);
	  if (Arrays.stream(programs).allMatch(p -> p.equals("CCAP") || p.equals("NONE"))
	    	    && Arrays.asList(programs).contains("CCAP")) {
			postExpectingRedirect("supportAndCare", "supportAndCare", "false", "assets");
	  } else {
			postExpectingRedirect("childCareCosts", "childCareCosts", "false", "adultCareCosts");
			postExpectingRedirect("adultCareCosts", "adultCareCosts", "false", "supportAndCare");
			postExpectingRedirect("supportAndCare", "supportAndCare", "false", "assets");
	  }
  }

  /**
   * Start at incomeUpNext page (Got it! You're almost done with the income section).
   * Fill in supportAndCare page.
   * Ends at assets page.
   * @param programs
   * @throws Exception
   */
  private void fillSupportAndCare(String... programs) throws Exception {
    assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");
    postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED",
        "otherUnearnedIncome");
    if (Arrays.stream(programs).allMatch(p -> p.equals("CCAP"))) {
        postExpectingRedirect("otherUnearnedIncome","otherUnearnedIncome","NO_OTHER_UNEARNED_INCOME_SELECTED","futureIncome");
	} else {
	    postExpectingRedirect("otherUnearnedIncome","otherUnearnedIncome","NO_OTHER_UNEARNED_INCOME_SELECTED","advancedChildTaxCredit");
	}
    fillAdditionalIncomeInfo(programs);
    if (Arrays.stream(programs).allMatch(p -> p.equals("CCAP") || p.equals("NONE")) && Arrays.asList(programs).contains("CCAP")) {
		postExpectingRedirect("supportAndCare", "supportAndCare", "false", "assets");
	} else {
		postExpectingRedirect("specialCareExpenses", "specialCareExpenses", "NONE_OF_THE_ABOVE", "childCareCosts");
		postExpectingRedirect("childCareCosts", "childCareCosts", "false", "adultCareCosts");
		postExpectingRedirect("adultCareCosts", "adultCareCosts", "false", "supportAndCare");
		postExpectingRedirect("supportAndCare", "supportAndCare", "false", "assets");
	}
  }
}
