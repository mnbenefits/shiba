package org.codeforamerica.shiba.output.documentfieldpreparers;

import static org.codeforamerica.shiba.output.FullNameFormatter.getFullName;
import static org.codeforamerica.shiba.output.FullNameFormatter.getListOfSelectedFullNames;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.getValues;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.HAS_HOUSE_HOLD;
import static org.codeforamerica.shiba.application.parsers.ApplicationDataParser.Field.INVESTMENT_TYPE_INDIVIDUAL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.codeforamerica.shiba.application.Application;
import org.codeforamerica.shiba.output.Document;
import org.codeforamerica.shiba.output.DocumentField;
import org.codeforamerica.shiba.output.DocumentFieldType;
import org.codeforamerica.shiba.output.Recipient;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class InvestmentOwnerPreparer implements DocumentFieldPreparer {

  @Override
  public List<DocumentField> prepareDocumentFields(Application application, Document document,
      Recipient recipient) {
    List<DocumentField> results = new ArrayList<>();
    boolean hasHouseHold = getValues(application.getApplicationData().getPagesData(),HAS_HOUSE_HOLD).contains("true");
      
    List<DocumentField> stockOwners = getAssetsOwnerSection(application, "stocksHouseHoldSource", "stocksHouseHoldSource","investmentOwners", hasHouseHold, "STOCKS" );
    results.addAll(stockOwners);
    List<DocumentField> bondOwners = getAssetsOwnerSection(application, "bondsHouseHoldSource", "bondsHouseHoldSource","investmentOwners", hasHouseHold, "BONDS" );
    results.addAll(bondOwners);
    List<DocumentField> retirementAccountOwners = getAssetsOwnerSection(application, "retirementAccountsHouseHoldSource", "retirementAccountsHouseHoldSource","investmentOwners", hasHouseHold, "RETIREMENT_ACCOUNTS" );
    results.addAll(retirementAccountOwners);

    return results;
  }
  
  @NotNull
  private static List<DocumentField> getAssetsOwnerSection(Application application, String pageName,
      String inputName, String outputName, boolean hasHouseHold, String assetType) {
    List<String> assetOwnersSource =  getListOfSelectedFullNames(application, pageName, inputName);
    System.out.println("assetOwnersSource = " + assetOwnersSource); //TODO emj delete
    List<DocumentField> fields = new ArrayList<>();
    AtomicInteger i = new AtomicInteger(0);
    if (hasHouseHold) {
      fields = assetOwnersSource
          .stream().map(fullName -> new DocumentField("assetOwnerSource", outputName, List.of(fullName), DocumentFieldType.SINGLE_VALUE, i.get()))
          .peek(docField1 -> System.out.println("docField1 = " + docField1 ))
          .collect(Collectors.toList());
      fields.addAll(assetOwnersSource
              .stream()
              .map(fullName -> new DocumentField("assetOwnerSource", "investmentType", List.of(assetType), DocumentFieldType.SINGLE_VALUE, i.getAndIncrement()))
              .peek(docField2 -> System.out.println("docField2 = " + docField2 ))
              .collect(Collectors.toList()));
    } else {
      if (getValues(application.getApplicationData().getPagesData(),INVESTMENT_TYPE_INDIVIDUAL).contains(assetType))
        fields.add(
            new DocumentField("assetOwnerSource", outputName, List.of(getFullName(application)),
                DocumentFieldType.SINGLE_VALUE, i.getAndIncrement()));
      fields.add(
              new DocumentField("assetOwnerSource", "investmentType", List.of(assetType),
                  DocumentFieldType.SINGLE_VALUE, i.getAndIncrement()));
      
    }
    return fields;
  }
}
