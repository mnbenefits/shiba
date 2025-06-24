package org.codeforamerica.shiba.journeys;

import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.application.FlowType.EXPEDITED;
import static org.codeforamerica.shiba.application.FlowType.MINIMUM;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.NO;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.YES;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.codeforamerica.shiba.pages.Sentiment;
import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.pages.enrichment.Address;
import org.codeforamerica.shiba.testutilities.SuccessPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@Tag("minimumFlowJourney")

public class MinimumSnapFlowJourneyTest extends JourneyTest {

  private final String signature = "some signature";
  
  @Test
  void nonExpeditedFlow() {
	when(featureFlagConfiguration.get("show-wic-recommendation")).thenReturn(FeatureFlag.ON);
    // No permanent address for this test
    getToHomeAddress("Hennepin", List.of(PROGRAM_SNAP));

    // Where are you currently Living? (with home address)
    testPage.enter("zipCode", "23456");
    testPage.enter("city", "someCity");
    testPage.enter("streetAddress", "someStreetAddress");
    testPage.enter("apartmentNumber", "someApartmentNumber");
    assertThat(driver.findElement(By.id("state")).getAttribute("value")).isEqualTo("MN"); // home address page default state is MN
    testPage.enter("state", "WI"); // user can set state to something besides MN
    testPage.clickContinue("Out of State Address Notice"); // go to out of state address page, then back
    testPage.clickCustomLink("Edit my address", "Home Address");
    assertThat(driver.findElement(By.id("state")).getAttribute("value")).isEqualTo("WI");
    testPage.enter("state", "MN");
    testPage.clickContinue("Mailing address"); // go to the mailing address page
    assertThat(driver.findElement(By.id("state")).getAttribute("value")).isEqualTo("MN"); // mailing address page default state is MN
    assertThat(testPage.getTitle()).isEqualTo("Mailing address");
    testPage.goBack();

    // Where are you currently Living? (without address)
    testPage.enter("isHomeless", "I don't have a permanent address"); // check
    testPage.enter("isHomeless", "I don't have a permanent address"); // uncheck
    testPage.clickContinue("Home Address");
    assertThat(testPage.hasInputError("streetAddress")).isTrue(); // verify cleared previous inputs but state is MN by default
    assertThat(driver.findElement(By.id("state")).getAttribute("value")).isEqualTo("MN"); 
    testPage.enter("isHomeless", "I don't have a permanent address"); // check
    testPage.clickContinue("Where to send mail");

    // General Delivery
    testPage.clickSubtleLink("I will pick up mail at a General Delivery post office near me.", "City for General Delivery");
    assertThat(testPage.getTitle()).isEqualTo("City for General Delivery");
    testPage.clickContinue("City for General Delivery"); // Error on "Continue" without selecting a city
    assertThat(testPage.hasErrorText("Make sure to provide a city")).isTrue();
    testPage.selectFromDropdown("whatIsTheCity[]", "Ada");
    testPage.clickContinue("General Delivery address");

    // General Delivery address
    assertThat(testPage.getTitle()).isEqualTo("General Delivery address");
    String generalDeliveryText = testPage.getElementText("general-delivery");
    assertThat(generalDeliveryText).contains("General Delivery");
    testPage.clickButtonLink("Continue", "Contact Info");

    // Contact
    fillOutContactAndReview(false, "Hennepin");

    // Let's review your info
    assertThat(driver.findElement(By.id("homeAddress-address_message")).getText())
        .isEqualTo("No permanent address");
    assertThat(testPage.findElementById("generalDelivery_streetAddress").getText())
        .isEqualTo("Ada, MN");

    testPage.clickLink("Submit an incomplete application now with only the above information.", "Do you need help immediately?");

    // Opt not to answer expedited questions
    testPage.clickCustomButton("Finish application now", 3, "Additional Info");

    // Additional Info
    assertThat(testPage.getTitle()).isEqualTo("Additional Info");
    String additionalInfo = "Some additional information about my application";
    String caseNumber = "654321";
    driver.findElement(By.id("additionalInfo")).sendKeys(additionalInfo);
    testPage.enter("caseNumber", caseNumber);
    testPage.clickContinue("Can we ask");
    testPage.clickButtonLink("No, skip this question", "Legal Stuff");

    // Legal Stuff
    assertThat(testPage.getTitle()).isEqualTo("Legal Stuff");
    testPage.enter("agreeToTerms", "I agree");
    testPage.enter("drugFelony", NO.getDisplayValue());
    testPage.clickContinue("Sign this application");
    List<String> expectedMessages = List.of(
    		"You did not upload documents with your application today.",
    		"To upload documents later, you can return to our homepage and click on ‘Upload documents’ to get started.",
    		"Expect an eligibility worker to contact you by phone or mail with information about your next steps.\n\n"
    		+ "The time it takes to review applications can vary.",
	 		"Program(s) on your application may require you to talk with a worker about your application.",
	 		"A worker from your county or Tribal Nation will contact you to schedule an interview. Your interview can be held over the phone or face-to-face.");

    // Finish Application
    applicationId = signApplicationAndDownloadApplicationZipFiles(signature,expectedMessages);
    assertApplicationSubmittedEventWasPublished(applicationId, MINIMUM, 1);

    // PDF assertions
    assertCafContainsAllFieldsForMinimumSnapFlow(applicationId,
        "This application was submitted to Hennepin County with the information that you provided. Some parts of this application will be blank. A caseworker will follow up with you if additional information is needed.\n\nFor more support, you can call Hennepin County (612-596-1300).");
    assertCafFieldEquals("MEDICAL_EXPENSES_SELECTION", "Off");
    assertCafFieldEquals("SNAP_EXPEDITED_ELIGIBILITY", "");
    assertCafFieldEquals("DRUG_FELONY", "No");
    //assertCafFieldEquals("ADDITIONAL_APPLICATION_INFO", additionalInfo); // TODO:  Verify that ADDITIONAL_APPLICATION_INFO is no longer on the CAF
    assertCafFieldEquals("ADDITIONAL_INFO_CASE_NUMBER", caseNumber);
    assertCafFieldEquals("APPLICANT_HOME_STREET_ADDRESS", "No permanent address");
    assertCafFieldEquals("APPLICANT_HOME_APT_NUMBER", "");
    assertCafFieldEquals("APPLICANT_HOME_CITY", "");
    assertCafFieldEquals("APPLICANT_HOME_STATE", "MN");
    assertCafFieldEquals("APPLICANT_HOME_ZIPCODE", "");
    assertCafFieldEquals("APPLICANT_MAILING_STREET_ADDRESS", "General Delivery");
    assertCafFieldEquals("APPLICANT_MAILING_APT_NUMBER", "");
    assertCafFieldEquals("APPLICANT_MAILING_CITY", "Ada");
    assertCafFieldEquals("APPLICANT_MAILING_STATE", "MN");
    assertCafFieldEquals("APPLICANT_MAILING_ZIPCODE", "56510-9999");
  }
  
