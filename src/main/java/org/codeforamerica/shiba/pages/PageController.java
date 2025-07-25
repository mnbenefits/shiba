package org.codeforamerica.shiba.pages;

import static java.lang.Boolean.parseBoolean;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.codeforamerica.shiba.application.FlowType.HEALTHCARE_RENEWAL;
import static org.codeforamerica.shiba.application.FlowType.LATER_DOCS;
import static org.codeforamerica.shiba.application.Status.DELIVERED;
import static org.codeforamerica.shiba.application.Status.SENDING;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getFirstValue;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.HAS_HOUSE_HOLD;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.HOME_ZIPCODE;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.USE_ENRICHED_HOME_COUNTY;
import static org.codeforamerica.shiba.output.Document.UPLOADED_DOC;

import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.codeforamerica.shiba.Program;
import org.codeforamerica.shiba.RoutingDestinationMessageService;
import org.codeforamerica.shiba.UploadDocumentConfiguration;
import org.codeforamerica.shiba.Utils;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.ApplicationFactory;
import org.codeforamerica.shiba.application.ApplicationRepository;
import org.codeforamerica.shiba.application.ApplicationStatusRepository;
import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.application.parsers.CountyParser;
import org.codeforamerica.shiba.application.parsers.DocumentListParser;
import org.codeforamerica.shiba.configurations.CityInfoConfiguration;
import org.codeforamerica.shiba.documents.DocumentRepository;
import org.codeforamerica.shiba.inputconditions.Condition;
import org.codeforamerica.shiba.internationalization.LocaleSpecificMessageSource;
import org.codeforamerica.shiba.mnit.RoutingDestination;
import org.codeforamerica.shiba.output.CustomMultipartFile;
import org.codeforamerica.shiba.output.caf.CcapExpeditedEligibilityDecider;
import org.codeforamerica.shiba.output.caf.Eligibility;
import org.codeforamerica.shiba.output.caf.EligibilityListBuilder;
import org.codeforamerica.shiba.output.caf.ExpeditedEligibility;
import org.codeforamerica.shiba.output.caf.SnapExpeditedEligibilityDecider;
import org.codeforamerica.shiba.pages.config.ApplicationConfiguration;
import org.codeforamerica.shiba.pages.config.FeatureFlagConfiguration;
import org.codeforamerica.shiba.pages.config.LandmarkPagesConfiguration;
import org.codeforamerica.shiba.pages.config.NextPage;
import org.codeforamerica.shiba.pages.config.PageConfiguration;
import org.codeforamerica.shiba.pages.config.PageTemplate;
import org.codeforamerica.shiba.pages.config.PageWorkflowConfiguration;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.pages.data.DatasourcePages;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;
import org.codeforamerica.shiba.pages.data.UploadedDocument;
import org.codeforamerica.shiba.pages.enrichment.ApplicationEnrichment;
import org.codeforamerica.shiba.pages.events.ApplicationSubmittedEvent;
import org.codeforamerica.shiba.pages.events.PageEventPublisher;
import org.codeforamerica.shiba.pages.events.SubworkflowCompletedEvent;
import org.codeforamerica.shiba.pages.events.SubworkflowIterationDeletedEvent;
import org.codeforamerica.shiba.pages.events.UploadedDocumentsSubmittedEvent;
import org.jboss.logging.MDC;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import mobi.openddr.classifier.model.Device;
import net.coobird.thumbnailator.Thumbnails;

@Controller
@Slf4j
public class PageController {

  private static final ZoneId CENTRAL_TIMEZONE = ZoneId.of("America/Chicago");
  private static final int MAX_FILES_UPLOADED = 50;
  private static final int TOTAL_MAX_FILE_SIZE = 250000000; //bytes
  private static final String VIRUS_STATUS_CODE = "418";
  private final ApplicationData applicationData;
  private final ApplicationConfiguration applicationConfiguration;
  private final Clock clock;
  private final ApplicationRepository applicationRepository;
  private final ApplicationFactory applicationFactory;
  private final MessageSource messageSource;
  private final PageEventPublisher pageEventPublisher;
  private final ApplicationEnrichment applicationEnrichment;
  private final FeatureFlagConfiguration featureFlags;
  private final UploadDocumentConfiguration uploadDocumentConfiguration;
  private final CityInfoConfiguration cityInfoConfiguration;
  private final SnapExpeditedEligibilityDecider snapExpeditedEligibilityDecider;
  private final CcapExpeditedEligibilityDecider ccapExpeditedEligibilityDecider;
  private final NextStepsContentService nextStepsContentService;
  private final DocRecommendationMessageService docRecommendationMessageService;
  private final WicRecommendationService wicRecommendationService;
  private final RoutingDecisionService routingDecisionService;
  private final DocumentRepository documentRepository;
  private final RoutingDestinationMessageService routingDestinationMessageService;
  private final ApplicationStatusRepository applicationStatusRepository;
  private final EligibilityListBuilder listBuilder;
  private final String clammitUrl;
  private final String clammitEnabled;
  private final String filenetEnabled;
  private static final List<String> IMAGE_TYPES_TO_COMPRESS = List
	      .of("jpg", "jpeg");
  private static float imageQuality;


  public PageController(
      ApplicationConfiguration applicationConfiguration,
      ApplicationData applicationData,
      Clock clock,
      ApplicationFactory applicationFactory,
      MessageSource messageSource,
      PageEventPublisher pageEventPublisher,
      ApplicationEnrichment applicationEnrichment,
      FeatureFlagConfiguration featureFlags,
      UploadDocumentConfiguration uploadDocumentConfiguration,
      CityInfoConfiguration cityInfoConfiguration,
      SnapExpeditedEligibilityDecider snapExpeditedEligibilityDecider,
      CcapExpeditedEligibilityDecider ccapExpeditedEligibilityDecider,
      NextStepsContentService nextStepsContentService,
      DocRecommendationMessageService docRecommendationMessageService,
      WicRecommendationService wicRecommendationService,
      RoutingDecisionService routingDecisionService,
      DocumentRepository documentRepository,
      ApplicationRepository applicationRepository,
      RoutingDestinationMessageService routingDestinationMessageService,
      ApplicationStatusRepository applicationStatusRepository,
      EligibilityListBuilder listBuilder, 
      @Value("${mnit-clammit.url}") String clammitUrl,
      @Value("${mnit-clammit.enabled}") String clammitEnabled,
      @Value("${mnit-filenet.enabled}") String filenetEnabled) {
    this.applicationData = applicationData;
    this.applicationConfiguration = applicationConfiguration;
    this.clock = clock;
    this.applicationFactory = applicationFactory;
    this.messageSource = messageSource;
    this.pageEventPublisher = pageEventPublisher;
    this.applicationEnrichment = applicationEnrichment;
    this.featureFlags = featureFlags;
    this.uploadDocumentConfiguration = uploadDocumentConfiguration;
    this.cityInfoConfiguration = cityInfoConfiguration;
    this.snapExpeditedEligibilityDecider = snapExpeditedEligibilityDecider;
    this.ccapExpeditedEligibilityDecider = ccapExpeditedEligibilityDecider;
    this.nextStepsContentService = nextStepsContentService;
    this.docRecommendationMessageService = docRecommendationMessageService;
    this.wicRecommendationService = wicRecommendationService;
    this.routingDecisionService = routingDecisionService;
    this.documentRepository = documentRepository;
    this.applicationRepository = applicationRepository;
    this.routingDestinationMessageService = routingDestinationMessageService;
    this.applicationStatusRepository = applicationStatusRepository;
    this.listBuilder = listBuilder;
    this.clammitUrl = clammitUrl;
    this.clammitEnabled = clammitEnabled;
    this.filenetEnabled = filenetEnabled;
  }

