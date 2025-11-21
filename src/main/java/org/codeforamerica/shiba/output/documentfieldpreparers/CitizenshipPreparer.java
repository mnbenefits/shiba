package org.codeforamerica.shiba.output.documentfieldpreparers;


import java.util.ArrayList;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.springframework.stereotype.Component;


@Component
public class CitizenshipPreparer implements DocumentFieldPreparer {
	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		
		List<DocumentField> result = new ArrayList<>();
	
		// new citizenship flow
		PageData usCitizendata = application.getApplicationData().getPageData("citizenship");
		
		if (usCitizendata != null) {
			InputData citizenshipStatus = usCitizendata.get("citizenshipStatus");
			
			if (citizenshipStatus != null && !citizenshipStatus.getValue().isEmpty()) {
				List<String> statuses = citizenshipStatus.getValue();
				
				// CAF: include everyone
				if (document == Document.CAF) {
					for (int i = 0; i < statuses.size(); i++) {
						String status = statuses.get(i);
						String statusText = mapCitizenshipStatus(status);
						result.add(new DocumentField("citizenship", "citizenshipStatus", statusText,
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
							
							result.add(new DocumentField("citizenship", "citizenshipStatus", isUsCitizen,
									DocumentFieldType.SINGLE_VALUE, ccapIndex));
							ccapIndex++;
						}
					}
				}
			}
		}
		
		return result;
	}
	
	private String mapCitizenshipStatus(String status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case "BIRTH_RIGHT" -> "Citizen";
			case "NATURALIZED", "DERIVED" -> "Naturalized";
			default -> "Not_Citizen";
		};
	}
	
	private String mapCitizenshipStatusForCCAP(String status) {
		if (status == null || "NOT_CITIZEN".equals(status)) {
			return "No";
		}
		return "Yes";
	}
}