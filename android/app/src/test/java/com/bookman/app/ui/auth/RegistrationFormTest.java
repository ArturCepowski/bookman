package com.bookman.app.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bookman.app.ui.common.FieldError;
import org.junit.Test;

public class RegistrationFormTest {

    @Test
    public void REQ01_validFormIsNormalized() {
        RegistrationForm form = RegistrationForm.of("  Anna@Example.COM ", "secret123", "+48 600-100 200");

        assertTrue(form.isValid());
        assertEquals("anna@example.com", form.email);
        assertEquals("+48600100200", form.phoneNumber);
    }

    @Test
    public void REQ01_phoneMustBeE164() {
        assertEquals(FieldError.PHONE_INVALID, RegistrationForm.of("a@b.pl", "secret123", "600100200").phoneError);
        assertEquals(FieldError.PHONE_INVALID, RegistrationForm.of("a@b.pl", "secret123", "+0600100200").phoneError);
        assertEquals(FieldError.PHONE_INVALID, RegistrationForm.of("a@b.pl", "secret123", "").phoneError);
        assertEquals(FieldError.PHONE_INVALID,
                RegistrationForm.of("a@b.pl", "secret123", "+4860010020012345").phoneError);
        assertNull(RegistrationForm.of("a@b.pl", "secret123", "+12025550123").phoneError);
    }

    @Test
    public void REQ01_emailAndPasswordAreChecked() {
        RegistrationForm form = RegistrationForm.of("not-an-email", "short", "+48600100200");

        assertFalse(form.isValid());
        assertEquals(FieldError.EMAIL_INVALID, form.emailError);
        assertEquals(FieldError.PASSWORD_TOO_SHORT, form.passwordError);
        assertNull(form.phoneError);
    }
}
