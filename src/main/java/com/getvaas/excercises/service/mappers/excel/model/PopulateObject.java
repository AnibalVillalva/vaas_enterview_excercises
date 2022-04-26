package com.getvaas.excercises.service.mappers.excel.model;

import java.util.Map;

public interface PopulateObject<T> {
     T populate(Map<String,Object> data);
    /**
     * It allows to map a value of a map in a safe way
     * 
     * @param fieldName name of field
     * @param data      data to map
     * @return mapped value , or  {@code null} if the value does not exist
     */
     default Object getToMap(String fieldName, Map<String, Object> data) {
        if (data.containsKey(fieldName)) {
            return data.get(fieldName);
        }
        return null;
    }
}