  @Test
  void expeditedFlow() {
	when(featureFlagConfiguration.get("show-wic-recommendation")).thenReturn(FeatureFlag.ON);
    getToHomeAddress("Hennepin", List.of(PROGRAM_SNAP));

    // Where are you currently Living?
    String homeZip = "03104";
    String homeCity = "Cooltown";
    String homeStreetAddress = "smarty street";
    String homeApartmentNumber = "1b";
    String state = "MN";
    fillOutHomeAndMailingAddress(homeZip, homeCity, homeStreetAddress, homeApartmentNumber, state);
    fillOutContactAndReview(true, "Hennepin");

    testPage.clickLink("Submit an incomplete application now with only the above information.", "Do you need help immediately?");

    // Answer expedited questions such that we will be expedited
    testPage.clickCustomButton("Yes, I want to see if I qualify", 3, "Do you want to add household members?");

    // Add Household Members
    testPage.chooseYesOrNo("addHouseholdMembers", YES.getDisplayValue(), "Thirty Day Income, Household");

    // How much money has your household made in the last 30 days?
    assertThat(driver.findElement(By.cssSelector("h1")).getText())
        .isEqualTo("How much money has your household made in the last 30 days?");
    String moneyMadeLast30Days = "1";
    testPage.enter("moneyMadeLast30Days", moneyMadeLast30Days);
    testPage.clickContinue("Savings");

    // Do you have savings?
    testPage.chooseYesOrNo("haveSavings", YES.getDisplayValue(), "Expedited Cash, Household");
    String liquidAssets = "1.00";
    testPage.enter("liquidAssets", liquidAssets);
    testPage.clickContinue("Expedited Expense, Household");

    // Home expenses
    testPage.chooseYesOrNo("payRentOrMortgage", YES.getDisplayValue(), "Expedited Expenses Amount, Household");
    String homeExpensesAmount = "333";
    testPage.enter("homeExpensesAmount", homeExpensesAmount);
    testPage.clickContinue("Expedited Utility Payments, Household");

    // Utilities
    testPage.enter("payForUtilities", "Cooling");
    testPage.clickContinue("Expedited Migrant Farm Worker, Household");

    // Migrant or Seasonal worker
    String migrantOrSeasonalFarmWorker = NO.getDisplayValue();
    testPage.chooseYesOrNo("migrantOrSeasonalFarmWorker", migrantOrSeasonalFarmWorker, "Qualify for Expedited Service");

    // You are expedited!
    assertThat(driver.findElement(By.tagName("p")).getText()).contains(
        "Your county or Tribal Nation should reach out to you to discuss your application within 24 hours.");
    testPage.clickButtonLink("Finish application", "Legal Stuff");

    // Legal Stuff
    assertThat(testPage.getTitle()).isEqualTo("Legal Stuff");
    testPage.enter("agreeToTerms", "I agree");
    testPage.enter("drugFelony", YES.getDisplayValue());
    testPage.clickContinue("Sign this application");

    // Finish Application
    List<String> expectedMessages = List.of(
    		"You did not upload documents with your application today.",
    		"To upload documents later, you can return to our homepage and click on ‘Upload documents’ to get started.",
    		"Within the next 5 days, expect a phone call from an eligibility worker with information about your next steps.",
	 		"Program(s) on your application may require you to talk with a worker about your application.",
	 		"A worker from your county or Tribal Nation will contact you to schedule an interview. Your interview can be held over the phone or face-to-face.");

    applicationId = signApplicationAndDownloadApplicationZipFiles(signature, expectedMessages);
    SuccessPage successPage = new SuccessPage(driver);
    assertThat(successPage.findElementById("snapExpeditedNotice").getText()).contains(
        "You were recommended for expedited food assistance (SNAP).");
    assertApplicationSubmittedEventWasPublished(applicationId, EXPEDITED, 1);
    // continue to the recommendations page
    testPage.clickButtonLink("View more programs", "Recommendations");
    // verify that the recommendations page does contain the "apply for healthcare coverage" item
    assertThat(testPage.elementDoesNotExistById("healthcareCoverage")).isFalse();
    // verify that the recommendations page does not contain the "paying for child care" item
    assertThat(testPage.elementDoesNotExistById("ccapCoverage")).isTrue();

    testFeedbackScreen();

    // PDF assertions
    assertCafContainsAllFieldsForMinimumSnapFlow(applicationId,
        """
            This application was submitted to Hennepin County with the information that you provided. Some parts of this application will be blank. A caseworker will follow up with you if additional information is needed.

            For more support, you can call Hennepin County (612-596-1300).""");
    assertCafFieldEquals("APPLICANT_IS_US_CITIZEN", "Off");
    assertCafFieldEquals("MEDICAL_EXPENSES_SELECTION", "Off");
    assertCafFieldEquals("SNAP_EXPEDITED_ELIGIBILITY", "SNAP");
    assertCafFieldEquals("DRUG_FELONY", "Yes");
    assertCafFieldEquals("MONEY_MADE_LAST_MONTH", moneyMadeLast30Days + ".00");
    assertCafFieldEquals("EXPEDITED_QUESTION_2", liquidAssets);
    assertCafFieldEquals("HOUSING_EXPENSES", homeExpensesAmount);
    assertCafFieldEquals("HEAT", "No"); 
    assertCafFieldEquals("AIR_CONDITIONING", "Yes");
    assertCafFieldEquals("ELECTRICITY", "No");
    assertCafFieldEquals("PHONE", "No");
    assertCafFieldEquals("NO_EXPEDITED_UTILITIES_SELECTED", "Off");  // TODO:  should this be "Off" or should it be "No"
    assertCafFieldEquals("MIGRANT_SEASONAL_FARM_WORKER", migrantOrSeasonalFarmWorker);
    // assertCafFieldEquals("HEATING_COOLING_SELECTION", "ONE_SELECTED"); // TODO: verify that HEATING_COOLING_SELECTION is no longer on the CAF
    assertCafFieldEquals("WATER_SEWER_SELECTION", "NEITHER_SELECTED");
    // assertCafFieldEquals("COOKING_FUEL", "No"); // TODO: verify that COOKING_FUEL is no longer on the CAF
    assertCafFieldEquals("HAVE_SAVINGS", "Yes");
    assertCafFieldEquals("APPLICANT_HOME_STREET_ADDRESS", homeStreetAddress);
    assertCafFieldEquals("APPLICANT_HOME_APT_NUMBER", homeApartmentNumber);
    assertCafFieldEquals("APPLICANT_HOME_CITY", homeCity);
    assertCafFieldEquals("APPLICANT_HOME_STATE", "MN");
    assertCafFieldEquals("APPLICANT_HOME_ZIPCODE", homeZip);
    assertCafFieldEquals("APPLICANT_MAILING_STREET_ADDRESS", "smarty street");
    assertCafFieldEquals("APPLICANT_MAILING_APT_NUMBER", "1b");
    assertCafFieldEquals("APPLICANT_MAILING_CITY", "Cooltown");
    assertCafFieldEquals("APPLICANT_MAILING_STATE", "CA");
    assertCafFieldEquals("APPLICANT_MAILING_ZIPCODE", "03104");
  }

