package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

@Data
public class AlertBox implements Serializable {

  @Serial
  private static final long serialVersionUID = 5831139204290643630L;

  private AlertBoxType type;
  private String message;

  public String getAlertBoxType() {
    return switch (type) {
      case CHOOSE_NONE_WARNING -> "warning";
      case NOTICE -> "notice";
    };
  }
}
