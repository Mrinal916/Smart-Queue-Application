package com.smartqueue;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartqueue.user.entity.Role;
import com.smartqueue.user.entity.UserAccount;
import com.smartqueue.user.enums.RoleName;
import com.smartqueue.user.repository.RoleRepository;
import com.smartqueue.user.repository.UserAccountRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class RoleJourneyIntegrationTests {

  private static final String TEST_PASSWORD = "Password-123!";

  @Autowired private TestRestTemplate rest;
  private final ObjectMapper json = new ObjectMapper();
  @Autowired private RoleRepository roles;
  @Autowired private UserAccountRepository users;
  @Autowired private PasswordEncoder passwords;

  @Test
  void citizenOfficerAndAdminCompleteTheTokenJourney() throws Exception {
    String suffix = String.valueOf(System.nanoTime());
    String adminToken = createStaff("admin-" + suffix + "@example.test", RoleName.ADMIN);
    Staff officer = createOfficer("officer-" + suffix + "@example.test");
    String citizenToken = registerCitizen("citizen-" + suffix + "@example.test");

    JsonNode office =
        post(
            "/api/v1/offices",
            adminToken,
            Map.of(
                "code",
                "HOSP-" + suffix,
                "name",
                "Test Hospital " + suffix,
                "address",
                "Test location",
                "category",
                "HOSPITAL"));
    JsonNode department =
        post(
            "/api/v1/departments",
            adminToken,
            Map.of("officeId", office.path("publicId").asText(), "name", "Outpatients"));
    LocalTime firstSlot = LocalTime.now().plusMinutes(15).withSecond(0).withNano(0);
    LocalTime secondSlot = firstSlot.plusMinutes(5);
    JsonNode service =
        post(
            "/api/v1/services",
            adminToken,
            Map.of(
                "departmentId",
                department.path("publicId").asText(),
                "name",
                "General care",
                "startTime",
                firstSlot.toString(),
                "endTime",
                firstSlot.plusMinutes(30).toString(),
                "dailyCapacity",
                20,
                "averageServiceMinutes",
                5));
    JsonNode availabilityService =
        post(
            "/api/v1/services",
            adminToken,
            Map.of(
                "departmentId",
                department.path("publicId").asText(),
                "name",
                "Availability checks",
                "startTime",
                "00:00",
                "endTime",
                "23:59",
                "dailyCapacity",
                20,
                "averageServiceMinutes",
                30));
    JsonNode counter =
        post(
            "/api/v1/counters",
            adminToken,
            Map.of("officeId", office.path("publicId").asText(), "code", "DESK-1"));
    post("/api/v1/counters/" + counter.path("publicId").asText() + "/open", adminToken, Map.of());
    post(
        "/api/v1/counters/service-assignments",
        adminToken,
        Map.of(
            "counterId",
            counter.path("publicId").asText(),
            "serviceId",
            service.path("publicId").asText()));
    post(
        "/api/v1/counters/officer-assignments",
        adminToken,
        Map.of("counterId", counter.path("publicId").asText(), "officerId", officer.id()));

    JsonNode booking =
        post(
            "/api/v1/tokens",
            citizenToken,
            Map.of(
                "visitorName",
                "Test Visitor",
                "visitorPhone",
                "+91 98765 43210",
                "visitorAge",
                32,
                "visitorGender",
                "OTHER",
                "serviceId",
                service.path("publicId").asText(),
                "appointmentDate",
                LocalDate.now().toString(),
                "appointmentTime",
                firstSlot.toString(),
                "idempotencyKey",
                "booking-" + suffix));
    String tokenId = booking.path("publicId").asText();
    assertThat(booking.path("status").asText()).isEqualTo("WAITING");
    assertThat(booking.path("visitorName").asText()).isEqualTo("Test Visitor");
    ResponseEntity<String> scheduledWait =
        rest.exchange(
            "/api/v1/tokens/" + tokenId + "/wait-time",
            HttpMethod.GET,
            entity(citizenToken),
            String.class);
    assertThat(scheduledWait.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(json.readTree(scheduledWait.getBody()).path("data").path("estimatedMinutes").asInt())
        .isGreaterThan(0);

    String secondCitizenToken = registerCitizen("second-citizen-" + suffix + "@example.test");
    ResponseEntity<String> elapsedSlot =
        rest.exchange(
            "/api/v1/tokens",
            HttpMethod.POST,
            entity(
                secondCitizenToken,
                Map.of(
                    "visitorName",
                    "Late Visitor",
                    "visitorPhone",
                    "+91 90000 00000",
                    "visitorAge",
                    40,
                    "visitorGender",
                    "OTHER",
                    "serviceId",
                    availabilityService.path("publicId").asText(),
                    "appointmentDate",
                    LocalDate.now().toString(),
                    "appointmentTime",
                    "00:00",
                    "idempotencyKey",
                    "elapsed-slot-" + suffix)),
            String.class);
    assertThat(elapsedSlot.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(elapsedSlot.getBody()).contains("future time slot");
    JsonNode reservedTomorrow =
        post(
            "/api/v1/tokens",
            secondCitizenToken,
            Map.of(
                "visitorName",
                "Another Visitor",
                "visitorPhone",
                "+91 90000 00000",
                "visitorAge",
                40,
                "visitorGender",
                "OTHER",
                "serviceId",
                availabilityService.path("publicId").asText(),
                "appointmentDate",
                LocalDate.now().plusDays(1).toString(),
                "appointmentTime",
                "00:00",
                "idempotencyKey",
                "reserved-slot-" + suffix));
    assertThat(reservedTomorrow.path("status").asText()).isEqualTo("WAITING");
    String thirdCitizenToken = registerCitizen("third-citizen-" + suffix + "@example.test");
    ResponseEntity<String> duplicateSlot =
        rest.exchange(
            "/api/v1/tokens",
            HttpMethod.POST,
            entity(
                thirdCitizenToken,
                Map.of(
                    "visitorName",
                    "Duplicate Visitor",
                    "visitorPhone",
                    "+91 92222 22222",
                    "visitorAge",
                    40,
                    "visitorGender",
                    "OTHER",
                    "serviceId",
                    availabilityService.path("publicId").asText(),
                    "appointmentDate",
                    LocalDate.now().plusDays(1).toString(),
                    "appointmentTime",
                    "00:00",
                    "idempotencyKey",
                    "duplicate-slot-" + suffix)),
            String.class);
    assertThat(duplicateSlot.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    String seniorCitizenToken = registerCitizen("senior-citizen-" + suffix + "@example.test");
    JsonNode seniorBooking =
        post(
            "/api/v1/tokens",
            seniorCitizenToken,
            Map.of(
                "visitorName",
                "Priority Visitor",
                "visitorPhone",
                "+91 91111 11111",
                "visitorAge",
                54,
                "visitorGender",
                "OTHER",
                "serviceId",
                service.path("publicId").asText(),
                "appointmentDate",
                LocalDate.now().toString(),
                "appointmentTime",
                secondSlot.toString(),
                "idempotencyKey",
                "senior-booking-" + suffix));
    String seniorTokenId = seniorBooking.path("publicId").asText();
    assertThat(seniorBooking.path("agePriority").asBoolean()).isTrue();
    assertThat(seniorBooking.path("tokenNumber").asInt())
        .isGreaterThan(booking.path("tokenNumber").asInt());

    ResponseEntity<String> citizenAdminEndpoint =
        rest.exchange(
            "/api/v1/counters/operation-options",
            HttpMethod.GET,
            entity(citizenToken),
            String.class);
    assertThat(citizenAdminEndpoint.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    ResponseEntity<String> officerCounters =
        rest.exchange(
            "/api/v1/officer/counters", HttpMethod.GET, entity(officer.token()), String.class);
    assertThat(officerCounters.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseEntity<String> adminOptions =
        rest.exchange(
            "/api/v1/counters/operation-options", HttpMethod.GET, entity(adminToken), String.class);
    assertThat(adminOptions.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(json.readTree(adminOptions.getBody()).path("data")).isNotEmpty();

    JsonNode called =
        post(
            "/api/v1/tokens/next",
            officer.token(),
            Map.of(
                "counterId",
                counter.path("publicId").asText(),
                "serviceId",
                service.path("publicId").asText()));
    assertThat(called.path("publicId").asText()).isEqualTo(seniorTokenId);
    assertThat(called.path("status").asText()).isEqualTo("CALLED");

    ResponseEntity<String> cancellation =
        rest.exchange(
            "/api/v1/tokens/" + seniorTokenId + "/cancel",
            HttpMethod.POST,
            entity(seniorCitizenToken),
            String.class);
    assertThat(cancellation.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    JsonNode completed =
        post(
            "/api/v1/tokens/" + seniorTokenId + "/complete",
            officer.token(),
            Map.of("counterId", counter.path("publicId").asText()));
    assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");
    ResponseEntity<String> history =
        rest.exchange(
            "/api/v1/tokens/history", HttpMethod.GET, entity(seniorCitizenToken), String.class);
    assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            json.readTree(history.getBody())
                .path("data")
                .path("content")
                .get(0)
                .path("status")
                .asText())
        .isEqualTo("COMPLETED");

    ResponseEntity<String> disabled =
        rest.exchange(
            "/api/v1/users/" + officer.id() + "/disable",
            HttpMethod.POST,
            entity(adminToken),
            String.class);
    assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseEntity<String> disabledOfficer =
        rest.exchange(
            "/api/v1/officer/counters", HttpMethod.GET, entity(officer.token()), String.class);
    assertThat(disabledOfficer.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    ResponseEntity<String> usersAfterDisabling =
        rest.exchange("/api/v1/users", HttpMethod.GET, entity(adminToken), String.class);
    assertThat(usersAfterDisabling.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(usersAfterDisabling.getBody()).contains("officer-" + suffix + "@example.test");
  }

  @Test
  void faviconRequestDoesNotProduceAnApplicationError() {
    ResponseEntity<String> response = rest.getForEntity("/favicon.ico", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void administratorCanChangeAnotherUsersRole() throws Exception {
    String suffix = String.valueOf(System.nanoTime());
    String adminToken = createStaff("admin-role-" + suffix + "@example.test", RoleName.ADMIN);
    Role citizenRole = roles.findByName(RoleName.CITIZEN).orElseThrow();
    String citizenEmail = "citizen-role-" + suffix + "@example.test";
    UserAccount citizen =
        users.save(
            new UserAccount(
                java.util.UUID.randomUUID(),
                citizenEmail,
                passwords.encode(TEST_PASSWORD),
                citizenRole));

    ResponseEntity<String> promote =
        rest.exchange(
            "/api/v1/users/" + citizen.getPublicId() + "/role",
            HttpMethod.PUT,
            entity(adminToken, Map.of("role", "OFFICER")),
            String.class);

    assertThat(promote.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(json.readTree(promote.getBody()).path("data").path("role").asText())
        .isEqualTo("OFFICER");
    ResponseEntity<String> promotedOfficer =
        rest.exchange(
            "/api/v1/officer/counters", HttpMethod.GET, entity(login(citizenEmail)), String.class);
    assertThat(promotedOfficer.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private String createStaff(String email, RoleName roleName) throws Exception {
    Role role = roles.findByName(roleName).orElseThrow();
    users.save(
        new UserAccount(java.util.UUID.randomUUID(), email, passwords.encode(TEST_PASSWORD), role));
    return login(email);
  }

  private Staff createOfficer(String email) throws Exception {
    Role role = roles.findByName(RoleName.OFFICER).orElseThrow();
    UserAccount officer =
        users.save(
            new UserAccount(
                java.util.UUID.randomUUID(), email, passwords.encode(TEST_PASSWORD), role));
    return new Staff(officer.getPublicId().toString(), login(email));
  }

  private String registerCitizen(String email) throws Exception {
    return post("/api/v1/auth/register", null, Map.of("email", email, "password", TEST_PASSWORD))
        .path("accessToken")
        .asText();
  }

  private String login(String email) throws Exception {
    return post("/api/v1/auth/login", null, Map.of("email", email, "password", TEST_PASSWORD))
        .path("accessToken")
        .asText();
  }

  private JsonNode post(String path, String token, Map<String, ?> body) throws Exception {
    ResponseEntity<String> response =
        rest.exchange(path, HttpMethod.POST, entity(token, body), String.class);
    assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CREATED);
    return json.readTree(response.getBody()).path("data");
  }

  private HttpEntity<Void> entity(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(headers);
  }

  private HttpEntity<Map<String, ?>> entity(String token, Map<String, ?> body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) headers.setBearerAuth(token);
    return new HttpEntity<>(body, headers);
  }

  private record Staff(String id, String token) {}
}
