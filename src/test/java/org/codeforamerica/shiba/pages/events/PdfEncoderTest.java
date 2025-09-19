package org.codeforamerica.shiba.pages.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.DocumentListParser;
import org.codeforamerica.shiba.output.ApplicationFile;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.pdf.PdfGenerator;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class PdfEncoderTest {

	private PdfGenerator pdfGenerator;
	private Application application;
	private ApplicationData applicationData;
	private PdfEncoder pdfEncoder;

	@BeforeEach
	void setUp() {
		pdfGenerator = mock(PdfGenerator.class);
		application = mock(Application.class);
		applicationData = mock(ApplicationData.class);
		pdfEncoder = new PdfEncoder(pdfGenerator);
	}

	@Test
	void testCreateEncodedPdfString() {

		String applicationId = "test-app-id";
		Document doc1 = Document.CAF;
		Document doc2 = Document.CCAP;
		ApplicationFile pdf1 = new ApplicationFile("someContent1".getBytes(), "someFileName1.pdf");
		ApplicationFile pdf2 = new ApplicationFile("someContent2".getBytes(), "someFileName2.pdf");

		when(application.getId()).thenReturn(applicationId);
		when(application.getApplicationData()).thenReturn(applicationData);
		try (MockedStatic<DocumentListParser> mockedDocumentListParser = mockStatic(DocumentListParser.class)) {
			mockedDocumentListParser.when(() -> DocumentListParser.parse(applicationData))
					.thenReturn(new ArrayList<>(Arrays.asList(doc1, doc2)));

			when(pdfGenerator.generate(eq(applicationId), eq(doc1), any())).thenReturn(pdf1);
			when(pdfGenerator.generate(eq(applicationId), eq(doc2), any())).thenReturn(pdf2);

			String result = pdfEncoder.createEncodedPdfString(application);
			
			// Assert
			String expectedPdf1 = "someFileName1.pdf|" + Base64.getEncoder().encodeToString("someContent1".getBytes());
			String expectedPdf2 = "someFileName2.pdf|" + Base64.getEncoder().encodeToString("someContent2".getBytes());
			
			String expected = expectedPdf1 + ", " + expectedPdf2;
			assertEquals(expected, result);

			verify(pdfGenerator, times(1)).generate(eq(applicationId), eq(doc1), any());
			verify(pdfGenerator, times(1)).generate(eq(applicationId), eq(doc2), any());
		}
	}
}
