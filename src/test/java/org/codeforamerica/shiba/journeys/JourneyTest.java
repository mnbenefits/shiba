package org.codeforamerica.shiba.journeys;

import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.codeforamerica.shiba.output.Document.CAF;
import static org.codeforamerica.shiba.output.Document.CCAP;
import static org.codeforamerica.shiba.output.Document.CERTAIN_POPS;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.YES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.codeforamerica.shiba.UploadDocumentConfiguration;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.documents.DocumentRepository;
import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.pages.config.FeatureFlagConfiguration;
import org.codeforamerica.shiba.pages.emails.MailGunEmailClient;
import org.codeforamerica.shiba.pages.enrichment.Address;
import org.codeforamerica.shiba.pages.enrichment.smartystreets.SmartyStreetClient;
import org.codeforamerica.shiba.pages.events.ApplicationSubmittedEvent;
import org.codeforamerica.shiba.pages.events.PageEvent;
import org.codeforamerica.shiba.pages.events.PageEventPublisher;
import org.codeforamerica.shiba.testutilities.AbstractBasePageTest;
import org.codeforamerica.shiba.testutilities.SuccessPage;
import org.codeforamerica.shiba.testutilities.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

abstract class JourneyTest extends AbstractBasePageTest {

  protected PDAcroForm caf;
  protected PDAcroForm ccap;
  protected PDAcroForm certainPops;
  protected String applicationId;

  @MockBean
  protected Clock clock;
  @MockBean
  protected SmartyStreetClient smartyStreetClient;
  @SpyBean
  protected DocumentRepository documentRepository;
  @MockBean
  private ClientRegistrationRepository springSecurityFilterChain;
  @MockBean
  protected PageEventPublisher pageEventPublisher;
  @MockBean
  protected MailGunEmailClient mailGunEmailClient;
  @MockBean
  protected FeatureFlagConfiguration featureFlagConfiguration;
  @SpyBean
  protected UploadDocumentConfiguration uploadDocumentConfiguration;

  @Override
  @BeforeEach
  protected void setUp() throws IOException {
    super.setUp();
    driver.navigate().to(baseUrl);
    when(clock.instant()).thenReturn(Instant.now());
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(smartyStreetClient.validateAddress(any())).thenReturn(Optional.empty());

    when(featureFlagConfiguration.get("submit-via-api")).thenReturn(FeatureFlag.OFF);
    when(featureFlagConfiguration.get("clamav")).thenReturn(FeatureFlag.ON);
    caf = null;
    ccap = null;
    certainPops = null;
  }

  @AfterEach
  void tearDown() {
    if (applicationId != null) {
      Arrays.stream(Objects.requireNonNull(path.toFile().listFiles()))
          .filter(file -> file.getName().contains(applicationId))
          .forEach(File::delete);
    }
  }

  protected void assertCafFieldEquals(String fieldName, String expectedVal) {
    TestUtils.assertPdfFieldEquals(fieldName, expectedVal, caf);
  }

  protected void assertCcapFieldEquals(String fieldName, String expectedVal) {
    TestUtils.assertPdfFieldEquals(fieldName, expectedVal, ccap);
  }
  
  protected void assertCertainPopsFieldEquals(String fieldName, String expectedVal) {
	    TestUtils.assertPdfFieldEquals(fieldName, expectedVal, certainPops);
  }

  protected String signApplicationAndDownloadApplicationZipFiles(String signature) {
    testPage.enter("applicantSignature", signature);
    testPage.clickButton("Submit");
    testPage.clickContinue();
    testPage.clickContinue();

    // No document upload
    testPage.clickButton("I'll do this later");
    testPage.clickButton("Finish application");

    // Next steps screen
    testPage.clickContinue();
    return downloadPdfs();
  }

  protected String downloadPdfs() {
    // Download CAF
    SuccessPage successPage = new SuccessPage(driver);
    successPage.downloadPdfZipFile();
    await().until(zipDownloadCompletes(successPage));
    unzipFiles();
     var pdfs = getAllFiles(); 
     caf = pdfs.getOrDefault(CAF, null); 
     ccap = pdfs.getOrDefault(CCAP,null);
     certainPops = pdfs.getOrDefault(CERTAIN_POPS,null);
     
    return getApplicationId();
  }

