package org.codeforamerica.shiba.pages;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class childrenUnder19FlowTest extends AbstractShibaMockMvcTest{
	
	 @BeforeEach
	  protected void setUp() throws Exception {
	    super.setUp();
	    mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
	    postExpectingSuccess("identifyCountyBeforeApplying", "county", "Hennepin");
	    postExpectingSuccess("writtenLanguage", Map.of("writtenLanguage", List.of("ENGLISH")));
	    postExpectingSuccess("spokenLanguage", Map.of("spokenLanguage", List.of("ENGLISH")));
	  }
	 
	 @Test
	 void shouldNavigateToChildrenUnder19() throws Exception {
		 selectPrograms("CASH");
		 addHouseholdMembersWithProgram("CASH");
		 postExpectingRedirect("temporaryAbsence", "hasTemporaryAbsence", "true", "childrenUnder19");
	 }
	 
	 @Test 
	 void shouldNavigateToParentNotAtHome() throws Exception {
		 selectPrograms("CASH");
		 addHouseholdMembersWithProgram("CCAP");
		 postExpectingRedirect("temporaryAbsence", "hasTemporaryAbsence", "true", "parentNotAtHome");
	 }
	 
	 @Test
	 void shouldNotNavigateToChildrenUnder19() throws Exception {
		 selectPrograms("CASH");
		 postExpectingRedirect("temporaryAbsence", "hasTemporaryAbsence", "true", "introPersonalDetails");
	 }
	 
	 @Test
	 void shouldNavigateToPreparingMeals() throws Exception {
		 selectPrograms("CASH", "SNAP");
		 addHouseholdMembersWithProgram("CASH");
		 postExpectingRedirect("temporaryAbsence", "hasTemporaryAbsence", "true", "childrenUnder19");
		 postExpectingRedirect("childrenUnder19", "hasChildrenUnder19", "true", "parentNotAtHome");
		 postExpectingRedirect("parentNotAtHome", "hasParentNotAtHome", "true", "preparingMealsTogether");
	 }
	 
	 @Test
	 void shouldNavigateToHousingSubsidy() throws Exception {
		 selectPrograms("CASH");
		 addHouseholdMembersWithProgram("CASH");
		 postExpectingRedirect("temporaryAbsence", "hasTemporaryAbsence", "true", "childrenUnder19");
		 postExpectingRedirect("childrenUnder19", "hasChildrenUnder19", "true", "parentNotAtHome");
		 postExpectingRedirect("parentNotAtHome", "hasParentNotAtHome", "true", "housingSubsidy");
	 }
	 
	 @Test
	 void shouldNavigateToChildrenInNeedOfCare() throws Exception {
		 selectPrograms("CASH");
		 addHouseholdMembersWithProgram("CCAP");
		 postExpectingRedirect("temporaryAbsence", "hasTemporaryAbsence", "true", "parentNotAtHome");
		 postExpectingRedirect("parentNotAtHome", "hasParentNotAtHome", "true", "childrenInNeedOfCare");
	 }
	 
	 
	 


}
