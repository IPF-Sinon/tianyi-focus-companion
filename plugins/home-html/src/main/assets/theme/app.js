'use strict';

/* ══════════════════════════════════════════════════════════════
 * 默认主题脚本
 * 通过 window.TianyiHost（宿主注入）读取插件列表、导航项、统计数据
 * 并渲染全部页面（专注/统计/插件/设置）+ 底部导航。
 *
 * TianyiHost 返回 JSON 字符串，用 JSON.parse 解析。
 * ══════════════════════════════════════════════════════════════ */

const Host = window.TianyiHost;

/* ── 全局状态 ─────────────────────────────────────────── */
const state = {
  navItems: [],
  plugins: [],
  currentNav: null,
  themeHostReady: false,
};

function $(sel, root) { return (root || document).querySelector(sel); }
function $$(sel, root) { return Array.from((root || document).querySelectorAll(sel)); }

function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function toast(msg) {
  const el = document.getElementById('toast');
  if (!el) return;
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(el._t);
  el._t = setTimeout(() => el.classList.remove('show'), 1800);
}

/* ── 启动 ──────────────────────────────────────────────── */
async function init() {
  // 等待宿主注入
  if (!Host) {
    document.body.innerHTML = '<div class="empty">宿主接口未就绪</div>';
    return;
  }

  state.plugins = JSON.parse(Host.getPlugins() || '{"plugins":[]}').plugins || [];
  state.navItems = JSON.parse(Host.getNavItems() || '{"items":[]}').items || [];

  // 内置导航项（主题自带底栏）：即使插件没注册也能用
  const builtinNav = [
    { id: 'home', label: '专注', icon: '🍅', order: 0, pluginId: '' },
    { id: 'plugins', label: '插件', icon: '🧩', order: 40, pluginId: '' },
    { id: 'settings', label: '设置', icon: '⚙️', order: 50, pluginId: '' },
  ];
  const all = [...builtinNav, ...state.navItems].sort((a, b) => a.order - b.order);
  state.navItems = dedupe(all);

  renderNav();
  // 默认切到第一个导航项
  navigation(0);
  state.themeHostReady = true;
}

function dedupe(items) {
  const seen = new Set();
  return items.filter(i => {
    if (seen.has(i.id)) return false;
    seen.add(i.id);
    return true;
  });
}

/* ── 底部导航渲染 ─────────────────────────────────────── */
function renderNav() {
  const nav = document.getElementById('bottom-nav');
  if (!nav) return;

  nav.innerHTML = state.navItems.map((item, idx) => `
    <button class="nav-item" data-idx="${idx}" data-id="${esc(item.id)}">
      <span class="nav-icon">${esc(item.icon || '·')}</span>
      <span>${esc(item.label)}</span>
    </button>
  `).join('');

  $$('.nav-item', nav).forEach(btn => {
    btn.addEventListener('click', () => {
      navigation(parseInt(btn.dataset.idx, 10));
    });
  });
}

function setActiveNav(idx) {
  $$('.nav-item').forEach((btn, i) => {
    btn.classList.toggle('active', i === idx);
  });
}

/* ── 页面切换 ─────────────────────────────────────────── */
function navigation(idx) {
  const item = state.navItems[idx];
  if (!item) return;
  state.currentNav = item;
  setActiveNav(idx);
  renderPage(item);
}

function renderPage(item) {
  const container = document.getElementById('page-container');
  container.innerHTML = '';

  // 插件提供的导航页 → 请求插件数据
  if (item.pluginId) {
    renderPluginNav(item, container);
  } else if (item.id === 'home') {
    renderHome(container);
  } else if (item.id === 'plugins') {
    renderPlugins(container);
  } else if (item.id === 'settings') {
    renderSettings(container);
  } else {
    container.innerHTML = '<div class="empty">页面不存在</div>';
  }
}

