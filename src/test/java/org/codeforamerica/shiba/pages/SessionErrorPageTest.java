package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.codeforamerica.shiba.testutilities.AbstractBasePageTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "server.servlet.session.timeout = 5s" }) // 5 second session timeout for the test
public class SessionErrorPageTest extends AbstractBasePageTest {

    @Override
    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        driver.navigate().to(baseUrl);
    }

    @Test
    void shouldDisplaySessionTimeoutPage() throws InterruptedException {
        testPage.clickButtonLink("Apply now", "Identify County");
        TimeUnit time = TimeUnit.SECONDS;
        time.sleep(6); // Sleep for 5 seconds + a margin
        testPage.clickButton("Continue", "Timeout");
        assertThat(driver.getTitle()).isEqualTo("Timeout");
    }
    
    @Test
    void shouldDisplayErrorUploadTimeoutPage() throws InterruptedException {
        testPage.clickButtonLink("Upload documents", "Ready to upload documents");
        TimeUnit time = TimeUnit.SECONDS;
        time.sleep(6); // Sleep for 5 seconds + a margin
        testPage.clickButtonLink("Continue", "Doc Upload Timeout");
        assertThat(driver.getTitle()).isEqualTo("Doc Upload Timeout");
    }
}
