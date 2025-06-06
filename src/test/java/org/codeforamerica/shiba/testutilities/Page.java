package org.codeforamerica.shiba.testutilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.codeforamerica.shiba.pages.Sentiment;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Page {

  protected final RemoteWebDriver driver;


  public Page(RemoteWebDriver driver) {
    this.driver = driver;
  }

  public String getTitle() {
    return driver.getTitle();
  }

  /*
   *  The purpose of this method is to verify that the message keys (i.e., keys in messages.properties)
   *  that are being referenced from this page are valid. When a page references a message key that is
   *  is not defined in messages.properties it will insert a string in the format ??key??.  For example:
   *  <h1 id="page-header" class="h2">??identify-county.select-your-countyx_en??</h1>
   */
  private void checkForBadMessageKeys() {
	assertThat(getTitle()).doesNotContain("??");
	String pageSource = driver.getPageSource();
	// Checking the page for the existence of an <html> tag provides some validity.
	// If it does we will assume that it has the matching end tag.
	int htmlStart = pageSource.indexOf("<html");
	assertThat(htmlStart).isGreaterThanOrEqualTo(0);
	// Does the <html> contain a substring that fits the pattern "??<message-key>??"
	String badMessageKey = StringUtils.substringBetween(pageSource, "??");
	assertThat(badMessageKey).isNull();
  }

  public String getHeader() {
    return driver.findElement(By.tagName("h1")).getText();
  }
  
 
  /**
   * Click the Go Back link and wait for the previous page to load and Go Back link to be clickable.
   */
  public void goBack() {
    driver.findElement(By.partialLinkText("Go Back")).click();
	Duration duration = Duration.of(5, ChronoUnit.SECONDS);
	WebDriverWait wait = new WebDriverWait(driver, duration);
	wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Go Back")));
  }
  
  /**
   * Use this method for tests that go back to the terminal page.
   * The terminal page has no Go Back link.
   * @param terminalPage
   */
  public void goBackToTerminalPage(String terminalPage) {
	    driver.findElement(By.partialLinkText("Go Back")).click();
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.titleContains(terminalPage));
	  }

  /**
   * Use clickLink(String linkText, String nextPage)
   * @param linkText
   */
  @Deprecated()
  public void clickLink(String linkText) {
    checkForBadMessageKeys();
    driver.findElement(By.linkText(linkText)).click(); 
  }
  
  /**
   * Same functionality as deprecated method clickLink, but only to be used for external links.
   * @param linkText
   */
  public void clickLinkToExternalWebsite(String linkText) {
	    checkForBadMessageKeys();
	    driver.findElement(By.linkText(linkText)).click(); 
	  }
  
  /**
   * Click link, then waits for the next page to load.
   * @param linkText
   * @param nextPage
   */
  public void clickLink(String linkText, String nextPage) {
	    checkForBadMessageKeys();
	    driver.findElement(By.linkText(linkText)).click();
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.titleContains(nextPage));
	  }


  /**
   * Click button with sendKeys() method instead of click method.<br>
   * Will retry if stale element exception occurs. <br>
   * Waits until next page is loaded.
   * @param buttonText
   * @param retryCount
   * @param nextPage
   */
  public void clickButtonWithRetry(String buttonText, int retryCount, String nextPage) {
	try {  
	    checkForBadMessageKeys();
	    WebElement buttonToClick = driver.findElements(By.id("form-submit-button")).stream()
		        .filter(button -> button.getText().contains(buttonText))
		        .findFirst()
		        .orElseThrow(() -> new RuntimeException("No button found containing text: " + buttonText));

	    // Instead of click(), sendKeys(Keys.RETURN) is supposed to be more reliable. This works too: .sendKeys(Keys.ENTER); 
		    buttonToClick.sendKeys(Keys.RETURN);
	    
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.titleContains(nextPage));
	} catch(StaleElementReferenceException e) {
		if (retryCount > 0) { // try again...
			this.clickButtonWithRetry(buttonText, retryCount-1, nextPage);
		} else { // we tried... but we can't ignore the exception
			throw e;
		}
	}
  }
  
  /**
   * This is for custom pages with submit buttons that DO NOT have the id = "form-submit-button".
   * @param buttonText
   * @param retryCount
   * @param nextPage
   */
  public void clickCustomButton(String buttonText, int retryCount, String nextPage) {
	try {  
	    checkForBadMessageKeys();
	    WebElement buttonToClick = driver.findElements(By.className("button")).stream()
	        .filter(button -> button.getText().contains(buttonText))
	        .findFirst()
	        .orElseThrow(() -> new RuntimeException("No button found containing text: " + buttonText));
	    buttonToClick.sendKeys(Keys.RETURN); 
	    
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.titleContains(nextPage));
	} catch(StaleElementReferenceException e) {
		if (retryCount > 0) { // try again...
			this.clickCustomButton(buttonText, retryCount-1, nextPage);
		} else { // we tried... but we can't ignore the exception
			throw e;
		}
	}
  }

	 /**
	  * For links that look like buttons. Old original method without the wait.<br>
	  * Use clickButtonLink(String buttonText, String nextPageTitle).
	  * @param buttonLinkText
	  */
  @Deprecated
  public void clickButtonLink(String buttonLinkText) {
    checkForBadMessageKeys();
    WebElement buttonToClick = driver.findElements(By.className("button--link")).stream()
        .filter(button -> button.getText().contains(buttonLinkText))
        .findFirst()
        .orElseThrow(
            () -> new RuntimeException("No button link found containing text: " + buttonLinkText));
    buttonToClick.sendKeys(Keys.RETURN);
  }
  
  /**
   * This is for buttons that look like links on the web page. They use the "button--link" class to appear as links.
   * @param buttonLinkText
   * @param nextPage
   */
  public void clickCustomLink(String buttonLinkText, String nextPage) {
	    checkForBadMessageKeys();
	    WebElement buttonToClick = driver.findElements(By.className("button--link")).stream()
	        .filter(button -> button.getText().contains(buttonLinkText))
	        .findFirst()
	        .orElseThrow(
	            () -> new RuntimeException("No link found containing text: " + buttonLinkText));
	    buttonToClick.sendKeys(Keys.RETURN);
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.titleContains(nextPage));
	  }
  
	public void clickAccordianButton(String buttonattributeText) {
		checkForBadMessageKeys();
		WebElement buttonToClick = driver
				.findElement(By.xpath("//button[@aria-controls=\"" + buttonattributeText + "\"]"));
		buttonToClick.sendKeys(Keys.RETURN); 
	}

	@Deprecated
  public void clickContinue() {
    clickButton("Continue");
  }
	
	/**
	 * Use clickButtonLink(String buttonText, String nextPageTitle)
	 * @param buttonText
	 */
	@Deprecated
	public void clickButton(String buttonText) {
		checkForBadMessageKeys();
		WebElement buttonToClick = driver.findElements(By.className("button")).stream()
				.filter(button -> button.getText().contains(buttonText)).findFirst()
				.orElseThrow(() -> new RuntimeException("No button found containing text: " + buttonText));
		buttonToClick.sendKeys(Keys.RETURN); 
	}
  
  /**
   * Click the Continue button, then wait for the next page to load.
   * @param nextPage
   */
  public void clickContinue(String nextPage) {
	  clickButton("Continue", nextPage);
	}
  
	  /**
	   * This is for links (anchors) that look like buttons.
	   * Click the button/link, then wait for the next page to load.
	   * @param buttonText
	   * @param nextPageTitle
	   */
	public void clickButtonLink(String buttonText, String nextPageTitle) {
		checkForBadMessageKeys();
		WebElement buttonToClick = driver.findElements(By.className("button")).stream()
				.filter(button -> button.getText().contains(buttonText)).findFirst()
				.orElseThrow(() -> new RuntimeException("No button found containing text: " + buttonText));
		buttonToClick.sendKeys(Keys.ENTER);
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.titleContains(nextPageTitle));
	}
	
	/**
	 * This is for clicking subtle links, anchors that use the "link--subtle" class.
	 * @param buttonText
	 * @param nextPageTitle
	 */
	public void clickSubtleLink(String buttonText, String nextPageTitle) {
		checkForBadMessageKeys();
		WebElement buttonToClick = driver.findElements(By.className("link--subtle")).stream()
				.filter(button -> button.getText().contains(buttonText)).findFirst()
				.orElseThrow(() -> new RuntimeException("No button found containing text: " + buttonText));
		buttonToClick.sendKeys(Keys.ENTER);
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.titleContains(nextPageTitle));
	}

	  /**
	   * Click form submit button, then waits for the next page to load.<br>
	   * This method is to be used on pages created by the framework where the submit button has<br>
	   * the id "form-submit-button".
	   * @param buttonText
	   * @param nextPage
	   */
	public void clickButton(String buttonText, String nextPage) {
		checkForBadMessageKeys();
		WebElement buttonToClick = driver.findElements(By.id("form-submit-button")).stream()
				.filter(button -> button.getText().contains(buttonText)).findFirst()
				.orElseThrow(() -> new RuntimeException("No button found containing text: " + buttonText));
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.elementToBeClickable(buttonToClick));
		buttonToClick.sendKeys(Keys.ENTER);
		wait.until(ExpectedConditions.titleContains(nextPage));
	}
	
 
  
	public void enter(String inputName, String value) {
		checkForBadMessageKeys();
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(inputName + "[]")));
		List<WebElement> formInputElements = driver.findElements(By.name(inputName + "[]"));
		WebElement firstElement = formInputElements.get(0);
		FormInputHtmlTag formInputHtmlTag = FormInputHtmlTag.valueOf(firstElement.getTagName());
		switch (formInputHtmlTag) {
		case select -> selectFromDropdown(firstElement, value);
		case button -> choose(formInputElements, value);
		case textarea -> enterInput(firstElement, value);
		case input -> {
			switch (InputTypeHtmlAttribute.valueOf(firstElement.getAttribute("type"))) {
			case text -> {
				if (firstElement.getAttribute("class").contains("dob-input")) {
					enterDateInput(inputName, value);
				} else {
					enterInput(firstElement, value);
				}
			}
			case radio, checkbox -> selectEnumeratedInput(formInputElements, value);
			default -> enterInput(firstElement, value);
			}
		}
		default -> throw new IllegalArgumentException("Cannot find element");
		}
	}

	/**
	 * Yes and No buttons perform their own page submit action and need their own
	 * wait period for the next page to load.
	 * 
	 * @param inputName
	 * @param value
	 * @param nextPage
	 */
	public void chooseYesOrNo(String inputName, String value, String nextPage) {
		List<WebElement> formInputElements = driver.findElements(By.name(inputName + "[]"));
		WebElement buttonToClick = formInputElements.stream().filter(button -> button.getText().contains(value))
				.findFirst().orElseThrow();
		buttonToClick.sendKeys(Keys.ENTER);
		Duration duration = Duration.of(5, ChronoUnit.SECONDS);
		WebDriverWait wait = new WebDriverWait(driver, duration);
		wait.until(ExpectedConditions.titleContains(nextPage));
	}

  public void enter(String inputName, List<String> value) {
    checkForBadMessageKeys();
    List<WebElement> formInputElements = driver.findElements(By.name(inputName + "[]"));
    WebElement firstElement = formInputElements.get(0);
    FormInputHtmlTag formInputHtmlTag = FormInputHtmlTag.valueOf(firstElement.getTagName());
    if (formInputHtmlTag == FormInputHtmlTag.input) {
      if (InputTypeHtmlAttribute.valueOf(firstElement.getAttribute("type"))
          == InputTypeHtmlAttribute.checkbox) {
        selectEnumeratedInput(formInputElements, value);
      } else {
        throw new IllegalArgumentException("Can't select multiple options for non-checkbox inputs");
      }
    } else {
      throw new IllegalArgumentException("Cannot find element");
    }
  }

  private void enterInput(WebElement webElement, String input) {
    webElement.clear();
    webElement.sendKeys(input);
  }

  private void enterInputById(String inputId, String value) {
    enterInput(driver.findElement(By.id(inputId)), value);
  }

  private void enterDateInput(String inputName, String value) {
    String[] dateParts = value.split("/", 3);
    enterInputById(inputName + "-month", dateParts[DatePart.MONTH.getPosition() - 1]);
    enterInputById(inputName + "-day", dateParts[DatePart.DAY.getPosition() - 1]);
    enterInputById(inputName + "-year", dateParts[DatePart.YEAR.getPosition() - 1]);
  }

  private void selectEnumeratedInput(List<WebElement> webElements, String optionText) {
    WebElement inputToSelect = webElements.stream()
        .map(input -> input.findElement(By.xpath("./..")))
        .filter(label -> label.getText().contains(optionText))
        .findFirst()
        .orElseThrow(
            () -> new RuntimeException(String.format("Cannot find value \"%s\"", optionText)));
    inputToSelect.click();
  }

  private void selectEnumeratedInput(List<WebElement> webElements, List<String> options) {
    options.forEach(option -> selectEnumeratedInput(webElements, option));
  }

  private void choose(List<WebElement> yesNoButtons, String value) {
    WebElement buttonToClick = yesNoButtons.stream()
        .filter(button -> button.getText().contains(value))
        .findFirst()
        .orElseThrow();
    buttonToClick.sendKeys(Keys.ENTER);
  }

  public void selectFromDropdown(String inputName, String optionText) {
    selectFromDropdown(
        driver.findElement(By.cssSelector(String.format("select[name='%s']", inputName))),
        optionText);
  }

  private void selectFromDropdown(WebElement webElement, String optionText) {
    WebElement optionToSelect = webElement
        .findElements(By.tagName("option")).stream()
        .filter(option -> option.getText().equals(optionText))
        .findFirst()
        .orElseThrow();
    optionToSelect.click();// sendKeys(Keys.ENTER) doesn't work here
  }

  public WebElement getSelectedOption(String elementId) {
    return driver.findElement(By.id(elementId))
        .findElements(By.tagName("option")).stream()
        .filter(WebElement::isSelected)
        .findFirst()
        .orElseThrow();
  }

  public String getInputValue(String inputName) {
    return driver.findElement(By.cssSelector(String.format("input[name='%s[]']", inputName)))
        .getAttribute("value");
  }

  public String getElementText(String inputId) {
    return driver.findElement(By.id(inputId)).getText();
  }

  public String getBirthDateValue(String inputName, DatePart datePart) {
    return driver.findElement(
            By.cssSelector(
                String.format("input[name='%s[]']:nth-of-type(%s)", inputName, datePart.getPosition())))
        .getAttribute("value");
  }

  public String getRadioValue(String inputName) {
    return driver.findElements(By.cssSelector(String.format("input[name='%s[]']", inputName)))
        .stream()
        .filter(WebElement::isSelected)
        .map(input -> input.findElement(By.xpath("./..")).getText())
        .findFirst()
        .orElse(null);
  }

  public List<String> getCheckboxValues(String inputName) {
    return driver.findElements(By.cssSelector(String.format("input[name='%s[]']", inputName)))
        .stream()
        .filter(WebElement::isSelected)
        .map(input -> input.findElement(By.xpath("./..")).getText().split("\n")[0])
        .collect(Collectors.toList());
  }
  
  public List<String> getCheckboxDisplays(String inputName) {
    return driver.findElements(By.cssSelector(String.format("input[name='%s[]']", inputName)))
        .stream()
        .filter(WebElement::isDisplayed)
        .map(input -> input.findElement(By.xpath("./..")).getText().split("\n")[0])
        .collect(Collectors.toList());
  }

  public String getSelectValue(String inputName) {
    return driver.findElement(By.cssSelector(String.format("select[name='%s[]']", inputName)))
        .findElements(By.tagName("option")).stream()
        .filter(WebElement::isSelected)
        .findFirst()
        .map(WebElement::getText)
        .orElseThrow();
  }

  public boolean hasInputError(String inputName) {
    return !driver.findElements(
        By.cssSelector(String.format("input[name='%s[]'] ~ p.text--error", inputName))).isEmpty();
  }

  public boolean selectHasInputError(String inputName) {
    return !driver.findElements(By.id(String.format("%s-error-p", inputName))).isEmpty();
  }

  public boolean hasErrorText(String errorMessage) {
    return driver.findElements(By.cssSelector("p.text--error > span"))
        .stream().anyMatch(webElement -> webElement.getText().equals(errorMessage));
  }

  public String getFirstInputError() {
    return driver.findElements(By.cssSelector("p.text--error > span")).stream().findFirst()
        .map(WebElement::getText).orElse(null);
  }

  /**
   * 
   * Javadoc for findElement tells us: Find the first WebElement using the given method. 
   * This method is affected by the'implicit wait' times in force at the time of execution. 
   * The findElement(..) invocation will return a matching row, or try again repeatedly until the configured timeout is reached. 
<b>findElement should not be used to look for non-present elements, use findElements(By) and assert zero length response instead. </b>
   * @param inputName
   * @return
   */
  public boolean inputIsValid(String inputName) {
    return driver.findElement(By.cssSelector(String.format("input[name='%s[]']", inputName)))
        .getDomAttribute("aria-invalid").equals("false");
  }

  public String getInputAriaLabel(String inputName) {
    return driver.findElement(By.cssSelector(String.format("input[name='%s[]']", inputName)))
        .getAttribute("aria-label");
  }

  public String getSelectAriaLabel(String inputName) {
    return driver.findElement(By.cssSelector(String.format("select[name='%s[]']", inputName)))
        .getAttribute("aria-label");
  }

  public String getInputAriaDescribedBy(String inputName) {
    return driver.findElement(By.cssSelector(String.format("input[name='%s[]']", inputName)))
        .getAttribute("aria-describedby");
  }

  public String getSelectAriaDescribedBy(String inputName) {
    return driver.findElement(By.cssSelector(String.format("select[name='%s[]']", inputName)))
        .getAttribute("aria-describedby");
  }

  public String getInputAriaLabelledBy(String inputName) {
    return getInputAriaLabelledBy("input", inputName);
  }

  public String getInputAriaLabelledBy(String elementType, String elementName) {
    return driver.findElement(
            By.cssSelector(String.format("%s[name='%s[]']", elementType, elementName)))
        .getAttribute("aria-labelledby");
  }

  public String findElementTextById(String id) {
    return driver.findElement(By.id(id)).getText();
  }

  public WebElement findElementById(String id) {
    return driver.findElement(By.id(id));
  }

  public boolean elementDoesNotExistById(String id) {
    try {
      driver.findElement(By.id(id));
      return false;//element found, it does exist so return false
    } catch (org.openqa.selenium.NoSuchElementException e) {
      return true;//element not found, it does not exist
    }
  }


  public void clickElementById(String id) {
    WebElement inputToSelect = driver.findElement(By.id(id));
    inputToSelect.click();
  }

  public void chooseSentiment(Sentiment sentiment) {
    driver.findElement(
            By.cssSelector(String.format("label[for='%s']", sentiment.name().toLowerCase())))
        .click();
  }

  enum FormInputHtmlTag {
    input,
    textarea,
    select,
    button
  }

  enum InputTypeHtmlAttribute {
    text,
    number,
    radio,
    checkbox,
    tel
  }

}
