package org.codeforamerica.shiba.inputconditions;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValueMatcherTest {

  @Test
  void doesNotEqualReturnsTrueWhenOneValueDoesNotEqualACompleteList(){
    ValueMatcher doesNotEqualMatcher = ValueMatcher.DOES_NOT_EQUAL;
    assertThat(doesNotEqualMatcher.matches(List.of("foo"), "foo")).isFalse();
    assertThat(doesNotEqualMatcher.matches(List.of("foo"), "bar")).isTrue();
    assertThat(doesNotEqualMatcher.matches(List.of("foo", "bar"), "foo")).isTrue();
  }
  
  @Test
  void doesNotEqualIgnoreCaseReturnsTrueWhenNoElementMatchesIgnoreCase() {
	  ValueMatcher doesNotEqualIgnoreCase = ValueMatcher.DOES_NOT_EQUAL_IGNORE_CASE;
	  assertThat(doesNotEqualIgnoreCase.matches(List.of ("MN"), "mn")).isFalse();
	  assertThat(doesNotEqualIgnoreCase.matches(List.of ("Mn"), "MN")).isFalse();
	  assertThat(doesNotEqualIgnoreCase.matches(List.of ("mN"), "mn")).isFalse();
	  assertThat(doesNotEqualIgnoreCase.matches(List.of ("mn"), "MN")).isFalse();
	  assertThat(doesNotEqualIgnoreCase.matches(List.of ("IA"), "mn")).isTrue();
	  assertThat(doesNotEqualIgnoreCase.matches(List.of ("Information"), "mn")).isTrue();
	  assertThat(doesNotEqualIgnoreCase.matches(List.of ("Information", "Iowa"), "mn")).isTrue();
	  assertThat(doesNotEqualIgnoreCase.matches(List.of (), "mn")).isTrue();
  }
  
  @Test
  void testMilleLacsRuralCounties() {
	  ValueMatcher valueMatcher = ValueMatcher.IS_MILLE_LACS_RURAL_COUNTY;
	  assertThat(valueMatcher.matches(List.of("Aitkin"), "Aitkin")).isTrue();
	  assertThat(valueMatcher.matches(List.of("Aitkin"), null)).isTrue(); // nulls work because the 2nd parameter is ignored in the enum
	  assertThat(valueMatcher.matches(List.of("Benton"), "Benton")).isTrue();
	  assertThat(valueMatcher.matches(List.of("Chisago"), "Chisago")).isTrue();
	  assertThat(valueMatcher.matches(List.of("CrowWing"), "Crow Wing")).isTrue();//space needs to be removed
	  assertThat(valueMatcher.matches(List.of("Kanabec"), "Kanabec")).isTrue();
	  assertThat(valueMatcher.matches(List.of("Morrison"), "Morrison")).isTrue();
	  assertThat(valueMatcher.matches(List.of("MilleLacs"), "Mille Lacs")).isTrue();
	  assertThat(valueMatcher.matches(List.of("Pine"), "Pine")).isTrue();
	  //anything else is false
	  assertThat(valueMatcher.matches(List.of("Anoka"), "Anoka")).isFalse();
	  assertThat(valueMatcher.matches(List.of("Hennepin"), "Hennepin")).isFalse();
	  assertThat(valueMatcher.matches(List.of("Hennepin"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("Hennepin"), "null")).isFalse();
	  assertThat(valueMatcher.matches(List.of("Lake of the Woods"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("foobar"), null)).isFalse();
	  //these two have spaces that need to be removed for the County enum
	  assertThat(valueMatcher.matches(List.of("Crow Wing"), "Crow Wing")).isFalse();
	  assertThat(valueMatcher.matches(List.of("Mille Lacs"), "Mille Lacs")).isFalse();
  }
  

  @Test 
  void testUrbanTribalNationCounties() {
	  ValueMatcher valueMatcher = ValueMatcher.IS_URBAN_TRIBAL_NATION_COUNTY;
	  assertThat(valueMatcher.matches(List.of("Hennepin"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("Anoka"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("Ramsey"), null)).isTrue();
	  //anything else is false
	  assertThat(valueMatcher.matches(List.of("Chisago"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("Kanabec"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("Aitkin"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("Yellow Medicine"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("foobar"), null)).isFalse();
  }
  
  @Test 
  void testIsMilleLacsServicedTribe() {
	  ValueMatcher valueMatcher = ValueMatcher.IS_URBAN_TRIBAL_NATION_MEMBER;
	  assertThat(valueMatcher.matches(List.of("Bois Forte"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("Grand Portage"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("Leech Lake"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("Mille Lacs Band of Ojibwe"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("White Earth Nation"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("Fond Du Lac"), null)).isTrue();
	  //anything else is false
	  assertThat(valueMatcher.matches(List.of("Shakopee Mdewakanton"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("Prairie Island"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("foobar"), null)).isFalse();
  }
  
  @Test 
  void testIsWhiteEarthServicedTribe() {
	  ValueMatcher valueMatcher = ValueMatcher.IS_WHITE_EARTH_COUNTY;
	  assertThat(valueMatcher.matches(List.of("Becker"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("Mahnomen"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("Clearwater"), null)).isTrue();
	  //anything else is false
	  assertThat(valueMatcher.matches(List.of("Aitkin"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("Yellow Medicine"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("foobar"), null)).isFalse();
  }
  
  @Test 
  void testDateOfBirthLessThan5() {
	  ValueMatcher valueMatcher = ValueMatcher.HH_MEMBER_AGE_LESS_THAN_5;
	  assertThat(valueMatcher.matches(List.of("02/02/2022","02/14/2023"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("01/01/2024","06/15/1955"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("05/05/2020"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("01/01/1999","12/12/1973"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("01/01/3333"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("05/08/1955"), null)).isFalse();
  }
  
  @Test 
  void testDateOfBirthLessThan18() {
	  ValueMatcher valueMatcher = ValueMatcher.HH_MEMBER_AGE_LESS_THAN_18;
	  assertThat(valueMatcher.matches(List.of("02/02/2012","02/14/2023"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("01/01/2007","06/15/1955"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("05/05/2000"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("01/01/1979","12/12/1973"), null)).isFalse();
	  assertThat(valueMatcher.matches(List.of("01/01/3333"), null)).isTrue();
	  assertThat(valueMatcher.matches(List.of("05/08/1955"), null)).isFalse();
  }
  
}