package com.cloudforge.invoice.delivery.service;

import com.cloudforge.invoice.delivery.enumeration.VerificationType;

public interface EmailService {
    void sendVerificationEmail(String firstName, String email, String verificationUrl,
                               VerificationType verificationType);
}
