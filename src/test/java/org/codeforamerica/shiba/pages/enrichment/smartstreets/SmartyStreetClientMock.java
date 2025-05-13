package org.codeforamerica.shiba.pages.enrichment.smartstreets;

import java.util.Optional;

import org.codeforamerica.shiba.pages.enrichment.Address;
import org.codeforamerica.shiba.pages.enrichment.LocationClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class SmartyStreetClientMock implements LocationClient {
	
	  private final String authId;
	  private final String authToken;
	  private final String smartyStreetUrl;
	  
	  public SmartyStreetClientMock(String authId,
		       String authToken,
		       String smartyStreetUrl) {
		    this.authId = authId;
		    this.authToken = authToken;
		    this.smartyStreetUrl = smartyStreetUrl;
		  }

	@Override
	public Optional<Address> validateAddress(Address address) {
		// TODO emj Auto-generated method stub
		//System.out.println("!!!!!!! SmartyStreetClientMock validateAddress !!!!!!!!!!!");
		return Optional.empty();
	}
	
	public static SmartyStreetClientMock buildMock() {
		//System.out.println("%%% SmartyStreetClientMock buildMock %%%%");//TODO emj delete
		return new SmartyStreetClientMock("someId", "someToken", "someUrl");
	}

}
