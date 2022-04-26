package com.getvaas.excercises.service.mappers.excel;

import com.getvaas.excercises.service.mappers.excel.model.CorrectnessLevel;
import com.getvaas.excercises.service.mappers.excel.model.ValidationResult;
import com.getvaas.excercises.service.mappers.excel.model.Validator;
import org.apache.logging.log4j.util.Strings;

public class RawDataValidator implements Validator<RawDataExcelDto> {

    @Override
    public ValidationResult validate(RawDataExcelDto value) {
        if (isEmpty(value)) {
            return new ValidationResult("vacio", CorrectnessLevel.ERROR);
        }
        return new ValidationResult("", CorrectnessLevel.OK);
    }

    private boolean isEmpty(RawDataExcelDto rowDataExcelDto) {
        String contId = rowDataExcelDto.getContractId() != null ? rowDataExcelDto.getContractId().toString() : "";
        String type = rowDataExcelDto.getProduct();
        return ((Strings.isBlank(contId) || contId.equals("0"))
                && (Strings.isBlank(type) || type.equals("0")));
    }
}
