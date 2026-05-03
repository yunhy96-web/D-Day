package com.hauly.platform.category.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hauly.platform.category.domain.model.FieldDefinition;
import com.hauly.platform.category.domain.model.ProductCategory;
import com.hauly.platform.category.domain.model.ValidationResult;
import com.hauly.platform.category.domain.model.ValidationResult.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain service that validates product attributes against category field schema.
 * No Spring imports — pure domain logic.
 */
public class CategorySchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(CategorySchemaValidator.class);

    private final ObjectMapper objectMapper;

    public CategorySchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidationResult validate(ProductCategory category, JsonNode attributes) {
        List<FieldDefinition> fields = parseSchema(category.getFieldSchema());
        List<FieldError> errors = new ArrayList<>();
        validateFields(fields, attributes, "", errors);
        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private void validateFields(List<FieldDefinition> fields, JsonNode node, String prefix, List<FieldError> errors) {
        if (fields == null || fields.isEmpty()) return;
        for (FieldDefinition field : fields) {
            String fullKey = prefix.isEmpty() ? field.key() : prefix + "." + field.key();
            JsonNode value = node != null ? node.get(field.key()) : null;

            if (field.required() && (value == null || value.isNull())) {
                errors.add(new FieldError(fullKey, "Field '" + fullKey + "' is required"));
                continue;
            }

            if (value == null || value.isNull()) continue;

            switch (field.type()) {
                case "text" -> {
                    if (!value.isTextual()) {
                        errors.add(new FieldError(fullKey, "Field '" + fullKey + "' must be a string"));
                    }
                }
                case "decimal" -> {
                    if (!value.isNumber()) {
                        errors.add(new FieldError(fullKey, "Field '" + fullKey + "' must be a number"));
                    } else {
                        double num = value.asDouble();
                        if (field.min() != null && num < field.min()) {
                            errors.add(new FieldError(fullKey, "Field '" + fullKey + "' must be >= " + field.min()));
                        }
                        if (field.max() != null && num > field.max()) {
                            errors.add(new FieldError(fullKey, "Field '" + fullKey + "' must be <= " + field.max()));
                        }
                    }
                }
                case "select" -> {
                    // options_code validation is done at application layer (needs code repository)
                    if (!value.isTextual()) {
                        errors.add(new FieldError(fullKey, "Field '" + fullKey + "' must be a string"));
                    }
                }
                case "group" -> {
                    if (!value.isObject()) {
                        errors.add(new FieldError(fullKey, "Field '" + fullKey + "' must be an object"));
                    } else {
                        validateFields(field.fields(), value, fullKey, errors);
                    }
                }
                default -> {
                    // unknown type — skip
                }
            }
        }
    }

    public List<FieldDefinition> parseSchema(String schemaJson) {
        try {
            JsonNode root = objectMapper.readTree(schemaJson);
            JsonNode fieldsNode = root.get("fields");
            if (fieldsNode == null || !fieldsNode.isArray()) return List.of();
            List<FieldDefinition> result = new ArrayList<>();
            for (JsonNode f : fieldsNode) {
                result.add(parseField(f));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse field_schema JSON, treating as empty: {}", e.getMessage());
            return List.of();
        }
    }

    private FieldDefinition parseField(JsonNode f) {
        String key = text(f, "key");
        String labelKey = text(f, "label_key");
        String type = text(f, "type");
        boolean required = f.path("required").asBoolean(false);
        String optionsCode = text(f, "options_code");
        Double min = f.hasNonNull("min") ? f.get("min").asDouble() : null;
        Double max = f.hasNonNull("max") ? f.get("max").asDouble() : null;
        Double step = f.hasNonNull("step") ? f.get("step").asDouble() : null;

        List<FieldDefinition> nested = new ArrayList<>();
        JsonNode nestedFields = f.get("fields");
        if (nestedFields != null && nestedFields.isArray()) {
            for (JsonNode nf : nestedFields) {
                nested.add(parseField(nf));
            }
        }
        return new FieldDefinition(key, labelKey, type, required, optionsCode, min, max, step,
                nested.isEmpty() ? null : nested);
    }

    private String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && n.isTextual()) ? n.asText() : null;
    }
}
