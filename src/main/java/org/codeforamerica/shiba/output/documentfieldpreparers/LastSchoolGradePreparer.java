package org.codeforamerica.shiba.output.documentfieldpreparers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.PageData;
import org.springframework.stereotype.Component;

@Component
public class LastSchoolGradePreparer implements DocumentFieldPreparer {


    @Override
    public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
        PageData pageData = application.getApplicationData().getPageData("lastSchoolGrade");

        if (pageData == null) {
            return List.of();
        }

        List<String> grades = pageData.get("lastSchoolGrade").getValue();
        List<String> personIdMap = pageData.get("lastSchoolGradePersonIdMap").getValue();

        if (grades == null || personIdMap == null || grades.isEmpty()) {
            return List.of();
        }

        List<DocumentField> result = new ArrayList<>();
        int householdMemberIndex = 0;

        for (int i = 0; i < grades.size() && i < personIdMap.size(); i++) {
            String grade = grades.get(i);
            String personId = personIdMap.get(i);

            if (grade.isEmpty()) {
                if (!"applicant".equals(personId)) {
                    householdMemberIndex++;
                }
                continue;
            }

            if ("applicant".equals(personId)) {
                result.add(new DocumentField("lastSchoolGrade", "lastSchoolGrade",
                        List.of(grade), DocumentFieldType.SINGLE_VALUE));
            } else {
                result.add(new DocumentField("lastSchoolGrade", "lastSchoolGrade",
                        List.of(grade), DocumentFieldType.SINGLE_VALUE, householdMemberIndex));
                householdMemberIndex++;
            }
        }

        return result;
    }
}