package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getGroup;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.PERSONAL_INFO_FIRST_NAME;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.PERSONAL_INFO_LAST_NAME;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_INCOME;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_INCOME_OTHER;
import static org.codeforamerica.shiba.output.DocumentFieldType.ENUMERATED_SINGLE_VALUE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

@Component
public class OtherUnearnedIncomePreparer  implements DocumentFieldPreparer /* extends OneToManyDocumentFieldPreparer */ {
	
	ApplicationData applicationData = null;
	PagesData pagesData = null;
	List<DocumentField> otherUnearnedIncomeDocumentFields = new ArrayList<>();
	String supplementPageText = "";
	boolean addToCoverPage = false;


	// Question 11, unearned income
	ArrayList<Person> persons = null;
	HashMap<String, Integer> lookup = null;
	
	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {

		System.out.println(">>>>>>> OtherUnearnedIncomePreparer <<<<<<<<<");//TODO emj delete
		applicationData = application.getApplicationData();
		pagesData = applicationData.getPagesData();
		otherUnearnedIncomeDocumentFields = new ArrayList<DocumentField>();
		supplementPageText = "";
		addToCoverPage = false;
		persons = null;
		lookup = null;
		
		return map(application, document, recipient);
	}
	
	// This method controls the mapping logic for each of the Certain Pops
	// questions.
	private List<DocumentField> map(Application application, Document document, Recipient recipient) {
		
		// Question 11, unearned income
		mapUnearnedIncomeFields();

		if (addToCoverPage) {
			otherUnearnedIncomeDocumentFields.add(new DocumentField("certainPops", "certainPopsSupplement", supplementPageText,
					ENUMERATED_SINGLE_VALUE));
		}
		System.out.println(">>>>>>> OtherUnearnedIncomePreparer returning " + otherUnearnedIncomeDocumentFields.size() + " DocumentFields <<<<<<<<<<<");//TODO emj delete
		return otherUnearnedIncomeDocumentFields;
	}
	
//ORIGINAL METHODS IN THIS PREPARER CLASS:
//  private static final List<String> UNEARNED_INCOME_OTHER_OPTIONS = List.of("BENEFITS",
//      "INSURANCE_PAYMENTS", "CONTRACT_FOR_DEED", "TRUST_MONEY", "HEALTH_CARE_REIMBURSEMENT",
//      "INTEREST_DIVIDENDS", "OTHER_PAYMENTS", "RENTAL_INCOME");
//
//  @Override
//  protected OneToManyParams getParams() {
//    return new OneToManyParams(
//        "otherUnearnedIncome",
//        UNEARNED_INCOME_OTHER,
//        UNEARNED_INCOME_OTHER_OPTIONS);
//  }
  
	// CAF Question 14, unearned income
	private void mapUnearnedIncomeFields() {
		boolean hasUnearnedIncome = !mapNoUnearnedIncome();
		if (hasUnearnedIncome) {
			identifyAllPersons();
			// inputs could be on individual unearned income sources pages or on one
			// combined unearned income sources page
			identifyUnearnedIncomeItemsFromIndividualSourcesPages();
			identifyUnearnedIncomeItemsFromCombinedSourcesPages();
			mapOtherUnearnedIncomeItems();
		}
	}

	// Iterate all Persons and generate DocumentFields for each of their unearned
	// income items. The new CAF Section 14 allows for a maximum of 2 persons with one income type each
	// certain pops had 2 people and 4 unearned income types per person.
	private void mapOtherUnearnedIncomeItems() {
		//TODO emj anything over 2 other income sources will go on the cover-pages. Instead of free form text, it will need to 
		// be in DocumentFields.
	    supplementPageText = String.format("%s\n\n", supplementPageText);
		supplementPageText = String.format("%sQUESTION 14 continued:", supplementPageText);
		int personCount = 1;
		for (Person p : persons) {
			if (p.unearnedIncomeItems.size() > 0) {
				String fieldName = String.format("certainPopsUnearnedIncomePerson%d", personCount);//TODO change this to something that maps to the PDF
				otherUnearnedIncomeDocumentFields.add(
						new DocumentField("certainPopsUnearnedIncome", fieldName, p.fullName, ENUMERATED_SINGLE_VALUE));
				if (personCount > 2 || p.unearnedIncomeItems.size() > 2) {//changed size from 4 to 2
					addToCoverPage = true;
					supplementPageText = String.format("%s\nPerson %d, %s:", supplementPageText, personCount,
							p.fullName);
					System.out.println("----- ADD TO COVER PAGE IS SET TO TRUE ---");//TODO emj delete
				}
				int itemCount = 1;
				for (UnearnedIncomeItem item : p.unearnedIncomeItems) {
					createDocumentFields(item.type, item.amount, personCount, itemCount);
					if (personCount > 2 || itemCount > 4) {
						supplementPageText = String.format("%s\n  %d) %s, %s, %s", supplementPageText, itemCount,
								item.type, item.amount, item.frequency);
					}
					itemCount++;
				}
				personCount++;
			}
		}
		
		System.out.println("=== supplementPageText: \n" + supplementPageText);
	}

