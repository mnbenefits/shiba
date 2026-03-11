package org.codeforamerica.shiba.output.documentfieldpreparers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.Subworkflow;
import org.springframework.stereotype.Component;

@Component
public class HouseholdRaceAndEthnicityPreparer implements DocumentFieldPreparer {

    private static final List<String> RACE_VALUES = List.of(
        "ASIAN",
        "AMERICAN_INDIAN_OR_ALASKA_NATIVE",
        "BLACK_OR_AFRICAN_AMERICAN",
        "NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER",
        "WHITE",
        "HISPANIC_LATINO_OR_SPANISH",
        "SOME_OTHER_RACE_OR_ETHNICITY"
    );

    @Override
    public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
        List<DocumentField> result = new ArrayList<>();

        Subworkflow householdSubworkflow = application.getApplicationData()
            .getSubworkflows().get("household");

        if (householdSubworkflow == null) {
            return result;
        }

        for (int i = 0; i < householdSubworkflow.size(); i++) {
            List<String> memberRaces = Optional
                .ofNullable(householdSubworkflow.get(i).getPagesData().getPage("householdRaceAndEthnicity"))
                .map(page -> page.get("householdRaceAndEthnicity"))
                .map(InputData::getValue)
                .orElse(List.of());

            // Check if preferNotToSay was selected
            List<String> preferNotToSay = Optional
                .ofNullable(householdSubworkflow.get(i).getPagesData().getPage("householdRaceAndEthnicity"))
                .map(page -> page.get("preferNotToSay"))
                .map(InputData::getValue)
                .orElse(List.of());

            for (String race : RACE_VALUES) {
                boolean selected;

                if (race.equals("WHITE")) {
                    selected = memberRaces.contains("WHITE") || memberRaces.contains("MIDDLE_EASTERN_OR_NORTH_AFRICAN");
                } else if (race.equals("SOME_OTHER_RACE_OR_ETHNICITY")) {
                    selected = memberRaces.contains("SOME_OTHER_RACE_OR_ETHNICITY");
                    if (selected) {
                        String otherValue = Optional
                            .ofNullable(householdSubworkflow.get(i).getPagesData().getPage("householdRaceAndEthnicity"))
                            .map(page -> page.get("otherRaceOrEthnicity"))
                            .map(InputData::getValue)
                            .map(values -> values.isEmpty() ? "" : values.get(0))
                            .orElse("");
                        result.add(new DocumentField(
                            "raceAndEthnicity",
                            "CLIENT_REPORTED",
                            otherValue,
                            DocumentFieldType.SINGLE_VALUE,
                            i
                        ));
                    }
                } else if (race.equals("HISPANIC_LATINO_OR_SPANISH")) {
                    selected = memberRaces.contains("HISPANIC_LATINO_OR_SPANISH");
                    if (!selected) {
                        result.add(new DocumentField(
                            "raceAndEthnicity",
                            "HISPANIC_LATINO_OR_SPANISH_NO",
                            "true",
                            DocumentFieldType.ENUMERATED_SINGLE_VALUE,
                            i
                        ));
                    }
                } else {
                    selected = memberRaces.contains(race);
                }

                result.add(new DocumentField(
                    "raceAndEthnicity",
                    race,
                    selected ? "Yes" : "No",
                    DocumentFieldType.SINGLE_VALUE,
                    i
                ));
            }

            // UNABLE_TO_DETERMINE: when Hispanic is the only race selected, or preferNotToSay
            boolean hispanicOnly = memberRaces.size() == 1
                && memberRaces.contains("HISPANIC_LATINO_OR_SPANISH");
            boolean prefersNotToSay = !preferNotToSay.isEmpty();

            if (hispanicOnly || prefersNotToSay) {
                result.add(new DocumentField(
                    "raceAndEthnicity",
                    "UNABLE_TO_DETERMINE",
                    "true",
                    DocumentFieldType.ENUMERATED_SINGLE_VALUE,
                    i
                ));
            }

            // Handle Middle Eastern / N. African as CLIENT_REPORTED when it's the only selection
            if (memberRaces.size() == 1 && memberRaces.contains("MIDDLE_EASTERN_OR_NORTH_AFRICAN")) {
                result.add(new DocumentField(
                    "raceAndEthnicity",
                    "CLIENT_REPORTED",
                    "Middle Eastern / N. African",
                    DocumentFieldType.SINGLE_VALUE,
                    i
                ));
            }
        }

        return result;
    }
}