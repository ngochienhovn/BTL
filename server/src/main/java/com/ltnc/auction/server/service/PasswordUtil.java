package com.ltnc.auction.server.service;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    
    /**
     * Hashes a password using BCrypt.
     * BCrypt handles salting automatically.
     */
    public static String hash(String password) {
        if (password == null) return null;
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * Verifies a password against a BCrypt hash.
     */
    public static boolean verify(String password, String hash) {
        if (password == null || hash == null) return false;
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            // In case the hash is old (e.g. plain SHA-256 during migration)
            return false;
        }
    }
}
