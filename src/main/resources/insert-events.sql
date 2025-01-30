INSERT INTO Events (type, description) VALUES
                                           ('LOGIN_ATTEMPT', 'User attempted to log in'),
                                           ('LOGIN_ATTEMPT_FAILURE', 'User failed to log in'),
                                           ('LOGIN_ATTEMPT_SUCCESS', 'User successfully logged in'),
                                           ('PROFILE_UPDATE', 'User updated their profile information'),
                                           ('PROFILE_PICTURE_UPDATE', 'User changed their profile picture'),
                                           ('ROLE_UPDATE', 'User role was updated'),
                                           ('ACCOUNT_SETTINGS_UPDATE', 'User updated account settings'),
                                           ('PASSWORD_UPDATE', 'User changed their password'),
                                           ('MFA_UPDATE', 'User updated multi-factor authentication settings');
