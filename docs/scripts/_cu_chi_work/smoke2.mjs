import { chromium } from 'playwright';

const OUT = 'd:/FPT/SU26/EXE101/HistAR/BE/docs/scripts/_cu_chi_work';
const URL = 'http://localhost:5173/tour/360/11111111-1111-1111-1111-111111111111';

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 820 } });
const errors = [];
page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
page.on('pageerror', (e) => errors.push('PAGEERROR: ' + e.message));

await page.goto(URL, { waitUntil: 'networkidle', timeout: 60000 });
await page.waitForTimeout(3500);

const pins = page.locator('.tour360-illustrated-pin');
const n = await pins.count().catch(() => -1);
console.log('pins:', n);

async function drag(dx, dy) {
  const box = await page.locator('canvas').first().boundingBox();
  if (!box) return;
  const cx = box.x + box.width / 2, cy = box.y + box.height / 2;
  await page.mouse.move(cx, cy);
  await page.mouse.down();
  await page.mouse.move(cx + dx, cy + dy, { steps: 12 });
  await page.mouse.up();
  await page.waitForTimeout(900);
}

// scene index -> label
const want = [{ i: 1, name: 'den' }, { i: 2, name: 'botulenh' }];
for (const w of want) {
  await pins.nth(w.i).click().catch(() => {});
  await page.waitForTimeout(4500);
  await page.screenshot({ path: `${OUT}/q-${w.name}-mid.png` });
  await drag(0, 300);   // look up (reveal sky fill)
  await page.screenshot({ path: `${OUT}/q-${w.name}-up.png` });
  await drag(0, -600);  // look down (reveal ground fill)
  await page.screenshot({ path: `${OUT}/q-${w.name}-down.png` });
  // close viewer if there is a back/close control by pressing Escape
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(1500);
}

console.log('ERR:', JSON.stringify(errors.slice(0, 8)));
await browser.close();
