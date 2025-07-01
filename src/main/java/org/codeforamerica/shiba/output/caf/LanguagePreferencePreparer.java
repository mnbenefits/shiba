package org.codeforamerica.shiba.output.caf;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.WRITTEN_LANGUAGE_PREFERENCES;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.SPOKEN_LANGUAGE_PREFERENCES;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.OTHER_WRITTEN_LANGUAGE_PREFERENCES;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.OTHER_SPOKEN_LANGUAGE_PREFERENCES;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;

import java.util.ArrayList;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.output.documentfieldpreparers.DocumentFieldPreparer;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.springframework.stereotype.Component;

@Component
public class LanguagePreferencePreparer implements DocumentFieldPreparer {

  @Override
  public List<DocumentField> prepareDocumentFields(Application application, Document _document,
      Recipient _recipient) {
    return map(application.getApplicationData().getPagesData(), application);
  }

	private List<DocumentField> map(PagesData pagesData, Application application) {
		List<DocumentField> languagePreference = new ArrayList<>();

		String writtenlanguage = getFirstValue(application.getApplicationData().getPagesData(),
				WRITTEN_LANGUAGE_PREFERENCES);
		String spokenLanguage = getFirstValue(application.getApplicationData().getPagesData(),
				SPOKEN_LANGUAGE_PREFERENCES);
		String otherWrittenLanguage = getFirstValue(application.getApplicationData().getPagesData(),
				OTHER_WRITTEN_LANGUAGE_PREFERENCES);
		String otherSpokenLanguage = getFirstValue(application.getApplicationData().getPagesData(),
				OTHER_SPOKEN_LANGUAGE_PREFERENCES);

		languagePreference.add(new DocumentField("writtenLanguage", "preferredWrittenLanguage",
				otherWrittenLanguage.isEmpty() ? writtenlanguage : otherWrittenLanguage, SINGLE_VALUE));
		
		languagePreference.add(new DocumentField("spokenLanguage", "preferredSpokenLanguage",
				otherSpokenLanguage.isEmpty() ? spokenLanguage : otherSpokenLanguage, SINGLE_VALUE));

		return languagePreference;
	}
}
