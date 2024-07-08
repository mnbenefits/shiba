package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

public class StudentFullNamePreparerTest {

  StudentFullNamePreparer mapper = new StudentFullNamePreparer();

  @Test
  void shouldCreateListOfStudentFullNamesAndSchoolNames() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPageData("whoIsGoingToSchool", "whoIsGoingToSchool",
            List.of("studentAFirstName studentALastName 939dc33-d13a-4cf0-9093-309293k3",
                "studentBFirstName studentBLastName b99f3f7e-d13a-4cf0-9093-23ccdba2a64d",
                "studentCFirstName studentCLastName b9tmgf7e-d13a-6mf0-9093-calkjasdfiv4d",
                "studentDFirstName studentDLastName y77f3f7e-b63a-4cf0-9089-asdfsafdba2a6"))
        .withPageData("childrenInNeedOfCare", "whoNeedsChildCare",
            List.of("studentBFirstName studentBLastName b99f3f7e-d13a-4cf0-9093-23ccdba2a64d",
            		"studentCFirstName studentCLastName b9tmgf7e-d13a-6mf0-9093-calkjasdfiv4d",
                    "studentDFirstName studentDLastName y77f3f7e-b63a-4cf0-9089-asdfsafdba2a6"))
        .withPageData("schoolDetails", "schoolName", List.of("test school name A", "test school name B", "test school name C"))
        .withPageData("schoolGrade", "schoolGrade", List.of("1", "3", "5"))
        .build();

