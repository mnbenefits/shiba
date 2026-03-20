package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LastSchoolGradePreparerTest {

  private LastSchoolGradePreparer preparer;

  @BeforeEach
  void setUp() {
    preparer = new LastSchoolGradePreparer();
  }

  // Helper to build an Application from ApplicationData
  private Application applicationWith(ApplicationData applicationData) {
    return Application.builder()
        .applicationData(applicationData)
        .build();
  }

  @Test
  void shouldReturnEmptyWhenNoPageDataExists() {
    ApplicationData applicationData = new ApplicationData();

    List<DocumentField> result = preparer.prepareDocumentFields(
        applicationWith(applicationData), null, null);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldCreateSingleFieldForApplicantOnly() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPageData("lastSchoolGrade", "lastSchoolGrade", List.of("12"))
        .withPageData("lastSchoolGrade", "lastSchoolGradePersonIdMap", List.of("applicant"))
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(
        applicationWith(applicationData), null, null);

    assertThat(result).containsExactly(
        new DocumentField("lastSchoolGrade", "lastSchoolGrade", List.of("12"), SINGLE_VALUE)
    );
  }

  @Test
  void shouldCreateFieldsForApplicantAndOneHouseholdMember() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPageData("lastSchoolGrade", "lastSchoolGrade", List.of("12", "10"))
        .withPageData("lastSchoolGrade", "lastSchoolGradePersonIdMap",
            List.of("applicant", "member1"))
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(
        applicationWith(applicationData), null, null);

    assertThat(result).containsExactly(
        new DocumentField("lastSchoolGrade", "lastSchoolGrade", List.of("12"), SINGLE_VALUE),
        new DocumentField("lastSchoolGrade", "lastSchoolGrade", List.of("10"), SINGLE_VALUE, 0)
    );
  }

  @Test
  void shouldCreateFieldsForApplicantAndMultipleHouseholdMembers() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPageData("lastSchoolGrade", "lastSchoolGrade", List.of("12", "9", "7"))
        .withPageData("lastSchoolGrade", "lastSchoolGradePersonIdMap",
            List.of("applicant", "member1", "member2"))
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(
        applicationWith(applicationData), null, null);

    assertThat(result).containsExactly(
        new DocumentField("lastSchoolGrade", "lastSchoolGrade", List.of("12"), SINGLE_VALUE),
        new DocumentField("lastSchoolGrade", "lastSchoolGrade", List.of("9"), SINGLE_VALUE, 0),
        new DocumentField("lastSchoolGrade", "lastSchoolGrade", List.of("7"), SINGLE_VALUE, 1)
    );
  }

  @Test
  void shouldCreateFieldsForHouseholdMembersOnlyWithNoApplicant() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPageData("lastSchoolGrade", "lastSchoolGrade", List.of("8", "6"))
        .withPageData("lastSchoolGrade", "lastSchoolGradePersonIdMap",
            List.of("member1", "member2"))
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(
        applicationWith(applicationData), null, null);

    assertThat(result).containsExactly(
        new DocumentField("lastSchoolGrade", "lastSchoolGrade", List.of("8"), SINGLE_VALUE, 0),
        new DocumentField("lastSchoolGrade", "lastSchoolGrade", List.of("6"), SINGLE_VALUE, 1)
    );
  }
}