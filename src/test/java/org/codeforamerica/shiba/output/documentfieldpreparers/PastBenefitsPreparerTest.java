package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;
import static org.codeforamerica.shiba.testutilities.TestUtils.createApplicationInputSingleValue;

import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

class PastBenefitsPreparerTest {

  private final PastBenefitsPreparer preparer = new PastBenefitsPreparer();

  @Test
  void shouldMapSingleCashAssistanceToCash() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPageData("pastBenefitDetails", "whichPastBenefits", List.of("CASH_ASSISTANCE"))
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).containsExactly(
        createApplicationInputSingleValue("pastBenefitDetails", "whichPastBenefits", "CASH"));
  }

  @Test
  void shouldMapAllThreeOptionsWhenSelected() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPageData("pastBenefitDetails", "whichPastBenefits",
            List.of("CASH_ASSISTANCE", "SNAP", "TRIBAL_COMMODITIES"))
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).containsExactly(
        createApplicationInputSingleValue("pastBenefitDetails", "whichPastBenefits",
            "CASH, SNAP, Food commodities"));
  }


  @Test
  void shouldMapEmptySelectionToBlankString() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPageData("pastBenefitDetails", "whichPastBenefits", List.of())
        .build();

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).containsExactly(
        new DocumentField("pastBenefitDetails", "whichPastBenefits", List.of(""), SINGLE_VALUE));
  }

  @Test
  void shouldReturnNothingWhenPastBenefitDetailsPageMissing() {
    ApplicationData applicationData = new ApplicationData();

    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).isEmpty();
  }
}