package org.codeforamerica.shiba.framework;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.County.Hennepin;
import static org.codeforamerica.shiba.pages.Sentiment.HAPPY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.pages.events.ApplicationSubmittedListener;
import org.codeforamerica.shiba.testutilities.AbstractStaticMessageSourceFrameworkTest;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {"pagesConfig=pages-config/test-submit-page.yaml"})
public class SubmissionAndTerminalPageTest extends AbstractStaticMessageSourceFrameworkTest {

  @MockitoBean
  private ApplicationSubmittedListener applicationSubmittedListener;
  @MockitoBean
  private ApplicationRepository applicationRepository;

  @Test
  void shouldProvideApplicationDataToTerminalPageWhenApplicationIsSigned() throws Exception {
    var applicationId = "someId";
    var county = Hennepin;
    new TestApplicationDataBuilder(applicationData)
        .withApplicantPrograms(List.of("SNAP", "CCAP"))
        .withPageData("identifyCounty", "county", county.name())
        .build();
    var sentiment = HAPPY;
    var feedbackText = "someFeedback";
    Application application = Application.builder()
        .id(applicationId)
        .completedAt(ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 11, 10), UTC))
        .applicationData(applicationData)
        .county(county)
        .timeToComplete(Duration.ofSeconds(124))
        .sentiment(sentiment)
        .feedback(feedbackText)
        .build();
    when(applicationRepository.find(any())).thenReturn(application);
    when(applicationRepository.getNextId()).thenReturn(applicationId);

    assertThat(getFormPage("firstPage").getInputByName("foo")).isNotNull();
    postToUrlExpectingSuccess("/submit",
        "/pages/firstPage/navigation",
        Map.of("foo", List.of("some value")));
    
    when(featureFlagConfiguration.get("show-wic-recommendation")).thenReturn(FeatureFlag.ON);
    var testTerminalPage = getNextPageAsFormPage("firstPage");
    assertThat(testTerminalPage.getElementTextById("submission-time"))
        .isEqualTo("2020-01-01T05:10-06:00[America/Chicago]");
    assertThat(testTerminalPage.getElementTextById("application-id")).isEqualTo(applicationId);
    assertThat(testTerminalPage.getElementTextById("county")).isEqualTo(county.name());
    assertThat(testTerminalPage.getElementTextById("feedback-text")).isEqualTo(feedbackText);
    assertThat(testTerminalPage.getElementTextById("CAF")).contains("CAF");
    assertThat(testTerminalPage.getElementTextById("CCAP")).contains("CCAP");
  }
}
