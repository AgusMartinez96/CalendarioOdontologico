package com.api.agenda_odontologica.api.security;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lista embebida de contraseñas comunes (en minúsculas) que se rechazan al registrarse. */
public final class CommonPasswords {
    private static final Set<String> COMMON = new HashSet<>(List.of(
            "1234567890", "12345678910", "123456789012", "0123456789", "1111111111", "0000000000",
            "1234512345", "12341234", "1q2w3e4r5t", "1qaz2wsx3edc", "qwertyuiop", "qwertyuiop123",
            "qwerty1234", "qwerty12345", "qwertyasdfgh", "asdfghjkl", "asdfghjkl1", "asdfghjkl123",
            "zxcvbnm123", "zxcvbnmasdf", "password1", "password12", "password123", "password1234",
            "password12345", "passw0rd123", "p@ssw0rd123", "p@ssword123", "contrasena", "contrasena1",
            "contrasena12", "contrasena123", "contrasena1234", "contraseña", "contraseña1",
            "contraseña12", "contraseña123", "mipassword", "mipassword1", "mipassword123", "micontrasena",
            "micontraseña", "clave12345", "clave123456", "mi clave123", "miclave123", "miclave1234",
            "administrador", "administrador1", "administrador123", "admin12345", "admin123456",
            "adminadmin", "adminadmin1", "admin1234567", "administrator", "iloveyou123", "iloveyou12",
            "iloveyou1234", "teamo12345", "teamo123456", "tequiero123", "tequiero1234", "letmein123",
            "letmein1234", "welcome123", "welcome1234", "welcome12345", "changeme123", "changeme1234",
            "abc1234567", "abcd123456", "abcd1234abcd", "abcdefghij", "abcdefghijk", "abcdefg123",
            "football123", "futbol12345", "boca123456", "riverplate", "riverplate1", "bocajuniors",
            "argentina123", "argentina1", "argentina12", "buenosaires", "buenosaires1", "monkey1234",
            "dragon12345", "sunshine123", "princess123", "superman123", "batman12345", "starwars123",
            "master12345", "trustno1234", "baseball123", "shadow12345", "michael1234", "jennifer123",
            "daniel12345", "agenda1234", "agenda12345", "odontologia", "odontologia1", "odontologia123",
            "dentista123", "dentista1234", "consultorio", "consultorio1", "consultorio123",
            "pacientes123", "turnos12345", "usuario1234", "usuario12345", "usuario123456",
            "123456789a", "a123456789", "123456abcd", "abcd123456", "q1w2e3r4t5", "q1w2e3r4t5y6",
            "1234qwer1234", "qwer123456", "asdf123456", "zaq12wsxcde3", "passwordpassword",
            "0987654321", "9876543210", "1029384756", "147258369", "147258369a", "741852963a"));

    private CommonPasswords() {
    }

    public static boolean contains(String password) {
        return COMMON.contains(password.toLowerCase(Locale.ROOT));
    }
}
