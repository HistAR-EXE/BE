-- POI name snapshot for session replay (immutable label at event time)
ALTER TABLE visit_session_events
  ADD COLUMN IF NOT EXISTS poi_name_snapshot VARCHAR(255);
