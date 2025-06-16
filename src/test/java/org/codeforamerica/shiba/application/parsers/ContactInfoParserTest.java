package org.codeforamerica.shiba.application.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.codeforamerica.shiba.pages.data.ApplicationData;
import org.codeforamerica.shiba.testutilities.TestApplicationDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContactInfoParserTest {

  private TestApplicationDataBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new TestApplicationDataBuilder();
  }

  @Test
  void shouldReturnPreferredWrittenLanguage() {
    String expectedValue = "AnyLanguageYouChoose";
    ApplicationData applicationData = builder
        .withPageData("writtenLanguage", "writtenLanguage", List.of("AnyLanguageYouChoose"))
        .build();

    String value = ContactInfoParser.writtenLanguagePref(applicationData);

    assertThat(value).isEqualTo(expectedValue);
  }

  @Test
  void shouldReturnPreferredSpokenLanguage() {
    String expectedValue = "AnyLanguageYouChoose";
    ApplicationData applicationData = builder
        .withPageData("spokenLanguage", "spokenSameAsWritten", "true")
        .withPageData("spokenLanguage", "spokenLanguage", List.of("AnyLanguageYouChoose"))
        .build();

    String value = ContactInfoParser.spokenLanguagePref(applicationData);

    assertThat(value).isEqualTo(expectedValue);
  }
}
