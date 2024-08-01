package org.codeforamerica.shiba.pages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.pages.WicRecommendationService;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

public class WicRecommendationServiceTest {
	
	WicRecommendationService wicRecommendationService = new WicRecommendationService();
	
	@Test
	public void isAnybodyInHouseholdPregnant() {
		
	    ApplicationData applicationData = new ApplicationData();
	    
	    //TODO create year more than 5 for test
	    
	    applicationData = new TestApplicationDataBuilder(applicationData)
		        .withPersonalInfo()
		        .withContactInfo()
		        .withApplicantPrograms(List.of("CCAP"))
		        .withSubworkflow("household",
		            new PagesDataBuilder().withPageData("householdMemberInfo", Map.of(
		                "firstName", "Jane",
		                "lastName", "Testerson",
		                "dateOfBirth", List.of("01", "09", "2016"))).build())
		        .withApplicantPrograms(List.of("SNAP"))
		        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
		        .withPageData("pregnant", "isPregnant", List.of("true"))
		        .build();
	    applicationData.setId("9870000123");
	    applicationData.setFlow(FlowType.FULL);
		boolean somebodyIsPregnant = wicRecommendationService.hasPregnantHouseholdMember(applicationData);
		assertTrue(somebodyIsPregnant);
	}

	
	@Test
	public void testChildLessThan5() {
		
	    ApplicationData applicationData = new ApplicationData();
	    
	    //TODO create year less than 5 for test
	    
	    applicationData = new TestApplicationDataBuilder(applicationData)
		        .withPersonalInfo()
		        .withContactInfo()
		        .withApplicantPrograms(List.of("CCAP"))
		        .withSubworkflow("household",
		            new PagesDataBuilder().withPageData("householdMemberInfo", Map.of(
		                "firstName", "Jane",
		                "lastName", "Testerson",
		                "dateOfBirth", List.of("01", "09", "2020"))).build())
		        .withApplicantPrograms(List.of("SNAP"))
		        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
		        .build();
	    applicationData.setId("9870000123");
	    applicationData.setFlow(FlowType.FULL);
	    boolean hasChild5OrUnder = wicRecommendationService.hasHouseholdMemberUpToAge5(applicationData);
	    assertTrue(hasChild5OrUnder);
	}
	
	@Test
	public void testChildMoreThan5() {
		
	    ApplicationData applicationData = new ApplicationData();
	    
	    //TODO create year more than 5 for test
	    
	    applicationData = new TestApplicationDataBuilder(applicationData)
		        .withPersonalInfo()
		        .withContactInfo()
		        .withApplicantPrograms(List.of("CCAP"))
		        .withSubworkflow("household",
		            new PagesDataBuilder().withPageData("householdMemberInfo", Map.of(
		                "firstName", "Jane",
		                "lastName", "Testerson",
		                "dateOfBirth", List.of("01", "09", "2016"))).build())
		        .withApplicantPrograms(List.of("SNAP"))
		        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
		        .build();
	    applicationData.setId("9870000123");
	    applicationData.setFlow(FlowType.FULL);
	    boolean hasChild5OrUnder = wicRecommendationService.hasHouseholdMemberUpToAge5(applicationData);
	    assertFalse(hasChild5OrUnder);
	}
	
	@Test
	public void testShowWicWithAnybodyInHouseholdPregnant() {
		
	    ApplicationData applicationData = new ApplicationData();
	    
	    //TODO create year more than 5 for test
	    
	    applicationData = new TestApplicationDataBuilder(applicationData)
		        .withPersonalInfo()
		        .withContactInfo()
		        .withApplicantPrograms(List.of("CCAP"))
		        .withSubworkflow("household",
		            new PagesDataBuilder().withPageData("householdMemberInfo", Map.of(
		                "firstName", "Jane",
		                "lastName", "Testerson",
		                "dateOfBirth", List.of("01", "09", "2016"))).build())
		        .withApplicantPrograms(List.of("SNAP"))
		        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
		        .withPageData("pregnant", "isPregnant", List.of("true"))
		        .build();
	    applicationData.setId("9870000123");
	    applicationData.setFlow(FlowType.FULL);
		boolean showWicMessage = wicRecommendationService.showWicMessage(applicationData);
		assertTrue(showWicMessage);
	}
	
	@Test
	public void testDoNotShowWicWithNobodyInHouseholdPregnant() {
		
	    ApplicationData applicationData = new ApplicationData();
    
	    applicationData = new TestApplicationDataBuilder(applicationData)
		        .withPersonalInfo()
		        .withContactInfo()
		        .withApplicantPrograms(List.of("CCAP"))
		        .withSubworkflow("household",
		            new PagesDataBuilder().withPageData("householdMemberInfo", Map.of(
		                "firstName", "Jane",
		                "lastName", "Testerson",
		                "dateOfBirth", List.of("01", "09", "2016"))).build())
		        .withApplicantPrograms(List.of("SNAP"))
		        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
		        .withPageData("pregnant", "isPregnant", List.of("false"))
		        .build();
	    applicationData.setId("9870000123");
	    applicationData.setFlow(FlowType.FULL);
		boolean showWicMessage = wicRecommendationService.showWicMessage(applicationData);
		assertFalse(showWicMessage);
	}
	
	@Test
	public void testDoNotShowWicWithNobodyUnder5() {
		//TODO identical to other test, 
	    ApplicationData applicationData = new ApplicationData();
    
	    applicationData = new TestApplicationDataBuilder(applicationData)
		        .withPersonalInfo()
		        .withContactInfo()
		        .withApplicantPrograms(List.of("CCAP"))
		        .withSubworkflow("household",
		            new PagesDataBuilder().withPageData("householdMemberInfo", Map.of(
		                "firstName", "Jane",
		                "lastName", "Testerson",
		                "dateOfBirth", List.of("01", "09", "2016"))).build())
		        .withApplicantPrograms(List.of("SNAP"))
		        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
		        //.withPageData("pregnant", "isPregnant", List.of("true"))
		        .build();
	    applicationData.setId("9870000123");
	    applicationData.setFlow(FlowType.FULL);
		boolean showWicMessage = wicRecommendationService.showWicMessage(applicationData);
		assertFalse(showWicMessage);
	}

}
