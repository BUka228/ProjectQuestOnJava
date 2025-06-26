package com.example.projectquestonjava.core.data.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.projectquestonjava.core.domain.security.PasswordHasher;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCrypt.Hasher bcryptHasher;
    private final BCrypt.Verifyer bcryptVerifier;

    @Inject
    public BCryptPasswordHasher() {
        this.bcryptHasher = BCrypt.withDefaults();
        this.bcryptVerifier = BCrypt.verifyer();
    }

    @Override
    public String hash(String password) {
        return bcryptHasher.hashToString(12, password.toCharArray());
    }

    @Override
    public boolean verify(String password, String hash) {
        if (password == null || hash == null) {
            return false; // или бросить IllegalArgumentException
        }
        return bcryptVerifier.verify(password.toCharArray(), hash.toCharArray()).verified;
    }
}