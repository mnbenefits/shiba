package org.codeforamerica.shiba.pages;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SocialWorkerNavigationTest extends AbstractShibaMockMvcTest{
	  @BeforeEach
	  protected void setUp() throws Exception {
	    super.setUp();
	    mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
	    postExpectingSuccess("identifyCountyBeforeApplying", "county", "Hennepin");
	    postExpectingSuccess("writtenLanguage", Map.of("writtenLanguage", List.of("ENGLISH")));
	    postExpectingSuccess("spokenLanguage", Map.of("spokenLanguage", List.of("ENGLISH")));
	  }
	  
	  @Test
	  void shouldNavigateToSocialWorker() throws Exception {
		  selectPrograms("SNAP");
		  postExpectingRedirect("healthcareCoverage", "healthcareCoverage", "false", "ebtInPast");
		  postExpectingRedirect("ebtInPast", "hadEBTInPast", "false", "socialWorker");
	  }
	  
	  @Test
	  void shouldNotNavigateToSocialWorker() throws Exception {
		  selectPrograms("CCAP");
		  postExpectingRedirect("healthcareCoverage", "healthcareCoverage", "false", "authorizedRep");
	  }
	  
	  @Test
	  void shouldNavigateToReferrals() throws Exception {
		  selectPrograms("SNAP");
		  postExpectingRedirect("socialWorker", "hasSocialWorker", "false", "referrals");
	  }
}
