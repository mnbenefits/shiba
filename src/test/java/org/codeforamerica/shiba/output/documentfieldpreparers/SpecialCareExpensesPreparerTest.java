package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.pdf.PdfFieldFiller;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

public class SpecialCareExpensesPreparerTest {

	private final PdfFieldFiller pdfFieldFiller = mock(PdfFieldFiller.class);
	private final SpecialCareExpensesPreparer preparer = new SpecialCareExpensesPreparer(pdfFieldFiller);

	@Test
	public void shouldReturnEmptyForMissingPageData() {
		ApplicationData applicationData = new ApplicationData();
		List result = preparer.prepareDocumentFields(Application.builder().applicationData(applicationData).build(),
				null, null);

		assertThat(result).isEmpty();
	}

	@Test
	public void shouldReturnEmptyWhenSpecialCareExpensesInputIsNull() {
		ApplicationData applicationData = new TestApplicationDataBuilder().build();

		List<DocumentField> result = preparer
				.prepareDocumentFields(Application.builder().applicationData(applicationData).build(), null, null);

		assertThat(result).isEmpty();
	}

	@Test
	public void shouldReturnEmptyWhenSpecialCareExpensesInputIsEmpty() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("specialCareExpenses", "specialCareExpenses", List.of()).build();

		List<DocumentField> result = preparer
				.prepareDocumentFields(Application.builder().applicationData(applicationData).build(), null, null);

		assertThat(result).isEmpty();
	}

	@Test
	public void shouldMapRepresentativePayeeFeesToYesWhenSelected() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("specialCareExpenses", "specialCareExpenses", List.of("REPRESENTATIVE_PAYEE_FEES"))
				.build();

		List<DocumentField> result = preparer
				.prepareDocumentFields(Application.builder().applicationData(applicationData).build(), null, null);

		assertThat(result).containsOnly(
				new DocumentField("specialCareExpenses", "representativePayeeFees", "Yes", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "guardianAndConservatorFees", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "specialDietPrescribedByDoctor", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "highHousingCosts", "No", SINGLE_VALUE));
	}

	@Test
	public void shouldMapGuardianAndConservatorFeesToYesWhenSelected() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("specialCareExpenses", "specialCareExpenses", List.of("GUARDIAN_AND_CONSERVATOR_FEES"))
				.build();

		List<DocumentField> result = preparer
				.prepareDocumentFields(Application.builder().applicationData(applicationData).build(), null, null);

		assertThat(result).containsOnly(
				new DocumentField("specialCareExpenses", "representativePayeeFees", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "guardianAndConservatorFees", "Yes", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "specialDietPrescribedByDoctor", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "highHousingCosts", "No", SINGLE_VALUE));
	}

	@Test
	public void shouldMapSpecialDietPrescribedByDoctorToYesWhenSelected() {
		ApplicationData applicationData = new TestApplicationDataBuilder().withPageData("specialCareExpenses",
				"specialCareExpenses", List.of("SPECIAL_DIET_PRESCRIBED_BY_DOCTOR")).build();

		List<DocumentField> result = preparer
				.prepareDocumentFields(Application.builder().applicationData(applicationData).build(), null, null);

		assertThat(result).containsOnly(
				new DocumentField("specialCareExpenses", "representativePayeeFees", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "guardianAndConservatorFees", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "specialDietPrescribedByDoctor", "Yes", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "highHousingCosts", "No", SINGLE_VALUE));
	}

	@Test
	public void shouldMapHighHousingCostsToYesWhenSelected() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("specialCareExpenses", "specialCareExpenses", List.of("HIGH_HOUSING_COSTS")).build();

		List<DocumentField> result = preparer
				.prepareDocumentFields(Application.builder().applicationData(applicationData).build(), null, null);

		assertThat(result).containsOnly(
				new DocumentField("specialCareExpenses", "representativePayeeFees", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "guardianAndConservatorFees", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "specialDietPrescribedByDoctor", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "highHousingCosts", "Yes", SINGLE_VALUE));
	}

	@Test
	public void shouldMapMultipleSelectedExpensesToYes() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("specialCareExpenses", "specialCareExpenses", List.of("REPRESENTATIVE_PAYEE_FEES",
						"GUARDIAN_AND_CONSERVATOR_FEES", "SPECIAL_DIET_PRESCRIBED_BY_DOCTOR", "HIGH_HOUSING_COSTS"))
				.build();

		List<DocumentField> result = preparer
				.prepareDocumentFields(Application.builder().applicationData(applicationData).build(), null, null);

		assertThat(result).containsOnly(
				new DocumentField("specialCareExpenses", "representativePayeeFees", "Yes", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "guardianAndConservatorFees", "Yes", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "specialDietPrescribedByDoctor", "Yes", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "highHousingCosts", "Yes", SINGLE_VALUE));
	}

	@Test
	public void shouldMapAllExpensesToNoWhenNoneSelected() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("specialCareExpenses", "specialCareExpenses", List.of("NONE")).build();

		List<DocumentField> result = preparer
				.prepareDocumentFields(Application.builder().applicationData(applicationData).build(), null, null);

		assertThat(result).containsOnly(
				new DocumentField("specialCareExpenses", "representativePayeeFees", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "guardianAndConservatorFees", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "specialDietPrescribedByDoctor", "No", SINGLE_VALUE),
				new DocumentField("specialCareExpenses", "highHousingCosts", "No", SINGLE_VALUE));
	}
}