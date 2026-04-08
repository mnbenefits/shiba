package org.codeforamerica.shiba.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;


@ControllerAdvice
public class ShibaGlobalModelAttributes {

  @Value("${shiba.build-version:release}")
  private String buildVersion;

  @ModelAttribute("shibaBuildVersion")
  public String shibaBuildVersion() {
    return buildVersion;
  }
}