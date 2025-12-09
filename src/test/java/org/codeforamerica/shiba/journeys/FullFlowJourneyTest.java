package org.codeforamerica.shiba.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.application.FlowType.FULL;
import static org.codeforamerica.shiba.testutilities.TestUtils.getAbsoluteFilepathString;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.NO;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.YES;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.codeforamerica.shiba.testutilities.PercyTestPage;
import org.codeforamerica.shiba.testutilities.SuccessPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

@Tag("fullFlowJourney")
public class FullFlowJourneyTest extends JourneyTest {

	protected void initTestPage() {
		testPage = new PercyTestPage(driver);
	}

	@Test
	void fullApplicationWithDocumentUploads() {
		when(clock.instant()).thenReturn(LocalDateTime.of(2020, 1, 1, 10, 10).atOffset(ZoneOffset.UTC).toInstant(),
				LocalDateTime.of(2020, 1, 1, 10, 15, 30).atOffset(ZoneOffset.UTC).toInstant());

		// Assert intercom button is present on landing page
		// TODO: Note: The check for Intercom is temporarily removed due to a timeout
		// issue. This needs to be resolved and restored.
		// await().atMost(5, SECONDS).until(() ->
		// !driver.findElements(By.id("intercom-frame")).isEmpty());
		// assertThat(driver.findElement(By.id("intercom-frame"))).isNotNull();
		assertThat(driver.findElement(By.id("generalNotice"))).isNotNull();
		// Assert that the EBT Scam Alert is displayed on the landing page.
		assertThat(driver.findElement(By.id("banner-alert"))).isNotNull();

		// Verify that the "Learn more here." link works
		String landingPageWindowHandle = driver.getWindowHandle();
		testPage.clickLinkToExternalWebsite("Learn more here.");
		ArrayList<String> windowHandles = new ArrayList<String>(driver.getWindowHandles());
		driver.switchTo().window(windowHandles.get(1));
		// Temporarily commenting this out to workaround Radware Captcha issue when test
		// run in GitHub, Story 144389
		// assertThat(driver.getTitle()).isEqualTo("Recent reports of card skimming
		// affecting EBT card users");
		driver.close(); // close the tab
		driver.switchTo().window(landingPageWindowHandle);

		// Verify that the "paying for child care" link exists and links to DHS-3551-ENG
		testPage.clickLinkToExternalWebsite("More child care resources.");
		windowHandles = new ArrayList<String>(driver.getWindowHandles());
		driver.switchTo().window(windowHandles.get(1));
		assertThat(driver.getCurrentUrl()).isEqualTo("https://edocs.dhs.state.mn.us/lfserver/Public/DHS-3551-ENG");
		driver.close();
		driver.switchTo().window(landingPageWindowHandle);

		// Assert presence and functionality of the SNAP non-discrimination link on the
		// footer.
		assertThat(driver.findElement(By.id("link-snap-nds"))).isNotNull();
		goToPageBeforeSelectPrograms("Chisago");
		selectProgramsAndEnterPersonalInfo();
		fillOutHomeAndMailingAddressWithoutEnrich("12345", "someCity", "someStreetAddress", "someApartmentNumber");
		fillOutContactAndReview(true, "Chisago");
		testPage.clickLink("This looks correct", "Do you want to add household members?");
		goBackToPage("Choose Programs");
		selectAllPrograms();
		enterOutOfStateHomeAndMailingAddress();
		goToContactAndReview();
		addSpouse();
		addHouseholdMemberToVerifySpouseCannotBeSelected();
		removeSpouseAndVerifySpouseCanBeSelectedForNewHouseholdMember();

		String householdMemberFirstName = "householdMemberFirstName";
		String householdMemberLastName = "householdMemberLastName";
		String householdMemberFullName = householdMemberFirstName + " " + householdMemberLastName;

		testPage.clickButtonLink("Yes, that's everyone", "Household members not living at home");
		testPage.chooseYesOrNo("hasTemporaryAbsence", YES.getDisplayValue(), "Who are the children in need of care?");

		// Who are the children in need of childcare
		testPage.enter("whoNeedsChildCare", householdMemberFullName);
		testPage.clickButton("Continue", "Do you have a child care provider?");

		testPage.chooseYesOrNo("hasChildCareProvider", NO.getDisplayValue(), "Who has a parent not at home?");

		// Who are the children that have a parent not living at home?
		testPage.enter("whoHasAParentNotLivingAtHome", "None of these children have parents living outside the home");

		
		testPage.clickContinue("Mental health needs & child care");
		// check it skips over child support, then go back
		assertThat(testPage.getTitle()).isEqualTo("Mental health needs & child care");
		testPage.goBack();

		testPage.enter("whoHasAParentNotLivingAtHome", householdMemberFullName);
		testPage.clickContinue("Name of parent outside home");

		// Tell us the name of any parent living outside the home.
		String parentNotAtHomeName = "My child's parent";
		driver.findElement(By.name("whatAreTheParentsNames[]")).sendKeys(parentNotAtHomeName);
		testPage.clickContinue("Child support payments");

		// child support
		testPage.enter("whoReceivesChildSupportPayments", householdMemberFullName);

		// Child care mental health section
		testPage.clickContinue("Mental health needs & child care");
		// skipCondition for the whoNeedsChildCareMentalHealth page is true. Applicant
		// is only adult.
		testPage.chooseYesOrNo("childCareMentalHealth", YES.getDisplayValue(), "Time needed each week");
		testPage.enter("childCareMentalHealthHours", "20");
		testPage.clickContinue("Preparing meals together");

		// Does everyone in your household buy and prepare food with you?
		testPage.enter("isPreparingMealsTogether", YES.getDisplayValue());

		// Are you getting a housing subsidy?
		testPage.enter("hasHousingSubsidy", YES.getDisplayValue());

		// What is your current living situation?
		testPage.enter("livingSituation", "Staying in a hotel or motel");	
		testPage.clickContinue("Housing Provider");
		
		testPage.enter("housingProvider", YES.getDisplayValue());
		
		driver.findElement(By.name("housingProviderName[]")).sendKeys("Group Home Provider");
		driver.findElement(By.name("housingProviderVendorNumber[]")).sendKeys("12345");
		testPage.clickContinue("Going to school");

		// Is anyone in your household going to school right now, either full or
		// part-time?
		testPage.chooseYesOrNo("goingToSchool", YES.getDisplayValue(), "Who is going to school?");

		// Who is going to school?
		testPage.enter("whoIsGoingToSchool", List.of(householdMemberFullName));
		testPage.clickContinue("School details");

		// What school?
		testPage.enter("schoolName", "ABC School");
		testPage.clickContinue("School grade level");

		// School grade?
		assertThat(testPage.findElementById("label-0").getText())
				.contains("householdMemberFirstName householdMemberLastName");
		testPage.enter("schoolGrade", "Pre-K");
		testPage.clickContinue("School start date");

		// School Start Date?
		testPage.enter("schoolStartDate", "01/01/2024");
		testPage.clickContinue("Pregnant");

		// Is anyone in your household pregnant?
		testPage.chooseYesOrNo("isPregnant", YES.getDisplayValue(), "Household: pregnant");

		// Who is pregnant?
		testPage.enter("whoIsPregnant", "me");
		testPage.clickContinue("Expedited Migrant Farm Worker, Household");

		// Is anyone in your household a migrant or seasonal farm worker?
		testPage.chooseYesOrNo("migrantOrSeasonalFarmWorker", NO.getDisplayValue(), "Citizenship");

		// Please confirm the citizenship status of your household
		testPage.clickElementById("citizenshipStatus[]-0-NOT_CITIZEN");
		testPage.clickElementById("citizenshipStatus[]-1-BIRTH_RIGHT");
		testPage.clickContinue("Disability");

		// Does anyone in your household have a physical or mental disability that
		// prevents them from working?
		testPage.chooseYesOrNo("hasDisability", NO.getDisplayValue(), "Work changes");
		testPage.clickContinue("Work changes");

		// In the last 2 months, did anyone in your household do any of these things?
		testPage.enter("workChanges", "Went on strike");
		testPage.clickContinue("Tribal Nation member");

		// Is anyone in your household a member of a tribal nation?
		testPage.chooseYesOrNo("isTribalNationMember", YES.getDisplayValue(), "Select a Tribal Nation");

		testPage.selectFromDropdown("selectedTribe[]", "Bois Forte");
		testPage.clickContinue("Nations Boundary");

		// Are any of the tribal members in your household living in or near the
		// Nation's boundaries?
		testPage.chooseYesOrNo("livingInNationBoundary", YES.getDisplayValue(), "Tribal Nation of residence");

		// Residence is in what Nation's boundaries?
		testPage.selectFromDropdown("selectedNationOfResidence[]", "Bois Forte");
		testPage.clickContinue("Intro: Income");

		// Income & Employment
		assertThat(testPage.getElementText("milestone-step")).isEqualTo("Step 3 of 6");
		testPage.clickButtonLink("Continue", "Employment status");

		// Is anyone in your household making money from a job?
		testPage.chooseYesOrNo("areYouWorking", YES.getDisplayValue(), "Income by job");

		// Add a job for the household
		testPage.clickButtonLink("Add a job", "Household selection for income");
		testPage.enter("whoseJobIsIt", householdMemberFullName);
		testPage.clickContinue("Employer's Name");
		testPage.enter("employersName", "some employer");
		testPage.clickContinue("Self-employment");
		testPage.chooseYesOrNo("selfEmployment", YES.getDisplayValue(), "Paid by the hour");
		testPage.chooseYesOrNo("paidByTheHour", YES.getDisplayValue(), "Hourly wage");
		testPage.enter("hourlyWage", "1.00");
		testPage.clickContinue("Hours a week");
		testPage.enter("hoursAWeek", "30");
		testPage.clickContinue("Job Builder");
		testPage.goBack();
		testPage.clickButtonLink("No, I'd rather keep going", "Job Builder");

		// Add a second job and delete
		testPage.clickButtonLink("Add a job", "Household selection for income");
		testPage.enter("whoseJobIsIt", householdMemberFullName);
		testPage.clickContinue("Employer's Name");
		testPage.enter("employersName", "some employer");
		testPage.clickContinue("Self-employment");
		testPage.chooseYesOrNo("selfEmployment", YES.getDisplayValue(), "Paid by the hour");
		testPage.chooseYesOrNo("paidByTheHour", YES.getDisplayValue(), "Hourly wage");
		testPage.enter("hourlyWage", "1.00");
		testPage.clickContinue("Hours a week");
		testPage.enter("hoursAWeek", "30");
		testPage.clickContinue("Job Builder");

		// You are about to delete your job
		driver.findElement(By.id("iteration1-delete")).click();
		testPage.clickButton("Yes, remove the job", "Job Builder");

		testPage.clickButtonLink("No, that's it.", "Job Search");

		// Is anyone in the household currently looking for a job?
		testPage.chooseYesOrNo("currentlyLookingForJob", NO.getDisplayValue(), "Income Up Next");

		// Got it! You're almost done with the income section.
		testPage.clickButtonLink("Continue", "Unearned Income");

		// Does anyone in your household get income from these sources?
		testPage.enter("unearnedIncome", "Social Security");
		testPage.clickContinue("Unearned Income Source");

		// Tell us how much money is received.
		testPage.clickElementById("householdMember-me");
		testPage.enter("socialSecurityAmount", "200.30");
		testPage.clickContinue("Unearned Income");

		// Does anyone in your household get income from these other sources?
		testPage.enter("otherUnearnedIncome",
				"Other Minnesota benefits programs (Benefits like GA, MFIP, Tribal TANF or others)");
		testPage.enter("otherUnearnedIncome", "Insurance payments");
		testPage.enter("otherUnearnedIncome", "Contract for deed");
		testPage.enter("otherUnearnedIncome", "Money from a trust");
		testPage.enter("otherUnearnedIncome", "Rental income");
		testPage.enter("otherUnearnedIncome", "Health care reimbursement");
		testPage.enter("otherUnearnedIncome", "Interest or dividends");
		testPage.enter("otherUnearnedIncome", "Other payments");
		testPage.clickContinue("Insurance payments");

		// Choose who receives that income (CCAP only)
		testPage.clickElementById("householdMember-me");
		testPage.enter("insurancePaymentsAmount", "100.00");
		testPage.clickContinue("Money from a trust");
		testPage.clickElementById("householdMember-me");
		testPage.enter("trustMoneyAmount", "100.00");
		testPage.clickContinue("Rental income");
		testPage.clickElementById("householdMember-me");
		testPage.enter("rentalIncomeAmount", "100.00");
		testPage.clickContinue("Interest or dividends");
		testPage.clickElementById("householdMember-me");
		testPage.enter("interestDividendsAmount", "100.00");
		testPage.clickContinue("Health care reimbursement");
		testPage.clickElementById("householdMember-me");
		testPage.enter("healthCareReimbursementAmount", "100.00");
		testPage.clickContinue("Benefits Programs");
		testPage.clickElementById("householdMember-me");
		testPage.enter("benefitsAmount", "100.00");
		testPage.clickContinue("Contract for deed");
		testPage.clickElementById("householdMember-me");
		testPage.enter("contractForDeedAmount", "100.00");
		testPage.clickContinue("Other payments");
		testPage.clickElementById("householdMember-me");
		testPage.enter("otherPaymentsAmount", "100.00");
		testPage.clickContinue("Future Income");

		// Do you think the household will earn less money this month than last month?
		testPage.enter("earnLessMoneyThisMonth", "Yes");
		driver.findElement(By.id("additionalIncomeInfo"))
				.sendKeys("I also make a small amount of money from my lemonade stand.");
		testPage.clickContinue("Start Expenses");

		// Expenses & Deductions
		testPage.clickButtonLink("Continue", "Home Expenses");

		// Does anyone in your household pay for room and board?
		testPage.enter("homeExpenses", "Room and Board");
		testPage.clickContinue("Home expenses amount");
		assertThat(testPage.getTitle()).isEqualTo("Home expenses amount");
		// Make sure the header says room and board
		assertThat(testPage.getHeader()).isEqualTo("How much does your household pay for room and board every month?");

		testPage.goBack();

		// Does anyone in your household pay for any of these?
		testPage.enter("homeExpenses", "Rent");
		testPage.enter("homeExpenses", "Mortgage");
		testPage.clickContinue("Home expenses amount");
		// Make sure the header includes all three selections
		assertThat(testPage.getHeader())
				.isEqualTo("How much does your household pay for rent, mortgage and room and board every month?");

		// How much does your household pay for your rent and mortgage every month?
		testPage.enter("homeExpensesAmount", "123321.50");
		testPage.clickContinue("Expedited Utility Payments, Household");

		// Does anyone in your household pay for utilities?
		testPage.enter("payForUtilities", "Heating");
		testPage.clickContinue("Energy Assistance");

		// Has your household received money for energy assistance (LIHEAP) in the last
		// 12 months?
		testPage.chooseYesOrNo("energyAssistance", YES.getDisplayValue(), "Energy Assistance More Than 20");

		// Has your household received more than $20 in energy assistance this year?
		testPage.chooseYesOrNo("energyAssistanceMoreThan20", YES.getDisplayValue(), "Medical expenses");
		testPage.enter("medicalExpenses", "Dental insurance premiums");
		testPage.enter("medicalExpenses", "Vision insurance premiums");
		testPage.enter("medicalExpenses", "Medical insurance premiums");
		testPage.clickContinue("Medical expenses sources");

		// Tell us how much money is paid.
		testPage.enter("dentalInsurancePremiumAmount", "12.34");
		testPage.enter("visionInsurancePremiumAmount", "56.35");
		testPage.enter("medicalInsurancePremiumAmount", "10.90");

		testPage.clickContinue("Support and Care");

		// Does anyone in the household pay for court-ordered child support, spousal
		// support, child care support or medical care?
		testPage.chooseYesOrNo("supportAndCare", YES.getDisplayValue(), "Child care costs");
		testPage.chooseYesOrNo("childCareCosts", YES.getDisplayValue(), "Assets");

		// Does anyone in your household have any of these?
		testPage.enter("assets", "A vehicle");
		testPage.enter("assets", "Stocks, bonds, retirement accounts");
		testPage.enter("assets", "Real estate (not including your own home)");
		testPage.clickContinue("Sold assets");

		// In the last 12 months, has anyone in the household given away or sold any
		// assets?
		testPage.chooseYesOrNo("haveSoldAssets", NO.getDisplayValue(), "Submitting Application");

		// Submitting your Application
		testPage.clickButtonLink("Continue", "Register to vote");

		// Register to vote
		testPage.clickCustomButton("Yes, send me more info", 3, "Healthcare Coverage");

		// Do you currently have healthcare coverage?
		testPage.enter("healthcareCoverage", YES.getDisplayValue());
		testPage.clickContinue("Authorized Rep");

		// Do you want to assign someone to help with your benefits?
		testPage.chooseYesOrNo("helpWithBenefits", YES.getDisplayValue(), "Authorized Rep Communicate");

		// Do you want your helper to communicate with the county on your behalf?
		testPage.chooseYesOrNo("communicateOnYourBehalf", YES.getDisplayValue(), "Authorized Rep mail and notices");

		// Do you want your helper to get mail and notices for you?
		testPage.chooseYesOrNo("getMailNotices", YES.getDisplayValue(), "Authorized Rep spend on your behalf");

		// Do you want your helper to spend your benefits on your behalf?
		testPage.chooseYesOrNo("authorizedRepSpendOnYourBehalf", YES.getDisplayValue(), "Authorized Rep contact info");

		// Let's get your helpers contact information
		testPage.enter("authorizedRepFullName", "defaultFirstName defaultLastName");
		testPage.enter("authorizedRepStreetAddress", "someStreetAddress");
		testPage.enter("authorizedRepCity", "someCity");
		testPage.enter("authorizedRepZipCode", "12345");
		testPage.enter("authorizedRepPhoneNumber", "7234567890");
		testPage.clickContinue("Additional Info");

		// Is there anything else you want to share?
		driver.findElement(By.id("additionalInfo")).sendKeys("I need you to contact my work for proof of termination");
		testPage.clickContinue("Can we ask");

		// Can we ask about your race and ethnicity?
		testPage.clickButtonLink("Yes, continue", "Race and Ethnicity");

		// What races or ethnicities do you identify with?
		testPage.enter("raceAndEthnicity", List.of("Black or African American"));
		testPage.clickContinue("Legal Stuff");

		// The legal stuff.
		testPage.enter("agreeToTerms", "I agree");
		testPage.enter("drugFelony", NO.getDisplayValue());
		testPage.clickContinue("Sign this application");

		// Upload documents
		testPage.enter("applicantSignature", "this is my signature");
		testPage.clickButtonLink("Continue", "Submit application");
		testPage.clickButton("Submit application", "Submission Confirmation");
		testPage.clickButtonLink("Continue", "Adding Documents");
		testPage.clickButtonLink("Continue", "Document Recommendation");
		testPage.clickButtonLink("Add documents now", "How to add documents");
		testPage.clickButtonLink("Continue", "Upload documents");
		testDocumentUploads();

		// Finish uploading docs, view next steps, and download PDFs
		testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
		testPage.clickCustomButton("Yes, submit and finish", 10, "Additional Program Documents");

		// Verify that we navigated to the programDocuments page.
		assertThat(driver.getTitle()).isEqualTo("Additional Program Documents");
		// Assert that applicant can't resubmit docs at this point
		navigateTo("uploadDocuments");
		assertThat(driver.getTitle()).isEqualTo("Additional Program Documents");

		// Before continuing to the next steps page, verify that the programDocuments
		// pages has the expected links.
		// Note: This does not verify that the links would actually load the PDFs into a
		// new tab.
		// eDocs will open a new tab even when the PDF doesn't exist that is a bit
		// harder to verify.
		assertThat(driver.findElement(By.linkText("Domestic violence information"))).isNotNull();
		assertThat(driver.findElement(By.linkText("Do you have a disability?"))).isNotNull();
		assertThat(driver.findElement(By.linkText("Program information for cash, food, and child care programs")))
				.isNotNull();
		assertThat(driver.findElement(By.linkText("How to use your Minnesota EBT card"))).isNotNull();
		assertThat(driver.findElement(By.linkText("Income and Eligibility Verification and Work Reporting System")))
				.isNotNull();
		assertThat(driver.findElement(By.linkText("Your appeal rights"))).isNotNull();
		assertThat(driver.findElement(By.linkText("SNAP program infosheet"))).isNotNull();
		assertThat(driver.findElement(By.linkText("SNAP reporting responsibilities"))).isNotNull();
		assertThat(driver.findElement(By.linkText("Facts on voluntarily quitting your job if you are on SNAP")))
				.isNotNull();

		testPage.clickCustomButton("Continue", 10, "Your next steps");

		// Verify that we navigated to the nextSteps page.
		assertThat(driver.getTitle()).isEqualTo("Your next steps");

		// TODO: Add this back to the test when navigation is fixed. Navigation should
		// take you back to nextSteps rather than programDocuments.
		// Assert that applicant can't resubmit docs at this point
		// navigateTo("uploadDocuments");
		// assertThat(driver.getTitle()).isEqualTo("Your next steps");

		// TODO: Fix this conditional logic once the enhanced nextSteps page is fully
		// implemented.
		List<WebElement> pageElements = driver.findElements(By.id("original-next-steps"));
		testPage.clickElementById("button-a2");
		testPage.clickElementById("button-a3");
		testPage.clickElementById("button-a4");
		if (pageElements.isEmpty()) {
			List<String> expectedMessages = List.of("We received the documents you uploaded with your application.",
					"If you need to upload more documents later, you can return to our homepage and click on ‘Upload documents’ to get started.",
					"Within the next 5 days, expect a phone call from an eligibility worker with information about your next steps.",
					"Program(s) on your application may require you to talk with a worker about your application.",
					"A worker from your county or Tribal Nation will contact you to schedule an interview. Your interview can be held over the phone or face-to-face.");
			List<String> nextStepSections = driver.findElements(By.className("next-step-section")).stream()
					.map(WebElement::getText).collect(Collectors.toList());
			assertThat(nextStepSections).containsExactly(expectedMessages.toArray(new String[0]));
		}

		assertThat(driver.getTitle()).isEqualTo("Your next steps");
		// TODO: Add this back to the test when navigation is fixed. Navigation should
		// take you back to nextSteps rather than programDocuments.
		// navigateTo("documentSubmitConfirmation");
		// assertThat(driver.getTitle()).isEqualTo("Your next steps");

		testPage.clickButtonLink("Continue", "Success");
		testPage.clickButtonLink("View more programs", "Recommendations");
		assertThat(driver.getTitle()).isEqualTo("Recommendations");
		testPage.goBackToTerminalPage("Success");
		SuccessPage successPage = new SuccessPage(driver);

		// County: Chisago Tribe: Bois Forte Nation of Residence: Bois Forte Programs:
		// SNAP, CCAP, EA, GRH
		// But NO Tribal TANF so application will only go to Chisago County.
		assertThat(successPage.findElementById("submission-date").getText())
				.contains("Your application was submitted to Chisago County (888-234-1246) on January 1, 2020.");
		applicationId = downloadPdfs();
		assertThat(successPage.findElementById("confirmation-number").getText())
				.contains("Confirmation # " + applicationId);

		// CCAP fields
		assertCcapFieldEquals("APPLICATION_ID", applicationId);
		assertCcapFieldEquals("SUBMISSION_DATETIME", "01/01/2020 at 04:15 AM");
		assertCcapFieldEquals("PAY_FREQUENCY_0", "Hourly");
		assertCcapFieldEquals("EMPLOYEE_FULL_NAME_0", householdMemberFullName);
		assertCcapFieldEquals("DATE_OF_BIRTH", "01/12/1928");
		assertCcapFieldEquals("APPLICANT_SSN", "XXX-XX-XXXX");
		assertCcapFieldEquals("APPLICANT_PHONE_NUMBER", "(723) 456-7890");
		assertCcapFieldEquals("APPLICANT_EMAIL", "some@example.com");
		assertCcapFieldEquals("PHONE_OPTIN", "Yes");
		assertCcapFieldEquals("ADDITIONAL_INFO_CASE_NUMBER", "");
		assertCcapFieldEquals("EMPLOYERS_NAME_0", "some employer");
		assertCcapFieldEquals("INCOME_PER_PAY_PERIOD_0", "1.00");
		assertCcapFieldEquals("DATE_OF_BIRTH_0", "09/14/2018");
		assertCcapFieldEquals("SSN_0", "XXX-XX-XXXX");
		assertCcapFieldEquals("COUNTY_INSTRUCTIONS",
				"""
						This application was submitted to Chisago County with the information that you provided. Some parts of this application will be blank. A caseworker will follow up with you if additional information is needed.

						For more support, you can call Chisago County (888-234-1246).""");
		assertCcapFieldEquals("EMERGENCY_TYPE", "Other emergency");
		assertCcapFieldEquals("EA_COMMENTS", "my emergency!");
		assertCcapFieldEquals("PROGRAMS", "SNAP, CCAP, EA, GRH");
		assertCcapFieldEquals("FULL_NAME", "Ahmed St. George");
		assertCcapFieldEquals("UTM_SOURCE", "");
		assertCcapFieldEquals("FULL_NAME_0", householdMemberFullName);
		assertCcapFieldEquals("TRIBAL_NATION", "Bois Forte");
		assertCcapFieldEquals("PROGRAMS_0", "CCAP");
		assertCcapFieldEquals("SNAP_EXPEDITED_ELIGIBILITY", "SNAP");
		assertCcapFieldEquals("CCAP_EXPEDITED_ELIGIBILITY", "CCAP");
		assertCcapFieldEquals("GROSS_MONTHLY_INCOME_0", "120.00");
		assertCcapFieldEquals("APPLICANT_MAILING_ZIPCODE", "03104");
		assertCcapFieldEquals("APPLICANT_MAILING_CITY", "Cooltown");
		assertCcapFieldEquals("APPLICANT_MAILING_STATE", "MN");
		assertCcapFieldEquals("APPLICANT_MAILING_STREET_ADDRESS", "smarty street, 1b");
		assertCcapFieldEquals("APPLICANT_HOME_CITY", "OutOfState City");
		assertCcapFieldEquals("APPLICANT_HOME_STATE", "MN");
		assertCcapFieldEquals("APPLICANT_HOME_ZIPCODE", "88888");
		assertCcapFieldEquals("HOUSING_SUBSIDY", "Yes");
		assertCcapFieldEquals("LIVING_SITUATION", "HOTEL_OR_MOTEL");
		assertCcapFieldEquals("APPLICANT_WRITTEN_LANGUAGE_PREFERENCE", "ENGLISH");
		assertCcapFieldEquals("APPLICANT_SPOKEN_LANGUAGE_PREFERENCE", "ENGLISH");
		assertCcapFieldEquals("NEED_INTERPRETER", "Yes");
		assertCcapFieldEquals("APPLICANT_FIRST_NAME", "Ahmed");
		assertCcapFieldEquals("APPLICANT_MIDDLE_NAME", "Abdel");
		assertCcapFieldEquals("APPLICANT_LAST_NAME", "St. George");
		assertCcapFieldEquals("APPLICANT_OTHER_NAME", "defaultOtherName");
		assertCcapFieldEquals("DATE_OF_BIRTH", "01/12/1928");
		assertCcapFieldEquals("APPLICANT_SSN", "XXX-XX-XXXX");
		assertCcapFieldEquals("MARITAL_STATUS", "NEVER_MARRIED");
		assertCcapFieldEquals("APPLICANT_SEX", "FEMALE");
		assertCcapFieldEquals("APPLICANT_PHONE_NUMBER", "(723) 456-7890");
		assertCcapFieldEquals("APPLICANT_EMAIL", "some@example.com");
		assertCcapFieldEquals("APPLICANT_HOME_STREET_ADDRESS", "123 Some Street, 1b");
		assertCcapFieldEquals("ADULT_REQUESTING_CHILDCARE_LOOKING_FOR_JOB_FULL_NAME_0", "");
		assertCcapFieldEquals("ADULT_REQUESTING_CHILDCARE_GOING_TO_SCHOOL_FULL_NAME_0", "");
		assertCcapFieldEquals("STUDENT_FULL_NAME_0", householdMemberFullName);
		assertCcapFieldEquals("SCHOOL_NAME_0", "ABC School");
		assertCcapFieldEquals("SCHOOL_GRADE_0", "Pre-K");
		assertCcapFieldEquals("SCHOOL_START_DATE_0", "01/01/2024");
		assertCcapFieldEquals("CHILDCARE_CHILD_NAME_0", householdMemberFullName);
		assertCcapFieldEquals("SSI", "No");
		assertCcapFieldEquals("VETERANS_BENEFITS", "No");
		assertCcapFieldEquals("UNEMPLOYMENT", "No");
		assertCcapFieldEquals("WORKERS_COMPENSATION", "No");
		assertCcapFieldEquals("RETIREMENT", "No");
		assertCcapFieldEquals("CHILD_OR_SPOUSAL_SUPPORT", "No");
		assertCcapFieldEquals("TRIBAL_PAYMENTS", "No");
		assertCcapFieldEquals("SELF_EMPLOYMENT_EMPLOYEE_FULL_NAME_0", householdMemberFullName);
		assertCcapFieldEquals("IS_US_CITIZEN_0", "Yes");
		assertCcapFieldEquals("SOCIAL_SECURITY_FREQUENCY", "Monthly");
		assertCcapFieldEquals("MEDICAL_INSURANCE_PREMIUM_FREQUENCY", "Monthly");
		assertCcapFieldEquals("VISION_INSURANCE_PREMIUM_FREQUENCY", "Monthly");
		assertCcapFieldEquals("DENTAL_INSURANCE_PREMIUM_FREQUENCY", "Monthly");
		assertCcapFieldEquals("MEDICAL_INSURANCE_PREMIUM_AMOUNT", "10.90");
		assertCcapFieldEquals("DENTAL_INSURANCE_PREMIUM_AMOUNT", "12.34");
		assertCcapFieldEquals("VISION_INSURANCE_PREMIUM_AMOUNT", "56.35");
		assertCcapFieldEquals("IS_WORKING", "No");
		assertCcapFieldEquals("SOCIAL_SECURITY", "Yes");
		assertCcapFieldEquals("TRUST_MONEY", "Yes");
		assertCcapFieldEquals("BENEFITS", "Yes");
		assertCcapFieldEquals("INSURANCE_PAYMENTS", "Yes");
		assertCcapFieldEquals("CONTRACT_FOR_DEED", "Yes");
		assertCcapFieldEquals("HEALTH_CARE_REIMBURSEMENT", "Yes");
		assertCcapFieldEquals("INTEREST_DIVIDENDS", "Yes");
		assertCcapFieldEquals("OTHER_PAYMENTS", "Yes");
		assertCcapFieldEquals("TRUST_MONEY_AMOUNT", "100.00");
		assertCcapFieldEquals("SOCIAL_SECURITY_AMOUNT", "200.30");
		assertCcapFieldEquals("BENEFITS_AMOUNT", "100.00");
		assertCcapFieldEquals("INSURANCE_PAYMENTS_AMOUNT", "100.00");
		assertCcapFieldEquals("CONTRACT_FOR_DEED_AMOUNT", "100.00");
		assertCcapFieldEquals("TRUST_MONEY_AMOUNT", "100.00");
		assertCcapFieldEquals("HEALTH_CARE_REIMBURSEMENT_AMOUNT", "100.00");
		assertCcapFieldEquals("INTEREST_DIVIDENDS_AMOUNT", "100.00");
		assertCcapFieldEquals("OTHER_PAYMENTS_AMOUNT", "100.00");
		assertCcapFieldEquals("BENEFITS_FREQUENCY", "Monthly");
		assertCcapFieldEquals("INSURANCE_PAYMENTS_FREQUENCY", "Monthly");
		assertCcapFieldEquals("CONTRACT_FOR_DEED_FREQUENCY", "Monthly");
		assertCcapFieldEquals("TRUST_MONEY_FREQUENCY", "Monthly");
		assertCcapFieldEquals("HEALTH_CARE_REIMBURSEMENT_FREQUENCY", "Monthly");
		assertCcapFieldEquals("INTEREST_DIVIDENDS_FREQUENCY", "Monthly");
		assertCcapFieldEquals("OTHER_PAYMENTS_FREQUENCY", "Monthly");
		assertCcapFieldEquals("EARN_LESS_MONEY_THIS_MONTH", "Yes");
		assertCcapFieldEquals("ADDITIONAL_INCOME_INFO", "I also make a small amount of money from my lemonade stand.");
		assertCcapFieldEquals("HAVE_MILLION_DOLLARS", "No");
		assertCcapFieldEquals("PARENT_NOT_LIVING_AT_HOME_0", "My child's parent");
		assertCcapFieldEquals("CHILD_FULL_NAME_0", householdMemberFullName);
		assertCcapFieldEquals("SELF_EMPLOYMENT_HOURS_A_WEEK_0", "30");
		assertCcapFieldEquals("LAST_NAME_0", "householdMemberLastName");
		assertCcapFieldEquals("SEX_0", "MALE");
		assertCcapFieldEquals("DATE_OF_BIRTH_0", "09/14/2018");
		assertCcapFieldEquals("SSN_0", "XXX-XX-XXXX");
		assertCcapFieldEquals("FIRST_NAME_0", "householdMemberFirstName");
		assertCcapFieldEquals("RELATIONSHIP_0", "child");
		assertCcapFieldEquals("SELF_EMPLOYMENT_GROSS_MONTHLY_INCOME_0", "120.00");
		assertCcapFieldEquals("LIVING_WITH_FAMILY_OR_FRIENDS", "Off");
		assertCcapFieldEquals("CREATED_DATE", "2020-01-01");
		assertCcapFieldEquals("APPLICANT_SIGNATURE", "this is my signature");
		assertCcapFieldEquals("ADDITIONAL_APPLICATION_INFO", "I need you to contact my work for proof of termination");
		assertCcapFieldEquals("BLACK_OR_AFRICAN_AMERICAN", "Yes");
		assertCcapFieldEquals("HISPANIC_LATINO_OR_SPANISH_NO", "Yes");
		assertCcapFieldEquals("REGISTER_TO_VOTE", "Yes");

		// CAF
		assertCafFieldEquals("APPLICATION_ID", applicationId);
		assertCafFieldEquals("SUBMISSION_DATETIME", "01/01/2020 at 04:15 AM");
		assertCafFieldEquals("PAY_FREQUENCY_0", "Hourly");
		assertCafFieldEquals("EMPLOYEE_FULL_NAME_0", householdMemberFullName);
		assertCafFieldEquals("DATE_OF_BIRTH", "01/12/1928");
		assertCafFieldEquals("APPLICANT_SSN", "XXX-XX-XXXX");
		assertCafFieldEquals("APPLICANT_PHONE_NUMBER", "(723) 456-7890");
		assertCafFieldEquals("APPLICANT_EMAIL", "some@example.com");
		assertCafFieldEquals("PHONE_OPTIN", "Yes");
		assertCafFieldEquals("ADDITIONAL_INFO_CASE_NUMBER", "");
		assertCafFieldEquals("EMPLOYERS_NAME_0", "some employer");
		assertCafFieldEquals("INCOME_PER_PAY_PERIOD_0", "1.00");
		assertCafFieldEquals("DATE_OF_BIRTH_0", "09/14/2018");
		assertCafFieldEquals("SSN_0", "XXX-XX-XXXX");
		// County: Chisago Tribe: Bois Forte Nation of Residence: Bois Forte Programs:
		// SNAP, CCAP, EA, GRH
		// But NO Tribal TANF so application will only go to Chisago County.
		assertCafFieldEquals("COUNTY_INSTRUCTIONS",
				"""
						This application was submitted to Chisago County with the information that you provided. Some parts of this application will be blank. A caseworker will follow up with you if additional information is needed.

						For more support, you can call Chisago County (888-234-1246).""");
		assertCafFieldEquals("EMERGENCY_TYPE", "Other emergency");
		assertCafFieldEquals("EA_COMMENTS", "my emergency!");
		assertCafFieldEquals("PROGRAMS", "SNAP, CCAP, EA, GRH");
		assertCafFieldEquals("FULL_NAME", "Ahmed St. George");
		assertCcapFieldEquals("TRIBAL_NATION", "Bois Forte");
		assertCafFieldEquals("FULL_NAME_0", householdMemberFullName);
		assertCafFieldEquals("PROGRAMS_0", "CCAP");
		assertCafFieldEquals("SNAP_EXPEDITED_ELIGIBILITY", "SNAP");
		assertCafFieldEquals("CCAP_EXPEDITED_ELIGIBILITY", "CCAP");
		assertCafFieldEquals("HOURS_PER_WEEK_0", "30");
		assertCafFieldEquals("PAID_BY_HOUR_0", "Yes");
		assertCafFieldEquals("GROSS_MONTHLY_INCOME_0", "120.00");
		assertCafFieldEquals("CREATED_DATE", "2020-01-01");
		assertCafFieldEquals("HEAT", "Yes");
		assertCafFieldEquals("WATER_SEWER_SELECTION", "NEITHER_SELECTED");
		assertCafFieldEquals("ELECTRICITY", "No");
		assertCafFieldEquals("GARBAGE_REMOVAL", "No");
		assertCafFieldEquals("PHONE", "No");
		assertCafFieldEquals("APPLICANT_MAILING_ZIPCODE", "03104");
		assertCafFieldEquals("APPLICANT_MAILING_CITY", "Cooltown");
		assertCafFieldEquals("APPLICANT_MAILING_STATE", "MN");
		assertCafFieldEquals("APPLICANT_MAILING_STREET_ADDRESS", "smarty street");
		assertCafFieldEquals("APPLICANT_MAILING_APT_NUMBER", "1b");
		assertCafFieldEquals("OTHER_INCOME_TYPE_0", "Social Security (RSDI/SSDI)");
		assertCafFieldEquals("OTHER_INCOME_FULL_NAME_0", "Ahmed St. George");
		assertCafFieldEquals("OTHER_INCOME_AMOUNT_0", "200.30");
		assertCafFieldEquals("OTHER_INCOME_FREQUENCY_0", "Monthly");
		assertCafFieldEquals("OTHER_INCOME_TYPE_1",
				"Insurance payments (settlements, short- or long-term disability, etc.)");
		assertCafFieldEquals("OTHER_INCOME_FULL_NAME_1", "Ahmed St. George");
		assertCafFieldEquals("OTHER_INCOME_AMOUNT_1", "100.00");
		assertCafFieldEquals("OTHER_INCOME_FREQUENCY_1", "Monthly");
		assertCafFieldEquals("OTHER_INCOME_TYPE_2", "Trusts");
		assertCafFieldEquals("OTHER_INCOME_FULL_NAME_2", "Ahmed St. George");
		assertCafFieldEquals("OTHER_INCOME_AMOUNT_2", "100.00");
		assertCafFieldEquals("OTHER_INCOME_TYPE_3", "Rental income");
		assertCafFieldEquals("OTHER_INCOME_FULL_NAME_3", "Ahmed St. George");
		assertCafFieldEquals("OTHER_INCOME_AMOUNT_3", "100.00");
		assertCafFieldEquals("OTHER_INCOME_TYPE_4", "Interest or dividends");
		assertCafFieldEquals("OTHER_INCOME_FULL_NAME_4", "Ahmed St. George");
		assertCafFieldEquals("OTHER_INCOME_AMOUNT_4", "100.00");
		assertCafFieldEquals("OTHER_INCOME_TYPE_5", "Health care reimbursement");
		assertCafFieldEquals("OTHER_INCOME_FULL_NAME_5", "Ahmed St. George");
		assertCafFieldEquals("OTHER_INCOME_AMOUNT_5", "100.00");
		assertCafFieldEquals("OTHER_INCOME_TYPE_6", "Contract for deed");
		assertCafFieldEquals("OTHER_INCOME_FULL_NAME_6", "Ahmed St. George");
		assertCafFieldEquals("OTHER_INCOME_AMOUNT_6", "100.00");
		assertCafFieldEquals("OTHER_INCOME_TYPE_7", "Public assistance (MFIP, DWP, GA, Tribal TANF)");
		assertCafFieldEquals("OTHER_INCOME_FULL_NAME_7", "Ahmed St. George");
		assertCafFieldEquals("OTHER_INCOME_AMOUNT_7", "100.00");

		assertCafFieldEquals("HOMEOWNERS_INSURANCE", "No");
		assertCafFieldEquals("REAL_ESTATE_TAXES", "No");
		assertCafFieldEquals("ASSOCIATION_FEES", "No");
		assertCafFieldEquals("ROOM_AND_BOARD", "Yes");
		assertCafFieldEquals("RECEIVED_LIHEAP", "Yes");
		assertCafFieldEquals("REGISTER_TO_VOTE", "Yes");
		assertCafFieldEquals("SELF_EMPLOYED", "Yes");
		assertCafFieldEquals("SELF_EMPLOYED_GROSS_MONTHLY_EARNINGS", "see question 9");
		assertCafFieldEquals("PAY_FREQUENCY_0", "Hourly");
		assertCafFieldEquals("APPLICANT_HOME_APT_NUMBER", "1b");
		assertCafFieldEquals("APPLICANT_HOME_CITY", "OutOfState City");
		assertCafFieldEquals("APPLICANT_HOME_STATE", "MN");
		assertCafFieldEquals("APPLICANT_HOME_ZIPCODE", "88888");
		assertCafFieldEquals("HOUSING_SUBSIDY", "Yes");
		assertCafFieldEquals("LIVING_SITUATION", "HOTEL_OR_MOTEL");
		assertCafFieldEquals("MEDICAL_EXPENSES_SELECTION", "ONE_SELECTED");
		assertCafFieldEquals("EMPLOYEE_FULL_NAME_0", householdMemberFullName);
		assertCafFieldEquals("WHO_IS_PREGNANT", "Ahmed St. George");
		assertCafFieldEquals("IS_US_CITIZEN_0", "Not_Citizen");
		assertCafFieldEquals("IS_US_CITIZEN_1", "Citizen");
		assertCafFieldEquals("APPLICANT_WRITTEN_LANGUAGE_PREFERENCE", "ENGLISH");
		assertCafFieldEquals("APPLICANT_SPOKEN_LANGUAGE_PREFERENCE", "ENGLISH");
		assertCafFieldEquals("NEED_INTERPRETER", "Yes");
		assertCafFieldEquals("FOOD", "Yes");
		assertCafFieldEquals("CASH", "Off");
		assertCafFieldEquals("CCAP", "Yes");
		assertCafFieldEquals("EMERGENCY", "Yes");
		assertCafFieldEquals("MN_HOUSING_SUPPORT", "Yes");
		assertCafFieldEquals("HOUSING_SUPPORT_VENDOR", "Group Home Provider / 12345");
		assertCafFieldEquals("TANF", "Off");
		assertCafFieldEquals("APPLICANT_FIRST_NAME", "Ahmed");
		assertCafFieldEquals("APPLICANT_MIDDLE_NAME", "Abdel");
		assertCafFieldEquals("APPLICANT_LAST_NAME", "St. George");
		assertCafFieldEquals("APPLICANT_OTHER_NAME", "defaultOtherName");
		assertCafFieldEquals("DATE_OF_BIRTH", "01/12/1928");
		assertCafFieldEquals("APPLICANT_SSN", "XXX-XX-XXXX");
		assertCafFieldEquals("MARITAL_STATUS", "NEVER_MARRIED");
		assertCafFieldEquals("APPLICANT_SEX", "FEMALE");
		assertCafFieldEquals("DATE_OF_MOVING_TO_MN", "10/20/1993");
		assertCafFieldEquals("APPLICANT_PREVIOUS_STATE", "Chicago");
		assertCafFieldEquals("APPLICANT_PHONE_NUMBER", "(723) 456-7890");
		assertCafFieldEquals("PREPARING_MEALS_TOGETHER", "Yes");
		assertCafFieldEquals("GOING_TO_SCHOOL", "Yes");
		assertCafFieldEquals("IS_PREGNANT", "Yes");
		assertCafFieldEquals("IS_US_CITIZEN_0", "Not_Citizen");
		assertCafFieldEquals("EXPEDITED_QUESTION_2", "0.00");
		assertCafFieldEquals("HOUSING_EXPENSES", "123321.50");
		assertCafFieldEquals("HEAT", "Yes");
		assertCafFieldEquals("SUPPORT_AND_CARE", "Yes");
		assertCafFieldEquals("MIGRANT_SEASONAL_FARM_WORKER", "No");
		assertCafFieldEquals("DRUG_FELONY", "No");
		assertCafFieldEquals("APPLICANT_SIGNATURE", "this is my signature");
		assertCafFieldEquals("HAS_DISABILITY", "No");
		assertCafFieldEquals("IS_WORKING", "No");
		assertCafFieldEquals("EARN_LESS_MONEY_THIS_MONTH", "Yes");
		assertCafFieldEquals("ADDITIONAL_INCOME_INFO", "I also make a small amount of money from my lemonade stand.");
		assertCafFieldEquals("RENT", "Yes");
		assertCafFieldEquals("MORTGAGE", "Yes");
		assertCafFieldEquals("HOUSING_EXPENSES", "123321.50");
		assertCafFieldEquals("HAVE_SAVINGS", "No");
		assertCafFieldEquals("HAVE_INVESTMENTS", "Yes");
		assertCafFieldEquals("HAVE_VEHICLE", "Yes");
		assertCafFieldEquals("HAVE_SOLD_ASSETS", "No");
		assertCafFieldEquals("AUTHORIZED_REP_FILL_OUT_FORM", "Yes");
		assertCafFieldEquals("AUTHORIZED_REP_GET_NOTICES", "Yes");
		assertCafFieldEquals("AUTHORIZED_REP_SPEND_ON_YOUR_BEHALF", "Yes");
		assertCafFieldEquals("AUTHORIZED_REP_NAME", "defaultFirstName defaultLastName");
		assertCafFieldEquals("AUTHORIZED_REP_ADDRESS", "someStreetAddress");
		assertCafFieldEquals("AUTHORIZED_REP_CITY", "someCity");
		assertCafFieldEquals("AUTHORIZED_REP_ZIP_CODE", "12345");
		assertCafFieldEquals("AUTHORIZED_REP_PHONE_NUMBER", "(723) 456-7890");
		assertCafFieldEquals("ADDITIONAL_APPLICATION_INFO", "I need you to contact my work for proof of termination");
		assertCafFieldEquals("EMPLOYERS_NAME_0", "some employer");
		assertCafFieldEquals("HOURLY_WAGE_0", "1.00");
		assertCafFieldEquals("LAST_NAME_0", "householdMemberLastName");
		assertCafFieldEquals("SEX_0", "MALE");
		assertCafFieldEquals("DATE_OF_BIRTH_0", "09/14/2018");
		assertCafFieldEquals("DATE_OF_MOVING_TO_MN_0", "02/18/1950");
		assertCafFieldEquals("SSN_0", "XXX-XX-XXXX");
		assertCafFieldEquals("FIRST_NAME_0", "householdMemberFirstName");
		assertCafFieldEquals("PREVIOUS_STATE_0", "Illinois");
		assertCafFieldEquals("OTHER_NAME_0", "houseHoldyMcMemberson");
		assertCafFieldEquals("CCAP_0", "Yes");
		assertCafFieldEquals("RELATIONSHIP_0", "child");
		assertCafFieldEquals("MARITAL_STATUS_0", "NEVER_MARRIED");
		assertCafFieldEquals("GROSS_MONTHLY_INCOME_0", "120.00");
		assertCafFieldEquals("APPLICANT_HOME_STREET_ADDRESS", "123 Some Street");
		assertCafFieldEquals("MONEY_MADE_LAST_MONTH", "920.30");
		assertCafFieldEquals("BLACK_OR_AFRICAN_AMERICAN", "Yes");
		assertCafFieldEquals("HISPANIC_LATINO_OR_SPANISH_NO", "Yes");
		assertCafFieldEquals("ANYONE_TEMPORARILY_NOT_HOME", "Yes");

	}

