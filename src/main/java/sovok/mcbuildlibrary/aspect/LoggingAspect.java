package sovok.mcbuildlibrary.aspect;

import java.util.Arrays;
import java.util.NoSuchElementException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sovok.mcbuildlibrary.exception.LoggingAspectException;

/**
 * Aspect for logging execution of specific Spring components.
 * Now includes Repository, Service, RestController, and Component annotated beans.
 */
@Aspect
@Component // Ensure this component itself is picked up by Spring
public class LoggingAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Pointcut that matches all repositories, services, controllers,
     * and general components within the application's defined packages.
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)"
            + " || within(@org.springframework.stereotype.Service *)"
            + " || within(@org.springframework.web.bind.annotation.RestController *)"
            + " || within(@org.springframework.stereotype.Component *)") // <-- Added this line
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
     * Advice that logs methods throwing exceptions.
     * Applied to methods matching both application package and Spring bean pointcuts.
     *
     * @param joinPoint join point for advice.
     * @param e         exception.
     */
    @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()",
            throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error("Exception in {}.{}() with cause = '{}' and exception = '{}'",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                e.getCause() != null ? e.getCause() : "NULL",
                e.getMessage(), // Log the exception message
                e); // Log the full exception stack trace via SLF4j parameter substitution
    }

    /**
     * Advice that logs when a method is entered and exited.
     * Uses @Around to capture entry, exit, and execution time.
     * Applied to methods matching both application package and Spring bean pointcuts.
     *
     * @param joinPoint join point for advice.
     * @return result.
     */
    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) { // Removed "throws Throwable"
        long startTime = System.currentTimeMillis(); // Start timing before logging entry

        // Log entry if debug is enabled
        if (log.isDebugEnabled()) {
            log.debug("Enter: {}.{}() with argument[s] = {}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    Arrays.toString(joinPoint.getArgs()));
        }

        Object result;
        try {
            // Proceed with the original method execution
            result = joinPoint.proceed();
            long endTime = System.currentTimeMillis(); // End timing after successful execution

            // Log successful exit if debug is enabled
            if (log.isDebugEnabled()) {
                log.debug("Exit: {}.{}() with result = {}; Execution time = {} ms",
                        joinPoint.getSignature().getDeclaringTypeName(),
                        joinPoint.getSignature().getName(),
                        result,
                        endTime - startTime);
            }
            return result;
        } catch (IllegalArgumentException | NoSuchElementException | IllegalStateException e) {
            // Minimal logging here; let @AfterThrowing handle detailed exception logging
            long endTime = System.currentTimeMillis();
            log.warn("Method {}.{}() failed after {} ms",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    endTime - startTime);

            // Wrap the exception with additional context before rethrowing
            String contextMessage = String.format(
                    "Exception occurred in %s.%s() with arguments: %s",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    Arrays.toString(joinPoint.getArgs())
            );
            throw new LoggingAspectException(contextMessage, e);
        } catch (Throwable t) {
            // Minimal logging here; let @AfterThrowing handle detailed exception logging
            long endTime = System.currentTimeMillis();
            log.error("Method {}.{}() failed unexpectedly after {} ms",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    endTime - startTime);

            // Wrap the throwable with additional context before rethrowing
            String contextMessage = String.format(
                    "Unexpected throwable in %s.%s() with arguments: %s",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    Arrays.toString(joinPoint.getArgs())
            );
            throw new LoggingAspectException(contextMessage, t);
        }
    }
}