	// A method to create the document fields for a single unearned income item.
	private void createDocumentFields(String incomeType, String amount, int person, int item) {
		String typeName = String.format("certainPopsUnearnedIncomeType_%d_%d", person, item);
		otherUnearnedIncomeDocumentFields
				.add(new DocumentField("certainPopsUnearnedIncome", typeName, incomeType, ENUMERATED_SINGLE_VALUE));

		String amountName = String.format("certainPopsUnearnedIncomeAmount_%d_%d", person, item);
		otherUnearnedIncomeDocumentFields
				.add(new DocumentField("certainPopsUnearnedIncome", amountName, amount, ENUMERATED_SINGLE_VALUE));

		String frequency = String.format("certainPopsUnearnedIncomeFrequency_%d_%d", person, item);
		otherUnearnedIncomeDocumentFields
				.add(new DocumentField("certainPopsUnearnedIncome", frequency, "Monthly", ENUMERATED_SINGLE_VALUE));

	}

	// Create the Person list (applicant and all household members) and a
	// corresponding "lookup" table
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
		System.out.println("=== identifyAllPersons: " + persons);//TODO emj delete
	}

	/**
	 * This would be for applicant AND HH members
	 */
	private void identifyUnearnedIncomeItemsFromIndividualSourcesPages() {

		processIncomeSource("insurancePaymentsIncomeSource", "monthlyIncomeInsurancePayments",
				"insurancePaymentsAmount", "Insurance payments");
		processIncomeSource("trustMoneyIncomeSource", "monthlyIncomeTrustMoney", "trustMoneyAmount", "Trust money");
		//processIncomeSource("rentalIncomeSource", "monthlyIncomeRental", "rentalIncomeAmount", "Rental income"); //certain pops
		processIncomeSource("interestDividendsIncomeSource", "monthlyIncomeInterestDividends",
				"interestDividendsAmount", "Interest or dividends");
		processIncomeSource("healthcareReimbursementIncomeSource", "monthlyIncomeHealthcareReimbursement",
				"healthCareReimbursementAmount", "Healthcare reimbursement");
		processIncomeSource("contractForDeedIncomeSource", "monthlyIncomeContractForDeed", "contractForDeedAmount",
				"Contract for Deed");
		processIncomeSource("benefitsProgramsIncomeSource", "monthlyIncomeBenefitsPrograms", "benefitsAmount",
				"Benefits programs");
		processIncomeSource("otherPaymentsIncomeSource", "monthlyIncomeOtherPayments", "otherPaymentsAmount",
				"Other payments");
	}

	private void processIncomeSource(String pageName, String personsKey, String amountsKey, String description) {
		System.out.println("======= processIncomeSource for " + pageName + " =========");//TODO emj delete
		PageData pageData = pagesData.getPage(pageName);
		if (pageData != null) {
			List<String> keys = pageData.get(personsKey).getValue();
			List<String> amounts = pageData.get(amountsKey).getValue();
			for (String key : keys) {
				int lookupIndex = lookup.get(key);
				Person person = persons.get(lookupIndex);
				String amount = amounts.get(person.personIndex);
				person.unearnedIncomeItems.add(new UnearnedIncomeItem(description, amount));
				System.out.println("Added " + person.fullName + " description: " +  description + " amount: " + amount);//TODO emj delete
			}
		}else {
			System.out.println("pageData is null for " + pageName);//TODO emj delete
		}
	}


	/**
	 * This is for applicant with no HH members.
	 */
	private void identifyUnearnedIncomeItemsFromCombinedSourcesPages() {
//			processUnearnedIncomeSource("insurancePaymentsIncomeSource", "insurancePaymentsAmount", "Insurance payments");
//			processUnearnedIncomeSource("trustMoneyIncomeSource", "trustMoneyAmount", "Trust money");
//			processUnearnedIncomeSource("rentalIncomeSource", "rentalIncomeAmount", "Rental income"); //CP only?
//			processUnearnedIncomeSource("interestDividendsIncomeSource", "interestDividendsAmount", "Interest or dividends");
//			processUnearnedIncomeSource("healthcareReimbursementIncomeSource", "healthCareReimbursementAmount", "Healthcare reimbursement");
//			processUnearnedIncomeSource("contractForDeedIncomeSource", "contractForDeedAmount", "Contract for Deed");
//			processUnearnedIncomeSource("benefitsProgramsIncomeSource", "benefitsAmount", "Benefits programs");
//			processUnearnedIncomeSource("otherPaymentsIncomeSource", "otherPaymentsAmount", "Other payments");
		
		PageData pageData = pagesData.getPage("otherUnearnedIncomeSources");
		if (pageData != null) {
			processUnearnedIncomeSource(pageData, "insurancePaymentsAmount", "Insurance payments");
			processUnearnedIncomeSource(pageData, "trustMoneyAmount", "Trust money");
			processUnearnedIncomeSource(pageData, "rentalIncomeAmount", "Rental income");
			processUnearnedIncomeSource(pageData, "interestDividendsAmount", "Interest or dividends");
			processUnearnedIncomeSource(pageData, "healthCareReimbursementAmount", "Healthcare reimbursement");
			processUnearnedIncomeSource(pageData, "contractForDeedAmount", "Contract for Deed");
			processUnearnedIncomeSource(pageData, "benefitsAmount", "Benefits programs");
			processUnearnedIncomeSource(pageData, "otherPaymentsAmount", "Other payments");
		}
		
	}
	

	/**
	 * TODO emj new method to capture each unearned income from PagesData
	 * delete if not needed
	 * @param inputKey
	 * @param amountKey
	 * @param description
	 */
