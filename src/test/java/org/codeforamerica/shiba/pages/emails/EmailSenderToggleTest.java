package org.codeforamerica.shiba.pages.emails;

import static org.codeforamerica.shiba.County.Ramsey;
import static org.codeforamerica.shiba.Program.CASH;
import static org.codeforamerica.shiba.Program.SNAP;
import static org.codeforamerica.shiba.output.Recipient.CLIENT;
import static org.codeforamerica.shiba.application.FlowType.FULL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.codeforamerica.shiba.MonitoringService;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.application.parsers.EmailParser;
import org.codeforamerica.shiba.mnit.CountyRoutingDestination;
import org.codeforamerica.shiba.output.ApplicationFile;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.MnitDocumentConsumer;
import org.codeforamerica.shiba.output.caf.CcapExpeditedEligibility;
import org.codeforamerica.shiba.output.caf.CcapExpeditedEligibilityDecider;
import org.codeforamerica.shiba.output.caf.SnapExpeditedEligibility;
import org.codeforamerica.shiba.output.caf.SnapExpeditedEligibilityDecider;
import org.codeforamerica.shiba.output.pdf.PdfGenerator;
import org.codeforamerica.shiba.pages.RoutingDecisionService;
import org.codeforamerica.shiba.pages.WicRecommendationService;
import org.codeforamerica.shiba.pages.config.FeatureFlagConfiguration;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.pages.events.ApplicationSubmittedEvent;
import org.codeforamerica.shiba.pages.events.ApplicationSubmittedListener;
import org.codeforamerica.shiba.pages.events.PdfEncoder;
import org.codeforamerica.shiba.pages.rest.CommunicationClient;
import org.codeforamerica.shiba.testutilities.PagesDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;

import com.google.gson.JsonObject;

public class EmailSenderToggleTest {
	
	  MnitDocumentConsumer mnitDocumentConsumer = mock(MnitDocumentConsumer.class);
	  ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
	  EmailClient emailClient = mock(EmailClient.class);
	  SnapExpeditedEligibilityDecider snapExpeditedEligibilityDecider = mock(
	      SnapExpeditedEligibilityDecider.class);
	  CcapExpeditedEligibilityDecider ccapExpeditedEligibilityDecider = mock(
	      CcapExpeditedEligibilityDecider.class);
	  PdfGenerator pdfGenerator = mock(PdfGenerator.class);
	  FeatureFlagConfiguration featureFlagConfiguration = mock(FeatureFlagConfiguration.class);
	  MonitoringService monitoringService = mock(MonitoringService.class);
	  ApplicationSubmittedListener applicationSubmittedListener;
	  RoutingDecisionService routingDecisionService = mock(RoutingDecisionService.class);
	  CommunicationClient communicationClient;// = mock(CommunicationClient.class);
	  PdfEncoder pdfEncoder = mock(PdfEncoder.class);
	  WicRecommendationService wicRecommendationService = mock(WicRecommendationService.class);
	  private final String CLIENT_EMAIL = "some@example.com";
	  
	  @BeforeEach
	  void setUp() {
		  communicationClient = mock(CommunicationClient.class);
	  }
	