  @GetMapping("/")
  ModelAndView getRoot() {
    return new ModelAndView(
        "forward:/pages/" + applicationConfiguration.getLandmarkPages().getLandingPages()
            .get(0));
  }

  @GetMapping("/privacy")
  String getPrivacyPolicy() {
    return "privacyPolicy";
  }

  @GetMapping("/faq")
  String getFaq() {
    return "faq";
  }

  @GetMapping("/snapNDS")
  String getSnapNDS() {
    return "snapNDS";
  }

  @GetMapping("/languageAndAccessibility")
  String getLanguageAndAccessibility() {
    return "languageAndAccessibility";
  }

  @GetMapping("/errorTimeout")
  String getErrorTimeout(@CookieValue(value = "application_id", defaultValue = "") String submittedAppId,
		  @CookieValue(value = "flow_type", defaultValue = "") String flowType,
		  @CookieValue(value = "page_name", defaultValue = "") String pageName,
		  HttpServletResponse httpResponse) {
    if(pageName.equals("healthcareRenewalUpload") || flowType.equals(FlowType.HEALTHCARE_RENEWAL.toString())) {
    	return "healthcareRenewalErrorUploadTimeout";
    }
    else if(pageName.equals("readyToUploadDocuments") || flowType.equals(FlowType.LATER_DOCS.toString())) {
    	return "errorUploadTimeout";
    }
    else if (submittedAppId.length() == 0) {
        return "errorSessionTimeout";
    }else {
    	// clear the cookie value so we don't keep getting upload timeouts
        Cookie submitCookie = new Cookie("application_id", "");
        submitCookie.setPath("/");
        submitCookie.setHttpOnly(true);
        submitCookie.setSecure(true);
        httpResponse.addCookie(submitCookie);
    	return "errorUploadTimeout";
    }
  }

  @GetMapping("/pages/{pageName}/navigation")
  RedirectView navigation(
      @PathVariable String pageName,
      @RequestParam(required = false, defaultValue = "0") Integer option
  ) {
    PageWorkflowConfiguration currentPage = applicationConfiguration.getWorkflow().get(pageName);
    if (currentPage == null) {
    	log.error("navigation error for pageName " + pageName);
    	return new RedirectView("/error");
    }

    PagesData pagesData = applicationData.getPagesData();
    NextPage nextPage = applicationData.getNextPageName(featureFlags, currentPage, option);
    ofNullable(nextPage.getFlow()).ifPresent(applicationData::setFlow);
    PageWorkflowConfiguration nextPageWorkflow = applicationConfiguration.getWorkflow()
        .get(nextPage.getPageName());

    if (shouldSkip(nextPageWorkflow)) {
      pagesData.remove(nextPageWorkflow.getPageConfiguration().getName());
      return new RedirectView(String.format("/pages/%s/navigation", nextPage.getPageName()));
    } else {
      return new RedirectView(String.format("/pages/%s", nextPage.getPageName()));
    }
  }

  private boolean shouldSkip(PageWorkflowConfiguration nextPageWorkflow) {
    Condition skipCondition = nextPageWorkflow.getSkipCondition();
    if (skipCondition != null) {
      PagesData pagesData = applicationData.getDatasourceDataForPageIncludingSubworkflows(
          nextPageWorkflow);
      String pageName = nextPageWorkflow.getPageConfiguration().getName();
      DatasourcePages datasourcePages = new DatasourcePages(pagesData);
      boolean retVal = datasourcePages.satisfies(skipCondition);
      log.debug("===== skipCondition " + skipCondition.getName() + " for page " + pageName + " returns " + retVal);
      return retVal;
    }
    return false;
  }

