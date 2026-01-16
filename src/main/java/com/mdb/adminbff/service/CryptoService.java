package com.mdb.adminbff.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String encryptPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public byte[] encryptPgp(byte[] data, String publicKey) {
        // PGP encryption logic would go here, usually requires Bouncy Castle
        throw new UnsupportedOperationException("PGP Encryption requires Bouncy Castle dependency");
    }

    public byte[] decryptPgp(byte[] encryptedData, String privateKey, String passPhrase) {
        // PGP decryption logic
        throw new UnsupportedOperationException("PGP Decryption requires Bouncy Castle dependency");
    }
}
