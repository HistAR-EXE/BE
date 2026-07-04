import { chromium } from 'playwright';

const OUT = 'd:/FPT/SU26/EXE101/HistAR/BE/docs/scripts/_cu_chi_work';
const URL = 'http://localhost:5173/tour/360/11111111-1111-1111-1111-111111111111';

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
const errors = [];
page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
page.on('pageerror', (e) => errors.push('PAGEERROR: ' + e.message));

await page.goto(URL, { waitUntil: 'networkidle', timeout: 60000 });
await page.waitForTimeout(3500);
await page.screenshot({ path: `${OUT}/v2-1-map.png` });

const pinCount = await page.locator('.tour360-illustrated-pin').count().catch(() => -1);
console.log('illustrated pins:', pinCount);

// hover pin 3 (Bo Tu Lenh)
const pins = page.locator('.tour360-illustrated-pin');
if (pinCount >= 2) {
  await pins.nth(1).click().catch(() => {});
  await page.waitForTimeout(5000);
  await page.screenshot({ path: `${OUT}/v3-den.png` });
}

console.log('CONSOLE_ERRORS:', JSON.stringify(errors.slice(0, 10), null, 2));
await browser.close();
