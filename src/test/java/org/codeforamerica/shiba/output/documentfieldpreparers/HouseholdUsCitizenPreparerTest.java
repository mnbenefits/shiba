package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class HouseholdUsCitizenPreparerTest {

  HouseholdUsCitizenPreparer preparer = new HouseholdUsCitizenPreparer();

  @Disabled("Citizenship is now a mandatory question - cannot be skipped")
  @Test
  void shouldParseOffWhenUsCitizenshipNotAsked() {
    ApplicationData applicationData = new ApplicationData();
    applicationData.getSubworkflows().addIteration("household", new PagesData());
    UUID householdMemberID = applicationData.getSubworkflows().get("household").get(0).getId();

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).isEqualTo(List.of(
        new DocumentField(
            "usCitizen",
            "applicantIsUsCitizen",
            List.of("Off"),
            DocumentFieldType.SINGLE_VALUE,
            null
        )
    ));
  }
    
  @Test
  void shouldParseFalseWhenApplicantIsNotUsCitizen() {
      ApplicationData applicationData = new ApplicationData();
      applicationData.getSubworkflows().addIteration("household", new PagesData());
  UUID householdMemberID = applicationData.getSubworkflows().get("household").get(0).getId();

  new TestApplicationDataBuilder(applicationData)
     .withPageData("usCitizen", "citizenshipStatus", List.of("NOT_CITIZEN", "BIRTH_RIGHT"))
      .withPageData("usCitizen", "citizenshipIdMap", List.of("applicant", householdMemberID.toString()));
 
  List<DocumentField> result = preparer.prepareDocumentFields(
	        Application.builder().applicationData(applicationData).build(), 
	        Document.CAF, 
	        null);

  assertThat(result).isEqualTo(List.of(
      new DocumentField(
          "usCitizen",
          "citizenshipStatus",
          List.of("Not_Citizen"),
          DocumentFieldType.SINGLE_VALUE,
          0
      ),
      new DocumentField(
          "usCitizen",
          "citizenshipStatus",
          List.of("Citizen"),
              DocumentFieldType.SINGLE_VALUE,
              1
          )
      ));
  }
}