  private String getApplicationId() {
    // Retrieves the application id from the filename of a downloaded PDF
    return Arrays.stream(Objects.requireNonNull(path.toFile().listFiles()))
        .filter(file -> file.getName().endsWith(".pdf"))
        .sorted((f1,f2)-> Long.compare(f2.lastModified(), f1.lastModified()))
        .map(File::getName)
        .findFirst()
        .orElseThrow()
        .split("_")[4];
  }

  @NotNull
  protected Callable<Boolean> zipDownloadCompletes(SuccessPage successPage) {
       return () -> getZipFile().size() == successPage.countDownloadLinks();
  }
  
  protected void goBackToPage(String pageName) {
	  System.out.println(" ------- goBackToPage " + pageName + "-------");//TODO emj delete
	    while(!testPage.getTitle().equalsIgnoreCase(pageName)){
	    	System.out.println(testPage.getTitle());//TODO emj delete
	    	 testPage.goBack();
	    }
  }

  protected void goToPageBeforeSelectPrograms(String county) {
	  System.out.println("====== Starting, goToPageBeforeSelectPrograms");//TODO emj delete
	    // Landing page
	    testPage.clickButton("Apply now");

	    // Select county
	    testPage.enter("county", county);
	    testPage.clickContinue();

	    // Informational pages
	    testPage.clickContinue();
	    testPage.clickContinue();

	    // Language Preferences
	    testPage.enter("writtenLanguage", "English");
	    testPage.enter("spokenLanguage", "English");
	    testPage.enter("needInterpreter", "Yes");
	    testPage.clickContinue();
  }
  
  //TODO emj new method for CP
  protected void selectProgramsWithoutCertainPopsAndEnterPersonalInfo() {
	  List<String> programSelections = List
		        .of(PROGRAM_SNAP, PROGRAM_CCAP, PROGRAM_EA, PROGRAM_GRH);
	  System.out.println("====== selectProgramsWithoutCertainPopsAndEnterPersonalInfo");//TODO emj delete
	    // Program Selection
	    programSelections.forEach(program -> testPage.enter("programs", program));
	    
	    testPage.clickContinue();
	    // Getting to know you (Personal Info intro page)
	    testPage.clickContinue();
	    testPage.clickContinue();
	   //   takeSnapShot("test.png"); //TODO remove this after troubleshooting
	    // Personal Info
	    testPage.enter("firstName", "Ahmed");
	    testPage.enter("lastName", "St. George");
	    testPage.enter("otherName", "defaultOtherName");
	    //DOB is optional
	    testPage.enter("maritalStatus", "Never married");
	    testPage.enter("sex", "Female");
	    testPage.enter("livedInMnWholeLife", "Yes");
	    testPage.enter("moveToMnDate", "10/20/1993");
	    testPage.enter("moveToMnPreviousCity", "Chicago");
	    testPage.clickContinue();
	    assertThat(testPage.getTitle()).isEqualTo("Home Address");
	    testPage.goBack();
	    testPage.enter("dateOfBirth", "01/12/1928");
	    testPage.clickContinue();
  }
  