/* ── 专注(首页) ───────────────────────────────────────── */
function renderHome(container) {
  const app = JSON.parse(Host.getAppInfo() || '{}');
  container.innerHTML = `
    <div class="card">
      <div class="row" style="border:none;padding-top:4px;">
        <div class="row-icon" style="background:linear-gradient(135deg,var(--primary-light),var(--primary));color:#fff;font-weight:700;">洛</div>
        <div class="row-body">
          <div class="row-title">依见钟勤</div>
          <div class="row-sub">${esc(app.appName || '依见钟勤')} · v${esc(app.versionName || '-')}</div>
        </div>
        <span class="badge">元气</span>
      </div>
    </div>
    <div class="card">
      <div class="page-title" style="margin-bottom:8px;">专注</div>
      <p class="muted">番茄钟功能由 focus 插件提供。当前已安装：</p>
      <div id="home-plugins"></div>
    </div>
  `;
  const box = document.getElementById('home-plugins');
  if (box) {
    box.innerHTML = state.plugins
      .filter(p => !p.builtin)
      .map(p => `<div class="row"><div class="row-icon">${esc(p.icon)}</div><div class="row-body"><div class="row-title">${esc(p.name)}</div><div class="row-sub">${esc(p.description)}</div></div></div>`)
      .join('') || '<p class="muted" style="padding:12px 0;">暂无第三方插件</p>';
  }
}

/* ── 插件页 ───────────────────────────────────────────── */
function renderPlugins(container) {
  const plugins = state.plugins.sort((a, b) => (a.builtin === b.builtin) ? 0 : (a.builtin ? -1 : 1));

  container.innerHTML = `
    <div class="page-title">插件</div>
    ${plugins.map(p => `
      <div class="card">
        <div class="row" style="border:none;padding-top:4px;">
          <div class="row-icon">${esc(p.icon)}</div>
          <div class="row-body">
            <div class="row-title">${esc(p.name)}</div>
            <div class="row-sub">${esc(p.description)}</div>
            <div class="row-sub" style="margin-top:6px;">v${esc(p.version)}</div>
          </div>
          <span class="badge ${p.enabled ? '' : 'off'}">${p.enabled ? '已启用' : '已禁用'}</span>
        </div>
        <div class="btn-group">
          ${p.hasConfig ? `<button class="btn" data-act="config-${esc(p.id)}">配置</button>` : ''}
          ${(p.actions || []).map(a =>
            `<button class="btn ${a.destructive ? 'danger' : ''}" data-act="action-${esc(p.id)}-${esc(a.id)}">${esc(a.icon)} ${esc(a.label)}</button>`
          ).join('')}
          ${p.builtin ? '' : `<button class="btn danger" data-act="uninstall-${esc(p.id)}">卸载</button>`}
        </div>
        <div class="config-area" data-config="${esc(p.id)}" style="margin-top:8px;"></div>
      </div>
    `).join('')}
  `;

  // 按钮绑定
  $$('.btn', container).forEach(btn => {
    btn.addEventListener('click', () => {
      const act = btn.dataset.act;
      if (!act) return;
      if (act.startsWith('config-')) {
        openConfig(act.slice(7), container);
      } else if (act.startsWith('action-')) {
        const parts = act.split('-');
        // action-<pluginId>-<actionId>，但 pluginId 含点号，需要重新解析
        invokePluginAction(act);
      } else if (act.startsWith('uninstall-')) {
        uninstallPlugin(act.slice(10));
      }
    });
  });
}

async function invokePluginAction(act) {
  // act = "action-<pluginId>-<actionId>"，pluginId 是 full id（含点号）
  const idx = act.lastIndexOf('-');
  const actionId = act.slice(idx + 1);
  const pluginId = act.slice('action-'.length, idx);
  const result = Host.invokeAction(pluginId, actionId);
  if (result && result !== 'null') {
    toast(`插件返回：${result}`);
  } else {
    toast('已执行');
  }
}

