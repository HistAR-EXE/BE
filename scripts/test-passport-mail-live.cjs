// LOCAL ONLY. Backend :8081 must use SMTP 127.0.0.1:2526, never an external provider.
// Creates disposable QA profiles and captures SMTP on loopback; never sends external email.
const net = require('node:net')
const { execFileSync } = require('node:child_process')
const { randomUUID, randomBytes } = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const base = 'http://127.0.0.1:8081/api'
const accounts = [], captured = [], checks = []
function sql(input) {
  return execFileSync('docker', ['exec', '-i', 'histar-postgres', 'psql', '-U', 'timelens', '-d', 'timelens', '-At', '-v', 'ON_ERROR_STOP=1'], { input, encoding: 'utf8', timeout: 30000 }).trim()
}
function fingerprint() {
  return sql(`SELECT md5(COALESCE(string_agg(id::text || ':' || COALESCE(total_points,0)::text || ':' || COALESCE(level,1)::text, ',' ORDER BY id),'')) FROM profiles;
    SELECT count(*) FROM user_badges; SELECT count(*) FROM user_discoveries; SELECT count(*) FROM xp_ledger; SELECT count(*) FROM explore_attempts; SELECT count(*) FROM passport_mail_attempts;`)
}
function pass(name) { checks.push(name); console.log('PASS ' + name) }
const sockets = new Set()
const smtp = net.createServer(socket => {
  sockets.add(socket); socket.on('close', () => sockets.delete(socket)); socket.on('error', () => {})
  socket.setTimeout(30000, () => socket.destroy())
  let input = '', collecting = false, message = [], envelope = []
  socket.write('220 localhost TimeLens QA capture\r\n')
  socket.on('data', bytes => {
    input += bytes.toString('utf8')
    let end
    while ((end = input.indexOf('\r\n')) >= 0) {
      const line = input.slice(0, end); input = input.slice(end + 2)
      if (collecting) {
        if (line === '.') { captured.push({ message: message.join('\r\n'), envelope }); collecting = false; message = []; envelope = []; socket.write('250 captured locally\r\n') }
        else message.push(line.startsWith('..') ? line.slice(1) : line)
      } else if (/^(EHLO|HELO)/i.test(line)) socket.write('250 localhost\r\n')
      else if (/^(MAIL FROM|RCPT TO)/i.test(line)) { envelope.push(line); socket.write('250 OK\r\n') }
      else if (/^DATA$/i.test(line)) { collecting = true; socket.write('354 End with dot\r\n') }
      else if (/^QUIT$/i.test(line)) socket.end('221 Bye\r\n')
      else socket.write('250 OK\r\n')
    }
  })
})
async function call(token, route, body, json = false) {
  const response = await fetch(base + route, { method: body ? 'POST' : 'GET',
    headers: { ...(token ? { Authorization: 'Bearer ' + token } : {}), ...(json ? { 'Content-Type': 'application/json' } : {}) },
    body: json ? JSON.stringify(body) : body, signal: AbortSignal.timeout(75000) })
  return { status: response.status, cache: response.headers.get('cache-control'), json: await response.json() }
}
function upload(image, id, extra) {
  const form = new FormData(); form.append('image', new Blob([image], { type: 'image/png' }), 'passport.png'); form.append('requestId', id)
  if (extra) form.append('to', 'someone-else@example.invalid')
  return form
}
;(async () => {
  let ready = false
  for (let attempt = 0; attempt < 30; attempt++) {
    try { const response = await fetch(base + '/leaderboard/passport/email/status', { signal: AbortSignal.timeout(1000) }); if (response.status === 401) { ready = true; break } } catch { /* Wait for the local QA backend only. */ }
    await new Promise(resolve => setTimeout(resolve, 1000))
  }
  assert.ok(ready, 'Local QA backend8081 must be running with loopback SMTP2526')
  await new Promise((resolve, reject) => { smtp.once('error', reject); smtp.listen(2526, '127.0.0.1', resolve) })
  const before = fingerprint()
  try {
    const image = fs.readFileSync(path.join(__dirname, '../../FE/test-results/honors-passport-export.png'))
    assert.equal(image.readUInt32BE(16), 1200); assert.equal(image.readUInt32BE(20), 1500)
    const id = randomUUID(), password = randomBytes(24).toString('hex'), email = `passport-qa-${id}@example.invalid`
    sql(`INSERT INTO profiles(id,email,password_hash,display_name,email_verified,email_verified_at,role,tier,total_points,level)
      VALUES('${id}','${email}',crypt('${password}',gen_salt('bf')),'Passport QA',true,now(),'USER','FREE',177,2);`)
    accounts.push(id)
    const login = await call(null, '/auth/login', { email, password }, true)
    assert.equal(login.status, 200); const token = login.json.data.token || login.json.data.accessToken; assert.ok(token)
    const initial = await call(token, '/leaderboard/passport/email/status')
    assert.equal(initial.status, 200); assert.equal(initial.json.data.email, email); assert.equal(initial.json.data.remainingToday, 3)
    assert.equal(initial.json.data.available, true); assert.match(initial.cache, /no-store/)
    assert.equal((await call(null, '/leaderboard/passport/email/status')).status, 401)
    const anonymous = await call(null, '/leaderboard/passport/email', upload(image, randomUUID()))
    assert.equal(anonymous.status, 401); assert.match(anonymous.cache, /no-store/)
    pass('Authenticated own-email status and anonymous boundaries on actual security chain')
    const requestId = randomUUID()
    const sent = await call(token, '/leaderboard/passport/email', upload(image, requestId))
    assert.equal(sent.status, 200); assert.equal(sent.json.data.status, 'ACCEPTED'); assert.equal(sent.json.data.remainingToday, 2)
    assert.match(sent.cache, /no-store/); assert.equal(captured.length, 1)
    assert.ok(captured[0].envelope.some(line => line.toLowerCase().includes('histar.timelens@gmail.com')))
    assert.ok(captured[0].envelope.some(line => line.includes(email)))
    const encoded = captured[0].message.match(/Content-Type: image\/png[^]*?\r\n\r\n([A-Za-z0-9+/=\r\n]+)\r\n--/i)
    assert.ok(encoded, 'Captured MIME must contain a base64 PNG attachment')
    const attached = Buffer.from(encoded[1].replace(/\s/g, ''), 'base64')
    assert.equal(attached.readUInt32BE(16), 1200); assert.equal(attached.readUInt32BE(20), 1500)
    fs.writeFileSync(path.join(__dirname, '../target/passport-mail-captured.png'), attached)
    const comparison = execFileSync('java', [path.join(__dirname, 'ComparePassportPixels.java'),
      path.join(__dirname, '../../FE/test-results/honors-passport-export.png'), path.join(__dirname, '../target/passport-mail-captured.png')], { encoding: 'utf8', timeout: 30000 })
    assert.match(comparison, /PASS complete passport/)
    pass('Full passport PNG travels through real API, DB reservation and loopback SMTP with correct sender/recipient')
    assert.equal((await call(token, '/leaderboard/passport/email', upload(image, requestId))).status, 200)
    assert.equal(captured.length, 1)
    const limited = await call(token, '/leaderboard/passport/email', upload(image, randomUUID()))
    assert.equal(limited.status, 429); assert.equal(limited.json.code, 'MAIL_RATE_LIMITED')
    assert.equal((await call(token, '/leaderboard/passport/email', upload(image, randomUUID(), true))).status, 422)
    const malformed = Buffer.from(image); malformed.writeUInt32BE(1, 16)
    assert.equal((await call(token, '/leaderboard/passport/email', upload(malformed, randomUUID()))).status, 422)
    pass('Accepted replay sends no duplicate; cooldown, arbitrary recipient and malformed PNG are rejected')
    sql(`UPDATE profiles SET email_verified=false WHERE id='${id}';`)
    assert.equal((await call(token, '/leaderboard/passport/email', upload(image, randomUUID()))).status, 403)
    sql(`UPDATE profiles SET email_verified=true WHERE id='${id}';`)
    for (let i = 0; i < 2; i++) {
      sql(`UPDATE passport_mail_attempts SET created_at=now()-interval '2 minutes' WHERE user_id='${id}';`)
      assert.equal((await call(token, '/leaderboard/passport/email', upload(image, randomUUID()))).status, 200)
    }
    assert.equal(captured.length, 3)
    sql(`UPDATE passport_mail_attempts SET created_at=now()-interval '2 minutes' WHERE user_id='${id}';`)
    assert.equal((await call(token, '/leaderboard/passport/email', upload(image, randomUUID()))).status, 429)
    assert.equal((await call(token, '/leaderboard/passport/email/status')).json.data.remainingToday, 0)
    assert.equal(sql(`SELECT total_points FROM profiles WHERE id='${id}'`), '177')
    pass('Verified-account guard, durable rolling24h cap and unchanged XP')
  } finally {
    for (const id of accounts) sql(`DELETE FROM profiles WHERE id='${id}' AND email='passport-qa-${id}@example.invalid';`)
    assert.equal(fingerprint(), before, 'Original progress and mail attempts must be preserved after QA cleanup')
    pass('Disposable QA cleaned; all original XP/levels/badges/discoveries/ledger/attempts preserved')
    fs.writeFileSync(path.join(__dirname, '../target/passport-mail-live-results.json'), JSON.stringify({ checks, externalEmailSent: false, captures: captured.length }, null, 2))
  }
})().catch(error => { console.error('FAILED ' + error.message); process.exitCode = 1 })
  .finally(() => { for (const socket of sockets) socket.destroy(); smtp.close() })
