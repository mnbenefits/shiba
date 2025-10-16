package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.pages.data.Iteration;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.Subworkflow;
import org.codeforamerica.shiba.testutilities.AbstractPageControllerTest;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

public class ChildCareTest extends AbstractPageControllerTest {
  @MockitoBean
  private WicRecommendationService wicRecommendationService;

  /**
   * This test validates that the whoNeedsChildCareForMentalHealth page is being generated correctly.  
   * More specifically, that the correct checkboxes are being put on the page when the application that includes
   * an applicant, spouse and child.
   * Current implementation excludes those individuals, other than the applicant, that are selected on the the 
   * childrenInNeedOfCare page.
   * @throws Exception
   */
  @Test
  void testPageWhoNeedsChildCareForHealthCare() throws Exception {
    MockHttpSession session = new MockHttpSession();

    applicationData.setStartTimeOnce(Instant.now());

    var id = "some-id";
    applicationData.setId(id);
    Application application = Application.builder()
        .id(id)
        .county(County.Hennepin)
        .timeToComplete(Duration.ofSeconds(12415))
        .completedAt(ZonedDateTime.now(ZoneOffset.UTC))
        .applicationData(applicationData)
        .build();
    
    when(applicationRepository.find(any())).thenReturn(application);

    TestApplicationDataBuilder applicationDataBuilder = new TestApplicationDataBuilder(applicationData)
        .withPageData("choosePrograms","programs", List.of("CCAP"))
        // add applicant info
        .withPageData("personalInfo", "firstName", "John")
        .withPageData("personalInfo", "lastName", "Doe")
        .withPageData("addHouseholdMembers", "addHouseholdMembers","true")
        .withSubworkflow("household", new PagesDataBuilder()
        	// add the spouse info
            .withPageData("householdMemberInfo", Map.of(
                "programs", List.of("CCAP"),
                "firstName", List.of("Jane"),
                "lastName", List.of("Doe")
            )), new PagesDataBuilder()
            // add the child info
            .withPageData("householdMemberInfo", Map.of(
                "programs", List.of("NONE"),
                "firstName", List.of("Jim"),
                "lastName", List.of("Doe")
            )))
        // this test expects the "Yes" response on the childCareMentalHealth page
        .withPageData("childCareMentalHealth", "childCareMentalHealth", "true");
    
    // Build a HashMap that maps each person's first name to their full name with id
    HashMap<String,String> personMap = new HashMap<String,String>();
    // Add the applicant to the HashMap
    personMap.put("John", "John Doe applicant");
    
    // Add each household member to the HashMap.
    Subworkflow householdSubworkflow = applicationData.getSubworkflows().get("household");
    for (Iteration iteration : householdSubworkflow) {
    	PageData householdMemberInfoPage = iteration.getPagesData().getPage("householdMemberInfo");
    	String firstName = householdMemberInfoPage.get("firstName").getValue(0);
    	String lastName = householdMemberInfoPage.get("lastName").getValue(0);
        String fullNameWithId = String.format("%s %s %s", firstName, lastName, iteration.getId());
    	personMap.put(firstName, fullNameWithId);
    }

    // Case #1: The child, Jim, is selected on the childrenInNeedOfCare page
    applicationDataBuilder
        .withPageData("childrenInNeedOfCare", "whoNeedsChildCare", List.of(personMap.get("Jim")));
    
    // GET the childrenInNeedOfCare page
    ResultActions results = mockMvc.perform(get("/pages/whoNeedsChildCareForMentalHealth").with(csrf()).session(session))
        .andExpect(status().isOk());
    Document htmlDocument = Jsoup.parse(results.andReturn().getResponse().getContentAsString());
    Elements checkboxes = htmlDocument.getElementsByAttributeValue("type", "checkbox");
    assertThat(checkboxes.size()).isEqualTo(2);
    List<String> values = checkboxes.eachAttr("value");
    assertThat(values).contains(personMap.get("John"));
    assertThat(values).contains(personMap.get("Jane"));
    
    // Case #2: The spouse, Jane, is selected on the childrenInNeedOfCare page
    applicationDataBuilder
        .withPageData("childrenInNeedOfCare", "whoNeedsChildCare", List.of(personMap.get("Jane")));

    // GET the childrenInNeedOfCare page
    results = mockMvc.perform(get("/pages/whoNeedsChildCareForMentalHealth").with(csrf()).session(session))
        .andExpect(status().isOk());
    htmlDocument = Jsoup.parse(results.andReturn().getResponse().getContentAsString());
    checkboxes = htmlDocument.getElementsByAttributeValue("type", "checkbox");
    assertThat(checkboxes.size()).isEqualTo(2);
    values = checkboxes.eachAttr("value");
    assertThat(values).contains(personMap.get("John"));
    assertThat(values).contains(personMap.get("Jim"));
    
    // Case #3: The applicant, John, is selected on the childrenInNeedOfCare page
    applicationDataBuilder
        .withPageData("childrenInNeedOfCare", "whoNeedsChildCare", List.of(personMap.get("John")));

    // GET the childrenInNeedOfCare page
    results = mockMvc.perform(get("/pages/whoNeedsChildCareForMentalHealth").with(csrf()).session(session))
        .andExpect(status().isOk());
    htmlDocument = Jsoup.parse(results.andReturn().getResponse().getContentAsString());
    checkboxes = htmlDocument.getElementsByAttributeValue("type", "checkbox");
    assertThat(checkboxes.size()).isEqualTo(3);
    values = checkboxes.eachAttr("value");
    assertThat(values).contains(personMap.get("John"));
    assertThat(values).contains(personMap.get("Jane"));
    assertThat(values).contains(personMap.get("Jim"));

    // With applicant and spouse selected as needing child care for mental health,
    // GET the childCareMentalHealthTime page and verify that the applicant and spouse are the only input options.
    applicationDataBuilder
        .withPageData("whoNeedsChildCareForMentalHealth", "whoNeedsChildCareMentalHealth", List.of(personMap.get("John"), personMap.get("Jane")));

    // GET the childCareMentalHealthTime page
    results = mockMvc.perform(get("/pages/childCareMentalHealthTimes").with(csrf()).session(session))
        .andExpect(status().isOk());
    htmlDocument = Jsoup.parse(results.andReturn().getResponse().getContentAsString());
    Elements labels = htmlDocument.getElementsByTag("label");
    values = labels.eachText();
    assertThat(values).contains("John Doe (you)");
    assertThat(values).contains("Jane Doe");
    assertThat(values).doesNotContain("Jim Doe");
  }
}
