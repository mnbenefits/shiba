package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.output.DocumentFieldType.ENUMERATED_SINGLE_VALUE;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HouseholdRaceAndEthnicityPreparerTest {
	 private final HouseholdRaceAndEthnicityPreparer preparer = new HouseholdRaceAndEthnicityPreparer();
	    private TestApplicationDataBuilder testApplicationDataBuilder;

	    @BeforeEach
	    void setUp() {
	        testApplicationDataBuilder = new TestApplicationDataBuilder();
	    }

	    @Test
	    void shouldReturnEmptyListWhenNoHouseholdSubworkflow() {
	        ApplicationData applicationData = new ApplicationData();
	        Application application = Application.builder()
	            .applicationData(applicationData)
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CCAP, Recipient.CLIENT);

	        assertThat(result).isEmpty();
	    }

	    @Test
	    void shouldMapAllRacesAsNoWhenNoneSelected() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	                .withSubworkflow("household", new PagesDataBuilder()
	                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", ""))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "ASIAN", "No", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "AMERICAN_INDIAN_OR_ALASKA_NATIVE", "No", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "BLACK_OR_AFRICAN_AMERICAN", "No", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER", "No", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "WHITE", "No", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "SOME_OTHER_RACE_OR_ETHNICITY", "No", SINGLE_VALUE, 0)
	        );
	    }

	    @Test
	    void shouldMarkSelectedRaceAsYes() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	                .withSubworkflow("household", new PagesDataBuilder()
	                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "ASIAN"))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "ASIAN", "Yes", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "AMERICAN_INDIAN_OR_ALASKA_NATIVE", "No", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "BLACK_OR_AFRICAN_AMERICAN", "No", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER", "No", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "WHITE", "No", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "SOME_OTHER_RACE_OR_ETHNICITY", "No", SINGLE_VALUE, 0)
	        );
	    }

	    @Test
	    void shouldMapMiddleEasternOrNorthAfricanAsWhite() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "MIDDLE_EASTERN_OR_NORTH_AFRICAN"))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "WHITE", "Yes", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "CLIENT_REPORTED", "Middle Eastern / N. African", SINGLE_VALUE, 0)
	        );
	    }

	    @Test
	    void shouldMapWhiteAsYesWhenWhiteSelected() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "WHITE"))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "WHITE", "Yes", SINGLE_VALUE, 0)
	        );
	    }

	    @Test
	    void shouldIncludeOtherRaceTextFieldWhenSomeOtherRaceSelected() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", Map.of("householdRaceAndEthnicity", "SOME_OTHER_RACE_OR_ETHNICITY",
		                				"otherRaceOrEthnicity", "Somali")))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "CLIENT_REPORTED", "Somali", SINGLE_VALUE, 0)
	        );
	    }

	    @Test
	    void shouldNotIncludeOtherRaceTextFieldWhenSomeOtherRaceNotSelected() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "ASIAN"))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).noneMatch(field -> field.getName().equals("CLIENT_REPORTED"));
	    }

	    @Test
	    void shouldAddHispanicNoFieldWhenHispanicNotSelected() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "ASIAN"))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "HISPANIC_LATINO_OR_SPANISH_NO", "true", ENUMERATED_SINGLE_VALUE, 0)
	        );
	    }

	    @Test
	    void shouldNotAddHispanicNoFieldWhenHispanicSelected() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "HISPANIC_LATINO_OR_SPANISH"))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).noneMatch(field -> field.getName().equals("HISPANIC_LATINO_OR_SPANISH_NO"));
	    }

	    @Test
	    void shouldAddUnableToDetermineWhenHispanicIsOnlyRaceSelected() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "HISPANIC_LATINO_OR_SPANISH"))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "UNABLE_TO_DETERMINE", "true", ENUMERATED_SINGLE_VALUE, 0)
	        );
	    }

	    @Test
	    void shouldNotAddUnableToDetermineWhenHispanicSelectedWithOtherRaces() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "HISPANIC_LATINO_OR_SPANISH"))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).anyMatch(field -> field.getName().equals("UNABLE_TO_DETERMINE"));
	    }

	    @Test
	    void shouldAddUnableToDetermineWhenPreferNotToSaySelected() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", "preferNotToSay", "true"))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).noneMatch(field -> field.getName().equals("UNABLE_TO_DETERMINE"));
	    }

	    @Test
	    void shouldGenerateFieldsForEachHouseholdMemberWithCorrectIndex() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	            		.withSubworkflow("household", new PagesDataBuilder()
		                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "ASIAN")
		                		, new PagesDataBuilder().withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity", "BLACK_OR_AFRICAN_AMERICAN"))
	                .build())
	            .build();
	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        // First household member (index 0)
	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "ASIAN", "Yes", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "BLACK_OR_AFRICAN_AMERICAN", "No", SINGLE_VALUE, 0)
	        );

	        // Second household member (index 1)
	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "ASIAN", "No", SINGLE_VALUE, 1),
	            new DocumentField("raceAndEthnicity", "BLACK_OR_AFRICAN_AMERICAN", "Yes", SINGLE_VALUE, 1)
	        );
	    }

	    @Test
	    void shouldHandleMultipleRacesSelected() {
	        Application application = Application.builder()
	            .applicationData(testApplicationDataBuilder
	                .withSubworkflow("household", new PagesDataBuilder()
	                		.withPageData("householdRaceAndEthnicity", "householdRaceAndEthnicity",
	                				List.of("ASIAN", "BLACK_OR_AFRICAN_AMERICAN", "NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER",
	                				"WHITE", "AMERICAN_INDIAN_OR_ALASKA_NATIVE")))
	                .build())
	            .build();

	        List<DocumentField> result = preparer.prepareDocumentFields(application, Document.CAF, Recipient.CLIENT);

	        assertThat(result).contains(
	            new DocumentField("raceAndEthnicity", "ASIAN", "Yes", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "BLACK_OR_AFRICAN_AMERICAN", "Yes", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER", "Yes", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "WHITE", "Yes", SINGLE_VALUE, 0),
	            new DocumentField("raceAndEthnicity", "AMERICAN_INDIAN_OR_ALASKA_NATIVE", "Yes", SINGLE_VALUE, 0)
	        );
	    }
}
