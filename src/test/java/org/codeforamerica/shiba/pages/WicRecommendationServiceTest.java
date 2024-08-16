package org.codeforamerica.shiba.pages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.pages.config.FeatureFlagConfiguration;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

public class WicRecommendationServiceTest {
	
	private FeatureFlagConfiguration featureFlagConfiguration = new FeatureFlagConfiguration(Map.of("show-wic-recommendation", FeatureFlag.ON));
	private WicRecommendationService wicRecommendationService = new WicRecommendationService(featureFlagConfiguration);
    ZonedDateTime now = ZonedDateTime.now();

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
		
	    ApplicationData applicationData = new ApplicationData();
	    String date = String.valueOf(now.getDayOfMonth()+1); 
	    String year = String.valueOf(now.getYear()-5); 
	    String month = String.valueOf(now.getMonthValue());
	    
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
	    ApplicationData applicationData = new ApplicationData();
	    String date = String.valueOf(now.getDayOfMonth()-1); 
	    String year = String.valueOf(now.getYear()-5); 
	    String month = String.valueOf(now.getMonthValue());
	    
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
		String date = String.valueOf(now.getDayOfMonth() - 1);
		String year = String.valueOf(now.getYear() - 5);
		String month = String.valueOf(now.getMonthValue());

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
		boolean showWicMessage = wicRecommendationService.showWicMessage(applicationData);
		assertFalse(showWicMessage);
	}

}
