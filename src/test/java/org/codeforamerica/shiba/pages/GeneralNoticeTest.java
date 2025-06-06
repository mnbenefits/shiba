package org.codeforamerica.shiba.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.codeforamerica.shiba.testutilities.AbstractBasePageTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

public class GeneralNoticeTest extends AbstractBasePageTest{

    @Autowired
    private WebDriver driver;

    @BeforeEach
    public void setUp () throws IOException {
    	 super.setUp();
    // Assuming the environment variable is set to true by default for most tests
        System.setProperty("mnb-notice-displayed", "true");
        System.setProperty("general.notice.title-en", "Important Update:");
        System.setProperty("general.notice.message-en", "Please be aware of the recent changes to our service on MNBenefits.");
        System.setProperty("general.notice.title-es", "Actualización importante:");
        System.setProperty("general.notice.message-es", "Tenga en cuenta los cambios recientes en nuestro servicio on MNBenefitos.");
        driver.navigate().to(baseUrl);
    }

    @Test
    public void testNoticeDisplayedWhenEnvironmentVariableIsTrue() {
    	driver.navigate().to(baseUrl);
        WebElement noticeElement = driver.findElement(By.id("generalNotice"));
        assertNotNull(noticeElement);
        assertTrue(noticeElement.isDisplayed());

       }

    @Test
    public void testNoticeNotDisplayedWhenEnvironmentVariableIsFalse() {
    	System.setProperty("mnb-notice-displayed", "false");
        driver.navigate().to(baseUrl);
        List<WebElement> noticeElements = driver.findElements(By.id("generalNotice"));
        assertTrue(noticeElements.isEmpty(), "Notice should not be displayed when mnb_notice_displayed is false");
    }


    @Test
    public void testDefaultEnglishMessageDisplayedWhenEnglishIsChosen() {
    	String baseUrlEn = "http://localhost:%s/?lang=en".formatted(localServerPort);
    	driver.navigate().to(baseUrlEn);
        WebElement noticeElement = driver.findElement(By.id("generalNotice"));
        assertNotNull(noticeElement);
        assertTrue(noticeElement.isDisplayed());
        WebElement noticeTitleEn =  driver.findElement(By.id("generalNoticeTitle"));
        assertNotNull(noticeTitleEn);
        assertEquals(noticeTitleEn.getText(), "Important Update:");
        WebElement noticeMessageEn =  driver.findElement(By.id("generalNoticeMessage"));
        assertNotNull(noticeMessageEn);
        assertEquals(noticeMessageEn.getText(), "Please be aware of the recent changes to our service on MNBenefits.");   
        }


    @Test
    public void testDefaultSpanishMessageDisplayedWhenSpanishIsChosen() {
    	String baseUrlEs = "http://localhost:%s/?lang=es".formatted(localServerPort);
    	driver.navigate().to(baseUrlEs);
        WebElement noticeElement = driver.findElement(By.id("generalNotice"));
        assertNotNull(noticeElement);
        assertTrue(noticeElement.isDisplayed());
        WebElement noticeTitleEs =  driver.findElement(By.id("generalNoticeTitle"));
        assertNotNull(noticeTitleEs);
        assertEquals(noticeTitleEs.getText(), "Actualización importante:");
        WebElement noticeMessageEs =  driver.findElement(By.id("generalNoticeMessage"));
        assertNotNull(noticeMessageEs);
        assertEquals(noticeMessageEs.getText(), "Tenga en cuenta los cambios recientes en nuestro servicio on MNBenefitos.");
            }
}

