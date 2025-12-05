package org.codeforamerica.shiba.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.NO;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.codeforamerica.shiba.testutilities.SuccessPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;


@Tag("minimumFlowJourney")
public class MinimumCcapFlowJourneyTest extends JourneyTest {

  @Test
  void fullApplicationOnlyCCAP() {
    when(clock.instant()).thenReturn(
        LocalDateTime.of(2020, 1, 1, 10, 10).atOffset(ZoneOffset.UTC).toInstant(),
        LocalDateTime.of(2020, 1, 1, 10, 15, 30).atOffset(ZoneOffset.UTC).toInstant());

    List<String> programSelections = List.of(PROGRAM_CCAP);
    getToHomeAddress("Hennepin", programSelections);

    // Where are you currently Living?
    fillOutHomeAndMailingAddress("03104", "Cooltown", "smarty street", "1b", "MN");
    
    fillOutContactAndReview(true, "Hennepin");
    
    testPage.clickButtonLink("This looks correct", "Do you want to add household members?");

    // Add 1 Household Member
    assertThat(testPage.getElementText("page-form")).doesNotContain(
        "Roommates that you buy and prepare food with");

    testPage.chooseYesOrNo("addHouseholdMembers", NO.getDisplayValue(), "Add Children confirmation");
    // "add child nudge" page
    assertThat(testPage.getTitle()).contains("Add Children confirmation");
    testPage.clickCustomButton("Add my children", 3, "Start Household");
    // startHousehold page
    testPage.clickButtonLink("Continue", "Housemate: Personal Info");
    String householdMemberFirstName = "householdMemberFirstName";
    String householdMemberLastName = "householdMemberLastName";
    String householdMemberFullName = householdMemberFirstName + " " + householdMemberLastName;

    testPage.enter("firstName", householdMemberFirstName);
    testPage.enter("lastName", householdMemberLastName);
    testPage.enter("otherName", "houseHoldyMcMemberson");
    testPage.enter("dateOfBirth", "09/14/2018");
    testPage.enter("maritalStatus", "Never married");
    testPage.enter("sex", "Male");
    testPage.enter("relationship", "My child");
    testPage.enter("programs", PROGRAM_CCAP);
    // Assert that the programs follow up questions are shown when a program is selected
    WebElement programsFollowUp = testPage.findElementById("programs-follow-up");
    assertThat(programsFollowUp.getCssValue("display")).isEqualTo("block");
    testPage.enter("ssn", "987654321");
    testPage.clickContinue("Household members");

    testPage.clickButtonLink("Yes, that's everyone", "Who are the children in need of care?");

    // Who are the children in need of child care
    // First, verify proper navigation when no children are selected
    testPage.clickContinue("Mental health needs & child care");
    assertThat(testPage.getTitle()).contains("Mental health needs & child care");
    testPage.chooseYesOrNo("childCareMentalHealth", NO.getDisplayValue(), "Housing subsidy");
   
    assertThat(testPage.getTitle()).contains("Housing subsidy");
    testPage.goBack();
    testPage.goBack();
    // Now pick the household member (i.e., child) from the childrenWhoNeedCare page.
    testPage.enter("whoNeedsChildCare", householdMemberFullName);
    testPage.clickContinue("Do you have a child care provider?");
    
    // Do you have a child care provider?
    testPage.chooseYesOrNo("hasChildCareProvider", NO.getDisplayValue(), "Who has a parent not at home?");

    // Who are the children that have a parent not living at home?
    testPage.enter("whoHasAParentNotLivingAtHome", householdMemberFullName);
    testPage.clickContinue("Name of parent outside home");

    // Tell us the name of any parent living outside the home.
    String parentNotAtHomeName = "My child's parent";
    driver.findElement(By.name("whatAreTheParentsNames[]")).sendKeys(parentNotAtHomeName);
    testPage.clickContinue("Child support payments");
    
    //child support
    testPage.enter("whoReceivesChildSupportPayments", householdMemberFullName);
    testPage.clickContinue("Mental health needs & child care");
    assertThat(testPage.getTitle()).contains("Mental health needs & child care");
    testPage.chooseYesOrNo("childCareMentalHealth", NO.getDisplayValue(), "Housing subsidy");
   
    // Do you receive housing subsidy
    testPage.chooseYesOrNo("hasHousingSubsidy", NO.getDisplayValue(), "Living situation");
    
    // What is your current living situation?
    testPage.enter("livingSituation", "Staying in a hotel or motel");
    testPage.clickContinue("Going to school");

    // Is anyone in your household going to school right now, either full or
    // part-time?
    testPage.chooseYesOrNo("goingToSchool", NO.getDisplayValue(), "Pregnant");

    // Is anyone in your household pregnant?
    testPage.chooseYesOrNo("isPregnant", NO.getDisplayValue(), "Expedited Migrant Farm Worker, Household");

    // Is anyone in your household a migrant or seasonal farm worker?
    testPage.chooseYesOrNo("migrantOrSeasonalFarmWorker", NO.getDisplayValue(), "Citizenship");

    // Please confirm the citizenship status of your household
    testPage.clickElementById("citizenshipStatus[]-0-BIRTH_RIGHT"); //Applicant status
    testPage.clickElementById("citizenshipStatus[]-1-BIRTH_RIGHT"); //person 2 status
	testPage.clickContinue("Tribal Nation member");

    // Is anyone in your household a member of a tribal nation?
    testPage.chooseYesOrNo("isTribalNationMember", NO.getDisplayValue(), "Intro: Income");

    // Income & Employment
    assertThat(testPage.getElementText("milestone-step")).isEqualTo("Step 3 of 6");
    testPage.clickButtonLink("Continue", "Employment status");

    // Is anyone in your household making money from a job?
    testPage.chooseYesOrNo("areYouWorking", NO.getDisplayValue(), "Job Search");

    // Is anyone in the household currently looking for a job?
    testPage.chooseYesOrNo("currentlyLookingForJob", NO.getDisplayValue(), "Income Up Next");

    // Got it! You're almost done with the income section.
    testPage.clickButtonLink("Continue", "Unearned Income");

    // Does anyone in your household get income from these sources?
    testPage.enter("unearnedIncome", "None of the above");
    testPage.clickContinue("Unearned Income");

    testPage.enter("otherUnearnedIncome", "None of the above");
    testPage.clickContinue("Future Income");

    driver.findElement(By.id("additionalIncomeInfo"))
        .sendKeys("I also make a small amount of money from my lemonade stand.");
    testPage.clickContinue("Start Expenses");

    // Expenses & Deductions
    testPage.clickButtonLink("Continue", "Medical expenses");
    testPage.enter("medicalExpenses", "None of the above");
    testPage.clickContinue("Support and Care");

    // Does anyone in the household pay for court-ordered child support, spousal
    // support, child care support or medical care?
    testPage.chooseYesOrNo("supportAndCare", NO.getDisplayValue(), "Assets");
   
    // Does anyone in your household have any of these?
    testPage.enter("assets", "None of the above");
    driver.findElement(By.xpath("//*[contains(text(),\"Assets include your family's cash, bank accounts, vehicles, investments, and real estate\")]")).isDisplayed();
    testPage.clickContinue("Sold assets");

    // In the last 12 months, has anyone in the household given away or sold any
    // assets?
    testPage.chooseYesOrNo("haveSoldAssets", NO.getDisplayValue(), "Submitting Application");

    // Submitting your Application
    testPage.clickButtonLink("Continue", "Register to vote");
    testPage.clickCustomButton("No thanks", 3, "Healthcare Coverage");

    // Do you currently have healthcare coverage?
    testPage.enter("healthcareCoverage", NO.getDisplayValue());
    testPage.clickContinue("Authorized Rep");

    // Do you want to assign someone to help with your benefits?
    testPage.chooseYesOrNo("helpWithBenefits", NO.getDisplayValue(), "Additional Info");

    // Is there anything else you want to share?
    driver.findElement(By.id("additionalInfo")).sendKeys("No I don't");
    testPage.clickContinue("Can we ask");

    // Can we ask about your race and ethnicity?
    testPage.clickButtonLink("Yes, continue", "Race and Ethnicity");

    // What races or ethnicities do you identify with?
    testPage.enter("raceAndEthnicity", List.of("Middle Eastern or North African"));
    testPage.clickContinue("Legal Stuff");

    // The legal stuff.
    testPage.enter("agreeToTerms", "I agree");
    // for CCAP only this should not be displayed
    assertTrue(testPage.elementDoesNotExistById("drugFelony1"));
    testPage.clickAccordianButton("a1");//Close accordion so Selenium can find the Continue button.
    testPage.clickContinue("Sign this application");

    // Upload documents
    testPage.enter("applicantSignature", "this is my signature");
    testPage.clickContinue("Submit application");
    testPage.clickCustomButton("Submit application", 3, "Submission Confirmation");
    testPage.clickButtonLink("Continue", "Adding Documents");// submissionConfirmation
    testPage.clickButtonLink("Continue", "Document Recommendation");// addingDocuments
    testPage.clickButtonLink("I'll do this later", "Document offboarding");// documentRecommendation
    testPage.clickButtonLink("Finish application", "Additional Program Documents");// documentOffboarding
    testPage.clickButtonLink("Continue", "Your next steps");// programDocuments

    assertThat(driver.getTitle()).isEqualTo("Your next steps");
    // Next steps screen
    // TODO:  Fix this conditional logic once the enhanced nextSteps page is fully implemented.
    List<WebElement> pageElements = driver.findElements(By.id("original-next-steps"));
    testPage.clickElementById("button-a2");
    testPage.clickElementById("button-a3");
    testPage.clickElementById("button-a4");
    if (pageElements.isEmpty()) {
    	 List<String> expectedMessages = List.of(
    	    		"You did not upload documents with your application today.",
    	    		"To upload documents later, you can return to our homepage and click on ‘Upload documents’ to get started.",
    	    		"Within the next 5 days, expect a phone call or letter in the mail from an eligibility worker with information about your next steps.",
			 		"Program(s) on your application may require you to talk with a worker about your application.",
			 		"A worker from your county or Tribal Nation will contact you to schedule an interview. Your interview can be held over the phone or face-to-face.");
    	 List<String> nextStepSections = driver.findElements(By.className("next-step-section")).stream().map(WebElement::getText).collect(Collectors.toList());
     	 assertThat(nextStepSections).containsExactly(expectedMessages.toArray(new String[0]));
    }
    testPage.clickButtonLink("Continue", "Success");// nextSteps
    SuccessPage successPage = new SuccessPage(driver);
    assertThat(successPage.findElementById("submission-date").getText()).
        contains(
            "Your application was submitted to Hennepin County (612-596-1300) on January 1, 2020.");
    
    testPage.clickButtonLink("View more programs", "Recommendations");
    // Verify that the "paying for child care" link exists and links to DHS-3551
    String successPageWindowHandle = driver.getWindowHandle();
    testPage.clickLinkToExternalWebsite("resources for families with young children.");
    ArrayList<String> windowHandles = new ArrayList<String>(driver.getWindowHandles());
    driver.switchTo().window(windowHandles.get(1));
    assertThat(driver.getCurrentUrl()).isEqualTo("https://edocs.dhs.state.mn.us/lfserver/Public/DHS-3551-ENG");
    driver.close();
    driver.switchTo().window(successPageWindowHandle);

  }
}
