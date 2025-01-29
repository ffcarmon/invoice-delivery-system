package com.cloudforge.invoice.delivery.utils;

import com.cloudforge.invoice.delivery.domain.UserPrincipal;
import com.cloudforge.invoice.delivery.dto.UserDTO;
import org.springframework.security.core.Authentication;


public class UserUtils {
    public static UserDTO getAuthenticatedUser(Authentication authentication) {
        return ((UserDTO) authentication.getPrincipal());
    }

    public static UserDTO getLoggedInUser(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUser();
    }
}
