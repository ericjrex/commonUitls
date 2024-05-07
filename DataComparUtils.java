package com.nannar.platform.basis.api.util;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.nannar.platform.basis.api.pb.asset.entity.AsAsset;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class DataComparUtils {

    private final static List<String> ignoreFieldList = Arrays.asList("id", "createTime", "createBy", "updateTime", "updateBy", "otherField", "isDelete");

    public static void main(String[] args) {
        AsAsset asset1 = new AsAsset();
        asset1.setId("123");
        asset1.setAssetName("测试1");
        asset1.setAssettypeName("设备分类1");
        asset1.setCreateBy("e9ca23d68d884d4ebb19d07889727dae");
        asset1.setBrand("厂商1");
        asset1.setCreateTime(new Date());
        AsAsset asset2 = new AsAsset();
        asset2.setId("123");
        asset2.setAssetName("测试");
        asset2.setAssettypeName("设备分类1");
        asset2.setCreateBy("e9ca23d68d884d4ebb19d07889727dae");
        asset2.setCreateTime(new Date());
        //System.out.println(insertContent(asset1));
        System.out.println(comparData(asset1, asset2));
    }

    public static String insertContent(Object obj) {
        try {
            List<String> editContentList = new ArrayList<>();
            Class<?> aClass = obj.getClass();
            Field[] declaredFields = aClass.getDeclaredFields();
            for (Field field : declaredFields) {
                String fieldName = field.getName();
                String showName = fieldName;
                if (ignoreFieldList.contains(fieldName)) {
                    continue;
                }
                TableField tableFieldAnnotation = field.getAnnotation(TableField.class);
                if (tableFieldAnnotation != null && !tableFieldAnnotation.exist()) {
                    continue;
                }
                ApiModelProperty apiAnnotation = field.getAnnotation(ApiModelProperty.class);
                // 注解不空，用注解描述
                if (apiAnnotation != null && org.apache.commons.lang3.StringUtils.isNotBlank(apiAnnotation.value())) {
                    showName = apiAnnotation.value();
                }
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(fieldName, aClass);
                Method readMethod = propertyDescriptor.getReadMethod();
                Object val = readMethod.invoke(obj) != null ? readMethod.invoke(obj) : "";
                if(field.getType().getTypeName().equals(Date.class.getTypeName())){
                    DateTimeFormat dateTimeFormat = field.getAnnotation(DateTimeFormat.class);
                    if(dateTimeFormat != null){
                        String pattern = dateTimeFormat.pattern();
                        if(org.apache.commons.lang3.StringUtils.isNotEmpty(val.toString()) && org.apache.commons.lang3.StringUtils.isNotEmpty(pattern)){
                            val = DateUtil.format((Date) val, pattern);
                        }
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append("").append(showName).append("：" + val);
                editContentList.add(sb.toString());
            }
            return String.join("\n", editContentList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public static String insertJsonObjContent(JSONObject obj) {
        try {
            List<String> editContentList = new ArrayList<>();
            Set<String> keySet = obj.keySet();
            for (String field : keySet) {
                Object val = obj.getOrDefault(field, "");
                StringBuilder sb = new StringBuilder();
                sb.append("").append(field).append("：" + val);
                editContentList.add(sb.toString());
            }
            return String.join("\n", editContentList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public static String comparData(Object oldObj, Object newObj) {
        try {
            Class<?> oldClass = oldObj.getClass();
            Class<?> newClass = newObj.getClass();
            Field[] oldDeclaredFields = oldClass.getDeclaredFields();
            Field[] newDeclaredFields = newClass.getDeclaredFields();

            Map<String, Field> newFieldMap = new HashMap<>();
            for (Field field : newDeclaredFields) {
                newFieldMap.put(field.getName(), field);
            }

            List<String> editContentList = new ArrayList<>();
            for (Field field : oldDeclaredFields) {
                String fieldName = field.getName();
                Field newField = newFieldMap.get(fieldName);
                if (ignoreFieldList.contains(fieldName)) {
                    continue;
                }
                if (newField == null) {
                    continue;
                }
                String showName = fieldName;
                ApiModelProperty apiAnnotation = newField.getAnnotation(ApiModelProperty.class);
                TableField tableFieldAnnotation = newField.getAnnotation(TableField.class);
                if (tableFieldAnnotation != null && !tableFieldAnnotation.exist()) {
                    continue;
                }
                // 注解不空，用注解描述
                if (apiAnnotation != null && StringUtils.isNotBlank(apiAnnotation.value())) {
                    showName = apiAnnotation.value();
                }
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(fieldName, newClass);
                Method readMethod = propertyDescriptor.getReadMethod();
                Object oldVal = readMethod.invoke(oldObj) != null ? readMethod.invoke(oldObj) : "";
                Object newVal = readMethod.invoke(newObj) != null ? readMethod.invoke(newObj) : "";

                if(field.getType().getTypeName().equals(Date.class.getTypeName())){
                    DateTimeFormat dateTimeFormat = field.getAnnotation(DateTimeFormat.class);
                    if(dateTimeFormat != null){
                        String pattern = dateTimeFormat.pattern();
                        if(org.apache.commons.lang3.StringUtils.isNotEmpty(oldVal.toString()) && org.apache.commons.lang3.StringUtils.isNotEmpty(pattern)){
                            oldVal = DateUtil.format((Date) oldVal, pattern);
                        }
                        if(org.apache.commons.lang3.StringUtils.isNotEmpty(newVal.toString()) && org.apache.commons.lang3.StringUtils.isNotBlank(pattern)){
                            newVal = DateUtil.format((Date) newVal, pattern);
                        }
                    }
                }

                StringBuilder sb = new StringBuilder();
                if (!Objects.equals(oldVal, newVal)) {
                    sb.append("").append(showName).append("：从[" + oldVal + "]更新为[" + newVal + "]");
                    editContentList.add(sb.toString());
                }
            }
            return String.join("\n", editContentList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public static String comparJsonObjData(JSONObject oldObj, JSONObject newObj) {
        try {
            List<String> editContentList = new ArrayList<>();
            Set<String> allFields = new LinkedHashSet<>();
            allFields.addAll(oldObj.keySet());
            allFields.addAll(newObj.keySet());

            for (String curField : allFields) {
                Object oldVal = oldObj.getOrDefault(curField, null);
                Object newVal = newObj.getOrDefault(curField, null);

                if (!Objects.equals(oldVal, newVal)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("").append(curField).append("：从[" + oldVal + "]更新为[" + newVal + "]");
                    editContentList.add(sb.toString());
                }
            }
            return String.join("\n", editContentList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

}
