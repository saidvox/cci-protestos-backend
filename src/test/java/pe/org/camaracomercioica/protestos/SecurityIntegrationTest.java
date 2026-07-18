package pe.org.camaracomercioica.protestos;
import org.junit.jupiter.api.*; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.mock.web.MockMultipartFile; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.test.context.support.WithMockUser; import org.springframework.test.web.servlet.MockMvc; import pe.org.camaracomercioica.protestos.model.*; import pe.org.camaracomercioica.protestos.repository.*; import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf; import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties = {
 "springdoc.api-docs.enabled=false",
 "springdoc.swagger-ui.enabled=false"
}) @AutoConfigureMockMvc class SecurityIntegrationTest {
 @Autowired MockMvc mvc; @Autowired RolRepository roles; @Autowired UsuarioRepository users; @Autowired EntidadFinancieraRepository entidades; @Autowired AnalistaRepository analistas; @Autowired InvitacionAnalistaRepository invitaciones; @Autowired SolicitudRepository solicitudes; @Autowired DocumentoRepository documentos; @Autowired PasswordEncoder encoder; @Autowired pe.org.camaracomercioica.protestos.security.LoginRateLimitFilter loginRateLimitFilter; @Autowired pe.org.camaracomercioica.protestos.service.AuditoriaService auditoriaService;
 @BeforeEach void seed(){((java.util.Map<?,?>)org.springframework.test.util.ReflectionTestUtils.getField(loginRateLimitFilter,"attempts")).clear();if(users.findByEmailIgnoreCase("login@test.local").isEmpty()){var role=roles.findByNombre("CCI_ADMIN").orElseGet(()->{var r=new Rol();r.setNombre("CCI_ADMIN");return roles.save(r);});var entidad=new EntidadFinanciera();entidad.setRuc("20999999999");entidad.setRazonSocial("Entidad Test");entidad=entidades.save(entidad);var u=new Usuario();u.setNombreCompleto("Login Test");u.setEmail("login@test.local");u.setPasswordHash(encoder.encode("Password1!"));u.setRol(role);u.setEntidad(entidad);users.save(u);}}
 @Test void error401UsaContratoApiError() throws Exception {mvc.perform(get("/api/solicitudes")).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.code").value("UNAUTHORIZED")).andExpect(jsonPath("$.path").value("/api/solicitudes"));}
 @Test void loginExitosoEntregaJwtSoloEnCookieHttpOnly() throws Exception {mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"login@test.local\",\"password\":\"Password1!\"}")) .andExpect(status().isOk()).andExpect(cookie().httpOnly("CCI_ACCESS_TOKEN",true)).andExpect(cookie().sameSite("CCI_ACCESS_TOKEN","Strict")).andExpect(jsonPath("$.accessToken").doesNotExist()).andExpect(jsonPath("$.usuario.roles[0]").value("CCI_ADMIN"));}
 @Test void deudorPuedeIniciarSesionConDocumento() throws Exception {mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"20123456789\",\"password\":\"password\"}")) .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.email").value("deudor@demo.local")).andExpect(jsonPath("$.usuario.roles[0]").value("USER_DEBTOR"));}
 @Test void registroPuedeBuscarDeudorPorDocumento() throws Exception {mvc.perform(get("/api/v1/auth/debtor-lookup").param("tipoDocumento","RUC").param("numeroDocumento","20123456789")).andExpect(status().isOk()).andExpect(jsonPath("$.found").value(true)).andExpect(jsonPath("$.nombreCompleto").isNotEmpty()).andExpect(jsonPath("$.email").doesNotExist());}
 @Test void publicaTokenCsrfParaSpa() throws Exception {mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());}
 @Test void credencialesInvalidasResponden401() throws Exception {mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"login@test.local\",\"password\":\"incorrecta\"}")) .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));}
 @Test @WithMockUser(roles="USER_DEBTOR") void deudorNoListaTodasLasSolicitudes() throws Exception {mvc.perform(get("/api/solicitudes")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));}
 @Test @WithMockUser(roles="USER_DEBTOR") void deudorNoAccedeReportes() throws Exception {mvc.perform(get("/api/reportes/solicitudes")).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="USER_DEBTOR") void deudorAutenticadoPuedeListarEntidades() throws Exception {mvc.perform(get("/api/entidades")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));}
 @Test @WithMockUser(roles="USER_DEBTOR") void deudorNoPuedeCrearEntidades() throws Exception {mvc.perform(post("/api/entidades").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"ruc\":\"20999999998\",\"razonSocial\":\"Entidad Test\",\"email\":\"contacto@test.local\"}")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));}
 @Test @WithMockUser(roles="USER_DEBTOR") void deudorNoCargaExcel() throws Exception {var f=new MockMultipartFile("file","carga.xls","application/vnd.ms-excel",new byte[]{(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1});mvc.perform(multipart("/api/excel/upload").file(f).with(csrf())).andExpect(status().isForbidden());}
 @Test @WithMockUser(username="login@test.local",roles="BANK_ANALYST") void analistaPuedeCargarExcel() throws Exception {var f=new MockMultipartFile("file","carga.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",excelValido());mvc.perform(multipart("/api/excel/upload").file(f).with(csrf())).andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("PROCESADA"));}
 @Test void solicitudConDocumentosEsAtomica() throws Exception {
  var admin=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("login@test.local").roles("CCI_ADMIN");
  long entidadId=entidades.findByRuc("20999999999").orElseThrow().getId();
  long solicitudesAntes=solicitudes.count();
  String invalidJson="{\"entidadId\":"+entidadId+",\"tipoTramite\":\"REGISTRO_PROTESTO\",\"numeroDocumentoDeudor\":\"20988888001\",\"monto\":100,\"moneda\":\"PEN\",\"motivo\":\"Prueba atomica invalida\"}";
  var invalidRequest=new MockMultipartFile("solicitud","","application/json",invalidJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  var validPdf=new MockMultipartFile("files","voucher.pdf","application/pdf","%PDF-1.4\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
  var invalidFile=new MockMultipartFile("files","formato.txt","text/plain","archivo invalido".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  mvc.perform(multipart("/api/solicitudes/con-documentos").file(invalidRequest).file(validPdf).file(invalidFile).with(admin).with(csrf()))
    .andExpect(status().isBadRequest());
  org.assertj.core.api.Assertions.assertThat(solicitudes.count()).isEqualTo(solicitudesAntes);

  String validJson="{\"entidadId\":"+entidadId+",\"tipoTramite\":\"REGISTRO_PROTESTO\",\"numeroDocumentoDeudor\":\"20988888002\",\"monto\":100,\"moneda\":\"PEN\",\"motivo\":\"Prueba atomica valida\"}";
  var validRequest=new MockMultipartFile("solicitud","","application/json",validJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  var result=mvc.perform(multipart("/api/solicitudes/con-documentos").file(validRequest).file(validPdf).with(admin).with(csrf()))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.codigo").isNotEmpty()).andReturn();
  long solicitudId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString()).get("id").asLong();
  org.assertj.core.api.Assertions.assertThat(documentos.findBySolicitudIdOrderByCreadoEnAsc(solicitudId)).hasSize(1);
  mvc.perform(multipart("/api/documentos/solicitud/"+solicitudId+"/upload-batch").file(validPdf).file(invalidFile).with(admin).with(csrf()))
    .andExpect(status().isBadRequest());
  org.assertj.core.api.Assertions.assertThat(documentos.findBySolicitudIdOrderByCreadoEnAsc(solicitudId)).hasSize(1);
 }
 @Test void validaPayloadLogin() throws Exception {mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"mal\",\"password\":\"1\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));}
 @Test void usuariosInicialesPuedenIniciarSesion() throws Exception {
  String[] users={"admin@demo.local:CCI_ADMIN","staff@demo.local:CCI_STAFF","analista@demo.local:BANK_ANALYST","deudor@demo.local:USER_DEBTOR"};
  for(String item:users){var parts=item.split(":");mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+parts[0]+"\",\"password\":\"password\"}")).andExpect(status().isOk()).andExpect(cookie().httpOnly("CCI_ACCESS_TOKEN",true)).andExpect(jsonPath("$.usuario.roles[0]").value(parts[1]));}
 }
 @Test void sessionDevuelveUsuarioAutenticadoDesdeCookie() throws Exception {
  var result=mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"deudor@demo.local\",\"password\":\"password\"}")).andExpect(status().isOk()).andReturn();
  mvc.perform(get("/api/v1/auth/session").cookie(result.getResponse().getCookie("CCI_ACCESS_TOKEN"))).andExpect(status().isOk()).andExpect(jsonPath("$.usuario.email").value("deudor@demo.local")).andExpect(jsonPath("$.usuario.roles[0]").value("USER_DEBTOR"));
 }
 @Test void logoutEliminaCookieJwt() throws Exception {
  mvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isNoContent()).andExpect(cookie().maxAge("CCI_ACCESS_TOKEN",0));
 }
 @Test void rutaInexistenteResponde404SinExponerErrorInterno() throws Exception {
  mvc.perform(get("/v3/api-docs"))
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
    .andExpect(jsonPath("$.path").value("/v3/api-docs"));
 }
 @Test void administradorInvitaAnalistaYLaActivacionEsDeUnSoloUso() throws Exception {
  String email="analista.seguridad@test.local";
  users.findByEmailIgnoreCase(email).ifPresent(user->{analistas.findAll().stream().filter(item->item.getUsuario().getId().equals(user.getId())).findFirst().ifPresent(item->{invitaciones.findAll().stream().filter(invitation->invitation.getAnalista().getId().equals(item.getId())).forEach(invitaciones::delete);analistas.delete(item);});users.delete(user);});
  long entidadId=entidades.findByRuc("20999999999").orElseThrow().getId();
  String create="{\"nombre\":\"Analista Seguridad\",\"email\":\""+email+"\",\"codigo\":\"AN-SEC-01\",\"entidadId\":"+entidadId+"}";
  var admin=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("login@test.local").roles("CCI_ADMIN");
  var created=mvc.perform(post("/api/analistas").with(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(create))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.analista.email").value(email)).andExpect(jsonPath("$.analista.accessStatus").value("PENDING_ACTIVATION")).andExpect(jsonPath("$.activationToken").isNotEmpty()).andReturn();
  var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
  var createdRoot=mapper.readTree(created.getResponse().getContentAsString());
  long analystId=createdRoot.get("analista").get("id").asLong();
  String oldToken=createdRoot.get("activationToken").asText();
  mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"Inicial1!\"}"))
    .andExpect(status().isUnauthorized());
  var storedInvitation=invitaciones.findAll().stream().filter(item->item.getAnalista().getId().equals(analystId)).findFirst().orElseThrow();
  storedInvitation.setExpiraEn(java.time.Instant.now().minusSeconds(1));
  invitaciones.save(storedInvitation);
  mvc.perform(get("/api/v1/auth/analyst-activation").param("token",oldToken)).andExpect(status().isBadRequest());
  var regenerated=mvc.perform(post("/api/analistas/"+analystId+"/invitacion").with(admin).with(csrf()))
    .andExpect(status().isOk()).andExpect(jsonPath("$.activationToken").isNotEmpty()).andReturn();
  String token=mapper.readTree(regenerated.getResponse().getContentAsString()).get("activationToken").asText();
  mvc.perform(get("/api/v1/auth/analyst-activation").param("token",oldToken)).andExpect(status().isBadRequest());
  var entity=entidades.findById(entidadId).orElseThrow();entity.setActivo(false);entidades.save(entity);
  mvc.perform(get("/api/v1/auth/analyst-activation").param("token",token)).andExpect(status().isBadRequest());
  entity.setActivo(true);entidades.save(entity);
  mvc.perform(get("/api/v1/auth/analyst-activation").param("token",token)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
  mvc.perform(post("/api/v1/auth/analyst-activation").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+token+"\",\"password\":\"debil\"}"))
    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  mvc.perform(post("/api/v1/auth/analyst-activation").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+token+"\",\"password\":\"Inicial1!\"}"))
    .andExpect(status().isNoContent());
  mvc.perform(post("/api/v1/auth/analyst-activation").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+token+"\",\"password\":\"OtraClave2@\"}"))
    .andExpect(status().isBadRequest());
  var login=mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"Inicial1!\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.roles[0]").value("BANK_ANALYST")).andReturn();
  var oldCookie=login.getResponse().getCookie("CCI_ACCESS_TOKEN");
  mvc.perform(patch("/api/analistas/"+analystId+"/password").with(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"debil\"}"))
    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  mvc.perform(patch("/api/analistas/"+analystId+"/password").with(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"NuevaClave2@\"}"))
    .andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/auth/session").cookie(oldCookie)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"Inicial1!\"}"))
    .andExpect(status().isUnauthorized());
  var newLogin=mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"NuevaClave2@\"}"))
    .andExpect(status().isOk()).andReturn();
  mvc.perform(patch("/api/analistas/"+analystId+"/estado").with(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"disponible\":false}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(false));
  mvc.perform(get("/api/v1/auth/session").cookie(newLogin.getResponse().getCookie("CCI_ACCESS_TOKEN"))).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"NuevaClave2@\"}"))
    .andExpect(status().isUnauthorized());
  var restarted=mvc.perform(post("/api/analistas/"+analystId+"/reactivacion").with(admin).with(csrf()))
    .andExpect(status().isOk()).andExpect(jsonPath("$.analista.accessStatus").value("PENDING_ACTIVATION"))
    .andExpect(jsonPath("$.activationToken").isNotEmpty()).andReturn();
  String restartToken=mapper.readTree(restarted.getResponse().getContentAsString()).get("activationToken").asText();
  mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"NuevaClave2@\"}"))
    .andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/auth/analyst-activation").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+restartToken+"\",\"password\":\"Reactivada3#\"}"))
    .andExpect(status().isNoContent());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"Reactivada3#\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.roles[0]").value("BANK_ANALYST"));
 }
 @Test @WithMockUser(roles="CCI_STAFF") void staffNoPuedeCrearNiRegenerarInvitacionesDeAnalista() throws Exception {
  mvc.perform(post("/api/analistas").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Sin permiso\",\"email\":\"staff.invita@test.local\",\"codigo\":\"AN-NO\",\"entidadId\":1}"))
    .andExpect(status().isForbidden());
  mvc.perform(post("/api/analistas/1/invitacion").with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/api/analistas/1/reactivacion").with(csrf())).andExpect(status().isForbidden());
 }
 @Test void notificacionesSonPersistentesYExclusivasDelErp() throws Exception {
  auditoriaService.registrar("integration@test.local","ACTUALIZAR","ENTIDAD",1L,"Actividad de prueba");
  var admin=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("login@test.local").roles("CCI_ADMIN");
  var response=mvc.perform(get("/api/v1/erp/notificaciones").param("limit","10").with(admin))
    .andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray()).andExpect(jsonPath("$.unreadCount").value(org.hamcrest.Matchers.greaterThan(0))).andReturn();
  var root=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getResponse().getContentAsString());
  if(!root.get("items").isEmpty()){
   long newest=root.get("items").get(0).get("id").asLong();
   mvc.perform(patch("/api/v1/erp/notificaciones/leidas").with(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"throughId\":"+newest+"}"))
     .andExpect(status().isNoContent());
   mvc.perform(get("/api/v1/erp/notificaciones").with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
  }
  mvc.perform(get("/api/v1/erp/notificaciones").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff@demo.local").roles("CCI_STAFF"))).andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(org.hamcrest.Matchers.greaterThan(0)));
  mvc.perform(get("/api/v1/erp/notificaciones").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("deudor@demo.local").roles("USER_DEBTOR"))).andExpect(status().isForbidden());
 }
 private byte[] excelValido() throws Exception {
  String[] headers={"Tipo_Documento","Numero_Documento","Nombre_Razon_Social","Tipo_Persona","Email_Deudor","Telefono_Deudor","RUC_Entidad_Financiera","Numero_Titulo","Tipo_Titulo","Fecha_Protesto","Fecha_Vencimiento","Moneda","Monto","Estado","Observaciones","Codigo_Externo"};
  Object[] values={"DNI","45871236","Deudor Test","NATURAL","deudor@test.local","999111222","20999999999","TV-TEST-001","PAGARE","2026-03-01","2026-02-20","PEN",1500.50,"VIGENTE","",""};
  try(var wb=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var out=new java.io.ByteArrayOutputStream()){
   var sheet=wb.createSheet("Plantilla Protestos");
   var header=sheet.createRow(6);
   for(int i=0;i<headers.length;i++)header.createCell(i).setCellValue(headers[i]);
   var row=sheet.createRow(7);
   for(int i=0;i<values.length;i++){if(values[i] instanceof Number n)row.createCell(i).setCellValue(n.doubleValue());else row.createCell(i).setCellValue(String.valueOf(values[i]));}
   wb.write(out);
   return out.toByteArray();
  }
 }
}
