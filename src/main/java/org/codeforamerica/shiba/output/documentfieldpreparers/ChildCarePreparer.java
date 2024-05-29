package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;
import java.util.ArrayList;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.Iteration;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.pages.data.Subworkflow;
import org.springframework.stereotype.Component;

@Component
public class ChildCarePreparer implements DocumentFieldPreparer {
	List<DocumentField> childCareDocumentFields = new ArrayList<>();

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		childCareDocumentFields = new ArrayList<DocumentField>();

		return map(application, document, recipient);
	}

	// This method controls the mapping logic for each of the child care questions.
	private List<DocumentField> map(Application application, Document document, Recipient recipient) {
		// We collected based on providers, it needs to be mapped back to a per child
		// basis.
		ArrayList<ChildNeedingCare> childrenNeedingCare = mapChildCareProvidersToChildren(application);
		if (childrenNeedingCare.size() == 0)
			return childCareDocumentFields;
		PageData providerInfo = application.getApplicationData().getPageData("doYouHaveChildCareProvider");
		boolean hasProviders = providerInfo != null && providerInfo.get("hasChildCareProvider").getValue(0).equalsIgnoreCase("true");

		// In these loops, c=child index and p=child's provider index. The CCAP supports
		// two providers per child.
		for (int c = 0; c < childrenNeedingCare.size(); c++) {
			ChildNeedingCare childNeedingCare = childrenNeedingCare.get(c);
			childCareDocumentFields.add(
					new DocumentField("childNeedsChildcare", "childName", childNeedingCare.childName, SINGLE_VALUE, c));
			
			if(!hasProviders) {
				childCareDocumentFields.add(
						new DocumentField("childNeedsChildcare",String.format("provider%dName", 1), "Household does not have provider(s)", SINGLE_VALUE, c));
			}else if (childNeedingCare.childCareProviders.isEmpty()) {
					childCareDocumentFields
							.add(new DocumentField("childNeedsChildcare", String.format("provider%dName", 1), "No provider entered for this child", SINGLE_VALUE, c));
			}else {
					for (int p = 1; p <= childNeedingCare.childCareProviders.size(); p++) {
						ChildCareProvider childCareProvider = childNeedingCare.childCareProviders.get(p - 1);
						childCareDocumentFields.add(new DocumentField("childNeedsChildcare",
								String.format("provider%dName", p), childCareProvider.providerName, SINGLE_VALUE, c));
						childCareDocumentFields.add(new DocumentField("childNeedsChildcare",
								String.format("provider%dPhone", p), childCareProvider.providerPhone, SINGLE_VALUE, c));
						String streetAddress = childCareProvider.providerStreet;
						if (!childCareProvider.providerSuite.isBlank())
							streetAddress = streetAddress + " #" + childCareProvider.providerSuite;
						childCareDocumentFields.add(new DocumentField("childNeedsChildcare",
								String.format("provider%dStreet", p), streetAddress, SINGLE_VALUE, c));
						childCareDocumentFields.add(new DocumentField("childNeedsChildcare",
								String.format("provider%dCity", p), childCareProvider.providerCity, SINGLE_VALUE, c));
						childCareDocumentFields.add(new DocumentField("childNeedsChildcare",
								String.format("provider%dState", p), childCareProvider.providerState, SINGLE_VALUE, c));
						childCareDocumentFields
								.add(new DocumentField("childNeedsChildcare", String.format("provider%dZipCode", p),
										childCareProvider.providerZipCode, SINGLE_VALUE, c));
					}
				}
			}  

		return childCareDocumentFields;
	}

	private ArrayList<ChildNeedingCare> mapChildCareProvidersToChildren(Application application) {
		ArrayList<ChildNeedingCare> childrenNeedingCare = new ArrayList<ChildNeedingCare>();

		// Make a list of all children who need child care
		PageData childrenInNeedOfCare = application.getApplicationData().getPageData("childrenInNeedOfCare");
		if (childrenInNeedOfCare == null)
			return childrenNeedingCare; // abort, childrenInNeedOfCare page does not exist

		InputData whoNeedsChildCare = childrenInNeedOfCare.get("whoNeedsChildCare");

		for (String child : whoNeedsChildCare.getValue()) {
			childrenNeedingCare.add(new ChildNeedingCare(child));
		}

		// Map the providers to the children
		Subworkflow childCareProvidersSubworkflow = application.getApplicationData().getSubworkflows()
				.get("childCareProviders");

		if (childCareProvidersSubworkflow != null) {
			for (Iteration iteration : childCareProvidersSubworkflow) {
				ChildCareProvider provider = new ChildCareProvider(iteration);

				PagesData pagesData = iteration.getPagesData();
				PageData childrenAtThisProvider = pagesData.getPage("childrenAtThisProvider");
				InputData childrenNames = childrenAtThisProvider.get("childrenNames");
				for (String childNameWithId : childrenNames.getValue()) {
					for (ChildNeedingCare child : childrenNeedingCare) {
						if (childNameWithId.contains(child.childId)) {
							child.addProvider(provider);
						}
					}
				}
			}
		}

		return childrenNeedingCare;
	}

// ************* INTERNAL CLASSES ****************

	// This internal class is used to keep track of the details for one child care provider.
	public class ChildCareProvider {
		String providerId = "";
		String providerName = "";
		String providerPhone = "";
		String providerStreet = "";
		String providerSuite = "";
		String providerCity = "";
		String providerState = "";
		String providerZipCode = "";

		public ChildCareProvider(Iteration iteration) {
			this.providerId = iteration.getId().toString();
			PagesData pagesData = iteration.getPagesData();
			PageData childCareProviderInfo = pagesData.getPage("childCareProviderInfo");
			this.providerName = childCareProviderInfo.get("childCareProviderName").getValue(0);
			this.providerPhone = childCareProviderInfo.get("phoneNumber").getValue(0);
			this.providerStreet = childCareProviderInfo.get("streetAddress").getValue(0);
			this.providerSuite = childCareProviderInfo.get("suiteNumber").getValue(0);
			this.providerCity = childCareProviderInfo.get("city").getValue(0);
			this.providerState = childCareProviderInfo.get("state").getValue(0);
			this.providerZipCode = childCareProviderInfo.get("zipCode").getValue(0);
		}
	}

	// This internal class is used to keep track of the details for one child in need of care.
	public class ChildNeedingCare {
		String childId = "";
		String childName = "";
		ArrayList<ChildCareProvider> childCareProviders = new ArrayList<ChildCareProvider>();

		public ChildNeedingCare(String childNameWithId) {
			int splitIndex = childNameWithId.lastIndexOf(' ');
			this.childId = childNameWithId.substring(splitIndex + 1);
			this.childName = childNameWithId.substring(0, splitIndex);
		}

		public void addProvider(ChildCareProvider provider) {
			childCareProviders.add(provider);
		}
	}

}
