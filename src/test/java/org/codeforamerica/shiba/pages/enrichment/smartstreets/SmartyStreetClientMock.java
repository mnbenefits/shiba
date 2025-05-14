package org.codeforamerica.shiba.pages.enrichment.smartstreets;

import java.util.Optional;

import org.codeforamerica.shiba.pages.enrichment.Address;
import org.codeforamerica.shiba.pages.enrichment.LocationClient;
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
	public Optional<Address> validateAddress(Address address){
		if(true) {
			try {
				FakeException.makeException();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
		}
		return Optional.empty();
	}
	
	public static SmartyStreetClientMock buildMock() {
		return new SmartyStreetClientMock("someId", "someToken", "someUrl");
	}
	
	private class FakeException{
		
		public static void makeException() throws Exception {
			throw new Exception("SmartyStreetClientMock");
		}
	}

}
