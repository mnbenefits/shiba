package org.codeforamerica.shiba.pages.config;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

@Data
public class PromptMessage implements Serializable {

	@Serial
	private static final long serialVersionUID = 5861139204290273630L;

	private String promptMessageFragmentName;
	private String promptMessageKey;
}