  @GetMapping("/pages/{pageName}")
  ModelAndView getPage(
      @PathVariable String pageName,
      @RequestParam(required = false, defaultValue = "") String iterationIndex,
      @RequestParam(name = "utm_source", defaultValue = "", required = false) String utmSource,
      HttpServletResponse response,
      HttpSession httpSession,
      Locale locale
  ) {
	  // Temporary cookie indicating user page
      Cookie pageNameCookie = new Cookie("page_name", StringEscapeUtils.escapeJava(pageName));
      pageNameCookie.setPath("/");
      pageNameCookie.setSecure(true);
      pageNameCookie.setHttpOnly(true);
      response.addCookie(pageNameCookie);
      //to set flow on cookie
      Cookie flowCookie = new Cookie("flow_type", applicationData.getFlow().toString());
      flowCookie.setPath("/");
      flowCookie.setSecure(true);
      flowCookie.setHttpOnly(true);
      response.addCookie(flowCookie);
      

    var landmarkPagesConfiguration = applicationConfiguration.getLandmarkPages();

    if (landmarkPagesConfiguration.isLandingPage(pageName)) {
      // in case we have already submitted and completed an application
      landmarkPagesConfiguration.removeCompleted();
      httpSession.invalidate();
    }
   
    if (landmarkPagesConfiguration.isStartTimerPage(pageName)) {
      applicationData.setStartTimeOnce(clock.instant());
      if (!utmSource.isEmpty()) {
        applicationData.setUtmSource(utmSource);
      }
    }

    // Validations and special case redirects
    if (shouldRedirectToUploadDocumentsPage(pageName)) {
      log.info(
          "documentSubmitConfirmation redirect back to uploadDocuments, no documents in uploadDocs list");
      return new ModelAndView(
          String.format("redirect:/pages/%s", landmarkPagesConfiguration.getCorrectUploadDocumentPage(pageName)));
    }
    
    if (isDocsUploadedandCompleted(pageName)) {
      landmarkPagesConfiguration.addCompleted(pageName);    
    }
    if (shouldRedirectToTerminalPage(pageName)) {
      return new ModelAndView(
          String.format("redirect:/pages/%s", landmarkPagesConfiguration.getTerminalPage()));
    }
    
    if (shouldRedirectToSubmissionConfirmation(pageName)) {   	
      return new ModelAndView(
          String.format("redirect:/pages/%s", landmarkPagesConfiguration.getSubmissionConfirmationPage()));
      }

    if (shouldRedirectToProgramDocumentsPage(pageName)) {
      return new ModelAndView(
          String.format("redirect:/pages/%s", landmarkPagesConfiguration.getProgramDocumentsPage()));
    }
    
   if (shouldRedirectToLaterDocsTerminalPage(pageName)) {
      return new ModelAndView(
          String.format("redirect:/pages/%s",
              landmarkPagesConfiguration.getLaterDocsTerminalPage()));
    }
   
   if (shouldRedirectToHealthcareRenewalLandingPage(pageName)) {
       httpSession.invalidate();
       return new ModelAndView(
               String.format("redirect:/pages/%s",
                   landmarkPagesConfiguration.getHealthcareRenewalLandingPage()));
     }
   
    if (shouldRedirectToHealthcareRenewalTerminalPage(pageName)) {
      return new ModelAndView(
          String.format("redirect:/pages/%s",
              landmarkPagesConfiguration.getHealthcareRenewalTerminalPage()));
    }

    if (shouldRedirectToLandingPage(pageName)) {
      return new ModelAndView(
          String.format("redirect:/pages/%s",
              landmarkPagesConfiguration.getLandingPages().get(0)));
    }

    var pageWorkflowConfig = applicationConfiguration.getWorkflow().get(pageName);
    if (pageWorkflowConfig == null) {
    	log.error("getPage error for pageName " + pageName);
      return new ModelAndView("redirect:/error");
    }

    if (missingRequiredSubworkflows(pageWorkflowConfig)) {
      return new ModelAndView(
          "redirect:/pages/" + pageWorkflowConfig.getDataMissingRedirect());
    }

    // Update pagesData with data for incomplete subworkflows
    var pagesData = applicationData.getPagesData();
    if (pageWorkflowConfig.getGroupName() != null) { // If page is part of a group
      var dataForIncompleteIteration = getIncompleteIterationPagesData(pageName,
          pageWorkflowConfig);

      if (dataForIncompleteIteration == null) {
        String redirectPageForGroup = applicationConfiguration.getPageGroups()
            .get(pageWorkflowConfig.getGroupName()).getRedirectPage();
        return new ModelAndView("redirect:/pages/" + redirectPageForGroup);
      }
      pagesData = (PagesData) pagesData
          .clone(); // Avoid changing the original applicationData PagesData by cloning the object
      pagesData.putAll(dataForIncompleteIteration);
    }

    // Add extra pagesData if this page workflow specifies that it applies to a group
    if (requestedPageAppliesToGroup(iterationIndex, pageWorkflowConfig)) {
      String groupName = pageWorkflowConfig.getAppliesToGroup();
      if (Integer.parseInt(iterationIndex) < applicationData.getSubworkflows().get(groupName)
          .size()) {
        var dataForGroup = getPagesDataForGroupAndIteration(iterationIndex,
            pageWorkflowConfig,
            groupName);

        pagesData = (PagesData) pagesData.clone();
        pagesData.putAll(dataForGroup);
      } else {
        return new ModelAndView(
            "redirect:/pages/" + applicationConfiguration.getPageGroups().get(groupName)
                .getReviewPage());
      }
    }

    var pageTemplate = pagesData.evaluate(featureFlags, pageWorkflowConfig, applicationData);

    var model = buildModelForThymeleaf(pageName, locale, landmarkPagesConfiguration,
        pageTemplate,
        pageWorkflowConfig, pagesData, iterationIndex);
    var view =
        pageWorkflowConfig.getPageConfiguration().isUsingPageTemplateFragment() ? "pageTemplate"
            : pageName;
    return new ModelAndView(view, model);
  }

  private PagesData getPagesDataForGroupAndIteration(String iterationIndex,
      PageWorkflowConfiguration pageWorkflowConfig, String groupName) {
    return pageWorkflowConfig.getSubworkflows(applicationData)
        .get(groupName)
        .get(Integer.parseInt(iterationIndex))
        .getPagesData();
  }

  private PagesData getIncompleteIterationPagesData(String pageName,
      PageWorkflowConfiguration pageWorkflow) {
    PagesData currentIterationPagesData;
    String groupName = pageWorkflow.getGroupName();
    if (isStartPageForGroup(pageName, groupName)) {
      currentIterationPagesData = applicationData.getIncompleteIterations()
          .getOrDefault(groupName, new PagesData());
    } else {
      currentIterationPagesData = applicationData.getIncompleteIterations().get(groupName);
    }
    return currentIterationPagesData;
  }

  /** 
   * This method may be intended to ensure workflow configurations are complete.
   * The method name "hasRequiredSubworkflows" implies this is to enforce correct configuration.
   * In certain conditions that use subworkflow data, there may no need for that 
   * data, so the PageDatasource optional variable is set to true OR the groupName is missing. (See the ApplicationData 
   * hasRequiredSubworkflows method)
   * This needs further investigation for a better explanation of what this is doing.
   * In some pages, booleandoesNotHaveRSWFs is true, example pages are homeExpenses, legalStuff, signThisApplication, so it isn't clear what this is for.
   * Modified this method to log any time doesNotHaveRSWFs is true for development purposes. Uncomment for troubleshooting.
   * @param pageWorkflow
   * @return
   */
  private boolean missingRequiredSubworkflows(PageWorkflowConfiguration pageWorkflow) {
	  var dataSources = pageWorkflow.getDatasources();
	  boolean inputsAreEmpty = pageWorkflow.getPageConfiguration().getInputs().isEmpty();
	  boolean doesNotHaveRSWFs = !applicationData.hasRequiredSubworkflows(dataSources);
//	  if(doesNotHaveRSWFs) {
//		  log.info("CONFIGURATION ERROR! A datasource may need to be set to optional = true for pageworkflow " + pageWorkflow.getPageConfiguration().getName());
//	  }
    return inputsAreEmpty && doesNotHaveRSWFs;
  }

  private boolean isStartPageForGroup(@PathVariable String pageName, String groupName) {
    return applicationConfiguration.getPageGroups().get(groupName).getStartPages()
        .contains(pageName);
  }

