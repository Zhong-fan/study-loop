package com.studyloop.common.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型输出校验器。
 * <p>
 * 大模型返回的 JSON 不能直接入库：字段可能缺、类型可能错、还可能多塞字段。
 * 这里按 classpath 下的 JSON Schema 校验，返回人类可读的错误列表。
 * Schema 解析结果按名字缓存，避免每次都读文件。
 */
public final class JsonSchemaValidator {

    private static final Map<String, JsonSchema> CACHE = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchemaValidator() {
    }

    /**
     * @param schemaName classpath:schemas/{schemaName}.schema.json
     * @return 空列表表示通过；否则每条是一行错误说明
     */
    public static List<String> validate(String schemaName, JsonNode payload) {
        JsonSchema schema = CACHE.computeIfAbsent(schemaName, JsonSchemaValidator::load);
        Set<ValidationMessage> messages = schema.validate(payload);

        List<String> errors = new ArrayList<>();
        for (ValidationMessage message : messages) {
            errors.add(message.getMessage());
        }
        errors.sort(Comparator.naturalOrder());
        return errors;
    }

    /** 校验一个字符串，解析失败也算不通过 */
    public static List<String> validateText(String schemaName, String json) {
        try {
            return validate(schemaName, MAPPER.readTree(json));
        } catch (Exception e) {
            return List.of("不是合法 JSON: " + e.getMessage());
        }
    }

    private static JsonSchema load(String schemaName) {
        String path = "/schemas/" + schemaName + ".schema.json";
        try (InputStream in = JsonSchemaValidator.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("找不到 Schema: " + path);
            }
            JsonNode schemaNode = MAPPER.readTree(in);
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
        } catch (Exception e) {
            throw new IllegalStateException("加载 Schema 失败: " + path, e);
        }
    }
}
