package org.codeforamerica.shiba.journeys;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@Tag("virusUploadJourney")
public class VirusUploadJourneyTest extends JourneyTest {

  @Test
  void whenDocumentUploadVirusThereShouldBeAnError() throws InterruptedException, IOException {
    getToDocumentUploadScreen();
    uploadGIFVirusFile();
    waitForErrorMessage();
    assertThat(driver.findElements(By.className("text--error")).get(0).getText()).contains(
        "Your file cannot be uploaded because a virus was detected. Try uploading a different copy of the file.");
    assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).contains(
        "0 files added");
    assertThat(driver.findElement(By.id("submit-my-documents")).getAttribute("class"))
    	.contains("hidden");
    testPage.clickLink("remove");

    uploadJPGVirusFile();
    waitForErrorMessage();
    assertThat(driver.findElements(By.className("text--error")).get(0).getText()).contains(
        "Your file cannot be uploaded because a virus was detected. Try uploading a different copy of the file.");
    assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).contains(
        "0 files added");
    assertThat(driver.findElement(By.id("submit-my-documents")).getAttribute("class"))
    	.contains("hidden");
    testPage.clickLink("remove");

    uploadPNGVirusFile();
    waitForErrorMessage();
    assertThat(driver.findElements(By.className("text--error")).get(0).getText()).contains(
        "Your file cannot be uploaded because a virus was detected. Try uploading a different copy of the file.");
    assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).contains(
        "0 files added");
    assertThat(driver.findElement(By.id("submit-my-documents")).getAttribute("class"))
    	.contains("hidden");
    testPage.clickLink("remove");
    
    uploadPDFVirusFile1();
    waitForErrorMessage();
    assertThat(driver.findElements(By.className("text--error")).get(0).getText()).contains(
        "Your file cannot be uploaded because a virus was detected. Try uploading a different copy of the file.");
    assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).contains(
        "0 files added");
    assertThat(driver.findElement(By.id("submit-my-documents")).getAttribute("class"))
    	.contains("hidden");
    testPage.clickLink("remove");

    uploadPDFVirusFile2();
    waitForErrorMessage();
    assertThat(driver.findElements(By.className("text--error")).get(0).getText()).contains(
        "Your file cannot be uploaded because a virus was detected. Try uploading a different copy of the file.");
    assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).contains(
        "0 files added");
    assertThat(driver.findElement(By.id("submit-my-documents")).getAttribute("class"))
    	.contains("hidden");
    testPage.clickLink("remove");
    
    uploadPDFVirusFile3();
    waitForErrorMessage();
    assertThat(driver.findElements(By.className("text--error")).get(0).getText()).contains(
        "Your file cannot be uploaded because a virus was detected. Try uploading a different copy of the file.");
    assertThat(driver.findElement(By.id("number-of-uploaded-files")).getText()).contains(
        "0 files added");
    assertThat(driver.findElement(By.id("submit-my-documents")).getAttribute("class"))
    	.contains("hidden");
    testPage.clickLink("remove");
  }
}