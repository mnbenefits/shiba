package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;
import java.util.ArrayList;
import java.util.List;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.ContactInfoParser;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.springframework.stereotype.Component;

@Component
public class CommunicationsOptInPreparer implements DocumentFieldPreparer {

  @Override
  public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
	  List<DocumentField> result = new ArrayList<>();
	  ApplicationData applicationData = application.getApplicationData();
	  
	  if (ContactInfoParser.optedIntoEmailCommunications(applicationData)) {
		  result.add(new DocumentField("communicationsOptIn", "commOptInEmail", "true", SINGLE_VALUE));
		  result.add(new DocumentField("communicationsOptIn", "commOptInEmailAddress", List.of(ContactInfoParser.email(applicationData)), SINGLE_VALUE));
	  } else {
		  result.add(new DocumentField("communicationsOptIn", "commOptInEmail", "false", SINGLE_VALUE));
		  result.add(new DocumentField("communicationsOptIn", "commOptInEmailAddress", List.of(""), SINGLE_VALUE));
	  }
	  
	  if (ContactInfoParser.optedIntoTEXT(applicationData)) {
		  result.add(new DocumentField("communicationsOptIn", "commOptInPhone", "true", SINGLE_VALUE));
		  result.add(new DocumentField("communicationsOptIn", "commOptInPhoneNumber", List.of(ContactInfoParser.phoneNumber(applicationData)), SINGLE_VALUE));
	  } else {
		  result.add(new DocumentField("communicationsOptIn", "commOptInPhone", "false", SINGLE_VALUE));
		  result.add(new DocumentField("communicationsOptIn", "commOptInPhoneNumber", List.of(""), SINGLE_VALUE));
	  }
	  
	  return result;
  }
}
