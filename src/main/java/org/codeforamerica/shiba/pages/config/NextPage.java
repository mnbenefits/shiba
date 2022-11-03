package org.codeforamerica.shiba.pages.config;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import org.codeforamerica.shiba.application.FlowType;
import org.codeforamerica.shiba.inputconditions.Condition;

@Data
public class NextPage implements Serializable {

  @Serial
  private static final long serialVersionUID = 5831139285190273630L;

  private String pageName;
  private Condition condition;
  private FlowType flow;
  private String flag;
}
