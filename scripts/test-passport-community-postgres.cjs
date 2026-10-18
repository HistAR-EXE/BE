// Exact repository SQL against temporary tables only. Always rolls back; no application writes.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict')
const {execFileSync}=require('node:child_process')
const root=path.join(__dirname,'../src/main/java/com/histar/be/leaderboard/repository')
const ranking=fs.readFileSync(path.join(root,'LeaderboardReadRepository.java'),'utf8')
const facts=fs.readFileSync(path.join(root,'HonorsFactsRepository.java'),'utf8')
const rank=ranking.match(/String sql = """([\s\S]*?)""" \+ filter \+ """([\s\S]*?)""";/)
const community=facts.match(/String sql="""([\s\S]*?)"""\+filter\+"""([\s\S]*?)""";/)
assert.ok(rank&&community,'Repository layout changed: update exact SQL harness')
const viewer='f4000000-0000-4000-8000-000000000015',location='11111111-1111-1111-1111-111111111111',group='f4000000-0000-4000-8000-000000000099'
const hash=ranking.match(/LEGACY_SEED_HASH = "([^"]+)"/)[1]
const emails=Array.from({length:8},(_,i)=>[`'user${i+1}@histar.vn'`,`'demo_user_${i+1}@histar.vn'`]).flat().join(',')
const catalog=fs.readFileSync(path.join(__dirname,'../src/main/java/com/histar/be/discovery/explore/ExploreChallengeCatalog.java'),'utf8')
const keys=[...catalog.matchAll(/(?:Map.entry\(|CONCLUSION = |Set.of\()"([^"\n]+)"/g)].map(m=>m[1])
for(const key of ['clue:ev-02','clue:ev-03','clue:ev-04'])keys.push(key)
assert.equal(new Set(keys).size,18)
const bind=s=>s.replaceAll(':legacySeedHash',`'${hash}'`).replaceAll(':legacySeedEmails',emails).replaceAll(':viewer',`'${viewer}'::uuid`).replaceAll(':limit','10').replaceAll(':location',`'${location}'::uuid`).replaceAll(':keys',keys.map(k=>`'${k}'`).join(','))
const cq=(filter='')=>bind(community[1]+filter+community[2])
const passport=facts.match(/jdbc.query\("(SELECT badge_id[^"\n]+)"/)[1]
const passportQuery=bind(passport).replaceAll(':ids',"'b0000001-0000-4000-8000-000000000201'::uuid")
const sql=`BEGIN;
CREATE TEMP TABLE profiles(id uuid primary key,display_name text,avatar_url text,email text,total_points int,role text,city text,org_id uuid,password_hash text,provider text) ON COMMIT DROP;
CREATE TEMP TABLE user_badges(user_id uuid,badge_id uuid,earned_at timestamptz) ON COMMIT DROP;
CREATE TEMP TABLE user_discoveries(user_id uuid,location_id uuid,discovery_key text) ON COMMIT DROP;
CREATE TEMP TABLE locations(id uuid) ON COMMIT DROP;
CREATE TEMP TABLE study_group_members(user_id uuid,group_id uuid) ON COMMIT DROP;
INSERT INTO locations VALUES('${location}');
INSERT INTO profiles(id,email,total_points,role,city) SELECT ('f4000000-0000-4000-8000-'||lpad(i::text,12,'0'))::uuid,'qa'||i||'@test.invalid',CASE WHEN i<9 THEN 2000-i ELSE 100 END,'USER','Hue' FROM generate_series(1,15)i;
INSERT INTO profiles(id,email,total_points,role,provider) VALUES(gen_random_uuid(),'preview@test.invalid',99999,'USER','ui-preview-honors'),(gen_random_uuid(),'admin@test.invalid',99999,'ADMIN',null),('a1000000-0000-4000-8000-000000000001','seed-analytics-001@histar.vn',99999,'USER',null);
-- Ranking deliberately retains local preview users. Exclude them only for this official ordering fixture.
UPDATE profiles SET total_points=0 WHERE provider='ui-preview-honors';
INSERT INTO user_badges VALUES('f4000000-0000-4000-8000-000000000014','b0000001-0000-4000-8000-000000000202',NULL);
CREATE TEMP TABLE ranked AS ${bind(rank[1]+rank[2])};
DO $qa$ BEGIN
 IF (SELECT count(*) FROM ranked WHERE ordinal<=10)<>10 THEN RAISE EXCEPTION 'Top ten cap'; END IF;
 IF NOT EXISTS(SELECT 1 FROM ranked WHERE ordinal=9 AND id='f4000000-0000-4000-8000-000000000014' AND honor_rank=9 AND xp_peers=7) THEN RAISE EXCEPTION 'Badge ordering / XP rank / peer count'; END IF;
 IF NOT EXISTS(SELECT 1 FROM ranked WHERE id='${viewer}' AND ordinal=15 AND honor_rank=9) THEN RAISE EXCEPTION 'Viewer retained outside ten'; END IF;
 IF (SELECT xp_peers-2 FROM ranked WHERE ordinal=10)<>5 THEN RAISE EXCEPTION 'Omitted cutoff peers'; END IF;
END $qa$;
INSERT INTO user_badges VALUES('${viewer}','b0000001-0000-4000-8000-000000000201',NULL),('${viewer}','b0000001-0000-4000-8000-000000000201',NULL);
INSERT INTO user_discoveries VALUES('${viewer}','${location}','cipher:hoang-cam'),('${viewer}','${location}','cipher:hoang-cam'),('${viewer}','${location}','bogus'),('${viewer}',gen_random_uuid(),'cipher:sandals');
CREATE TEMP TABLE passport AS ${passportQuery};
CREATE TEMP TABLE community AS ${cq()};
INSERT INTO study_group_members VALUES('${viewer}','${group}');
CREATE TEMP TABLE grouped AS ${cq(` AND EXISTS (SELECT 1 FROM study_group_members m WHERE m.user_id=p.id AND m.group_id='${group}')`)};
DO $qa$ BEGIN
 IF (SELECT count(*) FROM passport)<>1 OR EXISTS(SELECT 1 FROM passport WHERE earned_at IS NOT NULL) THEN RAISE EXCEPTION 'Persisted null-date earned badge'; END IF;
 IF NOT EXISTS(SELECT 1 FROM community WHERE explorers=15 AND completed_challenges=1 AND discoveries=1 AND badges=2) THEN RAISE EXCEPTION 'Distinct metrics / eligibility'; END IF;
 IF NOT EXISTS(SELECT 1 FROM grouped WHERE explorers=1 AND completed_challenges=1 AND discoveries=1 AND badges=1) THEN RAISE EXCEPTION 'Group boundary'; END IF;
END $qa$;
SELECT 'PASS PostgreSQL exact SQL: cap10, XP ranks, badge display order, cutoff peers, outside viewer, persisted passport/null dates, distinct official counters, group boundary' result;
ROLLBACK;`
process.stdout.write(execFileSync('docker',['exec','-i','histar-postgres','psql','-U','timelens','-d',process.env.HONORS_TEST_DATABASE||'timelens','-v','ON_ERROR_STOP=1'],{input:sql,encoding:'utf8',timeout:30000}))
