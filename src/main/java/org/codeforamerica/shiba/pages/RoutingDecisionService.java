package org.codeforamerica.shiba.pages;

import static org.codeforamerica.shiba.County.Beltrami;
import static org.codeforamerica.shiba.County.Clearwater;
import static org.codeforamerica.shiba.Program.*;
import static org.codeforamerica.shiba.TribalNation.*;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.APPLYING_FOR_TRIBAL_TANF;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.LIVING_IN_TRIBAL_NATION_BOUNDARY;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.SELECTED_TRIBAL_NATION;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.TRIBAL_NATION;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.LINEAL_DESCENDANT_WEN;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.IDENTIFY_TRIBAL_NATION_LATER_DOCS;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.IDENTIFY_TRIBAL_NATION_HEALTHCARE_RENEWAL;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.IDENTIFY_COUNTY_HEALTHCARE_RENEWAL;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.NATION_OF_RESIDENCE;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getBooleanValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.ServicingAgencyMap;
import org.codeforamerica.shiba.TribalNation;
import org.codeforamerica.shiba.TribalNationRoutingDestination;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.application.parsers.CountyParser;
import org.codeforamerica.shiba.application.parsers.DocumentListParser;
import org.codeforamerica.shiba.mnit.CountyRoutingDestination;
import org.codeforamerica.shiba.mnit.RoutingDestination;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("DanglingJavadoc")
@Service
@Slf4j
/**
 * The tests for this class live in a few places:
 * @see org.codeforamerica.shiba.pages.TribalNationsMockMvcTest
 * @see org.codeforamerica.shiba.output.MnitDocumentConsumerTest
 */
public class RoutingDecisionService {

  private final List<String> TRIBES_WE_CAN_ROUTE_TO = Stream.of(MilleLacsBandOfOjibwe,
      WhiteEarthNation, BoisForte, FondDuLac, GrandPortage, LeechLake, RedLakeNation,
      OtherFederallyRecognizedTribe).map(Enum::toString).toList();
  private final ServicingAgencyMap<TribalNationRoutingDestination> tribalNations;
  private final ServicingAgencyMap<CountyRoutingDestination> countyRoutingDestinations;
  public RoutingDecisionService(ServicingAgencyMap<TribalNationRoutingDestination> tribalNations,
			@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ServicingAgencyMap<CountyRoutingDestination> countyRoutingDestinations
			) {
    this.tribalNations = tribalNations;
    this.countyRoutingDestinations = countyRoutingDestinations;
  }

  public List<RoutingDestination> getRoutingDestinations(ApplicationData applicationData,
      Document document) {
    if (applicationData.getFlow() == FlowType.LATER_DOCS) {
	    return getLaterDocsRoutingDestinations(applicationData, document);
    }
    if (applicationData.getFlow() == FlowType.HEALTHCARE_RENEWAL) {
	    return getHealthcareRenewalRoutingDestinations(applicationData, document);
    }
    Set<String> programs = applicationData.getApplicantAndHouseholdMemberPrograms();
    County county = CountyParser.parse(applicationData);
    String tribeName = getFirstValue(applicationData.getPagesData(), SELECTED_TRIBAL_NATION);
    var pagesData= applicationData.getPagesData();
    Boolean isTribalNationMember = getBooleanValue(pagesData, TRIBAL_NATION);
    Boolean isLinealDescendantWEN = getBooleanValue(pagesData, LINEAL_DESCENDANT_WEN);
    if(Boolean.FALSE.equals(isTribalNationMember) && Boolean.FALSE.equals(isLinealDescendantWEN)){
    	return List.of(countyRoutingDestinations.get(county));
    }
    if (tribeName != null && TRIBES_WE_CAN_ROUTE_TO.contains(tribeName)) {
      TribalNation tribalNation = TribalNation.getFromName(tribeName);
      
      List<RoutingDestination> isRoutedToRedLakeAndBoundaries = routeToRedLakeAndBoundaries(programs, applicationData, county, tribeName);
      if(isRoutedToRedLakeAndBoundaries!=null) {
    	  return isRoutedToRedLakeAndBoundaries;
      }
      // Route members of Tribal Nations we service
      return switch (tribalNation) {
        case WhiteEarthNation -> routeWhiteEarthClients(programs, applicationData, document, county);
        case MilleLacsBandOfOjibwe, BoisForte, FondDuLac, GrandPortage, LeechLake ->
            routeClientsServicedByMilleLacs(
                programs, applicationData, document, county);
        case RedLakeNation -> routeRedLakeClients(programs, applicationData, county);
        case OtherFederallyRecognizedTribe -> routeClientsInOtherFederallyRecognizedTribe(
            county);
        default -> List.of(countyRoutingDestinations.get(county));
      };
    }
	//var pagesData = applicationData.getPagesData();
    //boolean isLinealDescendantWEN = getBooleanValue(pagesData, LINEAL_DESCENDANT_WEN);
    if(COUNTIES_SERVICED_BY_WHITE_EARTH.contains(county) && isLinealDescendantWEN){
    	return List.of(tribalNations.get(WhiteEarthNation));
    }

    // By default, just send to county
    return List.of(countyRoutingDestinations.get(county));
  }

