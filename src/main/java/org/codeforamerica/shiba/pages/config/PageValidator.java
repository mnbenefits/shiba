package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.codeforamerica.shiba.inputconditions.Condition;
import org.codeforamerica.shiba.output.LogicalOperator;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.PagesData;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * Validator that allows two inputs to be validated together. Inputs are usually
 * validated alone. Work in Progress, will need to remove many of these once
 * this is figured out.
 * 
 * TODO emj create test class for this
 *
 */
@Data
public class PageValidator {
	@Serial
	private static final long serialVersionUID = -644544878960451235L;
	private String name;
	@JsonIgnore
	private LogicalOperator logicalOperator = LogicalOperator.AND;
	private List<FormInputTemplate> inputs;
	private List<String> inputsToValidate;
	private Validation validation = Validation.NONE;
	private String errorMessageKey;
	private List<Condition> conditions;

//TODO emj create test for this
	public boolean isPageValid(PageData pageData) {
		List<Boolean> booleanArray = new ArrayList<Boolean>();
//		System.out.println("====== PageValidator isPageValid =========");
//		System.out.println(" --- logicalOperator : " + logicalOperator);

		if(pageData == null) {
			return true;
		}
		boolean retval = false;
		for (String input : inputsToValidate) {

			InputData inputData = pageData.get(input);
			Boolean isInputDataValid = inputData.valid(pageData);
			booleanArray.add(isInputDataValid);
	//		System.out.println("  --input: " + input + " isInputDataValid? " + isInputDataValid);
			if(!isInputDataValid) {
				inputData.setErrorKey(errorMessageKey);
		    }
		}
		
		if(logicalOperator.equals(LogicalOperator.AND)) {
			retval = booleanArray.stream().allMatch(value -> {return value.equals(Boolean.TRUE);});
		//	System.out.println("   === PageValidator isPageValid AND " + retval);
			
		}
		
		if(logicalOperator.equals(LogicalOperator.OR)) {
			retval = booleanArray.stream().anyMatch(value -> {return value.equals(Boolean.TRUE);});
	//		System.out.println("   === PageValidator isPageValid OR " + retval);
		}
	//	System.out.println("====== PageValidator isPageValid returning " + retval);

		return retval;
	}

}
