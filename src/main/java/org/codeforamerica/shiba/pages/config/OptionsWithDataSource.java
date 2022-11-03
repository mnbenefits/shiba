package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class OptionsWithDataSource implements Serializable {

	@Serial
	private static final long serialVersionUID = 5837154204290273630L;
	List<PageDatasource> datasources = new ArrayList<>();
	List<Option> selectableOptions = new ArrayList<>();
}
