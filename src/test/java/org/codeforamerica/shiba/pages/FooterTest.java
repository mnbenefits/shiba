package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.codeforamerica.shiba.testutilities.AbstractBasePageTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class FooterTest extends AbstractBasePageTest {

  @Override
  @BeforeEach
  protected void setUp() throws IOException {
    super.setUp();
  }

  /**
   * This test will verify that the tagline is present on the landing, FAQ, SNAP NDS and Privacy Policy pages.
   * This test will verify that the tagline is NOT present on (a subset) of other pages
   * @param title - expected page title
   * @param page - the page URL
   * @param shouldHaveTagline - indicates whether or not the page should have the tagline
   */
  @ParameterizedTest
  @CsvSource(value = {
			"MNbenefits, /, true",
			"Frequently Asked Questions, /faq, true",
			"SNAP NDS, /snapNDS, true",
			"Privacy Policy, /privacy, true",
			"Language and Accessibility, /languageAndAccessibility, false",
			"Identify County, /pages/identifyCountyBeforeApplying, false",
		    "Ready to upload documents, /pages/readyToUploadDocuments, false"
  })
  void footerIncludesTagline(String title, String page, String shouldHaveTagline) {
	    driver.navigate().to(baseUrl + page);
	    
	    assertEquals(title, driver.getTitle());
	    assertTrue(driver.findElement(By.id("footerMNbenefits")) != null);
	    if (Boolean.parseBoolean(shouldHaveTagline)) {
	    	assertTrue(driver.findElement(By.id("taglineMNbenefits")) != null);
	    } else {
	    	assertThrows(org.openqa.selenium.NoSuchElementException.class, ()->{driver.findElement(By.id("taglineMNbenefits"));} );
	    }
  }

  /**
   * This test will verify that the FAQ, SNAP NDS, Privacy Policy and Language and Accessibility links on the footer link to the correct pages
   * @param anchorId - HTML anchor element id attribute value
   * @param title - expected page title
   */
  // This test will verify that the tagline language links link to the Language and Accessibility page.
  @ParameterizedTest
  @CsvSource(value = {
			"link-faq, Frequently Asked Questions",
			"link-snap-nds, SNAP NDS",
			"link-privacy-policy, Privacy Policy",
			"link-language-and-accessibility, Language and Accessibility"
  })
  void footerLinksToInfoPages(String anchorId, String title) {
	    driver.navigate().to(baseUrl);
	    assertEquals("MNbenefits", driver.getTitle());
	    
	    WebElement anchorElement = driver.findElement(By.id(anchorId));
	    assertTrue(anchorElement != null);
	    
	    anchorElement.click();
		assertThat(driver.getTitle()).isEqualTo(title);
		driver.navigate().back();
		// should be back on the landing page
	    assertEquals("MNbenefits", driver.getTitle());
  }

  /**
   * This test will verify that the language links on the tagline all link to the Language and Accessibility page
   * @param anchorId - HTML anchor element id attribute value
   */
  @ParameterizedTest
  @CsvSource(value = {
			"link-am",
			"link-ar",
			"link-my",
			"link-zh",
			"link-fr",
			"link-hmn",
			"link-kar",
			"link-km",
			"link-ko",
			"link-lo",
			"link-om",
			"link-ru",
			"link-so",
			"link-es",
			"link-vi"
  })
  void taglineLinksToLanuageAndAccessibilityPage(String anchorId) {
	    driver.navigate().to(baseUrl);
	    assertEquals("MNbenefits", driver.getTitle());
	    assertTrue(driver.findElement(By.id("taglineMNbenefits")) != null);
	    
	    WebElement anchorElement = driver.findElement(By.id(anchorId));
	    assertTrue(anchorElement != null);
	    
	    anchorElement.click();
		assertThat(driver.getTitle()).isEqualTo("Language and Accessibility");
		driver.navigate().back();
		// should be back on the landing page
	    assertEquals("MNbenefits", driver.getTitle());
  }

  /**
   * This test will verify that the FAQ, SNAP NDS, Privacy Policy and Language and Accessibility links that are on the navigation header (on the info pages) 
   * will link to the correct pages.
   * @param anchorId - HTML anchor element id attribute value
   * @param title - expected page title
   */
  // This test will verify that the tagline language links link to the Language and Accessibility page.
  @ParameterizedTest
  @CsvSource(value = {
			"nav-faq, Frequently Asked Questions",
			"nav-snapnds, SNAP NDS",
			"nav-privacy, Privacy Policy",
			"nav-accessibility, Language and Accessibility"
  })
  void navLinksToInfoPages(String anchorId, String title) {
	    driver.navigate().to(baseUrl + "/languageAndAccessibility");
	    assertEquals("Language and Accessibility", driver.getTitle());
	    
	    WebElement anchorElement = driver.findElement(By.id(anchorId));
	    assertTrue(anchorElement != null);
	    
	    anchorElement.click();
		assertThat(driver.getTitle()).isEqualTo(title);
		driver.navigate().back();
		// should be back on the Language and Accessibility page or the landing page in the case where we
		// navigate to the Language and Accessibility page (i.e., the same page we were on).
		if (anchorId.equals("nav-accessibility")) {
			assertEquals("MNbenefits", driver.getTitle());
		} else {
			assertEquals("Language and Accessibility", driver.getTitle());
		}
  }
  
}