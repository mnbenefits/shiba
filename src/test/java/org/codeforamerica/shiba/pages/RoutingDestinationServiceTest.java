package org.codeforamerica.shiba.pages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codeforamerica.shiba.County.Olmsted;
import static org.codeforamerica.shiba.application.FlowType.FULL;
import static org.codeforamerica.shiba.output.Document.CAF;
import static org.codeforamerica.shiba.output.Document.XML;
import static org.codeforamerica.shiba.output.Recipient.CASEWORKER;
import static org.codeforamerica.shiba.testutilities.TestUtils.resetApplicationData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.MonitoringService;
import org.codeforamerica.shiba.ServicingAgencyMap;
import org.codeforamerica.shiba.TribalNation;
import org.codeforamerica.shiba.TribalNationRoutingDestination;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.application.ApplicationStatusRepository;
import org.codeforamerica.shiba.documents.DocumentRepository;
import org.codeforamerica.shiba.mnit.CountyRoutingDestination;
import org.codeforamerica.shiba.mnit.FilenetWebServiceClient;
import org.codeforamerica.shiba.mnit.RoutingDestination;
import org.codeforamerica.shiba.output.ApplicationFile;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.MnitDocumentConsumer;
import org.codeforamerica.shiba.output.caf.FilenameGenerator;
import org.codeforamerica.shiba.output.pdf.PdfGenerator;
import org.codeforamerica.shiba.output.xml.XmlGenerator;
import org.codeforamerica.shiba.pages.config.FeatureFlag;
import org.codeforamerica.shiba.pages.config.FeatureFlagConfiguration;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.emails.EmailClient;
import org.codeforamerica.shiba.testutilities.NonSessionScopedApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ActiveProfiles("test")
@SpringBootTest
@ContextConfiguration(classes = {NonSessionScopedApplicationData.class})
@Tag("db")
public class RoutingDestinationServiceTest {
	
	  public static final byte[] FILE_BYTES = new byte[10];

	  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	  @Autowired
	  private ServicingAgencyMap<CountyRoutingDestination> countyMap;
	  @Autowired
	  private ServicingAgencyMap<TribalNationRoutingDestination> tribalNationsMap;

	  @MockitoBean
	  private FeatureFlagConfiguration featureFlagConfig;
	  @MockitoBean
	  private FilenetWebServiceClient mnitClient;
	  @MockitoBean
	  private EmailClient emailClient;
	  @MockitoBean
	  private XmlGenerator xmlGenerator;
	  @MockitoBean
	  private MonitoringService monitoringService;
	  @MockitoBean
	  private DocumentRepository documentRepository;
	  @MockitoBean
	  private ClientRegistrationRepository repository;
	  @MockitoBean
	  private FilenameGenerator fileNameGenerator;
	  @MockitoBean
	  private ApplicationRepository applicationRepository;
	  @MockitoBean
	  private ApplicationStatusRepository applicationStatusRepository;
	  @MockitoBean
	  private MessageSource messageSource;
	  @MockitoSpyBean
	  private PdfGenerator pdfGenerator;

	  @Autowired
	  private ApplicationData applicationData;
	  @Autowired
	  private MnitDocumentConsumer documentConsumer;

	  private Application application;
	  
	  @Autowired
	  private RoutingDecisionService routingDecisionService;

	  @BeforeEach
	  void setUp() {
	    applicationData = new TestApplicationDataBuilder(applicationData)
	        .withPersonalInfo()
	        .withContactInfo()
	        .withApplicantPrograms(List.of("SNAP"))
	        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
	        .build();

	    ZonedDateTime completedAt = ZonedDateTime.of(
	        LocalDateTime.of(2021, 6, 10, 1, 28),
	        ZoneOffset.UTC);

	    application = Application.builder()
	        .id("someId")
	        .completedAt(completedAt)
	        .applicationData(applicationData)
	        .county(Olmsted)
	        .timeToComplete(null)
	        .flow(FULL)
	        .build();
	    when(messageSource.getMessage(any(), any(), any())).thenReturn("default success message");
	    when(fileNameGenerator.generatePdfFilename(any(), any())).thenReturn("some-file.pdf");
	    when(featureFlagConfig.get("filenet")).thenReturn(FeatureFlag.ON);

	    doReturn(application).when(applicationRepository).find(any());
	  }

	  @AfterEach
	  void afterEach() {
	    resetApplicationData(applicationData);
	  }
	  
