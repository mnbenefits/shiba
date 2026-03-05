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

            for (String race : RACE_VALUES) {
                boolean selected;
                if (race.equals("WHITE")) {
                    selected = memberRaces.contains("WHITE") || memberRaces.contains("MIDDLE_EASTERN_OR_NORTH_AFRICAN");
                } else if (race.equals("SOME_OTHER_RACE_OR_ETHNICITY")) {
                    // write Yes to the field but don't populate the text
                    selected = memberRaces.contains("SOME_OTHER_RACE_OR_ETHNICITY");
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
        }

        return result;
    }
}