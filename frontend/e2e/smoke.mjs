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
    await expectVisible(target.locator('.auth'));
    await target.getByRole('button', { name: '填入' }).click();
    assert.equal(await target.locator('#username').inputValue(), 'admin');
    await target.getByRole('button', { name: '登录', exact: true }).click();
    await target.waitForURL('**/#/dashboard', { timeout: 15000 });
    await expectVisible(target.locator('.lede'));
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

    // ---------- 未登录：登录页 / 注册页 ----------
    const guest = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    page = await guest.newPage();
    const pageErrors = [];
    page.on('pageerror', (error) => pageErrors.push(error.message));

    await step('登录页渲染', async () => {
        await page.goto(`${BASE}/#/login`, { waitUntil: 'domcontentloaded' });
        await expectVisible(page.locator('.auth'));
        await expectVisible(page.getByRole('link', { name: '登录', exact: true }));
        await expectVisible(page.getByRole('link', { name: '注册', exact: true }));
        await expectVisible(page.locator('#username'));
        await expectVisible(page.locator('#password'));
    });

    await step('登录页必填校验', async () => {
        await page.getByRole('button', { name: '登录', exact: true }).click();
        await expectVisible(page.locator('.el-form-item__error').first());
    });

    await step('切换到注册页', async () => {
        await page.getByRole('link', { name: '注册', exact: true }).click();
        await page.waitForURL('**/#/register');
        await expectVisible(page.getByRole('heading', { name: '创建账号' }));
        await expectVisible(page.locator('#nickname'));
        await expectVisible(page.locator('#confirm'));
    });

    await step('注册页校验（空提交 + 密码不一致）', async () => {
        await page.getByRole('button', { name: '注册并登录' }).click();
        await expectVisible(page.locator('.el-form-item__error').first());

        await page.locator('#username').fill('e2e_user');
        await page.locator('#password').fill('123456');
        await page.locator('#confirm').fill('654321');
        await page.getByRole('button', { name: '注册并登录' }).click();
        await expectVisible(page.getByText('两次输入的密码不一致'));
        // 不真正创建账号，避免污染数据
    });

    await step('注册页返回登录', async () => {
        await page.getByRole('link', { name: '返回登录' }).click();
        await page.waitForURL('**/#/login');
        await expectVisible(page.locator('.auth'));
    });

    await guest.close();

    // ---------- 管理员：主流程 ----------
    const admin = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    page = await admin.newPage();
    page.on('pageerror', (error) => pageErrors.push(error.message));

    await step('管理员登录', async () => {
        await loginAsAdmin(page);
        await expectVisible(page.locator('.headline'));
        assert.match(await page.locator('.headline').innerText(), /今天/);
    });

    await step('顶部导航只有概览和做题', async () => {
        const items = await page.locator('.mast-nav a').allInnerTexts();
        assert.deepEqual(items.map((text) => text.trim()), ['概览', '做题']);
    });

    await step('概览数据面板', async () => {
        await expectVisible(page.locator('.stat-label').first(), 15000);
        const labels = await page.locator('.stat-label').allInnerTexts();
        assert.deepEqual(labels.map((text) => text.trim()), [
            '已掌握',
            '今天已复习',
            '题库题量',
            '连续复习'
        ]);
        await expectVisible(page.getByText('最近 14 天'));
        await expectVisible(page.getByText('连续复习'));
    });

    await step('导航到做题页', async () => {
        await page.locator('.mast-nav a', { hasText: '做题' }).click();
        await page.waitForURL('**/#/practice');
        await expectVisible(page.locator('.practice'));
        await expectVisible(page.locator('.mast-nav a.active', { hasText: '做题' }));
    });

    await step('返回概览', async () => {
        await page.locator('.mast-nav a', { hasText: '概览' }).click();
        await page.waitForURL('**/#/dashboard');
        await expectVisible(page.locator('.lede'));
    });

    await step('打开资料与工具', async () => {
        await page.locator('.tool-link[title="资料与工具"]').click();
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
