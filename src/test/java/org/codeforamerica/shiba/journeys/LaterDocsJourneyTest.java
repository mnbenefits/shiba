package org.codeforamerica.shiba.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@Tag("laterDocsJourney")
public class LaterDocsJourneyTest extends JourneyTest {

  
  @Disabled("This test passes on VDIs but fails on GitHub")
  @Test
  void laterDocsFlow() {
    testPage.clickButton("Upload documents");

    assertThat(driver.getTitle()).isEqualTo("Ready to upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    testPage.clickContinue();
//    testPage.clickLink("Enter my zip code instead.");
//    assertThat(driver.getTitle()).isEqualTo("Identify zip");
//
//    // should direct me to email the county if my zipcode is unrecognized or unsupported
//    testPage.enter("zipCode", "11111");
//    testPage.clickContinue();
//    assertThat(driver.getTitle()).isEqualTo("Email Docs To Your County");
//
//    // should allow me to proceed with the flow if I enter a zip code for an active county
//    testPage.clickLink("< Go Back");
//    testPage.enter("zipCode", "55444");
//    testPage.clickContinue();
//    assertThat(driver.getTitle()).isEqualTo("Match Info");
//
//    // should direct me to email docs to my county if my county is not supported
//    navigateTo("identifyCounty");

    assertThat(driver.getTitle()).isEqualTo("Match Info");
    // verify that the header & footer are the MNbenefits header & footer
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    // verify that the chat feature does exist on the MNbenefits header
    assertFalse(testPage.elementDoesNotExistById("intercom_custom_launcher"));

    testPage.enter("firstName", "defaultFirstName");
    testPage.enter("lastName", "defaultLastName");
    testPage.enter("ssn", "123456789");
    testPage.enter("caseNumber", "1234567");
    testPage.enter("phoneNumber", "7041234567");
    testPage.clickContinue();
    
    // Identify County
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue();

    // Tribal Nation Member
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.clickButton("Yes");
    
    // Select Tribal Nation
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue();
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    
    // when the county is Clearwater, the page after selectTheTribe is Nations Boundary
    navigateTo("identifyCounty");
    testPage.enter("county", "Clearwater");
    testPage.clickContinue();
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.clickButton("Yes");
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue();
    assertThat(driver.getTitle()).isEqualTo("Nations Boundary");
    testPage.clickButton("Yes");
    testPage.enter("selectedNationOfResidence", "Red Lake Nation");
    assertThat(testPage.getHeader()).isEqualTo("Tell us which Tribal Nation you live in.");
    testPage.clickContinue();
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    // when White Earth Nation is selected, Nations Boundary is not displayed
    navigateTo("selectTheTribe");
    testPage.enter("selectedTribe", "White Earth Nation");
    testPage.clickContinue();
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    //when the answer to live in nationsBoundary question is No, then next page is HowToAddDocuments
    navigateTo("selectTheTribe");
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue();
    //at this point we should be on the nations boundary page
    testPage.clickButton("No");
    
    // How to add documents
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.clickContinue();
    
    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButton("Submit my documents");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickButton("No, add more documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButton("Submit my documents");
    testPage.clickButton("Yes, submit and finish");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    verify(pageEventPublisher).publish(any());

    // Assert that applicant can't resubmit docs at this point
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
  }
  
  @Test
  void laterDocsMinimumFlow() {
     testPage.clickButton("Upload documents");

    assertThat(driver.getTitle()).isEqualTo("Ready to upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    testPage.clickContinue();

    assertThat(driver.getTitle()).isEqualTo("Match Info");
    // verify that the header & footer are the MNbenefits header & footer
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    // verify that the chat feature does exist on the MNbenefits header
    assertFalse(testPage.elementDoesNotExistById("intercom_custom_launcher"));

    testPage.enter("firstName", "defaultFirstName");
    testPage.enter("lastName", "defaultLastName");
    testPage.enter("ssn", "123456789");
    testPage.enter("caseNumber", "1234567");
    testPage.enter("phoneNumber", "7041234567");
    testPage.clickContinue();
    
    // Identify County
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue();

    // Tribal Nation Member
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.clickButton("No");

    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.clickContinue();
    
    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButton("Submit my documents");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickButton("No, add more documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButton("Submit my documents");
    testPage.clickButton("Yes, submit and finish");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    verify(pageEventPublisher).publish(any());

    // Assert that applicant can't resubmit docs at this point
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
  }
  
  @Disabled("This test randomly fails on VDIs")
  @Test
  void laterDocsTribalNationFlow() {
    testPage.clickButton("Upload documents");

    assertThat(driver.getTitle()).isEqualTo("Ready to upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    testPage.clickContinue();

    assertThat(driver.getTitle()).isEqualTo("Match Info");
    // verify that the header & footer are the MNbenefits header & footer
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    // verify that the chat feature does exist on the MNbenefits header
    assertFalse(testPage.elementDoesNotExistById("intercom_custom_launcher"));

    testPage.enter("firstName", "defaultFirstName");
    testPage.enter("lastName", "defaultLastName");
    testPage.enter("ssn", "123456789");
    testPage.enter("caseNumber", "1234567");
    testPage.enter("phoneNumber", "7041234567");
    testPage.clickContinue();

    // Identify County
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue();

    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.clickButton("Yes");
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.enter("selectedTribe", "Red Lake Nation");

    testPage.clickContinue();
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    testPage.enter("county", "Clearwater");
    testPage.clickContinue();
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.clickButton("Yes");

    testPage.clickContinue();

    assertThat(driver.getTitle()).isEqualTo("Nations Boundary");
    testPage.clickButton("Yes");
    testPage.enter("selectedNationOfResidence", "Red Lake Nation");
    assertThat(testPage.getHeader()).isEqualTo("Tell us which Tribal Nation you live in.");
    testPage.clickContinue();
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    // when White Earth Nation is selected, Nations Boundary is not displayed
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    testPage.enter("selectedTribe", "White Earth Nation");
    testPage.clickContinue();
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    //when the answer to live in nationsBoundary question is No, then next page is HowToAddDocuments
    testPage.goBack();
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue();
    //at this point we should be on the nations boundary page
    testPage.clickButton("No");
    
    // How to add documents
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.clickContinue();
    
    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButton("Submit my documents");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickButton("No, add more documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButton("Submit my documents");
    testPage.clickButton("Yes, submit and finish");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    verify(pageEventPublisher).publish(any());

    // Assert that applicant can't resubmit docs at this point
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
  }
  
}
