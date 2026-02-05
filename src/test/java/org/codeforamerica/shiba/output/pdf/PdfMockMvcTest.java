package org.codeforamerica.shiba.output.pdf;

import static org.codeforamerica.shiba.output.Document.CCAP;
import static org.codeforamerica.shiba.output.caf.CoverPagePreparer.CHILDCARE_WAITING_LIST_UTM_SOURCE;
import static org.codeforamerica.shiba.testutilities.TestUtils.ADMIN_EMAIL;
import static org.codeforamerica.shiba.testutilities.TestUtils.assertPdfFieldContains;
import static org.codeforamerica.shiba.testutilities.TestUtils.assertPdfFieldEquals;
import static org.codeforamerica.shiba.testutilities.TestUtils.assertPdfFieldIsEmpty;
import static org.codeforamerica.shiba.testutilities.TestUtils.assertPdfFieldIsNull;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.YES;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.text.PDFTextStripper;
import org.codeforamerica.shiba.Program;
import org.codeforamerica.shiba.output.ApplicationFile;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.enrichment.Address;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

@Tag("pdf")
public class PdfMockMvcTest extends AbstractShibaMockMvcTest {
	@Autowired
	private PdfGenerator pdfGenerator;
	
	@Override
	@BeforeEach
	protected void setUp() throws Exception {
		super.setUp();
		mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
		postExpectingSuccess("identifyCountyBeforeApplying", "county", "Hennepin");
	    postExpectingSuccess("writtenLanguage", Map.of("writtenLanguage", List.of("ENGLISH")));
	    postExpectingSuccess("spokenLanguage", Map.of("spokenLanguage", List.of("ENGLISH"), "needInterpreter", List.of("true")));

		postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");
		
	}
	

	@Test
	void shouldAnswerEnergyAssistanceQuestion() throws Exception {
		selectPrograms("CASH");

		postExpectingSuccess("energyAssistance", "energyAssistance", "true");
		postExpectingSuccess("energyAssistanceMoreThan20", "energyAssistanceMoreThan20", "false");

		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("RECEIVED_LIHEAP", "No", caf);
	}
	
	@Test
	void shouldMapForWorkChanges() throws Exception {
		selectPrograms("CASH");
		postExpectingSuccess("workChanges", "workChanges", List.of("STOP_WORK", "REFUSE_JOB", "FEWER_HOURS", "ON_STRIKE"));
		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("END_WORK", "Yes", caf);
		assertPdfFieldEquals("REFUSE_A_JOB_OFFER", "Yes", caf);
		assertPdfFieldEquals("ASK_TO_WORK_FEWER_HOURS", "Yes", caf);
		assertPdfFieldEquals("GO_ON_STRIKE", "Yes", caf);
	}
	
	@Test
	void shouldMapNoneOfTheAboveWorkChanges() throws Exception {
		selectPrograms("CASH");
		postExpectingSuccess("workChanges", "workChanges", "NONE_OF_THE_ABOVE");
		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("END_WORK", "Off", caf);
		assertPdfFieldEquals("REFUSE_A_JOB_OFFER", "Off", caf);
		assertPdfFieldEquals("ASK_TO_WORK_FEWER_HOURS", "Off", caf);
		assertPdfFieldEquals("GO_ON_STRIKE", "Off", caf);
		assertPdfFieldEquals("WORK_CHANGES_NONE", "Yes", caf);
	}

	@Test
	void shouldMapEnergyAssistanceWhenUserReceivedNoAssistance() throws Exception {
		selectPrograms("CASH");

		postExpectingSuccess("energyAssistance", "energyAssistance", "false");

		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("RECEIVED_LIHEAP", "No", caf);
	}

	@Test
	void shouldSupportCyrillicCharacters() throws Exception {
		selectPrograms("CASH");
		postExpectingSuccess("signThisApplication", "applicantSignature", List.of("aЕкатерина"));
		postToUrlExpectingSuccess("/submit", "/pages/submit/navigation", Map.of("", List.of("")));
		var caf = downloadCafClientPDF();

		assertPdfFieldEquals("APPLICANT_SIGNATURE", "aЕкатерина", caf);
	}
	
	@Nested
	@Tag("pdf")
	class IncomeAndOtherIncome {
	
		//The following tests are for CAF Q14 with the new addition of the yes/no radio button
		
		//should switch radio to YES if there are options on unearnedIncome page
		@Test
		void shouldMapYesOtherIncomeUnearnedIncomeOnly() throws Exception {
		selectPrograms("CASH");
		fillOutPersonalInfo();
		postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");
		postExpectingSuccess("unearnedIncome", "unearnedIncome", List.of("SOCIAL_SECURITY", "SSI", "VETERANS_BENEFITS", "UNEMPLOYMENT"));
		
		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("OTHER_INCOME_YES_NO", "Yes", caf);
		}
		
