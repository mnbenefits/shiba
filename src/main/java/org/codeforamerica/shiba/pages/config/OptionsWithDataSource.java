package org.codeforamerica.shiba.pages.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class OptionsWithDataSource {

  List<PageDatasource> datasources = new ArrayList<>(); //TODO emj this is a List, rename it to pageDatasourceList
  List<Option> selectableOptions = new ArrayList<>();
}
