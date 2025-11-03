package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.Subworkflow;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ListNonUSCitizenPreparerTest {

	ListNonUSCitizenPreparer preparer = new ListNonUSCitizenPreparer();
	TestApplicationDataBuilder applicationDataTest = new TestApplicationDataBuilder();

	@Test
	void preparesFieldsForEveryoneInHouseNotUSCitizen() {

		String dariaId = "11111111-1111-1111-1111-111111111111";
		String otherPersonId = "22222222-2222-2222-2222-222222222222";

		ApplicationData applicationData = applicationDataTest
				.withPersonalInfo()
				.withMultipleHouseholdMembers().build();

		// Set the IDs
		Subworkflow household = applicationData.getSubworkflows().get("household");
		household.get(0).setId(UUID.fromString(otherPersonId));
		household.get(1).setId(UUID.fromString(dariaId));

		// Now add citizenship data with matching IDs
		PageData usCitizenPage = new PageData();
		usCitizenPage.put("citizenshipStatus", new InputData(List.of("NOT_CITIZEN", "NOT_CITIZEN", "NOT_CITIZEN")));
		usCitizenPage.put("citizenshipIdMap", new InputData(List.of("applicant", dariaId, otherPersonId)));
		applicationData.getPagesData().put("citizenship", usCitizenPage);

		List<DocumentField> result = preparer.prepareDocumentFields(
				Application.builder().applicationData(applicationData).build(), null, Recipient.CASEWORKER);

		assertThat(result).isEqualTo(List.of(

				new DocumentField("whoIsNonUsCitizen", "nameOfApplicantOrSpouse", List.of("Jane Doe"),
						DocumentFieldType.SINGLE_VALUE, 0),
				new DocumentField("whoIsNonUsCitizen", "alienId", "", DocumentFieldType.SINGLE_VALUE, 0),
				new DocumentField("whoIsNonUsCitizen", "nameOfApplicantOrSpouse", List.of("Daria Agàta"),
						DocumentFieldType.SINGLE_VALUE, 1),
				new DocumentField("whoIsNonUsCitizen", "alienId", "", DocumentFieldType.SINGLE_VALUE, 1),
				new DocumentField("whoIsNonUsCitizen", "nameOfApplicantOrSpouse", List.of("Other Person"),
						DocumentFieldType.SINGLE_VALUE, 2),
				new DocumentField("whoIsNonUsCitizen", "alienId", "", DocumentFieldType.SINGLE_VALUE, 2)));
	}

	@Test
	void preparesFieldsForApplicantOnlyNotUSCitizen() {
		ApplicationData applicationData = applicationDataTest
				.withPersonalInfo()
				.withPageData("citizenship", "citizenshipStatus", List.of("NOT_CITIZEN"))
				.withPageData("citizenship", "citizenshipIdMap", List.of("applicant")).build();

		List<DocumentField> result = preparer.prepareDocumentFields(
				Application.builder().applicationData(applicationData).build(), null, Recipient.CASEWORKER);

		assertThat(result).isEqualTo(List.of(
				new DocumentField("whoIsNonUsCitizen", "nameOfApplicantOrSpouse", List.of("Jane Doe"),
						DocumentFieldType.SINGLE_VALUE, 0),
				new DocumentField("whoIsNonUsCitizen", "alienId", List.of(""), DocumentFieldType.SINGLE_VALUE, 0)));
	}

		@Test
		void preparesNoFieldsIfEveryoneInHouseIsUsCitizen() {
		    String member1Id = "11111111-1111-1111-1111-111111111111";
		    String member2Id = "22222222-2222-2222-2222-222222222222";
		    
		    ApplicationData applicationData = applicationDataTest
		        .withPersonalInfo()
		        .withMultipleHouseholdMembers()
		        // Everyone is a citizen (no NOT_CITIZEN)
		        .withPageData("citizenship", "citizenshipStatus", 
		            List.of("BIRTH_RIGHT", "NATURALIZED", "DERIVED"))
		        .withPageData("citizenship", "citizenshipIdMap", 
		            List.of("applicant", member1Id, member2Id))
		        .build();
		    
		    // Set household member IDs to match
		    Subworkflow household = applicationData.getSubworkflows().get("household");
		    household.get(0).setId(UUID.fromString(member1Id));
		    household.get(1).setId(UUID.fromString(member2Id));

		    List<DocumentField> result = preparer.prepareDocumentFields(
		        Application.builder().applicationData(applicationData).build(), 
		        null, 
		        Recipient.CASEWORKER);

		    assertThat(result).isEmpty();  // ✅ No non-citizens = empty list
		}

	@Disabled("Alien ID collection is paused; re-enable when alienIdNumbers page data returns")
	@Test
	void preparesFieldsForAlienId() {
		ApplicationData applicationData = applicationDataTest.withPersonalInfo().withMultipleHouseholdMembers()
				.withPageData("citizenship", "citizenshipStatus", List.of("NOT_CITIZEN", "NOT_CITIZEN", "NOT_CITIZEN"))
				.withPageData("citizenship", "citizenshipIdMap", List.of("someGuid", "applicant", "notSpouse"))

				.withPageData("alienIdNumbers", "alienIdMap", List.of("someGuid", "applicant", "notSpouse"))
				.withPageData("alienIdNumbers", "alienIdNumber", List.of("SpouseAlienId", "AppAlienId", "")).build();

		List<DocumentField> result = preparer.prepareDocumentFields(
				Application.builder().applicationData(applicationData).build(), null, Recipient.CASEWORKER);

		assertThat(result).isEqualTo(List.of(
				new DocumentField("whoIsNonUsCitizen", "nameOfApplicantOrSpouse", List.of("Daria Agàta"),
						DocumentFieldType.SINGLE_VALUE, 0),
				new DocumentField("whoIsNonUsCitizen", "alienId", List.of("SpouseAlienId"),
						DocumentFieldType.SINGLE_VALUE, 0),
				new DocumentField("whoIsNonUsCitizen", "nameOfApplicantOrSpouse", List.of("Jane Doe"),
						DocumentFieldType.SINGLE_VALUE, 1),
				new DocumentField("whoIsNonUsCitizen", "alienId", List.of("AppAlienId"), DocumentFieldType.SINGLE_VALUE,
						1),
				new DocumentField("whoIsNonUsCitizen", "nameOfApplicantOrSpouse", List.of("Other Person"),
						DocumentFieldType.SINGLE_VALUE, 2),
				new DocumentField("whoIsNonUsCitizen", "alienId", List.of(""), DocumentFieldType.SINGLE_VALUE, 2)));
	}
}
