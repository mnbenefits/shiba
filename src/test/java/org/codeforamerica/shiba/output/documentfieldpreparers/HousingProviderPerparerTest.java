package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

public class HousingProviderPerparerTest {

	 HousingProviderPreparer preparer = new HousingProviderPreparer();
	 
	  @Test
	  public void shouldNotHaveProviderWhenGRHNotSelected() {
	    ApplicationData applicationData = new TestApplicationDataBuilder()
	        .withPageData("housingProvider", "housingProvider",
	            List.of("false"))
	        .build();

	    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
	        .applicationData(applicationData)
	        .build(), Document.CAF, null);

	    assertThat(result).containsOnly(
	        new DocumentField(
	            "housingSupportVendor",
	            "housingSupportVendor",
	            List.of("Does not have a provider."),
	            DocumentFieldType.ENUMERATED_SINGLE_VALUE
	        ));
	  }
	  
	  @Test
	  public void shouldHaveNoProviderEnteredWhenNameAndNumberAreBlank() {
	    ApplicationData applicationData = new TestApplicationDataBuilder()
	        .withPageData("housingProvider", "housingProvider",
	            List.of("true"))
	        .withPageData("housingProviderInfo", "housingProviderName",
		            List.of(""))
	        .withPageData("housingProviderInfo", "housingProviderVendorNumber",
		            List.of(""))
	        .build();

	    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
	        .applicationData(applicationData)
	        .build(), Document.CAF, null);

	    assertThat(result).containsOnly(
	        new DocumentField(
	            "housingSupportVendor",
	            "housingSupportVendor",
	            List.of("No provider entered."),
	            DocumentFieldType.ENUMERATED_SINGLE_VALUE
	        ));
	  }

	  @Test
	  public void shouldHaveOnlyProviderEnteredWhenNumberIsBlank() {
	    ApplicationData applicationData = new TestApplicationDataBuilder()
	        .withPageData("housingProvider", "housingProvider",
	            List.of("true"))
	        .withPageData("housingProviderInfo", "housingProviderName",
		            List.of("Provider Name"))
	        .withPageData("housingProviderInfo", "housingProviderVendorNumber",
		            List.of(""))
	        .build();

	    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
	        .applicationData(applicationData)
	        .build(), Document.CAF, null);

	    assertThat(result).containsOnly(
	        new DocumentField(
	            "housingSupportVendor",
	            "housingSupportVendor",
	            List.of("Provider Name"),
	            DocumentFieldType.ENUMERATED_SINGLE_VALUE
	        ));
	  }
	  
	  @Test
	  public void shouldHaveOnlyProviderNumberEnteredWhenNameIsBlank() {
	    ApplicationData applicationData = new TestApplicationDataBuilder()
	        .withPageData("housingProvider", "housingProvider",
	            List.of("true"))
	        .withPageData("housingProviderInfo", "housingProviderName",
		            List.of(""))
	        .withPageData("housingProviderInfo", "housingProviderVendorNumber",
		            List.of("123456"))
	        .build();

	    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
	        .applicationData(applicationData)
	        .build(), Document.CAF, null);

	    assertThat(result).containsOnly(
	        new DocumentField(
	            "housingSupportVendor",
	            "housingSupportVendor",
	            List.of("123456"),
	            DocumentFieldType.ENUMERATED_SINGLE_VALUE
	        ));
	  }
	  
	  @Test
	  public void shouldHaveBothProviderNameAndNumberWithSlash() {
	    ApplicationData applicationData = new TestApplicationDataBuilder()
	        .withPageData("housingProvider", "housingProvider",
	            List.of("true"))
	        .withPageData("housingProviderInfo", "housingProviderName",
		            List.of("Provider Name"))
	        .withPageData("housingProviderInfo", "housingProviderVendorNumber",
		            List.of("123456"))
	        .build();

	    List<DocumentField> result = preparer.prepareDocumentFields(Application.builder()
	        .applicationData(applicationData)
	        .build(), Document.CAF, null);

	    assertThat(result).containsOnly(
	        new DocumentField(
	            "housingSupportVendor",
	            "housingSupportVendor",
	            List.of("Provider Name / 123456"),
	            DocumentFieldType.ENUMERATED_SINGLE_VALUE
	        ));
	  }
}
