package org.codeforamerica.shiba.pages;

import static java.lang.Boolean.parseBoolean;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.DOB_AS_DATE_FIELD_NAME;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getGroup;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.IS_PREGNANT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Group.HOUSEHOLD;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.Iteration;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.pages.enrichment.DateOfBirthEnrichment;
import org.codeforamerica.shiba.pages.enrichment.HouseholdMemberDateOfBirthEnrichment;
import org.springframework.stereotype.Service;

@Service
public class WicRecommendationService {

	public static final int CHILD_AGE_MAXIMUM = 5;
	private final DateOfBirthEnrichment dateOfBirthEnrichment = new HouseholdMemberDateOfBirthEnrichment();

	public boolean showWicMessage(ApplicationData applicationData) {
		return hasPregnantHouseholdMember(applicationData) || hasHouseholdMemberUpToAge5(applicationData);
	}

	public boolean hasPregnantHouseholdMember(ApplicationData applicationData) {
		PagesData pagesData = applicationData.getPagesData();
		return parseBoolean(getFirstValue(pagesData, IS_PREGNANT));
	}

	public boolean hasHouseholdMemberUpToAge5(ApplicationData applicationData) {
		//TODO emj fix this when there are no HH groups.  If no HH group, then return false.
        boolean hasHousehold = applicationData.getSubworkflows().containsKey("household");
        if(hasHousehold) {
		List<PagesData> householdMemberIterations = getGroup(applicationData, HOUSEHOLD).stream()
				.map(Iteration::getPagesData).toList();
		List<PageData> householdMemberIterationEnrichedDobPagesData = householdMemberIterations.stream()
				.map(dateOfBirthEnrichment::process).toList();
		List<String> householdMemberBirthDatesAsStrings = householdMemberIterationEnrichedDobPagesData.stream()
				.map(pagesData -> pagesData.get(DOB_AS_DATE_FIELD_NAME).getValue().get(0)).toList();
		for(String ab: householdMemberBirthDatesAsStrings) {
		System.out.println("DOB--->" +ab);
	}
		List<LocalDate> householdMemberBirthDatesAsLocalDates = getHouseHoldMemberDatesOfBirthAsDates(
				householdMemberBirthDatesAsStrings);
		return householdMemberBirthDatesAsLocalDates.stream()
				.anyMatch(date -> Period.between(date, LocalDate.now()).getYears() < CHILD_AGE_MAXIMUM);
	}
        return false;
	}

	private List<LocalDate> getHouseHoldMemberDatesOfBirthAsDates(List<String> birthDatesAsStrings) {
		return birthDatesAsStrings.stream().filter(s -> !s.isBlank())
				.map(stringDob -> LocalDate.parse(stringDob, DateTimeFormatter.ofPattern("MM/dd/yyyy")))
				.collect(Collectors.toList());
	}

}
