package org.codeforamerica.shiba.inputconditions;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.output.LogicalOperator;
import org.codeforamerica.shiba.pages.data.InputData;
import org.codeforamerica.shiba.pages.data.PageData;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Condition implements Serializable {

  @Serial
  private static final long serialVersionUID = -7300484979833484734L;
  private static final List<String> K_GRADES = List.of("hd strt", "pre-k", "k");
  String pageName;
  String name;
  String input;
  String value;
  String customCondition;
  @JsonIgnore
  ValueMatcher matcher = ValueMatcher.CONTAINS;
  @JsonIgnore
  private List<Condition> conditions;
  @JsonIgnore
  private LogicalOperator logicalOperator = LogicalOperator.AND;

  public Condition(List<Condition> conditions, LogicalOperator logicalOperator) {
    this.conditions = conditions;
    this.logicalOperator = logicalOperator;
  }

  public Condition(String pageName, String input, String value, ValueMatcher matcher) {
    this.pageName = pageName;
    this.input = input;
    this.value = value;
    this.matcher = matcher;
  }

  // Constructor for a "customCondition"
  // Takes one parameter, a string that identifies the custome condition.
  public Condition(String customCondition) {
    this.customCondition = customCondition;
  }

  public boolean matches(PageData pageData, Map<String, PageData> pagesData) {
    if (pageName != null) {
      return satisfies(pagesData.get(pageName));
    } else {
      return satisfies(pageData);
    }
  }

  public boolean satisfies(PageData pageData) {
    return pageData != null && !pageData.isEmpty() && matcher
        .matches(pageData.get(input).getValue(), value);
  }
  
  public boolean satisfies(String input) {
	    return input != null && !input.isEmpty() && matcher
	        .matches(List.of(input), value);
	  }
 
  /**
   * This method evaluates the SKIP_SCHOOL_DETAILS custom condition as specified in pages-config.yaml. 
   * @param childrenInNeedOfCarePage  
   * @param whoIsGoingToSchoolPage
   * @return 
   */
	public boolean satisfiesSkipSchoolDetailsCondition(PageData childrenInNeedOfCarePage,
			PageData whoIsGoingToSchoolPage) {
		if (childrenInNeedOfCarePage == null || whoIsGoingToSchoolPage == null) {
			return true;
		}
		// Retrieve the list of children in need of care and those going to school from
		// PageData
		Collection<InputData> childrenInNeedOfCare = childrenInNeedOfCarePage.values();
		Collection<InputData> childrenGoingToSchool = whoIsGoingToSchoolPage.values();
		if (childrenInNeedOfCare.isEmpty() || childrenGoingToSchool.isEmpty()) {
			return true;
		}
		// Check for any common elements between the two lists
		for (InputData child : childrenInNeedOfCare) {
			List<String> childrenInNeedOfCareNames = child.getValue();
			for (String name : childrenInNeedOfCareNames) {
				for (InputData schoolChild : childrenGoingToSchool) {
					List<String> childrenGoingToSchoolNames = schoolChild.getValue();
					if (childrenGoingToSchoolNames.contains(name)) {
						return false;
					}
				}

			}

		}
		return true;
	}
	 
  /**
   * This method evaluates the SKIP_SCHOOL_START_DATE custom condition.
   * @param schoolGradePageData - this is the PageData object for the schoolGrade page  
   * @return 
  */
	public boolean satisfiesSkipSchoolStartDateCondition(PageData schoolGradePageData) {
		if (schoolGradePageData == null) {
			return true;
		}
		// Retrieve the list of grades from the schoolGradePage PageData
		InputData schoolGradeInput = schoolGradePageData.get("schoolGrade");
		if (schoolGradeInput == null) {
			return true;
		}
		List<String> grades = schoolGradeInput.getValue();
		// Is there at least one grade from the list Head Start, Pre-K or Kindergarten?
		for (String grade : grades) {
			if (K_GRADES.contains(grade.toLowerCase())) {
				return false;
			}
		}
		return true;
	}

  @SuppressWarnings("unused")
  public void setConditions(List<Condition> conditions) {
    assertCompositeCondition();
    this.conditions = conditions;
  }

  @SuppressWarnings("unused")
  public void setLogicalOperator(LogicalOperator logicalOperator) {
    assertCompositeCondition();
    this.logicalOperator = logicalOperator;
  }

  public void setPageName(String pageName) {
    assertNotCompositeCondition();
    this.pageName = pageName;
  }

  public void setInput(String input) {
    assertNotCompositeCondition();
    this.input = input;
  }

  public void setValue(String value) {
    assertNotCompositeCondition();
    this.value = value;
  }

  public void setCustomConditionName(String customCondition) {
	    assertNotCompositeCondition();
	    this.customCondition = customCondition;
  }
  
  private void assertCompositeCondition() {
    if (pageName != null || input != null) {
      throw new IllegalStateException("Cannot set composite condition fields");
    }
  }

  private void assertNotCompositeCondition() {
    if (conditions != null) {
      throw new IllegalStateException("Cannot set noncomposite condition fields");
    }
  }
}
