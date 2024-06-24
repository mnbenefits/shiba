package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.FullNameFormatter.getListOfSelectedFullNames;

import java.util.ArrayList;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
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
		PageData gradePageData = application.getApplicationData().getPageData("schoolGrade");
		PageData schoolStartDatePageData = application.getApplicationData().getPageData("schoolStartDate");

		if (pageData == null) {
			return List.of();
		}
		List<String> schoolNames = pageData.get("schoolName").getValue();
		List<String> grades = gradePageData.get("schoolGrade").getValue();
		List<String> schoolStartDates = List.of();
		if (schoolStartDatePageData != null) {
			schoolStartDates = schoolStartDatePageData.get("schoolStartDate").getValue();
		}

		List<DocumentField> result = new ArrayList<>();

		for (int i = 0; i < students.size(); i++) {
			String fullName = students.get(i);
			String schoolName = schoolNames.get(i);
			String grade = grades.get(i);

			result.add(new DocumentField("whoIsGoingToSchool", "fullName", List.of(fullName),
					DocumentFieldType.SINGLE_VALUE, i));
			result.add(new DocumentField("schoolDetails", "schoolName", List.of(schoolName),
					DocumentFieldType.SINGLE_VALUE, i));
			result.add(new DocumentField("schoolGrade", "schoolGrade", List.of(grade),
					DocumentFieldType.SINGLE_VALUE, i));
		}
	
		for (int i = 0; i < schoolStartDates.size(); i++) {
			String element = schoolStartDates.get(i);
            if (element.length() == 1) {
            	schoolStartDates.set(i, "0" + element);
            }
		}		
		for (int i = 0, j=0;  i < schoolStartDates.size(); j++, i+=3) {
			
	        List<String> sublist = schoolStartDates.subList(i, Math.min(i+3, schoolStartDates.size())); // ["", "", ""]
	        boolean allEmpty = sublist.stream().allMatch(String::isEmpty);
	        String sublistString;

	        if (allEmpty) {
	            sublistString = "";
	        } else {
	            for (int k = 0; k < sublist.size(); k++) {
	                if (sublist.get(k).isEmpty()) {
	                    sublist.set(k, "-");
	                }
	            }
	            sublistString = String.join("/", sublist);
	        }


		result.add(new DocumentField("schoolStartDate", "schoolStartDate", List.of(sublistString),
				DocumentFieldType.SINGLE_VALUE, j));
		}
		return result;

	}

}
