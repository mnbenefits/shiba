package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.WORK_SITUATION;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WorkSituationPreparer extends OneToManyDocumentFieldPreparer {

	   private static final List<String> WORK_SITUATION_OPTIONS = List.of(
		        "STOP_WORKING", 
		        "REFUSE_A_JOB_OFFER", 
		        "ASK_TO_WORK_FEWER_HOURS", 
		        "GO_ON_STRIKE"
		    );

	   @Override
	    protected OneToManyParams getParams() {
	        return new OneToManyParams("workSituation", WORK_SITUATION, WORK_SITUATION_OPTIONS);
	    }

}