async function uninstallPlugin(pluginId) {
  if (!confirm(`确定卸载插件「${pluginId}」？`)) return;
  const ok = Host.uninstallPlugin(pluginId);
  if (ok) {
    toast('已卸载');
    state.plugins = state.plugins.filter(p => p.id !== pluginId);
    renderPlugins(document.getElementById('page-container'));
  } else {
    toast('卸载失败（内置插件不可卸载）');
  }
}

/* ── 插件配置 ─────────────────────────────────────────── */
function openConfig(pluginId, container) {
  const area = container.querySelector(`[data-config="${pluginId}"]`);
  if (!area) return;

  const schema = JSON.parse(Host.getConfigSchema(pluginId) || 'null');
  if (!schema) { toast('该插件无配置项'); return; }

  // 插件声明了自定义配置 HTML → 用 iframe + srcdoc 注入（宿主读取内容）
  if (schema.customHtml) {
    const html = Host.getCustomConfigHtml(pluginId) || '';
    area.innerHTML = `
      <iframe srcdoc="${esc(html)}"
        style="width:100%;height:520px;border:none;border-radius:12px;background:transparent;"></iframe>
    `;
    return;
  }

  area.innerHTML = schema.sections.map(section => `
    <div style="margin-top:12px;">
      <div class="row-sub" style="font-weight:600;margin-bottom:8px;">${esc(section.title)}</div>
      ${section.fields.map(renderField).join('')}
    </div>
  `).join('');

  // 绑定控件
  $$('[data-field]', area).forEach(ctl => {
    const pluginIdKey = pluginId;
    const key = ctl.dataset.field;
    const type = ctl.dataset.type;

    if (type === 'BOOLEAN') {
      ctl.addEventListener('click', () => {
        const on = ctl.classList.toggle('on');
        // 未初始化前不写
      });
    } else if (type === 'SELECT' || type === 'TEXT' || type === 'INT') {
      ctl.addEventListener('change', () => {
        Host.writeConfig(pluginIdKey, key, ctl.value);
      });
    }
  });

  // 布尔开关的初始化值
  $$('.switch', area).forEach(sw => {
    const key = sw.dataset.field;
    const cur = Host.readConfig(pluginId, key, sw.dataset.default);
    if (cur === 'true') sw.classList.add('on');
  });
}

function renderField(field) {
  const type = field.type;
  let control = '';
  const cur = field.value !== undefined && field.value !== null ? field.value : field.defaultValue;

  if (type === 'BOOLEAN') {
    control = `<button class="switch ${cur === 'true' ? 'on' : ''}" data-field="${esc(field.key)}" data-type="BOOLEAN" data-default="${esc(field.defaultValue)}"></button>`;
  } else if (type === 'SELECT') {
    const opts = (field.options || []).map(o =>
      `<option value="${esc(o.value)}" ${String(o.value) === String(cur) ? 'selected' : ''}>${esc(o.label)}</option>`
    ).join('');
    control = `<select data-field="${esc(field.key)}" data-type="SELECT">${opts}</select>`;
  } else if (type === 'INT') {
    control = `<input type="number" data-field="${esc(field.key)}" data-type="INT" value="${esc(cur)}" min="${field.min != null ? field.min : ''}" max="${field.max != null ? field.max : ''}">`;
  } else {
    control = `<input type="text" data-field="${esc(field.key)}" data-type="TEXT" value="${esc(cur)}">`;
  }

  return `
    <div class="field">
      ${type === 'BOOLEAN'
        ? `<div class="field-row"><div><div class="field-label">${esc(field.label)}</div>${field.description ? `<div class="field-desc">${esc(field.description)}</div>` : ''}</div>${control}</div>`
        : `<div class="field-label">${esc(field.label)}</div>${field.description ? `<div class="field-desc">${esc(field.description)}</div>` : ''}${control}`}
    </div>
  `;
}

