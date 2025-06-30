package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.ArrayList;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommunicationsOptInPreparerTest {

	private CommunicationsOptInPreparer communicationsOptInPreparer;

	@BeforeEach
	void setUp() {
		communicationsOptInPreparer = new CommunicationsOptInPreparer();
	}

	@Test
	void shouldReturnOptInTextAndEmail() {
		ApplicationData appData = new TestApplicationDataBuilder()
				.withPageData("contactInfo", "phoneNumber", "888-888-8888")
				.withPageData("contactInfo", "email", "me@test.local")
				.withPageData("contactInfo", "phoneOrEmail", List.of("TEXT", "EMAIL")).build();
		Application application = Application.builder().applicationData(appData).build();

		// build a list of what is expected
		ArrayList<DocumentField> expectedDocumentFields = new ArrayList<DocumentField>();
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInEmail", "true", SINGLE_VALUE));
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInEmailAddress",
				List.of("me@test.local"), SINGLE_VALUE));
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInPhone", "true", SINGLE_VALUE));
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInPhoneNumber",
				List.of("888-888-8888"), SINGLE_VALUE));

		// use the preparer to generate the list of DocumentFields and assert that it
		// matches the expected
		List<DocumentField> actualDocumentFields = communicationsOptInPreparer.prepareDocumentFields(application, null,
				Recipient.CLIENT);
		assertThat(actualDocumentFields.size()).isEqualTo(4);
		assertThat(actualDocumentFields).containsAll(expectedDocumentFields);
	}

	@Test
	void shouldReturnNeitherOptInTextOrEmail() {
		ApplicationData appData = new TestApplicationDataBuilder()
				.withPageData("contactInfo", "phoneNumber", "888-888-8888")
				.withPageData("contactInfo", "email", "me@test.local")
				.withPageData("contactInfo", "phoneOrEmail", List.of("")).build();
		Application application = Application.builder().applicationData(appData).build();

		// build a list of what is expected
		ArrayList<DocumentField> expectedDocumentFields = new ArrayList<DocumentField>();
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInEmail", "false", SINGLE_VALUE));
		expectedDocumentFields
				.add(new DocumentField("communicationsOptIn", "commOptInEmailAddress", List.of(""), SINGLE_VALUE));
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInPhone", "false", SINGLE_VALUE));
		expectedDocumentFields
				.add(new DocumentField("communicationsOptIn", "commOptInPhoneNumber", List.of(""), SINGLE_VALUE));

		// use the preparer to generate the list of DocumentFields and assert that it matches the expected
		List<DocumentField> actualDocumentFields = communicationsOptInPreparer.prepareDocumentFields(application, null,
				Recipient.CLIENT);
		assertThat(actualDocumentFields.size()).isEqualTo(4);
		assertThat(actualDocumentFields).containsAll(expectedDocumentFields);
	}

	@Test
	void shouldReturnOptInTextNotOptInEmail() {
		ApplicationData appData = new TestApplicationDataBuilder()
				.withPageData("contactInfo", "phoneNumber", "888-888-8888")
				.withPageData("contactInfo", "email", "me@test.local")
				.withPageData("contactInfo", "phoneOrEmail", List.of("TEXT")).build();
		Application application = Application.builder().applicationData(appData).build();

		// build a list of what is expected
		ArrayList<DocumentField> expectedDocumentFields = new ArrayList<DocumentField>();
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInEmail", "false", SINGLE_VALUE));
		expectedDocumentFields
				.add(new DocumentField("communicationsOptIn", "commOptInEmailAddress", List.of(""), SINGLE_VALUE));
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInPhone", "true", SINGLE_VALUE));
		expectedDocumentFields
				.add(new DocumentField("communicationsOptIn", "commOptInPhoneNumber", List.of("888-888-8888"), SINGLE_VALUE));

		// use the preparer to generate the list of DocumentFields and assert that it matches the expected
		List<DocumentField> actualDocumentFields = communicationsOptInPreparer.prepareDocumentFields(application, null,
				Recipient.CLIENT);
		assertThat(actualDocumentFields.size()).isEqualTo(4);
		assertThat(actualDocumentFields).containsAll(expectedDocumentFields);
	}

	@Test
	void shouldReturnOptInEmailNotOptInText() {
		ApplicationData appData = new TestApplicationDataBuilder()
				.withPageData("contactInfo", "phoneNumber", "888-888-8888")
				.withPageData("contactInfo", "email", "me@test.local")
				.withPageData("contactInfo", "phoneOrEmail", List.of("EMAIL")).build();
		Application application = Application.builder().applicationData(appData).build();

		// build a list of what is expected
		ArrayList<DocumentField> expectedDocumentFields = new ArrayList<DocumentField>();
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInEmail", "true", SINGLE_VALUE));
		expectedDocumentFields
				.add(new DocumentField("communicationsOptIn", "commOptInEmailAddress", List.of("me@test.local"), SINGLE_VALUE));
		expectedDocumentFields.add(new DocumentField("communicationsOptIn", "commOptInPhone", "false", SINGLE_VALUE));
		expectedDocumentFields
				.add(new DocumentField("communicationsOptIn", "commOptInPhoneNumber", List.of(""), SINGLE_VALUE));

		// use the preparer to generate the list of DocumentFields and assert that it matches the expected
		List<DocumentField> actualDocumentFields = communicationsOptInPreparer.prepareDocumentFields(application, null,
				Recipient.CLIENT);
		assertThat(actualDocumentFields.size()).isEqualTo(4);
		assertThat(actualDocumentFields).containsAll(expectedDocumentFields);
	}

}
