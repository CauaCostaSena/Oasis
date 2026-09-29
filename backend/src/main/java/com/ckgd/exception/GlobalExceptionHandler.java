package com.ckgd.exception;

import com.ckgd.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErrorResponse> error(int status, String title, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(status, title, message, request.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(404, "Não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> business(BusinessException ex, HttpServletRequest request) {
        return error(400, "Regra de negócio", ex.getMessage(), request);
    }

    @ExceptionHandler(FeatureUnavailableException.class)
    public ResponseEntity<ErrorResponse> unavailable(FeatureUnavailableException ex, HttpServletRequest request) {
        return error(503, "Recurso indisponível", ex.getMessage(), request);
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ErrorResponse> github(GitHubApiException ex, HttpServletRequest request) {
        return error(ex.getStatus(), "GitHub indisponível", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> credentials(BadCredentialsException ex, HttpServletRequest request) {
        return error(401, "Credenciais inválidas", "E-mail ou senha incorretos", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).distinct()
                .reduce((a, b) -> a + "; " + b).orElse("Dados inválidos");
        return error(400, "Erro de validação", message, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> malformed(Exception ex, HttpServletRequest request) {
        return error(400, "Dados inválidos", "Confira o formato e os valores enviados.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> conflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return error(409, "Conflito de dados", "Não foi possível salvar. Verifique se o cadastro já existe.", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> upload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return error(413, "Arquivo muito grande", "A imagem deve ter no máximo 5 MB.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generic(Exception ex, HttpServletRequest request) {
        // Não registrar corpo, credenciais, headers, token ou mensagem SQL contendo dados do usuário.
        log.error("Falha interna em {} ({})", request.getRequestURI(), ex.getClass().getSimpleName());
        return error(500, "Erro interno", "Não foi possível concluir a operação. Tente novamente mais tarde.", request);
    }
}
