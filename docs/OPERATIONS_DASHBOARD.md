# Production dashboard

The backend exposes Prometheus metrics at `GET /actuator/prometheus`. Keep this endpoint behind the
internal monitoring network or an authenticated reverse proxy; it is not intended for browser clients.

Configure Prometheus to scrape `https://<backend-host>/actuator/prometheus`, then create these Grafana
panels over a five-minute window:

| Panel | PromQL |
| --- | --- |
| Chat p95 latency | `histogram_quantile(0.95, sum(rate(histar_chat_latency_seconds_bucket[5m])) by (le))` |
| Chat fallback rate | `sum(rate(histar_chat_fallback_total[5m])) / sum(rate(histar_chat_latency_seconds_count[5m]))` |
| RAG citation rate | `sum(rate(histar_rag_citations_total{outcome="cited"}[5m])) / sum(rate(histar_rag_citations_total[5m]))` |
| Quest abandonment rate | `clamp_min(1 - (sum(increase(histar_quest_funnel_total{event="completed"}[24h])) / sum(increase(histar_quest_funnel_total{event="started"}[24h]))), 0)` |
| Minigame failure rate | `sum(rate(histar_minigame_attempts_total{outcome="failed"}[5m])) / sum(rate(histar_minigame_attempts_total{outcome=~"passed|failed"}[5m]))` |

Quest abandonment is intentionally a funnel metric: a started quest that has not completed in the selected
window is counted as abandonment. It does not prematurely label an in-progress quest as abandoned in the
transactional database.

Suggested alerts: chat p95 above 10 seconds for 10 minutes; fallback rate above 20%; citation rate below
80%; quest abandonment above 60% over 24 hours; and minigame failure above 50% for 15 minutes.
