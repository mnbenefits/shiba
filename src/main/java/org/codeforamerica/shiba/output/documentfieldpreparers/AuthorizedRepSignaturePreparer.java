package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getValues;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.AUTHORIZED_REP_SIGNATURE;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.springframework.stereotype.Component;

@Component
public class AuthorizedRepSignaturePreparer implements DocumentFieldPreparer {
	
	  @Override
	  public List<DocumentField> prepareDocumentFields(Application application, Document document,
	      Recipient recipient) {
		  List<DocumentField> result = new ArrayList<>();
		  List<String> authorizedRepSignature = getValues(application.getApplicationData().getPagesData(), AUTHORIZED_REP_SIGNATURE);

		  if (!authorizedRepSignature.isEmpty()) {
			  result.add(new DocumentField("authorizedRepSignature", "authorizedRepSignDate", List.of(
			            DateTimeFormatter.ISO_LOCAL_DATE.format(application.getCompletedAt().withZoneSameInstant(ZoneId.of("America/Chicago")))),
			                SINGLE_VALUE));
		  }
		  
		  return result;
	  }

}
