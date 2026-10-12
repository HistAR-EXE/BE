ALTER TABLE user_narrative_investigation_progress ADD COLUMN investigation_version INTEGER;
ALTER TABLE user_narrative_investigation_progress ADD COLUMN visited_node_codes_json TEXT;
ALTER TABLE user_narrative_investigation_progress ADD COLUMN evidence_codes_json TEXT;
ALTER TABLE user_narrative_investigation_progress ADD COLUMN selected_outcome_code VARCHAR(120);
ALTER TABLE narrative_investigations ADD COLUMN review_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE narrative_investigations ADD COLUMN approved_locales_json TEXT NOT NULL DEFAULT '[]';
ALTER TABLE narrative_investigations ADD COLUMN reviewed_by VARCHAR(160);
ALTER TABLE narrative_investigations ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;
-- The Củ Chi pilot remains disabled until specialist review supplies approved citations/translations.
INSERT INTO narrative_investigations (id, quest_id, version, enabled, graph_json, created_at, updated_at, review_status, approved_locales_json)
SELECT '33333333-3333-3333-3333-333333333390'::uuid, '33333333-3333-3333-3333-333333333338'::uuid, 1, false,
'{"startNodeCode":"tour-360","nodes":[{"code":"tour-360","type":"TOUR_360_OBSERVATION","choices":[{"code":"interview","targetNodeCode":"ai-interview"}]},{"code":"ai-interview","type":"AI_INTERVIEW","choices":[{"code":"compare","targetNodeCode":"source-comparison"}]},{"code":"source-comparison","type":"SOURCE_COMPARISON","choices":[{"code":"evidence","targetNodeCode":"evidence-lid"}]},{"code":"evidence-lid","type":"EVIDENCE","choices":[{"code":"quick","targetNodeCode":"outcome-partial"},{"code":"full","targetNodeCode":"outcome-verified"}]},{"code":"outcome-partial","type":"OUTCOME","minimumEvidence":0,"choices":[]},{"code":"outcome-verified","type":"OUTCOME","minimumEvidence":1,"choices":[]}],"evidenceNodeCodes":["evidence-lid"],"outcomeNodeCodes":["outcome-partial","outcome-verified"]}', NOW(), NOW(), 'DRAFT', '[]'
WHERE NOT EXISTS (SELECT 1 FROM narrative_investigations WHERE quest_id='33333333-3333-3333-3333-333333333338'::uuid);
