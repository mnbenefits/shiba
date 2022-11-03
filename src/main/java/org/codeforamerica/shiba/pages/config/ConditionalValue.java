package org.codeforamerica.shiba.pages.config;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import org.codeforamerica.shiba.inputconditions.Condition;

@Data
public class ConditionalValue implements Serializable {

  @Serial
  private static final long serialVersionUID = 5889439204290273630L;

  private Condition condition;
  private String flag;
  private String value;
}
