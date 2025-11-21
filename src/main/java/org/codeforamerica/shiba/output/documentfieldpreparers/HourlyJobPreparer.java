package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.DocumentFieldType.SINGLE_VALUE;

import java.util.ArrayList;
import java.util.List;

import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.application.parsers.GrossMonthlyIncomeParser;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.Recipient;
import org.codeforamerica.shiba.output.caf.HourlyJobIncomeInformation;
import org.codeforamerica.shiba.output.caf.JobIncomeInformation;
import org.springframework.stereotype.Component;

@Component
public class HourlyJobPreparer implements DocumentFieldPreparer {

  private final GrossMonthlyIncomeParser grossMonthlyIncomeParser;

  public HourlyJobPreparer(GrossMonthlyIncomeParser grossMonthlyIncomeParser) {
    this.grossMonthlyIncomeParser = grossMonthlyIncomeParser;
  }

  @Override
  public List<DocumentField> prepareDocumentFields(Application application, Document document,
      Recipient _recipient) {
    List<JobIncomeInformation> jobs =
        grossMonthlyIncomeParser.parse(application.getApplicationData());
    List<DocumentField> result = new ArrayList<>();
    for (int i = 0; i < jobs.size(); i++) {
      // ScopeTracker needs to track for every job iteration, even though we are only adding for hourly jobs
      if (jobs.get(i) instanceof HourlyJobIncomeInformation hourlyJob) {
        result.add(new DocumentField("payPeriod", "payPeriod", "Hourly",
            SINGLE_VALUE, i));
      }
    }
    return result;
  }
}
