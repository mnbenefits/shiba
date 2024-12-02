package org.codeforamerica.shiba.pages.events;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.codeforamerica.shiba.TribalNationRoutingDestination;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.DocumentListParser;
import org.codeforamerica.shiba.mnit.RoutingDestination;
import org.codeforamerica.shiba.pages.RoutingDecisionService;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Component
public class EmailJsonDataCreator {
	private final RoutingDecisionService routingDecisionService;

	public EmailJsonDataCreator(RoutingDecisionService routingDecisionService) {
		super();
		this.routingDecisionService = routingDecisionService;
	}
	
	private JsonObject createBaseEmailJsonObject(Application application, String recepientEmail, Locale locale, String emailType, String pageName) {
		JsonObject emailData = new JsonObject();
		ApplicationData appData = application.getApplicationData();
		Timestamp completedAt = Timestamp.valueOf(application.getCompletedAt().toLocalDateTime());

		Set<RoutingDestination> destinations = new LinkedHashSet<>();
		DocumentListParser.parse(appData).forEach(doc -> {
			List<RoutingDestination> routingDestinationsForThisDoc = routingDecisionService
					.getRoutingDestinations(appData, doc);
			destinations.addAll(routingDestinationsForThisDoc);
		});
		
		//Add common properties
		emailData.addProperty("emailType", emailType);
		emailData.addProperty("senderEmail", "help@mnbenefits.com");
		emailData.addProperty("applicationId", application.getId());
		emailData.addProperty("recepientEmail", recepientEmail);
		emailData.addProperty("locale", locale.toString());
		emailData.addProperty("completed-dt", completedAt.toString());
		
		//Get first&last name for specific page
		PageData pageData = appData.getPageData(pageName);
		emailData.addProperty("firstName",pageData.get("firstName").getValue(0));
		emailData.addProperty("lastName",pageData.get("lastName").getValue(0));
		
		//Add routing destinations
		JsonArray routingDestinationsArray = new JsonArray();
		destinations.forEach(destination -> {
			JsonObject dest = new JsonObject();
			dest.addProperty("name", destination.getName());
			dest.addProperty("phoneNumber", destination.getPhoneNumber());
			dest.addProperty("type", destination instanceof TribalNationRoutingDestination ? "TRIBAL" : "COUNTY");
			routingDestinationsArray.add(dest);

		});
		emailData.add("routingDestinations", routingDestinationsArray);

		return emailData;
		
	}

	public JsonObject createLaterDocsJsonObject(Application application, String recepientEmail, Locale locale) {
		return createBaseEmailJsonObject(
				application,recepientEmail,locale,"LATER_DOCS_CONFIRMATION","matchInfo");	
	}

	public JsonObject createHealthcareRenewalJsonObject(Application application, String recepientEmail, Locale locale) {
		return createBaseEmailJsonObject(
				application,recepientEmail,locale,"HEALTHCARE_RENEWAL","healthcareRenewalMatchInfo");	
	}
}
