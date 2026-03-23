
package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;


class MilitaryServicePreparerTest {

  private final MilitaryServicePreparer preparer = new MilitaryServicePreparer();


  //Applicant lives alone, answered No 
  @Test
  void shouldMapApplicantHasMilitaryServiceFalseWhenLivingAlone() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPersonalInfo()
        .withPageData("militaryService", "hasMilitaryService", "false")
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).containsExactlyInAnyOrder(
        new DocumentField(
            "militaryService",
            "applicantHasMilitaryService",
            List.of("false"),
            DocumentFieldType.SINGLE_VALUE,
            null
        )
    );
  }

  // Applicant lives alone, answered Yes 
  @Test
  void shouldMapApplicantHasMilitaryServiceTrueWhenLivingAlone() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPersonalInfo()
        .withPageData("militaryService", "hasMilitaryService", "true")
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).containsExactlyInAnyOrder(
        new DocumentField(
            "militaryService",
            "applicantHasMilitaryService",
            List.of("true"),
            DocumentFieldType.SINGLE_VALUE,
            null
        )
    );
  }

  // WITH HOUSEHOLD: whoHasMilitaryService page was shown
  @Test
  void shouldMapApplicantAndHouseholdMembersFromWhoHasMilitaryService() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPersonalInfo()
        .withMultipleHouseholdMembers()
        .withPageData("militaryService", "hasMilitaryService", "true")
        .build();

    // Get the real household member IDs (generated when subworkflow is created)
    String member1Id = applicationData.getSubworkflows().get("household").get(1).getId().toString();

    // Add whoHasMilitaryService: applicant + second household member (Daria Agàta) selected
    PageData whoHasPage = new PageData();
    whoHasPage.put("whoHasMilitaryService",
        new InputData(List.of("Jane Doe applicant", "Daria Agàta " + member1Id)));
    applicationData.getPagesData().put("whoHasMilitaryService", whoHasPage);

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    // Applicant served (in selected list)
    assertThat(result).contains(
        new DocumentField(
            "militaryService",
            "applicantHasMilitaryService",
            List.of("true"),
            DocumentFieldType.SINGLE_VALUE,
            null
        )
    );

    // Household member 0 (Other Person) - not in selected list
    assertThat(result).contains(
        new DocumentField(
            "militaryService",
            "hasMilitaryService",
            List.of("false"),
            DocumentFieldType.SINGLE_VALUE,
            0
        )
    );

    // Household member 1 (Daria Agàta) - in selected list
    assertThat(result).contains(
        new DocumentField(
            "militaryService",
            "hasMilitaryService",
            List.of("true"),
            DocumentFieldType.SINGLE_VALUE,
            1
        )
    );
  }

  /**
   * Household has members; applicant answered Yes to military service but only selected
   * the first household member (not themselves). applicantHasMilitaryService = "false".
   */
  @Test
  void shouldMapApplicantFalseWhenOnlyHouseholdMemberSelected() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPersonalInfo()
        .withMultipleHouseholdMembers()
        .withPageData("militaryService", "hasMilitaryService", "true")
        .build();

    String member0Id = applicationData.getSubworkflows().get("household").get(0).getId().toString();
    PageData whoHasPage = new PageData();
    whoHasPage.put("whoHasMilitaryService",
        new InputData(List.of("Other Person " + member0Id)));  
    applicationData.getPagesData().put("whoHasMilitaryService", whoHasPage);

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    // Applicant NOT in selected list
    assertThat(result).contains(
        new DocumentField(
            "militaryService",
            "applicantHasMilitaryService",
            List.of("false"),
            DocumentFieldType.SINGLE_VALUE,
            null
        )
    );

    // First household member (index 0) - selected
    assertThat(result).contains(
        new DocumentField(
            "militaryService",
            "hasMilitaryService",
            List.of("true"),
            DocumentFieldType.SINGLE_VALUE,
            0
        )
    );
    
    // Second household member (index 1) - Not selected
    assertThat(result).contains(
            new DocumentField(
                "militaryService",
                "hasMilitaryService",
                List.of("false"),
                DocumentFieldType.SINGLE_VALUE,
                1
            )
        );
  }

  /**
   * Household has members; applicant answered Yes but selected nobody on whoHasMilitaryService.
   * (Edge case: could happen since validation allows empty)
   */
  @Test
  void shouldMapAllFalseWhenNoOneSelectedOnWhoHasMilitaryService() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPersonalInfo()
        .withMultipleHouseholdMembers()
        .withPageData("militaryService", "hasMilitaryService", "true")
        .withPageData("whoHasMilitaryService", "whoHasMilitaryService", List.of())
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).contains(
        new DocumentField(
            "militaryService",
            "applicantHasMilitaryService",
            List.of("false"),
            DocumentFieldType.SINGLE_VALUE,
            null
        )
    );
    assertThat(result).contains(
        new DocumentField(
            "militaryService",
            "hasMilitaryService",
            List.of("false"),
            DocumentFieldType.SINGLE_VALUE,
            0
        )
    );
    assertThat(result).contains(
        new DocumentField(
            "militaryService",
            "hasMilitaryService",
            List.of("false"),
            DocumentFieldType.SINGLE_VALUE,
            1
        )
    );
  }
}