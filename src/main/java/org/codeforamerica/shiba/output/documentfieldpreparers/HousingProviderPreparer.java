package org.codeforamerica.shiba.output.documentfieldpreparers;

import static java.lang.Boolean.parseBoolean;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.HOUSING_PROVIDER_NAME;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.HOUSING_PROVIDER_NUMBER;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.MN_HOUSING_SUPPORT;
import static org.codeforamerica.shiba.output.DocumentFieldType.ENUMERATED_SINGLE_VALUE;

import java.util.List;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.springframework.stereotype.Component;

import io.micrometer.common.util.StringUtils;

@Component
public class HousingProviderPreparer implements DocumentFieldPreparer {
	
	private final static String NO_PROVIDER_ENTERED = "No provider entered.";
	private final static String DOES_NOT_HAVE_PROVIDER =  "Does not have a provider." ;

	@Override
	public List<DocumentField> prepareDocumentFields(Application application, Document document, Recipient recipient) {
		if(document != Document.CAF) {
			return List.of();
		}
		PagesData pagesData = application.getApplicationData().getPagesData();
		boolean hasHousingSupport = parseBoolean(getFirstValue(pagesData, MN_HOUSING_SUPPORT));
		if(!hasHousingSupport ) {
		    return List.of(
		            new DocumentField("housingSupportVendor",
		                "housingSupportVendor",
		                DOES_NOT_HAVE_PROVIDER,
		                ENUMERATED_SINGLE_VALUE));
		}
		
		PageData housingSupportData = pagesData.getPage("housingProviderInfo");
		if (housingSupportData == null) {
			return List.of();
		}else {
			String housingProviderName = getFirstValue(pagesData, HOUSING_PROVIDER_NAME);
			String housingProviderNumber = getFirstValue(pagesData, HOUSING_PROVIDER_NUMBER);
			boolean nameIsBlank = StringUtils.isBlank(housingProviderName);
			boolean numberIsBlank = StringUtils.isBlank(housingProviderNumber);
			if(nameIsBlank && numberIsBlank) {
			    return List.of(
			            new DocumentField("housingSupportVendor",
			                "housingSupportVendor",
			                NO_PROVIDER_ENTERED,
			                ENUMERATED_SINGLE_VALUE));
			}else if(!nameIsBlank && numberIsBlank) {
			    return List.of(
			            new DocumentField("housingSupportVendor",
			                "housingSupportVendor",
			                housingProviderName,
			                ENUMERATED_SINGLE_VALUE));
			}else if(nameIsBlank && !numberIsBlank) {
			    return List.of(
			            new DocumentField("housingSupportVendor",
			                "housingSupportVendor",
			                housingProviderNumber,
			                ENUMERATED_SINGLE_VALUE));
			}else {
			    return List.of(
			            new DocumentField("housingSupportVendor",
			                "housingSupportVendor",
			                housingProviderName + " / " + housingProviderNumber,
			                ENUMERATED_SINGLE_VALUE));
			}
			
		}

	}

}