  protected void verifyHouseholdMemberCannotSelectCertainPops() {
	  System.out.println("====== verifyHouseholdMemberCannotSelectCertainPops");//TODO emj delete
	  System.out.println("===== page name = " + testPage.getTitle());
	    // Add 1 Household Member
	    assertThat(testPage.getElementText("page-form")).contains(
	        "Roommates that you buy and prepare food with");
	    testPage.enter("addHouseholdMembers", YES.getDisplayValue());
	    testPage.clickContinue();
	    boolean cpIsPresent = testPage.getElementText("page-form").contains("Healthcare for Seniors and People with Disabilities");
	    System.out.println("== cpIsPresent: " + cpIsPresent);
	    assertThat(!(testPage.getElementText("page-form")).contains("Healthcare for Seniors and People with Disabilities"));
//	    String householdMemberFirstName = "householdMemberFirstName";
//	    String householdMemberLastName = "householdMemberLastName";
//	    String householdMemberFullName = householdMemberFirstName + " " + householdMemberLastName;
//	    testPage.enter("firstName", householdMemberFirstName);
//	    testPage.enter("lastName", householdMemberLastName);
//	    testPage.enter("otherName", "houseHoldyMcMemberson");
//	    testPage.enter("dateOfBirth", "09/14/2018");
//	    testPage.enter("maritalStatus", "Never married");
//	    testPage.enter("sex", "Male");
//	    testPage.enter("livedInMnWholeLife", "Yes"); // actually means they MOVED HERE
//	    testPage.enter("moveToMnDate", "02/18/1950");
//	    testPage.enter("moveToMnPreviousState", "Illinois");
//	    testPage.enter("relationship", "My child");
//	    testPage.enter("programs", PROGRAM_CCAP);
	    // Assert that the programs follow up questions are shown when a program is selected
//	    WebElement programsFollowUp = testPage.findElementById("programs-follow-up");
//	    assertThat(programsFollowUp.getCssValue("display")).isEqualTo("block");
//	    // Assert that the programs follow up is hidden when none is selected
//	    testPage.enter("programs", PROGRAM_NONE);
//	    assertThat(programsFollowUp.getCssValue("display")).isEqualTo("none");
//	    testPage.enter("programs", PROGRAM_CCAP);
//	    // Assert that the programs follow up shows again when a program is selected after having selected none
//	    assertThat(programsFollowUp.getCssValue("display")).isEqualTo("block");
//	    testPage.enter("ssn", "987654321");
	  
  }
  
  //TODO delete this, not needed,
  protected void selectAllProgramsApplicantNotQualifiedForCertainPops() {
	    List<String> programSelections = List
	            .of(PROGRAM_SNAP, PROGRAM_CCAP, PROGRAM_EA, PROGRAM_GRH, PROGRAM_CERTAIN_POPS);
	    System.out.println("====== selectAllProgramsApplicantNotQualifiedForCertainPops");//TODO emj delete
	    
	    programSelections.forEach(program -> testPage.enter("programs", program));
	    testPage.clickContinue();
	    testPage.enter("basicCriteria", "None of the above");
	    testPage.clickContinue();
	  
  }
  
  protected void selectAllProgramsAndVerifyApplicantIsQualifiedForCertainPops(){
	    List<String> programSelectionsWithCP = List
				.of(PROGRAM_SNAP, PROGRAM_CCAP, PROGRAM_EA, PROGRAM_GRH, PROGRAM_CERTAIN_POPS);
	    System.out.println("====== selectAllProgramsAndVerifyApplicantIsQualifiedForCertainPops");//TODO emj delete
	    testPage.enter("programs", PROGRAM_NONE);//reset programs
	    // Program Selection
	    programSelectionsWithCP.forEach(program -> testPage.enter("programs", program));
	    testPage.clickContinue();

	   // if (programSelections.contains(PROGRAM_CERTAIN_POPS)) {
	      // Test Certain pops offboarding flow first by selecting None of the above
	      testPage.enter("basicCriteria", "None of the above");
	      testPage.clickContinue();
	      assertThat(testPage.getTitle()).isEqualTo("Certain Pops Offboarding");
	      testPage.clickContinue();
	      assertThat(testPage.getTitle()).isEqualTo("Add other programs");
	      testPage.goBack();
	      testPage.goBack();

	      // Basic Criteria:
	      testPage.enter("basicCriteria", "I am 65 years old or older");
	      testPage.enter("basicCriteria", "I am blind");
	      testPage.enter("basicCriteria", "I currently receive SSI or RSDI for a disability");
	      testPage.enter("basicCriteria",
	          "I have a disability that has been certified by the Social Security Administration (SSA)");
	      testPage.enter("basicCriteria",
	          "I have a disability that has been certified by the State Medical Review Team (SMRT)");
	      testPage.enter("basicCriteria",
	          "I want to apply for Medical Assistance for Employed Persons with Disabilities (MA-EPD)");
	      testPage.enter("basicCriteria", "I have Medicare and need help with my costs");
	      testPage.clickContinue();
	      assertThat(testPage.getTitle()).isEqualTo("Certain Pops Confirmation");
	      testPage.clickContinue();
	      System.out.println(" >>>1 page is " + testPage.getTitle());//TODO emj delete
	      assertThat(testPage.getTitle()).isEqualTo("Expedited Notice");
	      testPage.clickContinue();
   
	      System.out.println(" >>>2 page is " + testPage.getTitle());//TODO emj delete
	    // Getting to know you (Personal Info intro page)
	    testPage.clickContinue();
	    // Personal Info
	    testPage.enter("firstName", "Ahmed");
	    testPage.enter("lastName", "St. George");
	    testPage.enter("otherName", "defaultOtherName");
	    //DOB is optional
	    testPage.enter("maritalStatus", "Never married");
	    testPage.enter("sex", "Female");
	    testPage.enter("livedInMnWholeLife", "Yes");
	    testPage.enter("moveToMnDate", "10/20/1993");
	    testPage.enter("moveToMnPreviousCity", "Chicago");
	    testPage.clickContinue();
	    assertThat(testPage.getTitle()).isEqualTo("Home Address");
	    testPage.goBack();
	    testPage.enter("dateOfBirth", "01/12/1928");
	    testPage.clickContinue();
  
  }
  
