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
	// System.setProperty("webdriver.chrome.driver", "path/to/chromedriver");//TODO emj testing this
	//  new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.visibilityOf(element));
    WebDriverManager.chromedriver().setup();
    ChromeOptions options = new ChromeOptions();
    HashMap<String, Object> chromePrefs = new HashMap<>();
    chromePrefs.put("download.default_directory", tempdir.toString());
    options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
    // error: no chrome binary at chrome/win64-135.0.7049.85/chrome-win64/chrome.exe 
    // options.setBinary("chrome/win64-135.0.7049.85/chrome-win64/chrome.exe");
    //Found this while debugging: C:\Users\pwemj35\.cache\selenium\chromedriver\win64\135.0.7049.114\chromedriver.exe
    // error: no chrome binary at ../chromedriver/win64/135.0.7049.114//chromedriver.exe 
    //options.setBinary("../chromedriver/win64/135.0.7049.114//chromedriver.exe");
    options.setExperimentalOption("prefs", chromePrefs);
    options.addArguments("--window-size=1280,1600");//needed for snapshots
    options.addArguments("--headless=new");
    options.addArguments("--remote-allow-origins=*");//prevents session errors
	Duration duration = Duration.of(5, ChronoUnit.SECONDS);
	options.setImplicitWaitTimeout(duration);
    driver = new ChromeDriver(options);
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    System.out.println("✅ ChromeDriver started and should be visible now!");//TODO emj delete
   
  }

  public void stop() {
    if (driver != null) {
      driver.close();
      driver.quit();
    }
  }
}
