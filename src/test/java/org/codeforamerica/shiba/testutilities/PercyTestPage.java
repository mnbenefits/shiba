package org.codeforamerica.shiba.testutilities;

import io.percy.selenium.Percy;
import org.openqa.selenium.remote.RemoteWebDriver;

public class PercyTestPage extends Page {

  protected final Percy percy;

  public PercyTestPage(RemoteWebDriver driver) {
    super(driver);
    this.percy = new Percy(driver);
  }
}
