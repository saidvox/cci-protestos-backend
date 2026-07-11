package pe.org.camaracomercioica.protestos.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.org.camaracomercioica.protestos.model.Usuario;
import pe.org.camaracomercioica.protestos.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UsuarioRepository repository;

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        var user = findUser(identifier);
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRol().getNombre())
                .disabled(!user.isActivo())
                .build();
    }

    private Usuario findUser(String identifier) {
        String value = identifier.trim();
        if (value.contains("@")) {
            return repository.findByEmailIgnoreCase(value)
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
        }

        String document = value.replaceAll("\\s+", "").toUpperCase();
        if (document.matches("\\d+")) {
            document = document.replaceAll("\\D", "");
        }

        var matches = repository.findByDeudorNumeroDocumento(document);
        if (matches.size() != 1) {
            throw new UsernameNotFoundException("Credenciales invalidas");
        }
        return matches.get(0);
    }
}
