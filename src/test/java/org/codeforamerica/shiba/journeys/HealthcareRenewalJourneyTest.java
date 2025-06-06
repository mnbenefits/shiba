package org.codeforamerica.shiba.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.testutilities.PercyTestPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;


@Tag("HealthcareRenewalJourney")
public class HealthcareRenewalJourneyTest extends JourneyTest {

  protected void initTestPage() {
	    testPage = new PercyTestPage(driver);
	  }
  
  @Test
  void healthcareRenewalPageCheckDhsLink() {
    navigateTo("healthcareRenewalUpload");
    assertThat(driver.getTitle()).isEqualTo("Health Care Renewal Document Upload");
    assertNotNull(testPage.findElementById("headerHealthcareRenewal"));
    assertNotNull(testPage.findElementById("footerHealthcareRenewal"));
    WebElement logoElement = testPage.findElementById("mn-healthcare-renewal-logo");
    assertTrue(logoElement.getAttribute("href").equalsIgnoreCase("https://mn.gov/dhs/renewmycoverage/"));
    // Temporarily commenting this out to workaround Radware Captcha issue when test run in GitHub - Story 144389
    //testPage.clickLink("Renew my MN health care coverage");
    //assertThat(driver.getTitle()).isEqualTo("Renew my coverage / Minnesota Department of Human Services");
    //driver.navigate().back();

    // Switch to the Spanish version of the page and repeat verifications.
    testPage.selectFromDropdown("locales", "Español");
    logoElement = testPage.findElementById("mn-healthcare-renewal-logo");
    assertTrue(logoElement.getAttribute("href").equalsIgnoreCase("https://mn.gov/dhs/renewmycoverage-es/"));
    // Temporarily commenting this out to workaround Radware Captcha issue when test run in GitHub
    //testPage.clickElementById("mn-healthcare-renewal-logo");
    //assertThat(driver.getTitle()).isEqualTo("Renovar mi cobertura / Minnesota Department of Human Services");
  }
  
  @Test
  void healthcareRenewalFlow() {
	when(featureFlagConfiguration.get("show-wic-recommendation")).thenReturn(FeatureFlag.ON);
    navigateTo("healthcareRenewalUpload");
    assertThat(driver.getTitle()).isEqualTo("Health Care Renewal Document Upload");
    assertNotNull(testPage.findElementById("headerHealthcareRenewal"));
    assertNotNull(testPage.findElementById("footerHealthcareRenewal"));

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
    // verify that the header & footer are the health care renewal header & footer
    assertNotNull(testPage.findElementById("headerHealthcareRenewal"));
    assertNotNull(testPage.findElementById("footerHealthcareRenewal"));
    // verify that the chat feature does exist on the health care renewal header
    assertNotNull(testPage.findElementById("intercom_custom_launcher"));
    
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
    assertNotNull(testPage.findElementById("headerHealthcareRenewal"));
    assertNotNull(testPage.findElementById("footerHealthcareRenewal"));
    testPage.clickButtonLink("Continue", "Upload documents");

    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertNotNull(testPage.findElementById("headerHealthcareRenewal"));
    assertNotNull(testPage.findElementById("footerHealthcareRenewal"));
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    assertNotNull(testPage.findElementById("headerHealthcareRenewal"));
    assertNotNull(testPage.findElementById("footerHealthcareRenewal"));
    testPage.clickButtonLink("No, add more documents", "Upload documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    testPage.clickCustomButton("Yes, submit and finish", 3, "Documents Sent");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    assertNotNull(testPage.findElementById("headerHealthcareRenewal"));
    assertNotNull(testPage.findElementById("footerHealthcareRenewal"));
    verify(pageEventPublisher).publish(any());

    // Assert that applicant can't resubmit docs at this point
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    // repeat renewal flow to verify another session has been created
    navigateTo("healthcareRenewalUpload");
    assertThat(driver.getTitle()).isEqualTo("Health Care Renewal Document Upload");
    assertNotNull(testPage.findElementById("headerHealthcareRenewal"));
    assertNotNull(testPage.findElementById("footerHealthcareRenewal"));
    
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
    testPage.clickCustomButton("Yes, submit and finish", 3, "Documents Sent");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    // Assert that applicant can't resubmit docs at this point
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
  }

  
}
