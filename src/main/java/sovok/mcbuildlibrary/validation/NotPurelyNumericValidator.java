package sovok.mcbuildlibrary.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NotPurelyNumericValidator implements ConstraintValidator<NotPurelyNumeric, String> {

    private static final Pattern PURELY_NUMERIC_PATTERN = Pattern.compile("^\\d+$");

    @Override
    public void initialize(NotPurelyNumeric constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return !PURELY_NUMERIC_PATTERN.matcher(value).matches();
    }
}