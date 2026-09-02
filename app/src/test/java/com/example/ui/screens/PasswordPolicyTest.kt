package com.example.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {
    @Test fun acceptsAnyEnglishLetterCase() {
        assertTrue(passwordPolicySatisfied("uiui@9600"))
        assertTrue(passwordPolicySatisfied("UIUI@9600"))
        assertTrue(passwordPolicySatisfied("Uiui@9600"))
    }

    @Test fun rejectsMissingRequiredCharacterGroups() {
        assertFalse(passwordPolicySatisfied("uiui9600"))
        assertFalse(passwordPolicySatisfied("uiui@@@@"))
        assertFalse(passwordPolicySatisfied("9600@@@@"))
    }

    @Test fun rejectsInvalidLengthAndWhitespace() {
        assertFalse(passwordPolicySatisfied("ui@960"))
        assertFalse(passwordPolicySatisfied("abcdefghijklmnop1@"))
        assertFalse(passwordPolicySatisfied("uiui @9600"))
    }
}
