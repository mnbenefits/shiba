package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getValues;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.ALIEN_IDS;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.ALIEN_ID_MAP;
import static org.codeforamerica.shiba.output.FullNameFormatter.getFullName;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.springframework.stereotype.Component;

@Component
public class ListNonUSCitizenPreparer implements DocumentFieldPreparer {

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		List<DocumentField> nonUSCitizens = new ArrayList<>();
		List<NonUSCitizen> allApplicantsNonCitizen = getNonUSCitizens(application, document, recipient);
		int index = 0;
		for (NonUSCitizen person : allApplicantsNonCitizen) {
			nonUSCitizens.add(new DocumentField("whoIsNonUsCitizen", "nameOfApplicantOrSpouse",
					String.join(" ", person.fullName), DocumentFieldType.SINGLE_VALUE, index));
			nonUSCitizens.add(new DocumentField("whoIsNonUsCitizen", "alienId", String.join(" ", person.alienId),
					DocumentFieldType.SINGLE_VALUE, index));
			index++;
		}
		return nonUSCitizens;
	}

	public List<NonUSCitizen> getNonUSCitizens(Application application, Document document, Recipient recipient) {
		List<NonUSCitizen> allApplicantsNonCitizen = new ArrayList<NonUSCitizen>();
		PagesData pagesData = application.getApplicationData().getPagesData();

		// Get citizenship data from NEW flow
		PageData usCitizenPage = pagesData.getPage("citizenship");
		if (usCitizenPage == null) {
			return List.of();
		}

		InputData citizenshipStatusData = usCitizenPage.get("citizenshipStatus");
		InputData citizenshipIdMapData = usCitizenPage.get("citizenshipIdMap");

		if (citizenshipStatusData == null || citizenshipStatusData.getValue().isEmpty()) {
			return List.of();
		}

		List<String> statuses = citizenshipStatusData.getValue();
		List<String> personIds = citizenshipIdMapData != null ? citizenshipIdMapData.getValue() : List.of();

		// Get alien ID data not needed
		List<String> alienIds = getValues(pagesData, ALIEN_IDS);
		List<String> alienIdMap = getValues(pagesData, ALIEN_ID_MAP);

		// Find all non-citizens
		for (int i = 0; i < statuses.size(); i++) {
			if ("NOT_CITIZEN".equals(statuses.get(i))) {
				String personId = i < personIds.size() ? personIds.get(i) : "";

				// Get person's name
				String fullName;
				if ("applicant".equals(personId)) {
					fullName = getFullName(application);
				} else {
					fullName = getHouseholdMemberName(application, personId);
				}

				// Get alien ID for this person
				String alienId = "";
				int alienIndex = alienIdMap.indexOf(personId);
				if (alienIndex >= 0 && alienIndex < alienIds.size()) {
					alienId = alienIds.get(alienIndex);
				}

				allApplicantsNonCitizen.add(new NonUSCitizen(fullName, alienId));
			}
		}

		return allApplicantsNonCitizen;
	}
	
		// Helper method to find a specific household members by their unique ID
		private String getHouseholdMemberName(Application application, String personId) {
			// Get the household subworkflow (contains all household members)
			var householdMembers = application.getApplicationData().getSubworkflows().get("household");
			if (householdMembers != null) {
				// Find the specific household member with matching personId
				var matchingMember = householdMembers.stream()
						.filter(member -> personId.equals(member.getId().toString())).findFirst();
				// Extract first and last name
				if (matchingMember.isPresent()) {
					var memberData = matchingMember.get();
					var memberInfo = memberData.getPagesData().getPage("householdMemberInfo");
					if (memberInfo != null) {
						String firstName = memberInfo.get("firstName") != null ? memberInfo.get("firstName").getValue(0)
								: "";
						String lastName = memberInfo.get("lastName") != null ? memberInfo.get("lastName").getValue(0)
								: "";
						return firstName + " " + lastName;
					}
				}
			}
			return "";
		}

		@SuppressWarnings("unused")
		private String getAlienNumber(PagesData pagesData, String condition) {
			String result = "";
			List<String> alienIdMap = getValues(pagesData, ALIEN_ID_MAP);
			int index = alienIdMap.stream().collect(Collectors.toList()).indexOf(condition);
			List<String> alienNumbers = getValues(pagesData, ALIEN_IDS);
			result = alienNumbers.size() != 0 ? alienNumbers.get(index) : result;
			return result;
		}

		public class NonUSCitizen {
			String fullName = "";
			String alienId = "";

			public NonUSCitizen(String fullName, String alienId) {
				this.fullName = fullName;
				this.alienId = alienId;
			}

		}

	}
