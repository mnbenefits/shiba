package org.codeforamerica.shiba.pages.config;

import java.util.Collections;
import java.util.List;

import org.codeforamerica.shiba.inputconditions.Condition;

import lombok.Data;

@Data
public class FormInput{
  
  private FormInputType type;
  private String name;
  private String customInputFragment; // for type=CUSTOM
  private PromptMessage promptMessage;
  private String helpMessageKey;
  private String placeholder;
  private OptionsWithDataSource options; // for type=RADIO,CHECKBOX,SELECT
  private List<FormInput> followUps = Collections.emptyList();
  private List<String> followUpValues = Collections.emptyList();
  private List<Validator> validators = Collections.emptyList();
  private Boolean readOnly = false; // disables input
  private String defaultValue;
  private Condition condition;
  private List<PageDatasource> datasources; // for options
  private String customFollowUps;
  private String inputPostfix; // for text behind the input, name is from honeycrisp css class
  private String helpMessageKeyBelow;// help message appear below inputs
  private String noticeMessage;//for NOTICE input type
  private String cssClass;
  private Boolean validationIcon = true;
  private String noneCheckboxText;
  private String ariaDescribedbyInput;
  private String maxlength;
}