  @NotNull
  private Map<String, Object> buildModelForThymeleaf(String pageName, Locale locale,
      LandmarkPagesConfiguration landmarkPagesConfiguration, PageTemplate pageTemplate,
      PageWorkflowConfiguration pageWorkflow, PagesData pagesData, String iterationIndex) {
    Map<String, Object> model = new HashMap<>();
    model.put("page", pageTemplate);
    model.put("pageName", pageName);
    model.put("postTo",
        landmarkPagesConfiguration.isSubmitPage(pageName) ? "/submit" : "/pages/" + pageName);
    model.put("applicationId", applicationData.getId());
    model.put("county", CountyParser.parse(applicationData));
    model.put("cityInfo", cityInfoConfiguration.getCityToZipAndCountyMapping());
    model.put("zipCode", getFirstValue(applicationData.getPagesData(), HOME_ZIPCODE));
    model.put("featureFlags", featureFlags);
    model.put("hasHousehold", getFirstValue(applicationData.getPagesData(), HAS_HOUSE_HOLD));

    var snapExpeditedEligibility = snapExpeditedEligibilityDecider.decide(applicationData);
    model.put("expeditedSnap", snapExpeditedEligibility);
    var ccapExpeditedEligibility = ccapExpeditedEligibilityDecider.decide(applicationData);
    model.put("expeditedCcap", ccapExpeditedEligibility);
    List<Eligibility> expeditedEligibilityList = new ArrayList<>();
    expeditedEligibilityList.add(snapExpeditedEligibility);
    expeditedEligibilityList.add(ccapExpeditedEligibility);
    List<ExpeditedEligibility> list = listBuilder.buildEligibilityList(expeditedEligibilityList);
    applicationData.setExpeditedEligibility(list);

    if (pageWorkflow.getPageConfiguration().isStaticPage()) {
      model.put("pageNameContext", pageName);
    }

    Set<String> programs = applicationData.getApplicantAndHouseholdMemberPrograms();
    if (!programs.isEmpty()) {
      model.put("programs", String.join(", ", programs));
    }
    model.put("totalMilestones", programs.contains(Program.CERTAIN_POPS) ? "7" : "6");

	if (landmarkPagesConfiguration.isPostSubmitPage(pageName)) {
      model.put("docRecommendations", docRecommendationMessageService
          .getPageSpecificRecommendationsMessage(applicationData, locale));
      model.put("nextStepSections", nextStepsContentService
          .createSectionsForNextStepsPage(new ArrayList<>(programs), snapExpeditedEligibility,
              ccapExpeditedEligibility, locale));
      model.put("nextStepsDocumentUpload", nextStepsContentService
              .getNextStepsForDocumentUpload(!applicationData.getUploadedDocs().isEmpty(), locale));
      model.put("nextStepsAllowTimeForReview", nextStepsContentService
              .getNextStepsAllowTimeForReview(new ArrayList<>(programs), snapExpeditedEligibility,
                  ccapExpeditedEligibility, locale));
      model.put("nextStepsCompleteAnInterview", nextStepsContentService
              .getNextStepsCompleteAnInterview(new ArrayList<>(programs), snapExpeditedEligibility,
                  ccapExpeditedEligibility, locale));
      }
    // the terminal page has always been the success page. The success page needs more items in the model to display correctly.
    if (landmarkPagesConfiguration.isTerminalPage(pageName) || landmarkPagesConfiguration.isHealthcareRenewalTerminalPage(pageName)) {
      Application application = applicationRepository.find(applicationData.getId());
      model.put("documents", DocumentListParser.parse(application.getApplicationData()));
      model.put("hasUploadDocuments", !applicationData.getUploadedDocs().isEmpty());
      ZonedDateTime zonedDateTime = application.getCompletedAt();
      if(zonedDateTime == null) {
    	  zonedDateTime = ZonedDateTime.now(CENTRAL_TIMEZONE);
      }else {
    	  zonedDateTime = zonedDateTime.withZoneSameInstant(CENTRAL_TIMEZONE);
      }
      model.put("submissionTime", zonedDateTime);
      
      model.put("feedbackText", application.getFeedback());
      model.put("combinedFormText", applicationData.combinedApplicationProgramsList());   
      String inputData = pagesData
          .getPageInputFirstValue("healthcareCoverage", "healthcareCoverage");
      boolean doesNotHaveHealthcare = !"YES".equalsIgnoreCase(inputData);
      boolean isNotCertainPops = !application.getApplicationData().isCertainPopsApplication();
      boolean recommendHealthCare = doesNotHaveHealthcare && isNotCertainPops;
      model.put("recommendHealthCare", recommendHealthCare);
      boolean isCCAP = application.getApplicationData().isCCAPApplication();
      model.put("recommendCC", isCCAP);
      boolean recommendWIC = wicRecommendationService.showWicMessage(application.getApplicationData());
      model.put("recommendWIC", recommendWIC);    
      boolean showRecommendationLink = recommendHealthCare || isCCAP || recommendWIC;
      model.put("showRecommendationLink", showRecommendationLink);
      Sentiment sentiment = application.getSentiment();
      boolean showFeedback = sentiment == null ? true:false;
      model.put("showFeedback", showFeedback); 
      // Get all routing destinations for this application
      Set<RoutingDestination> routingDestinations = new LinkedHashSet<>();
      DocumentListParser.parse(applicationData).forEach(doc -> {
        List<RoutingDestination> routingDestinationsForThisDoc =
            routingDecisionService.getRoutingDestinations(applicationData, doc);
        routingDestinations.addAll(routingDestinationsForThisDoc);
      });
      applicationRepository.save(application);

      // Generate human-readable list of routing destinations for success page
      String finalDestinationList = routingDestinationMessageService.generatePhrase(locale,
          application.getCounty(),
          true,
          new ArrayList<>(routingDestinations));
      model.put("routingDestinationList", finalDestinationList);
    }
    
    if (landmarkPagesConfiguration.isRecommendationsPage(pageName)){
        Application application = applicationRepository.find(applicationData.getId());
        String inputData = pagesData
                .getPageInputFirstValue("healthcareCoverage", "healthcareCoverage");
        boolean doesNotHaveHealthcare = !"YES".equalsIgnoreCase(inputData);
        boolean isNotCertainPops = !application.getApplicationData().isCertainPopsApplication();
        boolean recommendHealthCare = doesNotHaveHealthcare && isNotCertainPops;
        model.put("recommendHealthCare", recommendHealthCare);
        boolean isCCAP = application.getApplicationData().isCCAPApplication();
        model.put("recommendCC", isCCAP);
        boolean recommendWIC = wicRecommendationService.showWicMessage(application.getApplicationData());
        model.put("recommendWIC", recommendWIC);    
        Sentiment sentiment = application.getSentiment();
         boolean showFeedback = sentiment == null ? true:false;
         model.put("showFeedback", showFeedback); 
    }
    


    if (landmarkPagesConfiguration.isUploadDocumentsPage(pageName)) {
      record DocWithThumbnail(UploadedDocument doc, String thumbnail) {

      }
      List<DocWithThumbnail> uploadedDocsWithThumbnails = applicationData.getUploadedDocs().stream()
          .parallel()
          .map(doc -> new DocWithThumbnail(doc, documentRepository.getThumbnail(doc)))
          .toList();
      model.put("uploadedDocs", uploadedDocsWithThumbnails);
      model.put("uploadDocMaxFileSize", uploadDocumentConfiguration.getMaxFilesize());
    }

    if (pageWorkflow.getPageConfiguration().isStaticPage() || !pageWorkflow
        .getPageConfiguration()
        .isUsingPageTemplateFragment()) {
      model.put("data", pagesData.getDatasourcePagesBy(pageWorkflow.getDatasources()));

      if (applicationData.hasRequiredSubworkflows(pageWorkflow.getDatasources())) {
        model.put("subworkflows", pageWorkflow.getSubworkflows(applicationData));
        model.put("subworkflowsNext", (!pageWorkflow.getSubworkflows(applicationData).keySet().isEmpty())?pageWorkflow.getSubworkflows(applicationData).keySet().iterator().next():"") ;
        if (isNotBlank(iterationIndex)) {
          var iterationData = pageWorkflow.getSubworkflows(applicationData)
              .get(pageWorkflow.getAppliesToGroup())
              .get(Integer.parseInt(iterationIndex));
          model.put("iterationData", iterationData);
        }
      }
    } else {
      model.put("pageDatasources",
          pagesData.getDatasourcePagesBy(pageWorkflow.getDatasources())
              .mergeDatasourcePages(
                  pagesData.getDatasourceGroupBy(pageWorkflow.getDatasources(),
                      applicationData.getSubworkflows())));
      model.put("data", pagesData
          .getPageDataOrDefault(pageTemplate.getName(), pageWorkflow.getPageConfiguration()));
    }
    model.put("applicationData", applicationData);

    return model;
  }

