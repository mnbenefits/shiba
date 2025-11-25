package org.codeforamerica.shiba.output.caf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.codeforamerica.shiba.Money;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.Test;

class UnearnedIncomeCalculatorTest {

  UnearnedIncomeCalculator unearnedIncomeCalculator = new UnearnedIncomeCalculator();
  private final TestApplicationDataBuilder applicationDataBuilder = new TestApplicationDataBuilder();

  @Test
  void unearnedIncomeCafShouldCalculateTo360() {
    ApplicationData applicationData = applicationDataBuilder
        .withPageData("unearnedIncomeSources", "socialSecurityAmount", "140")
        .withPageData("unearnedIncomeSources", "supplementalSecurityIncomeAmount",
            List.of("49.50", "49.50"))
        .withPageData("unearnedIncomeSources", "veteransBenefitsAmount", "10")
        .withPageData("unearnedIncomeSources", "unemploymentAmount", List.of())
        .withPageData("unearnedIncomeSources", "workersCompensationAmount", "30")
        .withPageData("unearnedIncomeSources", "retirementAmount", List.of("40", "0"))
        .withPageData("unearnedIncomeSources", "childOrSpousalSupportAmount", List.of())
        .withPageData("unearnedIncomeSources", "tribalPaymentsAmount", List.of("41", ""))
        .build();
    Money totalUnearnedIncome = unearnedIncomeCalculator.unearnedAmount(applicationData);
    assertThat(totalUnearnedIncome).isEqualTo(Money.parse("360.00"));
  }

  @Test
  void unearnedIncomeCafShouldCalculateToZeroWhenFieldsAreBlank() {
    ApplicationData applicationData = applicationDataBuilder
        .withPageData("unearnedIncomeSources", "socialSecurityAmount", List.of())
        .withPageData("unearnedIncomeSources", "supplementalSecurityIncomeAmount", List.of())
        .withPageData("unearnedIncomeSources", "veteransBenefitsAmount", List.of())
        .withPageData("unearnedIncomeSources", "unemploymentAmount", List.of())
        .withPageData("unearnedIncomeSources", "workersCompensationAmount", List.of())
        .withPageData("unearnedIncomeSources", "retirementAmount", List.of())
        .withPageData("unearnedIncomeSources", "childOrSpousalSupportAmount", List.of())
        .withPageData("unearnedIncomeSources", "tribalPaymentsAmount", List.of())
        .build();
    Money totalUnearnedIncome = unearnedIncomeCalculator.unearnedAmount(applicationData);

    assertThat(totalUnearnedIncome).isEqualTo(Money.parse("0.00"));
  }

  @Test
  void unearnedIncomeCafShouldIgnoreNonNumberCharacters() {
    ApplicationData applicationData = applicationDataBuilder
        .withPageData("unearnedIncomeSources", "socialSecurityAmount", "138.10")
        .withPageData("unearnedIncomeSources", "supplementalSecurityIncomeAmount", "100.90")
        .withPageData("unearnedIncomeSources", "veteransBenefitsAmount", "1,010")
        .withPageData("unearnedIncomeSources", "unemploymentAmount", List.of())
        .withPageData("unearnedIncomeSources", "workersCompensationAmount", "30")
        .withPageData("unearnedIncomeSources", "retirementAmount", "40")
        .withPageData("unearnedIncomeSources", "childOrSpousalSupportAmount", List.of())
        .withPageData("unearnedIncomeSources", "tribalPaymentsAmount", "41")
        .build();
        Money totalUnearnedIncome = unearnedIncomeCalculator.unearnedAmount(applicationData);
        assertThat(totalUnearnedIncome).isEqualTo(Money.parse("1360.00"));
  }

  @Test
  void otherUnearnedIncomeShouldIgnoreBenefitsAndHealthcareInCalculation() {
	// Note: The benefits programs and health care reimbursement unearned income amounts are not included in the calculation.
    ApplicationData applicationData = applicationDataBuilder
        .withPageData("benefitsProgramsIncomeSource", "benefitsAmount", "139")
        .withPageData("insurancePaymentsIncomeSource", "insurancePaymentsAmount", "100")
        .withPageData("contractForDeedIncomeSource", "contractForDeedAmount", "10")
        .withPageData("trustMoneyIncomeSource", "trustMoneyAmount", "80")
        .withPageData("healthcareReimbursementIncomeSource",
            "healthCareReimbursementAmount", "30")
        .withPageData("interestDividendsIncomeSource", "interestDividendsAmount", "40")
        .withPageData("rentalIncomeSource", "rentalIncomeAmount", "1")
        .withPageData("otherPaymentsIncomeSource", "otherPaymentsAmount", "20")
        .build();
    Money totalUnearnedIncome = unearnedIncomeCalculator.unearnedAmount(applicationData);

    // x + 100 + 10 + 80 + x + 40 + 1 + 20 = 251 (where x means amount is not used)
    assertThat(totalUnearnedIncome).isEqualTo(Money.parse("251"));
  }

