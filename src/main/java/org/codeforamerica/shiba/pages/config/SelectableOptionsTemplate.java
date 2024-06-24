package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codeforamerica.shiba.pages.data.PageData;
import org.codeforamerica.shiba.pages.data.Subworkflow;

import lombok.Data;

@Data
public class SelectableOptionsTemplate implements OptionsWithDataSourceTemplate, Serializable {

  @Serial
  private static final long serialVersionUID = 5831139291290273630L; 

  List<Option> selectableOptions = new ArrayList<>();
  Map<String, Subworkflow> subworkflows = new HashMap<>();
  Map<String, PageData> datasources = new HashMap<>();
}
