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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
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

  public void start() throws IOException {
    WebDriverManager.chromedriver().setup();
    ChromeOptions options = new ChromeOptions();
    HashMap<String, Object> chromePrefs = new HashMap<>();
    chromePrefs.put("download.default_directory", tempdir.toString());
    //options.setPageLoadStrategy(PageLoadStrategy.NORMAL);  //TODO emj remove these options if not needed
    options.setExperimentalOption("prefs", chromePrefs);
    options.addArguments("--window-size=1280,1600");//needed for snapshots
    options.addArguments("--headless=new");
    options.addArguments("--remote-allow-origins=*");//prevents session errors
	//Duration duration = Duration.of(35, ChronoUnit.SECONDS);
	//options.setImplicitWaitTimeout(duration);
    driver = new ChromeDriver(options);
  }

  public void stop() {
    if (driver != null) {
      driver.close();
      driver.quit();
    }
  }
}
