package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getBooleanValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.IS_SELF_EMPLOYMENT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Group.JOBS;

import java.util.function.Predicate;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.Iteration;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.pages.data.Subworkflow;
import org.springframework.stereotype.Component;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getGroup;
import static org.codeforamerica.shiba.output.FullNameFormatter.getFullName;
import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;


import java.util.List;

/**
 * Create enumerated document fields just for non self-employment jobs.
 * <p>
 * Ex. [{employer: jobA, self-employed: false}, {employer: jobB, self-employed: true}, {employer:
 * jobC, self-employed: false}]
 * <p>
 * Will return 2 document fields for the 2 non-self employed jobs --> [{nonSelfEmployed_employer:
 * jobA, 0}, {nonSelfEmployed_employer: jobC, 1}]
 */
@Component
public class NonSelfEmploymentPreparer extends SubworkflowScopePreparer {

  @Override
	protected ScopedParams getParams(Document document, Application application) {
		Predicate<PagesData> isSelfEmployed = pagesData -> !getBooleanValue(pagesData, IS_SELF_EMPLOYMENT);
		ScopedParams retval = new ScopedParams(isSelfEmployed, JOBS, "nonSelfEmployment_");
		return retval;
	}

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		List<DocumentField> fields = super.prepareDocumentFields(application, document, recipient);
		String applicantName = getFullName(application);
		if (applicantName == null || applicantName.trim().isEmpty()) {
			return fields;
		}

		ScopedParams params = getParams(document, application);
		Subworkflow subworkflow = getGroup(application.getApplicationData(), params.group());
		if (subworkflow == null || subworkflow.isEmpty()) {
			return fields;
		}
		int index = 0;
		for (Iteration iteration : subworkflow) {
			if (params.scope().test(iteration.getPagesData())) {
				PagesData pagesData = iteration.getPagesData();

				boolean hasHousehold = pagesData.getPage("addHouseholdMembers") != null
						&& pagesData.getPage("addHouseholdMembers").get("addHouseholdMembers") != null;

				if (!hasHousehold) {

					fields.add(new DocumentField("householdSelectionForIncome", "whoseJobIsItFormatted", applicantName,
							SINGLE_VALUE, index));
				}
				index++;
			}
		}

		return fields;
	}
}
