package org.codeforamerica.shiba.output.caf;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.SOCIAL_SECURITY_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.SSI_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.VETERANS_BENEFITS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEMPLOYMENT_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.WORKERS_COMPENSATION_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.RETIREMENT_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.CHILD_OR_SPOUSAL_SUPPORT_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.TRIBAL_PAYMENTS_AMOUNT;

//import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.BENEFITS_PROGRAMS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.INSURANCE_PAYMENTS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.CONTRACT_FOR_DEED_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.TRUST_MONEY_AMOUNT;
//import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.HEALTHCARE_REIMBURSEMENT_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.INTEREST_DIVIDENDS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.RENTAL_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.OTHER_PAYMENTS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.ANNUITIES_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.DAY_TRADING_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.GIFTS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.LOTTERY_GAMBLING_AMOUNT;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_SOCIAL_SECURITY_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_SSI_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_VETERANS_BENEFITS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_UNEMPLOYMENT_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_WORKERS_COMPENSATION_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_RETIREMENT_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_CHILD_OR_SPOUSAL_SUPPORT_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_TRIBAL_PAYMENTS_AMOUNT;
//import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_BENEFITS_PROGRAMS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_INSURANCE_PAYMENTS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_CONTRACT_FOR_DEED_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_TRUST_MONEY_AMOUNT;
//import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_HEALTHCARE_REIMBURSEMENT_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_INTEREST_DIVIDENDS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_RENTAL_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_OTHER_PAYMENTS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_ANNUITIES_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_DAY_TRADING_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_GIFTS_AMOUNT;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.UNEARNED_LOTTERY_GAMBLING_AMOUNT;



import java.util.List;
import org.codeforamerica.shiba.Money;
import org.codeforamerica.shiba.application.parsers.ApplicationDataParser;
import org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.springframework.stereotype.Component;

@Component
public class UnearnedIncomeCalculator {

  public static final List<Field> UNEARNED_INCOME_FIELDS = List.of(
	  // unearnedIncomeSources page, applicant-only
      SOCIAL_SECURITY_AMOUNT,
      SSI_AMOUNT,
      VETERANS_BENEFITS_AMOUNT,
      UNEMPLOYMENT_AMOUNT,
      WORKERS_COMPENSATION_AMOUNT,
      RETIREMENT_AMOUNT,
      CHILD_OR_SPOUSAL_SUPPORT_AMOUNT,
      TRIBAL_PAYMENTS_AMOUNT,
      // otherUnearnedIncomeSources page, applicant-only
      //BENEFITS_PROGRAMS_AMOUNT,  Note:  Minnesota Benefits is not included in the income calculation
      INSURANCE_PAYMENTS_AMOUNT,
      CONTRACT_FOR_DEED_AMOUNT,
      TRUST_MONEY_AMOUNT,
      // HEALTHCARE_REIMBURSEMENT_AMOUNT,,  Note:  Health Care Reimbursements is not included in the income calculation
      INTEREST_DIVIDENDS_AMOUNT,
      RENTAL_AMOUNT,
      OTHER_PAYMENTS_AMOUNT, 
      ANNUITIES_AMOUNT,
      DAY_TRADING_AMOUNT,
      GIFTS_AMOUNT,
      LOTTERY_GAMBLING_AMOUNT,
      
      // from xxIncomeSource pages when applicant + household
      // associated with the unearnedIncome page
      UNEARNED_SOCIAL_SECURITY_AMOUNT,
      UNEARNED_SSI_AMOUNT,
      UNEARNED_VETERANS_BENEFITS_AMOUNT,
      UNEARNED_UNEMPLOYMENT_AMOUNT,
      UNEARNED_WORKERS_COMPENSATION_AMOUNT,
      UNEARNED_RETIREMENT_AMOUNT,
      UNEARNED_CHILD_OR_SPOUSAL_SUPPORT_AMOUNT,
      UNEARNED_TRIBAL_PAYMENTS_AMOUNT,
      
      // associated with the otherUnearnedIncome page
      // UNEARNED_BENEFITS_PROGRAMS_AMOUNT,  Note:  Minnesota Benefits is not included in the income calculation
      UNEARNED_INSURANCE_PAYMENTS_AMOUNT,
      UNEARNED_CONTRACT_FOR_DEED_AMOUNT,
      UNEARNED_TRUST_MONEY_AMOUNT,
      //UNEARNED_HEALTHCARE_REIMBURSEMENT_AMOUNT,  Note:  Health Care Reimbursements is not included in the income calculation
      UNEARNED_INTEREST_DIVIDENDS_AMOUNT,
      UNEARNED_RENTAL_AMOUNT,
      UNEARNED_OTHER_PAYMENTS_AMOUNT,
      UNEARNED_ANNUITIES_AMOUNT,
      UNEARNED_DAY_TRADING_AMOUNT,
      UNEARNED_GIFTS_AMOUNT,
      UNEARNED_LOTTERY_GAMBLING_AMOUNT
      
  );

  /**
   * This method calculates the unearned income amount for a MNbenefits application.
   * Because unearned income is collected differently when the application is for a single applicant versus applicant + household, 
   * this method will use all unearned income amounts, as defined in ApplicationDataParser.java, in its calculation.
   * @param applicationData
   * @return
   */
  public Money unearnedAmount(ApplicationData applicationData) {
    return UNEARNED_INCOME_FIELDS.stream().reduce(
        Money.ZERO,
        (total, unearnedIncomeField) -> total.add(
            getUnearnedIncome(unearnedIncomeField, applicationData)),
        Money::add
    );
  }

  private Money getUnearnedIncome(Field otherUnearnedIncomeField,
      ApplicationData applicationData) {
    List<String> householdAmounts = ApplicationDataParser.getValues(
        applicationData.getPagesData(),
        otherUnearnedIncomeField
    );
    return householdAmounts.stream().reduce(
        Money.ZERO,
        (total, individualAmount) -> total.add(Money.parse(individualAmount, "0.00")),
        Money::add
    );
  }
}
