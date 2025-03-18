package org.codeforamerica.shiba.pages;

import static org.codeforamerica.shiba.County.Beltrami;
import static org.codeforamerica.shiba.County.Clearwater;
import static org.codeforamerica.shiba.Program.*;
import static org.codeforamerica.shiba.TribalNation.*;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.APPLYING_FOR_TRIBAL_TANF;
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
    // From this point we look at Document type rather than Flow type
    if (document.equals(Document.CCAP)) {
    	return getCcapRoutingDestinations(applicationData, document);
    }
    // This handles Document types: CAF, CERTAIN_POPS, UPLOADED_DOC, XML
    return getApplicationRoutingDestinations(applicationData, document);
  }
  

	/*
	 * Rules for Later Docs routing (based on LaterDocsTribalNatonsRouting_Iteration2_12092024.xlsx):
	 * 
	 * Tribal Nation Member is Yes: AND
	 * 
	 * 	 Live within the boundaries of a Tribal Nation is YES and live within RLN
	 * 	 route to both Clearwater and Red Lake Nation
	 * 
	 *   Live within the boundaries of a Tribal Nation is NO OR
	 *   Live within the boundaries of a Tribal Nation is Yes And
	 *   Nation of residence is not live within RLN
	 *   route Clearwater county only
	 * 
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
				//Does applicant live in nationsBoundary
				Boolean doesLiveInNationsBoundary = Boolean.valueOf("false");
				PageData nationsBoundaryPageData = applicationData.getPageData("nationsBoundary");
				
				tribalNation = TribalNation.getFromName(tribalNationName);
				
				if (nationsBoundaryPageData != null) {
					//only applicants who live within Clearwater county can see this condition
					//unless changed in the future

					doesLiveInNationsBoundary = Boolean
							.valueOf(nationsBoundaryPageData.get("livingInNationBoundary").getValue(0));
					if (doesLiveInNationsBoundary) {
						String nationOfResidence = getFirstValue(applicationData.getPagesData(), NATION_OF_RESIDENCE);
						TribalNation selectedNationOfResidence = TribalNation.getFromName(nationOfResidence);
						if (shouldRouteLaterDocsToClearwaterAndRedLakeNationWhenLiveWithinNatioBoundary(
								selectedNationOfResidence)) {
							return List.of(countyRoutingDestinations.get(Clearwater), tribalNations.get(RedLakeNation));
						}
						if (tribalNation.equals(TribalNation.LeechLake)) {
							if (shouldRouteLaterDocsToWENWhenLiveWithinNatioBoundary(selectedNationOfResidence)) {
								return List.of(tribalNations.get(WhiteEarthNation));
							}
						} else {
							return List.of(countyRoutingDestinations.get(Clearwater));
						}
					}
					// when boundary question answer is No
					return List.of(countyRoutingDestinations.get(Clearwater));
				}
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

	private boolean shouldRouteLaterDocsToClearwaterAndRedLakeNationWhenLiveWithinNatioBoundary(TribalNation nationOfesidancy) {
		return (nationOfesidancy.equals(TribalNation.RedLakeNation));
	}
	
	private boolean shouldRouteLaterDocsToWENWhenLiveWithinNatioBoundary(TribalNation nationOfesidancy) {
		return (nationOfesidancy.equals(TribalNation.WhiteEarthNation));
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

  private boolean isApplyingForTribalTanf(PagesData pagesData) {
    return getBooleanValue(pagesData, APPLYING_FOR_TRIBAL_TANF);
  }

  public List<RoutingDestination> routeToRedLakeAndBoundaries(Set<String> programs, ApplicationData applicationData,
			County county, String tribeName) {

		Boolean isTribalTANF = getBooleanValue(applicationData.getPagesData(), APPLYING_FOR_TRIBAL_TANF);
		Boolean isLinealDescendantWEN = getBooleanValue(applicationData.getPagesData(), LINEAL_DESCENDANT_WEN);
		String nationOfResidence = getFirstValue(applicationData.getPagesData(), NATION_OF_RESIDENCE);
		List<RoutingDestination> routingDestinations = new ArrayList<RoutingDestination>();

		if (RedLakeNation.toString().equals(nationOfResidence)) {
			if (county.equals(County.Clearwater) || county.equals(County.Beltrami)) {
				if (programs.contains(SNAP) || programs.contains(EA) || programs.contains(CCAP)
						|| Boolean.TRUE.equals(isTribalTANF)) {

					routingDestinations.add(tribalNations.get(RedLakeNation));
				}
				if (programs.contains(CASH) || programs.contains(GRH)) {
					routingDestinations.add(countyRoutingDestinations.get(county));
				}
			}
		} else {

			if (!tribeName.equals(LeechLake.toString())) {
				if (county.equals(County.Beltrami)) {
					if (programs.contains(SNAP) || programs.contains(EA) || programs.contains(CCAP)
							|| Boolean.TRUE.equals(isTribalTANF)) {
						routingDestinations.add(tribalNations.get(RedLakeNation));
					}
					if (programs.contains(CASH) || programs.contains(GRH)) {
						routingDestinations.add(countyRoutingDestinations.get(county));
					}
				}
			}

			if (Boolean.TRUE.equals(isLinealDescendantWEN)) {
				if (county.equals(County.Beltrami)) {
					if (programs.contains(SNAP) || programs.contains(EA) || programs.contains(CASH)
							|| programs.contains(GRH) || programs.contains(CCAP)) {
						routingDestinations.add(countyRoutingDestinations.get(county));
					}
				}
			}

			if (!tribeName.equals(WhiteEarthNation.toString())) {
				if (county.equals(County.Clearwater)) {
					if (programs.contains(SNAP) || programs.contains(EA) || programs.contains(CASH)
							|| programs.contains(GRH) || programs.contains(CCAP)) {
						routingDestinations.add(countyRoutingDestinations.get(county));
					}
				}
			}
		}

		return routingDestinations;
	}
  
  
  /*
	 * Rules for application routing are defined in MNbenefits Tribal Nation Routing Use Cases.xlsx.
	 * A CCAP will always have just one routing destination because is serves just one program.
	 * The following are the possible routing destinations for a CCAP document:
	 *  - Red Lake Nation - when:
	 *      the applicant lives within the boundaries of Red Lake Nation
	 *      AND the applicant  and is a member of any tribal nation
	 *      ANDthe applicant is a resident of Beltrami or Clearwater County
	 *    OR
	 *      the applicant does not live within the boundaries of Red Lake Nation
	 *      AND the applicant is an enrolled member of any Tribal Nation except Leech Lake
	 *      AND the applicant is a resident of Beltrami County
	 *    OR
	 *         
	 *  - White Earth Nation - when:
	 *      the applicant is a tribal member or a lineal decendant of a tribal member of White Earth Nation
	 *      AND the applicant is a resident of county serviced by White Earth Nation
	 *      
	 *  - The county of residence only
	 */ 
	private List<RoutingDestination> getCcapRoutingDestinations(ApplicationData applicationData, Document document) {
		    var pagesData= applicationData.getPagesData();
		    // TODO: Should we be returning a routing destination for Document.CCAP if the application does
		    //       not include CCAP?  For now, we do to remain compatible, mainly so that tests do not break.
		    // Simply set the list of programs to CCAP as it is the only program that should be considered.
		    Set<String> programs = Set.of("CCAP");
		    
		    County county = CountyParser.parse(applicationData);
		    boolean isTribalNationMember = getBooleanValue(pagesData, TRIBAL_NATION);
		    boolean isLinealDescendantWEN = getBooleanValue(pagesData, LINEAL_DESCENDANT_WEN);
		    
		    // Shortcut, if not a Tribal Nation member then the routing destination is the county of residence
		    if(!isTribalNationMember && !isLinealDescendantWEN) {
		    	return countyOfResidence(county, applicationData);
		    }
		    
		    // now handle the Tribal Nation member cases
		    String tribalNationName = getFirstValue(applicationData.getPagesData(), SELECTED_TRIBAL_NATION);
	
			TribalNation tribalNation = null;
			if (tribalNationName != null && !tribalNationName.isEmpty()) {
				tribalNation = TribalNation.getFromName(tribalNationName);
			}
			
	        if (isLinealDescendantWEN) { // special case - WEN lineal descendant
	        	if (shouldRouteApplicationToWhiteEarthNation(county, tribalNation, isLinealDescendantWEN)) {
	        		return List.of(tribalNations.get(WhiteEarthNation));
	        	}
	        }
			
			// If there is no TribalNation then route to the county
			if (tribalNation==null) {
				return countyOfResidence(county, applicationData);
			}
				
			if (shouldRouteApplicationToWhiteEarthNation(county, tribalNation, isLinealDescendantWEN)) {
					return List.of(tribalNations.get(WhiteEarthNation));
			}
			
			boolean livesWithinNationsBoundaries = livesInBoudariesOfRedLakeNation(pagesData);
			if (shouldRouteApplicationToRedLakeNation(county, tribalNation, programs, livesWithinNationsBoundaries)) {
				return List.of(tribalNations.get(RedLakeNation));
		    }
			
			return countyOfResidence(county, applicationData);
	}
  
  
    /*
	 * Rules for application routing are defined in MNbenefits Tribal Nation Routing Use Cases.xlsx:
	 * The following are the possible routing destination combinations:
	 *  - Red Lake Nation
	 *  - Red Lake Nation + Beltrami County
	 *  - White Earth Nation
	 *  - Mille Lacs Band of Ojibwe
	 *  - Mille Lacs Band of Ojibwe + MLBO rural county or MLBO urban county
	 *  - The county of residence only
	 */ 
	private List<RoutingDestination> getApplicationRoutingDestinations(ApplicationData applicationData, Document document) {
		    var pagesData= applicationData.getPagesData();
		    // This method will add pseudo program TANF (if present in the application)
		    Set<String> programs = applicantPrograms(applicationData);
		    // When Document type is CAF remove CCAP so that it doesn't influence routing but include Tribal TANF if it is in the application
		    if (document.equals(Document.CAF)) {
		    	programs.remove("CCAP");
		    }
		    County county = CountyParser.parse(applicationData);
		    boolean isTribalNationMember = getBooleanValue(pagesData, TRIBAL_NATION);
		    boolean isLinealDescendantWEN = getBooleanValue(pagesData, LINEAL_DESCENDANT_WEN);
		    
		    // Shortcut, if not a Tribe Nation member then the routing destination is the county of residence
		    if(!isTribalNationMember && !isLinealDescendantWEN) {
		    	return countyOfResidence(county, applicationData);
		    }
	
		    // Now handle the Tribal Nation member cases
		    String tribalNationName = getFirstValue(applicationData.getPagesData(), SELECTED_TRIBAL_NATION);
	
			TribalNation tribalNation = null;
			if (tribalNationName != null && !tribalNationName.isEmpty()) {
				tribalNation = TribalNation.getFromName(tribalNationName);
			} else if (isLinealDescendantWEN) {
				// test for routing to WEN otherwise just route to the county
				if (shouldRouteApplicationToWhiteEarthNation(county, tribalNation, isLinealDescendantWEN)) {
					return List.of(tribalNations.get(WhiteEarthNation));
				} else {
					return countyOfResidence(county, applicationData);
				}
			}
			else {
				return countyOfResidence(county, applicationData);			
			}
			
			// At this point we are a tribal nation member
			if (shouldRouteApplicationToWhiteEarthNation(county, tribalNation, isLinealDescendantWEN)) {
					return List.of(tribalNations.get(WhiteEarthNation));
			}
			
			boolean livesWithinNationsBoundaries = livesInBoudariesOfRedLakeNation(pagesData);
			if (shouldRouteApplicationToRedLakeNation(county, tribalNation, programs, livesWithinNationsBoundaries)) {
				return List.of(tribalNations.get(RedLakeNation));
		    }
			
			if (shouldRouteApplicationToRedLakeNationAndBeltramiOrClearwaterCounty(county, tribalNation, programs, livesWithinNationsBoundaries)) {
				return List.of(tribalNations.get(RedLakeNation), countyRoutingDestinations.get(county));
		    }
			
			if (shouldRouteApplicationToMilleLacsBandOfOjibwe(county, tribalNation, programs)) {
				return List.of(tribalNations.get(MilleLacsBandOfOjibwe));
		    }
			
			if (shouldRouteApplicationToMilleLacsBandOfOjibweAndCounty(county, tribalNation, programs, document)) {
				return List.of(tribalNations.get(MilleLacsBandOfOjibwe), countyRoutingDestinations.get(county));
		    }
			
			return countyOfResidence(county, applicationData);
	}

	/*
	 * There are two scenarios where we route to Red Lake Nation only
	 * 1.) County of residence is one of: Beltrami, Clearwater
	 *     AND tribal member of any tribe (Note: this method would not be invoked unless the applicant was a tribal member)
	 *     AND Lives within boundaries of Red Lake Nation
	 *     AND all selected programs are from this set: SNAP, EA, Child Care, Tribal TANF
	 * 
	 * 2.) County of residence is Beltrami
	 *     AND tribal member of any tribe except Leech Lake
	 *     AND does not live within the boundaries of Red Lake Nation
	 *     AND all selected programs are from this set: SNAP, EA, Child Care, Tribal TANF
	 */
	private boolean shouldRouteApplicationToRedLakeNation(County county, TribalNation tribe, Set<String> programs,
			boolean livesInRedLakeNationBoundaries) {
		if (List.of(Beltrami, Clearwater).contains(county) && livesInRedLakeNationBoundaries
				&& allProgramsAreFromSet(programs, Set.of("SNAP", "EA", "CCAP", "TANF"))) {
			return true;
		}
	
		if (List.of(Beltrami).contains(county) && !livesInRedLakeNationBoundaries && !List.of(LeechLake).contains(tribe)
				&& allProgramsAreFromSet(programs, Set.of("SNAP", "EA", "CCAP", "TANF"))) {
			return true;
		}
		return false;
	}

	/*
	 * There are two scenarios where we route to Red Lake Nation and Beltrami or Clearwater County
	 * 1.) County of residence is Beltrami or Clearwater
	 *     AND tribal member of any tribe
	 *     AND Lives within boundaries of Red Lake Nation
	 *     AND at least one selected program is from this set: SNAP, EA, Child Care, Tribal TANF
	 *     AND at least one selected program is from this set: CASH, GRH
	 * 
	 * 2.) County of residence is Beltrami
	 *     AND tribal member of any tribe except Leech Lake
	 *     AND does not live within the boundaries of Red Lake Nation
	 *     AND at lest one selected programs are from this set: SNAP, EA, Child Care, Tribal TANF
	 *     AND at least one selected program is from this set: CASH, GRH
	 */
	private boolean shouldRouteApplicationToRedLakeNationAndBeltramiOrClearwaterCounty(County county, TribalNation tribe, 
			        Set<String> programs, boolean livesInRedLakeNationBoundaries) {
		if (List.of(Beltrami,Clearwater).contains(county) && livesInRedLakeNationBoundaries
				&& anyProgramsAreFromSet(programs, Set.of("SNAP", "EA", "CCAP", "TANF"))
				&& anyProgramsAreFromSet(programs, Set.of("CASH", "GRH"))) {
			return true;
		}
	
		if (List.of(Beltrami).contains(county) && !livesInRedLakeNationBoundaries && !List.of(LeechLake).contains(tribe)
				&& anyProgramsAreFromSet(programs, Set.of("SNAP", "EA", "CCAP", "TANF"))
				&& anyProgramsAreFromSet(programs, Set.of("CASH", "GRH"))) {
			return true;
		}
		return false;
	}

	/*
	 * There is one scenario where we route to White Earth Nation only
	 * 1.) County of residence is one of: Becker, Clearwater, Mahnomen
	 *     AND a tribal member of White Earth Nation OR a lineal decendant of a member of White Earth Nation 
	 */
	private boolean shouldRouteApplicationToWhiteEarthNation(County county, TribalNation tribalNation, boolean linealDecendantWEN) {
		if (COUNTIES_SERVICED_BY_WHITE_EARTH.contains(county)
			&& ((tribalNation != null && tribalNation.equals(TribalNation.WhiteEarthNation)) || linealDecendantWEN)) {
			return true;
		}
		return false;
	}

	/*
	 * There are two scenarios where we route to Mille Lacs Band of Ojibwe only
	 * 1.) County of residence is one of MLBO Rural Counties 
	 * AND tribal member of Mille Lacs Band of Ojibwe 
	 * AND selected programs are SNAP and Tribal TANF or just Tribal TANF
	 * 
	 * 2.) County of residence is one of MLBO Urban Counties 
	 * AND tribal member of one of MN Chippewa Tribes 
	 * AND selected programs are SNAP and Tribal TANF or just Tribal TANF
	 */
	private boolean shouldRouteApplicationToMilleLacsBandOfOjibwe(County county, TribalNation tribe, Set<String> programs) {
		if (MILLE_LACS_RURAL_COUNTIES.contains(county)
				&& Set.of(MilleLacsBandOfOjibwe).contains(tribe)
				&& (programsAreExactlyThisSet(programs, Set.of("SNAP", "TANF"))
						|| programsAreExactlyThisSet(programs, Set.of("TANF")))) {
			return true;
		}
		if (URBAN_COUNTIES.contains(county)
				&& MN_CHIPPEWA_TRIBES.contains(tribe)
				&& (programsAreExactlyThisSet(programs, Set.of("SNAP", "TANF"))
						|| programsAreExactlyThisSet(programs, Set.of("TANF")))) {
			return true;
		}
		
		return false;
	}

	/*
	 * There are two scenarios where we route to Mille Lacs Band of Ojibwe and a county
	 * 1.) County of residence is one of MLBO Rural Counties
	 *     AND tribal member of Mille Lacs Band of Ojibwe
	 *     AND selected programs includes Tribal TANF
	 *     AND selected programs includes at least one of SNAP, CASH, EA, Child Care, GRH (but not exactly Tribal TANF and SNAP)
	 * 
	 * 2.) County of residence is one of MLBO Urban Counties
	 *     AND tribal member of one of MN Chippewa Tribes
	 *     AND selected programs includes Tribal TANF
	 *     AND selected programs includes at least one of SNAP, CASH, EA, Child Care, GRH (but not exactly Tribal TANF and SNAP)
	 */
	private boolean shouldRouteApplicationToMilleLacsBandOfOjibweAndCounty(County county, TribalNation tribe, 
			        Set<String> programs, Document document) {
		if (MILLE_LACS_RURAL_COUNTIES.contains(county)
				&& Set.of(MilleLacsBandOfOjibwe).contains(tribe)
				&& anyProgramsAreFromSet(programs, Set.of("TANF"))
				&& anyProgramsAreFromSet(programs, Set.of("SNAP", "CASH", "EA", "CCAP", "GRH"))
				&& !programsAreExactlyThisSet(programs, Set.of("SNAP", "TANF"))) {
			return true;
		}
		if (URBAN_COUNTIES.contains(county)
				&& MN_CHIPPEWA_TRIBES.contains(tribe)
				&& anyProgramsAreFromSet(programs, Set.of("TANF"))
				&& anyProgramsAreFromSet(programs, Set.of("SNAP", "CASH", "EA", "CCAP", "GRH"))
				&& !programsAreExactlyThisSet(programs, Set.of("SNAP", "TANF"))) {
			return true;
		}
	
		return false;
	}

	/*
	 * Determines if all of the application programs are included in a specific set of programs.
	 */
	private boolean allProgramsAreFromSet(Set<String> applicationPrograms, Set<String> setPrograms) {
		for (String program : applicationPrograms) {
			if (!setPrograms.contains(program)) return false;
		}
		return true;
	}

	/*
	 * Determines if any one of the application programs are included in a specific set of programs.
	 */
	private boolean anyProgramsAreFromSet(Set<String> applicationPrograms, Set<String> setPrograms) {
		for (String program : applicationPrograms) {
			if (setPrograms.contains(program)) return true;
		}
		return false;
	}

	/*
	 * Determines if all of the application programs are a specific set of programs.
	 */
	private boolean programsAreExactlyThisSet(Set<String> applicationPrograms, Set<String> setPrograms) {
		if (applicationPrograms.size() != setPrograms.size()) {
			return false;
		}
		for (String program : applicationPrograms) {
			if (!setPrograms.contains(program)) return false;
		}
		return true;
	}

	/*
	 * Tribal TANF isn't a program but if its in the application we include it
	 * because the routing logic can be based on the programs including Tribal TANF. 
	 */
	private Set<String> applicantPrograms(ApplicationData applicationData) {
		Set<String> programs = getProgramsFoundInTheApplication(applicationData);
	    if (isApplyingForTribalTanf(applicationData.getPagesData())) {
	    	programs.add("TANF"); // TANF is added as a pseudo program 
	    }
	    return programs;
	}

	/*
	 * We need this helper method because although method getApplicantAndHouseholdMemberPrograms
	 * returns a Set<String>, the String in a given element can represent multiple programs.
	 * For example: "SNAP, CASH"
	 * This method will parse out the individual programs. 
	 */
	private Set<String> getProgramsFoundInTheApplication(ApplicationData applicationData) {
		HashSet<String> programs = new HashSet<String>();
		// We use "split" because getApplicantAndHouseholdMemberPrograms is a bit weird in
		// that it returns a Set<String> but not each program as a separate string in the Set.
		for (String element : applicationData.getApplicantAndHouseholdMemberPrograms()) {
			for (String p : element.split(",")) {
				programs.add(p.trim());
			}
		}
		return programs;
	}

	/*
	 * Return just the county of residence as the routing destination.
	 */
	private List<RoutingDestination> countyOfResidence(County county, ApplicationData applicationData) {
		// TODO: Investigate whether or not we should send documents to the default
		// (i.e., County.Other). We don't for LATER_DOCS.
		// if (county.equals(County.Other)) {
		//     log.info("Note: Application " + applicationData.getId()
		//         + " has no routing destinations, the county = County.Other.");
		//     return List.of();
		// }
		return List.of(countyRoutingDestinations.get(county));
	}

	/*
	 * Helper method to determine if the applicant lives within the boundaries of
	 * Red Lake Nation Note: This will return false when the nationsBoundary page or
	 * nationOfResidence pages were not displayed.
	 */
	private boolean livesInBoudariesOfRedLakeNation(PagesData pagesData) {
		String nationOfResidence = getFirstValue(pagesData, NATION_OF_RESIDENCE);
		if (nationOfResidence != null && RedLakeNation.toString().equals(nationOfResidence)) {
			return true;
		}
		return false;
	}
}