	/*
	 * Rules for Later Docs routing (based on LaterDocsTribalNatonsRouting_Iteration2_12092024.xlsx):
	 * 
	 * Tribal Nation Member is Yes: AND
	 *   Tribal Nation is White Earth Nation:
	 *   County is Becker, Mahnomen or Clearwater - route White Earth Nation
	 * 
	 *   Tribal Nation is any other than Leech Lake or White Earth Nation 
	 *   AND county is Clearwater - route to both Clearwater and Red Lake Nation
	 * 
	 *   Tribal Nation is any other than Leech Lake 
	 *   AND county is Beltrami - route to Beltrami and Red Lake Nation
	 * 
	 *   Tribal Nation is Mille Lacs Band of Ojibwe 
	 *     AND county is Aitkin, Benton, Chisago, Crow Wing, Kanabec, Morrison, Mille Lacs, Pine 
	 *   OR
	 *   Tribal Nation is Bois Forte, Fond Du Lac, Grand Portage, Leech Lake, Mille Lacs Band Of Ojibwe, White Earth Nation  
	 *     AND county is Anoka, Hennepin, Ramsey - route to the county and to Mille Lacs Band of Ojibwe
	 * 
	 * Any other scenario:
	 *   Tribal Nation Member is Yes but doesn't fit above scenarios
	 *   OR Tribal Nation Member is No - route to the county
	 */
	private List<RoutingDestination> getLaterDocsRoutingDestinations(ApplicationData applicationData,
			Document document) {

		County county = CountyParser.parse(applicationData);
		
		// Is applicant a Tribal Nation member?
		Boolean isTribalNationMember = Boolean.valueOf("false");
		PageData tribalNationMemberPageData = applicationData.getPageData("tribalNationMember");
		if (tribalNationMemberPageData != null) {
			isTribalNationMember = Boolean.valueOf(tribalNationMemberPageData.get("isTribalNationMember").getValue(0));
		}
		if (isTribalNationMember.equals(true)) {
			// Applicant indicated they are a Tribal Nation member
			String tribalNationName = getFirstValue(applicationData.getPagesData(), IDENTIFY_TRIBAL_NATION_LATER_DOCS);

			// Here is where we determine if a Tribal Nation should be added to the routing
			// destinations.
			TribalNation tribalNation;
			if (tribalNationName != null && !tribalNationName.isEmpty()) {
				tribalNation = TribalNation.getFromName(tribalNationName);
				if (shouldRouteLaterDocsToWhiteEarthNation(county, tribalNation)) {
					return List.of(tribalNations.get(WhiteEarthNation));
				}
				if (shouldRouteLaterDocsToClearwaterAndRedLakeNation(county, tribalNation)) {
					return List.of(countyRoutingDestinations.get(Clearwater), tribalNations.get(RedLakeNation));
				}
				if (shouldRouteLaterDocsToBeltramiAndRedLakeNation(county, tribalNation)) {
					return List.of(countyRoutingDestinations.get(Beltrami), tribalNations.get(RedLakeNation));
				}
				if (shouldRouteLaterDocsToMilleLacsBandOfOjibweAndCounty(county, tribalNation)) {
					return List.of(countyRoutingDestinations.get(county), tribalNations.get(MilleLacsBandOfOjibwe));
				}
			}
		}
		// All other cases return the county
		// But stop sending documents to the "default county".
		if (county.equals(County.Other)) {
			log.info("Note: Later Docs application " + applicationData.getId() + " has no routing destinations, the county = County.Other.");
			return List.of();
		}
		return List.of(countyRoutingDestinations.get(county));
	}

	private boolean shouldRouteLaterDocsToWhiteEarthNation(County county, TribalNation tribalNation) {
		return (tribalNation.equals(TribalNation.WhiteEarthNation)
				&& COUNTIES_SERVICED_BY_WHITE_EARTH.contains(county));
	}

	private boolean shouldRouteLaterDocsToClearwaterAndRedLakeNation(County county, TribalNation tribalNation) {
		return (county.equals(County.Clearwater) && !(tribalNation.equals(TribalNation.LeechLake)
				|| tribalNation.equals(TribalNation.WhiteEarthNation)));
	}

