package org.codeforamerica.shiba.output.documentfieldpreparers;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChildCarePreparerTest {

	private ChildCarePreparer childCarePreparer;

	@BeforeEach
	void setUp() {
		childCarePreparer = new ChildCarePreparer();
	}

	// Emulate the case when the childrenInNeedOfCare page is not displayed.
	@Test
	void shouldReturnEmptyListWhenThereAreNoChildrenInNeedOfCare() {
		ApplicationData appData = new TestApplicationDataBuilder()
				.withPageData("addHouseholdMembers", "addHouseholdMembers", "false").build();

		Application application = Application.builder().applicationData(appData).build();
		assertThat(childCarePreparer.prepareDocumentFields(application, null, Recipient.CLIENT)).isEqualTo(emptyList());
	}

	// Emulate the case where the childrenInNeedOfCare page is displayed but none of
	// the children are selected.
	@Test
	void shouldReturnEmptyListWhenNoneOfTheChildrenInNeedOfCareAreChecked() {
		ApplicationData appData = new TestApplicationDataBuilder()
				.withPageData("childrenInNeedOfCare", "whoNeedsChildCare", List.of()).build();
		Application application = Application.builder().applicationData(appData).build();
		assertThat(childCarePreparer.prepareDocumentFields(application, null, Recipient.CLIENT)).isEqualTo(emptyList());
	}

	// Emulate the case where the childrenInNeedOfCare page is displayed and
	// children are selected but the "No" button is clicked on the doYouHaveChildCareProvider page.
	@Test
	void shouldReturnOnlyChildNamesWhenApplicantDoesNotHaveAProvider() {
		ApplicationData appData = new TestApplicationDataBuilder()
				.withPageData("childrenInNeedOfCare", "whoNeedsChildCare",
						List.of("John Testerson 939dc33-d13a-4cf0-9093-309293k3",
								"Jane Testerson 939dc4-d13a-3cf0-9094-409293k4"))
				.withPageData("doYouHaveChildCareProvider", "hasChildCareProvider", "false").build();
		Application application = Application.builder().applicationData(appData).build();
		List<DocumentField> documentFields = childCarePreparer.prepareDocumentFields(application, null,
				Recipient.CLIENT);
		assertThat(documentFields).size().isEqualTo(2);
		assertThat(documentFields).contains(
				new DocumentField("childNeedsChildcare", "childName", List.of("John Testerson"), SINGLE_VALUE, 0));
		assertThat(documentFields).contains(
				new DocumentField("childNeedsChildcare", "childName", List.of("Jane Testerson"), SINGLE_VALUE, 1));
	}

	// Emulate the case where there are two children who both use the same (just one) child care provider.
	@Test
	void shouldReturnSameProviderFieldsWhenTwoChildrenUseTheSameProvider() {
		ApplicationData appData = new TestApplicationDataBuilder()
				.withSubworkflow("childCareProviders",
						new PagesDataBuilder()
								.withPageData("childCareProviderInfo",
										Map.of("childCareProviderName", List.of("Childrens Academy"), "phoneNumber",
												List.of("(612) 333-4444"), "streetAddress", List.of("101 Oak St"),
												"suiteNumber", List.of("100"), "city", List.of("Sometown"), "state",
												List.of("MN"), "zipCode", List.of("55123")))
								.withPageData("childrenAtThisProvider",
										Map.of("childrenNames",
												List.of("John Testerson 939dc33-d13a-4cf0-9093-309293k3",
														"Jane Testerson 939dc4-d13a-3cf0-9094-409293k4"))))
				.withPageData("childrenInNeedOfCare", "whoNeedsChildCare",
						List.of("John Testerson 939dc33-d13a-4cf0-9093-309293k3",
								"Jane Testerson 939dc4-d13a-3cf0-9094-409293k4"))
				.withPageData("doYouHaveChildCareProvider", "hasChildCareProvider", "true").build();

		Application application = Application.builder().applicationData(appData).build();
		List<DocumentField> documentFields = childCarePreparer.prepareDocumentFields(application, null,
				Recipient.CLIENT);
		assertThat(documentFields).size().isEqualTo(14);
		assertThat(documentFields.containsAll(List.of(
				new DocumentField("childNeedsChildcare", "childName", List.of("John Testerson"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1Name", List.of("Childrens Academy"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1Phone", List.of("(612) 333-4444"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1Street", List.of("101 Oak St #100"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1City", List.of("Sometown"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1State", List.of("MN"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1ZipCode", List.of("55123"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "childName", List.of("Jane Testerson"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1Name", List.of("Childrens Academy"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1Phone", List.of("(612) 333-4444"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1Street", List.of("101 Oak St #100"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1City", List.of("Sometown"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1State", List.of("MN"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1ZipCode", List.of("55123"), SINGLE_VALUE, 1))));
	}

	// Emulate the case where there are two children who each use a different (just one) child care provider.
	@Test
	void shouldReturnDifferentProviderFieldsWhenTwoChildrenUseDifferentProviders() {
		ApplicationData appData = new TestApplicationDataBuilder()
				.withSubworkflow("childCareProviders", new PagesDataBuilder()
						.withPageData("childCareProviderInfo",
								Map.of("childCareProviderName", List.of("Childrens Academy"), "phoneNumber",
										List.of("(612) 333-4444"), "streetAddress", List.of("101 Oak St"),
										"suiteNumber", List.of(""), // empty string
										"city", List.of("Sometown"), "state", List.of("MN"), "zipCode",
										List.of("55123")))
						.withPageData(
								"childrenAtThisProvider",
								Map.of("childrenNames", List.of("John Testerson 939dc33-d13a-4cf0-9093-309293k3"))),
						new PagesDataBuilder()
								.withPageData("childCareProviderInfo",
										Map.of("childCareProviderName", List.of("Kyds R Us"), "phoneNumber",
												List.of("(612) 222-1111"), "streetAddress", List.of("2022 Hemlock St"),
												"suiteNumber", List.of("  "), // just white space
												"city", List.of("Some City"), "state", List.of("MN"), "zipCode",
												List.of("55456")))
								.withPageData("childrenAtThisProvider",
										Map.of("childrenNames",
												List.of("Jane Testerson 939dc4-d13a-3cf0-9094-409293k4"))))
				.withPageData("childrenInNeedOfCare", "whoNeedsChildCare",
						List.of("John Testerson 939dc33-d13a-4cf0-9093-309293k3",
								"Jane Testerson 939dc4-d13a-3cf0-9094-409293k4"))
				.withPageData("doYouHaveChildCareProvider", "hasChildCareProvider", "true").build();

		Application application = Application.builder().applicationData(appData).build();
		List<DocumentField> documentFields = childCarePreparer.prepareDocumentFields(application, null,
				Recipient.CLIENT);
		assertThat(documentFields).size().isEqualTo(14);
		assertThat(documentFields.containsAll(List.of(
				new DocumentField("childNeedsChildcare", "childName", List.of("John Testerson"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1Name", List.of("Childrens Academy"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1Phone", List.of("(612) 333-4444"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1Street", List.of("101 Oak St"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1City", List.of("Sometown"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1State", List.of("MN"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "provider1ZipCode", List.of("55123"), SINGLE_VALUE, 0),
				new DocumentField("childNeedsChildcare", "childName", List.of("Jane Testerson"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1Name", List.of("Kyds R Us"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1Phone", List.of("(612) 222-1111"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1Street", List.of("2022 Hemlock St"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1City", List.of("Some City"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1State", List.of("MN"), SINGLE_VALUE, 1),
				new DocumentField("childNeedsChildcare", "provider1ZipCode", List.of("55456"), SINGLE_VALUE, 1))));
	}

}
