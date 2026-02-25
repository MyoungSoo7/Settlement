package github.lms.lemuel.product.adapter.in.web;

import github.lms.lemuel.product.adapter.in.web.request.CreateProductRequest;
import github.lms.lemuel.product.adapter.in.web.request.UpdateProductInfoRequest;
import github.lms.lemuel.product.adapter.in.web.request.UpdateProductPriceRequest;
import github.lms.lemuel.product.adapter.in.web.request.UpdateProductStockRequest;
import github.lms.lemuel.product.adapter.in.web.response.ProductResponse;
import github.lms.lemuel.product.application.port.in.CreateProductUseCase;
import github.lms.lemuel.product.application.port.in.GetProductUseCase;
import github.lms.lemuel.product.application.port.in.ManageProductStatusUseCase;
import github.lms.lemuel.product.application.port.in.UpdateProductUseCase;
import github.lms.lemuel.product.application.port.in.UpdateProductUseCase.UpdateProductInfoCommand;
import github.lms.lemuel.product.application.port.in.UpdateProductUseCase.UpdateProductPriceCommand;
import github.lms.lemuel.product.application.port.in.UpdateProductUseCase.UpdateProductStockCommand;
import github.lms.lemuel.product.domain.Product;
import github.lms.lemuel.product.domain.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final ManageProductStatusUseCase manageProductStatusUseCase;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody CreateProductRequest request) {
        Product product = createProductUseCase.createProduct(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        Product product = getProductUseCase.getProductById(productId);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<Product> products = getProductUseCase.getAllProducts();
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProductResponse>> getProductsByStatus(@PathVariable ProductStatus status) {
        List<Product> products = getProductUseCase.getProductsByStatus(status);
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/available")
    public ResponseEntity<List<ProductResponse>> getAvailableProducts() {
        List<Product> products = getProductUseCase.getAvailableProducts();
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{productId}/info")
    public ResponseEntity<ProductResponse> updateProductInfo(
            @PathVariable Long productId,
            @RequestBody UpdateProductInfoRequest request) {
        UpdateProductInfoCommand command = new UpdateProductInfoCommand(
                productId, request.name(), request.description());
        Product product = updateProductUseCase.updateProductInfo(command);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PutMapping("/{productId}/price")
    public ResponseEntity<ProductResponse> updateProductPrice(
            @PathVariable Long productId,
            @RequestBody UpdateProductPriceRequest request) {
        UpdateProductPriceCommand command = new UpdateProductPriceCommand(
                productId, request.newPrice());
        Product product = updateProductUseCase.updateProductPrice(command);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PutMapping("/{productId}/stock")
    public ResponseEntity<ProductResponse> updateProductStock(
            @PathVariable Long productId,
            @RequestBody UpdateProductStockRequest request) {
        UpdateProductStockCommand command = new UpdateProductStockCommand(
                productId, request.quantity(), request.operation());
        Product product = updateProductUseCase.updateProductStock(command);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PostMapping("/{productId}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable Long productId) {
        Product product = manageProductStatusUseCase.activateProduct(productId);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PostMapping("/{productId}/deactivate")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable Long productId) {
        Product product = manageProductStatusUseCase.deactivateProduct(productId);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PostMapping("/{productId}/discontinue")
    public ResponseEntity<ProductResponse> discontinueProduct(@PathVariable Long productId) {
        Product product = manageProductStatusUseCase.discontinueProduct(productId);
        return ResponseEntity.ok(ProductResponse.from(product));
    }
}
