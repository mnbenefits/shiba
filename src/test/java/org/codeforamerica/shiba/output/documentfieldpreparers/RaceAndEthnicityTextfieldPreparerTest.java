package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RaceAndEthnicityTextfieldPreparerTest {
    private final RaceAndEthnicityTextfieldPreparer preparer = new RaceAndEthnicityTextfieldPreparer();
    private final TestApplicationDataBuilder testApplicationDataBuilder = new TestApplicationDataBuilder();
    private Application application;

    @BeforeEach
    void setUp() {
        application = Application.builder().applicationData(testApplicationDataBuilder.base().build()).build();
    }

   
    @Test
    void shouldOnlyShowSelectedOption() {
      application.setApplicationData(testApplicationDataBuilder
          .withPageData("raceAndEthnicity", "race", List.of("ASIAN"))
          .build());
      
      assertThat(preparer.prepareDocumentFields(application, Document.CERTAIN_POPS, Recipient.CLIENT)).isEqualTo(
          List.of(new DocumentField("raceAndEthnicity", "applicantRaceAndEthnicity", "Asian", SINGLE_VALUE))
      );
    }
    
    @Test
    void shouldOnlyShowWriteOption() {
      application.setApplicationData(testApplicationDataBuilder
    	  .withPageData("raceAndEthnicity", "race", List.of("SOME_OTHER_RACE_OR_ETHNICITY"))
          .withPageData("raceAndEthnicity", "otherRaceOrEthnicity", "SomeOtherRaceOrEthnicity")
          .build());
      
      assertThat(preparer.prepareDocumentFields(application, Document.CERTAIN_POPS, Recipient.CLIENT)).isEqualTo(
          List.of(new DocumentField("raceAndEthnicity", "applicantRaceAndEthnicity", "SomeOtherRaceOrEthnicity", SINGLE_VALUE))
      );
    }
    
    @Test
    void shouldCombineSelectedOptionAndWriteOption() {
      application.setApplicationData(testApplicationDataBuilder
          .withPageData("raceAndEthnicity", "race", List.of("MIDDLE_EASTERN_OR_NORTH_AFRICAN","SOME_OTHER_RACE_OR_ETHNICITY"))
          .withPageData("raceAndEthnicity", "otherRaceOrEthnicity", "SomeOtherRaceOrEthnicity")
          .build());
      
      assertThat(preparer.prepareDocumentFields(application, Document.CERTAIN_POPS, Recipient.CLIENT)).isEqualTo(
          List.of(new DocumentField("raceAndEthnicity", "applicantRaceAndEthnicity", "Middle Eastern / N. African, SomeOtherRaceOrEthnicity", SINGLE_VALUE))
      );
    }

    @Test
    void shouldCombineSelectedOptionsAndWriteOption() {
    	application.setApplicationData(testApplicationDataBuilder
	      .withPageData("raceAndEthnicity", "race", List.of("ASIAN", "SOME_OTHER_RACE_OR_ETHNICITY"))
	      .withPageData("raceAndEthnicity", "ethnicity", List.of("HISPANIC_OR_LATINO"))
          .withPageData("raceAndEthnicity", "otherRaceOrEthnicity", "SomeOtherRaceOrEthnicity")
          .build());
      
      assertThat(preparer.prepareDocumentFields(application, Document.CERTAIN_POPS, Recipient.CLIENT)).isEqualTo(
          List.of(new DocumentField("raceAndEthnicity", "applicantRaceAndEthnicity", "Hispanic, Latino, or Spanish, Asian, SomeOtherRaceOrEthnicity", SINGLE_VALUE))
      );
    }
    
    @Test
    void shouldCombineSelectedOptions() {
    	application.setApplicationData(testApplicationDataBuilder
          .withPageData("raceAndEthnicity", "race", List.of("AMERICAN_INDIAN_OR_ALASKA_NATIVE", "BLACK_OR_AFRICAN_AMERICAN", "NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER"))
          .build());
      
      assertThat(preparer.prepareDocumentFields(application, Document.CERTAIN_POPS, Recipient.CLIENT)).isEqualTo(
          List.of(new DocumentField("raceAndEthnicity", "applicantRaceAndEthnicity", "American Indian or Alaska Native, Black or African American, "
          		+ "Native Hawaiian or Pacific Islander", SINGLE_VALUE))
      );
    }
}