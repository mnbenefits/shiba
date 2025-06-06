package org.codeforamerica.shiba.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;
import java.util.Locale;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.events.PageEventPublisher;
import org.codeforamerica.shiba.testutilities.AbstractStaticMessageSourcePageTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;

@Import(SessionScopedApplicationDataTest.ApplicationDataCaptureController.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
    "pagesConfig=pages-config/test-landmark-pages.yaml"})
@Tag("framework")
public class SessionScopedApplicationDataTest extends AbstractStaticMessageSourcePageTest {

  private static ApplicationData applicationData;
  @MockitoBean
  private PageEventPublisher pageEventPublisher;

  @Override
  @BeforeEach
  protected void setUp() throws IOException {
    super.setUp();
    staticMessageSource.addMessage("landing.page-description", Locale.ENGLISH, "landingPageDescription");
    staticMessageSource.addMessage("first-page-title", Locale.ENGLISH, "first page title");
    staticMessageSource.addMessage("second-page-title", Locale.ENGLISH, "second page title");
    staticMessageSource.addMessage("third-page-title", Locale.ENGLISH, "third page title");
    staticMessageSource.addMessage("fourth-page-title", Locale.ENGLISH, "fourth page title");
    staticMessageSource.addMessage("language-and-accessibility.amharic", Locale.ENGLISH, "Amharic");
    staticMessageSource.addMessage("language-and-accessibility.arabic", Locale.ENGLISH, "Arabic");
    staticMessageSource.addMessage("language-and-accessibility.burmese", Locale.ENGLISH, "Burmese");
    staticMessageSource.addMessage("language-and-accessibility.cantonese", Locale.ENGLISH, "Cantonese");
    staticMessageSource.addMessage("language-and-accessibility.french", Locale.ENGLISH, "French");
    staticMessageSource.addMessage("language-and-accessibility.hmoob", Locale.ENGLISH, "Hmoob");
    staticMessageSource.addMessage("language-and-accessibility.karen", Locale.ENGLISH, "Karen");
    staticMessageSource.addMessage("language-and-accessibility.khmer", Locale.ENGLISH, "Khmer");
    staticMessageSource.addMessage("language-and-accessibility.korean", Locale.ENGLISH, "Korean");
    staticMessageSource.addMessage("language-and-accessibility.lao", Locale.ENGLISH, "Lao");
    staticMessageSource.addMessage("language-and-accessibility.oromo", Locale.ENGLISH, "Oromo");
    staticMessageSource.addMessage("language-and-accessibility.russian", Locale.ENGLISH, "Russian");
    staticMessageSource.addMessage("language-and-accessibility.soomaali", Locale.ENGLISH, "Soomaali");
    staticMessageSource.addMessage("language-and-accessibility.spanish", Locale.ENGLISH, "Spanish");
    staticMessageSource.addMessage("language-and-accessibility.vietnamese", Locale.ENGLISH, "Vietnamese");
  }

  @Test
  void shouldClearTheSessionWhenUserNavigatesToALandingPage() {
    navigateTo("testStaticLandingPage");

    testPage.clickCustomButton("Continue", 3, "second page title");
    testPage.enter("foo", "someInput");
    testPage.clickContinue("third page title");

    navigateTo("testStaticLandingPage");
    driver.navigate().to(baseUrl + "/captureApplicationDataFromSession");
    assertThat(SessionScopedApplicationDataTest.applicationData).isEqualTo(new ApplicationData());
  }

  @Controller
  static class ApplicationDataCaptureController {

    private final ApplicationData sessionScopedApplicationData;

    public ApplicationDataCaptureController(ApplicationData sessionScopedApplicationData) {
      this.sessionScopedApplicationData = sessionScopedApplicationData;
    }

    @GetMapping("/captureApplicationDataFromSession")
    String captureApplicationDataFromSession() {
      ApplicationData applicationDataClone = new ApplicationData();
      applicationDataClone.setPagesData(this.sessionScopedApplicationData.getPagesData());
      applicationDataClone.setSubworkflows(this.sessionScopedApplicationData.getSubworkflows());
      applicationDataClone
          .setIncompleteIterations(this.sessionScopedApplicationData.getIncompleteIterations());
      applicationDataClone.setId(this.sessionScopedApplicationData.getId());
      applicationDataClone.setStartTimeOnce(this.sessionScopedApplicationData.getStartTime());
      SessionScopedApplicationDataTest.applicationData = applicationDataClone;
      return "testTerminalPage";
    }
  }
}
