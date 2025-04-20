// file: src/main/java/sovok/mcbuildlibrary/aspect/LoggingAspect.java
package sovok.mcbuildlibrary.aspect;

import java.util.Arrays;
// Removed NoSuchElementException, IllegalStateException imports as we won't catch them here anymore
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
// Removed LoggingAspectException import as we won't throw it from logAround anymore for handled exceptions

/**
 * Aspect for logging execution of specific Spring components.
 * Now includes Repository, Service, RestController, and Component annotated beans.
 */
@Aspect
@Component
public class LoggingAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Pointcut that matches all repositories, services, controllers,
     * and general components within the application's defined packages.
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)"
            + " || within(@org.springframework.stereotype.Service *)"
            + " || within(@org.springframework.web.bind.annotation.RestController *)"
            + " || within(@org.springframework.stereotype.Component *)")
    public void springBeanPointcut() {
        // Method is empty as this is just a Pointcut definition.
    }

    /**
     * Pointcut that matches all Spring beans within the application's main packages.
     */
    @Pointcut("within(sovok.mcbuildlibrary..*)")
    public void applicationPackagePointcut() {
        // Method is empty as this is just a Pointcut definition.
    }

    /**
     * Advice that logs methods throwing exceptions. Applied AFTER the exception is thrown.
     * Lets GlobalExceptionHandler handle the HTTP response.
     * Applied to methods matching both application package and Spring bean pointcuts.
     *
     * @param joinPoint join point for advice.
     * @param e         exception.
     */
    @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()",
            throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        // Log based on the exception type - client errors vs server errors
        if (e instanceof jakarta.validation.ConstraintViolationException ||
                e instanceof IllegalArgumentException ||
                e instanceof java.util.NoSuchElementException ||
                e instanceof IllegalStateException ||
                e instanceof org.springframework.web.bind.MethodArgumentNotValidException ||
                e instanceof org.springframework.web.bind.MissingServletRequestParameterException ||
                e instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ||
                e instanceof org.springframework.web.multipart.support.MissingServletRequestPartException) {

            // Log client-side errors (like validation) typically as WARN
            log.warn("Client Error in {}.{}() - Exception: {}, Message: '{}'",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    e.getClass().getSimpleName(),
                    e.getMessage()); // Log exception message, full trace might be too verbose for validation errors
            // Optionally log args if needed for debugging: log.warn("... with argument[s] = {}", Arrays.toString(joinPoint.getArgs()));
        } else {
            // Log unexpected server-side errors as ERROR with stack trace
            log.error("Exception in {}.{}() with cause = '{}' and exception = '{}'",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    e.getCause() != null ? e.getCause() : "NULL",
                    e.getMessage(), // Log the exception message
                    e); // Log the full exception stack trace via SLF4j parameter substitution
        }
    }

    /**
     * Advice that logs when a method is entered and exited, and measures execution time.
     * It allows exceptions to propagate naturally to be handled by GlobalExceptionHandler.
     *
     * @param joinPoint join point for advice.
     * @return result of the wrapped method.
     * @throws Throwable propagates exceptions from the wrapped method.
     */
    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable { // Allow Throwable propagation
        long startTime = System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug("Enter: {}.{}() with argument[s] = {}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    Arrays.toString(joinPoint.getArgs()));
        }

        try {
            // Proceed with the original method execution
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();

            if (log.isDebugEnabled()) {
                log.debug("Exit: {}.{}() with result = {}; Execution time = {} ms",
                        joinPoint.getSignature().getDeclaringTypeName(),
                        joinPoint.getSignature().getName(),
                        result,
                        endTime - startTime);
            }
            return result;
        } catch (Throwable t) {
            // *** FIX: Removed the catch blocks that wrapped exceptions ***
            // Just log timing on failure if needed (optional) and re-throw
            long endTime = System.currentTimeMillis();
            // Use the @AfterThrowing advice to log the exception details appropriately.
            // We might still log a simple timing message here if desired.
            log.debug("Method {}.{}() threw exception after {} ms", // Log as debug or trace
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    endTime - startTime);

            throw t; // *** IMPORTANT: Re-throw the original exception ***
        }
        // Removed the previous specific catch blocks (IllegalArgumentException etc.)
        // and the generic catch (Throwable t) that wrapped exceptions.
    }
}