package com.cloudforge.invoice.delivery.service;

import com.cloudforge.invoice.delivery.domain.User;
import com.cloudforge.invoice.delivery.dto.UserDTO;
import com.cloudforge.invoice.delivery.form.UpdateForm;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserDTO createUser(User user);
    UserDTO getUserByEmail(String email);
    void sendVerificationCode(UserDTO user);
    UserDTO verifyCode(String email, String code);
    void resetPassword(String email);
    UserDTO verifyPasswordKey(String key);
    void updatePassword(Long userId, String password, String confirmPassword);
    UserDTO verifyAccountKey(String key);
    UserDTO updateUserDetails(UpdateForm user);
    UserDTO getUserById(Long userId);
    void updatePassword(Long userId, String currentPassword, String newPassword, String confirmNewPassword);
    void updateUserRole(Long userId, String roleName);
    void updateAccountSettings(Long userId, Boolean enabled, Boolean notLocked);
    UserDTO toggleMfa(String email);
    void updateImage(UserDTO user, MultipartFile image);
}
