package com.getvaas.excercises.service.mappers.excel;

import java.util.ArrayList;
import java.util.List;

public class TrusteeAwsDataMapper {

    private static final String PDF_EXTENSION = "pdf";

    public static List<TrusteeLoansAws> mapToDTO(List<String> trusteeAwsLoanFileNames) {

        List<TrusteeLoansAws> result = new ArrayList<>();

        for (String trusteeAwsLoan : trusteeAwsLoanFileNames) {

            String[] loanIdWithPdfExtensionSplit = trusteeAwsLoan.split("\\.");

            if(loanIdWithPdfExtensionSplit.length > 1 && PDF_EXTENSION.equals(loanIdWithPdfExtensionSplit[1])) {

                String loanIdStr = loanIdWithPdfExtensionSplit[0];
                Long loanId = Long.parseLong(loanIdStr);
                TrusteeLoansAws trusteeLoansAws = new TrusteeLoansAws();
                trusteeLoansAws.setLoanId(loanId);

                result.add(trusteeLoansAws);

            }
        }

        return result;

    }
}
