package org.codeforamerica.shiba.output.documentfieldpreparers;

import java.nio.channels.Pipe.SourceChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.pages.data.Subworkflow;
import org.springframework.stereotype.Component;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.IS_US_CITIZEN;

@Component
public class HouseholdUsCitizenPreparer implements DocumentFieldPreparer {
	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		
		List<DocumentField> result = new ArrayList<>();
		
		/*
		List<String> nonUsCitizenHouseholdMembers = Optional
				.ofNullable(application.getApplicationData().getPageData("whoIsNonCitizen"))
				.map(pageData -> pageData.get("whoIsNonCitizen")).map(InputData::getValue).orElse(List.of(""));

		List<String> householdMemberIDs = nonUsCitizenHouseholdMembers.stream().map(selectedHouseholdMember -> {
			String[] householdMemberParts = selectedHouseholdMember.split(" ");
			return householdMemberParts[householdMemberParts.length - 1];
		}).collect(Collectors.toList());

		
		boolean usCitizenAsked = application.getApplicationData().getPagesData().getPage("usCitizen") != null;
		boolean whoIsNonCitizenAsked = application.getApplicationData().getPagesData().getPage("whoIsNonCitizen") != null;
		Subworkflow householdMemberSubworkflow = application.getApplicationData().getSubworkflows().get("household");
	    PagesData data = application.getApplicationData().getPagesData();

	    Boolean isApplicantUsCitizen = Boolean.valueOf(getFirstValue(data, IS_US_CITIZEN));

		if (!usCitizenAsked)
		{
			result.add(new DocumentField("usCitizen", "applicantIsUsCitizen", "Off", DocumentFieldType.SINGLE_VALUE, null));
		} else {
			if (!whoIsNonCitizenAsked) {
				result.add(new DocumentField("usCitizen", "applicantIsUsCitizen",
						isApplicantUsCitizen ? "true" : "false", DocumentFieldType.SINGLE_VALUE, null));
			} else {
				result.add(new DocumentField("usCitizen", "applicantIsUsCitizen",
						 "true",
						DocumentFieldType.SINGLE_VALUE, null));
			}

			if (householdMemberSubworkflow != null) {
				for (int i = 0; i < householdMemberSubworkflow.size(); i++) {
					result.add(
							new DocumentField("usCitizen", "isUsCitizen",
									List.of(householdMemberIDs.contains(
											householdMemberSubworkflow.get(i).getId().toString()) ? "false" : "true"),
									DocumentFieldType.SINGLE_VALUE, i));
				}

			}*/
		
		// new citizenship flow
		PageData usCitizendata = application.getApplicationData().getPageData("usCitizen");
		
		if (usCitizendata != null) {
			InputData citizenshipStatus = usCitizendata.get("citizenshipStatus");
			
			if (citizenshipStatus != null && !citizenshipStatus.getValue().isEmpty()) {
				List<String> statuses = citizenshipStatus.getValue();
				
				// CAF: include everyone
				if (document == Document.CAF) {
					for (int i = 0; i < statuses.size(); i++) {
						String status = statuses.get(i);
						String statusText = mapCitizenshipStatus(status);
						result.add(new DocumentField("usCitizen", "citizenshipStatus", statusText,
								DocumentFieldType.SINGLE_VALUE, i));
					}
				}
				// CCAP: New logic - skip applicant, only household members
				else if (document == Document.CCAP) {
					InputData personIdMap = usCitizendata.get("citizenshipIdMap");
					List<String> personIds = personIdMap != null ? personIdMap.getValue() : List.of();
					
					int ccapIndex = 0;
					for (int i = 0; i < statuses.size(); i++) {
						String status = statuses.get(i);
						String personId = i < personIds.size() ? personIds.get(i) : "";
						boolean isApplicant = "applicant".equals(personId);
						
						if (!isApplicant) {
							String isUsCitizen = mapCitizenshipStatusForCCAP(status);
							
							result.add(new DocumentField("usCitizen", "citizenshipStatus", isUsCitizen,
									DocumentFieldType.SINGLE_VALUE, ccapIndex));
							ccapIndex++;
						}
					}
				}
				//CERTAIN_POPS: Yes if all citizens, No if any non-citizen
				else if (document == Document.CERTAIN_POPS) {
					boolean hasNonCitizen = statuses.stream()
							.anyMatch(status -> "NOT_CITIZEN".equals(status));
					
					String isUsCitizen = hasNonCitizen ? "No" : "Yes";
					result.add(new DocumentField("usCitizen", "citizenshipStatus", isUsCitizen,
							DocumentFieldType.SINGLE_VALUE));
				}
			}
		}
		
		return result;
	}
	
	String mapCitizenshipStatus(String status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case "BIRTH_RIGHT" -> "Citizen";
			case "NATURALIZED", "DERIVED" -> "Naturalized";
			default -> "Not_Citizen";
		};
	}
	
	String mapCitizenshipStatusForCCAP(String status) {
		if (status == null || "NOT_CITIZEN".equals(status)) {
			return "No";
		}
		return "Yes";
	}
}