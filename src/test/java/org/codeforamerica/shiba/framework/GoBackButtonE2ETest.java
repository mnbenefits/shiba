package org.codeforamerica.shiba.framework;

import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;
import java.util.Locale;

import org.codeforamerica.shiba.testutilities.AbstractExistingStartTimePageTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
    "pagesConfig=pages-config/test-conditional-rendering.yaml"})
@Tag("framework")
public class GoBackButtonE2ETest extends AbstractExistingStartTimePageTest {

  private final String firstPageTitle = "firstPageTitle";
  private final String secondPageTitle = "secondPageTitle";
  private final String thirdPageTitle = "thirdPageTitle";

  @BeforeEach
  protected void setUp() throws IOException {
    super.setUp();
    staticMessageSource.addMessage("landing.page-description", ENGLISH, "landingPageDescription");
    staticMessageSource.addMessage("starting-page-title", ENGLISH, "starting page");
    staticMessageSource.addMessage("first-page-title", ENGLISH, firstPageTitle);
    staticMessageSource.addMessage("second-page-title", ENGLISH, secondPageTitle);
    staticMessageSource.addMessage("third-page-title", ENGLISH, thirdPageTitle);
    staticMessageSource.addMessage("fourth-page-title", ENGLISH, "fourthPageTitle");
    staticMessageSource.addMessage("eighth-page-title", ENGLISH, "eighthPageTitle");
    staticMessageSource.addMessage("ninth-page-title", ENGLISH, "ninthPageTitle");
    staticMessageSource.addMessage("skip-message-key", ENGLISH, "SKIP PAGE");
    staticMessageSource.addMessage("not-skip-message-key", ENGLISH, "NOT SKIP PAGE");
    staticMessageSource.addMessage("page-to-skip-title", ENGLISH, "pageToSkip");
    staticMessageSource.addMessage("last-page-title", ENGLISH, "lastPageTitle");
    staticMessageSource.addMessage("generic.footer.about", Locale.ENGLISH, "About");
    staticMessageSource.addMessage("generic.footer.help-and-resources", Locale.ENGLISH, "Help & Resources");
    staticMessageSource.addMessage("generic.footer.mn-official-website", Locale.ENGLISH, "State of Minnesota Official Website");
    staticMessageSource.addMessage("language-and-accessibility.language-and-accessibility", Locale.ENGLISH, "Language and Accessibility");
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
  void shouldBeAbleToNavigateBackMoreThanOnePage() {
    // should be able to navigate back more than one page
    driver.navigate().to(baseUrl + "/pages/firstPage");
    testPage.clickContinue("secondPageTitle");
    assertThat(driver.getTitle()).isEqualTo(secondPageTitle);
    testPage.clickContinue("thirdPageTitle");
    assertThat(driver.getTitle()).isEqualTo(thirdPageTitle);
    testPage.goBack();
    testPage.goBack();
    assertThat(driver.getTitle()).isEqualTo(firstPageTitle);

    // should skip going backwards over skip condition pages
    testPage.enter("someRadioInputName", "SKIP PAGE");
    testPage.clickContinue("thirdPageTitle");
    assertThat(driver.getTitle()).isEqualTo(thirdPageTitle);
    testPage.goBack();
    assertThat(driver.getTitle()).isEqualTo(firstPageTitle);
  }
}