  private boolean requestedPageAppliesToGroup(String iterationIndex,
      PageWorkflowConfiguration pageWorkflow) {
    return isNotBlank(iterationIndex) && applicationData.getSubworkflows()
        .containsKey(pageWorkflow.getAppliesToGroup());
  }

  private boolean shouldRedirectToLandingPage(@PathVariable String pageName) {
    LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
        .getLandmarkPages();
    // If they requested landing page or application is unstarted
    boolean unstarted = !landmarkPagesConfiguration.isLandingPage(pageName)
        && applicationData.getStartTime() == null;
    // If they are restarting the application process after submitting
    boolean restarted =
        applicationData.isSubmitted() && landmarkPagesConfiguration.isStartTimerPage(pageName);
    return unstarted || restarted;
  }

  private boolean shouldRedirectToUploadDocumentsPage(@PathVariable String pageName) {
    LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
        .getLandmarkPages();
    return landmarkPagesConfiguration.isSubmitUploadedDocumentsPage(pageName) &&
        applicationData.getUploadedDocs().size() < 1;
  }

  private boolean shouldRedirectToTerminalPage(@PathVariable String pageName) {
    LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
        .getLandmarkPages();
    // Application is already submitted and not at the beginning of the application process
    return  !landmarkPagesConfiguration.isFeedbackPage(pageName) &&
    		landmarkPagesConfiguration.isCompleted() &&
            !landmarkPagesConfiguration.isTerminalPage(pageName) &&
	        !landmarkPagesConfiguration.isLandingPage(pageName) &&
	        !landmarkPagesConfiguration.isStartTimerPage(pageName) &&
	    	!landmarkPagesConfiguration.isRecommendationsPage(pageName) &&
	        applicationData.isSubmitted();  
    }
  
  private boolean isDocsUploadedandCompleted(@PathVariable String pageName) {
	LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
	        .getLandmarkPages();
	return  applicationData.getUploadedDocs().size() >0 &&
		    landmarkPagesConfiguration.isTerminalPage(pageName) &&
	        applicationData.isSubmitted();
	  }
  
  private boolean shouldRedirectToSubmissionConfirmation(@PathVariable String pageName) {
	LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
	        .getLandmarkPages();
	    
	return !landmarkPagesConfiguration.isPostSubmitPage(pageName) &&
	       !landmarkPagesConfiguration.isLandingPage(pageName) &&
	       !landmarkPagesConfiguration.isStartTimerPage(pageName) &&           
	       applicationData.isSubmitted();        
	  }

  private boolean shouldRedirectToProgramDocumentsPage(String pageName) {
	// a shortcut; if the application_id is null return false
	// maybe this shortcut should exist at a higher level but lets look at its affect by having it here
	if (applicationData.getId() == null) {
		return false;
	};
	
    LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
        .getLandmarkPages();
    // Documents have been submitted in non-later docs flow and applicant is attempting to navigate back to upload/submit docs pages
    return !landmarkPagesConfiguration.isLandingPage(pageName) &&
      	   !landmarkPagesConfiguration.isProgramDocumentsPage(pageName) &&
     	   !landmarkPagesConfiguration.isNextStepsPage(pageName) &&
    	   !landmarkPagesConfiguration.isTerminalPage(pageName) &&
    	   !landmarkPagesConfiguration.isFeedbackPage(pageName) &&
    	   !landmarkPagesConfiguration.isRecommendationsPage(pageName) &&
    	   !landmarkPagesConfiguration.isHealthcareRenewalLandingPage(pageName) &&
    	   !landmarkPagesConfiguration.isHealthcareRenewalTerminalPage(pageName) &&
           applicationData.getFlow() != LATER_DOCS
			&& hasSubmittedDocuments();
  }

  private boolean shouldRedirectToLaterDocsTerminalPage(String pageName) {
LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
    .getLandmarkPages();
// Documents have been submitted in later docs flow and applicant is attempting to navigate back to a previous page in this flow
boolean isNotLaterDocsTerminalPage =  !landmarkPagesConfiguration.isLaterDocsTerminalPage(pageName); //documentsSent is the laterDocs terminal page
boolean isLaterDocsPostSubmitExcludePage = landmarkPagesConfiguration.isLaterDocsPostSubmitExcludePage(pageName);
boolean isLaterDocsFlow = applicationData.getFlow() == LATER_DOCS;
boolean hasSubmittedDocuments = hasSubmittedDocuments();
return isNotLaterDocsTerminalPage && isLaterDocsPostSubmitExcludePage && isLaterDocsFlow && hasSubmittedDocuments ;
}
  
  private boolean shouldRedirectToHealthcareRenewalTerminalPage(String pageName) {
    LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
        .getLandmarkPages();
    // Documents have been submitted in later docs flow and applicant is attempting to navigate back to a previous page in this flow
    return !landmarkPagesConfiguration.isHealthcareRenewalTerminalPage(pageName)
        && landmarkPagesConfiguration.isPostSubmitPage(pageName)
        && applicationData.getFlow() == HEALTHCARE_RENEWAL
			&& hasSubmittedDocuments();
  }
  
  private boolean shouldRedirectToHealthcareRenewalLandingPage(String pageName) {
	    LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
	        .getLandmarkPages();
	    return landmarkPagesConfiguration.isHealthcareRenewalLandingPage(pageName)
	        && applicationData.getFlow() == HEALTHCARE_RENEWAL
				&& hasSubmittedDocuments();
	  }

  @PostMapping("/groups/{groupName}/delete")
  RedirectView deleteGroup(@PathVariable String groupName, HttpSession httpSession) {
    applicationData.getSubworkflows().remove(groupName);
    pageEventPublisher
        .publish(new SubworkflowIterationDeletedEvent(httpSession.getId(), groupName));
    String startPage = applicationConfiguration.getPageGroups().get(groupName).getRestartPage();
    return new RedirectView("/pages/" + startPage);
  }

