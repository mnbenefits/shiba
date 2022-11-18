package org.codeforamerica.shiba.pages.data;

import static java.util.stream.Collectors.toMap;
import static org.codeforamerica.shiba.pages.config.OptionsWithDataSourceTemplate.createOptionsWithDataSourceTemplate;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.codeforamerica.shiba.inputconditions.Condition;
import org.codeforamerica.shiba.pages.config.*;

/**
 * PagesData extends HashMap&lt;String, PageData&gt; 
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PagesData extends HashMap<String, PageData> {

  @Serial
  private static final long serialVersionUID = 5350174349257543992L;

  public PagesData() {
    super();
  }

  public PagesData(Map<String, PageData> map) {
    super(map);
  }

  public PageData getPage(String pageName) {
    return get(pageName);
  }

  public PageData getPageDataOrDefault(String pageName, PageConfiguration pageConfiguration) {
    PageData defaultPageData = PageData.initialize(pageConfiguration);

    return this.getOrDefault(pageName, defaultPageData);
  }

  public void putPage(String pageName, PageData pageData) {
    this.put(pageName, pageData);
  }

  /**
   * PagesData satisfies method checks if condition contains multiple conditions,
   * which then uses allMatch for AND logicalOperator, or anyMatch for OR logicalOperator.</br>
   * If there are no multiple conditions, it checks if the single condition matches the pageData.</br>
   * This method recursivly calls itself.
   * @param condition
   * @return Boolean
   */
  public boolean satisfies(Condition condition) {
    if (condition.getConditions() != null) {
      Stream<Condition> conditionStream = condition.getConditions().stream();
      return switch (condition.getLogicalOperator()) {
        case AND -> conditionStream.allMatch(this::satisfies);
        case OR -> conditionStream.anyMatch(this::satisfies);
      };
    }

    PageData pageData = get(condition.getPageName()); // this can't handle groups
    return condition.matches(pageData, this);
  }

  public DatasourcePages getDatasourcePagesBy(List<PageDatasource> datasources) {
    return new DatasourcePages(new PagesData(datasources.stream()
        .filter(datasource -> datasource.getPageName() != null)
        .map(datasource -> Map.entry(
            datasource.getPageName(),
            getOrDefault(datasource.getPageName(), new PageData())))
        .collect(toMap(Entry::getKey, Entry::getValue))));
  }

  public DatasourcePages getDatasourceGroupBy(List<PageDatasource> datasources,
      Subworkflows subworkflows) {
    Map<String, PageData> pages = new HashMap<>();
    datasources.stream()
        .filter(datasource -> datasource.getGroupName() != null && subworkflows
            .containsKey(datasource.getGroupName()))
        .forEach(datasource -> {
          PageData value = new PageData();
          subworkflows.get(datasource.getGroupName()).stream()
              .map(iteration -> iteration.getPagesData().getPage(datasource.getPageName()))
              .forEach(value::mergeInputDataValues);
          pages.put(datasource.getPageName(), value);
        });
    return new DatasourcePages(pages);
  }

  public List<String> safeGetPageInputValue(String pageName, String inputName) {
    return Optional.ofNullable(get(pageName))
        .map(pageData -> pageData.get(inputName))
        .map(InputData::getValue)
        .orElse(List.of());
  }

  /**
   * Get the first element in the inputData values for the given pageName and inputName or null if
   * it doesn't exist.
   *
   * @param pageName  page that the element is stored on
   * @param inputName input name of the element
   * @return first element stored at that pageName > inputName or null if it doesn't exist
   */
  public String getPageInputFirstValue(String pageName, String inputName) {
    PageData pageData = get(pageName);
    if (pageData != null) {
      InputData inputData = pageData.get(inputName);
      if (inputData != null && !inputData.getValue().isEmpty()) {
        return inputData.getValue(0);
      }
    }
    return null;
  }

  /**
   * Figure out text based on conditional values in the page workflow configuration
   * <p>
   * Defaults to {@code value.getDefaultValue()} if all {@code value.getConditionalValues()} and
   * flags evaluate to "false".
   */
  private String resolve(FeatureFlagConfiguration featureFlags,
      PageWorkflowConfiguration pageWorkflowConfiguration,
      Value value) {
    if (value == null) {
      return "";
    }
    return value.getConditionalValues().stream()
        .filter(conditionalValue -> {
          // Check flag
          String flag = conditionalValue.getFlag();
          if (flag != null && featureFlags.get(flag).isOff()) {
            return false;
          }

          // Check condition
          Condition condition = conditionalValue.getCondition();
          if (condition == null) {
            return true;
          }
          Objects.requireNonNull(pageWorkflowConfiguration.getDatasources(),
              "Configuration mismatch! Conditional value cannot be evaluated without a datasource.");
          DatasourcePages datasourcePages = this
              .getDatasourcePagesBy(pageWorkflowConfiguration.getDatasources());
          return datasourcePages.satisfies(condition);
        })
        .findFirst()
        .map(ConditionalValue::getValue)
        .orElse(value.getDefaultValue());
  }

  /**
   * Evaluate this PagesData object for display on the web page.
   * Inputs to be displayed are determined by any conditionals that are applied to them.
   * @param featureFlags
   * @param pageWorkflowConfiguration
   * @param applicationData
   * @return
   */
  public PageTemplate evaluate(FeatureFlagConfiguration featureFlags,
      PageWorkflowConfiguration pageWorkflowConfiguration, ApplicationData applicationData) {
    PageConfiguration pageConfiguration = pageWorkflowConfiguration.getPageConfiguration();
    DatasourcePages datasourcePages = this
        .getDatasourcePagesBy(pageWorkflowConfiguration.getDatasources());
    //System.out.println("[[[ PagesData evaluate pageName = " + pageWorkflowConfiguration.getNextPages());//TODO emj remove sysouts
    boolean hasPageValidation = pageConfiguration.isPageScopeValidation();
    //System.out.println("[[[ PagesData evaluate hasPageValidation = " + hasPageValidation);
    List<FormInputTemplate> inputs = null;
    if(hasPageValidation) {
    	//handle this using page validation
    	
    	PageValidator pageValidator = pageConfiguration.getPageValidator();
    	boolean isPageValid = pageValidator.isPageValid(applicationData.getPageData(pageConfiguration.getName()));
    	  // System.out.println("[[[ PagesData evaluate isPageValid = " + isPageValid);
        /*
         * A filter processes a list in some order to produce a new list containing exactly those 
         * elements of the original list for which a given predicate (think Boolean expression) returns true.
    		A map applies a given function to each element of a list, 
    		returning a list of results in the same order.
         */
        inputs = pageConfiguration.getInputs().stream() //list of FormInputs
        		//.peek(x -> System.out.println("BEFORE FILTER input=|" + x + "|"))
        		// filter inputs that satisfies their conditions ??? (not sure that is what is happening)
            .filter(input ->
            // map in filter -> get the input condition and map (apply) the DatasourcePages satisfies method to each
                Optional.ofNullable(input.getCondition()).map(datasourcePages::satisfies).orElse(true))
            //.peek(x -> System.out.println("AFTER FILTER input=|" + x + "|"))
            // apply the function to each formInput and return a FormInputTemplate, then collect to a list.
            .map(formInput -> convertFormInputToFormInputTemplate(pageConfiguration, formInput, applicationData))
            .collect(Collectors.toList());
    	
    }else {
    	
        /* Use the original validation for each input?
         * 
         * A filter processes a list in some order to produce a new list containing exactly those 
         * elements of the original list for which a given predicate (think Boolean expression) returns true.
    		A map applies a given function to each element of a list, 
    		returning a list of results in the same order.
         */
        inputs = pageConfiguration.getInputs().stream() //list of FormInputs
        		// filter inputs that satisfies their conditions ??? (not sure that is what is happening)
            .filter(input ->
            // map in filter -> get the input condition and map (apply) the DatasourcePages satisfies method to each
                Optional.ofNullable(input.getCondition()).map(datasourcePages::satisfies).orElse(true))
            // apply the function to each formInput and return a FormInputTemplate, then collect to a list.
            .map(formInput -> convertFormInputToFormInputTemplate(pageConfiguration, formInput, applicationData))
            .collect(Collectors.toList());
    	
    }
    

     
    // evaluate method param PageConfiguration has List<FormInput>, PageTemplate constructor requires List<FormInputTemplate>   
    return new PageTemplate(
        inputs,
        pageConfiguration.getName(),
        resolve(featureFlags, pageWorkflowConfiguration, pageConfiguration.getPageTitle()),
        resolve(featureFlags, pageWorkflowConfiguration, pageConfiguration.getHeaderKey()),
        resolve(featureFlags, pageWorkflowConfiguration,
            pageConfiguration.getHeaderHelpMessageKey()),
        pageConfiguration.getPrimaryButtonTextKey(),
        resolve(featureFlags, pageWorkflowConfiguration, pageConfiguration.getSubtleLinkTextKey()),
        pageWorkflowConfiguration.getSubtleLinkTargetPage(),
        resolve(featureFlags, pageWorkflowConfiguration, pageConfiguration.getCardFooterTextKey()),
        pageConfiguration.getHasPrimaryButton(),
        pageConfiguration.getContextFragment(),
        pageConfiguration.getAlertBox()
    );
  }
  
  /**
   * Modified convert method to look for a page validator.
   * TODO seems like this only adds the errorMessageKeys and does not do the actual validation.
   * @param pageConfiguration
   * @param formInput
   * @param applicationData
   * @return
   */
  private FormInputTemplate convertFormInputToFormInputTemplate(PageConfiguration pageConfiguration, FormInput formInput,
	      ApplicationData applicationData) {
	  String pageName = pageConfiguration.getName();//this is the page determined after validation
	  boolean pageHasValidation = false;
	    var pageValidator = pageConfiguration.getPageValidator();
	    List<String> errorMessageKeys = null;
	    if(pageValidator != null) {//TODO emj incorporate the page level validation  
	    	pageHasValidation = true;
	    	//System.out.println("$$$$ PagesData convertFormInputToFormInputTemplate, pageValidator is not null! $$$$"); TODO emj remove sysouts
	    	//System.out.println("pageValidator = " + pageValidator.toString());
	    	boolean isPageValid = pageValidator.isPageValid(getPage(pageName));
	    	formInput.setIsFormScopeValidation(true);
	    	//System.out.println("$$$$ PagesData convertFormInputToFormInputTemplate, pageValidation, isPageValid = " + isPageValid + " $$$$");
	    	if(!isPageValid) {
		    	String errorMessageKey = pageValidator.getErrorMessageKey();
		    	errorMessageKeys = Collections.singletonList(errorMessageKey) ;
	    	}
	    }else {
	    	//System.out.println("$$$$ PagesData convertFormInputToFormInputTemplate, normal validation");
	    errorMessageKeys = Optional.ofNullable(this.getPage(pageName))
	        .map(pageData -> pageData.get(formInput.getName()).errorMessageKeys(pageData))
	        .orElse(List.of());
	    }
	   // System.out.println("$$$$ PagesData convertFormInputToFormInputTemplate, errorMessageKeys = " + errorMessageKeys);
	    return new FormInputTemplate(
	        formInput.getType(),
	        formInput.getName(),
	        formInput.getCustomInputFragment(),
	        formInput.getPromptMessage(),
	        formInput.getHelpMessageKey(),
	        formInput.getPlaceholder(),
	        errorMessageKeys,
	        createOptionsWithDataSourceTemplate(formInput, applicationData),
	        formInput.getFollowUps().stream()
	            .map(followup -> convertFormInputToFormInputTemplate(pageConfiguration, followup, applicationData))
	            .collect(Collectors.toList()),
	        formInput.getFollowUpValues(),
	        formInput.getReadOnly(),
	        formInput.getDefaultValue(),
	        formInput.getDatasources(),
	        formInput.getCustomFollowUps(),
	        formInput.getInputPostfix(),
	        formInput.getHelpMessageKeyBelow(),
	        pageHasValidation //formInput.getIsFormScopeValidation()
	    );
	  }

  private FormInputTemplate convert(String pageName, FormInput formInput,
      ApplicationData applicationData) {
    List<String> errorMessageKeys = Optional.ofNullable(this.getPage(pageName))
        .map(pageData -> pageData.get(formInput.getName()).errorMessageKeys(pageData))
        .orElse(List.of());

    return new FormInputTemplate(
        formInput.getType(),
        formInput.getName(),
        formInput.getCustomInputFragment(),
        formInput.getPromptMessage(),
        formInput.getHelpMessageKey(),
        formInput.getPlaceholder(),
        errorMessageKeys,
        createOptionsWithDataSourceTemplate(formInput, applicationData),
        formInput.getFollowUps().stream()
            .map(followup -> convert(pageName, followup, applicationData))
            .collect(Collectors.toList()),
        formInput.getFollowUpValues(),
        formInput.getReadOnly(),
        formInput.getDefaultValue(),
        formInput.getDatasources(),
        formInput.getCustomFollowUps(),
        formInput.getInputPostfix(),
        formInput.getHelpMessageKeyBelow(),
        formInput.getIsFormScopeValidation()
    );
  }
}
