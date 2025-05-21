package com.example.projectquestonjava.core.domain.security;

public interface PasswordHasher {
    String hash(String password);
    boolean verify(String password, String hash);
}