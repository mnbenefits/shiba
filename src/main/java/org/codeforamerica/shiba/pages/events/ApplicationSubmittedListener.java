package org.codeforamerica.shiba.pages.events;

import static org.codeforamerica.shiba.output.Recipient.CLIENT;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.MonitoringService;
import org.codeforamerica.shiba.TribalNationRoutingDestination;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.application.parsers.ContactInfoParser;
import org.codeforamerica.shiba.application.parsers.DocumentListParser;
import org.codeforamerica.shiba.application.parsers.EmailParser;
import org.codeforamerica.shiba.mnit.CountyRoutingDestination;
import org.codeforamerica.shiba.mnit.RoutingDestination;
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
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.emails.EmailClient;
import org.codeforamerica.shiba.pages.rest.CommunicationClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ApplicationSubmittedListener extends ApplicationEventListener {

  private final MnitDocumentConsumer mnitDocumentConsumer;
  private final EmailClient emailClient;
  private final SnapExpeditedEligibilityDecider snapExpeditedEligibilityDecider;
  private final CcapExpeditedEligibilityDecider ccapExpeditedEligibilityDecider;
  private final PdfGenerator pdfGenerator;
  private final RoutingDecisionService routingDecisionService;
  private final WicRecommendationService wicRecommendationService;
  private final CommunicationClient communicationClient;
  private final String filenetEnabled;
  private final PdfEncoder pdfEncoder;
  private final String emailSender;

  public ApplicationSubmittedListener(MnitDocumentConsumer mnitDocumentConsumer,
      ApplicationRepository applicationRepository,
      EmailClient emailClient,
      SnapExpeditedEligibilityDecider snapExpeditedEligibilityDecider,
      CcapExpeditedEligibilityDecider ccapExpeditedEligibilityDecider,
      PdfGenerator pdfGenerator,
      MonitoringService monitoringService,
      RoutingDecisionService routingDecisionService,
      CommunicationClient communicationClient,
      PdfEncoder pdfEncoder,
      WicRecommendationService wicRecommendationService,
      @Value ("${mnit-filenet.enabled}") String filenetEnabled,
      @Value("${comm-hub-email.delivery}") String emailSender) {
    super(applicationRepository, monitoringService);
    this.mnitDocumentConsumer = mnitDocumentConsumer;
    this.emailClient = emailClient;
    this.snapExpeditedEligibilityDecider = snapExpeditedEligibilityDecider;
    this.ccapExpeditedEligibilityDecider = ccapExpeditedEligibilityDecider;
    this.pdfGenerator = pdfGenerator;
    this.routingDecisionService = routingDecisionService;
    this.communicationClient = communicationClient;
    this.filenetEnabled = filenetEnabled;
	this.pdfEncoder = pdfEncoder;
    this.wicRecommendationService = wicRecommendationService;
    this.emailSender = emailSender;
  }

  @Async
  @EventListener
  public void sendViaApi(ApplicationSubmittedEvent event) {
    log.info("sendViaApi received ApplicationSubmittedEvent with application ID: "
        + event.getApplicationId());
    if(Boolean.parseBoolean(filenetEnabled)) {
      Application application = getApplicationFromEvent(event);
      logTimeSinceCompleted(application);
      mnitDocumentConsumer.processCafAndCcap(application);
    }
    MDC.clear();
  }


  /**
   * This event listener will control whether or not MNbenefits sends client emails.
   * It does not control whether or not the comm-hub sends emails.
   * @param event
   */
	@Async
	@EventListener
	public void sendConfirmationEmail(ApplicationSubmittedEvent event) {
		Application application = getApplicationFromEvent(event);
		ApplicationData applicationData = application.getApplicationData();

		EmailParser.parse(applicationData).ifPresent(email -> {
			String applicationId = application.getId();
			SnapExpeditedEligibility snapExpeditedEligibility = snapExpeditedEligibilityDecider.decide(applicationData);
			CcapExpeditedEligibility ccapExpeditedEligibility = ccapExpeditedEligibilityDecider.decide(applicationData);
			List<Document> docs = DocumentListParser.parse(applicationData);
			List<ApplicationFile> pdfs = docs.stream().map(doc -> pdfGenerator.generate(applicationId, doc, CLIENT))
					.toList();

			if (ContactInfoParser.optedIntoEmailCommunications(applicationData)) {
				sendOptedInEmails(applicationData, email, applicationId, snapExpeditedEligibility, ccapExpeditedEligibility, pdfs, event);
			} else {
				sendConfirmationEmailForOptOut(applicationData, email, applicationId, snapExpeditedEligibility, ccapExpeditedEligibility, pdfs, event);
			}
		});
		MDC.clear();
	}
	
	private void sendOptedInEmails(ApplicationData applicationData, String email, String applicationId, SnapExpeditedEligibility snapExpeditedEligibility,
			CcapExpeditedEligibility ccapExpeditedEligibility, List<ApplicationFile> pdfs, ApplicationSubmittedEvent event ) {
		if(emailSender.equalsIgnoreCase("mnbenefits")) {
		emailClient.sendShortConfirmationEmail(applicationData, email, applicationId,
				new ArrayList<>(applicationData.getApplicantAndHouseholdMemberPrograms()),
				snapExpeditedEligibility, ccapExpeditedEligibility, pdfs, event.getLocale());
		emailClient.sendNextStepsEmail(applicationData, email, applicationId,
				new ArrayList<>(applicationData.getApplicantAndHouseholdMemberPrograms()),
				snapExpeditedEligibility, ccapExpeditedEligibility, pdfs, event.getLocale());
		}
	}

	private void sendConfirmationEmailForOptOut(ApplicationData applicationData, String email, String applicationId, SnapExpeditedEligibility snapExpeditedEligibility,
			CcapExpeditedEligibility ccapExpeditedEligibility, List<ApplicationFile> pdfs, ApplicationSubmittedEvent event) {
		if(emailSender.equalsIgnoreCase("mnbenefits")) {
			emailClient.sendConfirmationEmail(applicationData, email, applicationId,
					new ArrayList<>(applicationData.getApplicantAndHouseholdMemberPrograms()),
					snapExpeditedEligibility, ccapExpeditedEligibility, pdfs, event.getLocale());
		}
	}

	@Async
	@EventListener
	/**
	 * The method consumes the ApplicationSubmittedEvent and makes a REST call to comm-hub endpoint. 
	 * The method offloads communication responsibility to the comm-hub. 
	 * @param event
	 */
	public void notifyApplicationSubmission(ApplicationSubmittedEvent event) {
		Application application = getApplicationFromEvent(event);
		ApplicationData applicationData = application.getApplicationData();
	
	    String pdfString = pdfEncoder.createEncodedPdfString(application);
	   
		MDC.put("applicationId", application.getId());

        Timestamp completedAt = Timestamp.valueOf(application.getCompletedAt().toLocalDateTime());

		County county = application.getCounty();
		RoutingDestination routingDestination = routingDecisionService.getRoutingDestinationByName(county.name());
		Set<RoutingDestination> destinations = routingDecisionService.findRoutingDestinations(application);
		JsonObject appJsonObject = new JsonObject();
		appJsonObject.addProperty("appId", applicationData.getId());
		appJsonObject.addProperty("expedited", applicationData.getExpeditedEligibility().toString());
		appJsonObject.addProperty("firstName", ContactInfoParser.firstName(applicationData));
		appJsonObject.addProperty("lastName", ContactInfoParser.lastName(applicationData));
		appJsonObject.addProperty("phoneNumber", ContactInfoParser.phoneNumber(applicationData).replaceAll("[^0-9]", ""));
		appJsonObject.addProperty("email", ContactInfoParser.email(applicationData));
		appJsonObject.addProperty("opt-status-sms", ContactInfoParser.optedIntoTEXT(applicationData));
		appJsonObject.addProperty("wic-message", wicRecommendationService.showWicMessage(applicationData));
		appJsonObject.addProperty("opt-status-email", ContactInfoParser.optedIntoEmailCommunications(applicationData));
		appJsonObject.addProperty("writtenLangPref", ContactInfoParser.writtenLanguagePref(applicationData));
        appJsonObject.addProperty("spokenLangPref", ContactInfoParser.spokenLanguagePref(applicationData));
        appJsonObject.addProperty("applicationPDF", pdfString);
		appJsonObject.addProperty("completed-dt", completedAt.toString());
		appJsonObject.addProperty("county", routingDestination.getName());
		appJsonObject.addProperty("countyPhoneNumber", routingDestination.getPhoneNumber());
		destinations.forEach(destination -> {
			if (destination instanceof CountyRoutingDestination){
				appJsonObject.addProperty("countyRoutingDestination", destination.getName());
				appJsonObject.addProperty("countyRoutingPhoneNumber",  destination.getPhoneNumber());
			}else if(destination instanceof TribalNationRoutingDestination){
				appJsonObject.addProperty("tribalNationRoutingDestination", destination.getName());
				appJsonObject.addProperty("tribalNationRoutingPhoneNumber",  destination.getPhoneNumber());
			}
		});
			
		communicationClient.send(appJsonObject);
		
		MDC.clear();
		
	}
	

  
}
