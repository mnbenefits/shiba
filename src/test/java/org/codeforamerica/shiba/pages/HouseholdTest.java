package org.codeforamerica.shiba.pages;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.testutilities.AbstractPageControllerTest;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class HouseholdTest extends AbstractPageControllerTest {
  @MockitoBean
  private WicRecommendationService wicRecommendationService;
	  
  @Test
  void shouldRedirectToHouseholdListWhenDeletingNonExistentHouseholdMember() throws Exception {
    makeApplicationWithTwoHouseholdMembers();

    MockHttpSession session = new MockHttpSession();

    //Delete the second household member
    String iterationIndex = "1";
    mockMvc.perform(get("/pages/householdDeleteWarningPage").param("iterationIndex", iterationIndex)
            .session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Second HouseholdMember")));

    mockMvc.perform(
            post("/groups/household/" + iterationIndex + "/delete").with(csrf()).session(session))
        .andExpect(redirectedUrl("/pages/householdList"));

    mockMvc.perform(get("/pages/householdList").session(session))
        .andExpect(status().isOk());

    // When we "hit back", we should be redirected to the household list
    mockMvc.perform(get("/pages/householdDeleteWarningPage").param("iterationIndex", iterationIndex)
            .session(session))
        .andExpect(redirectedUrl("/pages/householdList"));
  }

  private void makeApplicationWithTwoHouseholdMembers() {
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

    new TestApplicationDataBuilder(applicationData)
        .withPageData("personalInfo", "firstName", "The")
        .withPageData("personalInfo", "lastName", "Applicant")
        .withSubworkflow("household", new PagesDataBuilder()
            .withPageData("householdMemberInfo", Map.of(
                "programs", List.of("SNAP", "CCAP"),
                "firstName", List.of("First"),
                "lastName", List.of("HouseholdMember")
            )), new PagesDataBuilder()
            .withPageData("householdMemberInfo", Map.of(
                "programs", List.of("SNAP"),
                "firstName", List.of("Second"),
                "lastName", List.of("HouseholdMember")
            ))
        );
  }
}
