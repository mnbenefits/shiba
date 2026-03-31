package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.PageData;
import org.springframework.stereotype.Component;

@Component
public class PastBenefitsPreparer implements DocumentFieldPreparer {

  private Map<String, String> LABEL_BY_OPTION = Map.of(
      "CASH_ASSISTANCE", "CASH",
      "SNAP", "SNAP",
      "TRIBAL_COMMODITIES", "Food commodities");

  @Override
  public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
    PageData page = application.getApplicationData().getPagesData().getPage("pastBenefitDetails");
    if (page == null) {
      return List.of();
    }
   
    List<String> chosen =
        page.containsKey("whichPastBenefits") ? page.get("whichPastBenefits").getValue() : List.of();
    
    List<String> mapped = new ArrayList<>();
    for (String option : chosen) {
        String label = LABEL_BY_OPTION.get(option);
        if (label != null) {
            mapped.add(label);
        }
    }
    String value = String.join(", ", mapped);

    return List.of(new DocumentField("pastBenefitDetails", "whichPastBenefits", value, SINGLE_VALUE));
  }
}