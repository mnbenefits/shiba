package org.codeforamerica.shiba.pages.events;

import static org.codeforamerica.shiba.output.Recipient.CLIENT;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.DocumentListParser;
import org.codeforamerica.shiba.output.ApplicationFile;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.pdf.PdfGenerator;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.springframework.stereotype.Component;

@Component
public class PdfEncoder {
	private final PdfGenerator pdfGenerator;

	public PdfEncoder(PdfGenerator pdfGenerator) {
		this.pdfGenerator = pdfGenerator;
	}

	public String createEncodedPdfString(Application application) {
		 String applicationId = application.getId();
		 ApplicationData applicationData = application.getApplicationData();
			
			List<Document> docs = DocumentListParser.parse(applicationData);
			List<ApplicationFile> pdfs = docs.stream().map(doc -> pdfGenerator.generate(applicationId, doc, CLIENT))
					.toList();

			List<String> encodedPdfs = pdfs.stream()
					.map(pdf -> pdf.getFileName() + "|" + Base64.getEncoder().encodeToString(pdf.getFileBytes()))
					.collect(Collectors.toList());

			String pdfString = String.join(", ", encodedPdfs);
			return pdfString;
	}

}
