package com.histar.be.admin.service;

import com.histar.be.admin.dto.AdminDemoAccountResponse;
import com.histar.be.admin.dto.AdminDemoDatasetResponse;
import com.histar.be.admin.dto.PurgeDemoDatasetResponse;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.DemoProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDemoDatasetService {

    private static final String ACCOUNT_QUERY = """
            SELECT p.id, r.persona, p.email, p.display_name, p.role, p.tier, p.total_points
            FROM demo_account_registry r
            JOIN profiles p ON p.id = r.user_id
            WHERE r.deleted_at IS NULL
            ORDER BY r.persona
            """;

    private final JdbcTemplate jdbc;
    private final CurrentUserAccessor currentUser;
    private final DemoProperties properties;

    @Transactional(readOnly = true)
    public AdminDemoDatasetResponse preview() {
        if (!properties.isEnabled()) {
            return new AdminDemoDatasetResponse(false, 0, 0, List.of());
        }
        List<AdminDemoAccountResponse> accounts = jdbc.query(ACCOUNT_QUERY, this::mapAccount);
        Long operationalRows = jdbc.queryForObject(
                "SELECT demo_operational_row_count(NULL)", Long.class);
        return new AdminDemoDatasetResponse(
                true,
                accounts.size(),
                operationalRows == null ? 0 : operationalRows,
                accounts);
    }

    @Transactional
    public PurgeDemoDatasetResponse purge(UUID targetUserId, String reason) {
        if (!properties.isEnabled()) {
            throw new BusinessRuleException("Chỉ được xóa bộ dữ liệu mẫu trong môi trường demo");
        }
        UUID actorId = currentUser.getUserId()
                .orElseThrow(() -> new BusinessRuleException("Không xác định được admin thực hiện"));
        if (reason == null || reason.trim().length() < 10) {
            throw new BusinessRuleException("Lý do cần ít nhất 10 ký tự");
        }
        return jdbc.queryForObject(
                "SELECT deleted_accounts, deleted_rows FROM purge_demo_dataset(?, ?, ?)",
                (rs, rowNum) -> new PurgeDemoDatasetResponse(
                        rs.getInt("deleted_accounts"), rs.getLong("deleted_rows")),
                actorId,
                targetUserId,
                reason.trim());
    }

    private AdminDemoAccountResponse mapAccount(ResultSet rs, int rowNum) throws SQLException {
        return new AdminDemoAccountResponse(
                rs.getObject("id", UUID.class),
                rs.getInt("persona"),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("role"),
                rs.getString("tier"),
                rs.getLong("total_points"));
    }
}
