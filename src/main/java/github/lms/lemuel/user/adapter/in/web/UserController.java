package github.lms.lemuel.user.adapter.in.web;

import github.lms.lemuel.user.adapter.in.web.request.CreateUserRequest;
import github.lms.lemuel.user.adapter.in.web.response.UserResponse;
import github.lms.lemuel.user.application.port.in.CreateUserUseCase;
import github.lms.lemuel.user.application.port.in.GetUserUseCase;
import github.lms.lemuel.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User API Controller
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = createUserUseCase.createUser(
                new CreateUserUseCase.CreateUserCommand(request.getEmail(), request.getPassword())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = getUserUseCase.getUserById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
