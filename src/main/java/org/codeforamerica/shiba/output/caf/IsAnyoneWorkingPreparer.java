package org.codeforamerica.shiba.output.caf;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getBooleanValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.IS_SELF_EMPLOYMENT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.WHOSE_JOB_IS_IT;
import static org.codeforamerica.shiba.output.DocumentFieldType.ENUMERATED_SINGLE_VALUE;

import java.util.ArrayList;
import java.util.List;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.GrossMonthlyIncomeParser;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.output.documentfieldpreparers.DocumentFieldPreparer;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.springframework.stereotype.Component;

/**
 * If anybody in the household(either single applicant or household members) has
 * a self employment job, and nobody works for others(a nonSelfEmployment job), then mark the
 * IS_WORKING radio button to No.
 *
 */
@Component
public class IsAnyoneWorkingPreparer implements DocumentFieldPreparer {

	private final GrossMonthlyIncomeParser grossMonthlyIncomeParser;

	public IsAnyoneWorkingPreparer(GrossMonthlyIncomeParser grossMonthlyIncomeParser) {
		this.grossMonthlyIncomeParser = grossMonthlyIncomeParser;
	}

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		List<JobIncomeInformation> jobsToIncludeInGrossIncome = getJobIncomeInformationToIncludeInThisDocument(
				application, document);

		List<DocumentField> fields = new ArrayList<>();
		boolean somebodyInHouseholdHasSelfEmployment = false;
		boolean somebodyInHouseholdHasNonSelfEmployment = false;//works for somebody else
		for (JobIncomeInformation job : jobsToIncludeInGrossIncome) {
			//TODO this logic needs to be checked, seems like a scenario could be missed, or one of the booleans removed since they are opposites.
			boolean isSelfEmployment = getBooleanValue(job.getIteration().getPagesData(), IS_SELF_EMPLOYMENT);
			if (isSelfEmployment) {
				somebodyInHouseholdHasSelfEmployment = true;
			}else {
				somebodyInHouseholdHasNonSelfEmployment = true;
			}
		}

		if (somebodyInHouseholdHasNonSelfEmployment) {
			fields.add(new DocumentField("employmentStatus", "isAnyoneWorking", "true", ENUMERATED_SINGLE_VALUE, null));
		} else if(somebodyInHouseholdHasSelfEmployment){
			fields.add(new DocumentField("employmentStatus", "isAnyoneWorking", "false", ENUMERATED_SINGLE_VALUE, null));
		}

		return fields;

	}

	private List<JobIncomeInformation> getJobIncomeInformationToIncludeInThisDocument(Application application,
			Document document) {
		List<JobIncomeInformation> grossIncomeInfoForAllHouseholdMembers = grossMonthlyIncomeParser
				.parse(application.getApplicationData());

		List<JobIncomeInformation> grossIncomeInfoToIncludeInThisDocument = grossIncomeInfoForAllHouseholdMembers;
		if (document == Document.CERTAIN_POPS) {
			// Only include income info for jobs held by the applicant
			grossIncomeInfoToIncludeInThisDocument = grossIncomeInfoForAllHouseholdMembers.stream()
					.filter(IsAnyoneWorkingPreparer::doesApplicantHaveIncomeFromAJob).toList();
		}
		System.out.println(
				"=== grossIncomeInfoToIncludeInThisDocument = " + grossIncomeInfoToIncludeInThisDocument.toString());// TODO
																														// emj
		return grossIncomeInfoToIncludeInThisDocument;
	}

	/**
	 * WHOSE_JOB_IS_IT will find all household jobs. If there is a single applicant,
	 * there will be no household jobs and WHOSE_JOB_IS_IT will return nothing.
	 * 
	 * @param jobIncomeInformation
	 * @return
	 */
	private static boolean doesApplicantHaveIncomeFromAJob(JobIncomeInformation jobIncomeInformation) {
		PagesData pagesData = jobIncomeInformation.getIteration().getPagesData();
		boolean hasAJob = (getFirstValue(pagesData, WHOSE_JOB_IS_IT).contains("applicant")
				|| getFirstValue(pagesData, WHOSE_JOB_IS_IT).isEmpty());// will be empty for individual flow
		return hasAJob;
	}

}