  @PostMapping("/groups/{groupName}/{iteration}/delete")
  RedirectView deleteIteration(
      @PathVariable String groupName,
      @PathVariable int iteration,
      HttpSession httpSession
  ) {
    String nextPage;
    applicationData.getSubworkflows().get(groupName).remove(iteration);
    pageEventPublisher
        .publish(new SubworkflowIterationDeletedEvent(httpSession.getId(), groupName));

    if (applicationData.getSubworkflows().get(groupName).isEmpty()) {
      applicationData.getSubworkflows().remove(groupName);
      nextPage = applicationConfiguration.getPageGroups().get(groupName).getRestartPage();
    } else {
      nextPage = applicationConfiguration.getPageGroups().get(groupName).getReviewPage();
    }

    PageWorkflowConfiguration nextPageWorkflow = applicationConfiguration.getWorkflow()
        .get(nextPage);
    if (shouldSkip(nextPageWorkflow)) {
      return new RedirectView(String.format("/pages/%s/navigation", nextPage));
    } else {
      return new RedirectView(String.format("/pages/%s", nextPage));
    }
  }

  @PostMapping("/groups/{groupName}/{iteration}/deleteWarning")
  ModelAndView deleteIterationWarning(
      @PathVariable String groupName,
      @PathVariable int iteration
  ) {
    String deleteWarningPage = applicationConfiguration.getPageGroups().get(groupName)
        .getDeleteWarningPage();
    return new ModelAndView(
        "redirect:/pages/" + deleteWarningPage + "?iterationIndex=" + iteration);
  }

  @PostMapping("/pages/{pageName}")
  ModelAndView postFormPage(
      @RequestBody(required = false) MultiValueMap<String, String> model,
      @PathVariable String pageName,
      HttpSession httpSession
  ) {
    PageWorkflowConfiguration pageWorkflow = applicationConfiguration.getWorkflow().get(pageName);

    PageConfiguration page = pageWorkflow.getPageConfiguration();
    PageData pageData = PageData.fillOut(page, model);

    PagesData pagesData;
    Map<String, PagesData> incompleteIterations = applicationData.getIncompleteIterations();
    if (pageWorkflow.getGroupName() != null) {
      String groupName = pageWorkflow.getGroupName();
      if (isStartPageForGroup(page.getName(), groupName)) {
        incompleteIterations.putIfAbsent(groupName, new PagesData());
      }
      pagesData = incompleteIterations.get(groupName);
    } else {
      pagesData = applicationData.getPagesData();
    }

    pagesData.putPage(page.getName(), pageData);

    Boolean pageDataIsValid = pageData.isValid();
    if (pageDataIsValid &&
        pageWorkflow.getGroupName() != null &&
        applicationConfiguration.getPageGroups().get(pageWorkflow.getGroupName())
            .getCompletePages()
            .contains(page.getName())
    ) {
      String groupName = pageWorkflow.getGroupName();
      applicationData.getSubworkflows()
          .addIteration(groupName, incompleteIterations.remove(groupName));
      pageEventPublisher
          .publish(new SubworkflowCompletedEvent(httpSession.getId(), groupName));
    }

    if (pageDataIsValid) {
      if (applicationData.getId() == null) {
        applicationData.setId(applicationRepository.getNextId());
      }

      if (pageName != null && !pageName.isEmpty()) {
        applicationData.setLastPageViewed(pageName);
      }
      
      String id = applicationData.getId();
      MDC.put("applicationId", id);
      ofNullable(pageWorkflow.getEnrichment())
          .map(applicationEnrichment::getEnrichment)
          .map(enrichment -> enrichment.process(pagesData))
          .ifPresent(pageData::putAll);
      MDC.clear();

      Application application = applicationFactory.newApplication(applicationData);
      applicationRepository.save(application);
      return new ModelAndView(String.format("redirect:/pages/%s/navigation", pageName));
    } else {
      return new ModelAndView("redirect:/pages/" + pageName);
    }
  }

  @PostMapping("/submit")
  ModelAndView submitApplication(
      @RequestBody(required = false) MultiValueMap<String, String> model,
      HttpServletResponse httpResponse,
      HttpSession httpSession,
      @RequestAttribute("currentDevice") Device device
  ) {
    LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
        .getLandmarkPages();
    String submitPage = landmarkPagesConfiguration.getSubmitPage();
    PageConfiguration page = applicationConfiguration.getWorkflow().get(submitPage)
        .getPageConfiguration();

    PageData pageData = PageData.fillOut(page, model);
    PagesData pagesData = applicationData.getPagesData();
    pagesData.putPage(submitPage, pageData);

    if (pageData.isValid()) {
      if (applicationData.getId() == null) {
        // only happens in framework tests now we think, left in out of an abundance of caution
        log.error("Unexpected null applicationData ID on submit");
        applicationData.setId(applicationRepository.getNextId());
      }
      applicationData.setOriginalCounty(CountyParser.parse(applicationData).name());
      if (parseBoolean(getFirstValue(pagesData, USE_ENRICHED_HOME_COUNTY))) {
        applicationData.getPageData("identifyCounty").get("county").setValue(CountyParser.parseEnrich(applicationData).name(), 0);
      }
      Application application = applicationFactory.newApplication(applicationData);
      application.setCompletedAtTime(clock); // how we mark that the application is complete
      recordDeviceType(device, application);
      
      
      if(!applicationData.isSubmitted()) {
        applicationRepository.save(application);
        applicationStatusRepository.createOrUpdateApplicationType(application, SENDING);
        log.info(StringEscapeUtils.escapeJava("Invoking pageEventPublisher for application submission: " + application.getId()));
        pageEventPublisher.publish(
            new ApplicationSubmittedEvent(httpSession.getId(), application.getId(),
                application.getFlow(), LocaleContextHolder.getLocale())
        );
      }
      // Temporary cookie indicating user submitted an application
      Cookie submitCookie = new Cookie("application_id", StringEscapeUtils.escapeJava(application.getId()));
      submitCookie.setPath("/");
      submitCookie.setHttpOnly(true);
      submitCookie.setSecure(true);
      httpResponse.addCookie(submitCookie);

      applicationData.setSubmitted(true);
      return new ModelAndView(String.format("redirect:/pages/%s/navigation", submitPage));
    } else {
      return new ModelAndView("redirect:/pages/" + submitPage);
    }
  }

  private void recordDeviceType(Device device, Application application) {
  	String deviceType = "unknown";
	String platform = "unknown";

	if (device != null) {
		String isDesktop = device.getProperty("is_desktop");
		if (isDesktop != null && Boolean.parseBoolean(isDesktop)) {
			deviceType = "desktop";
		} else {
			String isTablet = device.getProperty("is_tablet");
			if (isTablet != null && Boolean.parseBoolean(isTablet)) {
				deviceType = "tablet";
			} else {
				String id = device.getId();
				if (id != null && !id.equalsIgnoreCase("unknown")) {
					deviceType = "mobile";
				}
			}
		}

		String devicePlatform = device.getProperty("device_os");
		if (devicePlatform != null) {
			platform = devicePlatform;
		}
	}

	application.getApplicationData().setDevicePlatform(platform);
	application.getApplicationData().setDeviceType(deviceType);
  }

