package pe.org.camaracomercioica.protestos.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
import static org.assertj.core.api.Assertions.*;

class UploadValidatorTest {
  private final UploadValidator validator = new UploadValidator(1024);
  @Test void aceptaPdfSeguro() { assertThatCode(() -> validator.validateDocument(new MockMultipartFile("file", "constancia.pdf", "application/pdf", "pdf".getBytes()))).doesNotThrowAnyException(); }
  @Test void rechazaRutaEnNombre() { assertThatThrownBy(() -> validator.validateDocument(new MockMultipartFile("file", "../evil.pdf", "application/pdf", "x".getBytes()))).isInstanceOf(BadRequestException.class); }
  @Test void aceptaXlsx() { assertThatCode(() -> validator.validateExcel(new MockMultipartFile("file", "carga.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "x".getBytes()))).doesNotThrowAnyException(); }
}
