package org.codeforamerica.shiba.output;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.codeforamerica.shiba.Money;
import org.codeforamerica.shiba.output.caf.HourlyJobIncomeInformation;
import org.codeforamerica.shiba.output.caf.NonHourlyJobIncomeInformation;
import org.codeforamerica.shiba.output.caf.TotalIncome;
import org.codeforamerica.shiba.output.caf.TotalIncomeCalculator;
import org.junit.jupiter.api.Test;

class TotalIncomeCalculatorTest {

  TotalIncomeCalculator totalIncomeCalculator = new TotalIncomeCalculator();

  @Test
  void calculateReturnsIncomeWhenNoJobInfoProvided() {
	assertThat(totalIncomeCalculator.calculate(new TotalIncome(Money.ONE, emptyList())))
        .isEqualTo(Money.ONE);
  }

  @Test
  void calculateReturnsTheSumOfAllJobIncomeWhenProvided() {
    assertThat(totalIncomeCalculator.calculate(
        new TotalIncome(Money.parse("9999"),
        		List.of(new NonHourlyJobIncomeInformation("EVERY_MONTH", "10", 0, null), new HourlyJobIncomeInformation("25", "1", 0, null)))))
        .isEqualTo(Money.parse("110"));
  }
  
  
  // YUVA G how does new total income work, doesnt use existing income if there are jobs?
  @Test
  void calculateReturnsCorrectTotalWhenPaidEachDay() {
    // Create a TotalIncome with one daily-paid non-hourly job and one hourly job
    TotalIncome totalIncome = new TotalIncome(
        Money.parse("100"), // existing income (e.g., other sources)
        List.of(
            new NonHourlyJobIncomeInformation("EVERY_DAY", "100", 0, null),
            new HourlyJobIncomeInformation("25", "2", 0, null)
        )
    );

    // For EACH_DAY, grossMonthlyIncome() = incomePerPayPeriod = 100 (no multiplier)
    // For the hourly job: 25 * 2 * 4 = 200 (4 weeks per month assumption)
    // So total should be 100 + 200 = 300
    assertThat(totalIncomeCalculator.calculate(totalIncome))
        .isEqualTo(Money.parse("300"));
  }

}
