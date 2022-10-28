package org.codeforamerica.shiba.pages.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import org.codeforamerica.shiba.pages.config.Validator;
import org.jetbrains.annotations.NotNull;

@Value
public class InputData implements Serializable {

  @Serial
  private static final long serialVersionUID = 8511070147741948268L;

  @NotNull List<String> value;
  @NotNull
  @JsonIgnore
  List<Validator> validators;
  @NonFinal
  @NotNull 
  String errorKey;

  InputData(List<String> value, @NotNull List<Validator> validators, String errorKey) {
    this.value = Objects.requireNonNullElseGet(value, List::of);
    this.validators = Objects.requireNonNullElseGet(validators, List::of);
    this.errorKey = errorKey;
  }

  InputData() {
    this(new ArrayList<>(), new ArrayList<>(), new String());
  }

  public InputData(@NotNull List<String> value) {
    this(value, new ArrayList<>(), new String());
  }

  public Boolean valid(PageData pageData) {
    return validators.stream().filter(
            validator -> validator.getCondition() == null || validator.getCondition()
                .satisfies(pageData)).map(Validator::getValidation)
        .allMatch(validation -> validation.apply(value));
  }

  public List<String> errorMessageKeys(PageData pageData) {
	  System.out.println("*** InputData errorMessageKeys START ***");
	  if(errorKey != null) {
		  System.out.println("*** returning single errorKey: " + errorKey);
		  System.out.println("*** InputData errorMessageKeys END***");
		  return Collections.singletonList(errorKey);
	  }
	  List<String> errorList = validators.stream()
		        .filter(validator ->
	            (validator.getCondition() == null || validator.getCondition().satisfies(pageData))
	            && !validator.getValidation().apply(value))
	        .map(Validator::getErrorMessageKey).collect(Collectors.toList());
	  System.out.println("*** returning list of errorKeys: " + errorList.toString());
	  System.out.println("*** InputData errorMessageKeys END***");
    return errorList;
  }

  public String getValue(int i) {
    return this.getValue().get(i);
  }

  public void setValue(String newValue, int i) {
    this.value.set(i, newValue);
  }
  
  public void setErrorKey(String key) {
	  this.errorKey = key;
  }
}
