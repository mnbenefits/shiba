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
        .withPageData("schoolDetails", "schoolName",
        	List.of("test school name A",
        			"test school name B",
        			"test school name C"))
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
        .withPageData("schoolDetails", "schoolName",
        	List.of("test school name B",
        			""))
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
        ));
  }
}
