package org.codeforamerica.shiba.journeys;

import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.codeforamerica.shiba.output.Document.CAF;
import static org.codeforamerica.shiba.output.Document.CCAP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.documents.DocumentRepository;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

abstract class JourneyTest extends AbstractBasePageTest {

  protected PDAcroForm caf;
  protected PDAcroForm ccap;
  protected String applicationId;

  @MockitoBean
  protected Clock clock;
  @MockitoBean
  protected SmartyStreetClient smartyStreetClient;
  @MockitoSpyBean
  protected DocumentRepository documentRepository;
  @MockitoBean
  private ClientRegistrationRepository springSecurityFilterChain;
  @MockitoBean
  protected PageEventPublisher pageEventPublisher;
  @MockitoBean
  protected MailGunEmailClient mailGunEmailClient;
  @MockitoBean
  protected FeatureFlagConfiguration featureFlagConfiguration;

  @Override
  @BeforeEach
  protected void setUp() throws IOException {
    super.setUp();
    driver.navigate().to(baseUrl);
    when(clock.instant()).thenReturn(Instant.now());
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(smartyStreetClient.validateAddress(any())).thenReturn(Optional.empty());
    caf = null;
    ccap = null;
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
  
  protected String signApplicationAndDownloadApplicationZipFiles(String signature,List<String> expectedMessages) {
    testPage.enter("applicantSignature", signature);
    testPage.clickContinue("Submit application");
    testPage.clickCustomButton("Submit application", 3, "Submission Confirmation");
    testPage.clickButtonLink("Continue", "Adding Documents");
    testPage.clickButtonLink("Continue", "Document Recommendation");

    // No document upload
    testPage.clickButtonLink("I'll do this later", "Document offboarding");
    testPage.clickButtonLink("Finish application", "Additional Program Documents");
    testPage.clickButtonLink("Continue", "Your next steps"); // on programDocuments page

    // Next steps screen
    List<WebElement> pageElements = driver.findElements(By.id("original-next-steps"));
    testPage.clickElementById("button-a2");
    testPage.clickElementById("button-a3");
    testPage.clickElementById("button-a4");
    if (pageElements.isEmpty()) {
    	List<String> nextStepSections = driver.findElements(By.className("next-step-section")).stream().map(WebElement::getText).collect(Collectors.toList());
    	assertThat(nextStepSections).containsExactly(expectedMessages.toArray(new String[0]));
    }
    
    testPage.clickButtonLink("Continue", "Success");
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
	    while(!testPage.getTitle().equalsIgnoreCase(pageName)){
	    	 testPage.goBack();
	    }
  }

  protected void goToPageBeforeSelectPrograms(String county) {
	    // Landing page
	    testPage.clickButtonLink("Apply now", "Identify County");

	    // Select county
	    testPage.enter("county", county);
	    testPage.clickContinue("Prepare To Apply");

	    // Informational pages
	    testPage.clickButtonLink("Continue","Timeout notice");
	    testPage.clickButtonLink("Continue", "Language Preferences - Written");

	    // Written Language Preferences
	    testPage.enter("writtenLanguage", "English");
	    testPage.clickContinue("Language Preferences - Spoken");
	    // Spoken Language Preferences
	    testPage.enter("spokenSameAsWritten", "Same as the language I read or write");
	    testPage.enter("needInterpreter", "Yes");
	    testPage.clickContinue("Choose Programs");
  }
  
  protected void getToHomeAddress(String county, List<String> programSelections) {
    // Landing page
    testPage.clickButtonLink("Apply now", "Identify County");

    // Select county
    testPage.enter("county", county);
    testPage.clickContinue("Prepare To Apply");

    // Informational pages
    testPage.clickButtonLink("Continue","Timeout notice");
    testPage.clickButtonLink("Continue", "Language Preferences - Written");

    // Written Language Preferences
    testPage.enter("writtenLanguage", "I read or write in some other language");
    testPage.enter("otherWrittenLanguage", "German");
    testPage.clickContinue("Language Preferences - Spoken");
    
    // Spoken Language Preferences
    testPage.enter("spokenLanguage", "I speak in some other language");
    testPage.enter("otherSpokenLanguage", "German");
    testPage.enter("needInterpreter", "Yes");
    testPage.clickContinue("Choose Programs");

    // Program Selection
    programSelections.forEach(program -> testPage.enter("programs", program));

    if(programSelections.contains(PROGRAM_SNAP)) {
      testPage.clickContinue("Expedited Notice");
      assertThat(testPage.getTitle()).isEqualTo("Expedited Notice");
      testPage.clickButtonLink("Continue", "Intro: Basic Info");
      testPage.clickButtonLink("Continue", "Personal Info");
    }
    
    if (programSelections.contains(PROGRAM_CCAP)) {
    	testPage.clickButtonLink("Continue", "Intro: Basic Info");
    	testPage.clickButtonLink("Continue", "Personal Info");
    }

    // Personal Info
    testPage.enter("firstName", "Ahmed");
    testPage.enter("lastName", "St. George");
    testPage.enter("otherName", "defaultOtherName");
    //DOB is optional
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
  
  /**
   * 
   * @param homeZip
   * @param homeCity
   * @param homeStreetAddress
   * @param homeApartmentNumber
   * @param homeState
   */
  protected void fillOutHomeAndMailingAddress(String homeZip, String homeCity,
      String homeStreetAddress, String homeApartmentNumber, String homeState) {
    testPage.enter("zipCode", homeZip);
    testPage.enter("city", homeCity);
    testPage.enter("streetAddress", homeStreetAddress);
    testPage.enter("state", homeState);
    testPage.enter("apartmentNumber", homeApartmentNumber);
    testPage.clickButtonWithRetry("Continue", 20, "Mailing address");
    testPage.enter("zipCode", "03104");
    testPage.enter("city", "Cooltown");
    testPage.enter("streetAddress", "smarty street");
    testPage.enter("state", "MN");
    testPage.enter("apartmentNumber", "1b");
    when(smartyStreetClient.validateAddress(any())).thenReturn(
            Optional.of(new Address("smarty street", "Cooltown", "CA", "03104", "1b", "someCounty"))
        );

    testPage.clickContinue("Address Validation");
    testPage.clickButton("Continue", "County Validation");
    testPage.clickButtonLink("Use this county", "Contact Info");
  }
  
  /**
   * Enter out of state address and confirm it can be changed with "Edit my address" link.
   */
  protected void enterOutOfStateHomeAndMailingAddress(){
	    testPage.enter("zipCode", "88888");
	    testPage.enter("city", "OutOfState City");
	    testPage.enter("streetAddress", "123 Some Street");
	    testPage.enter("state", "CA");
	    testPage.enter("apartmentNumber", "1b");
	    when(smartyStreetClient.validateAddress(any())).thenReturn(
	            Optional.of(new Address("smarty street", "Cooltown", "CA", "03104", "1b", "someCounty"))
	        );
	    testPage.clickButtonWithRetry("Continue", 10, "Out of State Address Notice");
	    testPage.clickCustomLink("Edit my address", "Home Address");
	    assertThat(driver.findElement(By.id("state")).getAttribute("value")).isEqualTo("CA");
	    testPage.enter("state", "MN");
	    when(smartyStreetClient.validateAddress(any())).thenReturn(
	            Optional.of(new Address("smarty street", "Cooltown", "MN", "03104", "1b", "Chisago"))
	        );
	    testPage.clickContinue("Mailing address"); // go to the mailing address page
	    assertThat(driver.findElement(By.id("state")).getAttribute("value")).isEqualTo("MN"); // mailing address page default state is MN
	    assertThat(testPage.getTitle()).isEqualTo("Mailing address");    
	    testPage.enter("zipCode", "03104");
	    testPage.enter("city", "Cooltown");
	    testPage.enter("streetAddress", "smarty street");
	    testPage.enter("state", "MN");
	    testPage.enter("apartmentNumber", "1b");
	    testPage.clickButtonWithRetry("Continue", 10, "Address Validation");
	    testPage.clickContinue("Contact Info");
  }
  
  /**
   * This method is looking for the "Use this address" button which is on the verifyMailingAddress page. <br>
   * "Use this address" button is shown when Smarty Streets can't find the address, which means there <br>
   * is no enrichment.
   * @param homeZip
   * @param homeCity
   * @param homeStreetAddress
   * @param homeApartmentNumber
   */
  protected void fillOutHomeAndMailingAddressWithoutEnrich(String homeZip, String homeCity,
      String homeStreetAddress, String homeApartmentNumber) {
    assertThat(testPage.getTitle()).isEqualTo("Home Address");
	testPage.enter("isHomeless", "I don't have a permanent address"); // check
	testPage.enter("isHomeless", "I don't have a permanent address"); // uncheck
    testPage.enter("zipCode", homeZip);
    testPage.enter("city", homeCity);
    testPage.enter("streetAddress", homeStreetAddress);
    testPage.enter("apartmentNumber", homeApartmentNumber);
    assertThat(testPage.findElementTextById("form-submit-button").equalsIgnoreCase("Continue"));
    testPage.clickButtonWithRetry("Continue", 20, "Mailing address");
    assertThat(testPage.getTitle()).isEqualTo("Mailing address");
    testPage.enter("zipCode", "03104");
    testPage.enter("city", "Cooltown");
    testPage.enter("streetAddress", "smarty street");
    testPage.enter("state", "MN");
    testPage.enter("apartmentNumber", "1b");
    testPage.clickContinue("Address Validation");
   
 // "Use this address" button is shown when Smarty Streets can't find the address
    testPage.clickButton("Use this address", "County Validation");
    testPage.clickButtonLink("Edit my county", "Identify County");
    testPage.enter("county", "Chisago");
    testPage.clickContinue("Contact Info");
  }
  
  
  protected void fillOutContactAndReview(boolean isReview, String county) {   
    // Check that we get the no phone number confirmation screen if no phone number is entered
    testPage.enter("email", "some@example.com");
    testPage.clickContinue("No phone number confirmation");
    assertThat(testPage.getTitle()).contains("No phone number confirmation");
    testPage.clickButtonLink("Add a phone number", "Contact Info");

    // How can we get in touch with you?
    testPage.enter("phoneNumber", "7234567890");
    testPage.enter("email", "some@example.com");
    assertThat(testPage.getCheckboxValues("phoneOrEmail")).contains("It's okay to text me",
        "It's okay to email me");
    testPage.clickContinue("Review info");
    
    if (isReview){
      // Let's review your info
      assertThat(driver.findElement(By.id("mailingAddress-address_street")).getText())
          .isEqualTo("smarty street");  
      assertThat(driver.findElement(By.id("home-address_county")).getText())
      .isEqualTo(county); 
    }
    
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
    testPage.clickLink("delete", "Delete a file");

    assertThat(testPage.getTitle()).isEqualTo("Delete a file");
    testPage.clickButton("Yes, delete the file", "Upload documents");
  }

  protected void waitForErrorMessage() {
    WebElement errorMessage = driver.findElement(By.className("text--error"));
    await().atMost(Duration.ofSeconds(30)).until(() -> !errorMessage.getText().isEmpty());
  }
}