  @Test
  void outOfStateApplicantFlow() {
    getToHomeAddress("Anoka", List.of(PROGRAM_SNAP));
    
    // Page title: Home Address
    assertTrue(testPage.getTitle().equals("Home Address"));
    
    // Input an out-of-state address
    testPage.enter("zipCode", "12345");
    testPage.enter("state",  "WI");
    testPage.enter("city", "someCity");
    testPage.enter("streetAddress", "someStreetAddress");
    testPage.enter("apartmentNumber", "someApartmentNumber");
    when(smartyStreetClient.validateAddress(any())).thenReturn(
            Optional.of(new Address("smarty street", "Cooltown", "WI", "03104", "1b", "someCounty"))
        );
    testPage.clickContinue("Out of State Address Notice");
    
    // Page title: Out of State Address Notice
    assertTrue(testPage.getTitle().equals("Out of State Address Notice"));
    // Verify that the given address is what was input on the homeAddress page
    assertTrue(testPage.findElementById("given-address-street").getText().equals("someStreetAddress"));
    assertTrue(testPage.findElementById("given-address-apt").getText().equals("someApartmentNumber"));
    assertTrue(testPage.findElementById("given-address-city-state").getText().equals("someCity, WI"));
    assertTrue(testPage.findElementById("given-address-zip").getText().equals("12345"));
    testPage.clickCustomButton("Yes, continue", 3, "Mailing address");

    // Page title: Mailing address
    assertTrue(testPage.getTitle().equals("Mailing address"));
    testPage.clickElementById("true");
    testPage.clickContinue("Address Validation");
    
    // Page title: Address Validation
    assertTrue(testPage.getTitle().equals("Address Validation"));
    testPage.clickElementById("enriched-address");
    testPage.clickContinue("Contact Info");
    
    // County Validation is skipped for out of state address
    // Page title: Contact Info
    assertTrue(testPage.getTitle().equals("Contact Info"));
    testPage.enter("phoneNumber", "(651) 555-1234");
    testPage.enter("email", "something@something.test");
    testPage.clickContinue("Review info");
    
    // Page title: Do you need help immediately?
    assertTrue(testPage.getTitle().equals("Review info"));
    
    // Now back up to the outOfStateAddressNotice page so that we can
    // verify the "Edit my address" route
    testPage.goBack(); // to the Contact Info page
    testPage.goBack(); // to the Address validation page
    testPage.goBack(); // to the Mailing address page
    testPage.goBack(); // to the Out of State Address Notice page
    testPage.clickCustomLink("Edit my address", "Home Address");
    
    // Page title: Home Address
    assertTrue(testPage.getTitle().equals("Home Address"));
    
    // Change the state to MN
    testPage.enter("state",  "MN");
    // mock a SmartyStreet "address found" response
    when(smartyStreetClient.validateAddress(any())).thenReturn(
        Optional.of(new Address("smarty street", "Cooltown", "MN", "03104", "1b", "someCounty"))
    );
    testPage.clickContinue("Mailing address");
    
    // Page title: Mailing address
    assertTrue(testPage.getTitle().equals("Mailing address"));
    // testPage.clickElementById("true"); Don't click this, it would uncheck the checkbox.
    testPage.clickContinue("Address Validation");
    
    // Page title: Address Validation
    assertTrue(testPage.getTitle().equals("Address Validation"));
    testPage.clickElementById("enriched-address");
    testPage.clickContinue("County Validation"); 

    // Page title: County Validation
    assertTrue(testPage.getTitle().equals("County Validation"));
    // The original county "Anoka" should be displayed on this page
    assertNotNull(testPage.findElementById("original-county"));
    // The enriched county "Hennepin" should be displayed on this page
    assertNotNull(testPage.findElementById("enriched-county"));
    testPage.clickContinue("Contact Info");
    
    // Page title: Contact Info
    assertTrue(testPage.getTitle().equals("Contact Info"));
    testPage.clickContinue("Review info");
    
    // Page title: Do you need help immediately?
    assertTrue(testPage.getTitle().equals("Review info"));
    
    // Now back up to the outOfStateAddressNotice page so that we can
    // verify the "No, quit application" route
    testPage.goBack(); // to the Contact Info page
    testPage.goBack(); // to the County validation page
    testPage.goBack(); // to the Address validation page
    testPage.goBack(); // to the Mailing address page
    testPage.goBack(); // to the Home address page
    
    testPage.enter("state",  "WI");
    // mock a SmartyStreet "address found" response
    when(smartyStreetClient.validateAddress(any())).thenReturn(
        Optional.of(new Address("smarty street", "Cooltown", "WI", "03104", "1b", "someCounty"))
    );
    testPage.clickContinue("Out of State Address Notice");
    
    // Page title: Out of State Address Notice
    assertTrue(testPage.getTitle().equals("Out of State Address Notice"));
    testPage.clickCustomButton("No, quit application", 3, "Quit confirmation");

    //Page title: Are you sure you want to quit
    assertTrue(testPage.getTitle().equals("Quit confirmation"));
    
    // Page Verify that clicking  'No, take me back' returns the user to the previous page
    testPage.clickButtonLink("Go back", "Out of State Address Notice");
    assertTrue(testPage.getTitle().equals("Out of State Address Notice"));
     
    //user returns to Out of State Address Notice page
    testPage.clickCustomButton("No, quit application", 3, "Quit confirmation");
    
    // Verify that clicking 'Yes,Quit application' ends the application process&routes user back to landing page
    testPage.clickButtonLink("Quit application", "MNbenefits");
   
    // Page title: Landing
    assertTrue(testPage.getTitle().equals("MNbenefits"));
  }

