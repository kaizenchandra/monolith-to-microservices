package demo.legacymonolith;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import demo.platform.Http;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LegacyFenceTest {
    @Test
    void frozenReadsNeverExposeStaleSourceDuringReverseMigration() {
        var db = mock(JdbcTemplate.class);
        when(db.queryForObject("select state from ownership where capability=? for share", String.class, "catalog")).thenReturn("FROZEN");
        var api = new LegacyApi(db, mock(Http.class), mock(org.springframework.transaction.support.TransactionTemplate.class));
        var failure = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> api.product(1));
        assertEquals(503, failure.getStatusCode().value());
        verify(db, never()).queryForList(anyString(), any(Object.class));
    }
}
