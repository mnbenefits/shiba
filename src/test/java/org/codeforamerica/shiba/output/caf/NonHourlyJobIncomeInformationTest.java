package org.codeforamerica.shiba.output.caf;

import static org.assertj.core.api.Assertions.assertThat;

import org.codeforamerica.shiba.Money;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NonHourlyJobIncomeInformationTest {

  @ParameterizedTest
  @CsvSource(value = {
	  "EVERY_DAY,1",
      "EVERY_WEEK,4",
      "EVERY_TWO_WEEKS,2",
      "TWICE_A_MONTH,2",
      "EVERY_MONTH,1",
      "IT_VARIES,1"
  })
  
  void shouldCalculateGrossMonthlyIncome(String payPeriod, String income) {
	  //1 is income per pay period 
	  //we see that if we get paid a day, we use the multiplier of 1 as we use 30 day income for that
	  //whereas they would get a multiplier of 4 for weekly pay 1*4 = 4 which is what our test is checking 
    assertThat(new NonHourlyJobIncomeInformation(payPeriod, "1", 0, null).grossMonthlyIncome())
        .isEqualTo(Money.parse(income));
    
  }
}
