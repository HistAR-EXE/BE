// LOCAL ONLY. Explicit opt-in fixtures for viewing the real honors UI with many accounts.
// Usage: node scripts/preview-honors-data.cjs create|status|remove
// No usable password, OAuth identity or session is created for these sample profiles.
const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const mode = process.argv[2] || 'status'
assert.ok(['create', 'status', 'remove'].includes(mode), 'Use create, status or remove')
const workspace = path.resolve(__dirname, '../..')
const manifestPath = path.join(workspace, '.secrets/honors-ui-preview.json')
const provider = 'ui-preview-honors'
const prefix = 'honors-ui-preview-'
const q = text => "'" + String(text).replaceAll("'", "''") + "'"
function run(args) { return execFileSync('docker', args, { encoding: 'utf8', timeout: 30000 }).trim() }
const context = JSON.parse(run(['context', 'inspect']))[0]
assert.match(context.Endpoints.docker.Host, /^(npipe:|unix:)/, 'Refusing non-local Docker endpoint')
function sql(input) {
  return execFileSync('docker', ['exec', '-i', 'histar-postgres', 'psql', '-U', 'timelens', '-d', 'timelens', '-At', '-v', 'ON_ERROR_STOP=1'], { input, encoding: 'utf8', timeout: 30000 }).trim()
}
assert.equal(sql('SELECT current_database()'), 'timelens')
function protect() {
  const data = [
    "SELECT id,total_points,level FROM profiles WHERE COALESCE(provider,'') <> 'ui-preview-honors'",
    ...['user_discoveries', 'user_badges', 'xp_ledger', 'explore_attempts'].map(table => `SELECT t.* FROM ${table} t WHERE NOT EXISTS (SELECT 1 FROM profiles p WHERE p.id=t.user_id AND p.provider='ui-preview-honors')`),
  ]
  return data.map(query => sql(`SELECT md5(COALESCE(string_agg(to_jsonb(x)::text,'' ORDER BY to_jsonb(x)::text),'')) FROM (${query}) x;`)).join(':')
}
const existing = Number(sql(`SELECT count(*) FROM profiles WHERE provider=${q(provider)} AND email LIKE '${prefix}%@example.invalid'`))
if (mode === 'status') { console.log(`Local honors preview profiles: ${existing}`); process.exit(0) }
fs.mkdirSync(path.dirname(manifestPath), { recursive: true })
const before = protect()
if (mode === 'remove') {
  if (!fs.existsSync(manifestPath)) { assert.equal(existing, 0, 'Missing manifest for existing fixtures; refusing unverified cleanup'); console.log('No preview fixtures to remove'); process.exit(0) }
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'))
  assert.equal(manifest.provider, provider)
  assert.ok(manifest.accounts.every(a => /^[0-9a-f-]{36}$/i.test(a.id)))
  const ids = manifest.accounts.map(a => q(a.id)).join(',')
  sql(`BEGIN; SET LOCAL lock_timeout='5s';
    DO $$ BEGIN ASSERT (SELECT count(*) FROM profiles WHERE id IN (${ids}) AND (provider<>${q(provider)} OR email NOT LIKE '${prefix}%@example.invalid'))=0, 'Preview identities changed; stop'; END $$;
    DELETE FROM user_badges WHERE user_id IN (SELECT id FROM profiles WHERE id IN (${ids}) AND provider=${q(provider)});
    DELETE FROM profiles WHERE id IN (${ids}) AND provider=${q(provider)} AND email LIKE '${prefix}%@example.invalid'; COMMIT;`)
  assert.equal(protect(), before, 'Original progress changed')
  manifest.removedAt = new Date().toISOString(); fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2))
  console.log('PASS removed only preview profiles; original XP/levels/discoveries/badges/ledger/attempts unchanged')
  process.exit(0)
}
if (existing > 0) { assert.ok(fs.existsSync(manifestPath), 'Existing fixtures have no manifest'); console.log(`Preview already contains ${existing} profiles; no duplicates created`); process.exit(0) }
const stamp = new Date().toISOString().replace(/[^0-9]/g, '').slice(0, 14)
const backupDir = path.join(workspace, '.secrets/db-backups'); fs.mkdirSync(backupDir, { recursive: true })
const dumpName = `histar-before-honors-ui-${stamp}.dump`
run(['exec', 'histar-postgres', 'pg_dump', '-U', 'timelens', '-d', 'timelens', '-Fc', '-f', '/tmp/' + dumpName])
run(['cp', 'histar-postgres:/tmp/' + dumpName, path.join(backupDir, dumpName)])
const names = ['Nguyễn Minh Anh', 'Trần Quốc Bảo', 'Lê Ngọc Linh', 'Phạm Bảo Trâm', 'Võ Hoàng Nam', 'Đặng Gia Hân',
  'Hồ Đức Minh', 'Nguyễn Hoàng Phương Anh', 'Bùi Khánh An', 'Đỗ Hải Đăng', 'Trương Thảo Vy', 'Lý Nhật Hạ',
  'Mai Thanh Tâm', 'Đinh Tuấn Kiệt', 'Lâm Minh Châu', 'Ngô Bảo Ngọc', 'Vũ Quang Huy', 'Phan Yến Nhi']
const points = [3240, 2980, 2750, 2460, 2280, 2140, 1970, 1450, 1120, 980, 810, 735, 620, 450, 300, 160, 80, 30]
const counts = [9, 8, 7, 6, 5, 4, 4, 3, 3, 2, 2, 1, 1, 1, 0, 0, 0, 0]
const accounts = names.map((name, i) => ({ id: crypto.randomUUID(), name: name + ' · Mẫu UI', points: points[i], badges: counts[i] }))
const statements = []
accounts.forEach((account, i) => {
  statements.push(`INSERT INTO profiles(id,email,provider,role,tier,display_name,total_points,level,email_verified,created_at)
    VALUES(${q(account.id)},${q(prefix + account.id + '@example.invalid')},${q(provider)},'USER',${q(i % 2 ? 'PREMIUM' : 'FREE')},${q(account.name)},${account.points},${[0,100,300,600,1000].filter(threshold => account.points >= threshold).length},false,now());`)
  statements.push(`INSERT INTO xp_ledger(user_id,event_key,points) VALUES(${q(account.id)},'baseline:v1',${account.points});`)
  for (let badge = 0; badge < account.badges; badge++) {
    const badgeId = 'b0000001-0000-4000-8000-' + String(201 + badge).padStart(12, '0')
    statements.push(`INSERT INTO user_badges(id,user_id,badge_id,earned_at) SELECT gen_random_uuid(),${q(account.id)},id,now() FROM badges WHERE id=${q(badgeId)};`)
  }
})
// Persist the exact fixture identities before the DB write, so cleanup remains possible after interruption.
fs.writeFileSync(manifestPath, JSON.stringify({ provider, createdAt: new Date().toISOString(), backup: path.join(backupDir, dumpName), accounts }, null, 2))
sql('BEGIN; SET LOCAL lock_timeout=\'5s\';\n' + statements.join('\n') + '\nCOMMIT;')
assert.equal(protect(), before, 'Original progress changed')
assert.equal(Number(sql(`SELECT count(*) FROM profiles WHERE provider=${q(provider)}`)), 18)
console.log('PASS created 18 labeled, non-login local preview profiles with varied XP, ties and badge counts')
console.log('PASS original profiles XP/levels/discoveries/badges/ledger/attempts unchanged; full backup created')
console.log('Remove later: node scripts/preview-honors-data.cjs remove')
