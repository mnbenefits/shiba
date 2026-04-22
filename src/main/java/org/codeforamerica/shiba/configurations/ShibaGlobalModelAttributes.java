package org.codeforamerica.shiba.configurations;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;


@ControllerAdvice
public class ShibaGlobalModelAttributes {

 
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

 
  private final BuildProperties buildProperties;

 
  private final GitProperties gitProperties;


  public ShibaGlobalModelAttributes(
      @Autowired(required = false) BuildProperties buildProperties,
      @Autowired(required = false) GitProperties gitProperties) {
    this.buildProperties = buildProperties;
    this.gitProperties = gitProperties;
  }

 
  @ModelAttribute("shibaBuildVersion")
  public String shibaBuildVersion() {
    String date = buildDate();
    String sha = shortSha();

    if (StringUtils.hasText(date) && StringUtils.hasText(sha)) {
      return date + " · " + sha;
    }
    if (StringUtils.hasText(date)) {
      return date;
    }
    if (StringUtils.hasText(sha)) {
      return sha;
    }
    return "";
  }

  private String buildDate() {
    if (buildProperties == null || buildProperties.getTime() == null) {
      return "";
    }
    return DATE_FORMAT.format(buildProperties.getTime());
  }

  private String shortSha() {
    if (gitProperties == null) {
      return "";
    }
    String sha = gitProperties.getShortCommitId();
    return sha != null ? sha.trim() : "";
  }
}