	/**
	 * Light(er) weight version of the full flow journey test which verifies the
	 * full flow when CASH is the only program selected.
	 */
	@Test
	void fullCashApplication() {
		when(clock.instant()).thenReturn(LocalDateTime.of(2020, 1, 1, 10, 10).atOffset(ZoneOffset.UTC).toInstant(),
				LocalDateTime.of(2020, 1, 1, 10, 15, 30).atOffset(ZoneOffset.UTC).toInstant());

		// Assert intercom button is present on landing page
		// TODO: Note: The check for Intercom is temporarily removed due to a timeout
		// issue. This needs to be resolved and restored.
		// await().atMost(5, SECONDS).until(() ->
		// !driver.findElements(By.id("intercom-frame")).isEmpty());

		goToPageBeforeSelectPrograms("Chisago");

		selectProgramsAndEnterPersonalInfo(List.of(PROGRAM_CASH));
		fillOutHomeAndMailingAddressWithoutEnrich("03104", "Cooltown", "smarty street", "1b");

		fillOutContactAndReview(true, "Chisago");

		testPage.clickButtonLink("This looks correct", "Do you want to add household members?");

		// add a spouse
		testPage.chooseYesOrNo("addHouseholdMembers", YES.getDisplayValue(), "Start Household");
		testPage.clickButtonLink("Continue", "Housemate: Personal Info");
		testPage.enter("firstName", "Celia");
		testPage.enter("middleName", "Anna");
		testPage.enter("lastName", "St. George");
		testPage.enter("dateOfBirth", "10/15/1950");
		testPage.enter("maritalStatus", "Married, living with spouse");
		testPage.enter("sex", "Female");
		testPage.enter("livedInMnWholeLife", "No");
		testPage.enter("relationship", "My spouse (e.g. wife, husband)");
		testPage.enter("programs", PROGRAM_CASH);
		// This javascript scrolls the page to the bottom. Shouldn't be necessary but
		// without
		// the scroll clickContinue doesn't seem to advance to the next page.
		JavascriptExecutor js = ((JavascriptExecutor) driver);
		js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
		testPage.clickContinue("Household members");

		testPage.clickButtonLink("Yes, that's everyone", "Household members not living at home");
		testPage.chooseYesOrNo("hasTemporaryAbsence", NO.getDisplayValue(), "Children in your household");
		testPage.chooseYesOrNo("hasChildrenUnder19", NO.getDisplayValue(), "Housing subsidy");

		// Are you getting a housing subsidy?
		testPage.chooseYesOrNo("hasHousingSubsidy", NO.getDisplayValue(), "Going to school");

		// Is anyone in your household going to school right now, either full or
		// part-time?
		testPage.chooseYesOrNo("goingToSchool", NO.getDisplayValue(), "Pregnant");

		// Is anyone in your household pregnant?
		testPage.chooseYesOrNo("isPregnant", NO.getDisplayValue(), "Expedited Migrant Farm Worker, Household");

		// Is anyone in your household a migrant or seasonal farm worker?
		testPage.chooseYesOrNo("migrantOrSeasonalFarmWorker", NO.getDisplayValue(), "Citizenship");

		// Please confirm the citizenship status of your household
		testPage.clickElementById("citizenshipStatus[]-0-BIRTH_RIGHT");
		testPage.clickElementById("citizenshipStatus[]-1-BIRTH_RIGHT");
		testPage.clickContinue("Disability");
		// Does anyone in your household have a physical or mental disability that
		// prevents them from working?
		testPage.chooseYesOrNo("hasDisability", NO.getDisplayValue(), "Work changes");
		testPage.clickContinue("Work changes");
		assertThat(testPage.findElementById("workChanges-error-message-1").getText())
				.contains("Make sure you choose 'None of the above' or another option.");

		// In the last 2 months, did anyone in your household do any of these things?
		testPage.enter("workChanges", "Stopped working, quit a job or ended self-employment");
		testPage.enter("workChanges", "Refused a job offer");
		testPage.enter("workChanges", "Asked to work fewer hours");
		testPage.enter("workChanges", "Went on strike");

		testPage.clickContinue("Tribal Nation member");
		testPage.goBack();
		testPage.enter("workChanges", "None of the above");
		testPage.clickContinue("Tribal Nation member");

		testPage.goBack();
		testPage.enter("workChanges", "Went on strike");
		testPage.clickContinue("Tribal Nation member");

		// Is anyone in your household a member of a tribal nation?
		testPage.chooseYesOrNo("isTribalNationMember", NO.getDisplayValue(), "Intro: Income");

		// Income & Employment
		testPage.clickButtonLink("Continue", "Employment status");

		// Is anyone in your household making money from a job?
		testPage.chooseYesOrNo("areYouWorking", NO.getDisplayValue(), "Income Up Next");

		// Got it! unearned income is next.
		testPage.clickButtonLink("Continue", "Unearned Income");

		// Unearned income
		testPage.enter("unearnedIncome", "None");

		// otherUnearnedIncome page
		testPage.clickContinue("Unearned Income");

		testPage.enter("otherUnearnedIncome", "None");
		testPage.clickContinue("Future Income");

		// Do you think the household will earn less money this month than last month?
		testPage.enter("earnLessMoneyThisMonth", "No");
		testPage.clickContinue("Start Expenses");

		// Expenses & Deductions
		testPage.clickButtonLink("Continue", "Home Expenses");

		// Does anyone in your household pay for room and board?
		testPage.enter("homeExpenses", "None");
		testPage.clickContinue("Expedited Utility Payments, Household");

		// Does anyone in your household pay for utilities?
		testPage.enter("payForUtilities", "None");
		testPage.clickContinue("Energy Assistance");

		// Has your household received money for energy assistance (LIHEAP) in the last
		// 12 months?
		testPage.chooseYesOrNo("energyAssistance", NO.getDisplayValue(), "Support and Care");

		// Does anyone in the household pay for court-ordered child support, spousal
		// support, child care support or medical care?
		testPage.chooseYesOrNo("supportAndCare", NO.getDisplayValue(), "Child care costs");
		testPage.chooseYesOrNo("childCareCosts", NO.getDisplayValue(), "Assets");

		// Does anyone in your household have any of these?
		testPage.enter("assets", "None");
		testPage.clickContinue("Sold assets");

		// In the last 12 months, has anyone in the household given away or sold any
		// assets?
		testPage.chooseYesOrNo("haveSoldAssets", NO.getDisplayValue(), "Submitting Application");

		// Submitting your Application
		testPage.clickButtonLink("Continue", "Register to vote");

		// Register to vote
		testPage.clickCustomButton("Yes, send me more info", 3, "Healthcare Coverage");

		// Do you currently have healthcare coverage?
		testPage.enter("healthcareCoverage", YES.getDisplayValue());
		testPage.clickContinue("Authorized Rep");

		// Do you want to assign someone to help with your benefits?
		testPage.chooseYesOrNo("helpWithBenefits", NO.getDisplayValue(), "Additional Info");

		// Is there anything else you want to share?
		driver.findElement(By.id("additionalInfo")).sendKeys("I have nothing else to share");
		testPage.clickContinue("Can we ask");

		// Can we ask about your race and ethnicity?
		testPage.clickButtonLink("Yes, continue", "Race and Ethnicity");

		// What races or ethnicities do you identify with?
		testPage.enter("raceAndEthnicity", List.of("Black or African American"));
		testPage.clickContinue("Legal Stuff");

		// The legal stuff.
		testPage.enter("agreeToTerms", "I agree");
		testPage.enter("drugFelony", NO.getDisplayValue());
		testPage.clickContinue("Sign this application");

		// Sign this application (applicant)
		testPage.enter("applicantSignature", "this is my signature");
		testPage.clickContinue("Signature notification");

		// Second signature notification
		testPage.clickButtonLink("Continue without it", "Submit application");
		// Ready to submit, but just go back
		testPage.goBack();
		// Back to second signature notification page
		testPage.clickButtonLink("Add another signature", "Legal stuff - Additional adult");

		// The legal stuff (for second signature)
		testPage.clickLink("Continue without another signature", "Submit application");

		// Ready to submit
		testPage.goBack();
		// Back on the legal stuff (for second signature)
		testPage.enter("agreeToTerms", "I agree");
		testPage.clickCustomButton("Continue", 3, "Additional Adult Signature");

		// Sign this application (second signature)
		testPage.clickLink("Continue without another signature", "Submit application");
		// Submit
		testPage.goBack();
		// Sign this application (second signature)
		testPage.enter("secondSignature", "second person signature");
		testPage.clickCustomButton("Continue", 3, "Submit application");

		// Submit
		testPage.clickCustomButton("Submit application", 3, "Submission Confirmation");

		// Submission confirmation, your application has been submitted
		testPage.clickButtonLink("Continue", "Adding Documents");

		// Adding documents
		testPage.clickButtonLink("Continue", "Document Recommendation");

		// Document recommendation
		testPage.clickButtonLink("I'll do this later", "Document offboarding");

		// document off boarding
		// Verify that we navigated to the documentOffboarding page.
		assertThat(driver.getTitle()).isEqualTo("Document offboarding");
		testPage.clickButtonLink("Finish application", "Additional Program Documents");

		// program documents
		// Verify that we navigated to the programDocuments page.
		assertThat(driver.getTitle()).isEqualTo("Additional Program Documents");
		testPage.clickButtonLink("Continue", "Your next steps");

		// next steps
		testPage.clickButtonLink("Continue", "Success");

		// success page
		applicationId = downloadPdfs();
		assertThat(testPage.findElementById("confirmation-number").getText())
				.contains("Confirmation # " + applicationId);

		// CAF
		assertCafFieldEquals("APPLICATION_ID", applicationId);
		assertCafFieldEquals("SUBMISSION_DATETIME", "01/01/2020 at 04:15 AM");
		// Applicant fields
		assertCafFieldEquals("FOOD", "Off");
		assertCafFieldEquals("CASH", "Yes");
		assertCafFieldEquals("CCAP", "Off");
		assertCafFieldEquals("EMERGENCY", "Off");
	    assertCafFieldEquals("MN_HOUSING_SUPPORT", "No");
	    assertCafFieldEquals("HOUSING_SUPPORT_VENDOR", "Does not have a provider.");
		assertCafFieldEquals("MN_HOUSING_SUPPORT", "No");
		assertCafFieldEquals("TANF", "Off");
		assertCafFieldEquals("APPLICANT_SIGNATURE", "this is my signature");
		assertCafFieldEquals("CREATED_DATE", "2020-01-01");
		// Household member fields
		assertCafFieldEquals("MIDDLE_NAME_0", "Anna");
		assertCafFieldEquals("PROGRAMS_0", "CASH");
		assertCafFieldEquals("OTHER_ADULT_SIGNATURE", "second person signature");
		assertCafFieldEquals("CREATED_DATE_SIGNATURE", "2020-01-01");

		// work status fields
		assertCafFieldEquals("GO_ON_STRIKE", "Yes");
		assertCafFieldEquals("END_WORK", "Off");
		assertCafFieldEquals("REFUSE_A_JOB_OFFER", "Off");
		assertCafFieldEquals("ASK_TO_WORK_FEWER_HOURS", "Off");

		// Expecting 2 events: 1.) SubworkflowCompletedEvent, 2.)
		// ApplicationSubmittedEvent
		assertApplicationSubmittedEventWasPublished(applicationId, FULL, 2);
	}