	private boolean shouldRouteLaterDocsToBeltramiAndRedLakeNation(County county, TribalNation tribalNation) {
		return (county.equals(County.Beltrami) && !tribalNation.equals(TribalNation.LeechLake));
	}

	private boolean shouldRouteLaterDocsToMilleLacsBandOfOjibweAndCounty(County county, TribalNation tribalNation) {
		return ((tribalNation.equals(TribalNation.MilleLacsBandOfOjibwe) && MILLE_LACS_RURAL_COUNTIES.contains(county))
				|| (MN_CHIPPEWA_TRIBES.contains(tribalNation) && URBAN_COUNTIES.contains(county)));
	}

	private List<RoutingDestination> getHealthcareRenewalRoutingDestinations(ApplicationData applicationData,
			Document document) {
		List<RoutingDestination> routingDestinations = new ArrayList<RoutingDestination>();

		String countyName = getFirstValue(applicationData.getPagesData(), IDENTIFY_COUNTY_HEALTHCARE_RENEWAL);
		if (countyName != null && !countyName.isEmpty()) {
			County county = County.getForName(countyName);
			routingDestinations.add(countyRoutingDestinations.get(county));
		}

		String tribalNationName = getFirstValue(applicationData.getPagesData(),
				IDENTIFY_TRIBAL_NATION_HEALTHCARE_RENEWAL);
		if (tribalNationName != null && !tribalNationName.isEmpty()) {
			TribalNation tribalNation = TribalNation.getFromName(tribalNationName);
			routingDestinations.add(tribalNations.get(tribalNation));
		}

		if (routingDestinations.size() < 1) {
			log.info("Note: Healthcare renewal upload " + applicationData.getId() + " has no routing destinations.");
		}
		return routingDestinations;
	}
	
  public RoutingDestination getRoutingDestinationByName(String name) {
    RoutingDestination result;
    try {
      result = tribalNations.get(TribalNation.getFromName(name));
    } catch (IllegalArgumentException e) {
      result = countyRoutingDestinations.get(County.getForName(name));
    }
    return result;
  }
  
	/**
	 * Find the routing destinations for the application.
	 * @param application
	 * @return HashSet of RoutingDestination objects
	 */
	public Set<RoutingDestination> findRoutingDestinations(Application application) {
		Set<RoutingDestination> allRoutingDestinations = new HashSet<>();
		ApplicationData applicationData = application.getApplicationData();
		DocumentListParser.parse(applicationData).forEach(doc -> {
			List<RoutingDestination> routingDestinations = getRoutingDestinations(applicationData, doc);
			allRoutingDestinations.addAll(routingDestinations);
		});
		return allRoutingDestinations;
	}

  private List<RoutingDestination> routeClientsInOtherFederallyRecognizedTribe(
      County county) {
    if (!county.equals(County.Beltrami)) {
      return List.of(countyRoutingDestinations.get(county));
    }
    return List.of(tribalNations.get(RedLakeNation));
  }
  
  private List<RoutingDestination> routeRedLakeClients(Set<String> programs,
      ApplicationData applicationData, County county) {

    boolean isLivingInTribalNationBoundary = getBooleanValue(applicationData.getPagesData(),
        LIVING_IN_TRIBAL_NATION_BOUNDARY);
    if (!isLivingInTribalNationBoundary || isOnlyApplyingForGrh(programs, applicationData)) {
      return List.of(countyRoutingDestinations.get(county));
    }

    if (programs.contains(GRH)) {
      return List.of(countyRoutingDestinations.get(county), tribalNations.get(RedLakeNation));
    }

    return List.of(tribalNations.get(RedLakeNation));
  }

  private boolean isOnlyApplyingForGrh(Set<String> programs, ApplicationData applicationData) {
    return programs.size() == 1 && programs.contains(GRH) &&
        !isApplyingForTribalTanf(applicationData.getPagesData());
  }

  private List<RoutingDestination> routeWhiteEarthClients(Set<String> programs,
      ApplicationData applicationData,
      Document document, County county) {

    var pagesData = applicationData.getPagesData();
    var selectedTribeName = getFirstValue(pagesData, SELECTED_TRIBAL_NATION);

    if (livesInCountyServicedByWhiteEarth(county, selectedTribeName)) {
      return List.of(tribalNations.get(WhiteEarthNation));
    }

    if (URBAN_COUNTIES.contains(county)) {
      return routeClientsServicedByMilleLacs(programs, applicationData, document, county);
    }
    return List.of(countyRoutingDestinations.get(county));
  }
  
  private boolean livesInCountyServicedByWhiteEarth(County county, String selectedTribeName) {
    return selectedTribeName != null
        && selectedTribeName.equals(WhiteEarthNation.toString())
        && COUNTIES_SERVICED_BY_WHITE_EARTH.contains(county);
  }

