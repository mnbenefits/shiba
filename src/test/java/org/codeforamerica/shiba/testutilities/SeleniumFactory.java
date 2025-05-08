package org.codeforamerica.shiba.testutilities;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.FactoryBean;

import io.github.bonigarcia.wdm.WebDriverManager;

public class SeleniumFactory implements FactoryBean<RemoteWebDriver> {

  private final Path tempdir;
  private RemoteWebDriver driver;

  public SeleniumFactory(Path tempdir) {
    this.tempdir = tempdir;
  }

  @Override
  public RemoteWebDriver getObject() {
    return driver;
  }

  @Override
  public Class<RemoteWebDriver> getObjectType() {
    return RemoteWebDriver.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  /**
   * The implicit timeouts don't seem to work. I am leaving them in for now, until the explicit timeouts in Page.java clickButton method are used for all tests.
   * Journey tests started failing in August 2024.
   * @throws IOException
   */
  public void start() throws IOException {
    WebDriverManager.chromedriver().setup();
    ChromeOptions options = new ChromeOptions();
    HashMap<String, Object> chromePrefs = new HashMap<>();
    chromePrefs.put("download.default_directory", tempdir.toString());
    options.setPageLoadStrategy(PageLoadStrategy.NORMAL); 
    options.setExperimentalOption("prefs", chromePrefs);
    options.addArguments("--window-size=1280,1600");//needed for snapshots
    options.addArguments("--headless=new");
    options.addArguments("--remote-allow-origins=*");//prevents session errors
	Duration duration = Duration.of(2, ChronoUnit.SECONDS);
	options.setImplicitWaitTimeout(duration);
    driver = new ChromeDriver(options);
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
  }

  public void stop() {
    if (driver != null) {
      driver.close();
      driver.quit();
    }
  }
}