	private void selectProgramsAndEnterPersonalInfo() {
		selectProgramsAndEnterPersonalInfo(List.of(PROGRAM_SNAP, PROGRAM_CCAP, PROGRAM_EA, PROGRAM_GRH));
	}

	private void selectProgramsAndEnterPersonalInfo(List<String> programSelections) {
		// Program Selection
		programSelections.forEach(program -> testPage.enter("programs", program));

		if (programSelections.contains(PROGRAM_EA)) {
			testPage.clickContinue("Emergency Type");
			// Emergency type page
			testPage.enter("emergencyType", "Other emergency");
			testPage.clickContinue("Other emergency");
			// Other emergency page
			testPage.enter("otherEmergency", "my emergency!");
			testPage.clickContinue("Expedited Notice");
		}

		testPage.clickButtonLink("Continue", "Intro: Basic Info");
		// Getting to know you (Personal Info intro page)
		testPage.clickButtonLink("Continue", "Personal Info");
		// Personal Info
		testPage.enter("firstName", "Ahmed");
		testPage.enter("middleName", "Abdel");
		testPage.enter("lastName", "St. George");
		testPage.enter("otherName", "defaultOtherName");
		// DOB is optional
		testPage.enter("maritalStatus", "Never married");
		testPage.enter("sex", "Female");
		testPage.enter("livedInMnWholeLife", "Yes");
		testPage.enter("moveToMnDate", "10/20/1993");
		testPage.enter("moveToMnPreviousCity", "Chicago");
		testPage.clickContinue("Home Address");
		assertThat(testPage.getTitle()).isEqualTo("Home Address");
		testPage.goBack();
		testPage.enter("dateOfBirth", "01/12/1928");
		testPage.clickContinue("Home Address");
	}