  private boolean isApplyingForTribalTanf(PagesData pagesData) {
    return getBooleanValue(pagesData, APPLYING_FOR_TRIBAL_TANF);
  }


  private List<RoutingDestination> routeClientsServicedByMilleLacs(Set<String> programs,
      ApplicationData applicationData, Document document, County county) {
    List<RoutingDestination> result = new ArrayList<>();
    if (shouldSendToMilleLacs(applicationData, document)) {
      result.add(tribalNations.get(MilleLacsBandOfOjibwe));
    }
    if (shouldSendToCounty(programs, applicationData, document)) {
      result.add(countyRoutingDestinations.get(county));
    }
    return result;
  }

  private boolean shouldSendToCounty(Set<String> programs, ApplicationData applicationData,
      Document document) {
    boolean shouldSendToMilleLacs = shouldSendToMilleLacs(applicationData, document);
    boolean isApplicableForCcap = programs.contains(CCAP) &&
        (document == Document.CCAP || document == Document.UPLOADED_DOC);
    return !shouldSendToMilleLacs
        || isApplicableForCcap
        || programs.contains(SNAP) || programs.contains(CASH) || programs.contains(GRH);
  }

  private boolean shouldSendToMilleLacs(ApplicationData applicationData, Document document) {
    var pagesData = applicationData.getPagesData();
    var selectedTribeName = getFirstValue(pagesData, SELECTED_TRIBAL_NATION);
    var programs = applicationData.getApplicantAndHouseholdMemberPrograms();

    return selectedTribeName != null
        && tribalNations.get(TribalNation.getFromName(selectedTribeName)) != null
        && (isApplyingForTribalTanf(pagesData) || programs.contains(EA))
        && Document.CCAP != document;
  }
  
  private List<RoutingDestination> routeToRedLakeAndBoundaries(Set<String> programs, ApplicationData applicationData,
			County county, String tribeName) {

		Boolean isTribalTANF = getBooleanValue(applicationData.getPagesData(), APPLYING_FOR_TRIBAL_TANF);
		Boolean isLinealDescendantWEN = getBooleanValue(applicationData.getPagesData(), LINEAL_DESCENDANT_WEN);
		String nationOfResidence = getFirstValue(applicationData.getPagesData(), NATION_OF_RESIDENCE);

		if (RedLakeNation.toString().equals(nationOfResidence)) {
			if (county.equals(County.Clearwater) || county.equals(County.Beltrami)) {
				if (programs.contains(SNAP) || programs.contains(EA) || programs.contains(CCAP)
						|| Boolean.TRUE.equals(isTribalTANF)) {

					return List.of(tribalNations.get(RedLakeNation));
				}
				if (programs.contains(CASH) || programs.contains(GRH)) {
					return List.of(countyRoutingDestinations.get(county));
				}
			}
		}

		if (tribeName != null && TRIBES_WE_CAN_ROUTE_TO.contains(tribeName)) {
			if (!LeechLake.equals(TribalNation.getFromName(tribeName))) {
				if (county.equals(County.Beltrami)) {

					if (programs.contains(SNAP) || programs.contains(EA) || programs.contains(CCAP)
							|| Boolean.TRUE.equals(isTribalTANF)) {
						return List.of(tribalNations.get(RedLakeNation));
					}
					if (programs.contains(CASH) || programs.contains(GRH)) {
						return List.of(countyRoutingDestinations.get(county));
					}
				}
			}
		}

//		if (Boolean.TRUE.equals(isLinealDescendantWEN)) {
//			if (county.equals(County.Beltrami)) {
//				if (programs.contains(SNAP) || programs.contains(EA) || programs.contains(CASH)
//						|| programs.contains(GRH) || programs.contains(CCAP)) {
//					return List.of(countyRoutingDestinations.get(county));
//				}
//			}
//		}

		if (!tribeName.equals(LeechLake.toString())) {
			if (county.equals(County.Beltrami)) {
				if (programs.contains(CASH) || programs.contains(GRH)) {
					return List.of(countyRoutingDestinations.get(county));
				}
				if (programs.contains(SNAP) || programs.contains(EA) || programs.contains(CCAP)) {
					return List.of(tribalNations.get(RedLakeNation));
				}
			}
		}

		if (!tribeName.equals(WhiteEarthNation.toString())) {
			if (county.equals(County.Clearwater)) {
				if (programs.contains(SNAP) || programs.contains(EA) || programs.contains(CASH)
						|| programs.contains(GRH) || programs.contains(CCAP)) {
					return List.of(countyRoutingDestinations.get(county));
				}
			}
		}

		return null;
	}
}

