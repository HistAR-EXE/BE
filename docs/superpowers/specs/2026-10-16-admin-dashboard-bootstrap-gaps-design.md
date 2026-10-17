# Admin Dashboard and Bootstrap Gaps Design

## Scope

Close only the verified Admin Dashboard metric/reliability gaps and regenerate the full local bootstrap through the latest existing migration, V76. Alert thresholds and scheduled exports remain out of scope.

## Dashboard contract

- `draftCount` counts every open narrative draft, regardless of review result.
- `reviewPendingCount` counts open narrative drafts whose review status is `DRAFT`.
- The Dashboard labels these independently as “Bản nháp đang mở” and “Chờ duyệt”.
- `openContentReports` counts report cases in `NEW` or `IN_REVIEW` and supplies “Báo cáo đang mở”.
- `contentReportsLast30Days` remains a separate count of report events received during the last 30 days. The Dashboard does not display it under an open-report label.

## Revenue no-data behavior

The financial service returns an empty revenue trend when all requested monthly buckets contain zero B2C and zero B2B revenue. MoM remains nullable when there is no prior-month baseline. The Dashboard renders explicit unavailable copy for null MoM and does not render a synthetic minimum-width trend bar.

## Bootstrap

Run `docs/scripts/New-HistarFullBootstrapSql.ps1`; never edit `TimeLens_full_bootstrap.sql` manually. The generated header, migration sections, and Flyway baseline must resolve to V76. Validate with both bootstrap structure and runner tests.

## Verification

- Focused BE tests for `AdminDashboardService`, `AdminFinancialService`, and `AdminBillingOverviewUiResponse`.
- FE Admin test suite and production build.
- Bootstrap SQL structure test and bootstrap runner structure test.

