package org.codeforamerica.shiba.pages;

import java.util.List;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.springframework.stereotype.Service;

@Service
public class TapRecommendationService {

	private final List<String> tapCounties = List.of("Anoka","Carver", "Dakota","Hennepin","Ramsey","Scott","Washington");
	
	public boolean showTapMessage(ApplicationData applicationData) {
		return  showTapForThisCounty(applicationData);
	}

	
	private boolean showTapForThisCounty(ApplicationData applicationData) {
		return tapCounties.stream()
	            .anyMatch(county -> county.equalsIgnoreCase(applicationData.getOriginalCounty()));
		
	}

}
