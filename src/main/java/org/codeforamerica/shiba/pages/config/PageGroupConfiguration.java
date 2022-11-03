package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class PageGroupConfiguration implements Serializable {

  @Serial
  private static final long serialVersionUID = 5831139204710273630L;

  private List<String> completePages;
  private List<String> startPages;
  private String reviewPage;
  private String deleteWarningPage;
  private String redirectPage;
  private String restartPage;
  private Integer startingCount;
}