  @Test
  void otherUnearnedIncomeShouldCalculateToZeroWhenFieldsAreBlank() {
    ApplicationData applicationData = applicationDataBuilder
        .withPageData("benefitsProgramsIncomeSource", "benefitsAmount", List.of())
        .withPageData("insurancePaymentsIncomeSource", "insurancePaymentsAmount", List.of())
        .withPageData("contractForDeedIncomeSource", "contractForDeedAmount", List.of())
        .withPageData("trustMoneyIncomeSource", "trustMoneyAmount", List.of())
        .withPageData("healthcareReimbursementIncomeSource",
            "healthCareReimbursementAmount", List.of())
        .withPageData("interestDividendsIncomeSource", "interestDividendsAmount", List.of())
        .withPageData("rentalIncomeSource", "rentalIncomeAmount", List.of())
        .withPageData("otherPaymentsIncomeSource", "otherPaymentsAmount", List.of())
        .build();
    Money totalUnearnedIncome = unearnedIncomeCalculator.unearnedAmount(applicationData);

    assertThat(totalUnearnedIncome).isEqualTo(Money.parse("0"));
  }

  @Test
  void otherUnearnedIncomeShouldIgnoreNonNumberCharacters() {
	// Note: The benefits programs and health care reimbursement unearned income amounts are not included in the calculation.
    ApplicationData applicationData = applicationDataBuilder
        .withPageData("benefitsProgramsIncomeSource", "benefitsAmount", "138.10")
        .withPageData("insurancePaymentsIncomeSource", "insurancePaymentsAmount", "100.90")
        .withPageData("contractForDeedIncomeSource", "contractForDeedAmount", "1,010")
        .withPageData("trustMoneyIncomeSource", "trustMoneyAmount", List.of())
        .withPageData("healthcareReimbursementIncomeSource", "healthCareReimbursementAmount",
            "30")
        .withPageData("interestDividendsIncomeSource", "interestDividendsAmount", "40")
        .withPageData("rentalIncomeSource", "rentalIncomeAmount", List.of())
        .withPageData("otherPaymentsIncomeSource", "otherPaymentsAmount", List.of())
        .build();
    Money totalUnearnedIncome = unearnedIncomeCalculator.unearnedAmount(applicationData);

    // x + 100.90 + 1010  + 0 + x + 40 + 0 + 0 = 1150.90 (where x means amount is not used)
    assertThat(totalUnearnedIncome).isEqualTo(Money.parse("1150.90"));
  }

  /**
   * Perform and unearned income calculation for an applicant-only application
   * where all unearned income options are selected and an amount is provided for each option.
   */
  @Test
  void otherUnearnedIncomeAllOptionsSelectedApplicantOnly() {
	// Note: The benefits programs and health care reimbursement unearned income amounts are not included in the calculation.
    ApplicationData applicationData = applicationDataBuilder
         .withPageData("unearnedIncomeSources", "socialSecurityAmount", "101")
         .withPageData("unearnedIncomeSources", "supplementalSecurityIncomeAmount", "202")
         .withPageData("unearnedIncomeSources", "veteransBenefitsAmount", "303")
         .withPageData("unearnedIncomeSources", "unemploymentAmount", "404")
         .withPageData("unearnedIncomeSources", "workersCompensationAmount", "505")
         .withPageData("unearnedIncomeSources", "retirementAmount", "606")
         .withPageData("unearnedIncomeSources", "childOrSpousalSupportAmount", "707")
         .withPageData("unearnedIncomeSources", "tribalPaymentsAmount", "808")
         .withPageData("otherUnearnedIncomeSources", "insurancePaymentsAmount", "909")
         .withPageData("otherUnearnedIncomeSources", "trustMoneyAmount", "1010")
         .withPageData("otherUnearnedIncomeSources", "rentalIncomeAmount", "1111")
         .withPageData("otherUnearnedIncomeSources", "interestDividendsAmount", "1212")
         .withPageData("otherUnearnedIncomeSources", "healthCareReimbursementAmount", "1313")
         .withPageData("otherUnearnedIncomeSources", "contractForDeedAmount", "1414")
         .withPageData("otherUnearnedIncomeSources", "benefitsAmount", "1515")
         .withPageData("otherUnearnedIncomeSources", "annuityPaymentsAmount", "1616")
         .withPageData("otherUnearnedIncomeSources", "giftsAmount", "1717")
         .withPageData("otherUnearnedIncomeSources", "lotteryGamblingAmount", "1818")
         .withPageData("otherUnearnedIncomeSources", "dayTradingProceedsAmount", "1919")
         .withPageData("otherUnearnedIncomeSources", "otherPaymentsAmount", "2020")
         .build();
         Money totalUnearnedIncome = unearnedIncomeCalculator.unearnedAmount(applicationData);
         
         // 101+202+303+404+505+606707+808+909+1010+1111+1212+x+1414+x+1616+1717+1818
         //    +1919+2020 = 18382 (where x means amount is not used)
         assertThat(totalUnearnedIncome).isEqualTo(Money.parse("18382"));
  }
  