	@Test
	public void confirmEmailSentToMNbenefitsAndNotToCommHub() {
		System.out.println("test start");//TODO emj delete
	    LocaleContextHolder.setLocale(Locale.ENGLISH);
	     applicationSubmittedListener = new ApplicationSubmittedListener(
	        mnitDocumentConsumer,
	        applicationRepository,
	        emailClient,
	        snapExpeditedEligibilityDecider,
	        ccapExpeditedEligibilityDecider,
	        pdfGenerator,
	        monitoringService, 
	        routingDecisionService, 
	        communicationClient,
	        pdfEncoder,
	        wicRecommendationService,
	        "true",
	        "mnbenefits"// MNbenefits is configured to send the email
	        );
			String applicationId = "applicationId";
			ApplicationData applicationData = mock(ApplicationData.class);
			when(applicationData.getPagesData())
			.thenReturn(new PagesDataBuilder()
					.withPageData("contactInfo", "phoneOrEmail", "TEXT")
					.withPageData("identifyCounty", "county", "Ramsey")
					.withPageData("contactInfo", "phoneNumber", "(651)555-5555")
					.withPageData("personalInfo", "lastName", "LastName")
					.withPageData("personalInfo", "firstName", "FirstName").build());
			String appIdFromDb = "id";
			JsonObject jsonObject = new JsonObject();
			Application application = Application.builder().id(appIdFromDb).completedAt(ZonedDateTime.now())
					.county(Ramsey)
					.applicationData(applicationData).build();
			when(applicationRepository.find(applicationId)).thenReturn(application);
			ApplicationSubmittedEvent event = new ApplicationSubmittedEvent("someSessionId", applicationId, null,
					Locale.ENGLISH);
			when(communicationClient.isCommHubEmailEnabled()).thenReturn(false);
			when(routingDecisionService.getRoutingDestinationByName("Ramsey")).thenReturn(new CountyRoutingDestination(Ramsey, "DPI", "email", "(651)555-5555"));
			applicationSubmittedListener.notifyApplicationSubmission(event);  
			verify(communicationClient, never()).sendEmailDataToCommhub(jsonObject);

		      String email = "abc@123.com";
		      when(applicationRepository.find(applicationId)).thenReturn(application);
		      when(applicationData.isCAFApplication()).thenReturn(true);
		      when(applicationData.getPagesData()).thenReturn(new PagesData());
		      when(snapExpeditedEligibilityDecider.decide(applicationData))
		          .thenReturn(SnapExpeditedEligibility.ELIGIBLE);
		      when(ccapExpeditedEligibilityDecider.decide(applicationData))
		          .thenReturn(CcapExpeditedEligibility.UNDETERMINED);
		      ApplicationFile applicationFile = new ApplicationFile("someContent".getBytes(),
		          "someFileName");
		      when(pdfGenerator.generate(appIdFromDb, Document.CAF, CLIENT)).thenReturn(applicationFile);

		      try (MockedStatic<EmailParser> mockEmailParser = Mockito.mockStatic(EmailParser.class)) {
		        mockEmailParser.when(() -> EmailParser.parse(any())).thenReturn(Optional.of(email));
		        applicationSubmittedListener.sendConfirmationEmail(event);
		      }

		      verify(emailClient).sendConfirmationEmail(applicationData,
		          email,
		          appIdFromDb,
		          List.of(),
		          SnapExpeditedEligibility.ELIGIBLE,
		          CcapExpeditedEligibility.UNDETERMINED,
		          List.of(applicationFile),
		          Locale.ENGLISH);
		System.out.println("test complete");//TODO emj delete
	}
	
