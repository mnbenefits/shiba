package org.codeforamerica.shiba.testutilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.codeforamerica.shiba.DocumentRepositoryTestConfig;
import org.codeforamerica.shiba.output.Document;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import({ WebDriverConfiguration.class, DocumentRepositoryTestConfig.class })
@ActiveProfiles("test")
public abstract class AbstractBasePageTest {

	protected static final String PROGRAM_SNAP = "Food (SNAP)";
	protected static final String PROGRAM_CASH = "Cash programs";
	protected static final String PROGRAM_GRH = "Housing Support Program (GRH)";
	protected static final String PROGRAM_CCAP = "Child Care Assistance";
	protected static final String PROGRAM_EA = "Emergency Assistance";
	protected static final String PROGRAM_NONE = "None of the above";
	private static final String UPLOADED_JPG_FILE_NAME = "shiba+file.jpg";
	private static final String UPLOADED_PDF_NAME = "test-caf.pdf";
	private static final String XFA_PDF_NAME = "xfa-invoice-example.pdf";
	private static final String PASSWORD_PROTECTED_PDF = "password-protected.pdf";
	private static final String UPLOAD_RIFF_WITH_RENAMED_JPG_EXTENSION = "RiffSavedAsJPGTestDoc.jpg";
	@Autowired
	protected RemoteWebDriver driver;

	@Autowired
	protected Path path;

	protected String baseUrl;

	@LocalServerPort
	protected String localServerPort;

	protected Page testPage;

	@BeforeEach
	protected void setUp() throws IOException {
		baseUrl = "http://localhost:%s".formatted(localServerPort);
		driver.navigate().to(baseUrl);
		initTestPage();
	}

	protected void initTestPage() {
		testPage = new Page(driver);
	}

	/**
	 * This method is the equivalent of using the browser's back history. Like
	 * holding the browser back arrow down to see history. Right click on the back
	 * arrow does the same thing.
	 * 
	 * @param pageName
	 */
	protected void navigateTo(String pageName) {
		driver.navigate().to(baseUrl + "/pages/" + pageName);
	}