//	private void processUnearnedIncomeSource(String inputKey, String amountKey, String description) {
//		System.out.println("======= processUnearnedIncomeSource for " + inputKey + " =========");//TODO emj delete
//		PageData pageData = pagesData.get(inputKey);
//		if (pageData != null) {
//			InputData inputData = pageData.get(amountKey);
//			if (!inputData.getValue().isEmpty()) {
//				String amount = inputData.getValue(0);
//				Person person = persons.get(0);
//				person.unearnedIncomeItems.add(new UnearnedIncomeItem(description, amount));
//				System.out.println("Added " + person.fullName + " description: " +  description + " amount: " + amount);
//			}
//		}else {
//			System.out.println("pageData is null for " + inputKey);//TODO emj delete
//		}
//	}

	/**
	 * 
	 * @param pageData
	 * @param amountKey 
	 * @param description
	 */
	private void processUnearnedIncomeSource(PageData pageData, String amountKey, String description) {
		System.out.println(" ========== processUnearnedIncomeSource for " + amountKey + " ==========");
		InputData inputData = pageData.get(amountKey);
		if (!inputData.getValue().isEmpty()) {
			String amount = inputData.getValue(0);
			Person person = persons.get(0);
			person.unearnedIncomeItems.add(new UnearnedIncomeItem(description, amount));
			System.out.println("Added " + description + " " + amount + " to Person " + person.fullName);//TODO emj delete
		}
	}

	private String composeApplicantName() {
		String person1FirstName = getFirstValue(pagesData, PERSONAL_INFO_FIRST_NAME);
		String person1LastName = getFirstValue(pagesData, PERSONAL_INFO_LAST_NAME);
		return String.format("%s %s", person1FirstName, person1LastName);
	}

	private boolean mapNoUnearnedIncome() {
		boolean hasNoUnearnedIncome = true;
		String unearnedIncomeChoice = getFirstValue(pagesData, UNEARNED_INCOME);
		if (unearnedIncomeChoice != null) {
			if (!unearnedIncomeChoice.equals("NO_UNEARNED_INCOME_SELECTED")) {
				hasNoUnearnedIncome = false;
			}
		}
		boolean hasNoOtherUnearnedIncome = true;
		String otherUnearnedIncomeChoice = getFirstValue(pagesData, UNEARNED_INCOME_OTHER);
		if (otherUnearnedIncomeChoice != null) {
			if (!otherUnearnedIncomeChoice.equals("NO_OTHER_UNEARNED_INCOME_SELECTED")) {
				hasNoOtherUnearnedIncome = false;
			}
		}
		otherUnearnedIncomeDocumentFields.add(new DocumentField("certainPopsUnearnedIncome", "noCertainPopsUnearnedIncome",
				String.valueOf(!(hasNoUnearnedIncome && hasNoOtherUnearnedIncome)), ENUMERATED_SINGLE_VALUE));

		return hasNoUnearnedIncome && hasNoOtherUnearnedIncome;
	}

	// This internal class is used to keep track of one person who has one or more
	// unearned income items.
	public class Person {
		
		@Override
		public String toString() {
			return "Person [fullName=" + fullName + ", id=" + id + ", personIndex=" + personIndex
					+ ", unearnedIncomeItems=" + unearnedIncomeItems + "]";
		}

		String fullName = "";
		String id = "";
		int personIndex = -1;
		ArrayList<UnearnedIncomeItem> unearnedIncomeItems = null;

		public Person(String fullName, String id, int personIndex) {
			this.fullName = fullName;
			this.id = id;
			this.personIndex = personIndex;
			this.unearnedIncomeItems = new ArrayList<UnearnedIncomeItem>();
		}
	}

	// This internal class is used to keep track of the details of one unearned
	// income item.
	public class UnearnedIncomeItem {
		String type = "";
		String amount = "";
		String frequency = "Monthly";

		public UnearnedIncomeItem(String type, String amount) {
			this.type = type;
			this.amount = amount;
		}
	}
}