  protected void verifySpouseCanSelectCertainPops() {
	  System.out.println("====== verifySpouseCanSelectCertainPops");//TODO emj delete
	  System.out.println(" >>>3 page is " + testPage.getTitle());//TODO emj delete
		testPage.enter("addHouseholdMembers", YES.getDisplayValue());
		testPage.clickContinue();
	    boolean cpIsPresent = testPage.getElementText("page-form").contains("Healthcare for Seniors and People with Disabilities");
	    System.out.println("== cpIsPresent: " + cpIsPresent);
	    assertThat(testPage.getElementText("page-form")).contains("Healthcare for Seniors and People with Disabilities");
		
	    testPage.enter("firstName", "Celia");
	    testPage.enter("lastName", "St. George");
	    testPage.enter("dateOfBirth", "10/15/1950");
	    testPage.enter("maritalStatus", "Married, living with spouse");
	    testPage.enter("sex", "Female");
	    testPage.enter("livedInMnWholeLife", "No");
	    testPage.enter("relationship", "My spouse (e.g. wife, husband)");
	    testPage.enter("programs", "None");
	    testPage.clickContinue();
  }
  
  //TODO emj moved test to here for CP
  protected void addHouseholdMemberToVerifySpouseCannotBeSelected() {
	  System.out.println("======= addHouseholdMemberToVerifySpouseCannotBeSelected");//TODO emj delete
	  //code was moved from FFJT

	    testPage.clickLink("Add a person");

	    String householdMemberFirstName = "householdMemberFirstName";
	    String householdMemberLastName = "householdMemberLastName";
	    String householdMemberFullName = householdMemberFirstName + " " + householdMemberLastName;
	    testPage.enter("firstName", householdMemberFirstName);
	    testPage.enter("lastName", householdMemberLastName);
	    testPage.enter("otherName", "houseHoldyMcMemberson");
	    testPage.enter("dateOfBirth", "09/14/2018");
	    testPage.enter("maritalStatus", "Never married");
	    testPage.enter("sex", "Male");
	    testPage.enter("livedInMnWholeLife", "Yes"); // actually means they MOVED HERE
	    testPage.enter("moveToMnDate", "02/18/1950");
	    testPage.enter("moveToMnPreviousState", "Illinois");
	    Select relationshipSelectWithRemovedSpouseOption = new Select(
		        driver.findElement(By.id("relationship")));
		    assertThat(relationshipSelectWithRemovedSpouseOption.getOptions().stream()
		        .noneMatch(option -> option.getText().equals("My spouse (e.g. wife, husband)"))).isTrue();
		//    testPage.goBack();
	    testPage.enter("relationship", "My child");
	    testPage.enter("programs", PROGRAM_CCAP);
	    // Assert that the programs follow up questions are shown when a program is selected
	    WebElement programsFollowUp = testPage.findElementById("programs-follow-up");
	    assertThat(programsFollowUp.getCssValue("display")).isEqualTo("block");
	    // Assert that the programs follow up is hidden when none is selected
	    testPage.enter("programs", PROGRAM_NONE);
	    assertThat(programsFollowUp.getCssValue("display")).isEqualTo("none");
	    testPage.enter("programs", PROGRAM_CCAP);
	    // Assert that the programs follow up shows again when a program is selected after having selected none
	    assertThat(programsFollowUp.getCssValue("display")).isEqualTo("block");
	    testPage.enter("ssn", "987654321");
	    testPage.clickContinue();
	    // Verify spouse option has been removed
	//    testPage.clickLink("Add a person");


	    // You are about to delete householdMember2 as a household member.
	    driver.findElement(By.id("iteration1-delete")).click();
	    testPage.clickButton("Yes, remove them");
	    // Check that My Spouse is now an option again after deleting the spouse
	    testPage.clickLink("Add a person");
	    Select relationshipSelectWithSpouseOption = new Select(
	        driver.findElement(By.id("relationship")));
	    assertThat(relationshipSelectWithSpouseOption.getOptions().stream()
	        .anyMatch(option -> option.getText().equals("My spouse (e.g. wife, husband)"))).isTrue();
	    testPage.goBack();
	    testPage.clickButton("Yes, that's everyone");
  }


