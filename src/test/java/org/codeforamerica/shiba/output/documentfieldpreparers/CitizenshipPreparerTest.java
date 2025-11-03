package org.codeforamerica.shiba.output.documentfieldpreparers;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

public class CitizenshipPreparerTest {
	
	CitizenshipPreparer preparer = new CitizenshipPreparer();

	@Test
	void shouldReturnEmptyWhenUsCitizenshipNotAsked() {
	    // Edge case: page data is null or citizenshipStatus is empty.
	    ApplicationData applicationData = new ApplicationData();
	    
	    List<DocumentField> result = preparer.prepareDocumentFields(
	        Application.builder().applicationData(applicationData).build(), 
	        Document.CAF, 
	        null);

	    assertThat(result).isEmpty();
	}
	    
	  @Test
	  void shouldCreateFieldsForHouseholdWithMixedCitizenship_CAF() {
	      ApplicationData applicationData = new ApplicationData();
	      applicationData.getSubworkflows().addIteration("household", new PagesData());
	  UUID householdMemberID = applicationData.getSubworkflows().get("household").get(0).getId();

	  new TestApplicationDataBuilder(applicationData)
	     .withPageData("citizenship", "citizenshipStatus", List.of("NOT_CITIZEN", "BIRTH_RIGHT"))
	      .withPageData("citizenship", "citizenshipIdMap", List.of("applicant", householdMemberID.toString()));
	 
	  List<DocumentField> result = preparer.prepareDocumentFields(
		        Application.builder().applicationData(applicationData).build(), 
		        Document.CAF, 
		        null);

	  assertThat(result).isEqualTo(List.of(
	      new DocumentField(
	          "citizenship",
	          "citizenshipStatus",
	          List.of("Not_Citizen"),
	          DocumentFieldType.SINGLE_VALUE,
	          0
	      ),
	      new DocumentField(
	          "citizenship",
	          "citizenshipStatus",
	          List.of("Citizen"),
	              DocumentFieldType.SINGLE_VALUE,
	              1
	          )
	      ));
	  }
	  
	  @Test
	  void shouldCreateFieldWhenApplicantIsCitizen_CAF() {
	      ApplicationData applicationData = new ApplicationData();
	      
	      new TestApplicationDataBuilder(applicationData)
	          .withPageData("citizenship", "citizenshipStatus", List.of("BIRTH_RIGHT"))
	          .withPageData("citizenship", "citizenshipIdMap", List.of("applicant"));

	      List<DocumentField> result = preparer.prepareDocumentFields(
	          Application.builder().applicationData(applicationData).build(), 
	          Document.CAF, 
	          null);

	      assertThat(result).isEqualTo(List.of(
	          new DocumentField("citizenship", "citizenshipStatus", "Citizen", DocumentFieldType.SINGLE_VALUE, 0)
	      ));
	  }
	  
	  @Test
	  void shouldCreateFieldWhenApplicantIsNotUsCitizen_CAF() {
	      ApplicationData applicationData = new ApplicationData();
	      
	      new TestApplicationDataBuilder(applicationData)
	          .withPageData("citizenship", "citizenshipStatus", List.of("NOT_CITIZEN"))
	          .withPageData("citizenship", "citizenshipIdMap", List.of("applicant"));

	      List<DocumentField> result = preparer.prepareDocumentFields(
	          Application.builder().applicationData(applicationData).build(), 
	          Document.CAF, 
	          null);

	      assertThat(result).isEqualTo(List.of(
	          new DocumentField("citizenship", "citizenshipStatus", "Not_Citizen", DocumentFieldType.SINGLE_VALUE, 0)
	      ));
	  }
	  
	  //Includes household members only with Yes/No values, Skips applicant
	  //Reindexes: first household member becomes index 0, second becomes index 1
	  @Test
	  void shouldCreateFieldsForHouseholdMembersOnly_CCAP() {
	      ApplicationData applicationData = new ApplicationData();
	      applicationData.getSubworkflows().addIteration("household", new PagesData());
	      applicationData.getSubworkflows().addIteration("household", new PagesData());
	      
	      UUID member1ID = applicationData.getSubworkflows().get("household").get(0).getId();
	      UUID member2ID = applicationData.getSubworkflows().get("household").get(1).getId();
	      
	      new TestApplicationDataBuilder(applicationData)
	          .withPageData("citizenship", "citizenshipStatus", 
	              List.of("BIRTH_RIGHT", "NOT_CITIZEN", "NATURALIZED"))
	          .withPageData("citizenship", "citizenshipIdMap", 
	              List.of("applicant", member1ID.toString(), member2ID.toString()));

	      List<DocumentField> result = preparer.prepareDocumentFields(
	          Application.builder().applicationData(applicationData).build(), 
	          Document.CCAP, 
	          null);

	      assertThat(result).isEqualTo(List.of(
	          new DocumentField("citizenship", "citizenshipStatus", "No", DocumentFieldType.SINGLE_VALUE, 0),
	          new DocumentField("citizenship", "citizenshipStatus", "Yes", DocumentFieldType.SINGLE_VALUE, 1)
	      ));
	  }

}