	@Test
	public void confirmEmailSentToCommHubAndNotMnbenefits() {
		System.out.println("test email to commhub start");
	    LocaleContextHolder.setLocale(Locale.ENGLISH);
	     applicationSubmittedListener = new ApplicationSubmittedListener(
	        mnitDocumentConsumer,
	        applicationRepository,
	        emailClient,
	        snapExpeditedEligibilityDecider,
	        ccapExpeditedEligibilityDecider,
	        pdfGenerator,
	        monitoringService, 
	        routingDecisionService, 
	        communicationClient,
	        pdfEncoder,
	        wicRecommendationService,
	        "true",
	        "commhub" //MNbenefits is configured to have Comm Hub send the email
	        );
	     
	     ZonedDateTime nowDate = ZonedDateTime.now();
	     String applicationId = "applicationId";
			String email = "some@email.com";
			ApplicationData applicationData = mock(ApplicationData.class);
		//	applicationData.setId("blah");can't set id directly in mocks
			when(applicationData.getId()).thenReturn(applicationId);
			when(applicationData.getPagesData())
			.thenReturn(new PagesDataBuilder()

					.withPageData("contactInfo", "phoneOrEmail", "EMAIL")
					.withPageData("identifyCounty", "county", "Ramsey")
					.withPageData("contactInfo", "phoneNumber", "5555555555")
					.withPageData("contactInfo", "email", "some@email.com")
		            .withPageData("contactInfo", Map.of(
		                    "email", List.of(CLIENT_EMAIL),
		                    "phoneOrEmail", List.of("EMAIL")))
					.withPageData("personalInfo", "lastName", "LastName")
					.withPageData("personalInfo", "firstName", "FirstName")
					.build());
			//String appIdFromDb = "id";
			//Timestamp completedAt = Timestamp.valueOf(nowDate);
			Timestamp completedAt = Timestamp.valueOf(nowDate.toLocalDateTime());
		    JsonObject jsonObject = new JsonObject();
		    jsonObject.addProperty("appId", applicationId);
		    jsonObject.addProperty("expedited", "[]");
		    jsonObject.addProperty("firstName", "FirstName");
		    jsonObject.addProperty("lastName", "LastName");
		    jsonObject.addProperty("phoneNumber", "5555555555");
		    jsonObject.addProperty("email", "some@email.com");
		    jsonObject.addProperty("county", "Ramsey");
		    jsonObject.addProperty("phoneOrEmail", "EMAIL");
		    jsonObject.addProperty("opt-status-sms", "false");
		    jsonObject.addProperty("wic-message", "false");
		    jsonObject.addProperty("opt-status-email", "true");
		    jsonObject.addProperty("applicationPDF", "null");
		    jsonObject.addProperty("completed-dt", completedAt.toString());
		    jsonObject.addProperty("spokenLangPref", "null");
		    
		    

		        applicationData.setPagesData(new PagesDataBuilder()
//		            .withPageData("contactInfo", Map.of(
//		                    "email", List.of(CLIENT_EMAIL),
//		                    "phoneOrEmail", List.of("EMAIL")))
		            .withPageData("contactInfo", "phoneNumber", "5555555555")
		            .withPageData("contactInfo", "email", "some@email.com")
		                //.withPageData("employmentStatus", "areYouWorking", "false")
		                //.withPageData("choosePrograms", "programs", List.of(SNAP, CASH))
		                .build());
			Application application = Application.builder().id(applicationId).completedAt(nowDate)
					.county(Ramsey)
					
					.applicationData(applicationData).build();
			
			when(applicationRepository.find(applicationId)).thenReturn(application);
		      ApplicationFile applicationFile = new ApplicationFile("someContent".getBytes(),
		              "someFileName");
//		          when(pdfGenerator.generate(applicationId, Document.CAF, CLIENT)).thenReturn(applicationFile);
			ApplicationSubmittedEvent event = new ApplicationSubmittedEvent("someSessionId", applicationId, FULL,
					Locale.ENGLISH);
			when(communicationClient.isCommHubEmailEnabled()).thenReturn(true);
			//when(communicationClient.sendEmailDataToCommhub(jsonObject)).
			when(routingDecisionService.getRoutingDestinationByName("Ramsey")).thenReturn(new CountyRoutingDestination(Ramsey, "DPI", "email", "(651)555-5555"));
			//applicationSubmittedListener.sendConfirmationEmail(event); //notifyApplicationSubmission(event);  
			//
		      try (MockedStatic<EmailParser> mockEmailParser = Mockito.mockStatic(EmailParser.class)) {
			        mockEmailParser.when(() -> EmailParser.parse(any())).thenReturn(Optional.of(email));
			        applicationSubmittedListener.sendConfirmationEmail(event);
			      }
		      
				verify(emailClient, never()).sendShortConfirmationEmail(applicationData,
			              email,
			              applicationId,
			              List.of(),
			              SnapExpeditedEligibility.ELIGIBLE,
			              CcapExpeditedEligibility.UNDETERMINED,
			              List.of(applicationFile),
			              Locale.ENGLISH);

			      System.out.println("--- emailClient never sent email");//TODO emj delete
		      
			     verify(communicationClient).sendEmailDataToCommhub(jsonObject);
		//	applicationSubmittedListener.notifyApplicationSubmission(event);
			 verify(communicationClient).send(jsonObject);

		//      when(applicationRepository.find(applicationId)).thenReturn(application);
		     // when(applicationData.isCAFApplication()).thenReturn(true);
		     // when(applicationData.getPagesData()).thenReturn(new PagesData());
		    //  when(snapExpeditedEligibilityDecider.decide(applicationData))
		    //      .thenReturn(SnapExpeditedEligibility.ELIGIBLE);
		   //   when(ccapExpeditedEligibilityDecider.decide(applicationData))
		   //       .thenReturn(CcapExpeditedEligibility.UNDETERMINED);
//		      ApplicationFile applicationFile = new ApplicationFile("someContent".getBytes(),
//		          "someFileName");
//		      when(pdfGenerator.generate(appIdFromDb, Document.CAF, CLIENT)).thenReturn(applicationFile);

//		      try (MockedStatic<EmailParser> mockEmailParser = Mockito.mockStatic(EmailParser.class)) {
//		        mockEmailParser.when(() -> EmailParser.parse(any())).thenReturn(Optional.of(email));
//		        applicationSubmittedListener.sendConfirmationEmail(event);
//		      }
		      //TODO verify comm hub sends JSON
		    //  JsonObject jsonObject = new JsonObject();
		    //  jsonObject.addProperty("whatever", "whatever");
		    //  verify(applicationSubmittedListener.notifyApplicationSubmission(event)).

//		      sendConfirmationEmail(applicationData,
//		          email,
//		          appIdFromDb,
//		          List.of(),
//		          SnapExpeditedEligibility.ELIGIBLE,
//		          CcapExpeditedEligibility.UNDETERMINED,
//		          List.of(applicationFile),
//		          Locale.ENGLISH);
		System.out.println("test to commhub complete");//TODO emj delete
		
	}

}
