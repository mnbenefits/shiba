package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Value implements Serializable {

	@Serial
	private static final long serialVersionUID = 5831139204590273630L;

	private String defaultValue;
	private List<ConditionalValue> conditionalValues = List.of();

	public Value(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
