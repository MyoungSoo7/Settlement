package github.lms.lemuel.product.application.service;

import github.lms.lemuel.product.application.port.in.CreateProductUseCase.CreateProductCommand;
import github.lms.lemuel.product.application.port.out.LoadProductPort;
import github.lms.lemuel.product.application.port.out.SaveProductPort;
import github.lms.lemuel.product.domain.Product;
import github.lms.lemuel.product.domain.ProductStatus;
import github.lms.lemuel.product.domain.exception.DuplicateProductNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * CreateProductService TDD Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductService 애플리케이션 서비스")
class CreateProductServiceTest {

    @Mock
    private LoadProductPort loadProductPort;

    @Mock
    private SaveProductPort saveProductPort;

    @InjectMocks
    private CreateProductService createProductService;

    @Nested
    @DisplayName("상품 생성 성공 시나리오")
    class SuccessScenario {

        @Test
        @DisplayName("유효한 정보로 상품을 생성한다")
        void createProduct_WithValidData_CreatesProduct() {
            // given
            String name = "Laptop";
            String description = "Gaming Laptop";
            BigDecimal price = new BigDecimal("2000000");
            Integer stockQuantity = 50;
            CreateProductCommand command = new CreateProductCommand(name, description, price, stockQuantity);

            given(loadProductPort.existsByName(name)).willReturn(false);
            given(saveProductPort.save(any(Product.class))).willAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId(100L);
                return product;
            });

            // when
            Product result = createProductService.createProduct(command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getDescription()).isEqualTo(description);
            assertThat(result.getPrice()).isEqualByComparingTo(price);
            assertThat(result.getStockQuantity()).isEqualTo(stockQuantity);
            assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);

            // verify
            then(loadProductPort).should().existsByName(name);
            then(saveProductPort).should().save(any(Product.class));
        }

        @Test
        @DisplayName("생성된 상품은 ACTIVE 상태이다")
        void createProduct_SetsInitialStatusToActive() {
            // given
            CreateProductCommand command = new CreateProductCommand(
                    "Product", "desc", BigDecimal.TEN, 10);

            given(loadProductPort.existsByName(anyString())).willReturn(false);
            given(saveProductPort.save(any(Product.class))).willAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId(1L);
                return product;
            });

            // when
            Product result = createProductService.createProduct(command);

            // then
            assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(result.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("상품명 중복 검증")
    class DuplicateNameValidation {

        @Test
        @DisplayName("이미 존재하는 상품명으로 생성 시 DuplicateProductNameException 발생")
        void createProduct_WithDuplicateName_ThrowsException() {
            // given
            String duplicateName = "Existing Product";
            CreateProductCommand command = new CreateProductCommand(
                    duplicateName, "desc", BigDecimal.TEN, 10);

            given(loadProductPort.existsByName(duplicateName)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> createProductService.createProduct(command))
                    .isInstanceOf(DuplicateProductNameException.class);

            // verify
            then(loadProductPort).should().existsByName(duplicateName);
            then(saveProductPort).should(never()).save(any(Product.class));
        }

        @Test
        @DisplayName("중복되지 않은 상품명은 정상적으로 처리된다")
        void createProduct_WithUniqueName_Succeeds() {
            // given
            String uniqueName = "Unique Product";
            CreateProductCommand command = new CreateProductCommand(
                    uniqueName, "desc", BigDecimal.TEN, 10);

            given(loadProductPort.existsByName(uniqueName)).willReturn(false);
            given(saveProductPort.save(any(Product.class))).willAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId(1L);
                return product;
            });

            // when
            Product result = createProductService.createProduct(command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(uniqueName);
        }
    }

    @Nested
    @DisplayName("도메인 검증 통합")
    class DomainValidationIntegration {

        @Test
        @DisplayName("빈 상품명으로 생성 시 도메인에서 예외 발생")
        void createProduct_WithEmptyName_ThrowsException() {
            // given
            given(loadProductPort.existsByName(anyString())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> new CreateProductCommand("", "desc", BigDecimal.TEN, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Product name cannot be empty");
        }

        @Test
        @DisplayName("음수 가격으로 생성 시 예외 발생")
        void createProduct_WithNegativePrice_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> new CreateProductCommand(
                    "Product", "desc", new BigDecimal("-100"), 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be zero or greater");
        }

        @Test
        @DisplayName("음수 재고로 생성 시 예외 발생")
        void createProduct_WithNegativeStock_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> new CreateProductCommand(
                    "Product", "desc", BigDecimal.TEN, -5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stock quantity must be zero or greater");
        }
    }

    @Nested
    @DisplayName("저장 로직")
    class SaveLogic {

        @Test
        @DisplayName("생성된 상품을 포트를 통해 저장한다")
        void createProduct_SavesProductThroughPort() {
            // given
            String name = "Test Product";
            CreateProductCommand command = new CreateProductCommand(
                    name, "desc", BigDecimal.TEN, 10);

            given(loadProductPort.existsByName(name)).willReturn(false);
            given(saveProductPort.save(any(Product.class))).willAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId(42L);
                return product;
            });

            // when
            Product result = createProductService.createProduct(command);

            // then
            assertThat(result.getId()).isEqualTo(42L);
            then(saveProductPort).should().save(argThat(product ->
                    product.getName().equals(name) &&
                            product.getStatus() == ProductStatus.ACTIVE
            ));
        }
    }

    @Nested
    @DisplayName("전체 통합 시나리오")
    class FullIntegrationScenario {

        @Test
        @DisplayName("상품 생성 전체 플로우: 중복확인 → 도메인생성 → 저장")
        void createProduct_FullFlow_Success() {
            // given
            String name = "Full Flow Product";
            String description = "Test Description";
            BigDecimal price = new BigDecimal("99999.99");
            Integer stockQuantity = 100;
            CreateProductCommand command = new CreateProductCommand(name, description, price, stockQuantity);

            given(loadProductPort.existsByName(name)).willReturn(false);
            given(saveProductPort.save(any(Product.class))).willAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId(777L);
                return product;
            });

            // when
            Product result = createProductService.createProduct(command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(777L);
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getDescription()).isEqualTo(description);
            assertThat(result.getPrice()).isEqualByComparingTo(price);
            assertThat(result.getStockQuantity()).isEqualTo(stockQuantity);
            assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();

            // verify
            var inOrder = inOrder(loadProductPort, saveProductPort);
            inOrder.verify(loadProductPort).existsByName(name);
            inOrder.verify(saveProductPort).save(any(Product.class));
        }
    }
}
