// Opt-in LOCAL ONLY API test. Creates its own two QA accounts, no email is sent.
// Requires histar-postgres and the local backend on port 8080. Always cleans its own fixtures.
const { execFileSync } = require('node:child_process')
const { randomUUID, randomBytes } = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const accounts = [], checks = []
function sql(input) {
  return execFileSync('docker', ['exec', '-i', 'histar-postgres', 'psql', '-U', 'timelens', '-d', 'timelens', '-At', '-v', 'ON_ERROR_STOP=1'], { input, encoding: 'utf8', timeout: 30000 }).trim()
}
function fingerprint() {
  return sql(`SELECT md5(COALESCE(string_agg(id::text || ':' || COALESCE(total_points,0)::text || ':' || COALESCE(level,1)::text, ',' ORDER BY id),'')) FROM profiles;
    SELECT count(*) FROM user_badges; SELECT count(*) FROM user_discoveries; SELECT count(*) FROM xp_ledger; SELECT count(*) FROM explore_attempts;`)
}
async function request(account, route, body) {
  const response = await fetch('http://localhost:8080/api' + route, { method: body ? 'POST' : 'GET',
    headers: { 'Content-Type': 'application/json', ...(account ? { Authorization: 'Bearer ' + account.token } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {}), signal: AbortSignal.timeout(30000) })
  return { status: response.status, cache: response.headers.get('cache-control'), json: await response.json() }
}
async function fixture(tier) {
  const id = randomUUID(), password = randomBytes(24).toString('hex'), email = `honors-qa-${id}@example.invalid`
  sql(`BEGIN; INSERT INTO profiles(id,email,password_hash,display_name,email_verified,email_verified_at,role,tier,total_points,level)
    VALUES('${id}','${email}',crypt('${password}',gen_salt('bf')),'Honors QA',true,now(),'USER','${tier}',177,2);
    INSERT INTO xp_ledger(user_id,event_key,points) VALUES('${id}','baseline:v1',177); COMMIT;`)
  const account = { id, email }; accounts.push(account)
  const login = await request(null, '/auth/login', { email, password })
  assert.equal(login.status, 200, 'QA login must succeed')
  account.token = login.json.data.token || login.json.data.accessToken
  assert.ok(account.token, 'Login must return a session')
  return account
}
function pass(name) { checks.push(name); console.log('PASS ' + name) }
;(async () => {
  let cleaned = false
  const before = fingerprint()
  try {
    const free = await fixture('FREE'), premium = await fixture('PREMIUM')
    const first = await request(free, '/leaderboard'), second = await request(premium, '/leaderboard')
    assert.equal(first.status, 200); assert.equal(second.status, 200)
    const a = first.json.data, b = second.json.data
    assert.equal(a.viewer.totalPoints, 177); assert.equal(a.viewer.rank, b.viewer.rank)
    assert.equal(a.totalParticipants, b.totalParticipants); assert.equal(a.viewerRankLocked, false)
    pass('Free and Premium authenticate and share the same XP/rank rules')
    const mailStatus = await request(free, '/leaderboard/passport/email/status')
    assert.equal(mailStatus.status, 200)
    assert.equal(mailStatus.json.data.email, free.email)
    assert.equal(mailStatus.json.data.verified, true)
    assert.match(mailStatus.cache, /no-store/)
    pass('Local application exposes verified owner-only passport email status without sending mail')
    assert.ok(a.nearby.some(e => e.userId === free.id)); assert.equal(a.viewer.userId, free.id)
    assert.ok(a.entries.every(e => e.userId !== premium.id || e.currentUser === false))
    assert.match(first.cache, /no-store/)
    assert.ok([a.viewer, ...a.entries, ...a.nearby].every(e => !('email' in e) && !('passwordHash' in e)))
    pass('Viewer/nearby ranks are personalized and responses expose no email or shared cache')
    assert.equal((await request(null, '/leaderboard')).status, 401)
    assert.equal((await request(free, '/leaderboard?scope=week')).status, 422)
    assert.equal((await request(free, '/leaderboard?groupId=' + randomUUID())).status, 403)
    pass('Anonymous, retired weekly scope and unauthorized group access are rejected')
    const story = await request(free, '/leaderboard/stories'), premiumStory = await request(premium, '/leaderboard/stories')
    assert.equal(story.status, 200); assert.equal(premiumStory.status, 200)
    assert.match(story.cache, /no-store/)
    assert.equal(story.json.data.userId, free.id); assert.equal(premiumStory.json.data.userId, premium.id)
    assert.equal(story.json.data.paths.length, 4); assert.equal(story.json.data.spotlights.length, 4)
    assert.deepEqual(story.json.data.paths, premiumStory.json.data.paths)
    assert.ok(story.json.data.paths.every(p => p.completed === 0 && !p.complete && p.href.startsWith('/explore?honorsPath=')))
    assert.ok(story.json.data.timeline.some(t => t.id === 'xp:100' && t.occurredAt === null && t.href === '/profile'))
    assert.ok(story.json.data.spotlights.every(s => s.leaders.length <= 3))
    assert.equal((await request(null, '/leaderboard/stories')).status, 401)
    assert.equal((await request(free, '/leaderboard/stories?groupId=' + randomUUID())).status, 403)
    assert.equal((await request(free, '/leaderboard/stories?scope=week')).status, 422)
    // Public response fixture for a UI parser integration check; never saves credentials.
    fs.writeFileSync(path.join(__dirname, '../target/honors-stories-contract.json'), JSON.stringify(story.json.data, null, 2))
    pass('Stories contract: owner, four paths/categories, unknown XP date, internal links, Free/Premium and access boundaries')
    // Positive evidence is created only for this run's disposable Premium QA profile.
    const cipherKeys = ['cipher:hoang-cam', 'cipher:two-wells', 'cipher:sandals']
    sql(`BEGIN;
      ${cipherKeys.map((key, i) => `INSERT INTO user_discoveries(user_id,location_id,discovery_key,discovered_at) VALUES('${premium.id}','11111111-1111-1111-1111-111111111111','${key}','2025-02-0${i + 1}T00:00:00Z');
        INSERT INTO xp_ledger(user_id,event_key,points) VALUES('${premium.id}','qa-stories:${key}',${[50, 75, 100][i]});`).join('\n')}
      UPDATE profiles SET total_points=402,level=3 WHERE id='${premium.id}';
      INSERT INTO user_badges(id,user_id,badge_id,earned_at) VALUES('${randomUUID()}','${premium.id}','b0000001-0000-4000-8000-000000000201','2025-02-03T00:00:00Z'); COMMIT;`)
    const positive = await request(premium, '/leaderboard/stories')
    assert.equal(positive.status, 200)
    assert.ok(positive.json.data.paths.find(p => p.id === 'ciphers').complete)
    const completion = positive.json.data.timeline.find(t => t.id === 'path:ciphers')
    assert.equal(Date.parse(completion.occurredAt), Date.parse('2025-02-03T00:00:00Z'))
    assert.ok(positive.json.data.timeline.some(t => t.icon === 'military_tech' && t.href === '/profile'))
    assert.equal(positive.json.data.spotlights.find(s => s.id === 'ciphers').viewerCount, 3)
    pass('Positive PostgreSQL evidence produces a dated completed path, earned badge and correct spotlight metric')
    assert.equal(a.displayLimit, 10); assert.ok(a.entries.length <= 10)
    const passport = await request(premium, '/leaderboard/passport?userId=' + free.id)
    assert.equal(passport.status, 200); assert.match(passport.cache, /no-store/)
    assert.equal(passport.json.data.userId, premium.id); assert.equal(passport.json.data.totalPoints, 402)
    assert.equal(passport.json.data.badgeCount, 1)
    assert.ok(!('email' in passport.json.data) && !('avatarUrl' in passport.json.data))
    const community = await request(free, '/leaderboard/community')
    assert.equal(community.status, 200); assert.match(community.cache, /no-store/)
    assert.ok(community.json.data.completedChallenges >= 3)
    for (const endpoint of ['/leaderboard/passport', '/leaderboard/community']) assert.equal((await request(null, endpoint)).status, 401)
    assert.equal((await request(free, '/leaderboard/community?groupId=' + randomUUID())).status, 403)
    fs.writeFileSync(path.join(__dirname, '../target/honors-extras-contract.json'), JSON.stringify({ passport: passport.json.data, community: community.json.data }, null, 2))
    pass('Top ten, private owner passport, official community counters and access boundaries work against PostgreSQL')
    const original = sql(`SELECT total_points FROM profiles WHERE id='${free.id}'; SELECT count(*) FROM user_badges WHERE user_id='${free.id}'; SELECT count(*) FROM xp_ledger WHERE user_id='${free.id}';`)
    for (let n = 0; n < 3; n++) {
      const summary = await request(free, '/me/explore/summary')
      assert.equal(summary.status, 200, 'Read-only summary must succeed')
      assert.equal(summary.json.data.profile.totalPoints, 177)
      assert.equal(summary.json.data.badges[0].earned, false, 'Viewing must not award an eligible badge')
      assert.match(summary.cache, /no-store/)
      await request(free, '/leaderboard')
      assert.equal((await request(free, '/leaderboard/stories')).status, 200)
      assert.equal((await request(free, '/leaderboard/passport')).status, 200)
      assert.equal((await request(free, '/leaderboard/community')).status, 200)
    }
    const after = sql(`SELECT total_points FROM profiles WHERE id='${free.id}'; SELECT count(*) FROM user_badges WHERE user_id='${free.id}'; SELECT count(*) FROM xp_ledger WHERE user_id='${free.id}';`)
    assert.equal(after, original)
    pass('Repeated honors/summary views preserve XP, ledger and earned badges')
  } finally {
    if (accounts.length) {
      const ids = accounts.map(a => `'${a.id}'`).join(',')
      sql(`BEGIN;
        CREATE TEMP TABLE honors_cleanup AS SELECT id FROM profiles WHERE id IN (${ids}) AND email LIKE 'honors-qa-%@example.invalid';
        DO $$ BEGIN ASSERT (SELECT count(*) FROM honors_cleanup)=${accounts.length}, 'QA identity mismatch'; END $$;
        DELETE FROM user_badges WHERE user_id IN (SELECT id FROM honors_cleanup);
        DELETE FROM user_discoveries WHERE user_id IN (SELECT id FROM honors_cleanup);
        DELETE FROM xp_ledger WHERE user_id IN (SELECT id FROM honors_cleanup);
        DELETE FROM profiles WHERE id IN (SELECT id FROM honors_cleanup); COMMIT;`)
    }
    cleaned = true
    assert.equal(fingerprint(), before, 'Original profile XP/levels and progress row counts must be preserved')
    pass('QA fixtures removed and original profiles/XP/discoveries/badges/ledger/attempts preserved')
    fs.mkdirSync(path.join(__dirname, '../target'), { recursive: true })
    fs.writeFileSync(path.join(__dirname, '../target/honors-live-results.json'), JSON.stringify({ checks, fixturesCleaned: cleaned }, null, 2))
  }
})().catch(error => { console.error('FAILED ' + error.message); process.exitCode = 1 })
