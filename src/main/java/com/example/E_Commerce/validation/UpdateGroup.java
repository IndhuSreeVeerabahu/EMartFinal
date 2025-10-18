package com.example.E_Commerce.validation;

import jakarta.validation.groups.Default;

/**
 * Validation group for user update operations.
 * This allows password to be optional during updates.
 */
public interface UpdateGroup extends Default {
}
