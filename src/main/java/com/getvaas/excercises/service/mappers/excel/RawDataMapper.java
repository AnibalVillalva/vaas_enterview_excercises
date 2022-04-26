package com.getvaas.excercises.service.mappers.excel;

import com.getvaas.excercises.service.mappers.excel.exceptions.ExcelMapperException;
import com.getvaas.excercises.service.mappers.excel.model.*;
import com.getvaas.excercises.service.mappers.excel.search.SimpleSearcher;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.xlsx4j.exceptions.Xlsx4jException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class RawDataMapper {
    private static final List<Field> rowDataFields;

    static {
        List<String> reportDateS = Arrays.asList("Report Date");
        List<String> funderS = Arrays.asList("Funder");
        List<String> productS = Arrays.asList("Product");
        List<String> contractDisbursementDateS = Arrays.asList("Contract Disbursement Date");
        List<String> contractIdS = Arrays.asList("Contract Id");
        List<String> contractNumberS = Arrays.asList("Contract Number");
        List<String> fullNameS = Arrays.asList("Full Name");
        List<String> idNumberS = Arrays.asList("ID Number");
        List<String> contractS = Arrays.asList("Contract Status");
        List<String> loanAmountS = Arrays.asList("Loan Amount");
        List<String> termMonthsS = Arrays.asList("Term Months");
        List<String> repaymentFrequencyS = Arrays.asList("Repayment Frequency");
        List<String> loanRepaymentAmountS = Arrays.asList("Loan Repayment Amount");
        List<String> outstandingPrincipalBalanceS = Arrays.asList("Outstanding Principal Balance");
        List<String> totalOutstandingBalanceS = Arrays.asList("Total Outstanding Balance");
        List<String> outstandingInterestAndFeesBalanceS = Arrays.asList("Outstanding Interest and Fees Balance");
        List<String> arrearsBucketS = Arrays.asList("Arrears Bucket");
        List<String> aprS = Arrays.asList("APR");

        rowDataFields = Arrays.asList(
                Field.Builder.aField().fieldName("reportDate").dataType(DataType.LOCAL_DATE).synonyms(reportDateS).build(),
                Field.Builder.aField().fieldName("funder").dataType(DataType.STRING).synonyms(funderS).build(),
                Field.Builder.aField().fieldName("product").dataType(DataType.STRING).synonyms(productS).build(),
                Field.Builder.aField().fieldName("contractDisbursementDate").dataType(DataType.LOCAL_DATE).synonyms(contractDisbursementDateS).build(),
                Field.Builder.aField().fieldName("contractId").dataType(DataType.LONG).synonyms(contractIdS).build(),
                Field.Builder.aField().fieldName("contractNumber").dataType(DataType.LONG).synonyms(contractNumberS).build(),
                Field.Builder.aField().fieldName("fullName").dataType(DataType.STRING).synonyms(fullNameS).build(),
                Field.Builder.aField().fieldName("idNumber").dataType(DataType.STRING).synonyms(idNumberS).build(),
                Field.Builder.aField().fieldName("contract").dataType(DataType.STRING).synonyms(contractS).build(),
                Field.Builder.aField().fieldName("loanAmount").dataType(DataType.LONG).synonyms(loanAmountS).build(),
                Field.Builder.aField().fieldName("termMonths").dataType(DataType.INT).synonyms(termMonthsS).build(),
                Field.Builder.aField().fieldName("repaymentFrequency").dataType(DataType.STRING).synonyms(repaymentFrequencyS).build(),
                Field.Builder.aField().fieldName("loanRepaymentAmount").dataType(DataType.LONG).synonyms(loanRepaymentAmountS).build(),
                Field.Builder.aField().fieldName("totalOutstandingBalance").dataType(DataType.LONG).synonyms(totalOutstandingBalanceS).build(),
                Field.Builder.aField().fieldName("outstandingPrincipalBalance").dataType(DataType.LONG).synonyms(outstandingPrincipalBalanceS).build(),
                Field.Builder.aField().fieldName("outstandingInterestAndFeesBalance").dataType(DataType.LONG).synonyms(outstandingInterestAndFeesBalanceS).build(),
                Field.Builder.aField().fieldName("arrearsBucket").dataType(DataType.STRING).synonyms(arrearsBucketS).build(),
                Field.Builder.aField().fieldName("apr").dataType(DataType.STRING).synonyms(aprS).build()
        );
    }

    public static List<RawDataExcelDto> mapToDTO(InputStream excelStream) throws IOException, Docx4JException, Xlsx4jException, ExcelMapperException {
        ExcelMapper<RawDataExcelDto> excelMapper = new ExcelMapper<>(new SimpleSearcher(), excelStream);
        PopulateObject<RawDataExcelDto> populateObject = new RawDataPopulate();
        Validator<RawDataExcelDto> validator = new RawDataValidator();
        MapperResponse<RawDataExcelDto> mapperResponse = excelMapper.mapExcelToDTO(rowDataFields, "Raw Data", validator, populateObject);
        return mapperResponse.getData();
    }
}
