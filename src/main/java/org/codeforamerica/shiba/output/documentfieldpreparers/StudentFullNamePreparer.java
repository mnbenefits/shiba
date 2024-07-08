package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.FullNameFormatter.getListOfSelectedFullNames;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
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
        List<String> schoolStartDates = schoolStartDatePageData != null ? 
            schoolStartDatePageData.get("schoolStartDate").getValue() : 
            new ArrayList<>();

        List<DocumentField> result = new ArrayList<>();
        
        // Process school start dates
        List<String> formattedStartDates = formatSchoolStartDates(schoolStartDates);
        
        int startDateIndex = 0;
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
            
            // Add school start date only for K, pre-K, or HS grades
            if (isGradeRequiringStartDate(grade)) {
                if (startDateIndex < formattedStartDates.size()) {
                    result.add(new DocumentField("schoolStartDate", "schoolStartDate", 
                            List.of(formattedStartDates.get(startDateIndex)),
                            DocumentFieldType.SINGLE_VALUE, i));
                    startDateIndex++;
                } 
            }
        }
        
        return result;
    }
    
    private List<String> formatSchoolStartDates(List<String> schoolStartDates) {
        List<String> formattedDates = new ArrayList<>();
        for (int i = 0; i < schoolStartDates.size(); i += 3) {
            List<String> sublist = schoolStartDates.subList(i, Math.min(i + 3, schoolStartDates.size()));
            if (sublist.stream().allMatch(String::isEmpty)) {
                formattedDates.add("");
            } else {
                String formattedDate = sublist.stream()
                    .map(s -> s.isEmpty() ? "-" : (s.length() == 1 ? "0" + s : s))
                    .collect(Collectors.joining("/"));
                formattedDates.add(formattedDate);
            }
        }
        return formattedDates;
    }
    
    private boolean isGradeRequiringStartDate(String grade) {
        return grade.equalsIgnoreCase("K") || 
               grade.equalsIgnoreCase("pre-K") || 
               grade.equalsIgnoreCase("Hd Strt");
    }
}