		/**
		 * This test verifies that the routing destination is White Earth Nation when
		 * the applicant is a lineal descendant of a White Earth Nation member and
		 * resides in one of the counties serviced by WEN.
		 * 
		 * @param county
		 * @param program
		 * @throws Exception
		 */
		@ParameterizedTest
		@CsvSource(value = { "Becker, SNAP", "Becker, EA", "Becker, CASH", "Becker, CCAP", "Becker, GRH",
				"Mahnomen, SNAP", "Mahnomen, EA", "Mahnomen, CASH", "Mahnomen, CCAP", "Mahnomen, GRH",
				"Clearwater, SNAP", "Clearwater, EA", "Clearwater, CASH", "Clearwater, CCAP", "Clearwater, GRH" })
		public void routeToWhiteEarthNationForLinealDescendantWhenLivingInTheThreeCounties(String countyName,
				String program) {
		    ApplicationFile pdfApplicationFile = new ApplicationFile("my pdf".getBytes(), "someFile.pdf");
		    doReturn(pdfApplicationFile).when(pdfGenerator).generate(anyString(), any(), any(), any());
		    ApplicationFile xmlApplicationFile = new ApplicationFile("my xml".getBytes(), "someFile.xml");
		    when(xmlGenerator.generate(any(), any(), any(), any())).thenReturn(xmlApplicationFile);

		    application.setApplicationData(new TestApplicationDataBuilder()
		        .withApplicantPrograms(List.of(program))
		        .withPageData("homeAddress", "county", List.of(countyName))
		        .withPageData("identifyCounty", "county", countyName)
		        .withPageData("linealDescendantWhiteEarthNation", "linealDescendantWEN", List.of("true") )
		        .build());
		    application.setCounty(County.getForName(countyName));
		    documentConsumer.processCafAndCcap(application);

		    Document docType = program.equals("CCAP")? Document.CCAP : Document.CAF;
		    TribalNationRoutingDestination routingDestination = tribalNationsMap.get(TribalNation.WhiteEarthNation);
		    if (docType.equals(Document.CAF)) {
			    verify(pdfGenerator).generate(application.getId(), docType, CASEWORKER, routingDestination);
			    verify(xmlGenerator).generate(application.getId(), docType, CASEWORKER, routingDestination);
			    verify(mnitClient, times(2)).send(any(), any(), any(), any());
			    verify(mnitClient).send(application, pdfApplicationFile, tribalNationsMap.get(TribalNation.WhiteEarthNation), docType);
			    verify(mnitClient).send(application, xmlApplicationFile, tribalNationsMap.get(TribalNation.WhiteEarthNation), XML);
		    } else {
			    verify(pdfGenerator).generate(application.getId(), docType, CASEWORKER, routingDestination);
			    verify(mnitClient, times(2)).send(any(), any(), any(), any());
			    verify(mnitClient).send(application, pdfApplicationFile, tribalNationsMap.get(TribalNation.WhiteEarthNation), docType);
		    }

	  }
	  
	  @ParameterizedTest
	  @CsvSource({
	      "Becker,Becker",
	      "Mahnomen,Mahnomen",
	      "Clearwater,Clearwater"})
	  public void routeToCountyForNonLinealDescendantWhenLivingInTheThreeCounties(String countyName, County expectedDestination) { 
		    ApplicationFile pdfApplicationFile = new ApplicationFile("my pdf".getBytes(), "someFile.pdf");
		    doReturn(pdfApplicationFile).when(pdfGenerator).generate(anyString(), any(), any(), any());
		    ApplicationFile xmlApplicationFile = new ApplicationFile("my xml".getBytes(), "someFile.xml");
		    when(xmlGenerator.generate(any(), any(), any(), any())).thenReturn(xmlApplicationFile);

		    application.setApplicationData(new TestApplicationDataBuilder()
		        .withApplicantPrograms(List.of("EA"))
		        .withPageData("homeAddress", "county", List.of(countyName))
		        .withPageData("identifyCounty", "county", countyName)
		        .withPageData("linealDescendantWhiteEarthNation", "linealDescendantWEN", List.of("false") )
		        .build());
		    application.setCounty(County.getForName(countyName));

		    documentConsumer.processCafAndCcap(application);
		    CountyRoutingDestination routingDestination = countyMap.get(expectedDestination);
		    verify(pdfGenerator).generate(application.getId(), CAF, CASEWORKER, routingDestination);
		    verify(xmlGenerator).generate(application.getId(), CAF, CASEWORKER, routingDestination);
		    verify(mnitClient, times(2)).send(any(), any(), any(), any());
		    verify(mnitClient).send(application, pdfApplicationFile, countyMap.get(expectedDestination), CAF);
		    verify(mnitClient).send(application, xmlApplicationFile, countyMap.get(expectedDestination), XML);
		  
	  }


	  @ParameterizedTest
	  @CsvSource({
	      "YellowMedicine,SNAP","YellowMedicine,CASH","YellowMedicine,EA","YellowMedicine,GRH","YellowMedicine,CCAP",
	      "Aitkin,SNAP","Aitkin,CASH","Aitkin,EA","Aitkin,GRH","Aitkin,CCAP",
	      "LakeOfTheWoods,SNAP","LakeOfTheWoods,CASH","LakeOfTheWoods,EA","LakeOfTheWoods,GRH","LakeOfTheWoods,CCAP",
	      "StLouis,SNAP","StLouis,CASH","StLouis,EA","StLouis,GRH","StLouis,CCAP",
	      "LacQuiParle,SNAP","LacQuiParle,CASH","LacQuiParle,EA","LacQuiParle,GRH","LacQuiParle,CCAP",})
	  public void routeToCountyForLinealDescendantWhenNotLivingInTheThreeCounties(String countyName, String program) { 
		    ApplicationFile pdfApplicationFile = new ApplicationFile("my pdf".getBytes(), "someFile.pdf");
		    doReturn(pdfApplicationFile).when(pdfGenerator).generate(anyString(), any(), any(), any());
		    ApplicationFile xmlApplicationFile = new ApplicationFile("my xml".getBytes(), "someFile.xml");
		    when(xmlGenerator.generate(any(), any(), any(), any())).thenReturn(xmlApplicationFile);

		    application.setApplicationData(new TestApplicationDataBuilder()
		        .withApplicantPrograms(List.of(program))
		        .withPageData("homeAddress", "county", List.of(countyName))
		        .withPageData("identifyCounty", "county", countyName)
		        .withPageData("linealDescendantWhiteEarthNation", "linealDescendantWEN", List.of("true") )
		        .build());
		    County county = County.getForName(countyName);
		    application.setCounty(county);

		    documentConsumer.processCafAndCcap(application);

		    Document docType = program.equals("CCAP")? Document.CCAP : Document.CAF;
		    CountyRoutingDestination routingDestination = countyMap.get(county);
		    if (docType.equals(Document.CAF)) {
			    verify(pdfGenerator).generate(application.getId(), docType, CASEWORKER, routingDestination);
			    verify(xmlGenerator).generate(application.getId(), docType, CASEWORKER, routingDestination);
			    verify(mnitClient, times(2)).send(any(), any(), any(), any());
			    verify(mnitClient).send(application, pdfApplicationFile, countyMap.get(county), docType);
			    verify(mnitClient).send(application, xmlApplicationFile, countyMap.get(county), XML);
		    } else {
			    verify(pdfGenerator).generate(application.getId(), docType, CASEWORKER, routingDestination);
			    verify(mnitClient, times(2)).send(any(), any(), any(), any());
			    verify(mnitClient).send(application, pdfApplicationFile, countyMap.get(county), docType);
		    }
	  }
	  
