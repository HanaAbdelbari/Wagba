package com.wagba.users.verification;

/** Names must match the CHECK constraint in verification_challenges. */
public enum ChallengePurpose {
    EmailVerification,
    PhoneVerification,
    PasswordReset
}