package com.goevent.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    @Test
    fun email_acceptsNormalAddresses() {
        assertNull(Validators.emailError("ntombi@example.com"))
        assertNull(Validators.emailError("  first.last+tag@mail.co.za  "))
    }

    @Test
    fun email_rejectsBadAddresses() {
        assertNotNull(Validators.emailError(""))
        assertNotNull(Validators.emailError("not-an-email"))
        assertNotNull(Validators.emailError("a@b"))
    }

    @Test
    fun password_enforcesEachRule() {
        assertNull(Validators.passwordError("Passw0rdX"))
        assertEquals("Use at least 8 characters", Validators.passwordError("Sh0rtA"))
        assertEquals("Add an uppercase letter", Validators.passwordError("alllowercase1"))
        assertEquals("Add a lowercase letter", Validators.passwordError("ALLUPPER123"))
        assertEquals("Add a number", Validators.passwordError("NoDigitsHere"))
    }

    @Test
    fun confirmPassword_mustMatch() {
        assertNull(Validators.confirmPasswordError("Passw0rdX", "Passw0rdX"))
        assertEquals("Passwords do not match", Validators.confirmPasswordError("Passw0rdX", "Passw0rdY"))
    }

    @Test
    fun name_acceptsRealNamesAndRejectsJunk() {
        assertNull(Validators.nameError("Ntombikayise Bontle Dingaan"))
        assertNull(Validators.nameError("O'Neil-Smith"))
        assertNotNull(Validators.nameError("A"))
        assertNotNull(Validators.nameError("John3"))
        assertNotNull(Validators.nameError("   "))
    }

    @Test
    fun sanitiseSearch_removesFilterBreakingCharacters() {
        assertEquals("jazz night 50", Validators.sanitiseSearch("  jazz, (night) %50 "))
        assertEquals(60, Validators.sanitiseSearch("x".repeat(200)).length)
    }
}
