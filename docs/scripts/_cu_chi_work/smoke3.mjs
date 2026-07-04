import { chromium } from 'playwright';
const OUT = 'd:/FPT/SU26/EXE101/HistAR/BE/docs/scripts/_cu_chi_work';
const URL = 'http://localhost:5173/tour/360/11111111-1111-1111-1111-111111111111';
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 760 } });
await page.goto(URL, { waitUntil: 'networkidle', timeout: 60000 });
await page.waitForTimeout(3500);
async function drag(dx, dy) {
  const box = await page.locator('canvas').first().boundingBox();
  if (!box) return;
  const cx = box.x + box.width / 2, cy = box.y + box.height / 2;
  await page.mouse.move(cx, cy); await page.mouse.down();
  await page.mouse.move(cx + dx, cy + dy, { steps: 18 }); await page.mouse.up();
  await page.waitForTimeout(800);
}
const pins = page.locator('.tour360-illustrated-pin');
const n = await pins.count().catch(() => -1);
console.log('pins', n);
await pins.nth(1).click().catch(() => {});      // den (was duplicated)
await page.waitForTimeout(4500);
await page.screenshot({ path: `${OUT}/r-front.png` });
await drag(-900, 0); await drag(-900, 0);        // turn ~180 to back
await page.screenshot({ path: `${OUT}/r-back.png` });
await browser.close();
console.log('done');
