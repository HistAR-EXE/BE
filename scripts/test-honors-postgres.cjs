// Tests the exact Java repository SQL against temporary PostgreSQL tables.
// The transaction rolls back; no application profiles, XP or badges are changed.
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const source = fs.readFileSync(path.join(__dirname, '../src/main/java/com/histar/be/leaderboard/repository/LeaderboardReadRepository.java'), 'utf8')
const match = source.match(/String sql = """([\s\S]*?)""" \+ filter \+ """([\s\S]*?)""";/)
if (!match) throw new Error('Repository SQL layout changed: update this harness before running')
const viewer = 'f2000000-0000-4000-8000-00000000000d'
const sampleHash = source.match(/LEGACY_SEED_HASH = "([^"]+)"/)[1]
const sampleEmails = Array.from({ length: 8 }, (_, i) => [`'user${i + 1}@histar.vn'`, `'demo_user_${i + 1}@histar.vn'`]).flat().join(',')
const query = (match[1] + match[2]).replaceAll(':limit', '3').replaceAll(':viewer', `'${viewer}'::uuid`)
  .replaceAll(':legacySeedHash', `'${sampleHash}'`).replaceAll(':legacySeedEmails', sampleEmails)
const sql = `BEGIN;
CREATE TEMP TABLE profiles(id uuid primary key, display_name text, avatar_url text, email text, total_points integer, role text, city text, org_id uuid, password_hash text) ON COMMIT DROP;
CREATE TEMP TABLE user_badges(user_id uuid, badge_id uuid) ON COMMIT DROP;
CREATE TEMP TABLE study_group_members(user_id uuid, group_id uuid) ON COMMIT DROP;
INSERT INTO profiles(id, display_name, email, total_points, role)
SELECT ('f2000000-0000-4000-8000-' || lpad(to_hex(i),12,'0'))::uuid, 'QA', 'qa' || i || '@example.test', CASE WHEN i <= 2 THEN 2000 ELSE 1500-i*50 END, 'USER' FROM generate_series(1,15) i;
INSERT INTO profiles(id, email, total_points, role) VALUES
('f2000000-0000-4000-8000-000000000099','qa-admin@example.test',99999,'ADMIN'),
('a1000000-0000-4000-8000-000000000001','seed-analytics-001@histar.vn',99998,'USER');
INSERT INTO profiles(id,email,total_points,role,password_hash) VALUES(gen_random_uuid(),'user1@histar.vn',99997,'USER','${sampleHash}');
INSERT INTO user_badges VALUES('${viewer}',gen_random_uuid());
CREATE TEMP TABLE qa_honors_before AS SELECT sum(total_points) total FROM profiles;
CREATE TEMP TABLE qa_honors_actual AS ${query};
DO $qa$ BEGIN
 IF (SELECT count(*) FROM qa_honors_actual) <> 8 THEN RAISE EXCEPTION 'Unexpected top + neighbor size'; END IF;
 IF (SELECT array_agg(honor_rank ORDER BY ordinal) FROM qa_honors_actual WHERE ordinal <= 3) <> ARRAY[1,1,3]::bigint[] THEN RAISE EXCEPTION 'Ties not ranked fairly'; END IF;
 IF NOT EXISTS(SELECT 1 FROM qa_honors_actual WHERE id='${viewer}' AND honor_rank=13 AND participants=15 AND badge_count=1) THEN RAISE EXCEPTION 'Viewer rank/count invalid'; END IF;
 IF (SELECT sum(total_points) FROM profiles) <> (SELECT total FROM qa_honors_before) THEN RAISE EXCEPTION 'XP was modified'; END IF;
END $qa$;
SELECT 'PASS PostgreSQL: shared ranks, viewer outside top, neighbors, eligibility, badge count, unchanged XP' result;
ROLLBACK;`
const database = process.env.HONORS_TEST_DATABASE || 'timelens'
const output = execFileSync('docker', ['exec', '-i', 'histar-postgres', 'psql', '-U', 'timelens', '-d', database, '-v', 'ON_ERROR_STOP=1'], { input: sql, encoding: 'utf8', timeout: 30000 })
process.stdout.write(output)