  @PostMapping("/submit-feedback")
  RedirectView submitFeedback(Feedback feedback,
      RedirectAttributes redirectAttributes,
      Locale locale) {
	String terminalPage = applicationConfiguration.getLandmarkPages().getTerminalPage();
    if (applicationData.getId() == null) {
        return new RedirectView("/pages/" + terminalPage);
    }
    String message = messageSource.getMessage(feedback.getMessageKey(), null, locale);
    if (feedback.isInvalid()) {
      redirectAttributes.addFlashAttribute("feedbackFailure", message);
      return new RedirectView("/pages/" + terminalPage);
    }
    redirectAttributes.addFlashAttribute("feedbackSuccess", message);

    Application application = applicationRepository.find(applicationData.getId());
    if (application != null) {
      Application updatedApplication = application.addFeedback(feedback);
      applicationRepository.save(updatedApplication);
    }
    return new RedirectView("/pages/" + terminalPage);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @PostMapping("/document-upload")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
      @RequestParam("dataURL") String dataURL,
      @RequestParam("type") String type,
      Locale locale) throws IOException, InterruptedException {
    LocaleSpecificMessageSource lms = new LocaleSpecificMessageSource(locale, messageSource);
    try {
      if (applicationData.getUploadedDocs().size() <= MAX_FILES_UPLOADED &&
          file.getSize() <= uploadDocumentConfiguration.getMaxFilesizeInBytes()) {
        ResponseEntity<String> errorResponse = getErrorResponseForInvalidFile(file, type, lms);
        Double totalFileSize = applicationData.getUploadedDocs().stream().filter(i -> i.getSize()> 0).mapToDouble(i -> i.getSize()).sum() + file.getSize();
        if (totalFileSize >= TOTAL_MAX_FILE_SIZE) {
        	return new ResponseEntity<>(
		          lms.getMessage("upload-documents.maximum-total-file-size-error"),
		          HttpStatus.UNPROCESSABLE_ENTITY);
        }
        int maxFiles = (applicationData.getFlow() == FlowType.HEALTHCARE_RENEWAL)? 50: 20;
        if (applicationData.getUploadedDocs().size() + 1 > maxFiles) {
	    	return new ResponseEntity<>(
	  	          lms.getMessage("upload-documents.maximum-total-file-size-error"),
	  	          HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (errorResponse != null) {
          return errorResponse;
        }

        UUID uuid = UUID.randomUUID();
        var filePath = applicationData.getId() + "/" + uuid;
        var thumbnailFilePath = applicationData.getId() + "/thumbnail-" + uuid;

        if (file.getContentType() != null && file.getContentType().contains("image")) {
          Path paths = Files.createTempDirectory("");
          File thumbFile = new File(paths.toFile(),
              requireNonNull(requireNonNull(file.getOriginalFilename())));
          FileOutputStream fos = new FileOutputStream(thumbFile);
          fos.write(file.getBytes());
          fos.close();
          ByteArrayOutputStream os = new ByteArrayOutputStream();
          BufferedImage outputImage = Thumbnails.of(thumbFile).size(300, 300).asBufferedImage();
          ImageIO.write(outputImage, "png", os);
          dataURL = "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
          outputImage.flush();
          thumbFile.delete();
          Files.delete(paths);
          byte[] compressedImage = compressImage(file.getBytes(), file.getOriginalFilename());
          file = new CustomMultipartFile(compressedImage, file.getName(), file.getOriginalFilename(), file.getContentType());
        }
        documentRepository.upload(filePath, file);
        documentRepository.upload(thumbnailFilePath, dataURL);
        applicationData.addUploadedDoc(file, filePath, thumbnailFilePath, type);
      }

      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      log.error("Error Occurred while uploading File " + e.getLocalizedMessage());
      // If there's any uncaught exception, return a default error message
      return new ResponseEntity<>(
          lms.getMessage("upload-documents.there-was-an-issue-on-our-end"),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
  
  @Value("${image.quality}")
  private void setIsEnableEmailResubmissionTask(float imgQuality) {
	  imageQuality = imgQuality;
  }
  
  private byte[] compressImage(byte[] imageFileBytes, String filename) throws IOException {
	  var extension = Utils.getFileType(filename);
	  if(IMAGE_TYPES_TO_COMPRESS.contains(extension)) {
		  ByteArrayOutputStream outputFile = new ByteArrayOutputStream();
	      BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFileBytes));
	      JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
	        BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(),
	            BufferedImage.TYPE_3BYTE_BGR);
	            ColorConvertOp op = new ColorConvertOp(null);
	            op.filter(image, img);
	      jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	      jpegParams.setCompressionQuality(imageQuality);        
	      ImageWriter writer = getImageWriter();
	      try (final ImageOutputStream stream = ImageIO.createImageOutputStream(outputFile)) {
	        writer.setOutput(stream);
	        try {
	          writer.write(null, new IIOImage(img, null, null), jpegParams);
	        } finally {
	          writer.dispose();
	          stream.flush();
	        }
	      }
	      imageFileBytes = outputFile.toByteArray();
	      outputFile.close();
	  }
      return imageFileBytes;
  }
  
  private static ImageWriter getImageWriter() throws IOException {
	    IIORegistry registry = IIORegistry.getDefaultInstance();
	    Iterator<ImageWriterSpi> services = registry.getServiceProviders(ImageWriterSpi.class, (provider) -> {
	        if (provider instanceof ImageWriterSpi) {
	            return Arrays.stream(((ImageWriterSpi) provider).getFormatNames()).anyMatch(formatName -> formatName.equalsIgnoreCase("JPEG"));
	        }
	        return false;
	    }, true);
	    ImageWriterSpi writerSpi = services.next();
	    ImageWriter writer = writerSpi.createWriterInstance();
	    return writer;
	}

	private boolean hasSubmittedDocuments() {
    Application application;
    String id = applicationData.getId();

    if (id == null) {
      return false;
    }

    try {
      application = applicationRepository.find(id);
    } catch (Exception e) {
      log.warn(
          "An error finding application with id [" + id + "] failed. Message: " + e.getMessage());
      return false;
    }
    return application.getApplicationStatuses(UPLOADED_DOC).stream()
        .anyMatch(documentStatus -> List.of(SENDING, DELIVERED).contains(documentStatus));
  }

  @Nullable
  private ResponseEntity<String> getErrorResponseForInvalidFile(MultipartFile file, String type, LocaleSpecificMessageSource lms) throws IOException{
    log.info(StringEscapeUtils.escapeJava(type));
    if (file.getSize() == 0) {
      return new ResponseEntity<>(
          lms.getMessage("upload-documents.this-file-appears-to-be-empty"),
          HttpStatus.UNPROCESSABLE_ENTITY);
    }
    if (type.contains("officedocument") || type.contains("msword"))
    {
      // officedocument = docx
      // msword = doc
      return new ResponseEntity<>(
          lms.getMessage("upload-documents.MS-word-files-not-accepted"),
          HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
	if (Boolean.parseBoolean(clammitEnabled)) {
		try {
			var client = HttpClient.newHttpClient();
			var request = HttpRequest.newBuilder(URI.create(clammitUrl))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(BodyPublishers.ofByteArray(file.getBytes())).build();

			var response = client.send(request, BodyHandlers.ofString());
			if (VIRUS_STATUS_CODE.equalsIgnoreCase(Integer.toString(response.statusCode()))) {
				log.info(StringEscapeUtils.escapeJava("Virus detected in file " + file.getOriginalFilename() + ". File size: " + file.getSize() + " bytes."));
				return new ResponseEntity<>(lms.getMessage("upload-documents.virus-detected"),
						HttpStatus.UNPROCESSABLE_ENTITY);
			}

		} catch (java.net.ConnectException ce) {
			// Use this log info string to search in DataDog and create an alert for the
			// service being down.
			log.info("Clammit server exception connection error: " + ce.getLocalizedMessage());
			return new ResponseEntity<>(lms.getMessage("upload-documents.clammit-server-error"),
					HttpStatus.SERVICE_UNAVAILABLE);
		} catch (Exception e) {
			// Catch any other exceptions as a precaution
			log.info("Clammit server exception type: " + e.getClass().getName() + " message: " +   e.getLocalizedMessage());
			return new ResponseEntity<>(lms.getMessage("upload-documents.clammit-server-error"),
					HttpStatus.SERVICE_UNAVAILABLE);
		}

	}
    
    if (type.contains("pdf")) {
      // Return an error response if this is an pdf we can't work with
      try (var pdfFile = Loader.loadPDF(file.getBytes())) {
        var acroForm = pdfFile.getDocumentCatalog().getAcroForm();
        if (acroForm != null && acroForm.xfaIsDynamic()) {
          log.info("User has attempted to upload a dynamic XFA.");
          return new ResponseEntity<>(
              lms.getMessage("upload-documents.this-pdf-is-in-an-old-format"),
              HttpStatus.UNPROCESSABLE_ENTITY);
        }
      } catch (InvalidPasswordException e) {
        return new ResponseEntity<>(
            lms.getMessage("upload-documents.this-pdf-is-password-protected"),
            HttpStatus.UNPROCESSABLE_ENTITY);
      }
    }
    if (type.contains("image")) {
      PDDocument doc = new PDDocument();
      String imageFileName = file.getName();
      var imageFileBytes = file.getBytes();
      try {
        var image = PDImageXObject.createFromByteArray(doc, imageFileBytes, imageFileName);
      } catch (Exception e) {
        log.warn("Error uploading image: " + e.getMessage());
        return new ResponseEntity<>(
            lms.getMessage("upload-documents.there-is-a-problem-with-the-image"),
            HttpStatus.UNPROCESSABLE_ENTITY);
      }
    }
    return null;
  }

  @PostMapping("/submit-documents")
  ModelAndView submitDocuments(HttpSession httpSession) {
    Application application = applicationRepository.find(applicationData.getId());
    application.getApplicationData().setUploadedDocs(applicationData.getUploadedDocs());
    
    if (applicationData.getFlow() == LATER_DOCS || applicationData.getFlow() == HEALTHCARE_RENEWAL ) {
      application.setCompletedAtTime(clock);
    }
    applicationStatusRepository.getAndSetFileNames(application, UPLOADED_DOC);
    applicationRepository.save(application);
    if (Boolean.parseBoolean(filenetEnabled)) {
      log.info("Invoking pageEventPublisher for UPLOADED_DOC submission: " + application.getId());
      applicationStatusRepository.createOrUpdateAllForDocumentType(application, SENDING,
          UPLOADED_DOC);
      pageEventPublisher.publish(
          new UploadedDocumentsSubmittedEvent(httpSession.getId(), application.getId(),
              LocaleContextHolder.getLocale()));
    }
    LandmarkPagesConfiguration landmarkPagesConfiguration = applicationConfiguration
        .getLandmarkPages();
    String nextPage = landmarkPagesConfiguration.getProgramDocumentsPage();
    return new ModelAndView(String.format("redirect:/pages/%s", nextPage));
  }

  @SuppressWarnings("SpringMVCViewInspection")
  @PostMapping("/remove-upload/{filename}")
  ModelAndView removeUpload(@PathVariable String filename) {
    applicationData.getUploadedDocs().stream()
        .filter(uploadedDocument -> uploadedDocument.getFilename().equals(filename))
        .map(UploadedDocument::getS3Filepath)
        .findFirst()
        .ifPresent(documentRepository::delete);
    applicationData.removeUploadedDoc(filename);

    return new ModelAndView("redirect:/pages/uploadDocuments");
  }
  
  @SuppressWarnings("SpringMVCViewInspection")
  @PostMapping("/healthcare-renewal-remove-upload/{filename}")
  ModelAndView healthcareRenewalRemoveUpload(@PathVariable String filename) {
    applicationData.getUploadedDocs().stream()
        .filter(uploadedDocument -> uploadedDocument.getFilename().equals(filename))
        .map(UploadedDocument::getS3Filepath)
        .findFirst()
        .ifPresent(documentRepository::delete);
    applicationData.removeUploadedDoc(filename);

    return new ModelAndView("redirect:/pages/healthcareRenewalUploadDocuments");
  }

  /*
   * This method is used in the CCAP flow when the applicant is nudged to add
   * a child to application if they don't choose other household members.
   * This method sets the livesAlone input data as needed to maintain application logic.
   */
  @PostMapping("/pages/{pageName}/{option}")
  ModelAndView livesAlone(@PathVariable String pageName, @PathVariable String option) {
    String livesAlone = "true";
    if (option.equals("1")) {
      livesAlone = "false";
    }
    var pageData = applicationData.getPageData("addHouseholdMembers");

    if (pageData != null) {
      pageData.remove("addHouseholdMembers");
      pageData.put("addHouseholdMembers", new InputData(List.of(livesAlone)));
    }
    return new ModelAndView(
        String.format("redirect:/pages/%s/navigation?option=%s", pageName, option));
  }
  
  @ModelAttribute("sessionIDModelAttribute")
  public String sessionIDModelAttribute(final HttpServletRequest request) {
	  if (request.getSession() != null) {
		  return request.getSession().getId();
	  }
      return null;
  }
  
  @ModelAttribute("sessionExistsModelAttribute")
  public String sessionExistsModelAttribute(final HttpServletRequest request) {
	  if (request.getSession() != null) {
		  return "true";
	  }
	  else return "false";
  }
  
}
