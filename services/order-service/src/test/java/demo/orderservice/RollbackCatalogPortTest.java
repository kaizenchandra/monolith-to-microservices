package demo.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import demo.platform.Http;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RollbackCatalogPortTest {
    @Test
    void checkoutUsesOwnershipAwareSupplierDuringCatalogRollback() {
        var db = mock(JdbcTemplate.class);
        var http = mock(Http.class);
        when(http.url("LEGACY", 8081)).thenReturn("http://legacy");
        when(http.url("CATALOG", 8083)).thenReturn("http://catalog");
        when(http.get("http://legacy/api/customers/1")).thenReturn(Map.of("id", 1));
        when(http.get("http://legacy/api/products/2")).thenReturn(Map.of("price_cents", 203));
        when(http.get("http://catalog/api/products/2")).thenReturn(Map.of("price_cents", 100));
        when(db.queryForMap(anyString(), eq("rollback-price"))).thenReturn(Map.of("id", "rollback-price", "customer_id", 1L, "product_id", 2L, "quantity", 1, "amount_cents", 203L, "state", "NEW"));
        new OrderApi(db, http).create(new OrderApi.Checkout("rollback-price", 1, 2, 1));
        verify(db).update(anyString(), eq("rollback-price"), eq(1L), eq(2L), eq(1), eq(203L));
        verify(http, never()).get("http://catalog/api/products/2");
    }
}