/* ── 设置页 ───────────────────────────────────────────── */
function renderSettings(container) {
  container.innerHTML = `
    <div class="page-title">设置</div>
    <div class="card">
      <div class="row" style="border:none;">
        <div class="row-icon">ℹ️</div>
        <div class="row-body">
          <div class="row-title">依见钟勤</div>
          <div class="row-sub">HTML 主题系统 · 自定义主界面</div>
        </div>
      </div>
    </div>
    <div class="card">
      <div class="page-title" style="font-size:16px;">主题说明</div>
      <p class="muted">将自定义主题包放在<br><code>Android/data/top.funcun.companion.app/files/themes/current/</code><br>并包含 index.html 即可接管全部界面。</p>
    </div>
  `;
}

/* ── 插件导航页数据渲染 ───────────────────────────────── */
function renderPluginNav(item, container) {
  container.innerHTML = `<div class="page-title">${esc(item.label)}</div><div id="plugin-nav-body"><div class="empty">加载中…</div></div>`;

  const body = document.getElementById('plugin-nav-body');
  try {
    const data = Host.requestNavData(item.pluginId, item.id);
    if (!data || data === 'null') {
      body.innerHTML = '<div class="empty">该插件未提供数据</div>';
      return;
    }
    // 插件返回 JSON → 用默认卡片渲染（数值/文本字段）
    renderNavData(body, JSON.parse(data));
  } catch (e) {
    body.innerHTML = '<div class="empty">数据解析失败</div>';
  }
}

function renderNavData(body, data) {
  if (data.daily && Array.isArray(data.daily)) {
    // 统计曲线
    const stats = [
      { label: '今日', num: data.todayMinutes || 0, unit: '分' },
      { label: '本周', num: data.weekMinutes || 0, unit: '分' },
      { label: '连续', num: data.streakDays || 0, unit: '天' },
      { label: '累计', num: data.totalMinutes || 0, unit: '分' },
    ];
    body.innerHTML = `
      <div class="stat-grid">
        ${stats.map(s => `<div class="stat-cell"><div class="stat-num">${s.num}<span style="font-size:13px;color:var(--sub);">${s.unit}</span></div><div class="stat-label">${s.label}</div></div>`).join('')}
      </div>
      <div class="card" style="margin-top:12px;">
        <div style="font-weight:600;font-size:14px;margin-bottom:4px;">近 7 天</div>
        <div class="bars">
          ${data.daily.map(d => {
            const max = Math.max(...data.daily.map(x => x.minutes || 0), 1);
            const h = Math.max(6, Math.round(((d.minutes || 0) / max) * 100));
            return `<div class="bar-wrap"><div class="bar" style="height:${h}px;"></div><span class="bar-label">${esc(labelForDay(d.epochDay))}</span></div>`;
          }).join('')}
        </div>
      </div>
    `;
  } else {
    // 通用键值渲染
    const rows = Object.entries(data).map(([k, v]) =>
      `<div class="row"><div class="row-body"><div class="row-title">${esc(k)}</div></div><span>${esc(v)}</span></div>`
    ).join('');
    body.innerHTML = rows || '<div class="empty">无数据</div>';
  }
}

function labelForDay(epochDay) {
  const d = new Date(epochDay * 86400000);
  const names = ['日', '一', '二', '三', '四', '五', '六'];
  return names[d.getDay()];
}

/* ── 启动 ─────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', init);

// 追加 Toast 元素
const toastEl = document.createElement('div');
toastEl.id = 'toast';
toastEl.style.cssText = 'position:fixed;left:50%;bottom:88px;transform:translateX(-50%);background:rgba(11,26,51,.85);color:#fff;padding:8px 16px;border-radius:20px;font-size:13px;z-index:999;opacity:0;transition:opacity .2s;max-width:80%;';
document.addEventListener('DOMContentLoaded', () => document.body.appendChild(toastEl));