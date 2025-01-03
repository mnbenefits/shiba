package org.codeforamerica.shiba.pages.config;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.validator.GenericValidator;

import org.codeforamerica.shiba.County;
import org.codeforamerica.shiba.TribalNation;

/* Validation on an input field */
public enum Validation {
  NONE(strings -> true),
  SHOULD_BE_BLANK(strings -> String.join("", strings).isBlank()),
  NOT_BLANK(strings -> !String.join("", strings).isBlank()),
  NONE_BLANK(strings -> strings.stream().noneMatch(String::isBlank)),
  SELECT_AT_LEAST_ONE(strings -> strings.size() > 0),
  SELECTED(strings -> strings.size() == 0),
  SSN(strings -> String.join("", strings).replace("-", "").matches("\\d{9}")),
  DATE(strings -> {
    return String.join("", strings).matches("^[0-9]*$") &&
        (GenericValidator.isDate(String.join("/", strings), "MM/dd/yyyy", true)
            || GenericValidator.isDate(String.join("/", strings), "M/dd/yyyy", true)
            || GenericValidator.isDate(String.join("/", strings), "M/d/yyyy", true)
            || GenericValidator.isDate(String.join("/", strings), "MM/d/yyyy", true));
  }),
  MULTIPLE_DATES(strings -> {return validateMultipleDates(strings); }),
  DOB_VALID(strings -> {
    String dobString = String.join("/", strings);
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    try {
      Integer inputYear = Integer.parseInt(strings.get(2));
      Date dobDate = sdf.parse(dobString);
      boolean notFutureDate = dobDate.getTime() < new Date().getTime();
      boolean notBefore1900 = inputYear >= 1900;
      return notFutureDate && notBefore1900;
    } catch (NumberFormatException e) {
      return false;
    } catch (ParseException e) {
      return false;
    } catch (IndexOutOfBoundsException e) {
      return false;
    }
  }),
  ZIPCODE(strings -> String.join("", strings).matches("\\d{5}")),
  CASE_NUMBER(strings -> String.join("", strings).matches("\\d{4,7}")),
  CASE_NUMBER_HC(strings -> String.join("", strings).matches("\\d{4,8}")),
  COUNTY(strings -> EnumUtils.isValidEnumIgnoreCase(County.class, strings.get(0).replaceAll(" ",""))),
  TRIBAL_NATION(strings -> EnumUtils.isValidEnumIgnoreCase(TribalNation.class, strings.get(0).replaceAll(" ",""))),
  STATE(strings -> Set
      .of("AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA",
          "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
          "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT",
          "VA", "WA", "WV", "WI", "WY", "AS", "DC", "FM", "GU", "MH", "MP", "PR", "VI", "AB", "BC",
          "MB", "NB", "NF", "NS", "ON", "PE", "PQ", "SK")
      .contains(strings.get(0).toUpperCase())),
  PHONE(strings -> String.join("", strings).replaceAll("[^\\d]", "").matches("\\d{10}")),
  PHONE_STARTS_WITH_ONE(
      strings -> !String.join("", strings).replaceAll("[^\\d]", "").startsWith("1")),
  PHONE_STARTS_WITH_ZERO(
	      strings -> !String.join("", strings).replaceAll("[^\\d]", "").startsWith("0")),
  MONEY(strings -> String.join("", strings)
      .matches("^(\\d{1,3},(\\d{3},)*\\d{3}|\\d+)(\\.\\d{1,2})?")),
  NUMBER(strings -> strings.get(0).trim().matches("\\d*")),
  EMAIL(strings -> String.join("", strings).trim().matches(
      "[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?")),
  //Email con will check the that the final string of characters does contain con
  EMAIL_DOES_NOT_END_WITH_CON(strings -> !String.join("", strings).endsWith(".con"));

  private final Predicate<List<String>> rule;

  Validation(Predicate<List<String>> rule) {
    this.rule = rule;
  }

	public Boolean apply(List<String> value) {
		return this.rule.test(value);
	}


	public static boolean validateMultipleDates(List<String> strings) {
		return strings.stream()
				.map(s -> {return partitionDateList(strings);})
				.allMatch(list -> {	return areAllDatesValid(list);});
	}
	
	/**
	 * Separate list of Strings into a collection of Lists of three Strings.
	 * @param <T>
	 * @param inputList
	 * @return
	 */
	private static <T> Collection<List<T>> partitionDateList(List<T> inputList) {
		final AtomicInteger counter = new AtomicInteger(0);
		return inputList.stream().collect(Collectors.groupingBy(s -> counter.getAndIncrement() / 3)).values();
	}
	
	private static boolean areAllDatesValid(Collection<List<String>> stringList) {
		Iterator<List<String>> iterator = stringList.iterator();
		boolean retVal = false;
		while(iterator.hasNext()) {
			List<String> date = iterator.next();
			boolean isEmpty = date.stream().allMatch(string -> string.isEmpty());
			if(isEmpty) {
				retVal = true;
				continue;
			}
			
			if (DATE.apply(date) == false) {
				return false;
			}else {
				retVal = true;
			}
		}
		return retVal;
	}

}
