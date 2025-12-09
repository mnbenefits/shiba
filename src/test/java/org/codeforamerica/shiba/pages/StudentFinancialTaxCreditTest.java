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
	  
	  //single applicant otherUnearnedIncome flow to studentFinancialAid
	  @Test
	  void shouldFlowFromOtherUnearnedIncomeToStudentFinancialAid() throws Exception {
		  selectPrograms("CASH");
		  postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED", "studentFinancialAid");
	  }
	  
	  //single applicant otherUnearnedIncomesources flow to studentFinancialAid
	  @Test
	  void shouldFlowFromOtherUnearnedIncomeSourcesToStudentFinancialAid() throws Exception {
		  selectPrograms("CASH");
		  postExpectingSuccess("otherUnearnedIncome", "otherUnearnedIncome", "OTHER_PAYMENTS");
		  postExpectingRedirect("otherUnearnedIncomeSources", "otherUnearnedIncome", "OTHER_PAYMENTS", "studentFinancialAid");
	  }
	  
	  //Household applicant otherUnearnedIncomeSources flow to studentFinancialAid
	  @Test
	  void shouldFlowFromOtherUnearnedIncomeSourcePagesToStudentFinancialAid() throws Exception {
		  selectPrograms("CASH");
		  fillOutPersonalInfo();
		  addHouseholdMembersWithProgram("CASH");
		  postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", List.of("DAY_TRADING", "OTHER_PAYMENTS"), "dayTradingIncomeSource");
		  postExpectingRedirect("dayTradingIncomeSource", Map.of("monthlyIncomeDayTradingProceeds", List.of("Dwight Schrute applicant"), "dayTradingProceedsAmount", List.of("230", "")), "otherPaymentsIncomeSource");
		  postExpectingRedirect("otherPaymentsIncomeSource", Map.of("monthlyIncomeOtherPayments", List.of("Dwight Schrute applicant"), "otherPaymentsAmount", List.of("230", "")), "studentFinancialAid");
		  
	  }
	  
	  //single applicant otherUnearnedIncome flow to advancedChildTaxCredit when no other unearned income
	  @Test
	  void shouldFlowFromOtherUnearnedIncomeToadvancedChildTaxCredit() throws Exception {
		  selectPrograms("SNAP");
		  postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", "NO_OTHER_UNEARNED_INCOME_SELECTED", "advancedChildTaxCredit");
		  postExpectingRedirect("advancedChildTaxCredit", "hasAdvancedChildTaxCredit", "false","studentFinancialAid");

	  }
	  
	  //single applicant otherUnearnedIncomesources flow to advancedChildTaxCredit
	  @Test
	  void shouldFlowFromOtherUnearnedIncomeSourcesToadvancedChildTaxCredit() throws Exception {
		  selectPrograms("SNAP");
		  postExpectingSuccess("otherUnearnedIncome", "otherUnearnedIncome", "OTHER_PAYMENTS");
		  postExpectingRedirect("otherUnearnedIncomeSources", "otherUnearnedIncome", "OTHER_PAYMENTS", "advancedChildTaxCredit");
		  postExpectingRedirect("advancedChildTaxCredit", "hasAdvancedChildTaxCredit", "false","studentFinancialAid");

	  }

	  //Household applicant otherUnearnedIncomeSources flow to advancedChildTaxCredit
	  @Test
	  void shouldFlowFromOtherUnearnedIncomeSourcePagesToadvancedChildTaxCredit() throws Exception {
		  selectPrograms("SNAP");
		  fillOutPersonalInfo();
		  addHouseholdMembersWithProgram("SNAP");
		  postExpectingRedirect("otherUnearnedIncome", "otherUnearnedIncome", List.of("DAY_TRADING", "OTHER_PAYMENTS"), "dayTradingIncomeSource");
		  postExpectingRedirect("dayTradingIncomeSource", Map.of("monthlyIncomeDayTradingProceeds", List.of("Dwight Schrute applicant"), "dayTradingProceedsAmount", List.of("230", "")), "otherPaymentsIncomeSource");
		  postExpectingRedirect("otherPaymentsIncomeSource", Map.of("monthlyIncomeOtherPayments", List.of("Dwight Schrute applicant"), "otherPaymentsAmount", List.of("230", "")), "advancedChildTaxCredit");
		  postExpectingRedirect("advancedChildTaxCredit", "hasAdvancedChildTaxCredit", "false","studentFinancialAid");
		  
	  }
	  
	  
	  
}
