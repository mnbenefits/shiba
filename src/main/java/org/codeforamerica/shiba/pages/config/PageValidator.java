package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.util.List;

import org.codeforamerica.shiba.inputconditions.Condition;
import org.codeforamerica.shiba.output.LogicalOperator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * Validator that allows two inputs to be validated together.
 * Inputs are usually validated alone.
 * Work in Progress, will need to remove many of these once this is figured out.
 * @author pwemj35
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

}
