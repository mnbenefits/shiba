package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.List;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NonSelfEmploymentPreparerTest {

  private NonSelfEmploymentPreparer preparer;

  @BeforeEach
  public void setup() {
    preparer = new NonSelfEmploymentPreparer();
  }

  @Test
  public void shouldMapValuesIfNonSelfEmployedJobExists() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withSubworkflow("jobs",
            new PagesDataBuilder().withHourlyJob("true", "10", "10"),
            new PagesDataBuilder().withNonHourlyJob("false", "12", "EVERY_WEEK"))
        .build();
    Application application = Application.builder().applicationData(applicationData).build();

    List<DocumentField> actual =
        preparer.prepareDocumentFields(application, null, Recipient.CLIENT);

    assertThat(actual).containsExactlyInAnyOrder(
        new DocumentField("nonSelfEmployment_incomePerPayPeriod", "incomePerPayPeriod", "12",
            SINGLE_VALUE, 0),
        new DocumentField("nonSelfEmployment_incomePerPayPeriod", "incomePerPayPeriod_EVERY_WEEK", "12",
            SINGLE_VALUE, 0),
        new DocumentField("nonSelfEmployment_payPeriod", "payPeriod", "EVERY_WEEK",
            SINGLE_VALUE, 0),
        new DocumentField("nonSelfEmployment_paidByTheHour", "paidByTheHour", "false",
            SINGLE_VALUE, 0),
        new DocumentField("nonSelfEmployment_selfEmployment", "selfEmployment", "false",
            SINGLE_VALUE, 0)
    );
  }
  
  @Test
  void shouldMapEmptyIfNoJobs() {
    ApplicationData applicationData =
        new TestApplicationDataBuilder().withPersonalInfo().withMultipleHouseholdMembers().build();
    Application application = Application.builder().applicationData(applicationData).build();

    assertThat(preparer.prepareDocumentFields(application, null, Recipient.CLIENT)).isEmpty();
  }

}