  /**
   * This test verifies the operations of the language preferences pages.
   */
  @Test
  void languagePreferencesSameLanguageFlow() {
	// Landing page
	  testPage.clickButtonLink("Apply now", "Identify County");

	// Select county
	testPage.enter("county", "Hennepin");
    testPage.clickContinue("Prepare To Apply");

    // Informational pages
    testPage.clickButtonLink("Continue","Timeout notice");
    testPage.clickButtonLink("Continue", "Language Preferences - Written");

	// Written Language Preference page
	String title = testPage.getTitle();
	assertThat(title).isEqualTo("Language Preferences - Written");
	
	// Verify that no written language preference is selected by default
	List<WebElement> writtenLanguageRadioInputs = driver.findElements(By.name("writtenLanguage[]"));
	String writtenLanguage = "";
	for (WebElement radioInput : writtenLanguageRadioInputs) {
		if (radioInput.isSelected()) {
			writtenLanguage = radioInput.getDomAttribute("value");
			break;
		};
	}
	assertThat(writtenLanguage).isEmpty();

	// Set written language preference to English
	testPage.enter("writtenLanguage", "English");
	writtenLanguageRadioInputs = driver.findElements(By.name("writtenLanguage[]"));
	writtenLanguage = "";
	for (WebElement radioInput : writtenLanguageRadioInputs) {
		if (radioInput.isSelected()) {
			writtenLanguage = radioInput.getDomAttribute("value");
			break;
		};
	}
	assertThat(writtenLanguage).isEqualTo("ENGLISH");
	
	// Continue to spoken language preference page
	testPage.clickContinue("Language Preferences - Spoken");
	
	title = testPage.getTitle();
	assertThat(title).isEqualTo("Language Preferences - Spoken");

	// Verify that the spokenSameAsWritten checkbox is not checked by default
	WebElement spokenSameAsWrittenCheckbox = driver.findElement(By.name("spokenSameAsWritten[]"));
	assertThat(spokenSameAsWrittenCheckbox.isSelected()).isFalse();

	// Verify that the spoken language radios are displayed
	WebElement spokenLanguageDiv = driver.findElement(By.name("spokenLanguage-div[]"));
	String spokenLanguageDivStyle = spokenLanguageDiv.getDomAttribute("style");
	// The absence of a style attribute would mean that the radio div is not hidden
	assertThat(spokenLanguageDivStyle).isNull();
	
	// Verify that no spoken language preference is selected by default
	List<WebElement> spokenLanguageRadioInputs = driver.findElements(By.name("spokenLanguage[]"));
	String spokenLanguage = "";
	for (WebElement radioInput : spokenLanguageRadioInputs) {
		if (radioInput.isSelected()) {
			spokenLanguage = radioInput.getDomAttribute("value");
			break;
		};
	}
	assertThat(spokenLanguage).isEmpty();
	
	// Click the spokenSameAsWritten checkbox
	testPage.clickElementById("true");
	
	// Verify that the checkbox is now checked.
	spokenSameAsWrittenCheckbox = driver.findElement(By.name("spokenSameAsWritten[]"));
	assertThat(spokenSameAsWrittenCheckbox.isSelected()).isTrue();

	// Verify that the spoken language radios are no longer displayed
	spokenLanguageDiv = driver.findElement(By.name("spokenLanguage-div[]"));
	spokenLanguageDivStyle = spokenLanguageDiv.getDomAttribute("style");
	assertThat(spokenLanguageDivStyle).contains("display: none");
	
	// Verify that the spoken language preference is now equal to the written language preference
	spokenLanguageRadioInputs = driver.findElements(By.name("spokenLanguage[]"));
	spokenLanguage = "";
	for (WebElement radioInput : spokenLanguageRadioInputs) {
		if (radioInput.isSelected()) {
			spokenLanguage = radioInput.getDomAttribute("value");
			break;
		};
	}
	assertThat(spokenLanguage).isEqualTo(writtenLanguage);
	
	testPage.enter("needInterpreter", "Yes");

	// Continue
	testPage.clickContinue("Choose Programs");
	title = testPage.getTitle();
	assertThat(title).isEqualTo("Choose Programs");
  }  
  

