package com.serverInfrastructure.service;

import com.serverDomain.service.PasswordHasher;
import org.mindrot.jbcrypt.BCrypt;

public class BcryptPasswordHasher implements PasswordHasher {

    @Override
    public String hash(String password) {
        // BCrypt.gensalt() genera un "salt" aleatorio para cada hash
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    @Override
    public boolean check(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            // Maneja el caso en que el hash almacenado no sea válido
            return false;
        }
    }
}