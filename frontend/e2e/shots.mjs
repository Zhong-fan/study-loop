import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';

const BASE = process.env.E2E_BASE || 'http://127.0.0.1:4173';
const OUT = path.resolve('..', 'runtime', '_payload', 'redesign');
fs.mkdirSync(OUT, { recursive: true });

const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
const page = await context.newPage();

const shot = async (name, url) => {
    await page.goto(`${BASE}${url}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1200);
    await page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage: true });
    console.log('shot', name);
};

await shot('login', '/#/login');

await page.locator('#username').fill('admin');
await page.locator('#password').fill('123456');
await page.getByRole('button', { name: '登录', exact: true }).click();
await page.waitForURL('**/#/dashboard');
await page.waitForTimeout(1200);
await page.screenshot({ path: path.join(OUT, 'dashboard.png'), fullPage: true });
console.log('shot dashboard');

await shot('practice', '/#/practice');
await shot('tools', '/#/tools');
await shot('stats', '/#/stats');
await shot('courses', '/#/courses');
await shot('questions', '/#/questions');
await shot('ai', '/#/ai');
await shot('quiz', '/#/quiz');
await shot('exams', '/#/exams');
await shot('admin', '/#/admin');

const mobile = await browser.newContext({ viewport: { width: 390, height: 844 } });
const mp = await mobile.newPage();
await mp.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
await mp.waitForTimeout(800);
await mp.screenshot({ path: path.join(OUT, 'login-mobile.png'), fullPage: true });
await mp.locator('#username').fill('admin');
await mp.locator('#password').fill('123456');
await mp.getByRole('button', { name: '登录', exact: true }).click();
await mp.waitForURL('**/#/dashboard');
await mp.waitForTimeout(1200);
console.log('mobile scrollY after login:', await mp.evaluate(() => window.scrollY));
await mp.evaluate(() => window.scrollTo(0, 0));
await mp.waitForTimeout(300);
await mp.screenshot({ path: path.join(OUT, 'dashboard-mobile.png') });
console.log('shot mobile');

await browser.close();
console.log('out:', OUT);
