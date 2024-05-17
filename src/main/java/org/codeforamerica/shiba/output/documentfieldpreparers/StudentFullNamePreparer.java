package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.FullNameFormatter.getListOfSelectedFullNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.FullNameFormatter;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.PageData;
import org.springframework.stereotype.Component;

@Component
public class StudentFullNamePreparer implements DocumentFieldPreparer {

  @Override
  public List<DocumentField> prepareDocumentFields(Application application, Document document,
			Recipient recipient) {
		List<String> students = getListOfSelectedFullNames(application, "whoIsGoingToSchool", "whoIsGoingToSchool");
		List<String> children = getListOfSelectedFullNames(application, "childrenInNeedOfCare", "whoNeedsChildCare");
		students.retainAll(children);

		PageData pageData = application.getApplicationData().getPageData("schoolDetails");

		if (pageData == null) {
			return List.of();
		}
		List<String> schoolNames = pageData.get("schoolName").getValue();

		List<DocumentField> result = new ArrayList<>();

		for (int i = 0; i < students.size(); i++) {

			String fullName = students.get(i);
			String schoolName = schoolNames.get(i);

			result.add(new DocumentField("whoIsGoingToSchool", "fullName", List.of(fullName),
					DocumentFieldType.SINGLE_VALUE, i));
			result.add(new DocumentField("schoolDetails", "schoolName", List.of(schoolName),
					DocumentFieldType.SINGLE_VALUE, i));
		}
		return result;

	}

}
