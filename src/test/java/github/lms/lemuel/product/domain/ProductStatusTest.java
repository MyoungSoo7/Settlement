package github.lms.lemuel.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * ProductStatus Enum TDD Test
 */
@DisplayName("ProductStatus Enum")
class ProductStatusTest {

    @Test
    @DisplayName("모든 ProductStatus 값을 확인한다")
    void verifyAllProductStatusValues() {
        // given & when
        ProductStatus[] statuses = ProductStatus.values();

        // then
        assertThat(statuses).hasSize(4);
        assertThat(statuses).containsExactlyInAnyOrder(
                ProductStatus.ACTIVE,
                ProductStatus.INACTIVE,
                ProductStatus.OUT_OF_STOCK,
                ProductStatus.DISCONTINUED
        );
    }

    @ParameterizedTest
    @CsvSource({
            "ACTIVE, ACTIVE",
            "INACTIVE, INACTIVE",
            "OUT_OF_STOCK, OUT_OF_STOCK",
            "DISCONTINUED, DISCONTINUED"
    })
    @DisplayName("fromString()은 대문자 문자열을 올바른 ProductStatus로 변환한다")
    void fromString_WithUpperCase_ReturnsCorrectStatus(String input, ProductStatus expected) {
        // when
        ProductStatus result = ProductStatus.fromString(input);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "active, ACTIVE",
            "inactive, INACTIVE",
            "out_of_stock, OUT_OF_STOCK",
            "discontinued, DISCONTINUED"
    })
    @DisplayName("fromString()은 소문자 문자열을 올바른 ProductStatus로 변환한다")
    void fromString_WithLowerCase_ReturnsCorrectStatus(String input, ProductStatus expected) {
        // when
        ProductStatus result = ProductStatus.fromString(input);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "UNKNOWN", "", "   "})
    @DisplayName("fromString()은 잘못된 문자열 입력 시 ACTIVE를 기본값으로 반환한다")
    void fromString_WithInvalidInput_ReturnsDefaultStatus(String input) {
        // when
        ProductStatus result = ProductStatus.fromString(input);

        // then
        assertThat(result).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("fromString()은 null 입력 시 ACTIVE를 기본값으로 반환한다")
    void fromString_WithNull_ReturnsDefaultStatus() {
        // when
        ProductStatus result = ProductStatus.fromString(null);

        // then
        assertThat(result).isEqualTo(ProductStatus.ACTIVE);
    }
}
