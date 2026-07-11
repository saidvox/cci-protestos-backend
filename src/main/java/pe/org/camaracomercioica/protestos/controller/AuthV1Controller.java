package pe.org.camaracomercioica.protestos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.org.camaracomercioica.protestos.dto.*;
import pe.org.camaracomercioica.protestos.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación V1", description = "Endpoints V1 para autenticación y registro")
public class AuthV1Controller {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar deudor", description = "Crea una nueva cuenta de deudor (USER_DEBTOR).")
    @ApiResponse(responseCode = "201", description = "Registro exitoso")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o formato incorrecto")
    @ApiResponse(responseCode = "409", description = "El correo electrónico o documento ya está registrado")
    @SecurityRequirements
    public void register(@Valid @RequestBody RegisterRequest r) {
        authService.register(r);
    }

    @GetMapping("/debtor-lookup")
    @Operation(summary = "Buscar deudor por documento", description = "Busca un deudor existente por tipo y numero de documento para autocompletar el registro publico.")
    @ApiResponse(responseCode = "200", description = "Busqueda procesada")
    @SecurityRequirements
    public DebtorLookupResponse lookupDebtor(
            @RequestParam String tipoDocumento,
            @RequestParam String numeroDocumento
    ) {
        var result = authService.lookupDebtor(tipoDocumento, numeroDocumento);
        return new DebtorLookupResponse(result.found(), result.nombreCompleto(), result.email());
    }

    @GetMapping("/session")
    @Operation(summary = "Obtener sesión actual", description = "Retorna la información del usuario autenticado actualmente.")
    @ApiResponse(responseCode = "200", description = "Sesión activa, retorna detalles del usuario")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    public LoginResponse session(Authentication authentication) {
        if (authentication == null) {
            throw new pe.org.camaracomercioica.protestos.exception.UnauthorizedException("No autenticado");
        }
        return authService.getSession(authentication.getName());
    }
}
