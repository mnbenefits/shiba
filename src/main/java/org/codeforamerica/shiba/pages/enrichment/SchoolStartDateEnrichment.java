package org.codeforamerica.shiba.pages.enrichment;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.SCHOOL_START_DATE_AS_DATE_FIELD_NAME;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getValues;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.SCHOOL_START_DATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.springframework.stereotype.Component;

@Component
public class SchoolStartDateEnrichment implements Enrichment {

	/**
	 * Adds '0' padding to single digit days and months in given birth date. Ex.
	 * {"1", "2", "1999"} would return "01/02/1999".
	 *
	 * @param schoolStartDate - An array list of dates
	 * @return A list of String representing formatted dates
	 */
	private static List<String> formatSchoolStartDate(List<String> schoolStartDate) {

		if (schoolStartDate.isEmpty()) {
			return List.of();
		}

		// each date is composed from 3 parts (MM, DD, YYYY).
		int datesSize = schoolStartDate.size() / 3;
		ArrayList<String> schoolStartDateAsDate = new ArrayList<String>();
		for (int i = 0; i < datesSize; i++) {
			int ii = i * 3;
			String date = StringUtils.leftPad(schoolStartDate.get(ii + 0), 2, '0') + '/'
					+ StringUtils.leftPad(schoolStartDate.get(ii + 1), 2, '0') + '/' + schoolStartDate.get(ii + 2);
			if (date.equals("00/00/")) { // handle case where the date was not entered (fields are blank)
				date = "";
			}
			schoolStartDateAsDate.add(date);
		}

		return schoolStartDateAsDate;
	}

	@Override
	public PageData process(PagesData pagesData) {
		List<String> schoolStartDateString = formatSchoolStartDate(parseSchoolStartDate(pagesData));
		return new PageData(Map.of(SCHOOL_START_DATE_AS_DATE_FIELD_NAME, new InputData(schoolStartDateString)));
	}

	private List<String> parseSchoolStartDate(PagesData pagesData) {
		return getValues(pagesData, SCHOOL_START_DATE);
	}
}
