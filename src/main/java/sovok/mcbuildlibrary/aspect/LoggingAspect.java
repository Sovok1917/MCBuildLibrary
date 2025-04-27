package sovok.mcbuildlibrary.aspect;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Pointcut("within(@org.springframework.stereotype.Repository *)"
            + " || within(@org.springframework.stereotype.Service *)"
            + " || within(@org.springframework.web.bind.annotation.RestController *)"
            + " || within(@org.springframework.stereotype.Component *)")
    public void springBeanPointcut() {

    }

    @Pointcut("within(sovok.mcbuildlibrary..*)")
    public void applicationPackagePointcut() {

    }

    @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()",
            throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {

        if (e instanceof jakarta.validation.ConstraintViolationException
                || e instanceof IllegalArgumentException
                || e instanceof java.util.NoSuchElementException
                || e instanceof IllegalStateException
                || e instanceof org.springframework.web.bind.MethodArgumentNotValidException
                || e instanceof org.springframework.web.bind
                .MissingServletRequestParameterException
                || e instanceof org.springframework.web.method.annotation
                .MethodArgumentTypeMismatchException
                || e instanceof org.springframework.web.multipart.support
                .MissingServletRequestPartException) {

            log.warn("Client Error in {}.{}() - Exception: {}, Message: '{}'",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    e.getClass().getSimpleName(),
                    e.getMessage());
        } else {
            log.error("Exception in {}.{}() with cause = '{}' and exception = '{}'",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    e.getCause() != null ? e.getCause() : "NULL",
                    e.getMessage(),
                    e);
        }
    }

    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug("Enter: {}.{}() with argument[s] = {}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    Arrays.toString(joinPoint.getArgs()));
        }

        try {

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


            long endTime = System.currentTimeMillis();


            log.debug("Method {}.{}() threw exception after {} ms",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    endTime - startTime);

            throw t;
        }
    }
}