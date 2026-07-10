package pe.org.camaracomercioica.protestos.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.org.camaracomercioica.protestos.dto.ExcelImportResponse;
import pe.org.camaracomercioica.protestos.dto.ExcelRowPreview;
import pe.org.camaracomercioica.protestos.dto.ExcelValidationError;
import pe.org.camaracomercioica.protestos.dto.ExcelValidationResponse;
import pe.org.camaracomercioica.protestos.exception.BadRequestException;
import pe.org.camaracomercioica.protestos.model.CargaExcel;
import pe.org.camaracomercioica.protestos.model.CargaExcelFila;
import pe.org.camaracomercioica.protestos.model.Deudor;
import pe.org.camaracomercioica.protestos.model.EntidadFinanciera;
import pe.org.camaracomercioica.protestos.model.EstadoCarga;
import pe.org.camaracomercioica.protestos.model.EstadoFilaCarga;
import pe.org.camaracomercioica.protestos.model.EstadoProtesto;
import pe.org.camaracomercioica.protestos.model.Protesto;
import pe.org.camaracomercioica.protestos.model.TipoDocumento;
import pe.org.camaracomercioica.protestos.model.TipoPersona;
import pe.org.camaracomercioica.protestos.repository.DeudorRepository;
import pe.org.camaracomercioica.protestos.repository.EntidadFinancieraRepository;
import pe.org.camaracomercioica.protestos.repository.ProtestoRepository;
import pe.org.camaracomercioica.protestos.util.UploadValidator;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExcelImportService {
    private static final String SHEET_NAME = "Plantilla Protestos";
    private static final int HEADER_ROW_INDEX = 6;
    private static final int DATA_ROW_INDEX = 7;
    private static final List<String> HEADERS = List.of(
            "Tipo_Documento", "Numero_Documento", "Nombre_Razon_Social", "Tipo_Persona",
            "Email_Deudor", "Telefono_Deudor", "RUC_Entidad_Financiera", "Numero_Titulo",
            "Tipo_Titulo", "Fecha_Protesto", "Fecha_Vencimiento", "Moneda", "Monto",
            "Estado", "Observaciones", "Codigo_Externo"
    );
    private static final Set<String> TIPOS_TITULO = Set.of("LETRA", "PAGARE", "CHEQUE", "FACTURA", "OTRO");

    private final EntidadFinancieraRepository entidades;
    private final DeudorRepository deudores;
    private final ProtestoRepository protestos;

    public ExcelValidationResponse validate(MultipartFile file, long maxBytes) throws IOException {
        new UploadValidator(maxBytes).validateExcel(file);
        return parse(file).toResponse();
    }

    @Transactional
    public ExcelImportResponse importRows(MultipartFile file, CargaExcel carga, long maxBytes) throws IOException {
        new UploadValidator(maxBytes).validateExcel(file);
        var parsed = parse(file);
        if (!parsed.errors().isEmpty()) {
            carga.setEstado(EstadoCarga.CON_ERROR);
            carga.setTotalFilas(parsed.totalRows());
            carga.setFilasImportadas(0);
            carga.setFilasConError(parsed.errorRows());
            carga.setResumen("Archivo rechazado: corrija las filas observadas antes de importar");
            return new ExcelImportResponse(
                    carga.getId(),
                    carga.getNombreArchivo(),
                    carga.getEstado().name(),
                    carga.getResumen(),
                    carga.getTotalFilas(),
                    0,
                    carga.getFilasConError(),
                    parsed.errors()
            );
        }

        for (ParsedRow row : parsed.rows()) {
            EntidadFinanciera entidad = entidades.findByRuc(row.rucEntidad())
                    .orElseThrow(() -> new BadRequestException("Entidad financiera no encontrada: " + row.rucEntidad()));
            Deudor deudor = deudores.findByTipoDocumentoAndNumeroDocumento(row.tipoDocumento(), row.numeroDocumento())
                    .orElseGet(Deudor::new);
            deudor.setTipoDocumento(row.tipoDocumento());
            deudor.setNumeroDocumento(row.numeroDocumento());
            deudor.setNombreRazonSocial(row.nombreRazonSocial());
            deudor.setTipoPersona(row.tipoPersona());
            deudor.setEmail(row.email());
            deudor.setTelefono(row.telefono());
            deudor.setActualizadoEn(Instant.now());
            deudor = deudores.save(deudor);

            var protesto = new Protesto();
            protesto.setEntidad(entidad);
            protesto.setDeudor(deudor);
            protesto.setOrigenCarga(carga);
            protesto.setNumeroTitulo(row.numeroTitulo());
            protesto.setTipoTitulo(row.tipoTitulo());
            protesto.setFechaProtesto(row.fechaProtesto());
            protesto.setFechaVencimiento(row.fechaVencimiento());
            protesto.setMoneda(row.moneda());
            protesto.setMonto(row.monto());
            protesto.setEstado(row.estado());
            protesto = protestos.save(protesto);

            var fila = new CargaExcelFila();
            fila.setCarga(carga);
            fila.setNumeroFila(row.excelRow());
            fila.setNumeroDocumento(row.numeroDocumento());
            fila.setNombreDeudor(row.nombreRazonSocial());
            fila.setMonto(row.monto());
            fila.setMoneda(row.moneda());
            fila.setFechaProtesto(row.fechaProtesto());
            fila.setEstado(EstadoFilaCarga.IMPORTADA);
            fila.setProtesto(protesto);
            carga.getFilas().add(fila);
        }

        carga.setEstado(EstadoCarga.PROCESADA);
        carga.setTotalFilas(parsed.totalRows());
        carga.setFilasImportadas(parsed.validRows());
        carga.setFilasConError(0);
        carga.setResumen("Archivo importado correctamente: " + parsed.validRows() + " protesto(s) registrados");

        return new ExcelImportResponse(
                carga.getId(),
                carga.getNombreArchivo(),
                carga.getEstado().name(),
                carga.getResumen(),
                carga.getTotalFilas(),
                carga.getFilasImportadas(),
                carga.getFilasConError(),
                List.of()
        );
    }

    private ParsedWorkbook parse(MultipartFile file) throws IOException {
        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new BadRequestException("El archivo debe contener la hoja '" + SHEET_NAME + "'");
            }
            validateHeaders(sheet);

            List<ParsedRow> rows = new ArrayList<>();
            List<ExcelValidationError> errors = new ArrayList<>();
            Set<String> titles = new HashSet<>();

            for (int i = DATA_ROW_INDEX; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isBlank(row)) {
                    continue;
                }
                int excelRow = i + 1;
                RowResult result = parseRow(row, excelRow, titles);
                errors.addAll(result.errors());
                if (result.row() != null && result.errors().isEmpty()) {
                    rows.add(result.row());
                }
            }

            int totalRows = rows.size() + errors.stream()
                    .map(ExcelValidationError::row)
                    .collect(HashSet::new, Set::add, Set::addAll)
                    .size();
            return new ParsedWorkbook(rows, errors, totalRows);
        }
    }

    private Workbook openWorkbook(MultipartFile file) throws IOException {
        if ("xls".equals(extension(file.getOriginalFilename()))) {
            return new HSSFWorkbook(file.getInputStream());
        }
        return new XSSFWorkbook(file.getInputStream());
    }

    private String extension(String name) {
        if (name == null) {
            return "";
        }
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private void validateHeaders(Sheet sheet) {
        Row header = sheet.getRow(HEADER_ROW_INDEX);
        if (header == null) {
            throw new BadRequestException("No se encontro la fila de encabezados de la plantilla");
        }
        for (int i = 0; i < HEADERS.size(); i++) {
            String actual = text(header.getCell(i));
            if (!HEADERS.get(i).equals(actual)) {
                throw new BadRequestException("Encabezado invalido en columna " + columnName(i) + ". Se esperaba '" + HEADERS.get(i) + "'");
            }
        }
    }

    private RowResult parseRow(Row row, int excelRow, Set<String> titles) {
        List<ExcelValidationError> errors = new ArrayList<>();
        String tipoDocumentoText = required(row, 0, excelRow, "Tipo_Documento", errors).toUpperCase(Locale.ROOT);
        String numeroDocumento = onlyDigits(required(row, 1, excelRow, "Numero_Documento", errors));
        String nombre = required(row, 2, excelRow, "Nombre_Razon_Social", errors);
        String tipoPersonaText = required(row, 3, excelRow, "Tipo_Persona", errors).toUpperCase(Locale.ROOT);
        String email = optional(row, 4);
        String telefono = optional(row, 5);
        String rucEntidad = onlyDigits(required(row, 6, excelRow, "RUC_Entidad_Financiera", errors));
        String numeroTitulo = required(row, 7, excelRow, "Numero_Titulo", errors);
        String tipoTitulo = required(row, 8, excelRow, "Tipo_Titulo", errors).toUpperCase(Locale.ROOT);
        LocalDate fechaProtesto = requiredDate(row, 9, excelRow, "Fecha_Protesto", errors);
        LocalDate fechaVencimiento = optionalDate(row, 10, excelRow, "Fecha_Vencimiento", errors);
        String moneda = required(row, 11, excelRow, "Moneda", errors).toUpperCase(Locale.ROOT);
        BigDecimal monto = requiredAmount(row, 12, excelRow, errors);
        String estadoText = required(row, 13, excelRow, "Estado", errors).toUpperCase(Locale.ROOT);

        TipoDocumento tipoDocumento = enumValue(TipoDocumento.class, tipoDocumentoText, excelRow, "Tipo_Documento", errors);
        TipoPersona tipoPersona = enumValue(TipoPersona.class, tipoPersonaText, excelRow, "Tipo_Persona", errors);
        EstadoProtesto estado = enumValue(EstadoProtesto.class, estadoText, excelRow, "Estado", errors);

        if (tipoDocumento != null) {
            validateDocument(tipoDocumento, numeroDocumento, excelRow, errors);
        }
        if (!rucEntidad.matches("\\d{11}")) {
            errors.add(error(excelRow, "RUC_Entidad_Financiera", rucEntidad, "Debe tener 11 digitos"));
        } else if (entidades.findByRuc(rucEntidad).isEmpty()) {
            errors.add(error(excelRow, "RUC_Entidad_Financiera", rucEntidad, "No existe una entidad financiera registrada con este RUC"));
        }
        if (!TIPOS_TITULO.contains(tipoTitulo)) {
            errors.add(error(excelRow, "Tipo_Titulo", tipoTitulo, "Use uno de: LETRA, PAGARE, CHEQUE, FACTURA, OTRO"));
        }
        if (!Set.of("PEN", "USD").contains(moneda)) {
            errors.add(error(excelRow, "Moneda", moneda, "Use PEN o USD"));
        }
        if (estado != null && !Set.of(EstadoProtesto.VIGENTE, EstadoProtesto.REGULARIZADO).contains(estado)) {
            errors.add(error(excelRow, "Estado", estado.name(), "Use VIGENTE o REGULARIZADO"));
        }
        if (fechaVencimiento != null && fechaProtesto != null && fechaVencimiento.isAfter(fechaProtesto)) {
            errors.add(error(excelRow, "Fecha_Vencimiento", fechaVencimiento.toString(), "No debe ser posterior a Fecha_Protesto"));
        }
        if (!numeroTitulo.isBlank() && !titles.add(numeroTitulo)) {
            errors.add(error(excelRow, "Numero_Titulo", numeroTitulo, "El numero de titulo esta duplicado dentro del archivo"));
        }

        ParsedRow parsed = null;
        if (tipoDocumento != null && tipoPersona != null && estado != null && fechaProtesto != null && monto != null) {
            parsed = new ParsedRow(excelRow, tipoDocumento, numeroDocumento, nombre, tipoPersona, email, telefono, rucEntidad,
                    numeroTitulo, tipoTitulo, fechaProtesto, fechaVencimiento, moneda, monto, estado);
        }
        return new RowResult(parsed, errors);
    }

    private void validateDocument(TipoDocumento tipo, String value, int row, List<ExcelValidationError> errors) {
        if (tipo == TipoDocumento.DNI && !value.matches("\\d{8}")) {
            errors.add(error(row, "Numero_Documento", value, "DNI debe tener 8 digitos"));
        }
        if (tipo == TipoDocumento.RUC && !value.matches("\\d{11}")) {
            errors.add(error(row, "Numero_Documento", value, "RUC debe tener 11 digitos"));
        }
        if (tipo == TipoDocumento.CE && !value.matches("\\d{6,12}")) {
            errors.add(error(row, "Numero_Documento", value, "CE debe tener entre 6 y 12 digitos"));
        }
    }

    private String required(Row row, int index, int excelRow, String field, List<ExcelValidationError> errors) {
        String value = optional(row, index);
        if (value.isBlank()) {
            errors.add(error(excelRow, field, "", "Campo obligatorio"));
        }
        return value;
    }

    private BigDecimal requiredAmount(Row row, int index, int excelRow, List<ExcelValidationError> errors) {
        Cell cell = row.getCell(index);
        try {
            BigDecimal amount = switch (cell == null ? CellType.BLANK : cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> new BigDecimal(cell.getStringCellValue().trim().replace(",", ""));
                default -> null;
            };
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(error(excelRow, "Monto", text(cell), "Debe ser un numero mayor que cero"));
                return null;
            }
            return amount;
        } catch (NumberFormatException ex) {
            errors.add(error(excelRow, "Monto", text(cell), "Debe ser un numero valido"));
            return null;
        }
    }

    private LocalDate requiredDate(Row row, int index, int excelRow, String field, List<ExcelValidationError> errors) {
        LocalDate date = optionalDate(row, index, excelRow, field, errors);
        if (date == null) {
            errors.add(error(excelRow, field, text(row.getCell(index)), "Campo obligatorio con formato fecha"));
        }
        return date;
    }

    private LocalDate optionalDate(Row row, int index, int excelRow, String field, List<ExcelValidationError> errors) {
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK || text(cell).isBlank()) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            return LocalDate.parse(text(cell));
        } catch (RuntimeException ex) {
            errors.add(error(excelRow, field, text(cell), "Debe tener formato de fecha valido, por ejemplo 2026-03-15"));
            return null;
        }
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, int row, String field, List<ExcelValidationError> errors) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException ex) {
            errors.add(error(row, field, value, "Valor no permitido"));
            return null;
        }
    }

    private boolean isBlank(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < HEADERS.size(); i++) {
            if (!optional(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String optional(Row row, int index) {
        return text(row.getCell(index)).trim();
    }

    private String text(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : numericText(cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private String numericText(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private String onlyDigits(String value) {
        return value.replaceAll("\\D", "");
    }

    private ExcelValidationError error(int row, String field, String value, String message) {
        return new ExcelValidationError(row, field, value, message);
    }

    private String columnName(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private record RowResult(ParsedRow row, List<ExcelValidationError> errors) {
    }

    private record ParsedWorkbook(List<ParsedRow> rows, List<ExcelValidationError> errors, int totalRows) {
        int validRows() {
            return rows.size();
        }

        int errorRows() {
            return errors.stream().map(ExcelValidationError::row).collect(HashSet::new, Set::add, Set::addAll).size();
        }

        ExcelValidationResponse toResponse() {
            List<ExcelRowPreview> preview = rows.stream()
                    .limit(10)
                    .map(row -> new ExcelRowPreview(row.excelRow(), row.numeroDocumento(), row.nombreRazonSocial(), row.rucEntidad(),
                            row.numeroTitulo(), row.tipoTitulo(), row.fechaProtesto(), row.moneda(), row.monto(), row.estado().name()))
                    .toList();
            return new ExcelValidationResponse(errors.isEmpty(), totalRows, validRows(), errorRows(), errors, preview);
        }
    }

    private record ParsedRow(
            int excelRow,
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            String nombreRazonSocial,
            TipoPersona tipoPersona,
            String email,
            String telefono,
            String rucEntidad,
            String numeroTitulo,
            String tipoTitulo,
            LocalDate fechaProtesto,
            LocalDate fechaVencimiento,
            String moneda,
            BigDecimal monto,
            EstadoProtesto estado
    ) {
    }
}