	private void selectAllPrograms() {
		List<String> programSelections = List.of(PROGRAM_SNAP, PROGRAM_CCAP, PROGRAM_EA, PROGRAM_GRH);
		testPage.enter("programs", PROGRAM_NONE);// reset programs
		// Program Selection
		programSelections.forEach(program -> testPage.enter("programs", program));
		testPage.clickContinue("Emergency Type");

		// Emergency type page - already set to "other emergency"
		testPage.clickContinue("Other emergency");
		// Other emergency page
		testPage.enter("otherEmergency", "my emergency!");
		testPage.clickButtonLink("Continue", "Expedited Notice");
		assertThat(testPage.getTitle()).isEqualTo("Expedited Notice");
		testPage.clickButtonLink("Continue", "Intro: Basic Info");

		// Getting to know you (Personal Info intro page)
		testPage.clickButtonLink("Continue", "Personal Info");
		// Personal Info
		testPage.enter("firstName", "Ahmed");
		testPage.enter("middleName", "Abdel");
		testPage.enter("lastName", "St. George");
		testPage.enter("otherName", "defaultOtherName");
		// DOB is optional
		testPage.enter("ssn", "123456789");
		testPage.enter("maritalStatus", "Never married");
		testPage.enter("sex", "Female");
		testPage.enter("livedInMnWholeLife", "Yes");
		testPage.enter("moveToMnDate", "10/20/1993");
		testPage.enter("moveToMnPreviousCity", "Chicago");
		testPage.clickContinue("Home Address");
		assertThat(testPage.getTitle()).isEqualTo("Home Address");
		testPage.goBack();
		testPage.enter("dateOfBirth", "01/12/1928");
		testPage.clickContinue("Home Address");

	}

