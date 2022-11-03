package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

@Data
public class PageDatasource implements Serializable {

  @Serial
  private static final long serialVersionUID = 5837439204290273630L;

  private String pageName;
  private String groupName;
  private String inputName;
  private boolean optional = false;
}
