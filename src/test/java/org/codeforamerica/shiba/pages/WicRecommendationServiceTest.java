package org.codeforamerica.shiba.pages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

public class WicRecommendationServiceTest {
	

	private WicRecommendationService wicRecommendationService = new WicRecommendationService(List.of("Anoka", "Carver"));
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime fiveYearsBefore = now.minusYears(5);    

	@Test
	public void isAnybodyInHouseholdPregnant() {
		
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
		        .withPageData("pregnant", "isPregnant", List.of("true"))
		        .build();
	    applicationData.setId("9870000123");
	    applicationData.setFlow(FlowType.FULL);
		boolean somebodyIsPregnant = wicRecommendationService.hasPregnantHouseholdMember(applicationData);
		assertTrue(somebodyIsPregnant);
	}

	
	@Test
	public void testChildLessThan5() {
		ZonedDateTime fiveYearsLessOneDayBefore = fiveYearsBefore.plusDays(1);
	    ApplicationData applicationData = new ApplicationData();
	    String month = String.valueOf(fiveYearsLessOneDayBefore.getMonthValue());
	    String date = String.valueOf(fiveYearsLessOneDayBefore.getDayOfMonth());
	    String year = String.valueOf(fiveYearsLessOneDayBefore.getYear());
	    applicationData = new TestApplicationDataBuilder(applicationData)
		        .withPersonalInfo()
		        .withContactInfo()
		        .withApplicantPrograms(List.of("CCAP"))
		        .withSubworkflow("household",
		            new PagesDataBuilder().withPageData("householdMemberInfo", Map.of(
		                "firstName", "Jane",
		                "lastName", "Testerson",
		                "dateOfBirth", List.of(month, date, year))).build())
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
		ZonedDateTime fiveYearsOneDayBefore = fiveYearsBefore.minusDays(1);
	    ApplicationData applicationData = new ApplicationData();
	    String month = String.valueOf(fiveYearsOneDayBefore.getMonthValue());
	    String date = String.valueOf(fiveYearsOneDayBefore.getDayOfMonth());
	    String year = String.valueOf(fiveYearsOneDayBefore.getYear());
	    
	    applicationData = new TestApplicationDataBuilder(applicationData)
		        .withPersonalInfo()
		        .withContactInfo()
		        .withApplicantPrograms(List.of("CCAP"))
		        .withSubworkflow("household",
		            new PagesDataBuilder().withPageData("householdMemberInfo", Map.of(
		                "firstName", "Jane",
		                "lastName", "Testerson",
		                "dateOfBirth", List.of(month, date, year))).build())
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
	    applicationData.setOriginalCounty("Anoka");
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
	    applicationData.setOriginalCounty("Anoka");
		boolean showWicMessage = wicRecommendationService.showWicMessage(applicationData);
		assertFalse(showWicMessage);
	}
	
	@Test
	public void testDoNotShowWicWithNobodyUnder5() {
		ZonedDateTime fiveYearsOneDayBefore = fiveYearsBefore.minusDays(1);
		String month = String.valueOf(fiveYearsOneDayBefore.getMonthValue());
	    String date = String.valueOf(fiveYearsOneDayBefore.getDayOfMonth());
	    String year = String.valueOf(fiveYearsOneDayBefore.getYear());
	   

		ApplicationData applicationData = new ApplicationData();

		applicationData = new TestApplicationDataBuilder(applicationData).withPersonalInfo().withContactInfo()
				.withApplicantPrograms(List.of("CCAP"))
				.withSubworkflow("household",
						new PagesDataBuilder().withPageData("householdMemberInfo",
								Map.of("firstName", "Jane", "lastName", "Testerson", "dateOfBirth",
										List.of(month, date, year)))
								.build())
				.withApplicantPrograms(List.of("SNAP"))
				.withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
				// .withPageData("pregnant", "isPregnant", List.of("true"))
				.build();
		applicationData.setId("9870000123");
		applicationData.setFlow(FlowType.FULL);
	    applicationData.setOriginalCounty("Anoka");
		boolean showWicMessage = wicRecommendationService.showWicMessage(applicationData);
		assertFalse(showWicMessage);
	}
	
	@Test
	public void testWithOutAnyHousholdMembersAndNobodyIsPregnant() {
	    ApplicationData applicationData = new ApplicationData();
    
	    applicationData = new TestApplicationDataBuilder(applicationData)
		        .withPersonalInfo()
		        .withContactInfo()
		        .withApplicantPrograms(List.of("CCAP"))
		        .withApplicantPrograms(List.of("SNAP"))
		        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
		        .withPageData("pregnant", "isPregnant", List.of("false"))
		        .build();
	    applicationData.setId("9870000123");
	    applicationData.setFlow(FlowType.FULL);
	    applicationData.setOriginalCounty("Anoka");
		boolean showWicMessage = wicRecommendationService.showWicMessage(applicationData);
		assertFalse(showWicMessage);
	}

    //Assert that method showMessage returns false when even though the household has a pregnant member,
	//the county is not in the wic-pilot-counties list.
	@Test
	public void doNotShowWicWithAnybodyInHouseholdPregnantButNotPilotCounty() {
		
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
		        .withPageData("pregnant", "isPregnant", List.of("true"))
		        .build();
	    applicationData.setId("9870000123");
	    applicationData.setFlow(FlowType.FULL);
	    applicationData.setOriginalCounty("Clay"); // not a pilot county
		boolean showWicMessage = wicRecommendationService.showWicMessage(applicationData);
		assertFalse(showWicMessage);
	}

}
