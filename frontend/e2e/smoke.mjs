import { chromium } from 'playwright';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const BASE = process.env.E2E_BASE || 'http://127.0.0.1:5173';
const OUT = path.resolve('..', 'runtime', '_payload', 'e2e');
fs.mkdirSync(OUT, { recursive: true });

const results = [];
let browser;
let page;

const slug = (name) => name.replace(/[^\w\u4e00-\u9fa5]+/g, '_').slice(0, 40);

async function step(name, fn) {
    try {
        await fn();
        results.push({ name, ok: true });
        console.log(`PASS  ${name}`);
    } catch (error) {
        results.push({ name, ok: false, error: error.message });
        console.log(`FAIL  ${name}\n      ${error.message}`);
        try {
            await page.screenshot({ path: path.join(OUT, `${slug(name)}.png`), fullPage: true });
        } catch {
            /* ignore */
        }
        throw error;
    }
}

async function expectVisible(locator, timeout = 10000) {
    await locator.waitFor({ state: 'visible', timeout });
    return locator;
}

async function loginAsAdmin(target) {
    await target.goto(`${BASE}/#/login`, { waitUntil: 'domcontentloaded' });
    await expectVisible(target.locator('.login-page'));
    await target.getByRole('button', { name: '填入' }).click();
    assert.equal(await target.locator('#username').inputValue(), 'admin');
    await target.locator('form').getByRole('button', { name: '登录' }).click();
    await target.waitForURL('**/#/dashboard', { timeout: 15000 });
    await expectVisible(target.locator('.app-ui h1', { hasText: '概览' }));
}

async function main() {
    try {
        browser = await chromium.launch({ channel: 'msedge', headless: true });
    } catch {
        browser = await chromium.launch({
            executablePath: 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
            headless: true
        });
    }

    const guest = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    page = await guest.newPage();
    const pageErrors = [];
    page.on('pageerror', (error) => pageErrors.push(error.message));

    await step('登录页渲染', async () => {
        await page.goto(`${BASE}/#/login`, { waitUntil: 'domcontentloaded' });
        await expectVisible(page.locator('.login-page'));
        await expectVisible(page.locator('#username'));
        await expectVisible(page.locator('#password'));
    });

    await step('登录页必填校验', async () => {
        await page.locator('form').getByRole('button', { name: '登录' }).click();
        await expectVisible(page.getByText('请输入用户名和密码'));
    });

    await step('切换到注册页', async () => {
        await page.getByRole('button', { name: '注册', exact: true }).click();
        await page.waitForURL('**/#/register');
        await expectVisible(page.locator('#nickname'));
        await expectVisible(page.locator('#confirm'));
    });

    await step('注册页校验（空提交 + 密码不一致）', async () => {
        await page.locator('form').getByRole('button', { name: '注册' }).click();
        await expectVisible(page.getByText('请输入用户名和密码'));

        await page.locator('#username').fill('e2e_user');
        await page.locator('#password').fill('123456');
        await page.locator('#confirm').fill('654321');
        await page.locator('form').getByRole('button', { name: '注册' }).click();
        await expectVisible(page.getByText('两次输入的密码不一致'));
    });

    await step('注册页返回登录', async () => {
        await page.getByRole('button', { name: '登录', exact: true }).first().click();
        await page.waitForURL('**/#/login');
        await expectVisible(page.locator('.login-page'));
    });

    await guest.close();

    const admin = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    page = await admin.newPage();
    page.on('pageerror', (error) => pageErrors.push(error.message));

    await step('管理员登录', async () => {
        await loginAsAdmin(page);
        await expectVisible(page.getByText('还有'));
    });

    await step('侧栏导航', async () => {
        const items = await page.locator('.app-ui aside nav a').allInnerTexts();
        assert.deepEqual(items.map((text) => text.trim()), ['概览', '做题', '题库', '资料', '统计']);
    });

    await step('概览数据面板', async () => {
        await expectVisible(page.getByText('题库题量'));
        await expectVisible(page.getByText('最近 14 天'));
        await expectVisible(page.getByText('开始复习'));
    });

    await step('导航到做题页', async () => {
        await page.locator('aside nav a', { hasText: '做题' }).click();
        await page.waitForURL('**/#/practice');
        await expectVisible(page.locator('.practice'));
    });

    await step('返回概览', async () => {
        await page.locator('aside nav a', { hasText: '概览' }).click();
        await page.waitForURL('**/#/dashboard');
        await expectVisible(page.locator('.app-ui h1', { hasText: '概览' }));
    });

    await step('打开题库', async () => {
        await page.locator('aside nav a', { hasText: '题库' }).click();
        await page.waitForURL('**/#/questions');
        await expectVisible(page.getByRole('heading', { name: '题库' }));
        assert.ok((await page.locator('tbody tr').count()) >= 1);
        await expectVisible(page.getByRole('button', { name: '导入' }));
    });

    await step('打开资料与工具', async () => {
        await page.locator('header a', { hasText: '资料与工具' }).click();
        await page.waitForURL('**/#/tools');
        await expectVisible(page.getByRole('heading', { name: '工具与资料' }));
        const cards = await page.locator('.row').count();
        assert.ok(cards >= 8, `工具卡片数量不足：${cards}`);
        await expectVisible(page.getByText('用户管理', { exact: true }));
    });

    await step('工具页进入统计', async () => {
        await page.locator('.row').filter({ has: page.getByText('统计', { exact: true }) }).first().click();
        await page.waitForURL('**/#/stats');
        await expectVisible(page.getByText('知识点覆盖率'));
    });

    await step('工具页进入用户管理', async () => {
        await page.goto(`${BASE}/#/tools`, { waitUntil: 'domcontentloaded' });
        await page.locator('.row').filter({ has: page.getByText('用户管理', { exact: true }) }).first().click();
        await page.waitForURL('**/#/admin');
        await expectVisible(page.getByText('AI 配置'));
        await expectVisible(page.locator('.el-card__header', { hasText: '用户管理' }));
    });

    await step('浏览器无脚本异常', async () => {
        assert.deepEqual(pageErrors, []);
    });

    await page.screenshot({ path: path.join(OUT, 'final-admin.png'), fullPage: true });
    await admin.close();
    await browser.close();

    const passed = results.filter((item) => item.ok).length;
    console.log(`\n${passed}/${results.length} passed`);
    console.log(`screenshots: ${OUT}`);
    process.exit(passed === results.length ? 0 : 1);
}

main().catch(async (error) => {
    console.error('\nE2E ABORTED:', error.message);
    try {
        await browser?.close();
    } catch {
        /* ignore */
    }
    const passed = results.filter((item) => item.ok).length;
    console.log(`${passed}/${results.length} passed before abort`);
    process.exit(1);
});
