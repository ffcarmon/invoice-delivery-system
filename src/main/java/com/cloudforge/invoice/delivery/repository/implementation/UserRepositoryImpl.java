package com.cloudforge.invoice.delivery.repository.implementation;

import com.cloudforge.invoice.delivery.domain.Role;
import com.cloudforge.invoice.delivery.domain.User;
import com.cloudforge.invoice.delivery.domain.UserPrincipal;
import com.cloudforge.invoice.delivery.dto.UserDTO;
import com.cloudforge.invoice.delivery.enumeration.VerificationType;
import com.cloudforge.invoice.delivery.exception.ApiException;
import com.cloudforge.invoice.delivery.form.UpdateForm;
import com.cloudforge.invoice.delivery.repository.RoleRepository;
import com.cloudforge.invoice.delivery.repository.UserRepository;
import com.cloudforge.invoice.delivery.rowmapper.UserRowMapper;
import com.cloudforge.invoice.delivery.utils.SmsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.cloudforge.invoice.delivery.query.UserQuery.*;
import static com.cloudforge.invoice.delivery.utils.SmsUtils.sendSMS;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Map.of;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

import com.cloudforge.invoice.delivery.service.*;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.time.DateFormatUtils.format;
import static com.cloudforge.invoice.delivery.constant.Constants.DATE_FORMAT;
import static com.cloudforge.invoice.delivery.enumeration.RoleType.ROLE_USER;
import static com.cloudforge.invoice.delivery.enumeration.VerificationType.ACCOUNT;
import static com.cloudforge.invoice.delivery.enumeration.VerificationType.PASSWORD;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImpl implements UserRepository<User>, UserDetailsService {
    private final NamedParameterJdbcTemplate jdbc;
    private final RoleRepository<Role> roleRepository;
    private final BCryptPasswordEncoder encoder;
    private final EmailService emailService;
    //private final SmsUtils smsUtils;

    @Override
    public User create(User user) {
        log.info("Creating user with email: {}", user.getEmail());
        if (getEmailCount(user.getEmail().trim().toLowerCase()) > 0) {
            log.warn("Email already in use: {}", user.getEmail());
            throw new ApiException("Email already in use. Please use a different email and try again.");
        }
        try {
            KeyHolder holder = new GeneratedKeyHolder();
            SqlParameterSource parameters = getSqlParameterSource(user);
            jdbc.update(INSERT_USER_QUERY, parameters, holder);
            Map<String, Object> keys = holder.getKeys();
            if (keys != null) {
                user.setId(((Number) keys.get("id")).longValue());
                log.info("User created with ID: {}", user.getId());
            } else {
                throw new ApiException("User ID not generated.");
            }

            roleRepository.addRoleToUser(user.getId(), ROLE_USER.name());
            String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), ACCOUNT.getType());
            jdbc.update(INSERT_ACCOUNT_VERIFICATION_URL_QUERY, of(
                    "userId", user.getId(),
                    "url", verificationUrl,
                    "date", LocalDate.now()
                    )
            );
            sendEmail(user.getFirstName(), user.getEmail(), verificationUrl, ACCOUNT);

            user.setEnabled(false);
            user.setNotLocked(true);
            log.info("User creation completed: {}", user);
            return user;
        } catch (Exception exception) {
            log.error("Error creating user: {}", exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Collection<User> list(int page, int pageSize) {
        return null;
    }

    @Override
    public User get(Long id) {
        log.info("Fetching user with ID: {}", id);
        try {
            User user = jdbc.queryForObject(SELECT_USER_BY_ID_QUERY, of("id", id), new UserRowMapper());
            log.info("User fetched successfully: {}", user);
            return user;
        } catch (EmptyResultDataAccessException exception) {
            log.warn("No User found by ID: {}", id);
            throw new ApiException("No User found by id: " + id);
        } catch (Exception exception) {
            log.error("Error fetching user: {}", exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User update(User data) {
        return null;
    }

    @Override
    public Boolean delete(Long id) {
        return null;
    }

    private Integer getEmailCount(String email) {
        return jdbc.queryForObject(COUNT_USER_EMAIL_QUERY, of("email", email), Integer.class);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = getUserByEmail(email);
        if (user == null) {
            log.error("User not found in the database");
            throw new UsernameNotFoundException("User not found in the database");
        } else {
            log.info("User found in the database: {}", email);
            return new UserPrincipal(user, roleRepository.getRoleByUserId(user.getId()));
        }
    }

    @Override
    public User getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        try {
            User user = jdbc.queryForObject(SELECT_USER_BY_EMAIL_QUERY, of("email", email), new UserRowMapper());
            log.info("User fetched successfully: {}", user);
            return user;
        } catch (EmptyResultDataAccessException exception) {
            log.warn("No User found by email: {}", email);
            throw new ApiException("No User found by email: " + email);
        } catch (Exception exception) {
            log.error("Error fetching user by email: {}", exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void sendVerificationCode(UserDTO user) {
        log.info("Generating verification code for user ID: {}", user.getId());
        String verificationCode = randomAlphabetic(8).toUpperCase();
        try {
            jdbc.update(DELETE_VERIFICATION_CODE_BY_USER_ID, of("id", user.getId()));
            jdbc.update(INSERT_VERIFICATION_CODE_QUERY, of("userId", user.getId(), "code", verificationCode));
            sendSMS(user.getPhone(), "From: SecureCapita \nVerification code\n" + verificationCode);
            log.info("Verification Code generated: {}", verificationCode);
        } catch (Exception exception) {
            log.error("Error generating verification code: {}", exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User verifyCode(String email, String code) {
        if (isVerificationCodeExpired(code)) throw new ApiException("This code has expired. Please login again.");
        try {
            User userByCode = jdbc.queryForObject(SELECT_USER_BY_USER_CODE_QUERY, of("code", code), new UserRowMapper());
            User userByEmail = jdbc.queryForObject(SELECT_USER_BY_EMAIL_QUERY, of("email", email), new UserRowMapper());
            if (userByCode.getEmail().equalsIgnoreCase(userByEmail.getEmail())) {
                jdbc.update(DELETE_CODE, of("code", code));
                return userByCode;
            } else {
                throw new ApiException("Code is invalid. Please try again.");
            }
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException("Could not find record");
        } catch (Exception exception) {
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void resetPassword(String email) {
        if (getEmailCount(email.trim().toLowerCase()) <= 0)
            throw new ApiException("There is no account for this email address.");
        try {
            String expirationDate = format(addDays(new Date(), 1), DATE_FORMAT);
            User user = getUserByEmail(email);
            String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), PASSWORD.getType());
            jdbc.update(DELETE_PASSWORD_VERIFICATION_BY_USER_ID_QUERY, of("userId", user.getId()));
            jdbc.update(INSERT_PASSWORD_VERIFICATION_QUERY, of("userId", user.getId(), "url", verificationUrl, "expirationDate", expirationDate));
            sendEmail(user.getFirstName(), email, verificationUrl, PASSWORD);
            log.info("Verification URL: {}", verificationUrl);
        } catch (Exception exception) {
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User verifyPasswordKey(String key) {
        if (isLinkExpired(key, PASSWORD))
            throw new ApiException("This link has expired. Please reset your password again.");
        try {
            User user = jdbc.queryForObject(SELECT_USER_BY_PASSWORD_URL_QUERY, of("url", getVerificationUrl(key, PASSWORD.getType())), new UserRowMapper());
            //jdbc.update("DELETE_USER_FROM_PASSWORD_VERIFICATION_QUERY", of("id", user.getId())); //Depends on use case / developer or business
            return user;
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("This link is not valid. Please reset your password again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void renewPassword(String key, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) throw new ApiException("Passwords don't match. Please try again.");
        try {
            jdbc.update(UPDATE_USER_PASSWORD_BY_URL_QUERY, of("password", encoder.encode(password), "url", getVerificationUrl(key, PASSWORD.getType())));
            jdbc.update(DELETE_VERIFICATION_BY_URL_QUERY, of("url", getVerificationUrl(key, PASSWORD.getType())));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void renewPassword(Long userId, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) throw new ApiException("Passwords don't match. Please try again.");
        try {
            jdbc.update(UPDATE_USER_PASSWORD_BY_USER_ID_QUERY, of("id", userId, "password", encoder.encode(password)));
            //jdbc.update(DELETE_PASSWORD_VERIFICATION_BY_USER_ID_QUERY, of("userId", userId));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User verifyAccountKey(String key) {
        log.info("Verifying account with key: {}", key);
        try {
            String verificationUrl = getVerificationUrl(key, ACCOUNT.getType());
            log.debug("Generated verification URL: {}", verificationUrl);

            User user = jdbc.queryForObject(
                    SELECT_USER_BY_ACCOUNT_URL_QUERY,
                    of("url", verificationUrl),
                    new UserRowMapper()
            );

            log.info("User found for verification. ID: {}, Email: {}", user.getId(), user.getEmail());

            jdbc.update(UPDATE_USER_ENABLED_QUERY, of("enabled", true, "id", user.getId()));
            log.info("User account enabled successfully. ID: {}", user.getId());

            // Delete after updating - depends on your requirements
            return user;
        } catch (EmptyResultDataAccessException exception) {
            log.warn("Invalid verification link used: {}", key);
            throw new ApiException("This link is not valid.");
        } catch (Exception exception) {
            log.error("Error verifying account for key: {}. Exception: {}", key, exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }


    @Override
    public User updateUserDetails(UpdateForm user) {
        try {
            jdbc.update(UPDATE_USER_DETAILS_QUERY, getUserDetailsSqlParameterSource(user));
            return get(user.getId());
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException("No User found by id: " + user.getId());
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void updatePassword(Long id, String currentPassword, String newPassword, String confirmNewPassword) {
        log.info("Updating password for user ID: {}", id);
        if (!newPassword.equals(confirmNewPassword)) {
            log.warn("Passwords do not match for user ID: {}", id);
            throw new ApiException("Passwords don't match. Please try again.");
        }
        User user = get(id);
        if (encoder.matches(currentPassword, user.getPassword())) {
            try {
                jdbc.update(UPDATE_USER_PASSWORD_BY_ID_QUERY, of("userId", id, "password", encoder.encode(newPassword)));
                log.info("Password updated successfully for user ID: {}", id);
            } catch (Exception exception) {
                log.error("Error updating password: {}", exception.getMessage());
                throw new ApiException("An error occurred. Please try again.");
            }
        } else {
            log.warn("Incorrect current password for user ID: {}", id);
            throw new ApiException("Incorrect current password. Please try again.");
        }
    }

    @Override
    public void updateAccountSettings(Long userId, Boolean enabled, Boolean notLocked) {
        try {
            jdbc.update(UPDATE_USER_SETTINGS_QUERY, of("userId", userId, "enabled", enabled, "notLocked", notLocked));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User toggleMfa(String email) {
        User user = getUserByEmail(email);
        if (isBlank(user.getPhone())) {
            throw new ApiException("You need a phone number to change Multi-Factor Authentication");
        }
        user.setUsingMfa(!user.isUsingMfa());
        try {
            jdbc.update(TOGGLE_USER_MFA_QUERY, of("email", email, "isUsingMfa", user.isUsingMfa()));
            return user;
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Unable to update Multi-Factor Authentication");
        }
    }

    @Override
    public void updateImage(UserDTO user, MultipartFile image) {
        log.info("Updating image for user: {}", user.getEmail());
        String userImageUrl = setUserImageUrl(user.getEmail());
        user.setImageUrl(userImageUrl);
        saveImage(user.getEmail(), image);
        jdbc.update(UPDATE_USER_IMAGE_QUERY, of("imageUrl", userImageUrl, "id", user.getId()));
        log.info("Successfully updated image for user: {}", user.getEmail());
    }

    private void sendEmail(String firstName, String email, String verificationUrl, VerificationType verificationType) {
        log.info("*********************************");
        log.info("Sending email to: {}", email);
        log.info("*********************************");
        CompletableFuture.runAsync(() -> emailService.sendVerificationEmail(firstName, email, verificationUrl, verificationType));

        /*CompletableFuture.runAsync(() -> {
            try {
                emailService.sendVerificationEmail(firstName, email, verificationUrl, verificationType);
            } catch (Exception exception) {
                throw new ApiException("Unable to send email");
            }
        });*/

        /*CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                emailService.sendVerificationEmail(firstName, email, verificationUrl, verificationType);
            } catch (Exception exception) {
                throw new ApiException("Unable to send email");
            }
        });*/

        /*CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    emailService.sendVerificationEmail(firstName, email, verificationUrl, verificationType);
                } catch (Exception exception) {
                    throw new ApiException("Unable to send email");
                }
            }
        });*/
    }

    private String setUserImageUrl(String email) {
        return fromCurrentContextPath().path("/user/image/" + email + ".png").toUriString();
    }

    private void saveImage(String email, MultipartFile image) {
        log.info("Saving image for user: {}", email);
        Path fileStorageLocation = Paths.get(System.getProperty("user.home") + "/Downloads/images/").toAbsolutePath().normalize();
        if (!Files.exists(fileStorageLocation)) {
            try {
                Files.createDirectories(fileStorageLocation);
                log.info("Created directories: {}", fileStorageLocation);
            } catch (Exception exception) {
                log.error("Error creating directories: {}", exception.getMessage());
                throw new ApiException("Unable to create directories to save image");
            }
        }
        try {
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(email + ".png"), REPLACE_EXISTING);
            log.info("File saved successfully in: {} folder", fileStorageLocation);
        } catch (IOException exception) {
            log.error("Error saving image: {}", exception.getMessage());
            throw new ApiException(exception.getMessage());
        }
    }

    private Boolean isLinkExpired(String key, VerificationType password) {
        try {
            return jdbc.queryForObject(SELECT_EXPIRATION_BY_URL, of("url", getVerificationUrl(key, password.getType())), Boolean.class);
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("This link is not valid. Please reset your password again");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again");
        }
    }

    private Boolean isVerificationCodeExpired(String code) {
        try {
            return jdbc.queryForObject(SELECT_CODE_EXPIRATION_QUERY, of("code", code), Boolean.class);
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException("This code is not valid. Please login again.");
        } catch (Exception exception) {
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    private SqlParameterSource getSqlParameterSource(User user) {
        return new MapSqlParameterSource()
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("email", user.getEmail())
                .addValue("password", encoder.encode(user.getPassword()));
    }

    private SqlParameterSource getUserDetailsSqlParameterSource(UpdateForm user) {
        return new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("email", user.getEmail())
                .addValue("phone", user.getPhone())
                .addValue("address", user.getAddress())
                .addValue("title", user.getTitle())
                .addValue("bio", user.getBio());
    }

    private String getVerificationUrl(String key, String type) {
        return fromCurrentContextPath().path("/user/verify/" + type + "/" + key).toUriString();
    }
}



















