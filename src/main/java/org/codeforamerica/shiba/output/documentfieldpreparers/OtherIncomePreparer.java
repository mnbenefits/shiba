package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getGroup;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.PERSONAL_INFO_FIRST_NAME;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.PERSONAL_INFO_LAST_NAME;
import static org.codeforamerica.shiba.output.DocumentFieldType.ENUMERATED_SINGLE_VALUE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.ApplicationDataParser;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.Iteration;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.pages.data.Subworkflow;
import org.springframework.stereotype.Component;

/**
 * The OtherIncomePreparer class creates all of the DocumentField objects which map to the following PDF fields:
 * <ul>
 * <li>OTHER_INCOME_TYPE</li>
 * <li>OTHER_INCOME_FULL_NAME</li>
 * <li>OTHER_INCOME_AMOUNT</li>
 * <li>OTHER_INCOME_FREQUENCY</li>
 * <ul>
 */
@Component
public class OtherIncomePreparer  implements DocumentFieldPreparer {

	// Define the unearned income "type" strings that we write to the PDFs
	private static final String SOCIAL_SECURITY = "Social Security (RSDI/SSDI)";
	private static final String SSI = "Supplemental Security Income (SSI)";
	private static final String VETERANS_BENEFITS = "Veterans benefits";
	private static final String UNEMPLOYMENT = "Unemployment";
	private static final String WORKERS_COMPENSATION = "Workers' compensation";
	private static final String RETIREMENT_INCOME = "Retirement or pension payments";
	private static final String CHILD_OR_SPOUSAL_SUPPORT = "Child support or spousal support";
	private static final String TRIBAL_PAYMENTS = "Tribal payments";
	private static final String INSURANCE_PAYMENTS = "Insurance payments (settlements, short- or long-term disability, etc.)";
	private static final String TRUST_MONEY = "Trusts";
	private static final String INTEREST_OR_DIVIDENDS = "Interest or dividends";
	private static final String HEALTHCARE_REIMBURSEMENT = "Health care reimbursement";
	private static final String CONTRACT_FOR_DEED = "Contract for deed";
	private static final String BENEFITS_PROGRAMS = "Public assistance (MFIP, DWP, GA, Tribal TANF)";
	private static final String OTHER_PAYMENTS = "Other Payments (inheritance, capital gains, etc.)";
	private static final String RENTAL_INCOME = "Rental income";
	private static final String ANNUITY_PAYMENTS = "Annuity payments";
	private static final String GIFTS = "Gifts";
	private static final String LOTTERY_GAMBLING = "Lottery or gambling winnings";
	private static final String DAY_TRADING_PROCEEDS = "Day trading proceeds";

	
	ApplicationData applicationData = null;
	PagesData pagesData = null;
	List<UnearnedIncomeItem> unearnedIncomeItemsList = null; // list of all unearned income items for all persons
	
