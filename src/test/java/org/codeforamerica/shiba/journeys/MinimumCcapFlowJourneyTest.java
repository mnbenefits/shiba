package org.codeforamerica.shiba.journeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.NO;
import static org.codeforamerica.shiba.testutilities.YesNoAnswer.YES;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.testutilities.SuccessPage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;


@Tag("minimumFlowJourney")
public class MinimumCcapFlowJourneyTest extends JourneyTest {

  @Disabled("This test passes on VDIs but fails on GitHub")
  @Test
  void fullApplicationOnlyCCAP() {
    when(clock.instant()).thenReturn(
        LocalDateTime.of(2020, 1, 1, 10, 10).atOffset(ZoneOffset.UTC).toInstant(),
        LocalDateTime.of(2020, 1, 1, 10, 15, 30).atOffset(ZoneOffset.UTC).toInstant());
    when(featureFlagConfiguration.get("child-care")).thenReturn(FeatureFlag.ON);
    when(featureFlagConfiguration.get("show-wic-recommendation")).thenReturn(FeatureFlag.ON);

    List<String> programSelections = List.of(PROGRAM_CCAP);
    getToHomeAddress("Hennepin", programSelections);

    // Where are you currently Living?
    fillOutHomeAndMailingAddress("12345", "someCity", "someStreetAddress", "someApartmentNumber");
    
    fillOutContactAndReview(true, "Hennepin");
    
    testPage.clickLink("This looks correct");

    // Add 1 Household Member
    assertThat(testPage.getElementText("page-form")).doesNotContain(
        "Roommates that you buy and prepare food with");

    testPage.enter("addHouseholdMembers", NO.getDisplayValue());
    // "add child nudge" page
    assertThat(testPage.getTitle()).contains("Add Children confirmation");
    testPage.clickButton("Add my children");
    // startHousehold page
    testPage.clickContinue();
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
    testPage.clickContinue();

    testPage.clickButton("Yes, that's everyone");

    // Who are the children in need of child care
    // First, verify proper navigation when no children are selected
    testPage.clickContinue();
    assertThat(testPage.getTitle()).contains("Housing subsidy");
    testPage.goBack();
    // Now pick the household member (i.e., child) from the childrenWhoNeedCare page.
    testPage.enter("whoNeedsChildCare", householdMemberFullName);
    testPage.clickContinue();
    
    // Do you have a child care provider?
    testPage.enter("hasChildCareProvider", NO.getDisplayValue());

    // Who are the children that have a parent not living at home?
    testPage.enter("whoHasAParentNotLivingAtHome", householdMemberFullName);
    testPage.clickContinue();

    // Tell us the name of any parent living outside the home.
    String parentNotAtHomeName = "My child's parent";
    //driver.findElementByName("whatAreTheParentsNames[]").sendKeys(parentNotAtHomeName);
    driver.findElement(By.name("whatAreTheParentsNames[]")).sendKeys(parentNotAtHomeName);
    testPage.clickContinue();
    
    //child support
    testPage.enter("whoReceivesChildSupportPayments", householdMemberFullName);
    testPage.clickContinue();

    // Do you receive housing subsidy
    testPage.enter("hasHousingSubsidy", NO.getDisplayValue());
    
    // What is your current living situation?
    testPage.enter("livingSituation", "Staying in a hotel or motel");
    testPage.clickContinue();

    // Is anyone in your household going to school right now, either full or
    // part-time?
    testPage.enter("goingToSchool", NO.getDisplayValue());

    // Is anyone in your household pregnant?
    testPage.enter("isPregnant", NO.getDisplayValue());

    // Is anyone in your household a migrant or seasonal farm worker?
    testPage.enter("migrantOrSeasonalFarmWorker", NO.getDisplayValue());

    // Is everyone in your household a U.S. Citizen?
    testPage.enter("isUsCitizen", YES.getDisplayValue());

    // In the last 2 months, did anyone in your household do any of these things?
    testPage.enter("hasWorkSituation", NO.getDisplayValue());

    // Is anyone in your household a member of a tribal nation?
    testPage.enter("isTribalNationMember", NO.getDisplayValue());

    // Income & Employment
    assertThat(testPage.getElementText("milestone-step")).isEqualTo("Step 3 of 6");
    testPage.clickContinue();

    // Is anyone in your household making money from a job?
    testPage.enter("areYouWorking", NO.getDisplayValue());

    // Is anyone in the household currently looking for a job?
    testPage.enter("currentlyLookingForJob", NO.getDisplayValue());

    // Got it! You're almost done with the income section.
    testPage.clickContinue();

    // Does anyone in your household get income from these sources?
    testPage.enter("unearnedIncome", "None of the above");
    testPage.clickContinue();

    testPage.enter("otherUnearnedIncome", "None of the above");
    testPage.clickContinue();

    driver.findElement(By.id("additionalIncomeInfo"))
        .sendKeys("I also make a small amount of money from my lemonade stand.");
    testPage.clickContinue();

    // Expenses & Deductions
    testPage.clickContinue();
    testPage.enter("medicalExpenses", "None of the above");
    testPage.clickContinue();

    // Does anyone in the household pay for court-ordered child support, spousal
    // support, child care support or medical care?
    testPage.enter("supportAndCare", NO.getDisplayValue());
   
    // Does anyone in your household have any of these?
    testPage.enter("assets", "None of the above");
    driver.findElement(By.xpath("//*[contains(text(),\"Assets include your family's cash, bank accounts, vehicles, investments, and real estate\")]")).isDisplayed();
    testPage.clickContinue();

    // Does anyone in the household have money in a bank account or debit card?
    testPage.enter("haveSavings", NO.getDisplayValue());

    // In the last 12 months, has anyone in the household given away or sold any
    // assets?
    testPage.enter("haveSoldAssets", NO.getDisplayValue());

    // Submitting your Application
    testPage.clickContinue();
    testPage.clickButton("No thanks");

    // Do you currently have healthcare coverage?
    testPage.enter("healthcareCoverage", NO.getDisplayValue());
    testPage.clickContinue();

    // Do you want to assign someone to help with your benefits?
    testPage.enter("helpWithBenefits", NO.getDisplayValue());

    // Is there anything else you want to share?
    driver.findElement(By.id("additionalInfo")).sendKeys("No I don't");
    testPage.clickContinue();

    // Can we ask about your race and ethnicity?
    testPage.clickLink("Yes, continue");

    // What races or ethnicities do you identify with?
    testPage.enter("raceAndEthnicity", List.of("Middle Eastern or North African"));
    testPage.clickContinue();

    // The legal stuff.
    testPage.enter("agreeToTerms", "I agree");
    // for CCAP only this should not be displayed
    assertTrue(testPage.elementDoesNotExistById("drugFelony1"));
    testPage.clickAccordianButton("a1");//Close accordion so Selenium can find the Continue button.
    testPage.clickContinue();

    // Upload documents
    testPage.enter("applicantSignature", "this is my signature");
    testPage.clickContinue();
    testPage.clickButton("Submit");
    testPage.clickContinue();// submissionConfirmation
    testPage.clickContinue();// addingDocuments
    testPage.clickButton("I'll do this later");// documentRecommendation
    testPage.clickButton("Finish application");// documentOffboarding
    testPage.clickContinue();// programDocuments

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
    testPage.clickContinue();// nextSteps
    SuccessPage successPage = new SuccessPage(driver);
    assertThat(successPage.findElementById("submission-date").getText()).
        contains(
            "Your application was submitted to Hennepin County (612-596-1300) on January 1, 2020.");
    
    testPage.clickLink("View more programs");
    // Verify that the "paying for child care" link exists and links to DHS-3551
    String successPageWindowHandle = driver.getWindowHandle();
    testPage.clickLink("resources for families with young children.");
    ArrayList<String> windowHandles = new ArrayList<String>(driver.getWindowHandles());
    driver.switchTo().window(windowHandles.get(1));
    assertThat(driver.getCurrentUrl()).isEqualTo("https://edocs.dhs.state.mn.us/lfserver/Public/DHS-3551-ENG");
    driver.close();
    driver.switchTo().window(successPageWindowHandle);

  }
}
