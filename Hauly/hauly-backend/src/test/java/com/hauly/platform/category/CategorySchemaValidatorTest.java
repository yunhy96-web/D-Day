package com.hauly.platform.category;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hauly.platform.category.domain.model.ProductCategory;
import com.hauly.platform.category.domain.model.ValidationResult;
import com.hauly.platform.category.domain.service.CategorySchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategorySchemaValidatorTest {

    private CategorySchemaValidator validator;
    private ObjectMapper objectMapper;

    // CONTACT_LENS schema from seed
    private static final String LENS_SCHEMA = """
            {
              "fields": [
                {"key": "brand", "label_key": "k", "type": "text", "required": true},
                {"key": "wear_cycle", "label_key": "k", "type": "select", "options_code": "LENS_WEAR_CYCLE", "required": true},
                {"key": "power_value", "label_key": "k", "type": "decimal", "required": false, "min": -20, "max": 6},
                {"key": "left_eye", "label_key": "k", "type": "group", "fields": [
                  {"key": "power", "label_key": "k", "type": "decimal", "required": true, "min": -20, "max": 6}
                ]}
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new CategorySchemaValidator(objectMapper);
    }

    private ProductCategory categoryWithSchema(String schema) {
        return ProductCategory.create("TEST", "test.key", schema, 0);
    }

    @Test
    void validCase_allRequiredFieldsPresent_returnsValid() throws Exception {
        ProductCategory cat = categoryWithSchema(LENS_SCHEMA);
        JsonNode attributes = objectMapper.readTree("""
                {"brand": "Acuvue", "wear_cycle": "1DAY", "left_eye": {"power": -3.5}}
                """);

        ValidationResult result = validator.validate(cat, attributes);

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void requiredFieldMissing_returnsInvalid() throws Exception {
        ProductCategory cat = categoryWithSchema(LENS_SCHEMA);
        JsonNode attributes = objectMapper.readTree("""
                {"wear_cycle": "1DAY", "left_eye": {"power": -3.5}}
                """);

        ValidationResult result = validator.validate(cat, attributes);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.field().equals("brand") && e.message().contains("required"));
    }

    @Test
    void wrongTypeForTextField_returnsInvalid() throws Exception {
        // brand should be text but we send a number
        ProductCategory cat = categoryWithSchema(LENS_SCHEMA);
        JsonNode attributes = objectMapper.readTree("""
                {"brand": 123, "wear_cycle": "1DAY", "left_eye": {"power": -3.5}}
                """);

        ValidationResult result = validator.validate(cat, attributes);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.field().equals("brand"));
    }

    @Test
    void decimalOutOfRangeMin_returnsInvalid() throws Exception {
        ProductCategory cat = categoryWithSchema(LENS_SCHEMA);
        JsonNode attributes = objectMapper.readTree("""
                {"brand": "X", "wear_cycle": "1DAY", "power_value": -25.0, "left_eye": {"power": -3.5}}
                """);

        ValidationResult result = validator.validate(cat, attributes);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.field().equals("power_value") && e.message().contains(">= -20"));
    }

    @Test
    void decimalOutOfRangeMax_returnsInvalid() throws Exception {
        ProductCategory cat = categoryWithSchema(LENS_SCHEMA);
        JsonNode attributes = objectMapper.readTree("""
                {"brand": "X", "wear_cycle": "1DAY", "power_value": 10.0, "left_eye": {"power": -3.5}}
                """);

        ValidationResult result = validator.validate(cat, attributes);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.field().equals("power_value") && e.message().contains("<= 6"));
    }

    @Test
    void nestedGroupRequiredMissing_returnsInvalid() throws Exception {
        ProductCategory cat = categoryWithSchema(LENS_SCHEMA);
        // left_eye.power is required but missing
        JsonNode attributes = objectMapper.readTree("""
                {"brand": "X", "wear_cycle": "1DAY", "left_eye": {}}
                """);

        ValidationResult result = validator.validate(cat, attributes);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.field().equals("left_eye.power"));
    }

    @Test
    void wrongTypeForDecimalField_returnsInvalid() throws Exception {
        ProductCategory cat = categoryWithSchema(LENS_SCHEMA);
        JsonNode attributes = objectMapper.readTree("""
                {"brand": "X", "wear_cycle": "1DAY", "left_eye": {"power": "not-a-number"}}
                """);

        ValidationResult result = validator.validate(cat, attributes);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.field().equals("left_eye.power"));
    }

    @Test
    void emptySchema_anyAttributes_returnsValid() throws Exception {
        ProductCategory cat = categoryWithSchema("{\"fields\": []}");
        JsonNode attributes = objectMapper.readTree("{\"anything\": \"goes\"}");

        ValidationResult result = validator.validate(cat, attributes);

        assertThat(result.isValid()).isTrue();
    }
}
