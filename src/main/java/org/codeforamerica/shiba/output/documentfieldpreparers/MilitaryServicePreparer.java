package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.MILITARY_SERVICE;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.pages.data.Subworkflow;
import org.springframework.stereotype.Component;


@Component
public class MilitaryServicePreparer implements DocumentFieldPreparer {
	
@Override
public List<DocumentField> prepareDocumentFields(Application application, Document document,
    Recipient recipient) {

  PagesData data = application.getApplicationData().getPagesData();

  // Get selected values from whoHasMilitaryService page
  List<String> selectedValues = Optional
      .ofNullable(application.getApplicationData().getPageData("whoHasMilitaryService"))
      .map(pageData -> pageData.get("whoHasMilitaryService"))
      .map(InputData::getValue)
      .orElse(List.of());

  // Extract IDs (last token of each value)
  List<String> selectedIds = selectedValues.stream()
      .map(value -> {
        String[] parts = value.split(" ");
        return parts[parts.length - 1];
      })
      .collect(Collectors.toList());

  Subworkflow householdSubworkflow =
      application.getApplicationData().getSubworkflows().get("household");

  List<DocumentField> results = new ArrayList<>();

  boolean whoHasMilitaryServiceAsked =
      application.getApplicationData().getPagesData().getPage("whoHasMilitaryService") != null;

    //Applicant — always uses applicantHasMilitaryService -> MILITARY_SERVICE_APPLICANT
  if (!whoHasMilitaryServiceAsked) {
    // Household page not reached, use single applicant military service value
    Boolean applicantServed = Boolean.valueOf(getFirstValue(data, MILITARY_SERVICE));
    results.add(new DocumentField(
        "militaryService",
        "applicantHasMilitaryService",
        Boolean.TRUE.equals(applicantServed) ? "true" : "false",
        DocumentFieldType.SINGLE_VALUE,
        null
    ));
  } else {
    // whoHasMilitaryService was answered — check if applicant is in selected IDs
    results.add(new DocumentField(
        "militaryService",
        "applicantHasMilitaryService",
        selectedIds.contains("applicant") ? "true" : "false",
        DocumentFieldType.SINGLE_VALUE,
        null
    ));

    // Household members — indexed, maps to MILITARY_SERVICE_0, _1, etc.
    if (householdSubworkflow != null) {
      for (int i = 0; i < householdSubworkflow.size(); i++) {
        String memberId = householdSubworkflow.get(i).getId().toString();
        results.add(new DocumentField(
            "militaryService",
            "hasMilitaryService",
            selectedIds.contains(memberId) ? "true" : "false",
            DocumentFieldType.SINGLE_VALUE,
            i  // maps to MILITARY_SERVICE_0, _1, ...
        ));
      }
    }
  }

  return results;
 }
}