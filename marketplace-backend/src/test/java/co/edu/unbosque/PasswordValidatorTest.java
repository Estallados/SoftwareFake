package co.edu.unbosque;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unbosque.marketplace.exception.PasswordNotValidException;
import co.edu.unbosque.marketplace.util.PasswordValidator;

import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

  @Test
  void acceptsStrongPassword() {
    assertTrue(PasswordValidator.isValid("Segura#2026"));
  }

  @Test
  void rejectsWeakPasswords() {
    assertFalse(PasswordValidator.isValid(null));
    assertFalse(PasswordValidator.isValid("Ab1#"));
    assertFalse(PasswordValidator.isValid("sinmayuscula#1"));
    assertFalse(PasswordValidator.isValid("SINMINUSCULA#1"));
    assertFalse(PasswordValidator.isValid("SinNumero#abc"));
    assertFalse(PasswordValidator.isValid("SinSimbolo123"));
  }

  @Test
  void validateThrows() {
    assertThrows(PasswordNotValidException.class, () -> PasswordValidator.validate("12345678"));
  }
}
