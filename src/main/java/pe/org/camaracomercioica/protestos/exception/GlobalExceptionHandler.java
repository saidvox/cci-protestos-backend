package pe.org.camaracomercioica.protestos.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return build(404, "NOT_FOUND", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> routeNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return build(404, "NOT_FOUND", "Recurso no encontrado", request, Map.of());
    }

    @ExceptionHandler({
            BadRequestException.class,
            MaxUploadSizeExceededException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    ResponseEntity<ApiError> badRequest(Exception exception, HttpServletRequest request) {
        String message = exception instanceof BadRequestException
                ? exception.getMessage()
                : "Formato o parametro invalido";
        return build(400, "BAD_REQUEST", message, request, Map.of());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> unauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return build(401, "UNAUTHORIZED", exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return build(403, "FORBIDDEN", "No tiene permisos para esta operacion", request, Map.of());
    }

    @ExceptionHandler({
            ConflictException.class,
            ObjectOptimisticLockingFailureException.class,
            DataIntegrityViolationException.class
    })
    ResponseEntity<ApiError> conflict(Exception exception, HttpServletRequest request) {
        String message = exception instanceof ConflictException
                ? exception.getMessage()
                : "Conflicto con el estado actual del recurso";
        return build(409, "CONFLICT", message, request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> Optional.ofNullable(error.getDefaultMessage()).orElse("Invalido"),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        return build(400, "VALIDATION_ERROR", "Datos invalidos", request, errors);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> generic(Exception exception, HttpServletRequest request) {
        logger.error("Unexpected error occurred", exception);
        return build(500, "INTERNAL_ERROR", "Ocurrio un error inesperado", request, Map.of());
    }

    private ResponseEntity<ApiError> build(
            int status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> errors
    ) {
        ApiError error = new ApiError(Instant.now(), status, code, message, request.getRequestURI(), errors);
        return ResponseEntity.status(status).body(error);
    }
}
