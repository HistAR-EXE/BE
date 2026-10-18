// Execute the exact stories aggregate SQL with temporary tables, always rollback.
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const source = fs.readFileSync(path.join(__dirname, '../src/main/java/com/histar/be/leaderboard/repository/HonorsStoriesRepository.java'), 'utf8')
const ranking = fs.readFileSync(path.join(__dirname, '../src/main/java/com/histar/be/leaderboard/repository/LeaderboardReadRepository.java'), 'utf8')
const match = source.match(/String sql="""(\s*WITH eligible[\s\S]*?)"""\+filter\+"""([\s\S]*?)""";/)
if (!match) throw new Error('Update harness: SQL layout changed')
const viewer = 'f3000000-0000-4000-8000-000000000003'
const hash = ranking.match(/LEGACY_SEED_HASH = "([^"]+)"/)[1]
const emails = Array.from({length:8},(_,i)=>[`'user${i+1}@histar.vn'`,`'demo_user_${i+1}@histar.vn'`]).flat().join(',')
const location = '11111111-1111-1111-1111-111111111111'
const query = (filter='') => (match[1]+filter+match[2]).replaceAll(':viewer',`'${viewer}'::uuid`).replaceAll(':location',`'${location}'::uuid`).replaceAll(':legacySeedHash',`'${hash}'`).replaceAll(':legacySeedEmails', emails)
const sql = `BEGIN;
CREATE TEMP TABLE profiles(id uuid primary key,display_name text,avatar_url text,email text,total_points integer,role text,city text,org_id uuid,password_hash text) ON COMMIT DROP;
CREATE TEMP TABLE user_badges(user_id uuid,badge_id uuid) ON COMMIT DROP;
CREATE TEMP TABLE locations(id uuid) ON COMMIT DROP;
CREATE TEMP TABLE user_discoveries(user_id uuid,location_id uuid,discovery_key text) ON COMMIT DROP;
CREATE TEMP TABLE study_group_members(user_id uuid,group_id uuid) ON COMMIT DROP;
INSERT INTO locations VALUES('${location}');
INSERT INTO profiles(id,email,total_points,role,city) SELECT ('f3000000-0000-4000-8000-'||lpad(i::text,12,'0'))::uuid,'qa'||i||'@test.invalid',1,'USER','TP.HCM' FROM generate_series(1,5)i;
INSERT INTO profiles(id,email,total_points,role) VALUES(gen_random_uuid(),'admin@test.invalid',999,'ADMIN'),('a1000000-0000-4000-8000-000000000001','seed-analytics-001@histar.vn',999,'USER');
INSERT INTO user_discoveries SELECT id,'${location}','cipher:hoang-cam' FROM profiles;
INSERT INTO user_discoveries SELECT id,'${location}','cipher:two-wells' FROM profiles WHERE id::text IN ('f3000000-0000-4000-8000-000000000001','f3000000-0000-4000-8000-000000000002');
CREATE TEMP TABLE actual AS ${query()};
DO $qa$ BEGIN
 IF (SELECT array_agg(honor_rank ORDER BY ordinal) FROM actual WHERE category='ciphers' AND ordinal<=3) <> ARRAY[1,1,3]::bigint[] THEN RAISE EXCEPTION 'Competition ties failed'; END IF;
 IF NOT EXISTS(SELECT 1 FROM actual WHERE category='ciphers' AND id='${viewer}' AND honor_rank=3 AND achievers=5) THEN RAISE EXCEPTION 'Viewer rank/eligibility failed'; END IF;
 IF (SELECT count(*) FROM actual WHERE category='ciphers' AND ordinal<=3)<>3 THEN RAISE EXCEPTION 'Leader cap failed'; END IF;
END $qa$;
INSERT INTO study_group_members VALUES('${viewer}','f3000000-0000-4000-8000-000000000099');
CREATE TEMP TABLE grouped AS ${query(" AND EXISTS (SELECT 1 FROM study_group_members m WHERE m.user_id=p.id AND m.group_id='f3000000-0000-4000-8000-000000000099')")};
DO $qa$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM grouped WHERE category='ciphers' AND id='${viewer}' AND achievers=1 AND honor_rank=1) THEN RAISE EXCEPTION 'Group boundary failed'; END IF;
 IF (SELECT sum(total_points) FROM profiles)<>2003 THEN RAISE EXCEPTION 'XP changed'; END IF;
END $qa$;
SELECT 'PASS stories PostgreSQL: ties1/1/3, cap3, viewer, eligibility, group boundary, unchanged XP' result;
ROLLBACK;`
process.stdout.write(execFileSync('docker',['exec','-i','histar-postgres','psql','-U','timelens','-d',process.env.HONORS_TEST_DATABASE||'timelens','-v','ON_ERROR_STOP=1'],{input:sql,encoding:'utf8',timeout:30000}))

