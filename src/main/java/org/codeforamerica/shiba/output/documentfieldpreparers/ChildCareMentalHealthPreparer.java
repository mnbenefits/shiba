package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.ArrayList;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.springframework.stereotype.Component;

@Component
public class ChildCareMentalHealthPreparer implements DocumentFieldPreparer {
	List<DocumentField> fields = new ArrayList<>();

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		fields = new ArrayList<DocumentField>();

		PageData whoNeedsCare = application.getApplicationData().getPageData("whoNeedsChildCareForMentalHealth");
		PageData timesData = application.getApplicationData().getPageData("childCareMentalHealthTimes");

		if (whoNeedsCare != null) {
			InputData whoNeedsCareInputData = whoNeedsCare.get("whoNeedsChildCareMentalHealth");
			if (whoNeedsCareInputData != null && !whoNeedsCareInputData.getValue().isEmpty()) {
				List<String> listOfWhoNeedsCare = whoNeedsCareInputData.getValue();

				for (int i = 0; i < listOfWhoNeedsCare.size(); i++) {
					String namewithId = listOfWhoNeedsCare.get(i);
					String fullName = namewithId.substring(0, namewithId.lastIndexOf(' '));
					fields.add(new DocumentField("whoNeedsChildCareForMentalHealth", "fullName", fullName, SINGLE_VALUE,
							i));
				}
			}

		} else {
			PageData personalInfo = application.getApplicationData().getPageData("personalInfo");
				if (personalInfo != null && timesData !=null) {
					InputData firstName = personalInfo.get("firstName");
					InputData lastName = personalInfo.get("lastName");
					if (firstName != null && lastName != null && !firstName.getValue().isEmpty()
							&& !lastName.getValue().isEmpty()) {
						String fullName = firstName.getValue(0) + " " + lastName.getValue(0);
						fields.add(new DocumentField("whoNeedsChildCareForMentalHealth", "fullName", fullName,
								SINGLE_VALUE, 0));

					}
				}

		}

		if (timesData != null) {
			InputData hours = timesData.get("childCareMentalHealthHours");
			if (hours != null && !hours.getValue().isEmpty()) {
				List<String> hoursValues = hours.getValue();
				for (int i = 0; i < hoursValues.size(); i++) {
					String hoursValue = hoursValues.get(i);
					if (hoursValue != null && !hoursValue.trim().isEmpty()) {
						fields.add(new DocumentField("childCareMentalHealthTimes", "childCareMentalHealthHours",
								hoursValue, SINGLE_VALUE, i));
					}
				}

			}
		}
		return fields;
	}

}
