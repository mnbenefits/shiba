package org.codeforamerica.shiba.pages;

import org.codeforamerica.shiba.application.Application;
import org.springframework.stereotype.Service;

@Service
public class WicRecommendationService {
	
	public boolean showWicMessage(Application application) {
		System.out.println("===================WicRecommendationService appdata start====================");
		System.out.println("Household size = " + application.getApplicationData().getSubworkflows().get("household").size());

		application.getApplicationData().getSubworkflows().get("household").stream().forEach(x -> System.out.println(x.getPagesData().getPage("householdMemberInfo").get("dobAsDate")));
		System.out.println("===================WicRecommendationService appdata end====================");
		System.out.println("============ WicRecommendationService returning true");
		return true;
	}

}
