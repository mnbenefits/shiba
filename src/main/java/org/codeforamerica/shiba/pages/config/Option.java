package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.io.Serializable;

import org.codeforamerica.shiba.inputconditions.Condition;
import lombok.Data;

@Data
public class Option implements Serializable {

  @Serial
  private static final long serialVersionUID = 5831139204290271730L;
  private String value;
  private String messageKey;
  private Boolean isNone;
  private String helpMessageKey;
  private String helpIcon;
  private boolean limitSelection = false;
  private String flag;
  private Condition condition;
}
