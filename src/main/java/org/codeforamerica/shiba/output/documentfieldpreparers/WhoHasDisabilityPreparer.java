package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.FullNameFormatter.getListOfSelectedFullNames;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.springframework.stereotype.Component;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getBooleanValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.HAS_DISABILITY;
import static org.codeforamerica.shiba.output.FullNameFormatter.getFullName;

@Component
public class WhoHasDisabilityPreparer implements DocumentFieldPreparer {

  @Override
  public List<DocumentField> prepareDocumentFields(Application application, Document document,
      Recipient recipient) {

    List<String> whoHasDisabilityHouseholdMembers =
        getListOfSelectedFullNames(application, "whoHasDisability", "whoHasDisability");
    
    if(getBooleanValue(application.getApplicationData().getPagesData(),HAS_DISABILITY) 
        && (whoHasDisabilityHouseholdMembers.get(0).isEmpty())) {
      whoHasDisabilityHouseholdMembers.clear();
      String applicantFullName = getFullName(application);
      // protect against a null "full name" - Defect 136465
      if (applicantFullName != null) {
    	  whoHasDisabilityHouseholdMembers.add(applicantFullName);
      }
    }

    List<DocumentField> result = new ArrayList<>();

    AtomicInteger i = new AtomicInteger(0);
    result = whoHasDisabilityHouseholdMembers.stream()
        .map(fullName -> new DocumentField("whoHasDisability", "whoHasDisability",
            List.of(fullName), DocumentFieldType.SINGLE_VALUE, i.getAndIncrement()))
        .collect(Collectors.toList());

    return result;
  }
}