		//should switch radio to YES if there are options on otherUnearnedIncome page
		@Test
		void shouldMapYesOtherIncomeOtherUnearnedIncomeOnly() throws Exception {
		selectPrograms("CASH");
		fillOutPersonalInfo();
		postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");	
		postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", List.of("INSURANCE_PAYMENTS", "TRUST_MONEY", "RENTAL_INCOME", "INTEREST_DIVIDENDS", 
				"HEALTH_CARE_REIMBURSEMENT", "CONTRACT_FOR_DEED", "BENEFITS", "ANNUITY_PAYMENTS", "GIFTS", "LOTTERY_GAMBLING", "DAY_TRADING", "OTHER_PAYMENTS"),
				"otherUnearnedIncomeSources");
		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("OTHER_INCOME_YES_NO", "Yes", caf);
		}
		
		//should switch radio to YES if there are options on both unearnedIncome & otherUnearnedIncome pages
		@Test
		void shouldMapYesOtherIncomeBothIncome() throws Exception {
		selectPrograms("CASH");
		fillOutPersonalInfo();
		postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");
		postExpectingSuccess("unearnedIncome", "unearnedIncome", List.of("SOCIAL_SECURITY", "SSI", "VETERANS_BENEFITS", "UNEMPLOYMENT"));
		postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", List.of("INSURANCE_PAYMENTS", "TRUST_MONEY", "RENTAL_INCOME", "INTEREST_DIVIDENDS", 
				"HEALTH_CARE_REIMBURSEMENT", "CONTRACT_FOR_DEED", "BENEFITS", "ANNUITY_PAYMENTS", "GIFTS", "LOTTERY_GAMBLING", "DAY_TRADING", "OTHER_PAYMENTS"),
				"otherUnearnedIncomeSources");
		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("OTHER_INCOME_YES_NO", "Yes", caf);
		}
		
		//should switch radio to NO if there are NONE selected on both unearnedIncome & otherUnearnedIncome pages
		@Test
		void shouldMapNoOtherIncomeNoIncome() throws Exception {
		selectPrograms("CASH");
		fillOutPersonalInfo();
		postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");
		postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");
		postExpectingSuccess("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED");
		postExpectingSuccess("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED" );

		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("OTHER_INCOME_YES_NO", "No", caf);
		}
	}
	
	
	@Test
	void shouldMapNoOtherIncome() throws Exception {
	selectPrograms("CASH");
	fillOutPersonalInfo();
	postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");
	postExpectingSuccess("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED");
	postExpectingSuccess("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED");

	var caf = downloadCafClientPDF();
	assertPdfFieldEquals("OTHER_INCOME_YES_NO", "No", caf);
	}
	
	@Test
	void shouldMapChildrenNeedingChildcareFullNames() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("CCAP");
		addHouseholdMembersWithProgram("CCAP");

		String jimHalpertId = getFirstHouseholdMemberId();
		postExpectingSuccess("childrenInNeedOfCare", "whoNeedsChildCare",
				List.of("Dwight Schrute applicant", "Jim Halpert " + jimHalpertId));

		postExpectingSuccess("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome",
				List.of("Dwight Schrute applicant", "Jim Halpert " + jimHalpertId));
		postExpectingSuccess("parentNotAtHomeNames", Map.of("whatAreTheParentsNames", List.of("", "Jim's Parent"),
				"childIdMap", List.of("applicant", jimHalpertId)));

		var ccap = downloadCcapClientPDF();
		assertPdfFieldEquals("CHILDCARE_CHILD_NAME_0", "Dwight Schrute", ccap);
		assertPdfFieldEquals("CHILDCARE_CHILD_NAME_1", "Jim Halpert", ccap);
		assertPdfFieldEquals("CHILD_FULL_NAME_0", "Dwight Schrute", ccap);
		assertPdfFieldIsEmpty("PARENT_NOT_LIVING_AT_HOME_0", ccap);
		assertPdfFieldEquals("CHILD_FULL_NAME_1", "Jim Halpert", ccap);
		assertPdfFieldEquals("PARENT_NOT_LIVING_AT_HOME_1", "Jim's Parent", ccap);
		assertPdfFieldIsEmpty("CHILD_FULL_NAME_2", ccap);
		assertPdfFieldIsEmpty("PARENT_NOT_LIVING_AT_HOME_2", ccap);
	}
	
	// This test verifies the yes button click on temporaryAbsence gets written to the caf 
	@Test
	void shouldMapTemporaryAbsenceTrue() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("SNAP");
		addHouseholdMembersWithProgram("CCAP");
		fillInRequiredPages();
		
		postExpectingSuccess("temporaryAbsence","hasTemporaryAbsence", "true");
		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("ANYONE_TEMPORARILY_NOT_HOME", "Yes", caf);

	}
	
	
	
	// This test verifies the no button click on temporaryAbsence gets written to the caf 
	@Test
	void shouldMapTemporaryAbsenceFalse() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("SNAP");
		addHouseholdMembersWithProgram("CCAP");
		fillInRequiredPages();
		
		postExpectingSuccess("temporaryAbsence","hasTemporaryAbsence", "false");
		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("ANYONE_TEMPORARILY_NOT_HOME", "No", caf);

	}
	
	// This test verifies the yes button click on advancedChildTaxCredit gets written to the caf 
	@Test
	void shouldMapAdvancedChildTaxCreditTrue() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("SNAP");
		postExpectingSuccess("advancedChildTaxCredit", "hasAdvancedChildTaxCredit", "true");
		
		var caf = downloadCafClientPDF();
		
		assertPdfFieldEquals("ADVANCED_CHILD_TAX_CREDIT", "Yes", caf);
	
	}
	
	// This test verifies the no button click on advancedChildTaxCredit gets written to the caf 
	@Test
	void shouldMapAdvancedChildTaxCreditFalse() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("SNAP");
		postExpectingSuccess("advancedChildTaxCredit", "hasAdvancedChildTaxCredit", "false");
		
		var caf = downloadCafClientPDF();
		
		assertPdfFieldEquals("ADVANCED_CHILD_TAX_CREDIT", "No", caf);
	}
	
	//This Test checks that the social worker question gets mapped right to the caf. 
	@Test
	void shouldMapsocialWorker() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("SNAP");
		postExpectingSuccess("socialWorker", "hasSocialWorker", "true");
		
		var caf = submitAndDownloadCaf();
		assertPdfFieldEquals("HAS_SOCIAL_WORKER", "Yes", caf);
	}
	
	//This Test checks that the direct deposit question gets mapped as YES to the caf. 
		@Test
		void shouldMapdirectDepositYes() throws Exception {
			fillOutPersonalInfo();
			selectPrograms("CASH");
			postExpectingSuccess("directDeposit", "hasDirectDeposit", "true");
			
			var caf = downloadCafClientPDF();
			assertPdfFieldEquals("DIRECT_DEPOSIT", "Yes", caf);
	}

	//This Test checks that the direct deposit question gets mapped as NO to the caf. 
		@Test
		void shouldMapdirectDepositNo() throws Exception {
			fillOutPersonalInfo();
			selectPrograms("CASH");
			postExpectingSuccess("directDeposit", "hasDirectDeposit", "false");
			
			var caf = downloadCafClientPDF();
			assertPdfFieldEquals("DIRECT_DEPOSIT", "No", caf);
	}
	
	@Test
	void shouldMapReferrals() throws Exception {
		selectPrograms("SNAP");
		postExpectingSuccess("referrals", "needsReferrals", "true");
		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("HELP_WITH_REFERRALS", "Yes", caf);
	}
	
	@Test
	void shouldMapEBTInPast() throws Exception {
		selectPrograms("SNAP");
		postExpectingSuccess("ebtInPast", "hadEBTInPast", "true");
		
		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("EBT_IN_PAST", "Yes", caf);
	}

	@Test
	void shouldNotMapParentsLivingOutsideOfHomeIfNoneSelected() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("CCAP");
		addHouseholdMembersWithProgram("CCAP");

		postExpectingSuccess("childrenInNeedOfCare", "whoNeedsChildCare",
				List.of("Dwight Schrute applicant", getJimFullNameAndId()));

		postExpectingSuccess("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome", "NONE_OF_THE_ABOVE");

		var ccap = downloadCcapClientPDF();
		assertPdfFieldEquals("CHILDCARE_CHILD_NAME_0", "Dwight Schrute", ccap);
		assertPdfFieldEquals("CHILDCARE_CHILD_NAME_1", "Jim Halpert", ccap);
		assertPdfFieldIsEmpty("CHILD_FULL_NAME_0", ccap);
		assertPdfFieldIsEmpty("PARENT_NOT_LIVING_AT_HOME_0", ccap);
		assertPdfFieldIsEmpty("CHILD_FULL_NAME_1", ccap);
		assertPdfFieldIsEmpty("PARENT_NOT_LIVING_AT_HOME_1", ccap);
		assertPdfFieldIsEmpty("CHILD_FULL_NAME_2", ccap);
		assertPdfFieldIsEmpty("PARENT_NOT_LIVING_AT_HOME_2", ccap);
	}

	@Test 
	void shouldDefaultToNoForMillionDollarQuestionWhenQuestionPageIsNotShown() throws Exception {
		selectPrograms("CCAP");

		postExpectingSuccess("energyAssistance", "energyAssistance", "false");
		postExpectingSuccess("medicalExpenses", "medicalExpenses", "NONE_OF_THE_ABOVE");
		postExpectingSuccess("supportAndCare", "supportAndCare", "false");
		postExpectingSuccess("assets", "assets", "NONE");
		postExpectingSuccess("soldAssets", "haveSoldAssets", "false"); 
		
		var ccap = downloadCcapClientPDF();
		assertPdfFieldEquals("HAVE_MILLION_DOLLARS", "No", ccap);
	}

	@Test
	void shouldMarkYesForMillionDollarQuestionWhenChoiceIsYes() throws Exception {
		selectPrograms("CCAP");

		postExpectingSuccess("energyAssistance", "energyAssistance", "false");
		postExpectingSuccess("medicalExpenses", "medicalExpenses", "NONE_OF_THE_ABOVE");
		postExpectingSuccess("supportAndCare", "supportAndCare", "false");
		postExpectingSuccess("assets", "assets", List.of("STOCK_BOND", "ONE_MILLION_ASSETS"));
		postExpectingSuccess("soldAssets", "haveSoldAssets", "false"); 
		
		var ccap = downloadCcapClientPDF();
		assertPdfFieldEquals("HAVE_MILLION_DOLLARS", "Yes", ccap);
	}

	@Test
	void shouldNotMapUnearnedIncomeCcapWhenNoneOfTheAboveIsSelected() throws Exception {
		selectPrograms("CCAP");
		fillInRequiredPages();
		postExpectingSuccess("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED");

		var ccap = downloadCcapClientPDF();
		assertPdfFieldEquals("BENEFITS", "No", ccap);
		assertPdfFieldEquals("INSURANCE_PAYMENTS", "No", ccap);
		assertPdfFieldEquals("CONTRACT_FOR_DEED", "No", ccap);
		assertPdfFieldEquals("TRUST_MONEY", "No", ccap);
		assertPdfFieldEquals("HEALTH_CARE_REIMBURSEMENT", "No", ccap);
		assertPdfFieldEquals("INTEREST_DIVIDENDS", "No", ccap);
		assertPdfFieldEquals("OTHER_PAYMENTS", "No", ccap);
	}

	/**
	 * This test verifies the following for an applicant-only CAF application:
	 *   - when multiple options are selected on the otherUnearnedIncome page, the next page is otherUnearnedIncomeSources
	 *   - when inputs are provided on the otherUnearnedIncomeSources page:
	 *       - the next page is futureIncome
	 *       - the input type and value are written to the CAF cover page (first 10 entries only)
	 *       - the applicant's full name, input type, value, and frequency are written to the CAF section 14 (first two entries)
	 * @throws Exception
	 */
    @Test
	void shouldMapOtherUnearnedIncomeCafSingleApplicant() throws Exception {
		selectPrograms("SNAP");
		fillOutPersonalInfo();
		
	    // Post to the otherUnearnedIncome page with multiple income types selected
		// Since there is no household, the next page will be the "otherUnearnedIncomeSources" page.
		postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", List.of("INSURANCE_PAYMENTS", "TRUST_MONEY", "RENTAL_INCOME", "INTEREST_DIVIDENDS", 
				"HEALTH_CARE_REIMBURSEMENT", "CONTRACT_FOR_DEED", "BENEFITS", "ANNUITY_PAYMENTS", "GIFTS", "LOTTERY_GAMBLING", "DAY_TRADING", "OTHER_PAYMENTS"),
				"otherUnearnedIncomeSources");

		// Post the inputs for the 12 other unearned income options, the method we use here asserts the next page's title rather than page name
		// Note: Map.of has a capacity of 10 entries so need to use Map.ofEntries rather than Map.of
		postExpectingNextPageTitle("otherUnearnedIncomeSources", Map.ofEntries(
				Map.entry("insurancePaymentsAmount", List.of("100.00")),
				Map.entry("trustMoneyAmount", List.of("110.00")),
				Map.entry("rentalIncomeAmount", List.of("120.00")),
				Map.entry("interestDividendsAmount", List.of("130.00")),
				Map.entry("healthCareReimbursementAmount", List.of("140.00")),
				Map.entry("contractForDeedAmount", List.of("150.00")),
				Map.entry("benefitsAmount", List.of("160.00")),
				Map.entry("annuityPaymentsAmount", List.of("170.00")),
				Map.entry("giftsAmount", List.of("180.00")),
				Map.entry("lotteryGamblingAmount", List.of("190.00")),
				Map.entry("dayTradingProceedsAmount", List.of("200.00")), 
				Map.entry("otherPaymentsAmount", List.of("210.00"))), "Advance Child Tax Credit");
	    postExpectingRedirect("advancedChildTaxCredit", "hasAdvancedChildTaxCredit", "false","studentFinancialAid");
	    postExpectingRedirect("studentFinancialAid", "studentFinancialAid", "false","futureIncome");

	    var caf = downloadCafClientPDF();

	    // Verify that each income type and its value is correctly reflected on the cover page
	    // The first two are also used to write to the CAF section 14
	    assertPdfFieldEquals("OTHER_INCOME_FULL_NAME_0", "Dwight Schrute", caf);
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_0", "Insurance payments (settlements, short- or long-term disability, etc.)", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_0", "100.00", caf);
	    assertPdfFieldEquals("OTHER_INCOME_FREQUENCY_0", "Monthly", caf);
	    
	    assertPdfFieldEquals("OTHER_INCOME_FULL_NAME_1", "Dwight Schrute", caf);
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_1", "Trusts", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_1", "110.00", caf);
	    assertPdfFieldEquals("OTHER_INCOME_FREQUENCY_1", "Monthly", caf);
	    
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_2", "Rental income", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_2", "120.00", caf);
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_3", "Interest or dividends", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_3", "130.00", caf);
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_4", "Health care reimbursement", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_4", "140.00", caf);
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_5", "Contract for deed", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_5", "150.00", caf);
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_6", "Public assistance (MFIP, DWP, GA, Tribal TANF)", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_6", "160.00", caf);
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_7", "Annuity payments", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_7", "170.00", caf);
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_8", "Gifts", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_8", "180.00", caf);
	    assertPdfFieldEquals("OTHER_INCOME_TYPE_9", "Lottery or gambling winnings", caf);
	    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_9", "190.00", caf);
		assertPdfFieldEquals("OTHER_INCOME_YES_NO", "Yes", caf);
	    
	    // The cover page has space for 10 unearned income entries so 
	    // these last two will not be written to the cover page or anywhere in the CAF
	    assertPdfFieldIsNull("OTHER_INCOME_TYPE_10", caf);
	    assertPdfFieldIsNull("OTHER_INCOME_AMOUNT_10", caf);
	    assertPdfFieldIsNull("OTHER_INCOME_TYPE_11", caf);
	    assertPdfFieldIsNull("OTHER_INCOME_AMOUNT_11", caf);
	}
	
	/**
	 * This test verifies the following for an applicant+household CAF application:
	 *   - when multiple options (5) are selected on the otherUnearnedIncome page:
	 *       - it is followed by a sequence of xxIncomeSource pages
	 *       - the next page after the last xxIncomeSoure page is the futureIncome page
	 *       - the input type and value are written to the CAF cover page
	 *       - the applicant's full name, input type, value, and frequency are written to the CAF section 14 (first two entries)
	 * @throws Exception
	 */
	@Test
	void shouldMapOtherUnearnedIncomeCafHousehold() throws Exception {
		selectPrograms("SNAP");
		fillOutPersonalInfo();
		addHouseholdMembersWithProgram("SNAP");
		postExpectingRedirect("unearnedIncome", "unearnedIncome", List.of("NO_UNEARNED_INCOME_SELECTED"),"otherUnearnedIncome");
		String applicant = getApplicantFullNameAndId();
		postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", List.of("RENTAL_INCOME", "ANNUITY_PAYMENTS", "GIFTS", "LOTTERY_GAMBLING", "DAY_TRADING"),"rentalIncomeSource");
		postExpectingRedirect("rentalIncomeSource", Map.of("monthlyIncomeRental", List.of(applicant), "rentalIncomeAmount", List.of("200")), "annuityIncomeSource");
		postExpectingRedirect("annuityIncomeSource", Map.of("monthlyIncomeAnnuityPayments", List.of(applicant), "annuityPaymentsAmount", List.of("210")), "giftsIncomeSource");
		postExpectingRedirect("giftsIncomeSource", Map.of("monthlyIncomeGifts", List.of(applicant), "giftsAmount", List.of("220")), "lotteryIncomeSource");
		postExpectingRedirect("lotteryIncomeSource", Map.of("monthlyIncomeLotteryGambling", List.of(applicant), "lotteryGamblingAmount", List.of("230")), "dayTradingIncomeSource");
		postExpectingRedirect("dayTradingIncomeSource", Map.of("monthlyIncomeDayTradingProceeds", List.of(applicant), "dayTradingProceedsAmount", List.of("240")), "advancedChildTaxCredit");
	    postExpectingRedirect("advancedChildTaxCredit", "hasAdvancedChildTaxCredit", "false","studentFinancialAid");
	    postExpectingRedirect("studentFinancialAid", "studentFinancialAid", "false","futureIncome");
		
		
		var caf = downloadCafClientPDF();
	    // Verify that each income type and its value is correctly reflected on the cover page
	    // The first two are also used to write to the CAF section 14
	    assertPdfFieldEquals("OTHER_INCOME_FULL_NAME_0", "Dwight Schrute", caf);
		assertPdfFieldEquals("OTHER_INCOME_TYPE_0", "Rental income", caf);
		assertPdfFieldEquals("OTHER_INCOME_AMOUNT_0", "200", caf);
	    assertPdfFieldEquals("OTHER_INCOME_FREQUENCY_0", "Monthly", caf);

	    assertPdfFieldEquals("OTHER_INCOME_FULL_NAME_1", "Dwight Schrute", caf);
		assertPdfFieldEquals("OTHER_INCOME_TYPE_1", "Annuity payments", caf);
		assertPdfFieldEquals("OTHER_INCOME_AMOUNT_1", "210", caf);
	    assertPdfFieldEquals("OTHER_INCOME_FREQUENCY_1", "Monthly", caf);
		
		assertPdfFieldEquals("OTHER_INCOME_TYPE_2", "Gifts", caf);
		assertPdfFieldEquals("OTHER_INCOME_AMOUNT_2", "220", caf);
	
		assertPdfFieldEquals("OTHER_INCOME_TYPE_3", "Lottery or gambling winnings", caf);
		assertPdfFieldEquals("OTHER_INCOME_AMOUNT_3", "230", caf);
		
		assertPdfFieldEquals("OTHER_INCOME_TYPE_4", "Day trading proceeds", caf);
		assertPdfFieldEquals("OTHER_INCOME_AMOUNT_4", "240", caf);
		assertPdfFieldEquals("OTHER_INCOME_YES_NO", "Yes", caf);

	}
	
	@Test
	void shouldMapParentNotAtHomeForChildUnder19() throws Exception {
		selectPrograms("CASH");
		addHouseholdMembersWithProgram("CCAP");
		postExpectingSuccess("parentNotAtHome", "hasParentNotAtHome", "true");
		
		var caf = downloadCafClientPDF();
		//Maps to No on PDF with Yes Field because the question is inverse
		assertPdfFieldEquals("BOTH_PARENTS_AT_HOME", "Yes", caf);
	}

	@Test
	void shouldMapAdultsInHouseholdRequestingChildcareAssistance() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("CCAP");
		addHouseholdMembersWithProgram("CCAP");

		String jim = getJimFullNameAndId();
		postExpectingSuccess("childrenInNeedOfCare", "whoNeedsChildCare", jim);

		postExpectingSuccess("jobSearch", "currentlyLookingForJob", "true");
		String pam = getPamFullNameAndId();
		postExpectingSuccess("whoIsLookingForAJob", "whoIsLookingForAJob", List.of(jim, pam));

		String me = getApplicantFullNameAndId();
		postExpectingSuccess("whoIsGoingToSchool", "whoIsGoingToSchool", List.of(me, jim));
		
		postExpectingSuccess("schoolDetails", "schoolName", List.of("Test School"));
		postExpectingSuccess("schoolGrade", "schoolGrade", List.of("K"));
		postExpectingSuccess("schoolStartDate", "schoolStartDate", List.of("09","01","2024"));

		// Add a job for Jim
		addFirstJob(jim, "Jim's Employer");

		// Add a job for Pam
		postWithQueryParam("jobBuilder", "option", "0");
		addJob(pam, "Pam's Employer");

		var ccap = downloadCcapClientPDF();
		assertPdfFieldEquals("ADULT_REQUESTING_CHILDCARE_LOOKING_FOR_JOB_FULL_NAME_0", "Pam Beesly", ccap);
		assertPdfFieldEquals("ADULT_REQUESTING_CHILDCARE_GOING_TO_SCHOOL_FULL_NAME_0", "Dwight Schrute", ccap);
		assertPdfFieldEquals("ADULT_REQUESTING_CHILDCARE_WORKING_FULL_NAME_0", "Pam Beesly", ccap);
		assertPdfFieldEquals("ADULT_REQUESTING_CHILDCARE_WORKING_EMPLOYERS_NAME_0", "Pam's Employer", ccap);
		assertPdfFieldEquals("STUDENT_FULL_NAME_0", "Jim Halpert", ccap);
		assertPdfFieldEquals("SCHOOL_NAME_0", "Test School", ccap);
		assertPdfFieldEquals("SCHOOL_GRADE_0", "K", ccap);
		assertPdfFieldEquals("SCHOOL_START_DATE_0", "09/01/2024", ccap);
		assertPdfFieldIsEmpty("ADULT_REQUESTING_CHILDCARE_LOOKING_FOR_JOB_FULL_NAME_1", ccap);
		assertPdfFieldIsEmpty("ADULT_REQUESTING_CHILDCARE_GOING_TO_SCHOOL_FULL_NAME_1", ccap);
		assertPdfFieldIsEmpty("ADULT_REQUESTING_CHILDCARE_WORKING_FULL_NAME_1", ccap);
		assertPdfFieldIsEmpty("ADULT_REQUESTING_CHILDCARE_WORKING_EMPLOYERS_NAME_1", ccap);
	}

	private void addFirstJob(String householdMemberNameAndId, String employersName) throws Exception {
		postWithQueryParam("incomeByJob", "option", "0");
		addJob(householdMemberNameAndId, employersName);
	}
	
	@Test
	void shouldMapEachDayJobToPDFmultipleApplicant() throws Exception{
		fillOutPersonalInfo();
		selectPrograms("SNAP");
		addHouseholdMembersWithProgram("CCAP");
		fillInRequiredPages();
		
		// Add a job for Jim
		postWithQueryParam("incomeByJob", "option", "0");
		String jim = getJimFullNameAndId();
		postExpectingSuccess("householdSelectionForIncome", "whoseJobIsIt", jim);
		postExpectingSuccess("employersName", "employersName", "A");
		postExpectingSuccess("selfEmployment", "selfEmployment", "false");
		postExpectingSuccess("paidByTheHour", "paidByTheHour", "false");
		postExpectingSuccess("payPeriod", "payPeriod", "EVERY_DAY");
		postExpectingSuccess("incomePerPayPeriod", "incomePerPayPeriod", "1000");

		// Add a job for Dwight
		postWithQueryParam("incomeByJob", "option", "0");
		String me = getApplicantFullNameAndId();
		postExpectingSuccess("householdSelectionForIncome", "whoseJobIsIt", me);
		postExpectingSuccess("employersName", "employersName", "B");
		postExpectingSuccess("selfEmployment", "selfEmployment", "false");
		postExpectingSuccess("paidByTheHour", "paidByTheHour", "false");
		postExpectingSuccess("payPeriod", "payPeriod", "EVERY_DAY");
		postExpectingSuccess("incomePerPayPeriod", "incomePerPayPeriod", "2000");
		
		postExpectingSuccess("principalWageEarner", "principalWageEarner", "I want to talk with my worker first.");
		
        var caf = downloadCafClientPDF();
        var ccap = downloadCcapClientPDF();
		
		assertPdfFieldEquals("INCOME_PER_PAY_PERIOD_0", "", caf);
		assertPdfFieldEquals("INCOME_PER_PAY_PERIOD_1", "", caf);
		assertPdfFieldEquals("PAY_FREQUENCY_0", "Daily", caf);
		assertPdfFieldEquals("PAY_FREQUENCY_1", "Daily", caf);
		assertPdfFieldEquals("GROSS_MONTHLY_INCOME_0", "1000.00", caf);
		assertPdfFieldEquals("GROSS_MONTHLY_INCOME_1", "2000.00", caf);
		assertPdfFieldEquals("MONEY_MADE_LAST_MONTH", "3000.00", caf);
		assertPdfFieldEquals("dummyFieldName20", "1000.00", caf);
		assertPdfFieldEquals("dummyFieldName21", "Daily", caf);
		assertPdfFieldEquals("dummyFieldName24", "2000.00", caf);
		assertPdfFieldEquals("dummyFieldName25", "Daily", caf);
		
		assertPdfFieldEquals("INCOME_PER_PAY_PERIOD_0", "", ccap);
		assertPdfFieldEquals("INCOME_PER_PAY_PERIOD_1", "", caf);
		assertPdfFieldEquals("PAY_FREQUENCY_0", "Daily", ccap);
		assertPdfFieldEquals("PAY_FREQUENCY_1", "Daily", ccap);
		assertPdfFieldEquals("GROSS_MONTHLY_INCOME_0", "1000.00", ccap);
		assertPdfFieldEquals("GROSS_MONTHLY_INCOME_1", "2000.00", ccap);
		assertPdfFieldEquals("NON_SELF_EMPLOYMENT_PAY_FREQUENCY_TEXT_0", "Daily", ccap);
		assertPdfFieldEquals("NON_SELF_EMPLOYMENT_GROSS_MONTHLY_INCOME_0", "1000.00", ccap);
		assertPdfFieldEquals("NON_SELF_EMPLOYMENT_PAY_FREQUENCY_TEXT_1", "Daily", ccap);
		assertPdfFieldEquals("NON_SELF_EMPLOYMENT_GROSS_MONTHLY_INCOME_1", "2000.00", ccap);
		
		assertPdfFieldEquals("PRINCIPLE_WAGE_EARNER", "I want to talk with my worker first.", caf);
		
	}
	
	@Test
	void shouldMapEachDayJobToPDFSingleApplicant() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("SNAP");
		fillInRequiredPages();
		postWithQueryParam("incomeByJob", "option", "0");
		postExpectingSuccess("employersName", "employersName", "someEmployerName");
		postExpectingSuccess("selfEmployment", "selfEmployment", "false");
		postExpectingSuccess("paidByTheHour", "paidByTheHour", "false");
		postExpectingSuccess("payPeriod", "payPeriod", "EVERY_DAY");
		postExpectingSuccess("incomePerPayPeriod", "incomePerPayPeriod", "1000");
		postWithQueryParam("jobBuilder", "option", "0");
		
		postExpectingSuccess("principalWageEarner", "principalWageEarner", "Dwight Schrute");
		
		var caf = downloadCafClientPDF();
		
		assertPdfFieldEquals("INCOME_PER_PAY_PERIOD_0", "", caf);
		assertPdfFieldEquals("PAY_FREQUENCY_0", "Daily", caf);
		assertPdfFieldEquals("GROSS_MONTHLY_INCOME_0", "1000.00", caf);
		assertPdfFieldEquals("MONEY_MADE_LAST_MONTH", "1000.00", caf);
		assertPdfFieldEquals("dummyFieldName20", "1000.00", caf);
		
		assertPdfFieldEquals("PRINCIPLE_WAGE_EARNER", "Dwight Schrute", caf);
	}

	@Test
	void shouldMapJobLastThirtyDayIncomeAllBlankIsUndetermined() throws Exception {
		fillOutPersonalInfo();
		selectPrograms("CASH");
		addHouseholdMembersWithProgram("CCAP");
		fillInRequiredPages();

		// Add a job for Jim
		postWithQueryParam("incomeByJob", "option", "0");
		String jim = getJimFullNameAndId();
		postExpectingSuccess("householdSelectionForIncome", "whoseJobIsIt", jim);
		postExpectingSuccess("employersName", "employersName", "someEmployerName");
		postExpectingSuccess("selfEmployment", "selfEmployment", "false");
		postExpectingSuccess("lastThirtyDaysJobIncome", "lastThirtyDaysJobIncome", "");

		// Add a job for Dwight
		postWithQueryParam("incomeByJob", "option", "0");
		String me = getApplicantFullNameAndId();
		postExpectingSuccess("householdSelectionForIncome", "whoseJobIsIt", me);
		postExpectingSuccess("employersName", "employersName", "someEmployerName");
		postExpectingSuccess("selfEmployment", "selfEmployment", "false");
		postExpectingSuccess("lastThirtyDaysJobIncome", "lastThirtyDaysJobIncome", "");

		var caf = downloadCafClientPDF();
		assertPdfFieldIsEmpty("SNAP_EXPEDITED_ELIGIBILITY", caf);
	}
	
	@Test
	void shouldMapStudentFinancialAid() throws Exception {
		selectPrograms("CASH");
		postExpectingSuccess("studentFinancialAid", "studentFinancialAid", "true");
		
		var caf = downloadCafClientPDF();
		
		assertPdfFieldEquals("STUDENT_FINANCIAL_AID", "Yes", caf);
		
		postExpectingSuccess("studentFinancialAid", "studentFinancialAid", "false");
		
		caf = downloadCafClientPDF();
		
		assertPdfFieldEquals("STUDENT_FINANCIAL_AID", "No", caf);
	}

	@Test
	void shouldNotAddAuthorizedRepFieldsIfNo() throws Exception {
		selectPrograms("CASH");
		postExpectingSuccess("authorizedRep", "communicateOnYourBehalf", "false");

		var caf = downloadCafClientPDF();
		assertPdfFieldEquals("AUTHORIZED_REP_FILL_OUT_FORM", "Off", caf);
		assertPdfFieldEquals("AUTHORIZED_REP_GET_NOTICES", "Off", caf);
		assertPdfFieldEquals("AUTHORIZED_REP_SPEND_ON_YOUR_BEHALF", "Off", caf);
	}

	@Test
	void shouldMapRecognizedUtmSourceCCAP() throws Exception {
		selectPrograms("CCAP");

		getWithQueryParam("identifyCountyBeforeApplying", "utm_source", CHILDCARE_WAITING_LIST_UTM_SOURCE);
		fillInRequiredPages();

		var ccap = downloadCcapClientPDF();
		assertPdfFieldEquals("UTM_SOURCE", "FROM BSF WAITING LIST", ccap);
	}

	@Test
	void shouldNotMapRecognizedUtmSourceCAF() throws Exception {
		selectPrograms("CASH");
		getWithQueryParam("identifyCountyBeforeApplying", "utm_source", CHILDCARE_WAITING_LIST_UTM_SOURCE);
		var caf = downloadCafClientPDF();
		assertPdfFieldIsEmpty("UTM_SOURCE", caf);
	}

	@Test 
	void ccapShouldHaveExpectedPage() throws Exception{
		// Run a simple CCAP flow
		fillOutPersonalInfo();
		selectPrograms("CCAP");
		addHouseholdMembersWithProgram("NONE");
		String jimHalpertId = getFirstHouseholdMemberId();
		postExpectingSuccess("childrenInNeedOfCare", "whoNeedsChildCare",
				List.of("Dwight Schrute applicant", "Jim Halpert " + jimHalpertId));
		postExpectingSuccess("doYouHaveChildCareProvider", "hasChildCareProvider", "false");
		postExpectingSuccess("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome", List.of("NONE_OF_THE_ABOVE"));
		postExpectingSuccess("housingSubsidy", "hasHousingSubsidy", "false");
		postExpectingSuccess("livingSituation", "livingSituation", "PAYING_FOR_HOUSING_WITH_RENT_LEASE_OR_MORTGAGE");
		postExpectingSuccess("goingToSchool", "goingToSchool", "false");
		postExpectingSuccess("pregnant", "isPregnant", "false");
		postExpectingSuccess("migrantFarmWorker", "migrantOrSeasonalFarmWorker", "false");
		postExpectingSuccess("citizenship", "citizenshipStatus", "BIRTH_RIGHT");
		postExpectingSuccess("workChanges", "workChanges", "GO_ON_STRIKE");
		postExpectingSuccess("tribalNationMember", "isTribalNationMember", "false");
		postExpectingSuccess("employmentStatus", "areYouWorking", "false");
		postExpectingSuccess("jobSearch", "currentlyLookingForJob", "false");
		postExpectingSuccess("unearnedIncome", "unearnedIncome", List.of("NO_UNEARNED_INCOME_SELECTED"));
		postExpectingSuccess("otherUnearnedIncome", "otherUnearnedIncome",
				List.of("NO_OTHER_UNEARNED_INCOME_SELECTED"));
		postExpectingSuccess("futureIncome", "additionalIncomeInfo", "");
		postExpectingSuccess("medicalExpenses", "medicalExpenses", List.of("NONE_OF_THE_ABOVE"));
		postExpectingSuccess("supportAndCare", "supportAndCare", "false");
		postExpectingSuccess("assets", "assets", List.of("VEHICLE"));
		postExpectingSuccess("soldAssets", "haveSoldAssets", "false");
		postExpectingSuccess("registerToVote", "registerToVote", "NO_ALREADY_REGISTERED");
		postExpectingSuccess("healthcareCoverage", "healthcareCoverage", "YES");
		postExpectingSuccess("authorizedRep", "helpWithBenefits", "false");
		postExpectingSuccess("additionalInfo", "caseNumber", "");
		postExpectingSuccess("raceAndEthnicity", "raceAndEthnicity", "WHITE");
		postExpectingRedirect("legalStuff", Map.of("agreeToTerms", List.of("true"), "drugFelony", List.of()),
				"signThisApplication");
		postExpectingSuccess("signThisApplication", "applicantSignature", "Dwight Schrute");
		// Submit the application
		submitApplication();

	}
	// Verify that the CCAP PDF which is generated for the caseworker has all of the
	// pieces\pages that it should have.
	@Test
	void ccapPdfForCaseWorkerShouldHaveExpectedPages() throws Exception {
		// Run a simple CCAP flow
		ccapShouldHaveExpectedPage(); 

		// Generate the CCAP PDF, caseworker version
		ApplicationFile ccapFile = pdfGenerator.generate(applicationData.getId(), CCAP, Recipient.CASEWORKER);
		byte[] ccapBytes = ccapFile.getFileBytes();
		PDDocument pdDocument = Loader.loadPDF(ccapBytes);

		// The CCAP PDF version for the caseworker should have 19 pages (based on viewing a real example)
		int pageCount = pdDocument.getNumberOfPages();
		assert (pageCount == 22);

		// Strip out all text so that we can search it for specific strings
		PDFTextStripper pdfStripper = new PDFTextStripper();
		String text = pdfStripper.getText(pdDocument);

		assertTrue(text.contains("Attached is a new MNbenefits Application")); // cover-pages.pdf
		assertTrue(text.contains("Minnesota Child Care Assistance Program Application")); // ccap-body-caseworker-page1.pdf
		assertTrue(text.contains("2. Family members")); // ccap-body.pdf
		assertTrue(text.contains("Authorization to share information for fraud investigation and audits.")); // ccap-body.pdf
		assertTrue(text.contains("Perjury and general declarations")); // ccap-body-perjury-and-general-declarations.pdf
		assertTrue(text.contains("Use this space if you need additional room")); // ccap-body-additional-room.pdf

		pdDocument.close();
	}
	// Verify that the CCAP PDF which is generated for the client has all of the
		@Test 
		void ccapPdfForClientShouldHaveExpectedPages() throws Exception {

			ccapShouldHaveExpectedPage();

			// Download the CCAP, the version for a caseworker. Note: /download downloads a
			// .zip file
			ResultActions resultActions = mockMvc.perform(get("/download")
					.with(oauth2Login().attributes(attrs -> attrs.put("email", ADMIN_EMAIL))).session(session));
			byte[] downloadBytes = resultActions.andReturn().getResponse().getContentAsByteArray();
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(downloadBytes);
			// The /download endpoint downloads a .zip file
			ZipInputStream zipFile = new ZipInputStream(byteArrayInputStream);
			List<File> zippedFiles = unzip(zipFile);

			// Extract the CCAP file
			File ccapFile = zippedFiles.stream().filter(file -> getDocumentType(file).equals(CCAP)).toList().get(0);
			byte[] ccapBytes = FileUtils.readFileToByteArray(ccapFile);
			// PDAcroForm pdAcroForm =
			// Loader.loadPDF(FileUtils.readFileToByteArray(ccapFile)).getDocumentCatalog().getAcroForm();
			PDDocument pdDocument = Loader.loadPDF(ccapBytes);

			// The CCAP PDF should have 29 pages (based on viewing a real example)
			int pageCount = pdDocument.getNumberOfPages();
			assert (pageCount == 35);

			// Strip out all text so that we can search it for specific strings
			PDFTextStripper pdfStripper = new PDFTextStripper();
			String text = pdfStripper.getText(pdDocument);
			
			assertTrue(text.contains("Read these instructions before you fill out the application.")); // ccap-headers.pdf
			assertTrue(text.contains("Your responsibilities")); // ccap-footers.pdf
			assertTrue(text.contains("Attached is a new MNbenefits Application")); // cover-pages.pdf
			assertTrue(text.contains("Minnesota Child Care Assistance Program Application")); //HDR/ ccap-body-caseworker-page1.pdf
			assertTrue(text.contains("2. Family members")); /// ccap-body.pdf
			assertTrue(text.contains("Authorization to share information for fraud investigation and audits.")); /// ccap-body.pdf
			assertTrue(text.contains("Perjury and general declarations")); /// ccap-body-perjury-and-general-declarations.pdf
			assertTrue(text.contains("Use this space if you need additional room")); // ccap-body-additional-room.pdf

			pdDocument.close();
		
		}

	@Nested
	@Tag("pdf")
	class CAFandCCAP {

		@BeforeEach
		void setUp() throws Exception {
			selectPrograms("SNAP", "CCAP", "CASH");
		}

		@Test
		void shouldMapOriginalAddressIfHomeAddressDoesNotUseEnrichedAddress() throws Exception {
			String originalStreetAddress = "originalStreetAddress";
			String originalApt = "originalApt";
			String originalCity = "originalCity";
			String originalZipCode = "54321";
			String acceptedAddress = "originalStreetAddress, originalApt";
			postExpectingSuccess("homeAddress",
					Map.of("streetAddress", List.of(originalStreetAddress), "apartmentNumber", List.of(originalApt),
							"city", List.of(originalCity), "zipCode", List.of(originalZipCode), "state", List.of("MN"),
							"sameMailingAddress", List.of("false")));
			postExpectingSuccess("verifyHomeAddress", "useEnrichedAddress", "false");

			var ccap = downloadCcapClientPDF();
			assertPdfFieldEquals("APPLICANT_HOME_STREET_ADDRESS", acceptedAddress, ccap);
			assertPdfFieldEquals("APPLICANT_HOME_CITY", originalCity, ccap);
			assertPdfFieldEquals("APPLICANT_HOME_STATE", "MN", ccap);
			assertPdfFieldEquals("APPLICANT_HOME_ZIPCODE", originalZipCode, ccap);
		}

		@Test
		void shouldMapNoForSelfEmployment() throws Exception {
			addFirstJob(getApplicantFullNameAndId(), "someEmployerName");

			var caf = downloadCafClientPDF();
			assertPdfFieldEquals("SELF_EMPLOYED", "No", caf);

			var ccap = downloadCcapClientPDF();
			assertPdfFieldEquals("NON_SELF_EMPLOYMENT_EMPLOYERS_NAME_0", "someEmployerName", ccap);
			assertPdfFieldEquals("NON_SELF_EMPLOYMENT_PAY_FREQUENCY_0", "Every week", ccap);
			assertPdfFieldEquals("NON_SELF_EMPLOYMENT_GROSS_MONTHLY_INCOME_0", "4.00", ccap);
		}
		
		@Test
		void shouldMapEnrichedAddressIfHomeAddressUsesEnrichedAddress() throws Exception {
			String enrichedStreetValue = "testStreet";
			String enrichedCityValue = "testCity";
			String enrichedZipCodeValue = "testZipCode";
			String enrichedApartmentNumber = "someApt";
			String enrichedState = "someState";

			when(locationClient.validateAddress(any())).thenReturn(Optional.of(new Address(enrichedStreetValue,
					enrichedCityValue, enrichedState, enrichedZipCodeValue, enrichedApartmentNumber, "Hennepin")));

			postExpectingSuccess("homeAddress",
					Map.of("streetAddress", List.of("originalStreetAddress"), "apartmentNumber", List.of("originalApt"),
							"city", List.of("originalCity"), "zipCode", List.of("54321"), "state", List.of("MN"),
							"sameMailingAddress", List.of()));

			postExpectingSuccess("verifyHomeAddress", "useEnrichedAddress", "true");

			var caf = downloadCafClientPDF();
			var ccap = downloadCcapClientPDF();

			List.of(caf).forEach(pdf -> {
				assertPdfFieldEquals("APPLICANT_HOME_STREET_ADDRESS", enrichedStreetValue, pdf);
				assertPdfFieldEquals("APPLICANT_HOME_CITY", enrichedCityValue, pdf);
				assertPdfFieldEquals("APPLICANT_HOME_STATE", enrichedState, pdf);
				assertPdfFieldEquals("APPLICANT_HOME_ZIPCODE", enrichedZipCodeValue, pdf);
			});
			
			
			List.of(ccap).forEach(pdf -> {
				assertPdfFieldEquals("APPLICANT_HOME_STREET_ADDRESS", enrichedStreetValue, pdf);
				assertPdfFieldEquals("APPLICANT_HOME_CITY", enrichedCityValue, pdf);
				assertPdfFieldEquals("APPLICANT_HOME_STATE", enrichedState, pdf);
				assertPdfFieldEquals("APPLICANT_HOME_ZIPCODE", enrichedZipCodeValue, pdf);
			});
			
			
			
		

			assertPdfFieldEquals("APPLICANT_HOME_APT_NUMBER", enrichedApartmentNumber, caf);
		}
		
		

		@Test
		void shouldMapFullEmployeeNames() throws Exception {
			fillOutPersonalInfo();
			addHouseholdMembersWithProgram("CCAP");
			String jim = getJimFullNameAndId();
			addFirstJob(jim, "someEmployerName");

			var caf = downloadCafClientPDF();
			var ccap = downloadCcapClientPDF();

			assertPdfFieldEquals("EMPLOYEE_FULL_NAME_0", "Jim Halpert", caf);
			assertPdfFieldEquals("NON_SELF_EMPLOYMENT_EMPLOYEE_FULL_NAME_0", "Jim Halpert", ccap);
		}

		@Test
		void shouldMapJobLastThirtyDayIncomeSomeBlankIsDetermined() throws Exception {
			fillOutPersonalInfo();
			addHouseholdMembersWithProgram("CCAP");

			fillInRequiredPages();

			// Add a job for Jim
			postWithQueryParam("incomeByJob", "option", "0");
			String jim = getJimFullNameAndId();
			postExpectingSuccess("householdSelectionForIncome", "whoseJobIsIt", jim);
			postExpectingSuccess("employersName", "employersName", "someEmployerName");
			postExpectingSuccess("selfEmployment", "selfEmployment", "false");
			postExpectingSuccess("lastThirtyDaysJobIncome", "lastThirtyDaysJobIncome", "123");

			// Add a job for Dwight
			postWithQueryParam("incomeByJob", "option", "0");
			String me = getApplicantFullNameAndId();
			postExpectingSuccess("householdSelectionForIncome", "whoseJobIsIt", me);
			postExpectingSuccess("employersName", "employersName", "someEmployerName");
			postExpectingSuccess("selfEmployment", "selfEmployment", "false");
			postExpectingSuccess("lastThirtyDaysJobIncome", "lastThirtyDaysJobIncome", "");

			var caf = downloadCafClientPDF();
			var ccap = downloadCcapClientPDF();

			assertPdfFieldEquals("GROSS_MONTHLY_INCOME_0", "123.00", caf);
			assertPdfFieldEquals("MONEY_MADE_LAST_MONTH", "123.00", caf);
			assertPdfFieldEquals("SNAP_EXPEDITED_ELIGIBILITY", "SNAP", caf);

			assertPdfFieldEquals("NON_SELF_EMPLOYMENT_GROSS_MONTHLY_INCOME_0", "123.00", ccap);
		}

		@Test
		void shouldMapLivingSituationToUnknownIfNoneOfTheseIsSelectedAndShouldNotMapTemporarilyWithFriendsOrFamilyYesNo()
				throws Exception {
			fillInRequiredPages();

			postExpectingSuccess("livingSituation", "livingSituation", "UNKNOWN");

			var caf = downloadCafClientPDF();
			var ccap = downloadCcapClientPDF();

			assertPdfFieldEquals("LIVING_SITUATION", "UNKNOWN", caf);
			assertPdfFieldEquals("LIVING_SITUATION", "UNKNOWN", ccap);
			assertPdfFieldEquals("LIVING_WITH_FAMILY_OR_FRIENDS", "Off", ccap);
		}

		@Test
		void shouldMapLivingSituationToUnknownIfNotAnswered() throws Exception {
			fillInRequiredPages();
			postWithoutData("livingSituation");

			var caf = downloadCafClientPDF();
			var ccap = downloadCcapClientPDF();

			assertPdfFieldEquals("LIVING_SITUATION", "UNKNOWN", caf);
			assertPdfFieldEquals("LIVING_SITUATION", "UNKNOWN", ccap);
		}

		@Test
		void shouldMapLivingWithFamilyAndFriendsDueToEconomicHardship() throws Exception {
			fillInRequiredPages();
			postExpectingSuccess("livingSituation", "livingSituation",
					"TEMPORARILY_WITH_FRIENDS_OR_FAMILY_DUE_TO_ECONOMIC_HARDSHIP");

			var caf = downloadCafClientPDF();
			var ccap = downloadCcapClientPDF();

			assertPdfFieldEquals("LIVING_SITUATION", "TEMPORARILY_WITH_FRIENDS_OR_FAMILY", ccap);
			assertPdfFieldEquals("LIVING_SITUATION", "TEMPORARILY_WITH_FRIENDS_OR_FAMILY", caf);
			assertPdfFieldEquals("LIVING_WITH_FAMILY_OR_FRIENDS", "Yes", ccap);
		}

		@Test
		void shouldMapTribalNationMemberYesOrNoAndWhichTribalNationBoundary() throws Exception {
			fillInRequiredPages();
			postExpectingSuccess("tribalNationMember", "isTribalNationMember", "true");
			postExpectingSuccess("selectTheTribe", "selectedTribe", "Prairie Island");
			postExpectingSuccess("nationsBoundary", "livingInNationBoundary", "true");

			var caf = downloadCafClientPDF();

			assertPdfFieldEquals("BOUNDARY_MEMBER", "Yes", caf);
			assertPdfFieldEquals("TRIBAL_NATION_BOUNDARY", "Prairie Island", caf);
		}
		
		//This test still needs to exist to verify that the application created before implementation 
		//of NationOfResidence is compatible, except the No radio field will be set. 
		 @Test
		  void shouldMapTribalNationMemberYesOrNoAndWhichTribalNation() throws Exception {
			 fillInRequiredPages();
				postExpectingSuccess("tribalNationMember", "isTribalNationMember", "true");
				postExpectingSuccess("selectTheTribe", "selectedTribe", "Leech Lake");
				postExpectingSuccess("nationsBoundary", "livingInNationBoundary", "false");

				var caf = downloadCafClientPDF();

				assertPdfFieldEquals("BOUNDARY_MEMBER","No", caf);
				assertPdfFieldIsEmpty("TRIBAL_NATION_BOUNDARY", caf);
			}
		 
		 // this test verify NationBoundary
		 @Test
		  void shouldMapTribalNationMemberYesOrNoNationBoundary() throws Exception {
			 fillInRequiredPages();
				postExpectingSuccess("tribalNationMember", "isTribalNationMember", "true");
				postExpectingSuccess("selectTheTribe", "selectedTribe", "Red Lake Nation");
				postExpectingSuccess("nationsBoundary", "livingInNationBoundary", "false");

				var caf = downloadCafClientPDF();

				assertPdfFieldEquals("BOUNDARY_MEMBER","No", caf);
				assertPdfFieldIsEmpty("TRIBAL_NATION_BOUNDARY", caf);
			}
		 // this test verify NationOfResidence

		 @Test
		  void shouldMapTribalNationMemberYesOrNoAndWhichNationBoundary() throws Exception {
			 fillInRequiredPages();
				postExpectingSuccess("tribalNationMember", "isTribalNationMember", "true");
				postExpectingSuccess("selectTheTribe", "selectedTribe", "Leech Lake");
				postExpectingSuccess("nationsBoundary", "livingInNationBoundary", "true");
				postExpectingSuccess("nationOfResidence", "selectedNationOfResidence", "Red Lake Nation");

				var caf = downloadCafClientPDF();

				assertPdfFieldEquals("BOUNDARY_MEMBER","Yes", caf);
				assertPdfFieldEquals("TRIBAL_NATION_BOUNDARY", "Red Lake Nation", caf);
			}
		
		@Test
		void shouldMapTribalTANF() throws Exception {
			fillInRequiredPages();
			postExpectingSuccess("applyForTribalTANF", "applyForTribalTANF", "true");

			var caf = downloadCafClientPDF();

			assertPdfFieldEquals("PROGRAMS", "SNAP, CCAP, CASH, TRIBAL TANF", caf);
		}

		@Test
		void shouldMapProgramSelections() throws Exception {
			fillInRequiredPages();
			selectPrograms("SNAP", "CASH", "EA");

			var caf = downloadCafClientPDF();

			assertPdfFieldEquals("FOOD", "Yes", caf);
			assertPdfFieldEquals("CASH", "Yes", caf);
			assertPdfFieldEquals("EMERGENCY", "Yes", caf);
			assertPdfFieldEquals("CCAP", "Off", caf);
			assertPdfFieldEquals("PROGRAM_NONE", "Off", caf);
		}

		@Test
		void shouldMapProgramNoneSelection() throws Exception {
			fillInRequiredPages();
			selectPrograms("NONE");
			fillOutHousemateInfo("SNAP");

			var caf = downloadCafClientPDF();

			assertPdfFieldEquals("FOOD", "Off", caf);
			assertPdfFieldEquals("CASH", "Off", caf);
			assertPdfFieldEquals("EMERGENCY", "Off", caf);
			assertPdfFieldEquals("CCAP", "Off", caf);
			assertPdfFieldEquals("PROGRAM_NONE", "Yes", caf);
		}

		@Test
		void shouldMapHHMemberMoreThan5LessThan10() throws Exception {
			fillInRequiredPages();
			selectPrograms("SNAP");
			fillOutHousemateInfoMoreThanFiveLessThanTen(9);
			var caf = downloadCafClientPDF();
			assertPdfFieldEquals("FIRST_NAME_4", "householdMemberFirstName4", caf);
			assertPdfFieldEquals("LAST_NAME_4", "householdMemberLastName4", caf);
			assertPdfFieldEquals("OTHER_NAME_4", "houseHoldyMcMemberson4", caf);
			assertPdfFieldEquals("FOOD_4", "Yes", caf);
			assertPdfFieldEquals("RELATIONSHIP_4", "housemate", caf);
			assertPdfFieldEquals("DATE_OF_BIRTH_4", "09/14/1950", caf);
			assertPdfFieldEquals("SSN_4", "XXX-XX-XXXX", caf);
			assertPdfFieldEquals("MARITAL_STATUS_4", "NEVER_MARRIED", caf); 
			assertPdfFieldEquals("SEX_4", "MALE", caf);
			assertPdfFieldEquals("PREVIOUS_STATE_4", "Illinois", caf);

		}
		
		@Test
        void shouldMapHHMemberMoreThan10() throws Exception {
            fillInRequiredPages();
            selectPrograms("SNAP");
            fillOutHousemateInfoMoreThanFiveLessThanTen(12);
            var caf = downloadCafClientPDF();
            assertPdfFieldEquals("FIRST_NAME_11", "householdMemberFirstName11", caf);
            assertPdfFieldEquals("LAST_NAME_11", "householdMemberLastName11", caf);
            assertPdfFieldEquals("OTHER_NAME_11", "houseHoldyMcMemberson11", caf);
            assertPdfFieldEquals("FOOD_11", "Yes", caf);
            assertPdfFieldEquals("RELATIONSHIP_11", "housemate", caf);
            assertPdfFieldEquals("DATE_OF_BIRTH_11", "09/14/1950", caf);
            assertPdfFieldEquals("SSN_11", "XXX-XX-XXXX", caf);
            assertPdfFieldEquals("MARITAL_STATUS_11", "NEVER_MARRIED", caf); 
            assertPdfFieldEquals("SEX_11", "MALE", caf);
            assertPdfFieldEquals("PREVIOUS_STATE_11", "Illinois", caf);

        }

		@Test
		void shouldNotMapHHMemberLessThan5() throws Exception {
			fillInRequiredPages();
			selectPrograms("SNAP");
			fillOutHousemateInfoMoreThanFiveLessThanTen(3);
			var caf = downloadCafClientPDF();
			assertNull(caf.getField("FIRST_NAME_4"));
		}

		@Test
		void shouldMapNoforTemporarilyWithFriendsOrFamilyDueToEconomicHardship() throws Exception {
			fillInRequiredPages();
			postExpectingSuccess("livingSituation", "livingSituation",
					"TEMPORARILY_WITH_FRIENDS_OR_FAMILY_OTHER_REASONS");

			var caf = downloadCafClientPDF();
			var ccap = downloadCcapClientPDF();

			assertPdfFieldEquals("LIVING_SITUATION", "TEMPORARILY_WITH_FRIENDS_OR_FAMILY", ccap);
			assertPdfFieldEquals("LIVING_SITUATION", "TEMPORARILY_WITH_FRIENDS_OR_FAMILY", caf);
			assertPdfFieldEquals("LIVING_WITH_FAMILY_OR_FRIENDS", "No", ccap);
		}

		@Test
		void shouldMapNoMedicalExpensesWhenNoneSelected() throws Exception {
			fillInRequiredPages();
			postExpectingSuccess("medicalExpenses", "medicalExpenses", List.of("NONE_OF_THE_ABOVE"));

			var caf = downloadCafClientPDF();
			assertPdfFieldEquals("MEDICAL_EXPENSES_SELECTION", "NONE_SELECTED", caf);
		}

		@Test
		void shouldMapYesMedicalExpensesWhenOneSelected() throws Exception {
			fillInRequiredPages();
			postExpectingSuccess("medicalExpenses", "medicalExpenses", List.of("MEDICAL_INSURANCE_PREMIUMS"));

			var caf = downloadCafClientPDF();
			assertPdfFieldEquals("MEDICAL_EXPENSES_SELECTION", "ONE_SELECTED", caf);
		}

		/**
		 * Verify that the county instructions on the cover page contains the correct list of document destinations.
		 * This test case verifies multiple destinations for the CAF and a single destination for the CCAP.
		 * This test case uses Tribal TANF for a resident of Hennepin County and a member of Mille Lacs Band of Ojibwe.
		 * @throws Exception
		 */
		@Test
		void countyInstructionsShouldIncludeMultipleDestinationsForCafAndSingleDestinationForCcap() throws Exception {
			// setup() selects SNAP, CASH and CCAP 
			addHouseholdMembersWithProgram("None"); // Tribal TANF requires a household

	        postExpectingSuccess("tribalNationMember", "isTribalNationMember", "true");
			postExpectingSuccess("selectTheTribe", "selectedTribe", "Mille Lacs Band of Ojibwe");
			postExpectingSuccess("applyForTribalTANF", "applyForTribalTANF", "true");

		    submitApplication();
		    PDAcroForm caf = downloadCaseWorkerPDF(applicationData.getId(), Document.CAF);
		    assertPdfFieldContains("COUNTY_INSTRUCTIONS", "MNbenefits sent this application to Mille Lacs Band of Ojibwe and Hennepin County.", caf);
		    PDAcroForm ccap = downloadCaseWorkerPDF(applicationData.getId(), Document.CCAP);
		    assertPdfFieldContains("COUNTY_INSTRUCTIONS", "MNbenefits sent this application to Hennepin County.", ccap);
		}

		@Nested
		@Tag("pdf")
		class WithPersonalAndContactInfo {

			@BeforeEach
			void setUp() throws Exception {
				fillOutPersonalInfo();
				fillOutContactInfo();
			}

			@Test
			void shouldUseAdditionalIncomeInfoAsFutureIncomeWhenIncomeIs0() throws Exception {
				selectPrograms("CASH", Program.CCAP);
				postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");
				postExpectingSuccess("employmentStatus", "areYouWorking", "false");
				postExpectingRedirect("unearnedIncome", "unearnedIncome", "NO_UNEARNED_INCOME_SELECTED",
						"otherUnearnedIncome");
				postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED",
						"studentFinancialAid");
			    postExpectingRedirect("studentFinancialAid", "studentFinancialAid", "false","futureIncome");


				var additionalIncomeInfo = "Here's something else about my situation";
				postExpectingRedirect("futureIncome", "additionalIncomeInfo", additionalIncomeInfo, "startExpenses");

				var caf = downloadCafClientPDF();
				var ccap = downloadCcapClientPDF();
				List.of(caf, ccap).forEach(pdf -> { 
					assertPdfFieldEquals("ADDITIONAL_INCOME_INFO", additionalIncomeInfo, pdf);
				});
			}

			@Test
			void shouldMapOriginalHomeAddressToMailingAddressIfSameMailingAddressIsTrueAndUseEnrichedAddressIsFalse()
					throws Exception {
				String originalStreetAddress = "originalStreetAddress";
				String originalApt = "originalApt";
				String originalCity = "originalCity";
				String originalZipCode = "54321";
				String originalStreetAddressCCAP = "originalStreetAddress, originalApt";
				
				postExpectingSuccess("homeAddress",
						Map.of("streetAddress", List.of(originalStreetAddress), "apartmentNumber", List.of(originalApt),
								"city", List.of(originalCity), "zipCode", List.of(originalZipCode), "state",
								List.of("MN")));
				postExpectingSuccess("mailingAddress", "sameMailingAddress", "true"); // THE KEY DIFFERENCE
				postExpectingSuccess("verifyHomeAddress", "useEnrichedAddress", "false");

				var ccap = downloadCcapClientPDF();
				assertPdfFieldEquals("APPLICANT_MAILING_STREET_ADDRESS", originalStreetAddressCCAP, ccap);
				assertPdfFieldEquals("APPLICANT_MAILING_CITY", originalCity, ccap);
				assertPdfFieldEquals("APPLICANT_MAILING_STATE", "MN", ccap);
				assertPdfFieldEquals("APPLICANT_MAILING_ZIPCODE", originalZipCode, ccap);
				
				
			}
			
			@Test
			void shouldMapCoverPageSelfEmploymentField() throws Exception {
				postExpectingSuccess("identifyCounty", "county", "Morrison");
				addFirstJob(getApplicantFullNameAndId(), "someEmployerName");
				addSelfEmployedJob(getApplicantFullNameAndId(), "My own boss");
				completeHelperWorkflow(true);
				var ccap = downloadCcapClientPDF();
				assertPdfFieldEquals("SELF_EMPLOYMENT_0", "No", ccap);
				assertPdfFieldEquals("SELF_EMPLOYMENT_1", "Yes", ccap);
			}

			/**
			 * This test verifies the following for a CCAP application:
			 *   - when multiple options are selected on the otherUnearnedIncome page, the next page is otherUnearnedIncomeSources
			 *   - when inputs are provided on the otherUnearnedIncomeSources page:
			 *       - the next page is futureIncome
			 *       - the input type and value are written to the CCAP cover page
			 * @throws Exception
			 */
			@Test
			void shouldMapCoverPageOtherUnearnedIncomeSingle() throws Exception{
				
				selectPrograms("CCAP");

			    // Post to the otherUnearnedIncome page with multiple income types selected
				// Since there is no household, the next page will be the "otherUnearnedIncomeSources" page.
				postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", 
						List.of("RENTAL_INCOME","ANNUITY_PAYMENTS", "GIFTS", "LOTTERY_GAMBLING", "DAY_TRADING_PROCEEDS"), "otherUnearnedIncomeSources");
				// Post the inputs for the 5 other unearned income options, note this method asserts the next page's title rather than page name
				postExpectingNextPageTitle("otherUnearnedIncomeSources", Map.of("rentalIncomeAmount", List.of("100.00"), "annuityPaymentsAmount", List.of("110.00"), 
						"giftsAmount", List.of("120.00"), "lotteryGamblingAmount", List.of("130.00"), "dayTradingProceedsAmount", List.of("140.00")), "Future Income");

			    var ccap = downloadCcapClientPDF();

			    // Verify that each income type and its value is correctly reflected on the cover page
			    assertPdfFieldEquals("OTHER_INCOME_TYPE_0", "Rental income", ccap);
			    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_0", "100.00", ccap);
			    assertPdfFieldEquals("OTHER_INCOME_TYPE_1", "Annuity payments", ccap);
			    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_1", "110.00", ccap);
			    assertPdfFieldEquals("OTHER_INCOME_TYPE_2", "Gifts", ccap);
			    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_2", "120.00", ccap);
			    assertPdfFieldEquals("OTHER_INCOME_TYPE_3", "Lottery or gambling winnings", ccap);
			    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_3", "130.00", ccap);
			    assertPdfFieldEquals("OTHER_INCOME_TYPE_4", "Day trading proceeds", ccap);
			    assertPdfFieldEquals("OTHER_INCOME_AMOUNT_4", "140.00", ccap);
			}

			/**
			 * This test verifies the following for an applicant+household CAF application:
			 *   - when multiple options (5) are selected on the otherUnearnedIncome page:
			 *       - it is followed by a sequence of xxIncomeSource pages
			 *       - the next page after the last xxIncomeSoure page is the futureIncome page
			 *       - the input type and value are written to the CAF cover page
			 *       - the applicant's full name, input type, value, and frequency are written to the CAF section 14 (first two entries)
			 * @throws Exception
			 */
		    @ParameterizedTest
		    @ValueSource(strings = {"SNAP", "CCAP"})
			void shouldMapCoverPageOtherUnearnedIncomeMulti(String program) throws Exception {
				selectPrograms(program);
				fillOutPersonalInfo();
				addHouseholdMembersWithProgram(program);
				
				String me = getApplicantFullNameAndId();
				String pam = getPamFullNameAndId();

				postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", List.of("RENTAL_INCOME", "ANNUITY_PAYMENTS", "GIFTS", "LOTTERY_GAMBLING", "DAY_TRADING"), "rentalIncomeSource");
				postExpectingRedirect("rentalIncomeSource", Map.of("monthlyIncomeRental", List.of(me, pam), "rentalIncomeAmount", List.of("200", "", "201")), "annuityIncomeSource");
				postExpectingRedirect("annuityIncomeSource", Map.of("monthlyIncomeAnnuityPayments", List.of(me, pam), "annuityPaymentsAmount", List.of("210", "", "211")), "giftsIncomeSource");
				postExpectingRedirect("giftsIncomeSource", Map.of("monthlyIncomeGifts", List.of(me, pam), "giftsAmount", List.of("220", "", "221")), "lotteryIncomeSource");
				postExpectingRedirect("lotteryIncomeSource", Map.of("monthlyIncomeLotteryGambling", List.of(me, pam), "lotteryGamblingAmount", List.of("230", "", "231")), "dayTradingIncomeSource");
				PDAcroForm document;
				if (program.equals("SNAP")) {
					postExpectingRedirect("dayTradingIncomeSource", Map.of("monthlyIncomeDayTradingProceeds", List.of(me, pam), "dayTradingProceedsAmount", List.of("240", "", "241")), "advancedChildTaxCredit");
				    postExpectingRedirect("advancedChildTaxCredit", "hasAdvancedChildTaxCredit", "false","studentFinancialAid");
				    postExpectingRedirect("studentFinancialAid", "studentFinancialAid", "false","futureIncome");
				} else {
					postExpectingRedirect("dayTradingIncomeSource", Map.of("monthlyIncomeDayTradingProceeds", List.of(me, pam), "dayTradingProceedsAmount", List.of("240", "", "241")), "futureIncome");
				}
				if (program.equals("SNAP")) {
					document = downloadCafClientPDF();
				} else {
					document = downloadCcapClientPDF();
				}
			    // Verify that each income type and its value is correctly reflected on the cover page
			    // The first two are also used to write to the CAF section 14
			    assertPdfFieldEquals("OTHER_INCOME_FULL_NAME_0", "Dwight Schrute", document);
				assertPdfFieldEquals("OTHER_INCOME_TYPE_0", "Rental income", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_0", "200", document);
				
				// This will only exist on the CAF
				if (program.equals("SNAP")) {
					assertPdfFieldEquals("OTHER_INCOME_FREQUENCY_0", "Monthly", document);
					assertPdfFieldEquals("OTHER_INCOME_YES_NO", "Yes", document);

				}

			    assertPdfFieldEquals("OTHER_INCOME_FULL_NAME_1", "Pam Beesly", document);
				assertPdfFieldEquals("OTHER_INCOME_TYPE_1", "Rental income", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_1", "201", document);
				// This will only exist on the CAF
				if (program.equals("SNAP")) {
					assertPdfFieldEquals("OTHER_INCOME_FREQUENCY_1", "Monthly", document);
					assertPdfFieldEquals("OTHER_INCOME_YES_NO", "Yes", document);

				}

				assertPdfFieldEquals("OTHER_INCOME_TYPE_2", "Annuity payments", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_2", "210", document);

				assertPdfFieldEquals("OTHER_INCOME_TYPE_3", "Annuity payments", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_3", "211", document);
				
				assertPdfFieldEquals("OTHER_INCOME_TYPE_4", "Gifts", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_4", "220", document);
				
				assertPdfFieldEquals("OTHER_INCOME_TYPE_5", "Gifts", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_5", "221", document);
				
				assertPdfFieldEquals("OTHER_INCOME_TYPE_6", "Lottery or gambling winnings", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_6", "230", document);
			
				assertPdfFieldEquals("OTHER_INCOME_TYPE_7", "Lottery or gambling winnings", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_7", "231", document);
				
				assertPdfFieldEquals("OTHER_INCOME_TYPE_8", "Day trading proceeds", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_8", "240", document);
				
				assertPdfFieldEquals("OTHER_INCOME_TYPE_9", "Day trading proceeds", document);
				assertPdfFieldEquals("OTHER_INCOME_AMOUNT_9", "241", document);
			}
				
				
			@Test
			void shouldMapEnrichedHomeAddressToMailingAddressIfSameMailingAddressIsTrueAndUseEnrichedAddressIsTrue()
					throws Exception {
				String enrichedStreetValue = "testStreet";
				String enrichedCityValue = "testCity";
				String enrichedZipCodeValue = "testZipCode";
				String enrichedApartmentNumber = "someApt";
				String enrichedState = "someState";

				when(locationClient.validateAddress(any())).thenReturn(Optional.of(new Address(enrichedStreetValue,
						enrichedCityValue, enrichedState, enrichedZipCodeValue, enrichedApartmentNumber, "Hennepin")));
				postExpectingSuccess("homeAddress",
						Map.of("streetAddress", List.of("originalStreetAddress"), "apartmentNumber",
								List.of("originalApt"), "city", List.of("originalCity"), "zipCode", List.of("54321"),
								"state", List.of("MN")));
				postExpectingSuccess("mailingAddress", "sameMailingAddress", "true"); // THE KEY DIFFERENCE
				postExpectingSuccess("verifyHomeAddress", "useEnrichedAddress", "true");

				var caf = downloadCafClientPDF();
				var ccap = downloadCcapClientPDF();
				List.of(caf,ccap).forEach(pdf -> {
					assertPdfFieldEquals("APPLICANT_MAILING_STREET_ADDRESS", enrichedStreetValue, pdf);
					assertPdfFieldEquals("APPLICANT_MAILING_CITY", enrichedCityValue, pdf);
					assertPdfFieldEquals("APPLICANT_MAILING_STATE", enrichedState, pdf);
					assertPdfFieldEquals("APPLICANT_MAILING_ZIPCODE", enrichedZipCodeValue, pdf);
				});
				
				
				assertPdfFieldEquals("APPLICANT_MAILING_APT_NUMBER", enrichedApartmentNumber, caf);
			}

			@Test
			void shouldMapToOriginalMailingAddressIfSameMailingAddressIsFalseAndUseEnrichedAddressIsFalse()
					throws Exception {
				postExpectingSuccess("homeAddress",
						Map.of("isHomeless", List.of(""), "streetAddress", List.of("originalHomeStreetAddress"),
								"apartmentNumber", List.of("originalHomeApt"), "city", List.of("originalHomeCity"),
								"zipCode", List.of("54321"), "state", List.of("MN"), "sameMailingAddress", List.of("") // THE
																														// KEY
																														// DIFFERENCE
						));
				String originalStreetAddress = "originalStreetAddress";
				String originalApt = "originalApt";
				String originalCity = "originalCity";
				String originalState = "IL";
				String originalStreetAddressCCAP = "originalStreetAddress, originalApt";

				postExpectingSuccess("mailingAddress",
						Map.of("streetAddress", List.of(originalStreetAddress), "apartmentNumber", List.of(originalApt),
								"city", List.of(originalCity), "zipCode", List.of("54321"), "state",
								List.of(originalState), "sameMailingAddress", List.of("false") // THE KEY
						// DIFFERENCE
						));
				postExpectingSuccess("verifyMailingAddress", "useEnrichedAddress", "false");

				var caf = downloadCafClientPDF();
				var ccap = downloadCcapClientPDF();
				List.of(caf).forEach(pdf -> {
					assertPdfFieldEquals("APPLICANT_MAILING_STREET_ADDRESS", originalStreetAddress, pdf);
					assertPdfFieldEquals("APPLICANT_MAILING_CITY", originalCity, pdf);
					assertPdfFieldEquals("APPLICANT_MAILING_STATE", originalState, pdf);
					assertPdfFieldEquals("APPLICANT_MAILING_ZIPCODE", "54321", pdf);
				});
				
				List.of(ccap).forEach(pdf -> {
					assertPdfFieldEquals("APPLICANT_MAILING_STREET_ADDRESS", originalStreetAddressCCAP, pdf);
					assertPdfFieldEquals("APPLICANT_MAILING_CITY", originalCity, pdf);
					assertPdfFieldEquals("APPLICANT_MAILING_STATE", originalState, pdf);
					assertPdfFieldEquals("APPLICANT_MAILING_ZIPCODE", "54321", pdf);
				});

				assertPdfFieldEquals("APPLICANT_MAILING_APT_NUMBER", originalApt, caf);
			}
		}

		@Nested
		@Tag("pdf")
		class RaceAndEthinicityCAF {

			@Test
			void shouldMarkWhiteAndWriteToClientReportedFieldWithMiddleEasternOrNorthAfricanOnly() throws Exception {
				selectPrograms("SNAP");

				postExpectingSuccess("raceAndEthnicity", "raceAndEthnicity", "MIDDLE_EASTERN_OR_NORTH_AFRICAN");

				var caf = downloadCafClientPDF();
				assertPdfFieldEquals("WHITE", "Yes", caf);
				assertPdfFieldEquals("CLIENT_REPORTED", "Middle Eastern / N. African", caf);
			}

			@Test
			void shouldMarkUnableToDetermineWithHispanicLatinoOrSpanishOnly() throws Exception {
				selectPrograms("SNAP");

				postExpectingSuccess("raceAndEthnicity", "raceAndEthnicity", "HISPANIC_LATINO_OR_SPANISH");

				var caf = downloadCafClientPDF();
				assertPdfFieldEquals("HISPANIC_LATINO_OR_SPANISH", "Yes", caf);
				assertPdfFieldEquals("UNABLE_TO_DETERMINE", "Yes", caf);
			}

			@Test
			void shouldNotMarkUnableToDetermineWithHispanicLatinoOrSpanishAndAsianSelected() throws Exception {
				selectPrograms("SNAP");

				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity", List.of("ASIAN", "HISPANIC_LATINO_OR_SPANISH", "WHITE")));
				var caf = downloadCafClientPDF();
				assertPdfFieldEquals("ASIAN", "Yes", caf);
				assertPdfFieldEquals("WHITE", "Yes", caf);
				assertPdfFieldEquals("HISPANIC_LATINO_OR_SPANISH", "Yes", caf);
				assertPdfFieldEquals("UNABLE_TO_DETERMINE", "Off", caf);
			}

			@Test
			void shouldMarkWhiteWhenWhiteSelected() throws Exception {
				selectPrograms("SNAP");

				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity", List.of("ASIAN", "WHITE", "MIDDLE_EASTERN_OR_NORTH_AFRICAN")));
				var caf = downloadCafClientPDF();
				assertPdfFieldEquals("WHITE", "Yes", caf);
				assertPdfFieldEquals("ASIAN", "Yes", caf);
				assertPdfFieldEquals("HISPANIC_LATINO_OR_SPANISH", "Off", caf);
				assertPdfFieldEquals("UNABLE_TO_DETERMINE", "Off", caf);
				assertPdfFieldEquals("CLIENT_REPORTED", "", caf);
			}

			@Test
			void shouldWriteClientReportedWhenOtherRaceOrEthnicitySelected() throws Exception {
				selectPrograms("SNAP");
				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity", List.of("SOME_OTHER_RACE_OR_ETHNICITY", "ASIAN"),
								"otherRaceOrEthnicity", List.of("SomeOtherRaceOrEthnicity")));
				var caf = downloadCafClientPDF();
				assertPdfFieldEquals("CLIENT_REPORTED", "SomeOtherRaceOrEthnicity", caf);
			}

			@Test
			void shouldWriteClientReportedForOthersOnlyWhenOtherRaceOrEthnicityAndMENASelected() throws Exception {
				selectPrograms("SNAP");
				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity",
								List.of("SOME_OTHER_RACE_OR_ETHNICITY", "MIDDLE_EASTERN_OR_NORTH_AFRICAN"),
								"otherRaceOrEthnicity", List.of("SomeOtherRaceOrEthnicity")));
				var caf = downloadCafClientPDF();
				assertPdfFieldEquals("WHITE", "Off", caf);
				assertPdfFieldEquals("CLIENT_REPORTED", "SomeOtherRaceOrEthnicity", caf);
			}

			@Test
			void shouldWriteClientReportedForOthersWhenOtherRaceOrEthnicityAndWHITESelected() throws Exception {
				selectPrograms("SNAP");
				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity", List.of("SOME_OTHER_RACE_OR_ETHNICITY", "WHITE"),
								"otherRaceOrEthnicity", List.of("SomeOtherRaceOrEthnicity")));
				var caf = downloadCafClientPDF();
				assertPdfFieldEquals("WHITE", "Yes", caf);
				assertPdfFieldEquals("CLIENT_REPORTED", "SomeOtherRaceOrEthnicity", caf);
			}
		}

		@Nested
		@Tag("pdf")
		class RaceAndEthinicityCCAP {

			@Test
			void shouldMarkWhiteAndWriteToClientReportedFieldWithMiddleEasternOrNorthAfricanOnly() throws Exception {
				selectPrograms("CCAP");

				postExpectingSuccess("raceAndEthnicity", "raceAndEthnicity", "MIDDLE_EASTERN_OR_NORTH_AFRICAN");

				var ccap = downloadCcapClientPDF();
				assertPdfFieldEquals("WHITE", "Yes", ccap);
				assertPdfFieldEquals("CLIENT_REPORTED", "Middle Eastern / N. African", ccap);
			}

			@Test
			void shouldMarkUnableToDetermineWithHispanicLatinoOrSpanishOnly() throws Exception {
				selectPrograms("CCAP");

				postExpectingSuccess("raceAndEthnicity", "raceAndEthnicity", "HISPANIC_LATINO_OR_SPANISH");

				var ccap = downloadCcapClientPDF();
				assertPdfFieldEquals("HISPANIC_LATINO_OR_SPANISH", "Yes", ccap);
				assertPdfFieldEquals("UNABLE_TO_DETERMINE", "Yes", ccap);
			}

			@Test
			void shouldNotMarkUnableToDetermineWithHispanicLatinoOrSpanishAndAsianSelected() throws Exception {
				selectPrograms("CCAP");

				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity", List.of("ASIAN", "HISPANIC_LATINO_OR_SPANISH", "WHITE")));
				var ccap = downloadCcapClientPDF();
				assertPdfFieldEquals("ASIAN", "Yes", ccap);
				assertPdfFieldEquals("WHITE", "Yes", ccap);
				assertPdfFieldEquals("HISPANIC_LATINO_OR_SPANISH", "Yes", ccap);
				assertPdfFieldEquals("UNABLE_TO_DETERMINE", "Off", ccap);
			}

			@Test
			void shouldMarkWhiteWhenWhiteSelected() throws Exception {
				selectPrograms("CCAP");

				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity", List.of("ASIAN", "WHITE", "MIDDLE_EASTERN_OR_NORTH_AFRICAN")));
				var ccap = downloadCcapClientPDF();
				assertPdfFieldEquals("WHITE", "Yes", ccap);
				assertPdfFieldEquals("ASIAN", "Yes", ccap);
				assertPdfFieldEquals("HISPANIC_LATINO_OR_SPANISH", "Off", ccap);
				assertPdfFieldEquals("UNABLE_TO_DETERMINE", "Off", ccap);
				assertPdfFieldEquals("CLIENT_REPORTED", "", ccap);
			}
			
			@Test
			void verifyPastEmployment() throws Exception {
				selectPrograms("SNAP");
				postExpectingSuccess("pastEmployment", "wereYouEmployed", "true");
			    assertNavigationRedirectsToCorrectNextPage("incomeUpNext", "unearnedIncome");

				var caf = downloadCafClientPDF();
				assertPdfFieldEquals("HAS_WORKED_IN_PAST_36_MONTHS", "Yes", caf);
			}

			@Test
			void shouldWriteClientReportedWhenOtherRaceOrEthnicitySelected() throws Exception {
				selectPrograms("CCAP");
				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity", List.of("SOME_OTHER_RACE_OR_ETHNICITY", "ASIAN"),
								"otherRaceOrEthnicity", List.of("SomeOtherRaceOrEthnicity")));
				var ccap = downloadCcapClientPDF();
				assertPdfFieldEquals("CLIENT_REPORTED", "SomeOtherRaceOrEthnicity", ccap);
			}

			@Test
			void shouldWriteClientReportedForOthersOnlyWhenOtherRaceOrEthnicityAndMENASelected() throws Exception {
				selectPrograms("CCAP");
				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity",
								List.of("SOME_OTHER_RACE_OR_ETHNICITY", "MIDDLE_EASTERN_OR_NORTH_AFRICAN"),
								"otherRaceOrEthnicity", List.of("SomeOtherRaceOrEthnicity")));
				var ccap = downloadCcapClientPDF();
				assertPdfFieldEquals("WHITE", "Off", ccap);
				assertPdfFieldEquals("CLIENT_REPORTED", "SomeOtherRaceOrEthnicity", ccap);
			}

			@Test
			void shouldWriteClientReportedForOthersWhenOtherRaceOrEthnicityAndWHITESelected() throws Exception {
				selectPrograms("CCAP");
				postExpectingSuccess("raceAndEthnicity",
						Map.of("raceAndEthnicity", List.of("SOME_OTHER_RACE_OR_ETHNICITY", "WHITE"),
								"otherRaceOrEthnicity", List.of("SomeOtherRaceOrEthnicity")));
				var ccap = downloadCcapClientPDF();
				assertPdfFieldEquals("WHITE", "Yes", ccap);
				assertPdfFieldEquals("CLIENT_REPORTED", "SomeOtherRaceOrEthnicity", ccap);
			}
		}
		
		@Test
		void verifyChildCareCost() throws Exception {
			selectPrograms("SNAP");
			postExpectingSuccess("childCareCosts", "childCareCosts", "true");
			var caf = downloadCafClientPDF();
			assertPdfFieldEquals("CCAP_HAS_COSTS_FOR_CHILD_CARE", "Yes", caf);
		}
		
		@Test
		void verifyAdultCareCost() throws Exception {
			selectPrograms("SNAP");
			postExpectingSuccess("adultCareCosts", "adultCareCosts", "true");
			var caf = downloadCafClientPDF();
			assertPdfFieldEquals("COSTS_FOR_DISABLED_ADULT", "Yes", caf);
		}
		

	}
	@Nested
	@Tag("pdf")
	class ChildSupportCCAP {

		@Test
		void shouldMapChildSupportCCAP() throws Exception {
			selectPrograms("CCAP");
			fillInPersonalInfoAndContactInfoAndAddress();
			addHouseholdMembersWithProgram("CCAP");
			String jimHalpertId = getFirstHouseholdMemberId();
			postExpectingSuccess("childrenInNeedOfCare", "whoNeedsChildCare",
					List.of("Dwight Schrute applicant", "Jim Halpert " + jimHalpertId));
			postExpectingSuccess("doYouHaveChildCareProvider", "hasChildCareProvider", "false");
			postExpectingSuccess("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome",
					List.of("Dwight Schrute applicant", "Jim Halpert " + jimHalpertId));
			postExpectingSuccess("parentNotAtHomeNames", Map.of("whatAreTheParentsNames", List.of("", "Jim's Parent"),
					"childIdMap", List.of("applicant", jimHalpertId)));
			postExpectingSuccess("childCareChildSupport", Map.of("whoReceivesChildSupportPayments", List.of("", "Jim Halpert " + jimHalpertId)));
			var ccap = downloadCcapClientPDF();
			assertPdfFieldEquals("CHILDCARE_CHILD_NAME_0", "Dwight Schrute", ccap);
			assertPdfFieldEquals("CHILDCARE_CHILD_NAME_1", "Jim Halpert", ccap);
			assertPdfFieldEquals("CHILD_FULL_NAME_0", "Dwight Schrute", ccap);
			assertPdfFieldIsEmpty("PARENT_NOT_LIVING_AT_HOME_0", ccap);
			assertPdfFieldEquals("CHILD_FULL_NAME_1", "Jim Halpert", ccap);
			assertPdfFieldEquals("PARENT_NOT_LIVING_AT_HOME_1", "Jim's Parent", ccap);
			assertPdfFieldIsEmpty("CHILD_FULL_NAME_2", ccap);
			assertPdfFieldIsEmpty("PARENT_NOT_LIVING_AT_HOME_2", ccap);
			assertPdfFieldEquals("CHILDCARE_CHILDSUPPORT_0", "No", ccap);
			assertPdfFieldEquals("CHILDCARE_CHILDSUPPORT_1", "Yes", ccap);
		}

	}
			
	@Nested
	@Tag("pdf")
	class ChildCareMentalHealthCCAP {
		@Test
		void shouldMapMentalHealthFieldToCCAP() throws Exception {
			fillOutPersonalInfo();
			selectPrograms("CCAP");
			addHouseholdMembersWithProgram("CCAP");
			String jimHalpertId = getFirstHouseholdMemberId();
			postExpectingSuccess("childrenInNeedOfCare", "whoNeedsChildCare",
					List.of("Dwight Schrute applicant", "Jim Halpert " + jimHalpertId));
			postExpectingSuccess("doYouHaveChildCareProvider", "hasChildCareProvider", "false");

			postExpectingSuccess("whoHasParentNotAtHome", "whoHasAParentNotLivingAtHome", "NONE_OF_THE_ABOVE");
			postExpectingSuccess("childCareMentalHealth", "childCareMentalHealth", "true");

			postExpectingSuccess("whoNeedsChildCareForMentalHealth", "whoNeedsChildCareMentalHealth",
					List.of("Dwight Schrute applicant", "Jim Halpert " + jimHalpertId));

			postExpectingSuccess("childCareMentalHealthTimes", "childCareMentalHealthHours", List.of("5", "9"));

			fillInRequiredPages();
			var ccap = downloadCcapClientPDF();
			assertPdfFieldEquals("ADULT_REQUESTING_CHILDCARE_TO_SUPPORT_MENTAL_HEALTH_FULL_NAME_0", "Dwight Schrute",
					ccap);
			assertPdfFieldEquals("ADULT_REQUESTING_CHILDCARE_TO_SUPPORT_MENTAL_HEALTH_FULL_NAME_1", "Jim Halpert",
					ccap);
			assertPdfFieldEquals("CHILDCARE_MENTAL_HEALTH_HOURS_A_WEEK_0", "5", ccap);
			assertPdfFieldEquals("CHILDCARE_MENTAL_HEALTH_HOURS_A_WEEK_1", "9", ccap);
		}
		
		//this test verifies that CCAP created properly when we answered yes to childCareMentalHealth question
		// and applicant only
		@Test
		void shouldMapMentalHealthFieldWhenAnswerIsNo() throws Exception {
			fillOutPersonalInfo();
			selectPrograms("CCAP");
			postExpectingSuccess("addHouseholdMembers", "addHouseholdMembers", "false");			
			postExpectingSuccess("childCareMentalHealth", "childCareMentalHealth", "true");
			postExpectingSuccess("childCareMentalHealthTimes", "childCareMentalHealthHours",  "10");

			fillInRequiredPages();
			var ccap = downloadCcapClientPDF();
			assertPdfFieldEquals("ADULT_REQUESTING_CHILDCARE_TO_SUPPORT_MENTAL_HEALTH_FULL_NAME_0", "Dwight Schrute", ccap);
			assertPdfFieldEquals("CHILDCARE_MENTAL_HEALTH_HOURS_A_WEEK_0", "10", ccap);
		}
	}
	
	@Nested
	@Tag("pdf")
	class SpecialCareExpenses {
		@Test
		void shouldMapSpecialCareExpensesFieldToCAF() throws Exception {
			selectPrograms("CASH");			
			
			postExpectingSuccess("medicalExpenses", "medicalExpenses", "true");
			postExpectingSuccess("specialCareExpenses", "specialCareExpenses", List.of("REPRESENTATIVE_PAYEE_FEES", "SPECIAL_DIET_PRESCRIBED_BY_DOCTOR"));
			postExpectingSuccess("childCareCosts", "childCareCosts", "false");
			postExpectingSuccess("adultCareCosts", "adultCareCosts", "false");
			postExpectingSuccess("supportAndCare", "supportAndCare", "false");

			var caf = downloadCafClientPDF();
			
			assertPdfFieldEquals("HAVE_PAYEE_FEES", "Yes", caf);
			assertPdfFieldEquals("HAVE_CONSERVATOR_FEES", "No", caf);
			assertPdfFieldEquals("HAVE_SPECIAL_DIET", "Yes", caf);
			assertPdfFieldEquals("HAVE_HIGH_HOUSING_COSTS", "No", caf);
			assertPdfFieldEquals("CCAP_HAS_COSTS_FOR_CHILD_CARE", "No", caf);
			assertPdfFieldEquals("COSTS_FOR_DISABLED_ADULT", "No", caf);
			assertPdfFieldEquals("SUPPORT_AND_CARE", "No", caf);
			
		}
		
		
	}
	

}