	ArrayList<Person> persons = null;  // person list generated from personalInfo and household subflow
	HashMap<String, Integer> lookup = null; // person index lookup, used to index unearned income amount

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		applicationData = application.getApplicationData();
		pagesData = applicationData.getPagesData();
		unearnedIncomeItemsList = new ArrayList<UnearnedIncomeItem>();
		identifyAllPersons();
		buildUnearnedIncomeItemsList();
		return buildOtherIncomeDocumentFieldList();
	}

    /**
     * Builds an ArrayList of UnearnedIncomeItem objects.
     * Constructs the UnearnedIncomdeItem objects from the inputs collected on a variety of pages:
     * <ul>
     * <li>unearnedIncomeSources</li>
     * <li>otherUnearnedIncomeSources</li>
     * <li>xxxIncomeSource (e.g., "socialSecurityIncomeSource), unearned income is collected on individual pages when there is a household.</li>
     * </ul>
     * The pages that actually exist in the application is dependent on whether the application is for the applicant only or for the applicant and household.
     * @return void
     */
	private void buildUnearnedIncomeItemsList() {
		// Applicant only applications may have the unearndedIncomeSources page and otherUnearnedIncomeSources page
		processUnearnedIncomeSourcesPage();
		processOtherUnearnedIncomeSourcesPage();
		
		// Applicant with household applications - income identified on the unearnedIncome page
		processUnearnedIncomeSource("socialSecurityIncomeSource", "monthlyIncomeSSorRSDI", "socialSecurityAmount", SOCIAL_SECURITY);
		processUnearnedIncomeSource("supplementalSecurityIncomeSource", "monthlyIncomeSSI", "supplementalSecurityIncomeAmount", SSI);
		processUnearnedIncomeSource("veteransBenefitsIncomeSource", "monthlyIncomeVeteransBenefits", "veteransBenefitsAmount", VETERANS_BENEFITS);
		processUnearnedIncomeSource("unemploymentIncomeSource", "monthlyIncomeUnemployment", "unemploymentAmount", UNEMPLOYMENT);
		processUnearnedIncomeSource("workersCompIncomeSource", "monthlyIncomeWorkersComp", "workersCompensationAmount", WORKERS_COMPENSATION);
		processUnearnedIncomeSource("retirementIncomeSource", "monthlyIncomeRetirement", "retirementAmount", RETIREMENT_INCOME);
		processUnearnedIncomeSource("childOrSpousalSupportIncomeSource", "monthlyIncomeChildOrSpousalSupport", "childOrSpousalSupportAmount", CHILD_OR_SPOUSAL_SUPPORT);
		processUnearnedIncomeSource("tribalPaymentIncomeSource", "monthlyIncomeTribalPayment", "tribalPaymentsAmount", TRIBAL_PAYMENTS);
		
		// Applicant with household applications - income identified on the otherUnearnedIncome page
		processUnearnedIncomeSource("insurancePaymentsIncomeSource", "monthlyIncomeInsurancePayments", "insurancePaymentsAmount", INSURANCE_PAYMENTS);
		processUnearnedIncomeSource("trustMoneyIncomeSource", "monthlyIncomeTrustMoney", "trustMoneyAmount", TRUST_MONEY);
		processUnearnedIncomeSource("interestDividendsIncomeSource", "monthlyIncomeInterestDividends", "interestDividendsAmount", INTEREST_OR_DIVIDENDS);
		processUnearnedIncomeSource("healthcareReimbursementIncomeSource", "monthlyIncomeHealthcareReimbursement", "healthCareReimbursementAmount", HEALTHCARE_REIMBURSEMENT);
		processUnearnedIncomeSource("contractForDeedIncomeSource", "monthlyIncomeContractForDeed", "contractForDeedAmount", CONTRACT_FOR_DEED);
		processUnearnedIncomeSource("benefitsProgramsIncomeSource", "monthlyIncomeBenefitsPrograms", "benefitsAmount", BENEFITS_PROGRAMS);
		processUnearnedIncomeSource("otherPaymentsIncomeSource", "monthlyIncomeOtherPayments", "otherPaymentsAmount", OTHER_PAYMENTS);
		processUnearnedIncomeSource("rentalIncomeSource", "monthlyIncomeRental", "rentalIncomeAmount", RENTAL_INCOME);
		processUnearnedIncomeSource("annuityIncomeSource", "monthlyIncomeAnnuityPayments", "annuityPaymentsAmount", ANNUITY_PAYMENTS);
		processUnearnedIncomeSource("giftsIncomeSource", "monthlyIncomeGifts", "giftsAmount", GIFTS);
		processUnearnedIncomeSource("lotteryIncomeSource", "monthlyIncomeLotteryGambling", "lotteryGamblingAmount", LOTTERY_GAMBLING);
		processUnearnedIncomeSource("dayTradingIncomeSource", "monthlyIncomeDayTradingProceeds", "dayTradingProceedsAmount", DAY_TRADING_PROCEEDS);

		
	}
	
	/**
	 * Processes unearned income from inputs provided on the unearnedIncomeSources page
	 */
	private void processUnearnedIncomeSourcesPage() {
		PageData pageData = pagesData.getPage("unearnedIncomeSources");
		if (pageData != null) {
			String applicantId = String.format("%s %s", persons.get(0).fullName, persons.get(0).id); // will always be the applicant
			processUnearnedIncomeSource(pageData.get("socialSecurityAmount"), applicantId, SOCIAL_SECURITY);
			processUnearnedIncomeSource(pageData.get("supplementalSecurityIncomeAmount"), applicantId, SSI);
			processUnearnedIncomeSource(pageData.get("veteransBenefitsAmount"), applicantId, VETERANS_BENEFITS);
			processUnearnedIncomeSource(pageData.get("unemploymentAmount"), applicantId, UNEMPLOYMENT);
			processUnearnedIncomeSource(pageData.get("workersCompensationAmount"), applicantId, WORKERS_COMPENSATION);
			processUnearnedIncomeSource(pageData.get("retirementAmount"), applicantId, RETIREMENT_INCOME);
			processUnearnedIncomeSource(pageData.get("childOrSpousalSupportAmount"), applicantId, CHILD_OR_SPOUSAL_SUPPORT);
			processUnearnedIncomeSource(pageData.get("tribalPaymentsAmount"), applicantId, TRIBAL_PAYMENTS);
		}
	}
	
	/**
	 * Processes unearned income from inputs provided on the otherUnearnedIncomeSources page
	 */
	private void processOtherUnearnedIncomeSourcesPage() {
		PageData pageData = pagesData.getPage("otherUnearnedIncomeSources");
		if (pageData != null) {
			String applicantId = String.format("%s %s", persons.get(0).fullName, persons.get(0).id); // will alway be the applicant
			processUnearnedIncomeSource(pageData.get("insurancePaymentsAmount"), applicantId, INSURANCE_PAYMENTS);
			processUnearnedIncomeSource(pageData.get("trustMoneyAmount"), applicantId, TRUST_MONEY);
			processUnearnedIncomeSource(pageData.get("interestDividendsAmount"), applicantId, INTEREST_OR_DIVIDENDS);
			processUnearnedIncomeSource(pageData.get("healthCareReimbursementAmount"), applicantId, HEALTHCARE_REIMBURSEMENT);
			processUnearnedIncomeSource(pageData.get("contractForDeedAmount"), applicantId, CONTRACT_FOR_DEED);
			processUnearnedIncomeSource(pageData.get("benefitsAmount"), applicantId, BENEFITS_PROGRAMS);
			processUnearnedIncomeSource(pageData.get("otherPaymentsAmount"), applicantId, OTHER_PAYMENTS);
			processUnearnedIncomeSource(pageData.get("rentalIncomeAmount"), applicantId, RENTAL_INCOME);
			processUnearnedIncomeSource(pageData.get("annuityPaymentsAmount"), applicantId, ANNUITY_PAYMENTS);
			processUnearnedIncomeSource(pageData.get("giftsAmount"), applicantId, GIFTS);
			processUnearnedIncomeSource(pageData.get("lotteryGamblingAmount"), applicantId, LOTTERY_GAMBLING);
			processUnearnedIncomeSource(pageData.get("dayTradingProceedsAmount"), applicantId, DAY_TRADING_PROCEEDS);

		}
	}

	/**
	 * Creates an UnearnedIncomeItem object from InputData that is found on the unearnedIncomeSources page or the otherUnearnedIncomeSources page.
	 * @param inputData - an InputData object which holds unearned income data 
	 * @param personId - a person ID e.g., "John Doe Applicant"
	 * @param description - short description of the unearned income type e.g., "Social Security (RSDI/SSDI)"
	 */
	private void processUnearnedIncomeSource(InputData inputData, String personId, String description) {
		if (inputData.getValue().size() > 0) {
			unearnedIncomeItemsList.add(new UnearnedIncomeItem(personId, description, inputData.getValue(0)));
		}
		
	}

	/**
	 * Creates an UnearnedIncomeItem object from an "xxxIncomeSource page.
	 * @param pageName - the specific xxxIncomeSource page name e.g., "socialSecurityIncomeSource"
	 * @param personsIdKey - the specific key on the page for the persons input e.g., "monthlyIncomeSSorRSDI"
	 * @param amountsKey - the specific key on the page for the amounts input e.g., "socialSecurityAmount"
	 * @param description - the short description of the unearned income type e.g., "Social Security (RSDI/SSDI)"
	 * <br/>
	 * JSON example for a socialSecurityIncomeSource page:<br/>
	 * "socialSecurityIncomeSource" : {<br/>
     *   "socialSecurityAmount" : {<br/>
     *     "value" : [ "", "", "206" ]<br/>
     *   },<br/>
     *   "monthlyIncomeSSorRSDI" : {<br/>
     *     "value" : [ "Colleen Walace 34df46a7-43ec-45c6-8738-c605178ec3a9" ]<br/>
     *   }<br/>
     * },<br/>
	 */
	private void processUnearnedIncomeSource(String pageName, String personsIdKey, String amountsKey, String description) {
		PageData pageData = pagesData.getPage(pageName);
		if (pageData != null) {
			List<String> personIds = pageData.get(personsIdKey).getValue();
			List<String> amounts = pageData.get(amountsKey).getValue();
			for (int i=0; i<personIds.size(); i++) {
				String personId = personIds.get(i);
				int personIndex = lookup.get(personId);
				String personIncomeAmount = amounts.get(personIndex);
				unearnedIncomeItemsList.add(new UnearnedIncomeItem(personId, description, personIncomeAmount));
			}
		}
	}
	
	/**
	 * Generates an List of DocumentField objects from the UnearnedIncomeItems collected.
	 * @return - List of DocumentField objects
	 */
	private List<DocumentField> buildOtherIncomeDocumentFieldList() {
		List<DocumentField> otherIncomeDocumentFields = new ArrayList<DocumentField>();
		for (int i=0; i<unearnedIncomeItemsList.size(); i++) {
			UnearnedIncomeItem unearnedIncomeItem = unearnedIncomeItemsList.get(i);
			otherIncomeDocumentFields.add(new DocumentField("otherIncome", "otherIncomeType", unearnedIncomeItem.type, ENUMERATED_SINGLE_VALUE, i));
			otherIncomeDocumentFields.add(new DocumentField("otherIncome", "otherIncomeFullName", unearnedIncomeItem.personFullName, ENUMERATED_SINGLE_VALUE, i));
			otherIncomeDocumentFields.add(new DocumentField("otherIncome", "otherIncomeAmount", unearnedIncomeItem.amount, ENUMERATED_SINGLE_VALUE, i));
			otherIncomeDocumentFields.add(new DocumentField("otherIncome", "otherIncomeFrequency", unearnedIncomeItem.frequency, ENUMERATED_SINGLE_VALUE, i));
		}

		return otherIncomeDocumentFields;
	}
    
	/**
	 * Generates the applicant id by concatenating applicant's  "<firstName> <lastName> Applicant" 
	 * @return - the applicant ID
	 */
	private String composeApplicantName() {
		String person1FirstName = getFirstValue(pagesData, PERSONAL_INFO_FIRST_NAME);
		String person1LastName = getFirstValue(pagesData, PERSONAL_INFO_LAST_NAME);
		return String.format("%s %s", person1FirstName, person1LastName);
	}

	/**
	 * Create the Person list (applicant and all household members) and a corresponding "lookup" table
	 */
	private void identifyAllPersons() {
		persons = new ArrayList<Person>();
		lookup = new HashMap<String, Integer>();
		String applicantName = composeApplicantName();
		Person applicant = new Person(applicantName, "applicant", 0);
		String key = String.format("%s %s", applicantName, "applicant");
		lookup.put(key, 0);
		persons.add(0, applicant);

		Subworkflow subWorkflow = getGroup(applicationData, ApplicationDataParser.Group.HOUSEHOLD);
		if (subWorkflow != null) {
			for (int i = 0; i < subWorkflow.size(); i++) {
				Iteration iteration = subWorkflow.get(i);
				String id = iteration.getId().toString();
				PagesData pagesData = iteration.getPagesData();
				PageData pageData = pagesData.getPage("householdMemberInfo");
				String fullName = String.format("%s %s", pageData.get("firstName").getValue(0),
						pageData.get("lastName").getValue(0));
				key = String.format("%s %s", fullName, id);
				lookup.put(key, i + 1);
				persons.add(new Person(fullName, id, i + 1));
			}
		}
	}
	

	/**
	 * This internal class is used to identify one person, the applicant or a household member.
	 */
	private class Person {
		private String fullName = "";
		private String id = "";

		private Person(String fullName, String id, int personIndex) {
			this.fullName = fullName;
			this.id = id;
		}
	}

	/**
	 * This internal class is used to keep the details of one unearned income item (for a person).
	 */
	private class UnearnedIncomeItem {
		private String personFullName = "";
		private String type = "";
		private String amount = "";
		private String frequency = "Monthly";

		/**
		 * Constructor
		 * @param personId - format is "<full-name> GUID" or, "<full-name> applicant" for the applicant 
		 * @param type - the short description for the unearned income type
		 * @param amount - the (monthly) amount of the unearned income
		 */
		private UnearnedIncomeItem(String personId, String type, String amount) {
			this.personFullName = personId.substring(0, personId.lastIndexOf(" ")).trim();
			this.type = type;
			this.amount = amount;
		}
	}
}