  private void assertCafContainsAllFieldsForMinimumSnapFlow(String applicationId,
      String countyInstructions) {
    // Page 1
    assertCafFieldEquals("APPLICATION_ID", applicationId);
    assertCafFieldEquals("COUNTY_INSTRUCTIONS", countyInstructions);
    assertCafFieldEquals("FULL_NAME", "Ahmed St. George");
    assertCafFieldEquals("CCAP_EXPEDITED_ELIGIBILITY", "");
    assertCafFieldEquals("APPLICANT_EMAIL", "some@example.com");
    assertCafFieldEquals("APPLICANT_PHONE_NUMBER", "(723) 456-7890");
    assertCafFieldEquals("EMAIL_OPTIN", "Yes");
    assertCafFieldEquals("PHONE_OPTIN", "Yes");
    assertCafFieldEquals("DATE_OF_BIRTH", "01/12/1928");
    assertCafFieldEquals("APPLICANT_SSN", "XXX-XX-XXXX");
    assertCafFieldEquals("PROGRAMS", "SNAP");

    // Page 5 and beyond
    assertCafFieldEquals("APPLICANT_LAST_NAME", "St. George");
    assertCafFieldEquals("APPLICANT_FIRST_NAME", "Ahmed");
    String otherName = "defaultOtherName";
    assertCafFieldEquals("APPLICANT_OTHER_NAME", otherName);
    String sex = "Female";
    assertCafFieldEquals("APPLICANT_SEX", sex.toUpperCase(ENGLISH));
    assertCafFieldEquals("MARITAL_STATUS", "NEVER_MARRIED");
    String needsInterpreter = "Yes";
    assertCafFieldEquals("NEED_INTERPRETER", needsInterpreter);
    assertCafFieldEquals("APPLICANT_SPOKEN_LANGUAGE_PREFERENCE", "ENGLISH");
    assertCafFieldEquals("APPLICANT_WRITTEN_LANGUAGE_PREFERENCE", "ENGLISH");
    String moveDate = "10/20/1993";
    assertCafFieldEquals("DATE_OF_MOVING_TO_MN", moveDate);
    String previousCity = "Chicago";
    assertCafFieldEquals("APPLICANT_PREVIOUS_STATE", previousCity);
    assertCafFieldEquals("FOOD", "Yes");
    assertCafFieldEquals("CASH", "Off");
    assertCafFieldEquals("EMERGENCY", "Off");
    assertCafFieldEquals("CCAP", "Off");
    //assertCafFieldEquals("GRH", "Off"); // TODO: Verify that GRH is no longer on the CAF
    assertCafFieldEquals("APPLICANT_SIGNATURE", signature);
  }

  private void testFeedbackScreen() {
    testPage.clickLink("Give us feedback", "Feedback");
    assertThat(testPage.getTitle()).isEqualTo("Feedback");
    assertThat(driver.findElement(By.id("happy"))).isNotNull();
    assertThat(driver.findElement(By.id("meh"))).isNotNull();
    assertThat(driver.findElement(By.id("sad"))).isNotNull();
    testPage.chooseSentiment(Sentiment.MEH);
    testPage.clickButton("Submit feedback", "Success");
  }
}
