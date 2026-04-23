package org.codeforamerica.shiba.configurations;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;


// @ControllerAdvice: makes the @ModelAttribute below visible to every Thymeleaf view in the app,
// so any template can use ${shibaBuildVersion} without each controller having to set it manually.
@ControllerAdvice
public class ShibaGlobalModelAttributes {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("America/Chicago"));

  // Provided by Spring Boot autoconfiguration when build-info.properties is on the classpath.
  // Exposes build.time, build.version, etc.
  private final BuildProperties buildProperties;

  // Provided by Spring Boot autoconfiguration when git.properties is on the classpath.
  // Exposes git.commit.id, git.commit.id.abbrev (short SHA), git.branch, etc.
  private final GitProperties gitProperties;

  // Both dependencies are optional (required = false) because either metadata file can be absent —
  // e.g. running tests before resources are processed, or building outside a git checkout.
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

    // Preferred form: both pieces available -> "2026-04-20 · a1b2c3d".
    if (StringUtils.hasText(date) && StringUtils.hasText(sha)) {
      return date + " · " + sha;
    }
    // No SHA (e.g. built without .git) -> just the date.
    if (StringUtils.hasText(date)) {
      return date;
    }
    // No build-info (e.g. ran app without bootBuildInfo) -> just the SHA.
    if (StringUtils.hasText(sha)) {
      return sha;
    }
    // Neither file reached the classpath. Returning empty hides the footer line via th:if,
    return "";
  }

  private String buildDate() {
    // buildProperties is null when META-INF/build-info.properties isn't on the classpath.
    // buildProperties.getTime() can be null if the file exists but has no build.time entry.
    if (buildProperties == null || buildProperties.getTime() == null) {
      return "";
    }
    // getTime() returns an Instant; DATE_FORMAT is UTC-zoned so output is deterministic.
    return DATE_FORMAT.format(buildProperties.getTime());
  }

  private String shortSha() {
    // gitProperties is null when git.properties isn't on the classpath (no git during build,
    if (gitProperties == null) {
      return "";
    }
    // Short SHA (7 chars by default), from git.commit.id.abbrev.
    String sha = gitProperties.getShortCommitId();
    return sha != null ? sha.trim() : "";
  }
}