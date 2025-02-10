package org.codeforamerica.shiba.output.documentfieldpreparers;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.springframework.stereotype.Component;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getBooleanValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.LIVING_IN_TRIBAL_NATION_BOUNDARY;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.SELECTED_TRIBAL_NATION;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.TRIBAL_NATION;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.NATION_OF_RESIDENCE;
import java.util.ArrayList;
import java.util.List;

@Component
public class TribalNationPreparer implements DocumentFieldPreparer {

	private static final List<String> TRIBES_REQUIRING_BOUNDARY_ANSWER = List.of("Lower Sioux", "Upper Sioux",
			"Prairie Island", "Red Lake Nation", "Shakopee Mdewakanton");

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		List<DocumentField> result = new ArrayList<>();
		PagesData pagesData = application.getApplicationData().getPagesData();
		boolean isTribalNationMember = getBooleanValue(pagesData, TRIBAL_NATION);

		// If not a Tribal Nation member there are no fields to generate so return
		if (!isTribalNationMember) {
			return result;
		}

		/*
		 * At this point it is a given that Tribal Nation membership exists. If the
		 * nationsBoundary page exists we must use its Yes\No response to set the CAF's
		 * "do you live on a reservation" Yes\No radio field. Thus, regarding backward
		 * compatibility, we may end up with regenerated (old) applications that have
		 * the No radio field set that would have had neither the Yes or No field set in
		 * the past. When we get a Yes response on the nationsBoundary page we use the
		 * existence of the nationOfResidence page to determine how we set the
		 * "reservation" field in the CAF.
		 */

		boolean nationsBoundaryPageExists = pagesData.getPage("nationsBoundary") != null ? true : false;
		if (nationsBoundaryPageExists) {
			boolean livesInNationBoundary = getBooleanValue(pagesData, LIVING_IN_TRIBAL_NATION_BOUNDARY);
			result.add(new DocumentField("nationsBoundary", "boundaryMember", livesInNationBoundary ? "Yes" : "No",
					DocumentFieldType.SINGLE_VALUE));
			if (livesInNationBoundary) {
				boolean nationOfResidencePageExists = pagesData.getPage("nationOfResidence") != null ? true : false;
				if (nationOfResidencePageExists) {
					String selectedNationOfResidence = getFirstValue(pagesData, NATION_OF_RESIDENCE);
					result.add(new DocumentField("nationsBoundary", "selectedNationBoundaryTribe",
							selectedNationOfResidence, DocumentFieldType.SINGLE_VALUE));
				} else {
					// the old way, only provide the reservation name if it is one of the "5 tribes"
					String selectedTribe = getFirstValue(pagesData, SELECTED_TRIBAL_NATION);
					if (TRIBES_REQUIRING_BOUNDARY_ANSWER.contains(selectedTribe)) {
						result.add(new DocumentField("nationsBoundary", "selectedNationBoundaryTribe", selectedTribe,
								DocumentFieldType.SINGLE_VALUE));
					}
				}
			}

		}

		return result;
	}

}
