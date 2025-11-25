package org.codeforamerica.shiba.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.NO;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.YES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.documents.DocumentRepository;
import org.codeforamerica.shiba.testutilities.AccessibilityTestPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;

@Tag("a11y")
public class AccessibilityJourneyTest extends JourneyTest {

  protected static List<Rule> resultsList = new ArrayList<>();
  protected static Results results;

  @MockitoBean
  protected DocumentRepository documentRepository;

  @AfterAll
  static void tearDownAll() {
    generateAccessibilityReport(results);
  }

  private static void generateAccessibilityReport(Results results) {
    results.setViolations(resultsList);
    List<Rule> violations = results.getViolations();
    System.out.println("Found " + violations.size() + " accessibility related issues.");
    if (results.getViolations().size() > 0) {
      violations.forEach(violation -> {
        System.out.println("Rule at issue: " + violation.getId());
        System.out.println("Rule description: " + violation.getDescription());
        System.out.println("Rule help text: " + violation.getHelp());
        System.out.println("Rule help page: " + violation.getHelpUrl());
        System.out.println("Accessibility impact: " + violation.getImpact());
        System.out.println("Page at issue: " + violation.getUrl());
        System.out.println("HTML with issue: " + violation.getNodes().get(0).getHtml());
      });
    }
    assertThat(violations.size()).isEqualTo(0);
  }

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    super.setUp();
  }

  protected void initTestPage() {
    testPage = new AccessibilityTestPage(driver);
  }

  @AfterEach
  void afterEach() {
    AxeBuilder builder = new AxeBuilder();
    results = builder.analyze(driver);
    Map<String, List<Rule>> resultMap = ((AccessibilityTestPage) testPage).getResultMap();
    resultMap.values().forEach(resultsList::addAll);
  }

  @Test
  void laterDocsFlow() {
    testPage.clickButtonLink("Upload documents", "Ready to upload documents");
    // Ready to upload documents page
    testPage.clickButtonLink("Continue", "Match Info");
    
    // Match Info page
    // Enter incorrect information to get validation errors to check against aria-properties
    assertThat(testPage.inputIsValid("firstName")).isTrue();
    assertThat(driver.findElement(By.id("dateOfBirth-day")).getAttribute("aria-invalid")).isEqualTo(
        "false");
    assertThat(testPage.inputIsValid("ssn")).isTrue();
    testPage.enter("firstName", "");
    testPage.enter("lastName", "defaultLastName");
    testPage.enter("dateOfBirth", "01/40/1999");
    testPage.enter("ssn", "12345");
    testPage.enter("caseNumber", "1234567");
    testPage.clickContinue("Match Info");
    assertThat(testPage.hasInputError("firstName")).isTrue();
    assertThat(testPage.hasInputError("ssn")).isTrue();
    assertThat(driver.findElements(By.className("firstName")).size()).isEqualTo(0);
    assertThat(driver.findElement(By.id("dateOfBirth-day")).getAttribute("aria-invalid")).isEqualTo(
        "true");
    assertThat(driver.findElements(By.className("ssn")).size()).isEqualTo(0);
    assertThat(testPage.getInputAriaLabelledBy("firstName")).isEqualTo(
        "firstName-error-p firstName-label");
    assertThat(testPage.getInputAriaDescribedBy("firstName")).isEqualTo(
        "firstName-error-message-1 firstName-help-message");
    assertThat(driver.findElement(By.id("dateOfBirth-day")).getAttribute("aria-labelledby")).isEqualTo(
        "dateOfBirth-error-p dateOfBirth-legend dateOfBirth-day-label");
    assertThat(
        driver.findElement(By.id("dateOfBirth-day")).getAttribute("aria-describedby")).isEqualTo(
        "dateOfBirth-error-message-1");

    assertThat(testPage.getInputAriaLabelledBy("ssn")).isEqualTo("ssn-error-p ssn-label");
    assertThat(testPage.getInputAriaDescribedBy("ssn")).isEqualTo(
        "ssn-error-message-1 ssn-help-message");

    testPage.enter("firstName", "defaultFirstName");
    testPage.enter("lastName", "defaultLastName");
    testPage.enter("dateOfBirth", "01/12/1928");
    testPage.enter("ssn", "123456789");
    testPage.enter("caseNumber", "1234567");
    testPage.enter("phoneNumber", "7041234567");
	testPage.clickContinue( "Identify County" ); 
    
    // Identify County page
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.clickContinue("Identify County");
    assertThat(testPage.selectHasInputError("county")).isTrue();
    assertThat(testPage.getSelectAriaLabel("county")).isEqualTo("Error county");
    assertThat(testPage.getSelectAriaDescribedBy("county")).isEqualTo("county-error-message-1");
    testPage.enter("county", "Dakota");
    testPage.clickContinue("Tribal Nation member");

    // Tribal Nation Member page
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.clickCustomButton("Yes", 10,  "Select a Tribal Nation");
    
    // Select Tribal Nation
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.clickContinue("Select a Tribal Nation");
    assertThat(testPage.selectHasInputError("selectedTribe")).isTrue();
    assertThat(testPage.getSelectAriaLabel("selectedTribe")).isEqualTo("Error selectedTribe");
    assertThat(testPage.getSelectAriaDescribedBy("selectedTribe")).isEqualTo("selectedTribe-error-message-1");
    
    // go back to isTribalNationMember
    testPage.goBack(); // this Go Back just takes you the selectTribalNation page that appeared before there was an error
    testPage.goBack();
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    
    // go back to identifyCounty
    testPage.goBack(); 
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue("Tribal Nation member");
    
    // isTribalNationMember
    testPage.clickCustomButton("No", 10, "How to add documents");
    
    // howToAddDocuments page
    testPage.clickButtonLink("Continue", "Upload documents");
    
    // should allow me to upload documents and those documents should be sent to the ESB
    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickCustomButton("Yes, submit and finish", 10, "Documents Sent");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
  }
  
  @Test
  void healthcareRenewalFlow() {
    navigateTo("healthcareRenewalUpload");
    assertThat(driver.getTitle()).isEqualTo("Health Care Renewal Document Upload");

    // should allow me to enter personal info and continue the flow if my county is supported
    testPage.enter("county", "Select your county");
    testPage.enter("tribalNation", "Select a Tribal Nation");
    testPage.clickContinue("Health Care Renewal Document Upload");
    assertThat(driver.getTitle()).isEqualTo("Health Care Renewal Document Upload");
    testPage.enter("county", "Hennepin");
    testPage.enter("tribalNation", "White Earth Nation");
    testPage.clickContinue("Health Care Renewal Document Upload");
    assertThat(driver.getTitle()).isEqualTo("Health Care Renewal Document Upload");
    testPage.enter("county", "Hennepin");
    testPage.enter("tribalNation", "Select a Tribal Nation");
    testPage.clickContinue("Match Info");

    assertThat(driver.getTitle()).isEqualTo("Match Info");
    testPage.enter("firstName", "defaultFirstName");
    testPage.enter("lastName", "defaultLastName");
    testPage.enter("ssn", "123456789");
    testPage.enter("caseNumber", "123456789");//9 digits will cause error
    testPage.enter("phoneNumber", "7041234567");
    assertThat(testPage.getHeader()).isEqualTo("Before you start, we need to match your documents to your health care case");
    testPage.clickContinue("Match Info");
    assertThat(driver.getTitle()).isEqualTo("Match Info");//stays on match info page
    testPage.enter("caseNumber", "123");//too short
    testPage.clickContinue("Match Info");
    assertThat(driver.getTitle()).isEqualTo("Match Info");//stays on match info page
    testPage.enter("caseNumber", "12345678");
    testPage.clickContinue("How to add documents");
    testPage.clickButtonLink("Continue", "Upload documents");
    

    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickButtonLink("No, add more documents", "Upload documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    testPage.clickCustomButton("Yes, submit and finish",10,  "Documents Sent");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    verify(pageEventPublisher).publish(any());

    // Assert that applicant can't resubmit docs at this point
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    // repeat renewal flow to verify another session has been created
    navigateTo("healthcareRenewalUpload");
    assertThat(driver.getTitle()).isEqualTo("Health Care Renewal Document Upload");
    
    WebElement selectedOption = testPage.getSelectedOption("county");
    assertThat(selectedOption.getText()).isEqualTo("Select your county");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue("Match Info");

    assertThat(driver.getTitle()).isEqualTo("Match Info");
    testPage.enter("firstName", "defaultFirstName");
    testPage.enter("lastName", "defaultLastName");
    testPage.enter("ssn", "123456789");
    testPage.enter("phoneNumber", "7041234567");
    assertThat(testPage.getHeader()).isEqualTo("Before you start, we need to match your documents to your health care case");
    testPage.enter("caseNumber", "12345678");
    testPage.clickContinue("How to add documents");
    testPage.clickButtonLink("Continue", "Upload documents");
    navigateTo("healthcareRenewalUpload");
    WebElement selectedOption2 = testPage.getSelectedOption("county");
    assertThat(selectedOption2.getText()).isEqualTo("Hennepin");
    testPage.clickContinue("Match Info");
    testPage.clickContinue("How to add documents");
    testPage.clickButtonLink("Continue", "Upload documents");
    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickCustomButton("Yes, submit and finish", 10, "Documents Sent");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    // Assert that applicant can't resubmit docs at this point
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
  }

  @Test
  void userCanCompleteTheNonExpeditedHouseholdFlow() {
    List<String> programSelections = List.of(PROGRAM_SNAP, PROGRAM_CCAP);

    testPage.clickButtonLink("Apply now", "Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickButtonLink("Continue", "Prepare To Apply");
    testPage.clickButtonLink("Continue", "Timeout notice");
    testPage.clickButtonLink("Continue", "Language Preferences - Written");
    testPage.enter("writtenLanguage", "English");
    testPage.clickContinue("Language Preferences - Spoken");
    testPage.enter("spokenSameAsWritten", "Same as the language I read or write");
    testPage.enter("needInterpreter", "Yes");
    testPage.clickContinue("Choose Programs");
    programSelections.forEach(program -> testPage.enter("programs", program));
    testPage.clickContinue("Expedited Notice");
    testPage.clickButtonLink("Continue", "Intro: Basic Info");//for Expedited Notice page
    testPage.clickButtonLink("Continue", "Personal Info");
    testPage.enter("firstName", "defaultFirstName");
    testPage.enter("lastName", "defaultLastName");
    testPage.enter("otherName", "defaultOtherName");
    testPage.enter("dateOfBirth", "01/12/1928");
    testPage.enter("ssn", "123456789");
    testPage.enter("maritalStatus", "Never married");
    testPage.enter("sex", "Female");
    testPage.enter("livedInMnWholeLife", "Yes");
    testPage.enter("moveToMnDate", "02/18/1776");
    testPage.enter("moveToMnPreviousCity", "Chicago");
    testPage.clickContinue("Home Address");
    fillOutHomeAndMailingAddress("03104", "Cooltown", "smarty street", "1b", "MN"); 
    testPage.enter("phoneNumber", "134567890");
    testPage.clickContinue("Contact Info");
    assertThat(testPage.hasInputError("phoneNumber")).isTrue();
    assertThat(testPage.getInputAriaLabelledBy("phoneNumber")).isEqualTo(
        "phoneNumber-error-p phoneNumber-label");
    assertThat(testPage.getInputAriaDescribedBy("phoneNumber")).isEqualTo(
        "phoneNumber-error-message-2 phoneNumber-error-message-1 phoneNumber-help-message");
    testPage.enter("phoneNumber", "7234567890");
    testPage.enter("email", "some@example.com");
    testPage.enter("phoneOrEmail", "It's okay to text me");
    testPage.clickContinue("Review info");   
    
    testPage.clickButtonLink("This looks correct", "Do you want to add household members?");
    
    testPage.chooseYesOrNo("addHouseholdMembers", YES.getDisplayValue(), "Start Household");
    testPage.clickButtonLink("Continue", "Housemate: Personal Info");
    
    testPage.enter("relationship", "Other");
    testPage.enter("programs", PROGRAM_CCAP);
    testPage.enter("firstName", "householdMemberFirstName");
    testPage.enter("lastName", "householdMemberLastName");
    testPage.enter("otherName", "houseHoldyMcMemberson");
    testPage.enter("dateOfBirth", "09/14/1950");
    testPage.enter("ssn", "987654321");
    testPage.enter("maritalStatus", "Never married");
    testPage.enter("sex", "Male");
    testPage.enter("livedInMnWholeLife", "Yes");
    testPage.enter("moveToMnDate", "02/18/1950");
    testPage.enter("moveToMnPreviousState", "Illinois");
    testPage.clickContinue("Household members");
    
    testPage.clickButtonLink("Yes, that's everyone", "Who are the children in need of care?");
    testPage.enter("whoNeedsChildCare", "householdMemberFirstName householdMemberLastName");

    testPage.clickContinue("Do you have a child care provider?");
    testPage.chooseYesOrNo( "hasChildCareProvider", NO.getDisplayValue(), "Who are the children that have a parent not living in the home?");
    
    testPage.enter("whoHasAParentNotLivingAtHome",
        "None of the children have parents living outside the home");
    testPage.clickContinue("Mental health needs & child care");
	testPage.chooseYesOrNo("childCareMentalHealth", YES.getDisplayValue(), "Time needed each week");
	testPage.enter("childCareMentalHealthHours", "20");
    testPage.clickContinue("Preparing meals together");
    testPage.chooseYesOrNo("isPreparingMealsTogether", YES.getDisplayValue(), "Housing subsidy");
    testPage.chooseYesOrNo("hasHousingSubsidy", NO.getDisplayValue(), "Living situation");
    testPage.enter("livingSituation", "None of these");
    testPage.clickContinue("Going to school");
    testPage.chooseYesOrNo("goingToSchool", NO.getDisplayValue(), "Pregnant");
    testPage.chooseYesOrNo("isPregnant", YES.getDisplayValue(), "Household: pregnant");
    testPage.enter("whoIsPregnant", "Me");
    testPage.clickContinue("Expedited Migrant Farm Worker, Household");
    testPage.chooseYesOrNo("migrantOrSeasonalFarmWorker", NO.getDisplayValue(), "Citizenship");
    testPage.clickElementById("citizenshipStatus[]-0-NOT_CITIZEN");
    testPage.clickElementById("citizenshipStatus[]-1-BIRTH_RIGHT");
    testPage.clickContinue("Disability");
    testPage.chooseYesOrNo("hasDisability", NO.getDisplayValue(), "Not able to work");
    testPage.chooseYesOrNo("unableToWork", NO.getDisplayValue(), "Work changes");
    testPage.enter("workChanges", "None of the above");
    testPage.clickContinue("Tribal Nation member");
    testPage.chooseYesOrNo("isTribalNationMember", YES.getDisplayValue(), "Select a Tribal Nation");
    testPage.selectFromDropdown("selectedTribe[]", "Red Lake Nation");
    testPage.clickContinue("Nations Boundary");
    testPage.chooseYesOrNo("livingInNationBoundary", NO.getDisplayValue(),"Intro: Income");
    testPage.clickButtonLink("Continue", "Employment status");
    testPage.chooseYesOrNo("areYouWorking", YES.getDisplayValue(), "Income by job");
    testPage.clickButtonLink("Add a job", "Household selection for income");
    testPage.enter("whoseJobIsIt", "householdMemberFirstName householdMemberLastName");
    testPage.clickContinue("Employer's Name");

    // Leave blank to trigger error and check aria properties
    assertThat(testPage.inputIsValid("employersName")).isTrue();
    assertThat(testPage.getInputAriaLabelledBy("employersName")).isEqualTo("employersName-label");
    testPage.clickContinue("Employer's Name");
    assertThat(driver.findElements(By.className("employersName")).size()).isEqualTo(0);
    assertThat(testPage.getInputAriaLabelledBy("employersName")).isEqualTo(
        "employersName-error-p employersName-label");
    assertThat(testPage.getInputAriaDescribedBy("employersName")).isEqualTo(
        "employersName-error-message-1");

    testPage.enter("employersName", "some employer");
    testPage.clickContinue("Self-employment");
    testPage.chooseYesOrNo("selfEmployment", YES.getDisplayValue(), "Paid by the hour");
    testPage.chooseYesOrNo("paidByTheHour", YES.getDisplayValue(), "Hourly wage");

    // Check aria-label is correct then enter incorrect value to throw error and check all aria properties have updated
    testPage.enter("hourlyWage", "-10");
    testPage.clickContinue("Hourly wage");
    assertThat(driver.findElements(By.className("hourlyWage")).size()).isEqualTo(0);
    assertThat(testPage.getInputAriaLabel("hourlyWage")).isEqualTo("Error hourlyWage");
    assertThat(testPage.getInputAriaDescribedBy("hourlyWage")).isEqualTo(
        "hourlyWage-error-message-1");

    testPage.enter("hourlyWage", "1");
    testPage.clickContinue("Hours a week");
    // Enter an incorrect value to trigger an error and check aria properties
    testPage.enter("hoursAWeek", "-30");
    testPage.clickContinue("Hours a week");
    assertThat(driver.findElements(By.className("hoursAWeek")).size()).isEqualTo(0);
    assertThat(testPage.getInputAriaLabel("hoursAWeek")).isEqualTo("Error hoursAWeek");
    assertThat(testPage.getInputAriaDescribedBy("hoursAWeek")).isEqualTo(
        "hoursAWeek-error-message-1");

    testPage.enter("hoursAWeek", "30");
    testPage.clickContinue("Job Builder");
    testPage.clickButtonLink("No, that's it.", "Job Search");
    // drill down to futureIncome page
    testPage.chooseYesOrNo("currentlyLookingForJob", NO.getDisplayValue(), "Income Up Next");
    testPage.clickButtonLink("Continue", "Unearned Income");
    testPage.enter("unearnedIncome", "None of the above");
    testPage.clickButton("Continue", "Unearned Income");
    testPage.enter("otherUnearnedIncome", "None of the above");
    testPage.clickButton("Continue", "Future Income");
    assertThat(testPage.getInputAriaLabelledBy("div", "earnLessMoneyThisMonth-div")).isEqualTo("page-header page-header-help-message");
    // now back up to jobBuilder page
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    
    testPage.clickButtonLink("No, that's it.", "Job Search");
    testPage.chooseYesOrNo("currentlyLookingForJob", NO.getDisplayValue(), "Income Up Next");
    testPage.clickButtonLink("Continue", "Unearned Income");
    testPage.enter("unearnedIncome", "Social Security");

    testPage.clickContinue("Unearned Income Source");
    testPage.clickElementById("householdMember-me");
    
    // Enter incorrect social security amount to trigger error and check aria properties
    testPage.enter("socialSecurityAmount", "-200");
    testPage.clickContinue("Unearned Income Source");
    testPage.hasInputError("socialSecurityAmount");

    assertThat(driver.findElements(By.className("socialSecurityAmount")).size()).isEqualTo(0);
    assertThat(testPage.getInputAriaDescribedBy("socialSecurityAmount")).isEqualTo(
        "socialSecurityAmount-help-message");
    assertThat(testPage.getInputAriaLabelledBy("socialSecurityAmount")).isEqualTo(
        "socialSecurityAmount-label");

    testPage.enter("socialSecurityAmount", "200");
	testPage.clickContinue("Unearned Income");
	
    testPage.enter("otherUnearnedIncome", "None of the above");
    testPage.clickContinue("Unearned Income");
    testPage.enter("otherUnearnedIncome", "None of the above");
    testPage.clickContinue("Future Income");

    testPage.enter("earnLessMoneyThisMonth", "Yes");
    testPage.clickButtonLink("Continue", "Start Expenses");
    testPage.clickButtonLink("Continue", "Home Expenses");
    testPage.enter("homeExpenses", "Rent");
    testPage.clickContinue("Home expenses amount");
    testPage.enter("homeExpensesAmount", "123321");
    testPage.clickContinue("Expedited Utility Payments, Household");
    testPage.enter("payForUtilities", "Heating");
    testPage.clickContinue("Energy Assistance");
    testPage.chooseYesOrNo("energyAssistance", YES.getDisplayValue(), "Energy Assistance More Than 20");
    testPage.chooseYesOrNo("energyAssistanceMoreThan20", YES.getDisplayValue(), "Medical expenses");
    testPage.enter("medicalExpenses", "None of the above");
    testPage.clickContinue("Support and Care");
    testPage.chooseYesOrNo("supportAndCare", YES.getDisplayValue(), "Assets");
    testPage.enter("assets", "A vehicle");
    testPage.enter("assets", "Real estate (not including your own home)");
    testPage.clickContinue("Sold assets");
    testPage.chooseYesOrNo("haveSoldAssets", NO.getDisplayValue(), "Submitting Application");
    testPage.clickButtonLink("Continue", "Register to vote");

    testPage.clickCustomButton("Yes, send me more info", 10, "Healthcare Coverage");
    testPage.enter("healthcareCoverage", YES.getDisplayValue());
    testPage.clickContinue("Authorized Rep");
    testPage.chooseYesOrNo("helpWithBenefits", YES.getDisplayValue(), "Authorized Rep Communicate");
    testPage.chooseYesOrNo("communicateOnYourBehalf", YES.getDisplayValue(), "Authorized Rep mail and notices");
    testPage.chooseYesOrNo("getMailNotices", YES.getDisplayValue(), "Authorized Rep spend on your behalf");
    testPage.chooseYesOrNo("authorizedRepSpendOnYourBehalf", YES.getDisplayValue(), "Authorized Rep contact info");
    testPage.enter("authorizedRepFullName", "defaultFirstName defaultLastName");
    testPage.enter("authorizedRepStreetAddress", "someStreetAddress");
    testPage.enter("authorizedRepCity", "someCity");
    testPage.enter("authorizedRepZipCode", "12345");
    testPage.enter("authorizedRepPhoneNumber", "7234567890");
    testPage.clickContinue("Additional Info");
    driver.findElement(By.id("additionalInfo"))
        .sendKeys("Some additional information about my application");
    testPage.clickContinue("Can we ask");
    testPage.clickLink("Yes, continue", "Race and Ethnicity");
    testPage.enter("raceAndEthnicity", List.of("Asian", "White"));
    testPage.clickContinue("Legal Stuff");
    testPage.enter("agreeToTerms", "I agree");
    testPage.enter("drugFelony", NO.getDisplayValue());
    testPage.clickContinue("Sign this application");
    testPage.enter("applicantSignature", "some name");
    testPage.clickButton("Continue", "Submit application");
    testPage.clickButton("Submit", "Submission Confirmation");
    
  }
}
