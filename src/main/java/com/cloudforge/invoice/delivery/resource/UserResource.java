package com.cloudforge.invoice.delivery.resource;

import com.cloudforge.invoice.delivery.domain.HttpResponse;
import com.cloudforge.invoice.delivery.domain.User;
import com.cloudforge.invoice.delivery.domain.UserPrincipal;
import com.cloudforge.invoice.delivery.dto.UserDTO;
import com.cloudforge.invoice.delivery.event.NewUserEvent;
import com.cloudforge.invoice.delivery.exception.ApiException;
import com.cloudforge.invoice.delivery.form.*;
import com.cloudforge.invoice.delivery.provider.TokenProvider;
import com.cloudforge.invoice.delivery.service.EventService;
import com.cloudforge.invoice.delivery.service.RoleService;
import com.cloudforge.invoice.delivery.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static com.cloudforge.invoice.delivery.dtomapper.UserDTOMapper.toUser;
import static com.cloudforge.invoice.delivery.enumeration.EventType.*;
import static com.cloudforge.invoice.delivery.utils.ExceptionUtils.processError;
import static com.cloudforge.invoice.delivery.utils.UserUtils.getAuthenticatedUser;
import static com.cloudforge.invoice.delivery.utils.UserUtils.getLoggedInUser;
import static java.time.LocalDateTime.now;
import static java.util.Map.of;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.unauthenticated;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

@Slf4j
@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserResource {
    private final UserService userService;
    private final RoleService roleService;
    private final EventService eventService;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ApplicationEventPublisher publisher;

    @PostMapping("/login")
    public ResponseEntity<HttpResponse> login(@RequestBody @Valid LoginForm loginForm) {
        log.info("Attempting login for email: {}", loginForm.getEmail());
        UserDTO user = authenticate(loginForm.getEmail(), loginForm.getPassword());
        log.info("Login {} for email: {}", user.isUsingMfa() ? "requires MFA" : "successful", loginForm.getEmail());
        return user.isUsingMfa() ? sendVerificationCode(user) : sendResponse(user);
    }

    @PostMapping("/register")
    public ResponseEntity<HttpResponse> saveUser(@RequestBody @Valid User user) throws InterruptedException {
        log.info("Registering new user: {} {}", user.getFirstName(), user.getLastName());
        TimeUnit.SECONDS.sleep(4);
        UserDTO userDto = userService.createUser(user);
        log.info("User registered successfully: {}", user.getEmail());
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", userDto))
                        .message(String.format("User account created for user %s", user.getFirstName()))
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build());
    }

    @GetMapping("/profile")
    public ResponseEntity<HttpResponse> profile(Authentication authentication) {
        log.info("Fetching profile for authenticated user.");
        UserDTO user = userService.getUserByEmail(getAuthenticatedUser(authentication).getEmail());
        log.info("Profile retrieved for user: {}", user.getEmail());
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user, "events", eventService.getEventsByUserId(user.getId()), "roles", roleService.getRoles()))
                        .message("Profile Retrieved")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/update")
    public ResponseEntity<HttpResponse> updateUser(@RequestBody @Valid UpdateForm user) {
        log.info("Updating user details for user ID: {}", user.getId());
        UserDTO updatedUser = userService.updateUserDetails(user);
        log.info("User details updated successfully for user ID: {}", user.getId());
        publisher.publishEvent(new NewUserEvent(updatedUser.getEmail(), PROFILE_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", updatedUser, "events", eventService.getEventsByUserId(user.getId()), "roles", roleService.getRoles()))
                        .message("User updated")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    // Additional methods will follow a similar pattern for logging operations.

    private UserDTO authenticate(String email, String password) {
        log.info("Authenticating user with email: {}", email);
        UserDTO userByEmail = userService.getUserByEmail(email);
        try {
            if (userByEmail != null) {
                publisher.publishEvent(new NewUserEvent(email, LOGIN_ATTEMPT));
            }
            Authentication authentication = authenticationManager.authenticate(unauthenticated(email, password));
            UserDTO loggedInUser = getLoggedInUser(authentication);
            if (!loggedInUser.isUsingMfa()) {
                publisher.publishEvent(new NewUserEvent(email, LOGIN_ATTEMPT_SUCCESS));
            }
            log.info("Authentication successful for email: {}", email);
            return loggedInUser;
        } catch (Exception exception) {
            if (userByEmail != null) {
                publisher.publishEvent(new NewUserEvent(email, LOGIN_ATTEMPT_FAILURE));
            }
            log.error("Authentication failed for email: {}. Error: {}", email, exception.getMessage());
            processError(request, response, exception);
            throw new ApiException(exception.getMessage());
        }
    }

    private URI getUri() {
        log.debug("Building URI for new user.");
        return URI.create(fromCurrentContextPath().path("/user/get/<userId>").toUriString());
    }

    private ResponseEntity<HttpResponse> sendResponse(UserDTO user) {
        log.info("Sending response for user: {}", user.getEmail());
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user, "access_token", tokenProvider.createAccessToken(getUserPrincipal(user))
                                , "refresh_token", tokenProvider.createRefreshToken(getUserPrincipal(user))))
                        .message("Login Success")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    private UserPrincipal getUserPrincipal(UserDTO user) {
        log.debug("Getting UserPrincipal for user: {}", user.getEmail());
        return new UserPrincipal(toUser(userService.getUserByEmail(user.getEmail())), roleService.getRoleByUserId(user.getId()));
    }

    private ResponseEntity<HttpResponse> sendVerificationCode(UserDTO user) {
        log.info("Sending verification code to user: {}", user.getEmail());
        userService.sendVerificationCode(user);
        log.info("Verification code sent to user: {}", user.getEmail());
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user))
                        .message("Verification Code Sent")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }
}



















