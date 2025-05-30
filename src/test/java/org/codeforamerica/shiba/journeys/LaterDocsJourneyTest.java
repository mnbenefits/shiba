package org.codeforamerica.shiba.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@Tag("laterDocsJourney")
public class LaterDocsJourneyTest extends JourneyTest {

  
  @Test
  void laterDocsFlow() {
    testPage.clickButtonLink("Upload documents", "Ready to upload documents");

    assertThat(driver.getTitle()).isEqualTo("Ready to upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    testPage.clickButtonLink("Continue", "Match Info");

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
    testPage.clickContinue("Identify County");
    
    // Identify County
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue("Tribal Nation member");

    // Tribal Nation Member
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.chooseYesOrNo("isTribalNationMember","Yes", "Select a Tribal Nation");
    
    // Select Tribal Nation
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue("How to add documents");
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    
    // when the county is Clearwater, the page after selectTheTribe is Nations Boundary
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    testPage.enter("county", "Clearwater");
    testPage.clickContinue("Tribal Nation member");
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.chooseYesOrNo("isTribalNationMember","Yes", "Select a Tribal Nation");
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue("Nations Boundary");
    assertThat(driver.getTitle()).isEqualTo("Nations Boundary");
    testPage.chooseYesOrNo("livingInNationBoundary", "Yes", "Tribal Nation of residence");
    testPage.enter("selectedNationOfResidence", "Red Lake Nation");
    testPage.clickContinue("How to add documents");
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    // when White Earth Nation is selected, Nations Boundary is not displayed
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.enter("selectedTribe", "White Earth Nation");
    testPage.clickContinue("How to add documents");
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    //when the answer to live in nationsBoundary question is No, then next page is HowToAddDocuments
    testPage.goBack();
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue("Nations Boundary");
    assertThat(driver.getTitle()).isEqualTo("Nations Boundary");
    //testPage.clickButton("No", "How to add documents");
    testPage.chooseYesOrNo("livingInNationBoundary","No", "How to add documents");
    
    // How to add documents
    testPage.clickButtonLink("Continue", "Upload documents");
    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickButtonLink("No, add more documents", "Upload documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    testPage.clickButton("Yes, submit and finish", "Documents Sent");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    verify(pageEventPublisher).publish(any());

    // Assert that applicant can't resubmit docs at this point
    navigateTo("uploadDocuments");//There is no back link on the Documents Sent page, so use the browser back arrow.
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");

    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
  }
  
  @Test
  void laterDocsMinimumFlow() {
     testPage.clickButtonLink("Upload documents", "Ready to upload documents");

    assertThat(driver.getTitle()).isEqualTo("Ready to upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    testPage.clickButtonLink("Continue", "Match Info");

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
    testPage.clickContinue("Identify County");

    // Identify County
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue("Tribal Nation member");

    // Tribal Nation Member
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.chooseYesOrNo("isTribalNationMember","No", "How to add documents");
    
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.clickButtonLink("Continue", "Upload documents");
    
    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickButtonLink("No, add more documents", "Upload documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    testPage.clickButton("Yes, submit and finish", "Documents Sent");
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
  void laterDocsTribalNationFlow() {
	  testPage.clickButtonLink("Upload documents", "Ready to upload documents");

    assertThat(driver.getTitle()).isEqualTo("Ready to upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    testPage.clickButtonLink("Continue", "Match Info");

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
    testPage.clickContinue("Identify County");

    // Identify County
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue("Tribal Nation member");

    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.chooseYesOrNo("isTribalNationMember","Yes", "Select a Tribal Nation");
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.enter("selectedTribe", "Red Lake Nation");

    testPage.clickContinue("How to add documents");
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    testPage.enter("county", "Clearwater");
    testPage.clickContinue("Tribal Nation member");
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.chooseYesOrNo("isTribalNationMember","Yes", "Select a Tribal Nation");
    
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue("Nations Boundary");

    assertThat(driver.getTitle()).isEqualTo("Nations Boundary");
    testPage.chooseYesOrNo("livingInNationBoundary", "Yes", "Tribal Nation of residence");
    testPage.enter("selectedNationOfResidence", "Red Lake Nation");
    assertThat(testPage.getHeader()).isEqualTo("Tell us which Tribal Nation you live in.");
    testPage.clickContinue("How to add documents");
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    // when White Earth Nation is selected, Nations Boundary is not displayed
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    testPage.enter("selectedTribe", "White Earth Nation");
    testPage.clickContinue("How to add documents");
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    //when the answer to live in nationsBoundary question is No, then next page is HowToAddDocuments
    testPage.goBack();
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue("Nations Boundary");
    //at this point we should be on the nations boundary page
    testPage.chooseYesOrNo("livingInNationBoundary", "No", "How to add documents");
    
    // How to add documents
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.clickButtonLink("Continue", "Upload documents");
    
    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickButtonLink("No, add more documents", "Upload documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    testPage.clickButton("Yes, submit and finish", "Documents Sent");
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
  void laterDocsMinimumFlowBrowserBackTest() {
     testPage.clickButtonLink("Upload documents", "Ready to upload documents");

    assertThat(driver.getTitle()).isEqualTo("Ready to upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    testPage.clickButtonLink("Continue", "Match Info");

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
    testPage.clickContinue("Identify County");

    // Identify County
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue("Tribal Nation member");

    // Tribal Nation Member
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.chooseYesOrNo("isTribalNationMember","No", "How to add documents");

    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.clickButtonLink("Continue", "Upload documents");
    
    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickButtonLink("No, add more documents", "Upload documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    testPage.clickButton("Yes, submit and finish", "Documents Sent");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    verify(pageEventPublisher).publish(any());

    // Assert that applicant can't go back to any previous pages or resubmit docs at this point.
   // Back navigation from the stack of pages in the browser history get called in backwards order.
    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
  
    navigateTo("howToAddDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("tribalNationMember");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("identifyCounty");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("matchInfo");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("readyToUploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("landing");
    assertThat(driver.getTitle()).isEqualTo("MNbenefits"); 
  }
  
  @Test
  void laterDocsTribalNationFlowBrowserBackTest() {
	testPage.clickButtonLink("Upload documents", "Ready to upload documents");

    assertThat(driver.getTitle()).isEqualTo("Ready to upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    testPage.clickButtonLink("Continue", "Match Info");

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
    testPage.clickContinue("Identify County");

    // Identify County
    assertThat(driver.getTitle()).isEqualTo("Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue("Tribal Nation member");

    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.chooseYesOrNo("isTribalNationMember","Yes", "Select a Tribal Nation");
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue("How to add documents");
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    testPage.enter("county", "Clearwater");
    testPage.clickContinue("Tribal Nation member");
    assertThat(driver.getTitle()).isEqualTo("Tribal Nation member");
    testPage.chooseYesOrNo("isTribalNationMember","Yes", "Select a Tribal Nation");
    
    assertThat(driver.getTitle()).isEqualTo("Select a Tribal Nation");
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue("Nations Boundary");

    assertThat(driver.getTitle()).isEqualTo("Nations Boundary");
    testPage.chooseYesOrNo("livingInNationBoundary","Yes", "Tribal Nation of residence");
    testPage.enter("selectedNationOfResidence", "Red Lake Nation");
    assertThat(testPage.getHeader()).isEqualTo("Tell us which Tribal Nation you live in.");
    testPage.clickContinue("How to add documents");
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    // when White Earth Nation is selected, Nations Boundary is not displayed
    testPage.goBack();
    testPage.goBack();
    testPage.goBack();
    testPage.enter("selectedTribe", "White Earth Nation");
    testPage.clickContinue("How to add documents");
    assertThat(driver.getTitle()).isEqualTo("How to add documents");

    //when the answer to live in nationsBoundary question is No, then next page is HowToAddDocuments
    testPage.goBack();
    testPage.enter("selectedTribe", "Red Lake Nation");
    testPage.clickContinue("Nations Boundary");
    //at this point we should be on the nations boundary page
    testPage.chooseYesOrNo("livingInNationBoundary","No", "How to add documents");
    
    // How to add documents
    assertThat(driver.getTitle()).isEqualTo("How to add documents");
    testPage.clickButtonLink("Continue", "Upload documents");
    
    // should allow me to upload documents and those documents should be sent to the ESB
    assertThat(driver.getTitle()).isEqualTo("Upload documents");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    assertThat(driver.findElements(By.className("reveal")).size()).isEqualTo(0);

    uploadPdfFile();
    waitForDocumentUploadToComplete();
    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    assertThat(driver.getTitle()).isEqualTo("Doc submit confirmation");
    testPage.clickButtonLink("No, add more documents", "Upload documents"); // Go back
    assertThat(driver.getTitle()).isEqualTo("Upload documents");

    testPage.clickButtonLink("Submit my documents", "Doc submit confirmation");
    testPage.clickCustomButton("Yes, submit and finish", 3, "Documents Sent");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    assertNotNull(testPage.findElementById("headerMNbenefits"));
    assertNotNull(testPage.findElementById("footerMNbenefits"));
    verify(pageEventPublisher).publish(any());

    // Assert that applicant can't go back to previous pages or resubmit docs at this point.
    // Back navigation from the stack of pages in the browser history get called in backwards order.
    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("documentSubmitConfirmation");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("uploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("howToAddDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("nationsBoundary");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("selectTheTribe");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("tribalNationMember");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("identifyCounty");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("matchInfo");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("readyToUploadDocuments");
    assertThat(driver.getTitle()).isEqualTo("Documents Sent");
    
    navigateTo("landing");
    assertThat(driver.getTitle()).isEqualTo("MNbenefits"); 

  }
  
  
}
