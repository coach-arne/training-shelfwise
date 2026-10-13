package be.npl.shelfwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import be.npl.shelfwise.catalogue.entity.Copy;
import be.npl.shelfwise.catalogue.entity.MaterialType;
import be.npl.shelfwise.lending.service.LoanQueryService;
import be.npl.shelfwise.member.entity.Member;
import be.npl.shelfwise.support.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Appendix A.9 (permissions) and the HTTP contract of the API: roles, reports, administration and training. */
@AutoConfigureMockMvc
class ApiIT extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    ApiClient api;

    @BeforeEach
    void client() {
        api = new ApiClient(mvc, json);
    }
    @Autowired LoanQueryService loanQueries;

    private int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private String cardOf(int member) {
        return fx.member(member).getCardNumber();
    }

    // ----- A.9 -----

    @Test
    @DisplayName("AC-ROL-01 a librarian of RIV cannot request R-02 for CEN, but can for their own branch")
    void librarianReportScope() throws Exception {
        Long cen = fx.branch("CEN").getId();
        Long riv = fx.branch("RIV").getId();
        assertThat(status(api.perform(api.as("librarian.riv", get("/api/reports/r02").param("branchId", String.valueOf(cen))))))
                .isEqualTo(403);
        assertThat(status(api.perform(api.as("librarian.riv", get("/api/reports/r02").param("branchId", String.valueOf(riv))))))
                .isEqualTo(200);
        assertThat(status(api.perform(api.as("manager.cen", get("/api/reports/r02").param("branchId", String.valueOf(riv))))))
                .isEqualTo(200);
        assertThat(status(api.perform(api.as("admin", get("/api/reports/r02"))))).isEqualTo(200);
    }

    @Test
    @DisplayName("AC-ROL-02 an administrator cannot check out a copy")
    void administratorCannotCheckOut() throws Exception {
        Copy copy = fx.newCopy(MaterialType.BOOK, "CEN");
        MvcResult result = api.perform(api.jsonBody(api.as("admin", post("/api/desk/checkout")),
                Map.of("cardNumber", cardOf(1), "barcode", copy.getBarcode())));
        assertThat(status(result)).isEqualTo(403);
        MvcResult allowed = api.perform(api.jsonBody(api.as("librarian.cen", post("/api/desk/checkout")),
                Map.of("cardNumber", cardOf(1), "barcode", copy.getBarcode())));
        assertThat(status(allowed)).isEqualTo(200);
    }

    @Test
    @DisplayName("AC-ROL-03 a member cannot see or renew the loans of another member")
    void memberSeesOnlyOwnData() throws Exception {
        Member kim = fx.member(11);
        assertThat(status(api.perform(api.as(cardOf(1), get("/api/desk/members/" + kim.getId() + "/loans"))))).isEqualTo(403);
        JsonNode own = api.read(api.perform(api.as(cardOf(11), get("/api/portal/loans"))));
        assertThat(own).hasSize(1);
        JsonNode adas = api.read(api.perform(api.as(cardOf(1), get("/api/portal/loans"))));
        assertThat(adas).isEmpty();
        Long kimsLoan = loanQueries.openLoansOf(kim.getId()).get(0).id();
        assertThat(status(api.perform(api.as(cardOf(1), post("/api/portal/loans/" + kimsLoan + "/renew"))))).isEqualTo(403);
    }

    @Test
    @DisplayName("AC-ROL-05 an anonymous visitor can search the catalogue with availability per branch")
    void anonymousCatalogueSearch() throws Exception {
        MvcResult result = api.perform(get("/api/catalogue/search").param("q", "lighthouse"));
        assertThat(status(result)).isEqualTo(200);
        JsonNode first = api.read(result).get(0);
        assertThat(first.get("title").asText()).contains("Lighthouse");
        assertThat(first.get("availability")).hasSize(6);
        assertThat(first.path("copies").isMissingNode() || first.get("copies").isNull()).isTrue(); // copy details are for staff only
    }

    @Test
    @DisplayName("Permission matrix: anonymous, member, librarian and administrator on protected areas")
    void permissionMatrix() throws Exception {
        assertThat(status(api.perform(get("/api/portal/me")))).isEqualTo(401);
        assertThat(status(api.perform(get("/api/reports/r01")))).isEqualTo(401);
        assertThat(status(api.perform(api.as(cardOf(1), get("/api/reports/r01"))))).isEqualTo(403);
        assertThat(status(api.perform(api.as(cardOf(1), get("/api/training/state"))))).isEqualTo(403);
        assertThat(status(api.perform(api.as("librarian.cen", get("/api/training/state"))))).isEqualTo(403);
        assertThat(status(api.perform(api.as("librarian.cen", get("/api/admin/holidays").param("year", "2026"))))).isEqualTo(403);
        assertThat(status(api.perform(api.as("manager.cen", get("/api/admin/fine-rates"))))).isEqualTo(403);
        assertThat(status(api.perform(api.as("admin", get("/api/admin/fine-rates"))))).isEqualTo(200);
        assertThat(status(api.perform(api.as("admin", get("/api/training/state"))))).isEqualTo(200);
        assertThat(status(api.perform(api.as("librarian.cen", get("/api/portal/me"))))).isEqualTo(403);
    }

    // ----- reports -----

    @Test
    @DisplayName("F-REP-01 R-02 lists overdue items with accrued fines; librarians see their own branch; CSV export")
    void overdueReport() throws Exception {
        JsonNode cen = api.read(api.perform(api.as("librarian.cen", get("/api/reports/r02"))));
        // seed: S-09 (CEN), S-13 book and AV (CEN); S-08 was checked out at RIV
        assertThat(cen).hasSize(3);
        assertThat(cen.findValuesAsText("branch")).containsOnly("CEN");
        JsonNode all = api.read(api.perform(api.as("manager.cen", get("/api/reports/r02"))));
        assertThat(all).hasSize(4);
        assertThat(all.findValuesAsText("memberName")).contains("Hugo Hale");

        MvcResult csv = api.perform(api.as("admin", get("/api/reports/r02").param("format", "csv")));
        assertThat(csv.getResponse().getContentType()).startsWith("text/csv");
        String body = csv.getResponse().getContentAsString();
        assertThat(body).startsWith("member,card_number,barcode,title");
        assertThat(body.lines().count()).isEqualTo(5);
    }

    @Test
    @DisplayName("F-REP-01 R-01 counts checkouts, returns and renewals per branch and material type")
    void circulationReport() throws Exception {
        Copy copy = fx.newCopy(MaterialType.BOOK, "CEN");
        api.perform(api.jsonBody(api.as("librarian.cen", post("/api/desk/checkout")),
                Map.of("cardNumber", cardOf(1), "barcode", copy.getBarcode())));
        api.perform(api.jsonBody(api.as("librarian.riv", post("/api/desk/returns")), Map.of("barcode", copy.getBarcode())));

        JsonNode cen = api.read(api.perform(api.as("librarian.cen", get("/api/reports/r01"))));
        assertThat(cen).hasSize(1);
        assertThat(cen.get(0).get("checkouts").asInt()).isEqualTo(1);
        assertThat(cen.get(0).get("returns").asInt()).isZero();
        JsonNode all = api.read(api.perform(api.as("manager.cen", get("/api/reports/r01"))));
        assertThat(all).hasSize(2);
        String csv = api.perform(api.as("manager.cen", get("/api/reports/r01").param("format", "csv")))
                .getResponse().getContentAsString();
        assertThat(csv).startsWith("branch,material_type,checkouts,returns,renewals");
    }

    // ----- administration -----

    @Test
    @DisplayName("F-ADM-01 fine rates: only future rates can be added or removed; SENIOR is derived")
    void fineRates() throws Exception {
        var past = Map.of("category", "ADULT", "materialType", "BOOK", "dailyRate", "0.30", "validFrom", "2026-03-15");
        MvcResult refused = api.perform(api.jsonBody(api.as("admin", post("/api/admin/fine-rates")), past));
        assertThat(status(refused)).isEqualTo(422);
        assertThat(api.read(refused).get("code").asText()).isEqualTo("PAST_RATE");

        var senior = Map.of("category", "SENIOR", "materialType", "BOOK", "dailyRate", "0.30", "validFrom", "2026-04-01");
        assertThat(api.read(api.perform(api.jsonBody(api.as("admin", post("/api/admin/fine-rates")), senior))).get("code").asText())
                .isEqualTo("SENIOR_RATE_DERIVED");

        var future = Map.of("category", "ADULT", "materialType", "BOOK", "dailyRate", "0.30", "validFrom", "2026-04-01");
        MvcResult created = api.perform(api.jsonBody(api.as("admin", post("/api/admin/fine-rates")), future));
        assertThat(status(created)).isEqualTo(200);
        long id = api.read(created).get("id").asLong();
        assertThat(status(api.perform(api.as("admin", delete("/api/admin/fine-rates/" + id))))).isEqualTo(200);

        JsonNode rates = api.read(api.perform(api.as("admin", get("/api/admin/fine-rates"))));
        long existingId = rates.get(0).get("id").asLong();
        MvcResult deleteExisting = api.perform(api.as("admin", delete("/api/admin/fine-rates/" + existingId)));
        assertThat(status(deleteExisting)).isEqualTo(422);
    }

    @Test
    @DisplayName("F-ADM-01 a new rate is used from its valid-from date")
    void newRateApplies() throws Exception {
        var future = Map.of("category", "ADULT", "materialType", "BOOK", "dailyRate", "0.40", "validFrom", "2026-03-17");
        api.perform(api.jsonBody(api.as("admin", post("/api/admin/fine-rates")), future));
        fx.today(java.time.LocalDate.of(2026, 2, 9));
        Copy copy = fx.newCopy(MaterialType.BOOK, "CEN");
        api.perform(api.jsonBody(api.as("librarian.cen", post("/api/desk/checkout")),
                Map.of("cardNumber", cardOf(1), "barcode", copy.getBarcode())));
        fx.today(java.time.LocalDate.of(2026, 3, 4));
        JsonNode loan = api.read(api.perform(api.as(cardOf(1), get("/api/portal/loans")))).get(0);
        assertThat(loan.get("accruedFine").decimalValue()).isEqualByComparingTo("0.50"); // old rate: 2 x 0.25
        fx.today(java.time.LocalDate.of(2026, 3, 18));
        loan = api.read(api.perform(api.as(cardOf(1), get("/api/portal/loans")))).get(0);
        // due 03-02, 03-18: 15 days minus 2 Sundays (8th, 15th) = 14 chargeable days at the new rate 0.40
        assertThat(loan.get("accruedFine").decimalValue()).isEqualByComparingTo("5.60");
    }

    @Test
    @DisplayName("F-ADM-02 public holidays can be added and removed, and change the due dates")
    void holidays() throws Exception {
        var holiday = Map.of("date", "2026-03-23", "name", "Test holiday");
        MvcResult created = api.perform(api.jsonBody(api.as("admin", post("/api/admin/holidays")), holiday));
        assertThat(status(created)).isEqualTo(200);
        assertThat(api.read(api.perform(api.jsonBody(api.as("admin", post("/api/admin/holidays")), holiday))).get("code").asText())
                .isEqualTo("DUPLICATE_HOLIDAY");
        assertThat(api.read(api.perform(api.as("admin", get("/api/admin/holidays").param("year", "2026")))))
                .hasSize(11);

        fx.today(java.time.LocalDate.of(2026, 3, 2));
        Copy copy = fx.newCopy(MaterialType.BOOK, "CEN");
        JsonNode receipt = api.read(api.perform(api.jsonBody(api.as("librarian.cen", post("/api/desk/checkout")),
                Map.of("cardNumber", cardOf(1), "barcode", copy.getBarcode()))));
        assertThat(receipt.get("loan").get("dueDate").asText()).isEqualTo("2026-03-24");

        api.perform(api.as("admin", delete("/api/admin/holidays/" + api.read(created).get("id").asLong())));
        assertThat(api.read(api.perform(api.as("admin", get("/api/admin/holidays").param("year", "2026"))))).hasSize(10);
    }

    // ----- registration -----

    @Test
    @DisplayName("F-MEM-02 registration warns about a possible duplicate and needs a guardian for a YOUTH")
    void registration() throws Exception {
        var duplicate = new java.util.HashMap<String, Object>(Map.of("firstName", "Ada", "lastName", "Adams",
                "birthDate", "1988-04-12", "street", "Elm Street", "postalCode", "1000", "city", "Northwick",
                "email", "ada2@example.org", "paymentMethod", "CASH"));
        MvcResult warned = api.perform(api.jsonBody(api.as("librarian.cen", post("/api/staff/members")), duplicate));
        assertThat(status(warned)).isEqualTo(422);
        assertThat(api.read(warned).get("code").asText()).isEqualTo("POSSIBLE_DUPLICATE");

        duplicate.put("confirmDuplicate", true);
        MvcResult created = api.perform(api.jsonBody(api.as("librarian.cen", post("/api/staff/members")), duplicate));
        assertThat(status(created)).isEqualTo(200);
        assertThat(api.read(created).get("temporaryPassword").asText()).isNotBlank();

        var youth = Map.of("firstName", "Yara", "lastName", "Young", "birthDate", "2014-01-01", "street", "Elm Street",
                "postalCode", "1000", "city", "Northwick", "email", "yara@example.org");
        MvcResult noGuardian = api.perform(api.jsonBody(api.as("librarian.cen", post("/api/staff/members")), youth));
        assertThat(api.read(noGuardian).get("code").asText()).isEqualTo("GUARDIAN_REQUIRED");
    }

    // ----- training -----

    @Test
    @DisplayName("F-TRN-01/03 the business date moves forward with catch-up, never backward, and a reset restores the seed")
    void trainingBusinessDate() throws Exception {
        MvcResult backward = api.perform(api.jsonBody(api.as("admin", post("/api/training/business-date")),
                Map.of("date", "2026-03-10")));
        assertThat(status(backward)).isEqualTo(422);
        assertThat(api.read(backward).get("code").asText()).isEqualTo("BACKWARD_NOT_ALLOWED");

        MvcResult forward = api.perform(api.jsonBody(api.as("admin", post("/api/training/business-date")),
                Map.of("date", "2026-03-18")));
        assertThat(status(forward)).isEqualTo(200);
        // processing ran for 2026-03-16 and 2026-03-17 (the day that is left is processed)
        assertThat(api.read(forward).get("datesProcessed")).hasSize(2);
        assertThat(api.read(forward).get("copiesDeclaredLost").asInt()).isEqualTo(1);
        JsonNode state = api.read(api.perform(api.as("admin", get("/api/training/state"))));
        assertThat(state.get("businessDate").asText()).isEqualTo("2026-03-18");
        assertThat(state.get("lastProcessedDate").asText()).isEqualTo("2026-03-17");

        JsonNode reset = api.read(api.perform(api.as("admin", post("/api/training/reset"))));
        assertThat(reset.get("businessDate").asText()).isEqualTo("2026-03-16");
        assertThat(fx.chargesOf(fx.member(9))).isEmpty();
    }

    @Test
    @DisplayName("F-TRN-02/04 running the daily processing now fills the outbox")
    void trainingRunNowAndOutbox() throws Exception {
        MvcResult run = api.perform(api.as("admin", post("/api/training/daily-processing")));
        assertThat(api.read(run).get("holdsExpired").asInt()).isEqualTo(1);
        JsonNode outbox = api.read(api.perform(api.as("admin", get("/api/training/outbox"))));
        assertThat(outbox.findValuesAsText("type")).contains("N-08", "N-06");
        assertThat(outbox.get(0).get("deliveryStatus").asText()).contains("simulated");
    }

    @Test
    @DisplayName("F-TRN-05 the simulated payment provider settles a member's online payment")
    void simulatedProvider() throws Exception {
        MvcResult started = api.perform(api.jsonBody(api.as(cardOf(18), post("/api/portal/payments")), Map.of("chargeIds", new long[0])));
        assertThat(status(started)).isEqualTo(200);
        String reference = api.read(started).get("reference").asText();

        JsonNode page = api.read(api.perform(get("/api/provider/payments/" + reference)));
        assertThat(page.get("amount").decimalValue()).isEqualByComparingTo("6.00");
        MvcResult result = api.perform(api.jsonBody(post("/api/provider/payments/" + reference + "/result"), Map.of("result", "SUCCEEDS")));
        assertThat(api.read(result).get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(fx.balance(fx.member(18))).isEqualByComparingTo("0.00");

        // another member cannot see this payment in the portal
        assertThat(status(api.perform(api.as(cardOf(1), get("/api/portal/payments/" + reference))))).isNotEqualTo(200);
    }

    @Test
    @DisplayName("F-MEM-07 members update their contact preferences and keep-history choice in the portal")
    void portalPreferences() throws Exception {
        var body = Map.of("email", "new.address@example.org", "phone", "+32 470 11 22 33", "smsOptIn", true, "keepHistory", true);
        MvcResult updated = api.perform(api.jsonBody(api.as(cardOf(1), org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/portal/me/preferences")), body));
        assertThat(status(updated)).isEqualTo(200);
        JsonNode me = api.read(api.perform(api.as(cardOf(1), get("/api/portal/me"))));
        assertThat(me.get("email").asText()).isEqualTo("new.address@example.org");
        assertThat(me.get("smsOptIn").asBoolean()).isTrue();
        assertThat(me.get("keepHistory").asBoolean()).isTrue();
    }
}
