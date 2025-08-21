package org.codeforamerica.shiba.testutilities;

import io.percy.selenium.Percy;
import org.openqa.selenium.remote.RemoteWebDriver;

public class PercyTestPage extends Page {

  protected final Percy percy;
  

  public PercyTestPage(RemoteWebDriver driver) {
    super(driver);
    this.percy = new Percy(driver);
  }

  public void clickLink(String linkText, String nextPage) {
	      percy.snapshot(driver.getTitle());
	      super.clickLink(linkText, nextPage);
	    }
	  
		public void clickButton(String buttonText, String nextPage) {
	      percy.snapshot(driver.getTitle());
	      super.clickButton(buttonText, nextPage);
	    }
	  
		public void clickButtonLink(String buttonLinkText, String nextPage) {
	      percy.snapshot(driver.getTitle());
	      super.clickButtonLink(buttonLinkText, nextPage);
	    }
}
