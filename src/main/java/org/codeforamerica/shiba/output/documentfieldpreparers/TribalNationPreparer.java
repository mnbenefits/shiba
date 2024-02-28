package org.codeforamerica.shiba.output.documentfieldpreparers;

import java.util.ArrayList;
import java.util.List;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.springframework.stereotype.Component;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getBooleanValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.LIVING_IN_TRIBAL_NATION_BOUNDARY;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.SELECTED_TRIBAL_NATION;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.TRIBAL_NATION;

@Component
public class TribalNationPreparer implements DocumentFieldPreparer {

    private static final List<String> TRIBES_REQUIRING_BOUNDARY_ANSWER = List.of(
    		 "Lower Sioux", "Upper Sioux", "Prairie Island","Red Lake Nation","Shakopee Mdewakanton"
    );

    @Override
    

    public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
        List<DocumentField> result = new ArrayList<>();

        // Initialize the value to null
        String livesOnReservationValue = null;

        // Retrieve the values from the application data
            Boolean isTribalNationMember = getBooleanValue(application.getApplicationData().getPagesData(), TRIBAL_NATION);
            
            if (!Boolean.TRUE.equals(isTribalNationMember)) {
            	result.add(new DocumentField("nationsBoundary", "livingInNationBoundary", " ", DocumentFieldType.SINGLE_VALUE));
            }else {
            String selectedTribe = getFirstValue(application.getApplicationData().getPagesData(), SELECTED_TRIBAL_NATION);
            Boolean livesInNationBoundary = getBooleanValue(application.getApplicationData().getPagesData(), LIVING_IN_TRIBAL_NATION_BOUNDARY);

       boolean tribeRequiresBoundaryAnswer = TRIBES_REQUIRING_BOUNDARY_ANSWER.contains(selectedTribe);

        // Set the value based on the conditions
        if (Boolean.TRUE.equals(isTribalNationMember)) {
            // If the tribe requires a boundary answer and the user lives in the boundary, set to "Yes"
            if (tribeRequiresBoundaryAnswer &&  livesInNationBoundary) {
                livesOnReservationValue = "Yes";
            }
            // If the tribe requires a boundary answer and the user does not live in the boundary, set to "No"
            else if (tribeRequiresBoundaryAnswer && !livesInNationBoundary) {
                livesOnReservationValue = "No";
            }
        }
        
        // If the value has been set (not null), add the DocumentField to the result
        if (livesOnReservationValue != null) {
            result.add(new DocumentField("nationsBoundary", "livingInNationBoundary", livesOnReservationValue, DocumentFieldType.SINGLE_VALUE));
        }

        // The "WHICH_TRIBAL_NATION" field is only added if the user lives within the nation's boundary and the tribe requires a boundary answer
        if ("Yes".equals(livesOnReservationValue)) {
            result.add(new DocumentField("nationsBoundary", "selectedNationBoundaryTribe", selectedTribe,
                            DocumentFieldType.SINGLE_VALUE));
        }
            }
        return result;
    }
}
