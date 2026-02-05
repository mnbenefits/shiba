package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.FullNameFormatter.format;
import static org.codeforamerica.shiba.output.FullNameFormatter.getFullName;

import java.util.ArrayList;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.springframework.stereotype.Component;

@Component
public class PenaltyWarningsPreparer implements DocumentFieldPreparer {
	private static final List<String> QUESTIONS = List.of("disqualifiedPublicAssistance", "fraudulentStatements",
			"hidingFromLaw", "drugFelonyConviction", "violatingParole");

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {

		PageData page = application.getApplicationData().getPagesData().getPage("penaltyWarnings");

		if (page == null || page.isEmpty()) {
			return List.of();
		}

		List<DocumentField> fields = new ArrayList<>();
		int index = 0;
		for (int i = 0; i < QUESTIONS.size(); i++) {
			String questionName = QUESTIONS.get(i);
			int questionNumber = i + 1;
			InputData input = page.get(questionName);
			if (input.getValue().contains("true")) {
				index = addFieldsIfAnsweredYes(application, fields, input, questionName, questionNumber, index);

			}

		}

		return fields;
	}

	private int addFieldsIfAnsweredYes(Application application, List<DocumentField> fields, InputData input,
			String questionName, int questionNumber, int index) {

		List<String> values = input.getValue();
		if (values == null || !values.contains("true")) {
			return index;
		}
		
		List<String> memberNames = extractHouseholdMemberNames(input.getValue());
		//If there are no household members use the applicant's
		if (memberNames.isEmpty()) {
			memberNames = List.of(getFullName(application));

		}
		
		for (String memberName : memberNames) {

			fields.add(new DocumentField("penaltyWarnings", "questionNumber", String.valueOf(questionNumber),
					DocumentFieldType.SINGLE_VALUE, index));

			fields.add(new DocumentField("penaltyWarnings", "householdMembers", memberName,
					DocumentFieldType.SINGLE_VALUE, index));
			index++;
		}

		return index;
	}

	private List<String> extractHouseholdMemberNames(List<String> values) {
		List<String> memberNames = new ArrayList<>();

		for (String value : values) {
			if (isHouseholdMemberValue(value)) {
				String formatted = format(value);
				if (!formatted.isBlank()) {
					memberNames.add(formatted);
				}
			}
		}

		return memberNames;
	}
	
	//ignoring boolean value(from the answer) like("true"/"false") from values
	private boolean isHouseholdMemberValue(String value) {

		return !"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value);
	}
}
