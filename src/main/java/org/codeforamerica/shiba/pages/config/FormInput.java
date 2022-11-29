package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import org.codeforamerica.shiba.inputconditions.Condition;

@Data
public class FormInput implements Serializable {

  @Serial
  private static final long serialVersionUID = 5831139204290273630L;
  
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
  private Boolean isFormScopeValidation = false;//TODO emj testing if this will allow the html fragment to have a form error message rather than individual input error messages.
  private String noticeMessage;//for NOTICE input type
  private Boolean validationIcon = true;
}
