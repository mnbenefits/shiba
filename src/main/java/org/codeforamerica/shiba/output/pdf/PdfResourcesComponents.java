package org.codeforamerica.shiba.output.pdf;

import static org.codeforamerica.shiba.output.Recipient.CASEWORKER;
import static org.codeforamerica.shiba.output.Recipient.CLIENT;
import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.output.Recipient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class PdfResourcesComponents {

  @Bean
  public List<Resource> getDefaultResources(@Value("classpath:cover-pages.pdf") Resource coverPages) {
		return (List.of(coverPages));
  }


  @Bean
  public Map<Recipient, Map<String, List<Resource>>> pdfResourceFillers(
      List<Resource> getDefaultResources, 
      List<Resource> getAdditionalIncome,
      List<Resource> getAdditionalHousehold1,
      List<Resource> getAdditionalHousehold2,
      List<Resource> getAdditionalHousehold3,
      List<Resource> getAdditionalHousehold4,
      List<Resource> getAdditionalHousehold5,
      List<Resource> getAdditionalHousehold6,
      List<Resource> getAdditionalHousehold7,
      List<Resource> getWhohasDisabilitySupp,
      List<Resource> getRetroactiveCoverageSupp) {
		return Map.of(CASEWORKER,
				Map.ofEntries(Map.entry("default", getDefaultResources), Map.entry("addIncome", getAdditionalIncome),
						Map.entry("addHousehold1.0", getAdditionalHousehold1),
						Map.entry("addHousehold2.0", getAdditionalHousehold2),
						Map.entry("addHousehold3.0", getAdditionalHousehold3),
						Map.entry("addHousehold4.0", getAdditionalHousehold4),
						Map.entry("addHousehold5.0", getAdditionalHousehold5),
						Map.entry("addHousehold6.0", getAdditionalHousehold6),
						Map.entry("addHousehold7.0", getAdditionalHousehold7),
						Map.entry("addDisabilitySupp", getWhohasDisabilitySupp),
						Map.entry("addRetroactiveCoverageSupp", getRetroactiveCoverageSupp)),
				CLIENT,
				Map.ofEntries(Map.entry("default", getDefaultResources), Map.entry("addIncome", getAdditionalIncome),
						Map.entry("addHousehold1.0", getAdditionalHousehold1),
						Map.entry("addHousehold2.0", getAdditionalHousehold2),
						Map.entry("addHousehold3.0", getAdditionalHousehold3),
						Map.entry("addHousehold4.0", getAdditionalHousehold4),
						Map.entry("addHousehold5.0", getAdditionalHousehold5),
						Map.entry("addHousehold6.0", getAdditionalHousehold6),
						Map.entry("addHousehold7.0", getAdditionalHousehold7),
						Map.entry("addDisabilitySupp", getWhohasDisabilitySupp),
						Map.entry("addRetroactiveCoverageSupp", getRetroactiveCoverageSupp)));
      }

}
