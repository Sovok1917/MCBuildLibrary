package sovok.mcbuildlibrary.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation to ensure a String field does not consist purely of numeric digits.
 * Null or empty values are considered valid by this constraint; combine with @NotBlank if needed.
 */
@Documented
@Constraint(validatedBy = NotPurelyNumericValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType
        .CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotPurelyNumeric {
    String message() default "Name cannot consist only of numbers";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}