  /**
   * Perform and unearned income calculation for a household where all unearned income options are selected
   * and unearned income amounts are provide for 2 of the 3 people on the application.
   */
  @Test
  void otherUnearnedIncomeAllOptionsSelectedHousehold() {
	// Note: The benefits programs and health care reimbursement unearned income amounts are not included in the calculation.
    ApplicationData applicationData = applicationDataBuilder
        .withPageData("socialSecurityIncomeSource", "socialSecurityAmount", List.of("100", "", "101"))
        .withPageData("supplementalSecurityIncomeSource", "supplementalSecurityIncomeAmount", List.of("200", "202", ""))
        .withPageData("veteransBenefitsIncomeSource", "veteransBenefitsAmount", List.of("", "300", "303"))
        .withPageData("unemploymentIncomeSource", "unemploymentAmount", List.of("400", "", "404"))
        .withPageData("workersCompIncomeSource", "workersCompensationAmount", List.of("500", "505", ""))
        .withPageData("retirementIncomeSource", "retirementAmount", List.of("", "600", "606"))
        .withPageData("childOrSpousalSupportIncomeSource", "childOrSpousalSupportAmount", List.of("700", "", "707"))
        .withPageData("tribalPaymentIncomeSource", "tribalPaymentsAmount", List.of("800", "808", ""))
        .withPageData("insurancePaymentsIncomeSource", "insurancePaymentsAmount", List.of("", "900", "909"))
        .withPageData("trustMoneyIncomeSource", "trustMoneyAmount", List.of("1000", "", "1010"))
        .withPageData("rentalIncomeSource", "rentalIncomeAmount", List.of("1100", "1111", ""))
        .withPageData("interestDividendsIncomeSource", "interestDividendsAmount", List.of("", "1200", "1212"))
        .withPageData("healthcareReimbursementIncomeSource", "healthCareReimbursementAmount", List.of("1300", "", "1313"))
        .withPageData("contractForDeedIncomeSource", "contractForDeedAmount", List.of("1400", "1414", ""))
        .withPageData("benefitsProgramsIncomeSource", "benefitsAmount", List.of("", "1500", "1515"))
        .withPageData("annuityIncomeSource", "annuityPaymentsAmount", List.of("1600", "", "1616"))
        .withPageData("giftsIncomeSource", "giftsAmount", List.of("1700", "1717", ""))
        .withPageData("lotteryIncomeSource", "lotteryGamblingAmount", List.of("", "1800", "1818"))
        .withPageData("dayTradingIncomeSource", "dayTradingProceedsAmount", List.of("1900", "", "1919"))
        .withPageData("otherPaymentsIncomeSource", "otherPaymentsAmount", List.of("2000", "2020", ""))
        .build();
    Money totalUnearnedIncome = unearnedIncomeCalculator.unearnedAmount(applicationData);

    // 100+101+200+202+300+303+400+404+500+505+600+606+700+707+800+808+900+909+1000+1010+1100+1111+1200+1212+
    //    x+1400+1414+x+1600+1616+1700+1717+1800+1818+1900+1919+2000+2020 = (where x means amount is not used)
    assertThat(totalUnearnedIncome).isEqualTo(Money.parse("36582"));
  }
  
}
