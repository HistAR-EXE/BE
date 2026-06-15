# BE — TimeLens Backend

Spring Boot API. Tài liệu dự án tập trung tại [`../docs/`](../docs/).

| Path | Mục đích |
|------|----------|
| [`../docs/04_api_&_integration_guide.md`](../docs/04_api_&_integration_guide.md) | API contract |
| [`../docs/05_development_&_deployment.md`](../docs/05_development_&_deployment.md) | Setup, test, deploy |
| [`database/`](./database/) | SQL schema + migrations |
| [`scripts/`](./scripts/) | Seed, smoke test PowerShell |

```powershell
cd BE
mvn test
.\docs\scripts\run-all-seed.ps1
```