  protected void getToHomeAddress(String county, List<String> programSelections) {
	  System.out.println("====== getToHomeAddress");//TODO emj delete
    // Landing page
    testPage.clickButton("Apply now");

    // Select county
    testPage.enter("county", county);
    testPage.clickContinue();

    // Informational pages
    testPage.clickContinue();
    testPage.clickContinue();

    // Language Preferences
    testPage.enter("writtenLanguage", "English");
    testPage.enter("spokenLanguage", "English");
    testPage.enter("needInterpreter", "Yes");
    testPage.clickContinue();

    // Program Selection
    programSelections.forEach(program -> testPage.enter("programs", program));
    testPage.clickContinue();

    if (programSelections.contains(PROGRAM_CERTAIN_POPS)) {
      // Test Certain pops offboarding flow first by selecting None of the above
      testPage.enter("basicCriteria", "None of the above");
      testPage.clickContinue();
      assertThat(testPage.getTitle()).isEqualTo("Certain Pops Offboarding");
      testPage.clickContinue();
      assertThat(testPage.getTitle()).isEqualTo("Add other programs");
      testPage.goBack();
      testPage.goBack();

      // Basic Criteria:
      testPage.enter("basicCriteria", "I am 65 years old or older");
      testPage.enter("basicCriteria", "I am blind");
      testPage.enter("basicCriteria", "I currently receive SSI or RSDI for a disability");
      testPage.enter("basicCriteria",
          "I have a disability that has been certified by the Social Security Administration (SSA)");
      testPage.enter("basicCriteria",
          "I have a disability that has been certified by the State Medical Review Team (SMRT)");
      testPage.enter("basicCriteria",
          "I want to apply for Medical Assistance for Employed Persons with Disabilities (MA-EPD)");
      testPage.enter("basicCriteria", "I have Medicare and need help with my costs");
      testPage.clickContinue();
      assertThat(testPage.getTitle()).isEqualTo("Certain Pops Confirmation");
      testPage.clickContinue();
    }
    if(programSelections.contains(PROGRAM_SNAP)) {
      assertThat(testPage.getTitle()).isEqualTo("Expedited Notice");
      testPage.clickContinue();
    }
    // Getting to know you (Personal Info intro page)
    testPage.clickContinue();

    // Personal Info
    testPage.enter("firstName", "Ahmed");
    testPage.enter("lastName", "St. George");
    testPage.enter("otherName", "defaultOtherName");
    //DOB is optional
    testPage.enter("ssn", "123456789");
    if (programSelections.contains(PROGRAM_CERTAIN_POPS)) {
      testPage.enter("noSSNCheck", "I don't have a social security number.");
      assertThat(testPage.getCheckboxValues("noSSNCheck")).contains("I don't have a social security number.", "I don't have a social security number.");
      testPage.enter("appliedForSSN", "Yes");
      testPage.clickContinue();
      //SSN textbox is filled and Checkbox is checked, so page won't advance and error shows 
      assertThat(testPage.getTitle()).contains("Personal Info");
      testPage.enter("noSSNCheck", "I don't have a social security number.");//deselect the SSN checkbox
    }
    testPage.enter("maritalStatus", "Never married");
    testPage.enter("sex", "Female");
    testPage.enter("livedInMnWholeLife", "Yes");
    testPage.enter("moveToMnDate", "10/20/1993");
    testPage.enter("moveToMnPreviousCity", "Chicago");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).isEqualTo("Home Address");
    testPage.goBack();
    testPage.enter("dateOfBirth", "01/12/1928");
    testPage.clickContinue();
  }
  
  protected void enterPersonalInfoForCertainPopsApplicant() {
	  System.out.println("===== enterPersonalInfoForCertainPopsApplicant");//TODO emj delete
	  System.out.println("===== page name = " + testPage.getTitle());
//	    if(programSelections.contains(PROGRAM_SNAP)) {
//	        assertThat(testPage.getTitle()).isEqualTo("Expedited Notice");
//	        testPage.clickContinue();
//	      }
	      // Getting to know you (Personal Info intro page)
//	      testPage.clickContinue();

	      // Personal Info
	      testPage.enter("firstName", "Ahmed");
	      testPage.enter("lastName", "St. George");
	      testPage.enter("otherName", "defaultOtherName");
	      //DOB is optional
	      testPage.enter("ssn", "123456789");
//	      if (programSelections.contains(PROGRAM_CERTAIN_POPS)) {
	        testPage.enter("noSSNCheck", "I don't have a social security number.");
	        assertThat(testPage.getCheckboxValues("noSSNCheck")).contains("I don't have a social security number.", "I don't have a social security number.");
	        testPage.enter("appliedForSSN", "Yes");
	        testPage.clickContinue();
	        //SSN textbox is filled and Checkbox is checked, so page won't advance and error shows 
	        assertThat(testPage.getTitle()).contains("Personal Info");
	        testPage.enter("noSSNCheck", "I don't have a social security number.");//deselect the SSN checkbox
//	      }
	      testPage.enter("maritalStatus", "Never married");
	      testPage.enter("sex", "Female");
	      testPage.enter("livedInMnWholeLife", "Yes");
	      testPage.enter("moveToMnDate", "10/20/1993");
	      testPage.enter("moveToMnPreviousCity", "Chicago");
	      testPage.clickContinue();
	      assertThat(testPage.getTitle()).isEqualTo("Home Address");
	      testPage.goBack();
	      testPage.enter("dateOfBirth", "01/12/1928");
	      testPage.clickContinue();
  }

  
  protected void enterPersonalInfo( List<String> programSelections) {
	  System.out.println("===== enterPersonalInfo");//TODO emj delete
	    if(programSelections.contains(PROGRAM_SNAP)) {
	        assertThat(testPage.getTitle()).isEqualTo("Expedited Notice");
	        testPage.clickContinue();
	      }
	      // Getting to know you (Personal Info intro page)
	      testPage.clickContinue();

	      // Personal Info
	      testPage.enter("firstName", "Ahmed");
	      testPage.enter("lastName", "St. George");
	      testPage.enter("otherName", "defaultOtherName");
	      //DOB is optional
	      testPage.enter("ssn", "123456789");
	      if (programSelections.contains(PROGRAM_CERTAIN_POPS)) {
	        testPage.enter("noSSNCheck", "I don't have a social security number.");
	        assertThat(testPage.getCheckboxValues("noSSNCheck")).contains("I don't have a social security number.", "I don't have a social security number.");
	        testPage.enter("appliedForSSN", "Yes");
	        testPage.clickContinue();
	        //SSN textbox is filled and Checkbox is checked, so page won't advance and error shows 
	        assertThat(testPage.getTitle()).contains("Personal Info");
	        testPage.enter("noSSNCheck", "I don't have a social security number.");//deselect the SSN checkbox
	      }
	      testPage.enter("maritalStatus", "Never married");
	      testPage.enter("sex", "Female");
	      testPage.enter("livedInMnWholeLife", "Yes");
	      testPage.enter("moveToMnDate", "10/20/1993");
	      testPage.enter("moveToMnPreviousCity", "Chicago");
	      testPage.clickContinue();
	      assertThat(testPage.getTitle()).isEqualTo("Home Address");
	      testPage.goBack();
	      testPage.enter("dateOfBirth", "01/12/1928");
	      testPage.clickContinue();
  }

  protected void fillOutHomeAndMailingAddress(String homeZip, String homeCity,
      String homeStreetAddress, String homeApartmentNumber) {
	  System.out.println("===== fillOutHomeAndMailingAddress"); //TODO emj delete
	  System.out.println("===== page name = " + testPage.getTitle());
    testPage.enter("zipCode", homeZip);
    testPage.enter("city", homeCity);
    testPage.enter("streetAddress", homeStreetAddress);
    testPage.enter("apartmentNumber", homeApartmentNumber);
    testPage.clickContinue();

    // Where can the county send your mail? (accept the smarty streets enriched address)
    testPage.enter("zipCode", "23456");
    testPage.enter("city", "someCity");
    testPage.enter("streetAddress", "someStreetAddress");
    testPage.enter("state", "IL");
    testPage.enter("apartmentNumber", "someApartmentNumber");
    when(smartyStreetClient.validateAddress(any())).thenReturn(
        Optional.of(new Address("smarty street", "Cooltown", "CA", "03104", "1b", "someCounty"))
    );
    testPage.clickContinue();
    testPage.clickElementById("enriched-address");
    testPage.clickContinue();
  }
  
  
  protected void fillOutContactAndReview(boolean isReview) {   
	  System.out.println("======== fillOutContactAndReview");//TODO emj delete
    // Check that we get the no phone number confirmation screen if no phone number is entered
    testPage.enter("email", "some@example.com");
    testPage.clickContinue();
    assertThat(testPage.getTitle()).contains("No phone number confirmation");
    testPage.goBack();

    // How can we get in touch with you?
    testPage.enter("phoneNumber", "7234567890");
    testPage.enter("email", "some@example.com");
    assertThat(testPage.getCheckboxValues("phoneOrEmail")).contains("It's okay to text me",
        "It's okay to email me");
    testPage.clickContinue();
    
    if (isReview)
    {
      // Let's review your info
      assertThat(driver.findElement(By.id("mailingAddress-address_street")).getText())
          .isEqualTo("smarty street");           
    }
    
  }
  
  /**
   * Call this only if phone and email have already been entered and tested before.
   */
  protected void goToContactAndReview() {   
	  System.out.println("======== goToContactAndReview");//TODO emj delete
    // How can we get in touch with you?
    testPage.enter("phoneNumber", "7234567890");
    testPage.enter("email", "some@example.com");
    testPage.clickContinue();
    testPage.clickLink("This looks correct");
    
  }

  protected void assertApplicationSubmittedEventWasPublished(String applicationId,
      FlowType flowType, int expectedNumberOfEvents) {
    ArgumentCaptor<PageEvent> captor = ArgumentCaptor.forClass(PageEvent.class);
    verify(pageEventPublisher, times(expectedNumberOfEvents)).publish(captor.capture());
    List<PageEvent> allValues = captor.getAllValues();
    ApplicationSubmittedEvent applicationSubmittedEvent = (ApplicationSubmittedEvent) allValues.stream()
        .filter(event -> event instanceof ApplicationSubmittedEvent).findFirst().get();
    assertThat(applicationSubmittedEvent.getFlow()).isEqualTo(flowType);
    assertThat(applicationSubmittedEvent.getApplicationId()).isEqualTo(applicationId);
    assertThat(applicationSubmittedEvent.getLocale()).isEqualTo(ENGLISH);
  }

  protected void deleteAFile() {
    await().until(
		  () -> driver.findElements(By.className("dz-remove")).get(0).getAttribute("innerHTML")
		  .contains("delete"));
    testPage.clickLink("delete");

    assertThat(testPage.getTitle()).isEqualTo("Delete a file");
    testPage.clickButton("Yes, delete the file");
  }

  protected void waitForErrorMessage() {
    WebElement errorMessage = driver.findElement(By.className("text--error"));
    await().until(() -> !errorMessage.getText().isEmpty());
  }
}
