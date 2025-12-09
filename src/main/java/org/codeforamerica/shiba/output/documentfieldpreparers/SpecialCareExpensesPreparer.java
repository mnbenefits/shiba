package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.output.pdf.PdfFieldFiller;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.springframework.stereotype.Component;

@Component
public class SpecialCareExpensesPreparer implements DocumentFieldPreparer {

	private final PdfFieldFiller uploadedDocCoverPageFilter;
	List<DocumentField> fields = new ArrayList<>();

	SpecialCareExpensesPreparer(PdfFieldFiller uploadedDocCoverPageFilter) {
		this.uploadedDocCoverPageFilter = uploadedDocCoverPageFilter;
	}

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		fields = new ArrayList<DocumentField>();
		PageData specialCareExpenses = application.getApplicationData().getPageData("specialCareExpenses");

		if (specialCareExpenses != null) {

			InputData specialCareExpensesInputData = specialCareExpenses.get("specialCareExpenses");
			if (specialCareExpensesInputData != null && !specialCareExpensesInputData.getValue().isEmpty()) {
				List<String> listOfSpecialCareExpenses = specialCareExpensesInputData.getValue();

				Map<String, String> expenseFieldMapping = Map.of("REPRESENTATIVE_PAYEE_FEES", "representativePayeeFees",
						"GUARDIAN_AND_CONSERVATOR_FEES", "guardianAndConservatorFees",
						"SPECIAL_DIET_PRESCRIBED_BY_DOCTOR", "specialDietPrescribedByDoctor", "HIGH_HOUSING_COSTS",
						"highHousingCosts");

				for (Map.Entry<String, String> entry : expenseFieldMapping.entrySet()) {
					String value = listOfSpecialCareExpenses.contains(entry.getKey()) ? "Yes" : "No";

					fields.add(new DocumentField("specialCareExpenses", entry.getValue(), value, SINGLE_VALUE));
				}

			}

		}
		return fields;
	}
}
