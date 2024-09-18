
package org.codeforamerica.shiba.pages.rest;


import org.springframework.context.annotation.Bean; import
org.springframework.context.annotation.Configuration;

@Configuration public class CommHubConfig {

	@Bean 
	public String commHubUrl() {  
		return "http://localhost:8081/mnb-confirmation";
	}

	@Bean 
	public Boolean commHubEnabled() { 
		return true; 
	}

}
