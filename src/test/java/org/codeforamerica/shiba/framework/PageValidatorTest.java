package org.codeforamerica.shiba.framework;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.codeforamerica.shiba.inputconditions.Condition;
import org.codeforamerica.shiba.inputconditions.ValueMatcher;
import org.codeforamerica.shiba.output.LogicalOperator;
import org.codeforamerica.shiba.pages.config.PageValidator;
import org.codeforamerica.shiba.pages.config.Validator;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.junit.jupiter.api.Test;

public class PageValidatorTest{

@Test	
public void testPageValidatorTwoValidInputsLogicalOperatorOR() {
	PageValidator pageValidator = new PageValidator();
	LogicalOperator logicalOperator = LogicalOperator.OR;
	pageValidator.setLogicalOperator(logicalOperator);
	pageValidator.setInputsToValidate(Arrays.asList("foo", "bar") );
	
	//Create inputs to validate
	Validator validator1 = new Validator();
	Condition condition1 = new Condition();
	condition1.setMatcher(ValueMatcher.NOT_EMPTY);
	condition1.setValue("value1");
	
	validator1.setCondition(condition1);//adding this condition makes it fail now, TODO need to populate this condition so it works
	List<Validator> validatorList1 = Arrays.asList(validator1);
	Validator validator2 = new Validator();
	List<Validator> validatorList2 = Arrays.asList(validator2);
	
	List<String> values1 = Arrays.asList("value1");
	List<String> values2 = Arrays.asList("value2");
	
    InputData inputData1 = new InputData(values1, validatorList1, null);
    //inputData1.setValidators(validatorList1); 
    InputData inputData2 = new InputData(values2, validatorList2, null);
	PageData pageData = new PageData();
	pageData.put("foo", inputData1);
	pageData.put("bar", inputData2);
	

	
	boolean isValid = pageValidator.isPageValid(pageData);
	assertTrue(isValid);
	
}

@Test	
public void testPageValidatorTwoInputsOneInvalidLogicalOperatorOR() {
	
}

@Test	
public void testPageValidatorTwoInputsBothValidLogicalOperatorAND() {
	
}

@Test	
public void testPageValidatorTwoInputsOneInvalidLogicalOperatorAND() {
	
}
	  
}
