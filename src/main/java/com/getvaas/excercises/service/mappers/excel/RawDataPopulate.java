package com.getvaas.excercises.service.mappers.excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getvaas.excercises.service.mappers.excel.model.PopulateObject;

import java.time.LocalDate;
import java.util.Map;

public class RawDataPopulate implements PopulateObject<RawDataExcelDto> {

    @Override
    public RawDataExcelDto populate(Map<String, Object> data) {
        ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper
        RawDataExcelDto pojo = mapper.convertValue(data, RawDataExcelDto.class);
        pojo.setReportDate((LocalDate) data.get("reportDate"));
        pojo.setContractDisbursementDate((LocalDate) data.get("contractDisbursementDate"));
        return pojo;
    }
}
