package org.codeforamerica.shiba.journeys;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.testutilities.PercyTestPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@Tag("certainPopsPilotCountyTest")
public class CertainPopsPilotCountyTest extends JourneyTest {

  protected void initTestPage() {
    testPage = new PercyTestPage(driver);
  }

  @Test
  void countyWithoutHealthCareProgramListed() {
    when(clock.instant()).thenReturn(
        LocalDateTime.of(2020, 1, 1, 10, 10).atOffset(ZoneOffset.UTC).toInstant(),
        LocalDateTime.of(2020, 1, 1, 10, 15, 30).atOffset(ZoneOffset.UTC).toInstant()
    );
    when(featureFlagConfiguration.get("certain-pops")).thenReturn(FeatureFlag.ON);

    // Assert intercom button is present on landing page
    await().atMost(5, SECONDS).until(() -> !driver.findElements(By.id("intercom-frame")).isEmpty());
    assertThat(driver.findElement(By.id("intercom-frame"))).isNotNull();
    assertThat(driver.findElement(By.id("generalNotice"))).isNotNull();
    // Assert that the EBT Scam Alert is displayed on the landing page.
    assertThat(driver.findElement(By.id("ebt-scam-alert"))).isNotNull();

   
 // Landing page
    testPage.clickButtonLink("Apply now", "Identify County");

    // Select other than pilot county
    testPage.enter("county", "Hennepin");
    testPage.clickContinue("Prepare To Apply");

    // Informational pages
    testPage.clickButtonLink("Continue","Timeout notice");
    testPage.clickButtonLink("Continue", "Language Preferences - Written");

    // Written Language Preferences
    testPage.enter("writtenLanguage", "English");
    testPage.clickContinue("Language Preferences - Spoken");
    // Spoken Language Preferences
    testPage.enter("spokenLanguage", "English");
    testPage.enter("needInterpreter", "Yes");
    testPage.clickContinue("Choose Programs");
    // Program Selection
    assertThat(testPage.getCheckboxDisplays("programs")).doesNotContain(PROGRAM_CERTAIN_POPS);
    
    
  }  
  
  @Test
  void countyWithHealthCareProgramListed() {
    when(clock.instant()).thenReturn(
        LocalDateTime.of(2020, 1, 1, 10, 10).atOffset(ZoneOffset.UTC).toInstant(),
        LocalDateTime.of(2020, 1, 1, 10, 15, 30).atOffset(ZoneOffset.UTC).toInstant()
    );
    when(featureFlagConfiguration.get("certain-pops")).thenReturn(FeatureFlag.ON);

    // Assert intercom button is present on landing page
    await().atMost(5, SECONDS).until(() -> !driver.findElements(By.id("intercom-frame")).isEmpty());
    assertThat(driver.findElement(By.id("intercom-frame"))).isNotNull();
    assertThat(driver.findElement(By.id("generalNotice"))).isNotNull();
    // Assert that the EBT Scam Alert is displayed on the landing page.
    assertThat(driver.findElement(By.id("ebt-scam-alert"))).isNotNull();

   
 // Landing page
    testPage.clickButtonLink("Apply now", "Identify County");

    // Select pilot county
    testPage.enter("county", "Chisago");
    testPage.clickContinue("Prepare To Apply");

    // Informational pages
    testPage.clickButtonLink("Continue","Timeout notice");
    testPage.clickButtonLink("Continue", "Language Preferences - Written");

    // Language Preferences
    testPage.enter("writtenLanguage", "English");
    testPage.clickContinue("Language Preferences - Spoken");
    // Spoken Language Preferences
    testPage.enter("spokenLanguage", "English");
    testPage.enter("needInterpreter", "Yes");
    testPage.clickContinue("Choose Programs");
       // Program Selection
    assertThat(testPage.getCheckboxDisplays("programs")).contains(PROGRAM_CERTAIN_POPS);
    
    
  }
}
