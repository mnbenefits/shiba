package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.codeforamerica.shiba.testutilities.AbstractBasePageTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class LocalePreferenceSelectionTest extends AbstractBasePageTest {

  @Override
  @BeforeEach
  protected void setUp() throws IOException {
    super.setUp();
    driver.navigate().to(baseUrl);
  }

  @AfterEach
  void tearDown() {
    testPage.selectFromDropdown("locales", "English");
  }

  @Test
  void noDefaultWrittenLanguageSelection() {
	testPage.clickButtonLink("Apply now", "Identify County");
    testPage.enter("county", "Hennepin");
    testPage.clickContinue("Prepare To Apply");
    testPage.selectFromDropdown("locales", "Español");
    assertThat(driver.findElements(By.tagName("h1")).get(0).getText()).isEqualTo("Como funciona");

    testPage.clickCustomButton("Continuar", 3, "Aviso de tiempo de espera");
    testPage.clickButtonLink("Continuar", "Preferencia de idioma – Escrito");
    // Verify that there is no default writtenLanguage selection.
    String selectedOption = testPage.getRadioValue("writtenLanguage");
    assertTrue(selectedOption==null);
  }

  
  @Test
  void selectingSpanishForWrittenLanguagePreferenceDoesNotChangeFlowLocaleToSpanish() {
	testPage.clickButtonLink("Apply now", "Identify County");
	testPage.enter("county", "Hennepin");
	testPage.clickContinue("Prepare To Apply");
    testPage.clickButtonLink("Continue", "Timeout notice");
    testPage.clickButtonLink("Continue", "Language Preferences - Written");
    testPage.enter("writtenLanguage", "Español");
    testPage.clickButton("Continue", "Language Preferences - Spoken");
    assertThat(driver.getTitle()).isEqualTo("Language Preferences - Spoken");
    
    WebElement selectedOption = testPage.getSelectedOption("locales");
    assertThat(selectedOption.getText()).isEqualTo("English");
  }
}