	private void addSpouse() {
		testPage.chooseYesOrNo("addHouseholdMembers", YES.getDisplayValue(), "Start Household");
		testPage.clickButtonLink("Continue", "Housemate: Personal Info");
		testPage.enter("firstName", "Celia");
		testPage.enter("middleName", "Anna");
		testPage.enter("lastName", "St. George");
		testPage.enter("dateOfBirth", "10/15/1950");
		testPage.enter("maritalStatus", "Married, living with spouse");
		testPage.enter("sex", "Female");
		testPage.enter("livedInMnWholeLife", "No");
		testPage.enter("relationship", "My spouse (e.g. wife, husband)");
		testPage.enter("programs", "None");
		// This javascript scrolls the page to the bottom. Shouldn't be necessary but
		// without
		// the scroll clickContinue doesn't seem to advance to the next page.
		JavascriptExecutor js = ((JavascriptExecutor) driver);
		js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
		testPage.clickContinue("Household members");
	}

	private void addHouseholdMemberToVerifySpouseCannotBeSelected() {
		testPage.clickButtonLink("Add a person", "Housemate: Personal Info");
		String householdMemberFirstName = "householdMemberFirstName";
		String householdMemberLastName = "householdMemberLastName";
		testPage.enter("firstName", householdMemberFirstName);
		testPage.enter("lastName", householdMemberLastName);
		testPage.enter("otherName", "houseHoldyMcMemberson");
		testPage.enter("dateOfBirth", "09/14/2018");
		testPage.enter("maritalStatus", "Never married");
		testPage.enter("sex", "Male");
		testPage.enter("livedInMnWholeLife", "Yes"); // actually means they MOVED HERE
		testPage.enter("moveToMnDate", "02/18/1950");
		testPage.enter("moveToMnPreviousState", "Illinois");
		Select relationshipSelectWithRemovedSpouseOption = new Select(driver.findElement(By.id("relationship")));
		assertThat(relationshipSelectWithRemovedSpouseOption.getOptions().stream()
				.noneMatch(option -> option.getText().equals("My spouse (e.g. wife, husband)"))).isTrue();
		testPage.enter("relationship", "My child");
		testPage.enter("programs", PROGRAM_CCAP);
		// Assert that the programs follow up questions are shown when a program is
		// selected
		WebElement programsFollowUp = testPage.findElementById("programs-follow-up");
		assertThat(programsFollowUp.getCssValue("display")).isEqualTo("block");
		// Assert that the programs follow up is hidden when none is selected
		testPage.enter("programs", PROGRAM_NONE);
		assertThat(programsFollowUp.getCssValue("display")).isEqualTo("none");
		testPage.enter("programs", PROGRAM_CCAP);

		// Assert that the programs follow up shows again when a program is selected
		// after having selected none
		assertThat(programsFollowUp.getCssValue("display")).isEqualTo("block");
		testPage.enter("ssn", "987654321");
		testPage.clickContinue("Household members");
	}

