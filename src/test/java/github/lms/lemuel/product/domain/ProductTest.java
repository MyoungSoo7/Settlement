package github.lms.lemuel.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Product Domain Entity TDD Test
 *
 * 테스트 범위:
 * 1. 생성자 및 팩토리 메서드
 * 2. name, price, stockQuantity 검증
 * 3. 비즈니스 메서드 (재고 증감, 가격 변경, 상태 변경)
 * 4. 상태 확인 메서드
 */
@DisplayName("Product 도메인 엔티티")
class ProductTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("기본 생성자로 Product 생성 시 기본값이 설정된다")
        void defaultConstructor() {
            // when
            Product product = new Product();

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(product.getStockQuantity()).isEqualTo(0);
            assertThat(product.getCreatedAt()).isNotNull();
            assertThat(product.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("전체 생성자로 Product 생성 시 모든 필드가 설정된다")
        void allArgsConstructor() {
            // given
            Long id = 1L;
            String name = "Test Product";
            String description = "Test Description";
            BigDecimal price = new BigDecimal("10000");
            Integer stockQuantity = 100;
            ProductStatus status = ProductStatus.ACTIVE;
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now();

            // when
            Product product = new Product(id, name, description, price, stockQuantity, status, createdAt, updatedAt);

            // then
            assertThat(product.getId()).isEqualTo(id);
            assertThat(product.getName()).isEqualTo(name);
            assertThat(product.getDescription()).isEqualTo(description);
            assertThat(product.getPrice()).isEqualTo(price);
            assertThat(product.getStockQuantity()).isEqualTo(stockQuantity);
            assertThat(product.getStatus()).isEqualTo(status);
            assertThat(product.getCreatedAt()).isEqualTo(createdAt);
            assertThat(product.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTest {

        @Test
        @DisplayName("create() 메서드로 유효한 Product를 생성한다")
        void create_WithValidData_CreatesProduct() {
            // given
            String name = "Laptop";
            String description = "High-end gaming laptop";
            BigDecimal price = new BigDecimal("2000000");
            Integer stockQuantity = 50;

            // when
            Product product = Product.create(name, description, price, stockQuantity);

            // then
            assertThat(product.getName()).isEqualTo(name);
            assertThat(product.getDescription()).isEqualTo(description);
            assertThat(product.getPrice()).isEqualTo(price);
            assertThat(product.getStockQuantity()).isEqualTo(stockQuantity);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("create() 메서드는 name 검증을 수행한다")
        void create_ValidatesName() {
            // when & then
            assertThatThrownBy(() -> Product.create("", "desc", BigDecimal.TEN, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name cannot be empty");
        }

        @Test
        @DisplayName("create() 메서드는 price 검증을 수행한다")
        void create_ValidatesPrice() {
            // when & then
            assertThatThrownBy(() -> Product.create("Product", "desc", new BigDecimal("-100"), 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product price must be zero or greater");
        }

        @Test
        @DisplayName("create() 메서드는 stockQuantity 검증을 수행한다")
        void create_ValidatesStockQuantity() {
            // when & then
            assertThatThrownBy(() -> Product.create("Product", "desc", BigDecimal.TEN, -5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stock quantity must be zero or greater");
        }
    }

    @Nested
    @DisplayName("name 검증 테스트")
    class NameValidationTest {

        @Test
        @DisplayName("유효한 name은 검증을 통과한다")
        void validateName_WithValidName_Passes() {
            // given
            Product product = new Product();
            product.setName("Valid Product Name");

            // when & then
            assertThatCode(() -> product.validateName()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null name은 예외를 발생시킨다")
        void validateName_WithNull_ThrowsException() {
            // given
            Product product = new Product();
            product.setName(null);

            // when & then
            assertThatThrownBy(() -> product.validateName())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name cannot be empty");
        }

        @Test
        @DisplayName("200자를 초과하는 name은 예외를 발생시킨다")
        void validateName_WithTooLongName_ThrowsException() {
            // given
            Product product = new Product();
            product.setName("a".repeat(201));

            // when & then
            assertThatThrownBy(() -> product.validateName())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name must not exceed 200 characters");
        }
    }

    @Nested
    @DisplayName("재고 관리 테스트")
    class StockManagementTest {

        @Test
        @DisplayName("재고를 증가시킨다")
        void increaseStock_IncreasesQuantity() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);
            int increaseAmount = 20;

            // when
            product.increaseStock(increaseAmount);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(30);
        }

        @Test
        @DisplayName("품절 상태에서 재고 증가 시 ACTIVE 상태로 변경된다")
        void increaseStock_FromOutOfStock_ChangesToActive() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 0);
            product.setStatus(ProductStatus.OUT_OF_STOCK);

            // when
            product.increaseStock(10);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(10);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("재고를 감소시킨다")
        void decreaseStock_DecreasesQuantity() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 30);

            // when
            product.decreaseStock(10);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(20);
        }

        @Test
        @DisplayName("재고 감소로 0이 되면 OUT_OF_STOCK 상태로 변경된다")
        void decreaseStock_ToZero_ChangesToOutOfStock() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);

            // when
            product.decreaseStock(10);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(0);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
        }

        @Test
        @DisplayName("재고 부족 시 예외가 발생한다")
        void decreaseStock_WithInsufficientStock_ThrowsException() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 5);

            // when & then
            assertThatThrownBy(() -> product.decreaseStock(10))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("0 이하의 수량으로 증가 시 예외가 발생한다")
        void increaseStock_WithNonPositive_ThrowsException() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);

            // when & then
            assertThatThrownBy(() -> product.increaseStock(0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> product.increaseStock(-5))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("가격 변경 테스트")
    class PriceChangeTest {

        @Test
        @DisplayName("가격을 변경한다")
        void changePrice_ChangesPrice() {
            // given
            Product product = Product.create("Product", "desc", new BigDecimal("10000"), 10);
            BigDecimal newPrice = new BigDecimal("15000");

            // when
            product.changePrice(newPrice);

            // then
            assertThat(product.getPrice()).isEqualByComparingTo(newPrice);
        }

        @Test
        @DisplayName("음수 가격으로 변경 시 예외가 발생한다")
        void changePrice_WithNegative_ThrowsException() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);

            // when & then
            assertThatThrownBy(() -> product.changePrice(new BigDecimal("-100")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("상태 관리 테스트")
    class StatusManagementTest {

        @Test
        @DisplayName("상품을 활성화한다")
        void activate_ActivatesProduct() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);
            product.deactivate();

            // when
            product.activate();

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("재고가 0일 때 활성화하면 OUT_OF_STOCK 상태가 된다")
        void activate_WithZeroStock_BecomesOutOfStock() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 0);
            product.setStatus(ProductStatus.INACTIVE);

            // when
            product.activate();

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
        }

        @Test
        @DisplayName("단종된 상품은 활성화할 수 없다")
        void activate_DiscontinuedProduct_ThrowsException() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);
            product.discontinue();

            // when & then
            assertThatThrownBy(() -> product.activate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot activate discontinued product");
        }

        @Test
        @DisplayName("상품을 비활성화한다")
        void deactivate_DeactivatesProduct() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);

            // when
            product.deactivate();

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        }

        @Test
        @DisplayName("상품을 단종한다")
        void discontinue_DiscontinuesProduct() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);

            // when
            product.discontinue();

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
        }
    }

    @Nested
    @DisplayName("정보 수정 테스트")
    class UpdateInfoTest {

        @Test
        @DisplayName("상품 정보를 수정한다")
        void updateInfo_UpdatesProductInfo() {
            // given
            Product product = Product.create("Old Name", "Old Desc", BigDecimal.TEN, 10);
            String newName = "New Name";
            String newDescription = "New Description";

            // when
            product.updateInfo(newName, newDescription);

            // then
            assertThat(product.getName()).isEqualTo(newName);
            assertThat(product.getDescription()).isEqualTo(newDescription);
        }

        @Test
        @DisplayName("null name으로 수정 시 기존 name 유지")
        void updateInfo_WithNullName_KeepsOriginalName() {
            // given
            Product product = Product.create("Original Name", "desc", BigDecimal.TEN, 10);
            String originalName = product.getName();

            // when
            product.updateInfo(null, "New Description");

            // then
            assertThat(product.getName()).isEqualTo(originalName);
            assertThat(product.getDescription()).isEqualTo("New Description");
        }
    }

    @Nested
    @DisplayName("상태 확인 메서드 테스트")
    class StatusCheckTest {

        @Test
        @DisplayName("isAvailableForSale()은 ACTIVE 상태이고 재고가 있을 때 true")
        void isAvailableForSale_WithActiveAndStock_ReturnsTrue() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);

            // when & then
            assertThat(product.isAvailableForSale()).isTrue();
        }

        @Test
        @DisplayName("isAvailableForSale()은 재고가 0이면 false")
        void isAvailableForSale_WithZeroStock_ReturnsFalse() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 0);

            // when & then
            assertThat(product.isAvailableForSale()).isFalse();
        }

        @Test
        @DisplayName("isAvailableForSale()은 INACTIVE 상태면 false")
        void isAvailableForSale_WithInactive_ReturnsFalse() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);
            product.deactivate();

            // when & then
            assertThat(product.isAvailableForSale()).isFalse();
        }

        @Test
        @DisplayName("hasStock()은 재고가 있을 때 true")
        void hasStock_WithStock_ReturnsTrue() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 1);

            // when & then
            assertThat(product.hasStock()).isTrue();
        }

        @Test
        @DisplayName("isActive()는 ACTIVE 상태일 때 true")
        void isActive_WithActiveStatus_ReturnsTrue() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);

            // when & then
            assertThat(product.isActive()).isTrue();
        }

        @Test
        @DisplayName("isDiscontinued()는 DISCONTINUED 상태일 때 true")
        void isDiscontinued_WithDiscontinuedStatus_ReturnsTrue() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 10);
            product.discontinue();

            // when & then
            assertThat(product.isDiscontinued()).isTrue();
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("상품 생성 후 판매 및 재고 관리 시나리오")
        void productLifecycleScenario() {
            // 1. 상품 생성
            Product product = Product.create("Laptop", "Gaming Laptop", new BigDecimal("2000000"), 100);
            assertThat(product.isAvailableForSale()).isTrue();

            // 2. 판매로 재고 감소
            product.decreaseStock(30);
            assertThat(product.getStockQuantity()).isEqualTo(70);
            assertThat(product.isAvailableForSale()).isTrue();

            // 3. 재입고
            product.increaseStock(50);
            assertThat(product.getStockQuantity()).isEqualTo(120);

            // 4. 가격 변경
            product.changePrice(new BigDecimal("1800000"));
            assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("1800000"));
        }

        @Test
        @DisplayName("재고 소진 후 품절 처리 시나리오")
        void outOfStockScenario() {
            // given
            Product product = Product.create("Product", "desc", BigDecimal.TEN, 5);

            // when: 재고 모두 소진
            product.decreaseStock(5);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(0);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
            assertThat(product.isAvailableForSale()).isFalse();

            // when: 재입고
            product.increaseStock(10);

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(product.isAvailableForSale()).isTrue();
        }

        @Test
        @DisplayName("상품 단종 시나리오")
        void discontinueScenario() {
            // given
            Product product = Product.create("Old Model", "desc", BigDecimal.TEN, 10);

            // when: 상품 단종
            product.discontinue();

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
            assertThat(product.isAvailableForSale()).isFalse();

            // 단종된 상품은 활성화할 수 없음
            assertThatThrownBy(() -> product.activate())
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