    List<DocumentField> result = mapper.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).contains(
        new DocumentField(
            "whoIsGoingToSchool",
            "fullName",
            List.of("studentBFirstName studentBLastName"),
            DocumentFieldType.SINGLE_VALUE,
            0
        ),
        new DocumentField(
            "whoIsGoingToSchool",
            "fullName",
            List.of("studentCFirstName studentCLastName"),
            DocumentFieldType.SINGLE_VALUE,
            1
        ),
        new DocumentField(
            "whoIsGoingToSchool",
            "fullName",
            List.of("studentDFirstName studentDLastName"),
            DocumentFieldType.SINGLE_VALUE,
            2
        ),
        new DocumentField(
            "schoolDetails",
            "schoolName",
            List.of("test school name A"),
            DocumentFieldType.SINGLE_VALUE,
            0
        ),
        new DocumentField(
            "schoolDetails",
            "schoolName",
            List.of("test school name B"),
            DocumentFieldType.SINGLE_VALUE,
            1
        ),
        new DocumentField(
            "schoolDetails",
            "schoolName",
            List.of("test school name C"),
            DocumentFieldType.SINGLE_VALUE,
            2
        ),
        new DocumentField(
            "schoolGrade",
            "schoolGrade",
            List.of("1"),
            DocumentFieldType.SINGLE_VALUE,
            0
        ),
        new DocumentField(
            "schoolGrade",
            "schoolGrade",
            List.of("3"),
            DocumentFieldType.SINGLE_VALUE,
            1
        ),
        new DocumentField(
            "schoolGrade",
            "schoolGrade",
            List.of("5"),
            DocumentFieldType.SINGLE_VALUE,
            2
        ));
  }
  
  @Test
  void shouldMatchSchoolAndStudentCorrectly() {
    ApplicationData applicationData = new TestApplicationDataBuilder()
        .withPageData("whoIsGoingToSchool", "whoIsGoingToSchool",
            List.of("studentBFirstName studentBLastName b99f3f7e-d13a-4cf0-9093-23ccdba2a64d",
                "studentCFirstName studentCLastName b9tmgf7e-d13a-6mf0-9093-calkjasdfiv4d"))
        .withPageData("childrenInNeedOfCare", "whoNeedsChildCare",
            List.of("studentAFirstName studentALastName y77f3f7e-b63a-4cf0-9089-asdfsafdba2a6",
            		"studentBFirstName studentBLastName b99f3f7e-d13a-4cf0-9093-23ccdba2a64d",
            		"studentCFirstName studentCLastName b9tmgf7e-d13a-6mf0-9093-calkjasdfiv4d"))
        .withPageData("schoolDetails", "schoolName", List.of("test school name B", ""))
        .withPageData("schoolGrade", "schoolGrade",	List.of("Pre-K", ""))
        .withPageData("schoolStartDate", "schoolStartDate",	List.of("01/01/2020"))
        .build();

    List<DocumentField> result = mapper.prepareDocumentFields(Application.builder()
        .applicationData(applicationData)
        .build(), null, null);

    assertThat(result).contains(
        new DocumentField(
            "whoIsGoingToSchool",
            "fullName",
            List.of("studentBFirstName studentBLastName"),
            DocumentFieldType.SINGLE_VALUE,
            0
        ),
        new DocumentField(
            "whoIsGoingToSchool",
            "fullName",
            List.of("studentCFirstName studentCLastName"),
            DocumentFieldType.SINGLE_VALUE,
            1
        ),
        new DocumentField(
            "schoolDetails",
            "schoolName",
            List.of("test school name B"),
            DocumentFieldType.SINGLE_VALUE,
            0
        ),
        new DocumentField(
            "schoolDetails",
            "schoolName",
            List.of(""),
            DocumentFieldType.SINGLE_VALUE,
            1
        ),
        new DocumentField(
            "schoolGrade",
            "schoolGrade",
            List.of("Pre-K"),
            DocumentFieldType.SINGLE_VALUE,
            0
        ),
        new DocumentField(
            "schoolGrade",
            "schoolGrade",
            List.of(""),
            DocumentFieldType.SINGLE_VALUE,
            1
        ),
        new DocumentField(
            "schoolStartDate",
            "schoolStartDate",
            List.of("01/01/2020"),
            DocumentFieldType.SINGLE_VALUE,
            0
        ));
  }
  
  @Test
  void shouldAssignStartDatesOnlyToSpecificGrades() {
      ApplicationData applicationData = new TestApplicationDataBuilder()
          .withPageData("whoIsGoingToSchool", "whoIsGoingToSchool",
              List.of("studentAFirstName studentALastName a1234567-b89c-10de-f123-456789abcdef",
                      "studentBFirstName studentBLastName b2345678-c90d-11ef-g234-567890bcdefg",
                      "studentCFirstName studentCLastName c3456789-d01e-12fg-h345-678901cdefgh",
                      "studentDFirstName studentDLastName d4567890-e12f-13gh-i456-789012defghi",
                      "studentEFirstName studentELastName e5678901-f23g-14hi-j567-890123efghij",
                      "studentFFirstName studentFLastName f6789012-g34h-15ij-k678-901234fghijk"))
          .withPageData("childrenInNeedOfCare", "whoNeedsChildCare",
              List.of("studentAFirstName studentALastName a1234567-b89c-10de-f123-456789abcdef",
                      "studentBFirstName studentBLastName b2345678-c90d-11ef-g234-567890bcdefg",
                      "studentCFirstName studentCLastName c3456789-d01e-12fg-h345-678901cdefgh",
                      "studentDFirstName studentDLastName d4567890-e12f-13gh-i456-789012defghi",
                      "studentEFirstName studentELastName e5678901-f23g-14hi-j567-890123efghij",
                      "studentFFirstName studentFLastName f6789012-g34h-15ij-k678-901234fghijk"))
          .withPageData("schoolDetails", "schoolName", 
              List.of("School A", "School B", "School C", "School D", "School E", "School F"))
          .withPageData("schoolGrade", "schoolGrade",    
              List.of("K", "1st", "Pre-K", "2nd", "Hd Strt", "3rd"))
          .withPageData("schoolStartDate", "schoolStartDate",    
              List.of("01", "15", "2023", "02", "20", "2023", "03", "25", "2023"))
          .build();
      
      List<DocumentField> result = mapper.prepareDocumentFields(Application.builder()
          .applicationData(applicationData)
          .build(), null, null);
      
      // Check that start dates are assigned to K, Pre-K, and Hd Strt
      assertThat(result).contains(
          new DocumentField("schoolStartDate", "schoolStartDate", List.of("01/15/2023"), DocumentFieldType.SINGLE_VALUE, 0),
          new DocumentField("schoolStartDate", "schoolStartDate", List.of("02/20/2023"), DocumentFieldType.SINGLE_VALUE, 2),
          new DocumentField("schoolStartDate", "schoolStartDate", List.of("03/25/2023"), DocumentFieldType.SINGLE_VALUE, 4)
      );
      
      // Check that start dates are not assigned to other grades
      assertThat(result).doesNotContain(
          new DocumentField("schoolStartDate", "schoolStartDate", List.of(""), DocumentFieldType.SINGLE_VALUE, 1),
          new DocumentField("schoolStartDate", "schoolStartDate", List.of(""), DocumentFieldType.SINGLE_VALUE, 3),
          new DocumentField("schoolStartDate", "schoolStartDate", List.of(""), DocumentFieldType.SINGLE_VALUE, 5)
      );
      
      // Verify that all students have their grades correctly assigned
      assertThat(result).contains(
          new DocumentField("schoolGrade", "schoolGrade", List.of("K"), DocumentFieldType.SINGLE_VALUE, 0),
          new DocumentField("schoolGrade", "schoolGrade", List.of("1st"), DocumentFieldType.SINGLE_VALUE, 1),
          new DocumentField("schoolGrade", "schoolGrade", List.of("Pre-K"), DocumentFieldType.SINGLE_VALUE, 2),
          new DocumentField("schoolGrade", "schoolGrade", List.of("2nd"), DocumentFieldType.SINGLE_VALUE, 3),
          new DocumentField("schoolGrade", "schoolGrade", List.of("Hd Strt"), DocumentFieldType.SINGLE_VALUE, 4),
          new DocumentField("schoolGrade", "schoolGrade", List.of("3rd"), DocumentFieldType.SINGLE_VALUE, 5)
      );

      // Verify that student names are correctly assigned
      assertThat(result).contains(
          new DocumentField("whoIsGoingToSchool", "fullName", List.of("studentAFirstName studentALastName"), DocumentFieldType.SINGLE_VALUE, 0),
          new DocumentField("whoIsGoingToSchool", "fullName", List.of("studentBFirstName studentBLastName"), DocumentFieldType.SINGLE_VALUE, 1),
          new DocumentField("whoIsGoingToSchool", "fullName", List.of("studentCFirstName studentCLastName"), DocumentFieldType.SINGLE_VALUE, 2),
          new DocumentField("whoIsGoingToSchool", "fullName", List.of("studentDFirstName studentDLastName"), DocumentFieldType.SINGLE_VALUE, 3),
          new DocumentField("whoIsGoingToSchool", "fullName", List.of("studentEFirstName studentELastName"), DocumentFieldType.SINGLE_VALUE, 4),
          new DocumentField("whoIsGoingToSchool", "fullName", List.of("studentFFirstName studentFLastName"), DocumentFieldType.SINGLE_VALUE, 5)
      );
  }
  
}