	  @ParameterizedTest
	  @CsvSource(value = {"Becker", "Mahnomen", "Clearwater"})
	  void routeToWhiteEarthForLinealDescentantsFromTheThreeCounties(String county) throws Exception {
		  // This directly tests the RoutingDecisionService 
		  ApplicationData   applicationData = new TestApplicationDataBuilder()
			        .withPersonalInfo()
			        .withContactInfo()
			        .withApplicantPrograms(List.of("SNAP"))
			        .withPageData("identifyCounty", "county", county)
			        .withPageData("homeAddress", "county", List.of(county))
			        .withPageData("linealDescendantWhiteEarthNation", "linealDescendantWEN", List.of("true") )
			        .withPageData("homeAddress", "enrichedCounty", List.of(county))
			        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
			        .build();

			    ZonedDateTime completedAt = ZonedDateTime.of(
			        LocalDateTime.of(2021, 6, 10, 1, 28),
			        ZoneOffset.UTC);

			    application = Application.builder()
			        .id("someId")
			        .completedAt(completedAt)
			        .applicationData(applicationData)
			        .county(County.getForName(county))
			        .timeToComplete(null)
			        .flow(FULL)
			        .build();

	    var routingDestinations = routingDecisionService.getRoutingDestinations(applicationData, CAF);
	    RoutingDestination routingDestination = routingDestinations.get(0);
	    assertThat(routingDestination.getDhsProviderId()).isEqualTo("A086642300");
	    assertThat(routingDestination.getEmail()).isEqualTo("mnbenefits@state.mn.us");
	    assertThat(routingDestination.getPhoneNumber()).isEqualTo("218-935-2359");
	  }
	  
	  @Test
	  void routeToCountyForWhiteEarthLinealDescentantsForAnyOtherCounties() throws Exception {
		  // Residents of any other counties than the three should never see the WEN lineal descendant page, but test this anyway
		  ApplicationData   applicationData = new TestApplicationDataBuilder()
			        .withPersonalInfo()
			        .withContactInfo()
			        .withApplicantPrograms(List.of("SNAP"))
			        .withPageData("identifyCounty", "county", "Aitkin")
			        .withPageData("homeAddress", "county", List.of("Aitkin"))
			        .withPageData("linealDescendantWhiteEarthNation", "linealDescendantWEN", List.of("true") )
			        .withPageData("homeAddress", "enrichedCounty", List.of("Aitkin"))
			        .withPageData("verifyHomeAddress", "useEnrichedAddress", List.of("true"))
			        .build();

			    ZonedDateTime completedAt = ZonedDateTime.of(
			        LocalDateTime.of(2021, 6, 10, 1, 28),
			        ZoneOffset.UTC);

			    application = Application.builder()
			        .id("someId")
			        .completedAt(completedAt)
			        .applicationData(applicationData)
			        .county(County.getForName("Aitkin"))
			        .timeToComplete(null)
			        .flow(FULL)
			        .build();

	    var routingDestinations = routingDecisionService.getRoutingDestinations(applicationData, CAF);
	    RoutingDestination routingDestination = routingDestinations.get(0);
	    assertThat(routingDestination.getDhsProviderId()).isEqualTo("A000001900");//Aitkin
	  }

