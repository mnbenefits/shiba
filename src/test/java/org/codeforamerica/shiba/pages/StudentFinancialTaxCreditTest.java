package org.codeforamerica.shiba.pages;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Map;
import org.codeforamerica.shiba.testutilities.AbstractShibaMockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StudentFinancialTaxCreditTest extends AbstractShibaMockMvcTest {
	
	  @BeforeEach
	  protected void setUp() throws Exception {
	    super.setUp();
	    mockMvc.perform(get("/pages/identifyCountyBeforeApplying").session(session)); // start timer
	    postExpectingSuccess("identifyCountyBeforeApplying", "county", "Hennepin");
	    postExpectingSuccess("writtenLanguage", Map.of("writtenLanguage", List.of("ENGLISH")));
	    postExpectingSuccess("spokenLanguage", Map.of("spokenLanguage", List.of("ENGLISH")));
	  }
	  
	  @Test
	  void shouldFlowFromOtherUnearnedIncomeToStudentFinancialAid() throws Exception {
		  selectPrograms("CASH");
		  postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED", "studentFinancialAid");
	  }
	  
	  @Test
	  void shouldFlowFromOtherUnearnedIncomeSourcesToStudentFinancialAid() throws Exception {
		  selectPrograms("CASH");
		  postExpectingSuccess("otherUnearnedIncome", "otherUnearnedIncome", "OTHER_PAYMENTS");
		  postExpectingRedirect("otherUnearnedIncomeSources", "otherUnearnedIncome", "OTHER_PAYMENTS", "studentFinancialAid");
	  }
	  
	  @Test
	  void shouldFlowFromOtherUnearnedIncomeSourcePagesToStudentFinancialAid() throws Exception {
		  selectPrograms("CASH");
		  fillOutPersonalInfo();
		  addHouseholdMembersWithProgram("CASH");
		  postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", List.of("DAY_TRADING", "OTHER_PAYMENTS"), "dayTradingIncomeSource");
		  postExpectingRedirect("dayTradingIncomeSource", Map.of("monthlyIncomeDayTradingProceeds", List.of("Dwight Schrute applicant"), "dayTradingProceedsAmount", List.of("230", "")), "otherPaymentsIncomeSource");
		  postExpectingRedirect("otherPaymentsIncomeSource", Map.of("monthlyIncomeOtherPayments", List.of("Dwight Schrute applicant"), "otherPaymentsAmount", List.of("230", "")), "studentFinancialAid");
		  
	  }

}
