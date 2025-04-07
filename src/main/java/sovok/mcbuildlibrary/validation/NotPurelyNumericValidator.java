package sovok.mcbuildlibrary.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator implementation for the @NotPurelyNumeric annotation.
 * Checks if a String consists entirely of digits.
 */
public class NotPurelyNumericValidator implements ConstraintValidator<NotPurelyNumeric, String> {

    // Regex to match strings containing only digits (one or more)
    private static final Pattern PURELY_NUMERIC_PATTERN = Pattern.compile("^\\d+$");

    @Override
    public void initialize(NotPurelyNumeric constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null or empty values are considered valid by this specific constraint.
        // Use @NotBlank or @NotEmpty alongside this annotation if null/empty are not allowed.
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        // If the value matches the purely numeric pattern, it's INVALID.
        return !PURELY_NUMERIC_PATTERN.matcher(value).matches();
    }
}