	protected void removeSpouseAndVerifySpouseCanBeSelectedForNewHouseholdMember() {
		// You are about to delete householdMember0 as a household member.
		driver.findElement(By.id("iteration0-delete")).click();
		testPage.clickButton("Yes, remove them", "Household members");
		// Check that My Spouse is now an option again after deleting the spouse
		testPage.clickButtonLink("Add a person", "Housemate: Personal Info");
		Select relationshipSelectWithSpouseOption = new Select(driver.findElement(By.id("relationship")));
		assertThat(relationshipSelectWithSpouseOption.getOptions().stream()
				.anyMatch(option -> option.getText().equals("My spouse (e.g. wife, husband)"))).isTrue();
		testPage.goBack();
	}

	/**
	 * Call this only if phone and email have already been entered and tested
	 * before.
	 */
	private void goToContactAndReview() {
		// How can we get in touch with you?
		testPage.enter("phoneNumber", "7234567890");
		testPage.enter("email", "some@example.com");
		testPage.clickContinue("Review info");
		testPage.clickLink("This looks correct", "Do you want to add household members?");
	}

	private void testDocumentUploads() {
		// Uploading a file should change the page styling
		uploadButtonDisabledCheck();
		deleteAFile();
		assertStylingOfEmptyDocumentUploadPage();
		uploadJpgFile();
		waitForDocumentUploadToComplete();
		assertStylingOfNonEmptyDocumentUploadPage();

		// Deleting the only uploaded document should keep you on the upload document
		// screen
		assertThat(driver.findElements(By.linkText("delete")).size()).isEqualTo(1);
		deleteAFile();

		assertThat(testPage.getTitle()).isEqualTo("Upload documents");
		assertThat(driver.findElements(By.linkText("delete")).size()).isEqualTo(0);

		assertStylingOfEmptyDocumentUploadPage();

		// Uploading multiple docs should work
		uploadJpgFile();
		waitForDocumentUploadToComplete();
		assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).isEqualTo("1 file added");
		uploadPdfFile();
		waitForDocumentUploadToComplete();
		assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).isEqualTo("2 files added");
		uploadFile(getAbsoluteFilepathString("pdf-without-acroform.pdf")); // Assert that we can still upload PDFs
		waitForDocumentUploadToComplete(); // without acroforms
		assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).isEqualTo("3 files added");
		// The delete link doesn't consistently show for the pdf-without-acroform.pdf.
		// takeSnapShot shows the cancel link is usually present, so test for either of
		// them here.
		int numberOfDeleteLinks = driver.findElements(By.linkText("delete")).size();
		if (numberOfDeleteLinks == 2) {
			assertThat(driver.findElements(By.linkText("cancel")).size()).isEqualTo(1);
		} else if (numberOfDeleteLinks == 1) {
			assertThat(driver.findElements(By.linkText("cancel")).size()).isEqualTo(2);
		} else {
			assertThat(driver.findElements(By.linkText("delete")).size()).isEqualTo(3);
		}

		// After deleting a file, the order of the remaining files should be maintained
		deleteAFile();
		assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).isEqualTo("2 files added");
		var filenameTextElements = driver.findElements(By.className("filename-text"));
		var fileDetailsElements = driver.findElements(By.className("file-details"));
		assertFileDetailsAreCorrect(filenameTextElements, fileDetailsElements, 0, "test-caf", "pdf", "0.4", "MB");
		assertFileDetailsAreCorrect(filenameTextElements, fileDetailsElements, 1, "shiba", "jpg", "19.1", "KB");
	}

	private void assertFileDetailsAreCorrect(List<WebElement> filenameTextElements,
			List<WebElement> fileDetailsElements, int index, String filenameWithoutExtension, String extension,
			String size, String sizeUnit) {
		// test-caf.pdf
		var filename = getAttributeForElementAtIndex(filenameTextElements, index, "innerHTML");
		var fileDetails = getAttributeForElementAtIndex(fileDetailsElements, index, "innerHTML");

		assertThat(filename).contains(filenameWithoutExtension);
		assertThat(filename).contains(extension);
		assertThat(fileDetails).contains(size);
		assertThat(fileDetails).contains(sizeUnit);
	}

	private void assertStylingOfNonEmptyDocumentUploadPage() {
		assertThat(driver.findElement(By.id("drag-and-drop-box")).getAttribute("class"))
				.contains("drag-and-drop-box-compact");
		assertThat(driver.findElement(By.id("upload-button")).getAttribute("class"))
				.contains("grid--item width-one-third");
		assertThat(driver.findElement(By.id("vertical-header-desktop")).getAttribute("class")).contains("hidden");
		assertThat(driver.findElement(By.id("vertical-header-mobile")).getAttribute("class")).contains("hidden");
		assertThat(driver.findElement(By.id("horizontal-header-desktop")).getAttribute("class"))
				.doesNotContain("hidden");
		assertThat(driver.findElement(By.id("horizontal-header-mobile")).getAttribute("class"))
				.doesNotContain("hidden");
		assertThat(driver.findElement(By.id("upload-doc-div")).getAttribute("class")).doesNotContain("hidden");
	}

	private void assertStylingOfEmptyDocumentUploadPage() {
		assertThat(driver.findElement(By.id("drag-and-drop-box")).getAttribute("class"))
				.doesNotContain("drag-and-drop-box-compact");
		assertThat(driver.findElement(By.id("upload-button")).getAttribute("class"))
				.doesNotContain("grid--item width-one-third");
		assertThat(driver.findElement(By.id("vertical-header-desktop")).getAttribute("class")).doesNotContain("hidden");
		assertThat(driver.findElement(By.id("vertical-header-mobile")).getAttribute("class")).doesNotContain("hidden");
		assertThat(driver.findElement(By.id("horizontal-header-desktop")).getAttribute("class")).contains("hidden");
		assertThat(driver.findElement(By.id("horizontal-header-mobile")).getAttribute("class")).contains("hidden");
		assertThat(driver.findElement(By.id("upload-doc-div")).getAttribute("class")).contains("hidden");
	}
}
