package org.codeforamerica.shiba.pages;

import org.codeforamerica.shiba.application.Application;
import org.springframework.stereotype.Service;

@Service
public class WicRecommendationService {
	
	public boolean showWicMessage(Application application) {
		System.out.println("===================WicRecommendationService appdata start====================");
		System.out.println(application.getApplicationData().getSubworkflows().get("household").size());
		//application.getApplicationData().getSubworkflows().get("household").stream().peek(x -> System.out.println("x=|" + x + "|"));
		application.getApplicationData().getSubworkflows().get("household").stream().forEach(x -> System.out.println(x.getPagesData()));
		System.out.println("===================WicRecommendationService appdata end====================");
		System.out.println("============ WicRecommendationService returning true");
		return true;
	}

}