		/**
		 * This test verifies that the routing destination for applicants who live
		 * within the boundaries of Red Lake Nation. In this case it does not matter
		 * what tribe they are a member of so this test will use White Earth Nation for that
		 * purpose. The routing destination is dependent upon the program(s) selected so
		 * the test cycles through each program individually and as a set of programs.
		 * 
		 * @param county
		 * @param program              - use ";" to separate a list of programs
		 * @param expectedDestinations - use ";" to separate a list of expected destinations
		 * @throws Exception
		 */
		@ParameterizedTest
		@CsvSource(value = { "Beltrami, SNAP, Red Lake Nation", 
				"Beltrami, EA, Red Lake Nation",
				"Beltrami, CCAP, Red Lake Nation", 
				"Beltrami, SNAP;TANF, Red Lake Nation", 
				"Beltrami, CASH, Beltrami",
				"Beltrami, GRH, Beltrami", 
				"Beltrami, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				"Clearwater, SNAP, White Earth Nation", // This test uses WEN tribal membership so route to WEN for Clearwater
				"Clearwater, EA, White Earth Nation",
				"Clearwater, CCAP, White Earth Nation", 
				"Clearwater, SNAP;TANF, White Earth Nation",
				"Clearwater, CASH, White Earth Nation", 
				"Clearwater, GRH, White Earth Nation",
				"Clearwater, SNAP;EA;CASH, White Earth Nation",
				"Norman, SNAP, Norman", 
				"Norman, EA, Norman",
				"Norman, CCAP, Norman", 
				"Norman, SNAP;TANF, Norman", 
				"Norman, CASH, Norman", 
				"Norman, GRH, Norman",
				"Norman, SNAP;EA;CASH, Norman" })
		public void routeWhenLivingInRedLakeNationBoundaries(String countyName, String programs,
				String expectedDestinations) {
			List<String> programsList = new ArrayList<String>(Arrays.asList(programs.split(";")));
			String[] expectedDestinationsArray = expectedDestinations.split(";");

			TestApplicationDataBuilder applicationDataBuilder = new TestApplicationDataBuilder();
			// "TANF" isn't really a program so we need to include pageData for the
			// applyForTribalTANF page and then remove it from the list.
			if (programsList.contains("TANF")) {
				applicationDataBuilder.withPageData("applyForTribalTANF", "applyForTribalTANF", List.of("true"));
				programsList.remove("TANF");
			}
			// build the rest of the application_data
			ApplicationData applicationData = applicationDataBuilder.withApplicantPrograms(programsList)
					.withPageData("identifyCounty", "county", countyName)
					.withPageData("tribalNationMember", "isTribalNationMember", List.of("true"))
					.withPageData("selectTheTribe", "selectedTribe", "White Earth Nation")
					.withPageData("nationsBoundary", "livingInNationBoundary", List.of("true"))
					.withPageData("nationOfResidence", "selectedNationOfResidence", "Red Lake Nation").build();
			application.setApplicationData(applicationData);
			application.setCounty(County.getForName(countyName));

			// consider 3 possible types of documents, CCAP, CAF and XML and merge
			// destinations into one list
			List<RoutingDestination> actualRoutingDestinations = new ArrayList<RoutingDestination>();
			if (programsList.contains("CCAP")) {
				actualRoutingDestinations
						.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CCAP));
			}
			List<String> cafPrograms = List.of("SNAP", "EA", "CASH", "GRH");
			boolean haveCafProgram = programsList.stream().anyMatch(cafPrograms::contains);
			if (haveCafProgram) {
				actualRoutingDestinations
						.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CAF));
				actualRoutingDestinations
						.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.XML));
			}
			List<String> actualRoutingDestinationNames = actualRoutingDestinations.stream()
					.map(RoutingDestination::getName).collect(Collectors.toList());
			actualRoutingDestinationNames = new ArrayList<>(new LinkedHashSet<>(actualRoutingDestinationNames));

			assertThat(actualRoutingDestinationNames).containsOnly(expectedDestinationsArray);
		}

		/**
		 * This test verifies that the routing destination(s) for applicants who live
		 * in Beltrami County but do not live within the boundaries of Red Lake Nation.
		 * When they are a member of any Tribal Nation other than Leech Lake, the documents
		 * are routed to Red Lake Nation for programs SNAP, EA, Child Care, Tribal TANF.
		 * Otherwise the documents are routed to Beltrami County.
		 * The routing destination is dependent upon:
		 *  - The county of residence (fixed, Beltrami)
		 *  - The program(s) selected
		 *  - Tribal Nation membership
		 *  - Does not live within the boundaries of Red Lake Nation (fixed)
		 * The test will cycle through each program individually and as a set of programs
		 * and the Tribal Nations
		 * 
		 * @param tribalNation
		 * @param program              - use ";" to separate a list of programs
		 * @param expectedDestinations - use ";" to separate a list of expected destinations
		 * @throws Exception
		 */
		@ParameterizedTest
		@CsvSource(value = { "Bois Forte, SNAP, Red Lake Nation",
				"Bois Forte, EA, Red Lake Nation",
				"Bois Forte, CCAP, Red Lake Nation", 
				"Bois Forte, SNAP;TANF, Red Lake Nation", 
				"Bois Forte, CASH, Beltrami",
				"Bois Forte, GRH, Beltrami", 
				"Bois Forte, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				"Fond Du Lac, SNAP, Red Lake Nation", 
				"Fond Du Lac, EA, Red Lake Nation",
				"Fond Du Lac, CCAP, Red Lake Nation", 
				"Fond Du Lac, SNAP;TANF, Red Lake Nation", 
				"Fond Du Lac, CASH, Beltrami", 
				"Fond Du Lac, GRH, Beltrami",
				"Fond Du Lac, SNAP;EA;CASH, Red Lake Nation;Beltrami",
				"Leech Lake, SNAP, Beltrami",
				"Leech Lake, EA, Beltrami",
				"Leech Lake, CCAP, Beltrami", 
				"Leech Lake, SNAP;TANF, Beltrami", 
				"Leech Lake, CASH, Beltrami",
				"Leech Lake, GRH, Beltrami", 
				"Leech Lake, SNAP;EA;CASH, Beltrami",
				"Lower Sioux, SNAP, Red Lake Nation",
				"Lower Sioux, EA, Red Lake Nation",
				"Lower Sioux, CCAP, Red Lake Nation", 
				"Lower Sioux, SNAP;TANF, Red Lake Nation", 
				"Lower Sioux, CASH, Beltrami",
				"Lower Sioux, GRH, Beltrami", 
				"Lower Sioux, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				"Mille Lacs Band of Ojibwe, SNAP, Red Lake Nation",
				"Mille Lacs Band of Ojibwe, EA, Red Lake Nation",
				"Mille Lacs Band of Ojibwe, CCAP, Red Lake Nation", 
				"Mille Lacs Band of Ojibwe, SNAP;TANF, Red Lake Nation", 
				"Mille Lacs Band of Ojibwe, CASH, Beltrami",
				"Mille Lacs Band of Ojibwe, GRH, Beltrami", 
				"Mille Lacs Band of Ojibwe, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				"Prairie Island, SNAP, Red Lake Nation",
				"Prairie Island, EA, Red Lake Nation",
				"Prairie Island, CCAP, Red Lake Nation", 
				"Prairie Island, SNAP;TANF, Red Lake Nation", 
				"Prairie Island, CASH, Beltrami",
				"Prairie Island, GRH, Beltrami", 
				"Prairie Island, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				"Red Lake Nation, SNAP, Red Lake Nation",
				"Red Lake Nation, EA, Red Lake Nation",
				"Red Lake Nation, CCAP, Red Lake Nation", 
				"Red Lake Nation, SNAP;TANF, Red Lake Nation", 
				"Red Lake Nation, CASH, Beltrami",
				"Red Lake Nation, GRH, Beltrami", 
				"Red Lake Nation, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				"Shakopee Mdewakanton, SNAP, Red Lake Nation",
				"Shakopee Mdewakanton, EA, Red Lake Nation",
				"Shakopee Mdewakanton, CCAP, Red Lake Nation", 
				"Shakopee Mdewakanton, SNAP;TANF, Red Lake Nation", 
				"Shakopee Mdewakanton, CASH, Beltrami",
				"Shakopee Mdewakanton, GRH, Beltrami", 
				"Shakopee Mdewakanton, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				"Upper Sioux, SNAP, Red Lake Nation",
				"Upper Sioux, EA, Red Lake Nation",
				"Upper Sioux, CCAP, Red Lake Nation", 
				"Upper Sioux, SNAP;TANF, Red Lake Nation", 
				"Upper Sioux, CASH, Beltrami",
				"Upper Sioux, GRH, Beltrami", 
				"Upper Sioux, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				"White Earth Nation, SNAP, Red Lake Nation",
				"White Earth Nation, EA, Red Lake Nation",
				"White Earth Nation, CCAP, Red Lake Nation", 
				"White Earth Nation, SNAP;TANF, Red Lake Nation", 
				"White Earth Nation, CASH, Beltrami",
				"White Earth Nation, GRH, Beltrami", 
				"White Earth Nation, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				"Federally recognized tribe outside of MN, SNAP, Red Lake Nation",
				"Federally recognized tribe outside of MN, EA, Red Lake Nation",
				"Federally recognized tribe outside of MN, CCAP, Red Lake Nation", 
				"Federally recognized tribe outside of MN, SNAP;TANF, Red Lake Nation", 
				"Federally recognized tribe outside of MN, CASH, Beltrami",
				"Federally recognized tribe outside of MN, GRH, Beltrami", 
				"Federally recognized tribe outside of MN, SNAP;EA;CASH, Red Lake Nation;Beltrami",  // with this combo of program we expect multiple destinations
				})
		public void routeBeltramiResidentsWhenNotLivingInRedLakeNationBoundaries(String tribalNation, String programs,
				String expectedDestinations) {
			List<String> programsList = new ArrayList<String>(Arrays.asList(programs.split(";")));
			String[] expectedDestinationsArray = expectedDestinations.split(";");

			TestApplicationDataBuilder applicationDataBuilder = new TestApplicationDataBuilder();
			// "TANF" isn't really a program so we need to include pageData for the
			// applyForTribalTANF page and then remove it from the list.
			if (programsList.contains("TANF")) {
				applicationDataBuilder.withPageData("applyForTribalTANF", "applyForTribalTANF", List.of("true"));
				programsList.remove("TANF");
			}
			// build the rest of the application_data
			ApplicationData applicationData = applicationDataBuilder.withApplicantPrograms(programsList)
					.withPageData("identifyCounty", "county", "Beltrami")
					.withPageData("tribalNationMember", "isTribalNationMember", List.of("true"))
					.withPageData("selectTheTribe", "selectedTribe", tribalNation)
					.withPageData("nationsBoundary", "livingInNationBoundary", List.of("false"))
					.build();
			application.setApplicationData(applicationData);
			application.setCounty(County.Beltrami);

			// consider 3 possible types of documents, CCAP, CAF and XML and merge
			// destinations into one list
			List<RoutingDestination> actualRoutingDestinations = new ArrayList<RoutingDestination>();
			if (programsList.contains("CCAP")) {
				actualRoutingDestinations
						.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CCAP));
			}
			List<String> cafPrograms = List.of("SNAP", "EA", "CASH", "GRH");
			boolean haveCafProgram = programsList.stream().anyMatch(cafPrograms::contains);
			if (haveCafProgram) {
				actualRoutingDestinations
						.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CAF));
				actualRoutingDestinations
						.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.XML));
			}
			List<String> actualRoutingDestinationNames = actualRoutingDestinations.stream()
					.map(RoutingDestination::getName).collect(Collectors.toList());
			actualRoutingDestinationNames = new ArrayList<>(new LinkedHashSet<>(actualRoutingDestinationNames));

			assertThat(actualRoutingDestinationNames).containsOnly(expectedDestinationsArray);
		}

		/**
		 * This test verifies that the routing destination(s) for applicants who live
		 * in Clearwater County but do not live within the boundaries of Red Lake Nation.
		 * When they are a member of any Tribal Nation, other than White Earth Nation, the documents
		 * are routed to Clearwater County for programs SNAP, Cash, Housing Supports, EA, Child Care.
		 * Note: We do not include Tribal TANF in this test, we do not include WEN in this test.
		 * The routing destination is dependent upon:
		 *  - The county of residence (fixed, Clearwater)
		 *  - The program(s) selected
		 *  - Tribal Nation membership (any except White Earth Nation)
		 *  - Does not live within the boundaries of Red Lake Nation (fixed)
		 * The test will cycle through each program individually and as a set of programs
		 * and the Tribal Nations
		 * 
		 * The expected routing destination is fixed to "Clearwater"
		 * @param tribalNation
		 * @param program              - use ";" to separate a list of programs
		 * @throws Exception
		 */
		@ParameterizedTest
		@CsvSource(value = { "Bois Forte, SNAP",
				"Bois Forte, EA",
				"Bois Forte, CCAP", 
				"Bois Forte, CASH",
				"Bois Forte, GRH", 
				"Bois Forte, SNAP;EA;CASH",
				"Fond Du Lac, SNAP", 
				"Fond Du Lac, EA",
				"Fond Du Lac, CCAP", 
				"Fond Du Lac, CASH", 
				"Fond Du Lac, GRH",
				"Fond Du Lac, SNAP;EA;CASH",
				"Leech Lake, SNAP",
				"Leech Lake, EA",
				"Leech Lake, CCAP", 
				"Leech Lake, CASH",
				"Leech Lake, GRH", 
				"Leech Lake, SNAP;EA;CASH",
				"Lower Sioux, SNAP",
				"Lower Sioux, EA",
				"Lower Sioux, CCAP", 
				"Lower Sioux, CASH",
				"Lower Sioux, GRH", 
				"Lower Sioux, SNAP;EA;CASH",
				"Mille Lacs Band of Ojibwe, SNAP",
				"Mille Lacs Band of Ojibwe, EA",
				"Mille Lacs Band of Ojibwe, CCAP", 
				"Mille Lacs Band of Ojibwe, CASH",
				"Mille Lacs Band of Ojibwe, GRH", 
				"Mille Lacs Band of Ojibwe, SNAP;EA;CASH",
				"Prairie Island, SNAP",
				"Prairie Island, EA",
				"Prairie Island, CCAP", 
				"Prairie Island, CASH",
				"Prairie Island, GRH", 
				"Prairie Island, SNAP;EA;CASH",
				"Red Lake Nation, SNAP",
				"Red Lake Nation, EA",
				"Red Lake Nation, CCAP", 
				"Red Lake Nation, CASH",
				"Red Lake Nation, GRH", 
				"Red Lake Nation, SNAP;EA;CASH",
				"Shakopee Mdewakanton, SNAP",
				"Shakopee Mdewakanton, EA",
				"Shakopee Mdewakanton, CCAP", 
				"Shakopee Mdewakanton, CASH",
				"Shakopee Mdewakanton, GRH", 
				"Shakopee Mdewakanton, SNAP;EA;CASH",
				"Upper Sioux, SNAP, Red Lakpper Sioux, EA",
				"Upper Sioux, CCAP", 
				"Upper Sioux, CASH",
				"Upper Sioux, GRH", 
				"Upper Sioux, SNAP;EA;CASH",
				"Federally recognized tribe outside of MN, SNAP",
				"Federally recognized tribe outside of MN, EA",
				"Federally recognized tribe outside of MN, CCAP", 
				"Federally recognized tribe outside of MN, CASH",
				"Federally recognized tribe outside of MN, GRH", 
				"Federally recognized tribe outside of MN, SNAP;EA;CASH",
				})
		public void routeClearwaterResidentsWhenNotLivingInRedLakeNationBoundaries(String tribalNation, String programs) {
			List<String> programsList = new ArrayList<String>(Arrays.asList(programs.split(";")));
			String[] expectedDestinationsArray = {"Clearwater"};

			// Build the application_data
			TestApplicationDataBuilder applicationDataBuilder = new TestApplicationDataBuilder();
			ApplicationData applicationData = applicationDataBuilder.withApplicantPrograms(programsList)
					.withPageData("identifyCounty", "county", "Clearwater")
					.withPageData("tribalNationMember", "isTribalNationMember", List.of("true"))
					.withPageData("selectTheTribe", "selectedTribe", tribalNation)
					.withPageData("nationsBoundary", "livingInNationBoundary", List.of("false"))
					.build();
			application.setApplicationData(applicationData);
			application.setCounty(County.Clearwater);

			// consider 3 possible types of documents, CCAP, CAF and XML and merge
			// destinations into one list
			List<RoutingDestination> actualRoutingDestinations = new ArrayList<RoutingDestination>();
			if (programsList.contains("CCAP")) {
				actualRoutingDestinations
						.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CCAP));
			}
			List<String> cafPrograms = List.of("SNAP", "EA", "CASH", "GRH");
			boolean haveCafProgram = programsList.stream().anyMatch(cafPrograms::contains);
			if (haveCafProgram) {
				actualRoutingDestinations
						.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CAF));
				actualRoutingDestinations
						.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.XML));
			}
			List<String> actualRoutingDestinationNames = actualRoutingDestinations.stream()
					.map(RoutingDestination::getName).collect(Collectors.toList());
			actualRoutingDestinationNames = new ArrayList<>(new LinkedHashSet<>(actualRoutingDestinationNames));

			assertThat(actualRoutingDestinationNames).containsOnly(expectedDestinationsArray);
		}


		/**
		 * This test verifies the routing destination for applicants who live in one of the MLBO
		 * rural counties: Aitkin, Benton, Crow Wing, Mille Lacs, Morrison, Kanabec, Chisago, Pine
		 * When they are a member of Mille Lacs Band of Ojibwe, the documents are routed
		 * to MLBO for programs Tribal TANF or (SNAP + Tribal TANF).
		 * Otherwise the documents are routed to the county of residence.
		 * The routing destination is dependent upon:
		 *  - The county of residence (in this test it is fixed to the MLBO rural counties)
		 *  - Tribal Nation membership
		 *  - The program(s) selected
		 * Note: This test does not consider living in tribal boundaries so the living in
		 * tribal boundaries question is fixed to "No".
		 * 
		 * @param isMlboMember         - "true" or "false"
		 * @param program              - use ";" to separate a list of programs
		 * @param expectedDestinations - use ";" to separate a list of expected destinations
		 * @throws Exception
		 */
		@ParameterizedTest
		@CsvSource(value = { 
				"true, SNAP, county of residence",
				"true, EA, county of residence",
				"true, CCAP, county of residence", 
				"true, SNAP;TANF, Mille Lacs Band of Ojibwe", 
				"true, CASH, county of residence",
				"true, GRH, county of residence", 
				"true, EA;TANF, Mille Lacs Band of Ojibwe;county of residence",  // expect multiple destinations
				"false, SNAP, county of residence", 
				"false, EA, county of residence",
				"false, CCAP, county of residence", 
				"false, SNAP;TANF, county of residence", 
				"false, CASH, county of residence", 
				"false, GRH, county of residence",
				"false, EA;TANF, county of residence"
				})
		public void routeDocumentsForResidentsOfMilleLacsBandOfOjibweRuralCounties(String isMlboMember, String programs,
				String expectedDestinations) {
			String[] ruralCounties = {"Aitkin", "Benton", "Crow Wing", "Mille Lacs", "Morrison", 
					"Kanabec", "Chisago", "Pine"};
			List<String> ruralCountiesList = new ArrayList<String>(Arrays.asList(ruralCounties));
			
            String[] mlbo = {"Mille Lacs Band of Ojibwe"};
			String[] otherTribes = {"Bois Forte", "Fond Du Lac", "Leech Lake", "Lower Sioux", "Prairie Island", 
					"Red Lake Nation", "Shakopee Mdewakanton", "Upper Sioux", "White Earth Nation", 
					"Federally recognized tribe outside of MN"};
			
            List<String> tribesList;
			if (Boolean.valueOf(isMlboMember).booleanValue()) {
            	tribesList = new ArrayList<String>(Arrays.asList(mlbo));
            } else {
            	tribesList = new ArrayList<String>(Arrays.asList(otherTribes));
            }
			
			String[] expectedDestinationsArray = expectedDestinations.split(";");

			// Outer loop is for tribe(s), inner loop for MLBO rural counties
			for (String tribe : tribesList) {
				for (String county : ruralCountiesList) {
					TestApplicationDataBuilder applicationDataBuilder = new TestApplicationDataBuilder();
					// "TANF" isn't really a program so we need to include pageData for the
					// applyForTribalTANF page and then remove it from the list.
					List<String> programsList = new ArrayList<String>(Arrays.asList(programs.split(";")));
					if (programsList.contains("TANF")) {
						applicationDataBuilder.withPageData("applyForTribalTANF", "applyForTribalTANF", List.of("true"));
						programsList.remove("TANF");
					}
					// build the rest of the application_data
					ApplicationData applicationData = applicationDataBuilder.withApplicantPrograms(programsList)
							.withPageData("identifyCounty", "county", county)
							.withPageData("tribalNationMember", "isTribalNationMember", List.of("true"))
							.withPageData("selectTheTribe", "selectedTribe", tribe)
							.withPageData("nationsBoundary", "livingInNationBoundary", List.of("false"))
							.build();
					application.setApplicationData(applicationData);
					application.setCounty(County.getForName(county));

					// consider 3 possible types of documents, CCAP, CAF and XML, then merge the
					// destinations into one list
					List<RoutingDestination> actualRoutingDestinations = new ArrayList<RoutingDestination>();
					if (programsList.contains("CCAP")) {
						actualRoutingDestinations
								.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CCAP));
					}
					List<String> cafPrograms = List.of("SNAP", "EA", "CASH", "GRH");
					boolean haveCafProgram = programsList.stream().anyMatch(cafPrograms::contains);
					if (haveCafProgram) {
						actualRoutingDestinations
								.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CAF));
						actualRoutingDestinations
								.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.XML));
					}
					List<String> actualRoutingDestinationNames = actualRoutingDestinations.stream()
							.map(RoutingDestination::getName).collect(Collectors.toList());
					actualRoutingDestinationNames = new ArrayList<>(new LinkedHashSet<>(actualRoutingDestinationNames));

					// Replace "county of residence" with the actual county name
					List<String> expectedDestinationsList = new ArrayList<String>(Arrays.asList(expectedDestinationsArray));
					if (expectedDestinationsList.contains("county of residence")) {
						expectedDestinationsList.remove("county of residence");
						expectedDestinationsList.add(county);
					}
					String [] expectedDestinationsThisLoop = expectedDestinationsList.toArray(new String[expectedDestinationsList.size()]);
					assertThat(actualRoutingDestinationNames).containsOnly(expectedDestinationsThisLoop);
					
				} // inner county loop
			} // outer tribe loop
		} // test

		/**
		 * This test verifies the routing destination for applicants who live in one of the MLBO
		 * urban counties: Anoka, Hennepin, Ramsey
		 * When they are a member of one of the MN Chippewa tribes the documents are routed
		 * to MLBO for programs Tribal TANF or (SNAP + Tribal TANF)
		 * Documents are routed to the county for programs EA, GRH, Child Care, Cash (other than Tribal TANF), SNAP
		 *  
		 * MN Chippewa tribes: Leech Lake, White Earth Nation, Bois Forte, Grand Portage, 
         *                     Fond du Lac, Mille Lacs Band of Ojibwe
		 * Otherwise when they are a member of one of the other tribes the documents are routed
		 * to the county of residence.
		 * Other tribes: Red Lake Nation, Lower Sioux, Prairie Island, Shakopee Mdewakanton, 
		 *			Upper Sioux, Federally recognized tribe outside of MN
		 * 
		 * The routing destination is dependent upon:
		 *  - The county of residence (in this test it is fixed to the MLBO urban counties)
		 *  - Tribal Nation membership
		 *  - The program(s) selected
		 * Note: This test does not consider living in tribal boundaries so the living in
		 * tribal boundaries question is fixed to "No".
		 * 
		 * @param isMlboMember         - "true" or "false"
		 * @param program              - use ";" to separate a list of programs
		 * @param expectedDestinations - use ";" to separate a list of expected destinations
		 * @throws Exception
		 */
		@ParameterizedTest
		@CsvSource(value = { 
				"true, SNAP, county of residence",
				"true, EA, county of residence",
				"true, CCAP, county of residence", 
				"true, CASH, county of residence",
				"true, GRH, county of residence", 
				"true, SNAP;TANF, Mille Lacs Band of Ojibwe",  // SNAP + TANF goes to MLBO only
				"false, SNAP, county of residence", 
				"false, EA, county of residence",
				"false, CCAP, county of residence", 
				"false, CASH, county of residence", 
				"false, GRH, county of residence",
				"false, SNAP;TANF, county of residence"
				})
		public void routeDocumentsForResidentsOfMilleLacsBandOfOjibweUrbanCounties(String isMnChippewaTribeMember, String programs,
				String expectedDestinations) {
			String[] urbanCounties = {"Anoka", "Hennepin", "Ramsey"};
			List<String> urbanCountiesList = new ArrayList<String>(Arrays.asList(urbanCounties));
			
            String[] mnChippewaTribes = {"Leech Lake", "White Earth Nation", "Bois Forte", "Grand Portage", 
            		"Fond Du Lac", "Mille Lacs Band of Ojibwe"};
			String[] otherTribes = {"Red Lake Nation", "Lower Sioux", "Prairie Island", "Shakopee Mdewakanton", 
					"Upper Sioux", "Federally recognized tribe outside of MN"};
			
            List<String> tribesList;
			if (Boolean.valueOf(isMnChippewaTribeMember).booleanValue()) {
            	tribesList = new ArrayList<String>(Arrays.asList(mnChippewaTribes));
            } else {
            	tribesList = new ArrayList<String>(Arrays.asList(otherTribes));
            }
			
			String[] expectedDestinationsArray = expectedDestinations.split(";");

			// Outer loop is for tribe(s), inner loop for MLBO urban counties
			for (String tribe : tribesList) {
				for (String county : urbanCountiesList) {
					TestApplicationDataBuilder applicationDataBuilder = new TestApplicationDataBuilder();
					// "TANF" isn't really a program so we need to include pageData for the
					// applyForTribalTANF page and then remove it from the list.
					List<String> programsList = new ArrayList<String>(Arrays.asList(programs.split(";")));
					if (programsList.contains("TANF")) {
						applicationDataBuilder.withPageData("applyForTribalTANF", "applyForTribalTANF", List.of("true"));
						programsList.remove("TANF");
					}
					// build the rest of the application_data
					ApplicationData applicationData = applicationDataBuilder.withApplicantPrograms(programsList)
							.withPageData("identifyCounty", "county", county)
							.withPageData("tribalNationMember", "isTribalNationMember", List.of("true"))
							.withPageData("selectTheTribe", "selectedTribe", tribe)
							.withPageData("nationsBoundary", "livingInNationBoundary", List.of("false"))
							.build();
					application.setApplicationData(applicationData);
					application.setCounty(County.getForName(county));

					// consider 3 possible types of documents, CCAP, CAF and XML, then merge the
					// destinations into one list
					List<RoutingDestination> actualRoutingDestinations = new ArrayList<RoutingDestination>();
					if (programsList.contains("CCAP")) {
						actualRoutingDestinations
								.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CCAP));
					}
					List<String> cafPrograms = List.of("SNAP", "EA", "CASH", "GRH");
					boolean haveCafProgram = programsList.stream().anyMatch(cafPrograms::contains);
					if (haveCafProgram) {
						actualRoutingDestinations
								.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.CAF));
						actualRoutingDestinations
								.addAll(routingDecisionService.getRoutingDestinations(applicationData, Document.XML));
					}
					List<String> actualRoutingDestinationNames = actualRoutingDestinations.stream()
							.map(RoutingDestination::getName).collect(Collectors.toList());
					actualRoutingDestinationNames = new ArrayList<>(new LinkedHashSet<>(actualRoutingDestinationNames));

					// Replace "county of residence" with the actual county name
					List<String> expectedDestinationsList = new ArrayList<String>(Arrays.asList(expectedDestinationsArray));
					if (expectedDestinationsList.contains("county of residence")) {
						expectedDestinationsList.remove("county of residence");
						expectedDestinationsList.add(county);
					}
					String [] expectedDestinationsThisLoop = expectedDestinationsList.toArray(new String[expectedDestinationsList.size()]);
					assertThat(actualRoutingDestinationNames).containsOnly(expectedDestinationsThisLoop);
					
				} // inner county loop
			} // outer tribe loop
		} // test

}
