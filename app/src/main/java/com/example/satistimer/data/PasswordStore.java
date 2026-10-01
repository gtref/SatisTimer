package com.example.satistimer.data;

public interface PasswordStore {
    String getPasswordHash();

    void savePasswordHash(String passwordHash);
}