	protected Map<Document, PDAcroForm> getAllFiles() {
		return Arrays.stream(Objects.requireNonNull(path.toFile().listFiles()))
				.filter(file -> file.getName().endsWith(".pdf"))
				.sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()))
				.collect(Collectors.toMap(this::getDocumentType, pdfFile -> {
					try {
						return Loader.loadPDF(pdfFile).getDocumentCatalog().getAcroForm();
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}, (r1, r2) -> r1));
	}

	protected List<File> getZipFile() {
		return Arrays.stream(Objects.requireNonNull(path.toFile().listFiles()))
				.filter(file -> file.getName().endsWith(".zip")).toList();
	}

	protected void unzipFiles() {
		List<File> filesList = Arrays.stream(Objects.requireNonNull(path.toFile().listFiles()))
				.filter(file -> file.getName().endsWith(".zip"))
				.collect(Collectors.toCollection(() -> new ArrayList<File>()));
		unzip(filesList);

	}

	protected List<File> unzip(List<File> filesList) {
		List<File> fileList = new ArrayList<File>();
		for (File file : filesList) {
			try {
				FileInputStream inputStream = new FileInputStream(file);
				ZipInputStream zipStream = new ZipInputStream(inputStream);
				ZipEntry zEntry;
				String destination = path.toFile().getPath();
				while ((zEntry = zipStream.getNextEntry()) != null) {
					if (zEntry.getName().contains("_CAF") || zEntry.getName().contains("_CCAP")) {
						if (!zEntry.isDirectory()) {
							File files = new File(destination, zEntry.getName());
							FileOutputStream fout = new FileOutputStream(files);
							BufferedOutputStream bufout = new BufferedOutputStream(fout);
							byte[] buffer = new byte[1024];
							int read;
							while ((read = zipStream.read(buffer)) != -1) {
								bufout.write(buffer, 0, read);
							}
							zipStream.closeEntry();// This will delete zip folder after extraction
							bufout.close();
							fout.close();
							fileList.add(files);
						}
					}
				}
				zipStream.close();// This will delete zip folder after extraction
			} catch (Exception e) {
				System.out.println("Unzipping failed");
				e.printStackTrace();
			}
		}
		return fileList;
	}

	private Document getDocumentType(File file) {
		String fileName = file.getName();
		if (fileName.contains("_CAF")) {
			return Document.CAF;
		} else if (fileName.contains("_CCAP")) {
			return Document.CCAP;
		} else {
			return Document.CAF;
		}
	}

	protected void waitForDocumentUploadToComplete() {
		await().atMost(15, TimeUnit.SECONDS).until(() -> !driver.findElements(By.linkText("delete")).isEmpty());
	}

	/**
	 * Creates an image of the browser page. It should be a PNG file, example:
	 * "webPage.png". If no path is used, the file will be located in the project
	 * root. (delete the file afterwards so it isn't committed to GitHub)
	 * 
	 * @param fileWithPath
	 */
	@SuppressWarnings("unused")
	public void takeSnapShot(String fileWithPath) {
		TakesScreenshot screenshot = driver;
		Path sourceFile = screenshot.getScreenshotAs(OutputType.FILE).toPath();
		Path destinationFile = new File(fileWithPath).toPath();
		try {
			Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void fillOutPersonalInfo() {
		fillOutPersonInfo();
		testPage.enter("moveToMnPreviousCity", "Chicago");
	}

	protected void fillOutPersonInfo() {
		testPage.enter("firstName", "defaultFirstName");
		testPage.enter("lastName", "defaultLastName");
		testPage.enter("otherName", "defaultOtherName");
		testPage.enter("dateOfBirth", "01/12/1928");
		testPage.enter("ssn", "123456789");
		testPage.enter("maritalStatus", "Never married");
		testPage.enter("sex", "Female");
		testPage.enter("livedInMnWholeLife", "Yes");
		testPage.enter("moveToMnDate", "02/18/1776");
	}

	protected void fillOutMatchInfo() {
		testPage.enter("firstName", "defaultFirstName");
		testPage.enter("lastName", "defaultLastName");
		testPage.enter("dateOfBirth", "01/12/1928");
		testPage.enter("ssn", "123456789");
		testPage.enter("phoneNumber", "234-567-8900");
		testPage.enter("email", "default@mailnator.com");
		testPage.enter("caseNumber", "1234567");
	}

	protected void uploadFile(String filepath) {
		testPage.clickElementById("drag-and-drop-box"); // is this needed?
		WebElement upload = driver.findElement(By.className("dz-hidden-input"));
		upload.sendKeys(filepath);
		await().until(
				() -> !driver.findElements(By.className("file-details")).get(0).getAttribute("innerHTML").isBlank());
	}

	protected void uploadJpgFile() {
		uploadFile(TestUtils.getAbsoluteFilepathString(UPLOADED_JPG_FILE_NAME));
		assertThat(driver.findElement(By.id("document-upload")).getText()).contains(UPLOADED_JPG_FILE_NAME);
	}

	protected void uploadInvalidJpg() {
		uploadFile(TestUtils.getAbsoluteFilepathString(UPLOAD_RIFF_WITH_RENAMED_JPG_EXTENSION));
		assertThat(driver.findElement(By.id("document-upload")).getText())
				.contains(UPLOAD_RIFF_WITH_RENAMED_JPG_EXTENSION);
	}

	protected void uploadButtonDisabledCheck() {
		testPage.clickElementById("drag-and-drop-box"); // is this needed?
		WebElement upload = driver.findElement(By.className("dz-hidden-input"));
		upload.sendKeys(TestUtils.getAbsoluteFilepathString(UPLOADED_JPG_FILE_NAME));
		assertThat(driver.findElement(By.id("submit-my-documents")).getAttribute("class")).contains("disabled");
		await().until(
				() -> !driver.findElements(By.className("file-details")).get(0).getAttribute("innerHTML").isBlank());
		assertThat(driver.findElement(By.id("document-upload")).getText()).contains(UPLOADED_JPG_FILE_NAME);
	}

	protected void uploadXfaFormatPdf() {
		uploadFile(TestUtils.getAbsoluteFilepathString(XFA_PDF_NAME));
		assertThat(driver.findElement(By.id("document-upload")).getText()).contains(XFA_PDF_NAME);
	}

	protected void uploadPasswordProtectedPdf() {
		uploadFile(TestUtils.getAbsoluteFilepathString(PASSWORD_PROTECTED_PDF));
		assertThat(driver.findElement(By.id("document-upload")).getText()).contains(PASSWORD_PROTECTED_PDF);
	}

	protected void uploadPdfFile() {
		uploadFile(TestUtils.getAbsoluteFilepathString(UPLOADED_PDF_NAME));
		assertThat(driver.findElement(By.id("document-upload")).getText()).contains(UPLOADED_PDF_NAME);
	}

	protected void uploadVirusFile(String testFileName) {
		uploadFile(TestUtils.getAbsoluteFilepathString(testFileName));
		assertThat(driver.findElement(By.id("document-upload")).getText()).contains(testFileName);
	}

	protected void getToDocumentUploadScreen() {
		// landing page
		testPage.clickButtonLink("Apply now", "Identify County");
		// identifyCountyBeforeApplying
		testPage.enter("county", "Hennepin");
		testPage.clickContinue("Prepare To Apply");
		// prepareToApply
		testPage.clickButtonLink("Continue", "Timeout notice");
		// timeoutNotice
		testPage.clickButtonLink("Continue", "Language Preferences - Written");
		// writtenLanguage
		testPage.enter("writtenLanguage", "English");
		testPage.clickContinue("Language Preferences - Spoken");
		// spokenLanguage
		testPage.enter("spokenLanguage", "English");
		testPage.enter("needInterpreter", "Yes");
		testPage.clickContinue("Choose Programs");
		// choosePrograms
		testPage.enter("programs", PROGRAM_EA);
		testPage.clickContinue("Emergency Type");
		// emergencyType
		testPage.enter("emergencyType", "Utility shut-off");
		testPage.clickContinue("Intro: Basic Info");
		// introBasicInfo
		testPage.clickButtonLink("Continue", "Personal Info");
		// personalInfo
		fillOutPersonalInfo();
		testPage.clickContinue("Home Address");
		// homeAddress
		navigateTo("signThisApplication");
		testPage.enter("applicantSignature", "some name");
		testPage.clickButton("Continue", "Submit application");
		testPage.clickCustomButton("Submit application", 3, "Submission Confirmation");
		testPage.clickButtonLink("Continue", "Adding Documents");
		testPage.clickButtonLink("Continue", "Document Recommendation");
		testPage.clickButtonLink("Add documents now", "How to add documents");
		testPage.clickButtonLink("Continue", "Upload documents");
	}

	protected void getToLaterDocsUploadScreen() {
		testPage.clickButtonLink("Upload documents", "Ready to upload documents");

		// Ready to upload documents?
		testPage.clickButtonLink("Continue", "Match Info");

		// Match Info
		fillOutMatchInfo();
		testPage.clickContinue("Identify County");

		// Identify County
		testPage.enter("county", "Hennepin");
		testPage.clickContinue("Tribal Nation member");

		// Tribal Nation Member
		testPage.chooseYesOrNo("isTribalNationMember", "No", "How to add documents");

		// How to add documents
		testPage.clickButtonLink("Continue", "Upload documents");
	}

	protected void getToHealthcareRenewalUploadScreen() {
		navigateTo("healthcareRenewalUpload");
		testPage.enter("county", "Hennepin");
		testPage.clickContinue("Match Info");

		fillOutMatchInfo();
		testPage.clickContinue("How to add documents");

		testPage.clickButtonLink("Continue", "Upload documents");
	}

	protected String getAttributeForElementAtIndex(List<WebElement> elementList, int index, String attributeName) {
		return elementList.get(index).getAttribute(attributeName);
	}

	@NotNull
	protected Callable<Boolean> uploadCompletes() {
		return () -> !getAttributeForElementAtIndex(driver.findElements(By.className("dz-remove")), 0, "innerHTML")
				.isBlank();
	}
}
