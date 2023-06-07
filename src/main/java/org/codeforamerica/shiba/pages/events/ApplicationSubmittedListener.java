package org.codeforamerica.shiba.pages.events;

import static org.codeforamerica.shiba.output.Recipient.CLIENT;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.MonitoringService;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.application.parsers.ContactInfoParser;
import org.codeforamerica.shiba.application.parsers.DocumentListParser;
import org.codeforamerica.shiba.application.parsers.EmailParser;
import org.codeforamerica.shiba.mnit.RoutingDestination;
import org.codeforamerica.shiba.output.ApplicationFile;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.MnitDocumentConsumer;
import org.codeforamerica.shiba.output.caf.CcapExpeditedEligibility;
import org.codeforamerica.shiba.output.caf.CcapExpeditedEligibilityDecider;
import org.codeforamerica.shiba.output.caf.SnapExpeditedEligibility;
import org.codeforamerica.shiba.output.caf.SnapExpeditedEligibilityDecider;
import org.codeforamerica.shiba.output.pdf.PdfGenerator;
import org.codeforamerica.shiba.pages.config.FeatureFlagConfiguration;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.emails.EmailClient;
import org.slf4j.MDC;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ApplicationSubmittedListener extends ApplicationEventListener {

  private final MnitDocumentConsumer mnitDocumentConsumer;
  private final EmailClient emailClient;
  private final SnapExpeditedEligibilityDecider snapExpeditedEligibilityDecider;
  private final CcapExpeditedEligibilityDecider ccapExpeditedEligibilityDecider;
  private final PdfGenerator pdfGenerator;
  private final FeatureFlagConfiguration featureFlags;

  public ApplicationSubmittedListener(MnitDocumentConsumer mnitDocumentConsumer,
      ApplicationRepository applicationRepository,
      EmailClient emailClient,
      SnapExpeditedEligibilityDecider snapExpeditedEligibilityDecider,
      CcapExpeditedEligibilityDecider ccapExpeditedEligibilityDecider,
      PdfGenerator pdfGenerator,
      FeatureFlagConfiguration featureFlagConfiguration,
      MonitoringService monitoringService) {
    super(applicationRepository, monitoringService);
    this.mnitDocumentConsumer = mnitDocumentConsumer;
    this.emailClient = emailClient;
    this.snapExpeditedEligibilityDecider = snapExpeditedEligibilityDecider;
    this.ccapExpeditedEligibilityDecider = ccapExpeditedEligibilityDecider;
    this.pdfGenerator = pdfGenerator;
    this.featureFlags = featureFlagConfiguration;
  }

  @Async
  @EventListener
  public void sendViaApi(ApplicationSubmittedEvent event) {
    log.info("sendViaApi received ApplicationSubmittedEvent with application ID: "
        + event.getApplicationId());
    if (featureFlags.get("submit-via-api").isOn()) {
      Application application = getApplicationFromEvent(event);
      logTimeSinceCompleted(application);
      mnitDocumentConsumer.processCafAndCcap(application);
    }
    MDC.clear();
  }

  @Async
  @EventListener
  public void sendConfirmationEmail(ApplicationSubmittedEvent event) {
    Application application = getApplicationFromEvent(event);
    ApplicationData applicationData = application.getApplicationData();

    EmailParser.parse(applicationData).ifPresent(email -> {
      String applicationId = application.getId();
      SnapExpeditedEligibility snapExpeditedEligibility =
          snapExpeditedEligibilityDecider.decide(applicationData);
      CcapExpeditedEligibility ccapExpeditedEligibility =
          ccapExpeditedEligibilityDecider.decide(applicationData);
      List<Document> docs = DocumentListParser.parse(applicationData);
      List<ApplicationFile> pdfs = docs.stream()
          .map(doc -> pdfGenerator.generate(applicationId, doc, CLIENT)).toList();

      if (ContactInfoParser.optedIntoEmailCommunications(applicationData)) {
        emailClient.sendShortConfirmationEmail(applicationData, email, applicationId,
            new ArrayList<>(applicationData.getApplicantAndHouseholdMemberPrograms()),
            snapExpeditedEligibility, ccapExpeditedEligibility, pdfs, event.getLocale());
        emailClient.sendNextStepsEmail(applicationData, email, applicationId,
            new ArrayList<>(applicationData.getApplicantAndHouseholdMemberPrograms()),
            snapExpeditedEligibility, ccapExpeditedEligibility, pdfs, event.getLocale());
      } else {
        emailClient.sendConfirmationEmail(applicationData, email, applicationId,
            new ArrayList<>(applicationData.getApplicantAndHouseholdMemberPrograms()),
            snapExpeditedEligibility, ccapExpeditedEligibility, pdfs, event.getLocale());
      }
    });
    MDC.clear();
  }
  
  @Async
  @EventListener
  public void notifyApplicationSubmission(ApplicationSubmittedEvent event) {
    
      Application application = getApplicationFromEvent(event);
      ApplicationData applicationData = application.getApplicationData();
      
      //String url = "http://survey-tool.mn-benefits-non-prod.svc.cluster.local:8080/mnb-confirmation";
      
      String url = "https://surveys-mn-benefits-non-prod.apps.gj1k10ie.centralus.aroapp.io/mnb-confirmation";

      RestTemplate rt = new RestTemplate();
      
      ZonedDateTime completedAt = application.getCompletedAt();
      String completedAtTime = completedAt.format(DateTimeFormatter.ofPattern("MMM d uuuu", Locale.US));
      
      //County county = application.getCounty();
      //RoutingDestination countyRoutingDestination = routingDecisionService.getRoutingDestinationByName(county.name());


      JSONObject appJsonObject = new JSONObject();

      try {
        appJsonObject.put("appId", applicationData.getId());
        appJsonObject.put("firstName", ContactInfoParser.firstName(applicationData));
        appJsonObject.put("phoneNumber", ContactInfoParser.optedIntophoneNumber(applicationData).replaceAll("[^0-9]", ""));
        appJsonObject.put("email",  ContactInfoParser.optedIntoEmail(applicationData));
        appJsonObject.put("opt-status-sms", ContactInfoParser.optedIntoTEXT(applicationData));
        appJsonObject.put("opt-status-email", ContactInfoParser.optedIntoEmailCommunications(applicationData));
        appJsonObject.put("completed-dt", completedAtTime);
        //appJsonObject.put("county", countyRoutingDestination.getName());        
        //appJsonObject.put("countyPhoneNumber", countyRoutingDestination.getPhoneNumber());
                
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      
      HttpEntity<String> entity = 
            new HttpEntity<String>(appJsonObject.toString(), headers);
      
      
      ResponseEntity<String> responseEntityStr = rt.
            postForEntity(url, entity, String.class);      
      // TODO: retry?
      
      log.info("Result={}", responseEntityStr);      
      
  }
  
}
