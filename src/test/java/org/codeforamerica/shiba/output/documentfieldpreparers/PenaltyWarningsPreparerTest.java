package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

class PenaltyWarningsPreparerTest {

	private final PenaltyWarningsPreparer preparer = new PenaltyWarningsPreparer();

	@Test
	void shouldReturnEmptyListWhenPageIsNull() {
		ApplicationData applicationData = new ApplicationData();
		Application application = Application.builder().applicationData(applicationData).build();

		List<DocumentField> result = preparer.prepareDocumentFields(application, null, null);

		assertThat(result).isEmpty();
	}

	@Test
	void shouldReturnEmptyListWhenAllAnswersAreNo() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("penaltyWarnings", "disqualifiedPublicAssistance", List.of("false"))
				.withPageData("penaltyWarnings", "fraudulentStatements", List.of("false"))
				.withPageData("penaltyWarnings", "hidingFromLaw", List.of("false"))
				.withPageData("penaltyWarnings", "drugFelonyConviction", List.of("false"))
				.withPageData("penaltyWarnings", "violatingParole", List.of("false")).build();

		Application application = Application.builder().applicationData(applicationData).build();

		List<DocumentField> result = preparer.prepareDocumentFields(application, null, null);

		assertThat(result).isEmpty();
	}

	@Test
	void shouldCreateFieldForApplicantWhenYesAndNoHouseholdMembersSelected() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("personalInfo", "firstName", "John").withPageData("personalInfo", "lastName", "Doe")
				.withPageData("penaltyWarnings", "disqualifiedPublicAssistance", List.of("true"))
				.withPageData("penaltyWarnings", "fraudulentStatements", List.of("true"))
				.withPageData("penaltyWarnings", "hidingFromLaw", List.of("true"))
				.withPageData("penaltyWarnings", "drugFelonyConviction", List.of("true"))
				.withPageData("penaltyWarnings", "violatingParole", List.of("true")).build();

		Application application = Application.builder().applicationData(applicationData).build();

		List<DocumentField> result = preparer.prepareDocumentFields(application, null, null);

		assertThat(result).containsExactly(new DocumentField("penaltyWarnings", "questionNumber", "1", SINGLE_VALUE, 0),
				new DocumentField("penaltyWarnings", "householdMembers", "John Doe", SINGLE_VALUE, 0),
				new DocumentField("penaltyWarnings", "questionNumber", "2", SINGLE_VALUE, 1),
				new DocumentField("penaltyWarnings", "householdMembers", "John Doe", SINGLE_VALUE, 1),
				new DocumentField("penaltyWarnings", "questionNumber", "3", SINGLE_VALUE, 2),
				new DocumentField("penaltyWarnings", "householdMembers", "John Doe", SINGLE_VALUE, 2),
				new DocumentField("penaltyWarnings", "questionNumber", "4", SINGLE_VALUE, 3),
				new DocumentField("penaltyWarnings", "householdMembers", "John Doe", SINGLE_VALUE, 3),
				new DocumentField("penaltyWarnings", "questionNumber", "5", SINGLE_VALUE, 4),
				new DocumentField("penaltyWarnings", "householdMembers", "John Doe", SINGLE_VALUE, 4));
	}

	@Test
	void shouldCreateFieldForEachSelectedHouseholdMemberAndUseCorrectQuestionNumbers() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("penaltyWarnings", "hidingFromLaw",
						List.of("true", "Jane Smith applicant", "Bob Johnson id-123"))
				.withPageData("penaltyWarnings", "fraudulentStatements", List.of("false"))
				.withPageData("penaltyWarnings", "violatingParole",
						List.of("true", "Jane Smith applicant", "Bob Johnson id-123", "misra Kabeto id-123"))
				.withPageData("penaltyWarnings", "disqualifiedPublicAssistance",
						List.of("true", "Jane Smith applicant", "Bob Johnson id-123"))
				.withPageData("penaltyWarnings", "drugFelonyConviction",
						List.of("true", "Jane Smith applicant", "Bob Johnson id-123"))
				.build();

		Application application = Application.builder().applicationData(applicationData).build();

		List<DocumentField> result = preparer.prepareDocumentFields(application, null, null);

		assertThat(result).containsExactlyInAnyOrder(
				new DocumentField("penaltyWarnings", "questionNumber", "1", SINGLE_VALUE, 0),
				new DocumentField("penaltyWarnings", "householdMembers", "Jane Smith", SINGLE_VALUE, 0),
				new DocumentField("penaltyWarnings", "questionNumber", "1", SINGLE_VALUE, 1),
				new DocumentField("penaltyWarnings", "householdMembers", "Bob Johnson", SINGLE_VALUE, 1),
				new DocumentField("penaltyWarnings", "questionNumber", "3", SINGLE_VALUE, 2),
				new DocumentField("penaltyWarnings", "householdMembers", "Jane Smith", SINGLE_VALUE, 2),
				new DocumentField("penaltyWarnings", "questionNumber", "3", SINGLE_VALUE, 3),
				new DocumentField("penaltyWarnings", "householdMembers", "Bob Johnson", SINGLE_VALUE, 3),
				new DocumentField("penaltyWarnings", "questionNumber", "4", SINGLE_VALUE, 4),
				new DocumentField("penaltyWarnings", "householdMembers", "Jane Smith", SINGLE_VALUE, 4),
				new DocumentField("penaltyWarnings", "questionNumber", "4", SINGLE_VALUE, 5),
				new DocumentField("penaltyWarnings", "householdMembers", "Bob Johnson", SINGLE_VALUE, 5),
				new DocumentField("penaltyWarnings", "questionNumber", "5", SINGLE_VALUE, 6),
				new DocumentField("penaltyWarnings", "householdMembers", "Jane Smith", SINGLE_VALUE, 6),
				new DocumentField("penaltyWarnings", "questionNumber", "5", SINGLE_VALUE, 7),
				new DocumentField("penaltyWarnings", "householdMembers", "Bob Johnson", SINGLE_VALUE, 7),
				new DocumentField("penaltyWarnings", "questionNumber", "5", SINGLE_VALUE, 8),
				new DocumentField("penaltyWarnings", "householdMembers", "misra Kabeto", SINGLE_VALUE, 8));
	}

	@Test
	void shouldHandleMixedYesAndNoAnswers() {
		ApplicationData applicationData = new TestApplicationDataBuilder()
				.withPageData("personalInfo", "firstName", "John").withPageData("personalInfo", "lastName", "Doe")
				.withPageData("penaltyWarnings", "disqualifiedPublicAssistance", List.of("true"))
				.withPageData("penaltyWarnings", "fraudulentStatements", List.of("false"))
				.withPageData("penaltyWarnings", "hidingFromLaw", List.of("true"))
				.withPageData("penaltyWarnings", "drugFelonyConviction", List.of("false"))
				.withPageData("penaltyWarnings", "violatingParole", List.of("true")).build();

		Application application = Application.builder().applicationData(applicationData).build();

		List<DocumentField> result = preparer.prepareDocumentFields(application, null, null);

		assertThat(result).containsExactlyInAnyOrder(
				new DocumentField("penaltyWarnings", "questionNumber", "1", SINGLE_VALUE, 0),
				new DocumentField("penaltyWarnings", "householdMembers", "John Doe", SINGLE_VALUE, 0),
				new DocumentField("penaltyWarnings", "questionNumber", "3", SINGLE_VALUE, 1),
				new DocumentField("penaltyWarnings", "householdMembers", "John Doe", SINGLE_VALUE, 1),
				new DocumentField("penaltyWarnings", "questionNumber", "5", SINGLE_VALUE, 2),
				new DocumentField("penaltyWarnings", "householdMembers", "John Doe", SINGLE_VALUE, 2));
	}

}
