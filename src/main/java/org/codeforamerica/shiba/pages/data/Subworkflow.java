package org.codeforamerica.shiba.pages.data;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class Subworkflow extends ArrayList<Iteration> {

  @Serial
  private static final long serialVersionUID = -8623969544656997967L;

  public Subworkflow(List<PagesData> pagesData) {
    pagesData.forEach(this::add);
  }

  public void add(PagesData pagesData) {
    add(new Iteration(pagesData));
  }

  public int indexOf(PagesData pagesData) {
    for (int i = 0; i < size(); i++) {
      if (this.get(i).getPagesData().equals(pagesData)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return the PagesData object associated with a given Iteration of this Subworkflow
   * @param id -  the UUID that identifies the given Iteration
   * @return - the PagesData object, or null
   */
  public PagesData pagesDataForId(UUID id) {
	Iteration iteration;
    for (int i = 0; i < size(); i++) {
      iteration = this.get(i);
      if (iteration.getId().compareTo(id) == 0) {
        return iteration.getPagesData();
      }
    }
    return null;
  }
  
  /**
* Return the PagesData object associated with a given Iteration of this Subworkflow
* Handles both UUID strings and the special "applicant" identifier
* @param id - the string identifier (UUID string or "applicant")
* @return - the PagesData object, or null
*/
public PagesData pagesDataForId(String id) {
	 // Check if the ID is the special "applicant" string
  if ("applicant".equals(id)) {
      // The applicant is not in the household subworkflow
	// Return null because the applicant's data is NOT stored in the household subworkflow - it's on the personalInfo page
      return null;
  }
  
  // If it's not "applicant", try to treat it as a UUID.Try to parse as UUID for household members
  try {
      UUID uuid = UUID.fromString(id);
      return pagesDataForId(uuid);
  } catch (IllegalArgumentException e) {
      // Invalid UUID format
      return null;
  }
} 
  
}
