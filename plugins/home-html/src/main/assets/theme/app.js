'use strict';

/* ══════════════════════════════════════════════════════════════
 * 默认主题脚本
 *
 * 通过 window.TianyiHost（宿主注入）读取插件列表、导航项、插件数据，
 * 渲染全部页面（专注/权限/统计/插件/设置）+ 底部导航。
 *
 * 约定：
 * - TianyiHost 所有方法返回 JSON 字符串，用 JSON.parse 解析
 * - 插件自定义配置 HTML 为「片段式」：只含 <style> 与 body 内容
 * - window.onTianyiBackPressed() 返回 true 表示已消费返回键
 * ══════════════════════════════════════════════════════════════ */

const Host = window.TianyiHost;

/* ── 全局状态 ─────────────────────────────────────────── */
const state = {
  navItems: [],
  plugins: [],
  currentNavId: null,
  openConfigPluginId: null,   // 当前展开配置的插件（用于 toggle 与返回键收起）
};

const STORAGE_TAB = 'tianyi.currentNavId';

function $(sel, root) { return (root || document).querySelector(sel); }
function $$(sel, root) { return Array.from((root || document).querySelectorAll(sel)); }

function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/* ── Toast ────────────────────────────────────────────── */
function toast(msg) {
  let el = document.getElementById('toast');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast';
    el.className = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(el._t);
  el._t = setTimeout(() => el.classList.remove('show'), 1800);
}

/* ── 自绘确认框（不依赖 window.confirm） ──────────────── */
function confirmDialog(message) {
  return new Promise(resolve => {
    const mask = document.createElement('div');
    mask.className = 'dialog-mask';
    mask.innerHTML = `
      <div class="dialog">
        <p class="dialog-msg">${esc(message)}</p>
        <div class="dialog-actions">
          <button class="btn" data-role="cancel">取消</button>
          <button class="btn danger" data-role="ok">确定</button>
        </div>
      </div>
    `;
    document.body.appendChild(mask);

    const close = (result) => {
      mask.remove();
      resolve(result);
    };
    mask.querySelector('[data-role="ok"]').addEventListener('click', () => close(true));
    mask.querySelector('[data-role="cancel"]').addEventListener('click', () => close(false));
    mask.addEventListener('click', e => { if (e.target === mask) close(false); });
  });
}

/* ── 启动 ──────────────────────────────────────────────── */
function init() {
  if (!Host) {
    document.body.innerHTML = '<div class="empty">宿主接口未就绪</div>';
    return;
  }

  reloadData();
  renderNav();

  // 恢复上次 Tab（主题重载后不丢失位置）
  const saved = sessionStorage.getItem(STORAGE_TAB);
  const idx = Math.max(0, state.navItems.findIndex(i => i.id === saved));
  navigate(idx);
}

function reloadData() {
  state.plugins = safeParse(Host.getPlugins(), { plugins: [] }).plugins || [];
  const pluginNav = safeParse(Host.getNavItems(), { items: [] }).items || [];

  // 主题自带导航项（即使无插件贡献也可用）
  const builtinNav = [
    { id: 'home', label: '首页', icon: '🏠', order: 0, pluginId: '' },
    { id: 'plugins', label: '插件', icon: '🧩', order: 40, pluginId: '' },
    { id: 'settings', label: '设置', icon: '⚙️', order: 50, pluginId: '' },
  ];
  state.navItems = dedupe([...builtinNav, ...pluginNav].sort((a, b) => a.order - b.order));
}

function safeParse(str, fallback) {
  try { return JSON.parse(str || 'null') || fallback; } catch (e) { return fallback; }
}

function dedupe(items) {
  const seen = new Set();
  return items.filter(i => (seen.has(i.id) ? false : (seen.add(i.id), true)));
}

/* ── 底部导航 ─────────────────────────────────────────── */
function renderNav() {
  const nav = document.getElementById('bottom-nav');
  if (!nav) return;

  nav.innerHTML = state.navItems.map((item, idx) => `
    <button class="nav-item" data-idx="${idx}">
      <span class="nav-icon">${esc(item.icon || '·')}</span>
      <span>${esc(item.label)}</span>
    </button>
  `).join('');

  $$('.nav-item', nav).forEach(btn => {
    btn.addEventListener('click', () => navigate(parseInt(btn.dataset.idx, 10)));
  });
}

function navigate(idx) {
  const item = state.navItems[idx];
  if (!item) return;
  state.currentNavId = item.id;
  state.openConfigPluginId = null;
  sessionStorage.setItem(STORAGE_TAB, item.id);

  $$('.nav-item').forEach((btn, i) => btn.classList.toggle('active', i === idx));
  renderPage(item);
}

function renderPage(item) {
  const container = document.getElementById('page-container');
  container.innerHTML = '';

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

/* ── 首页 ─────────────────────────────────────────────── */
function renderHome(container) {
  const app = safeParse(Host.getAppInfo(), {});
  const nonBuiltin = state.plugins.filter(p => !p.builtin);

  container.innerHTML = `
    <div class="card">
      <div class="row" style="border:none;padding-top:4px;">
        <div class="row-icon brand">洛</div>
        <div class="row-body">
          <div class="row-title">${esc(app.appName || '依见钟勤')}</div>
          <div class="row-sub">v${esc(app.versionName || '-')}</div>
        </div>
      </div>
    </div>
    <div class="card">
      <div class="section-title">已安装插件</div>
      ${state.plugins.map(p => `
        <div class="row">
          <div class="row-icon">${esc(p.icon)}</div>
          <div class="row-body">
            <div class="row-title">${esc(p.name)}</div>
            <div class="row-sub">${esc(p.description)}</div>
          </div>
          <span class="badge ${p.enabled ? '' : 'off'}">${p.enabled ? '已启用' : '已禁用'}</span>
        </div>
      `).join('')}
      ${nonBuiltin.length === 0 ? '<p class="muted" style="padding-top:10px;">暂无第三方插件</p>' : ''}
    </div>
  `;
}

/* ── 插件页 ───────────────────────────────────────────── */
function renderPlugins(container) {
  const plugins = state.plugins.slice().sort((a, b) =>
    (a.builtin === b.builtin) ? 0 : (a.builtin ? -1 : 1));

  container.innerHTML = `
    <div class="page-title">插件</div>
    ${plugins.map(p => `
      <div class="card" data-plugin-card="${esc(p.id)}">
        <div class="row" style="border:none;padding-top:4px;">
          <div class="row-icon">${esc(p.icon)}</div>
          <div class="row-body">
            <div class="row-title">${esc(p.name)}</div>
            <div class="row-sub">${esc(p.description)}</div>
            <div class="row-sub" style="margin-top:6px;">v${esc(p.version)}${p.builtin ? ' · 内置' : ''}</div>
          </div>
          <span class="badge ${p.enabled ? '' : 'off'}">${p.enabled ? '已启用' : '已禁用'}</span>
        </div>
        <div class="btn-group">
          ${p.hasConfig
            ? `<button class="btn" data-role="config" data-plugin="${esc(p.id)}">配置</button>`
            : ''}
          ${(p.actions || []).map(a =>
            `<button class="btn ${a.destructive ? 'danger' : ''}" data-role="action"
              data-plugin="${esc(p.id)}" data-action="${esc(a.id)}">${esc(a.icon)} ${esc(a.label)}</button>`
          ).join('')}
          ${p.builtin
            ? ''
            : `<button class="btn danger" data-role="uninstall" data-plugin="${esc(p.id)}">卸载</button>`}
        </div>
        <div class="config-area" data-config="${esc(p.id)}"></div>
      </div>
    `).join('')}
  `;

  // 事件绑定：用独立 data 属性，避免字符串拼接解析歧义
  $$('[data-role]', container).forEach(btn => {
    const role = btn.dataset.role;
    const pluginId = btn.dataset.plugin;

    if (role === 'config') {
      btn.addEventListener('click', () => toggleConfig(pluginId, container, btn));
    } else if (role === 'action') {
      btn.addEventListener('click', () => invokeAction(pluginId, btn.dataset.action));
    } else if (role === 'uninstall') {
      btn.addEventListener('click', () => uninstallPlugin(pluginId));
    }
  });
}

function invokeAction(pluginId, actionId) {
  const raw = Host.invokeAction(pluginId, actionId);
  const result = safeParse(raw, null);
  if (result && result.message) {
    toast(result.message);
  } else if (raw && raw !== 'null') {
    toast('已执行');
  } else {
    toast('该动作无返回');
  }
}

async function uninstallPlugin(pluginId) {
  const plugin = state.plugins.find(p => p.id === pluginId);
  const name = plugin ? plugin.name : pluginId;
  const ok = await confirmDialog(`确定卸载插件「${name}」？`);
  if (!ok) return;

  if (Host.uninstallPlugin(pluginId)) {
    toast('已卸载');
    reloadData();
    renderNav();
    renderPlugins(document.getElementById('page-container'));
  } else {
    toast('卸载失败：内置插件不可卸载');
  }
}

/* ── 插件配置（点击可展开/收起） ──────────────────────── */
function toggleConfig(pluginId, container, btn) {
  const area = container.querySelector(`[data-config="${pluginId}"]`);
  if (!area) return;

  // 已展开 → 收起
  if (state.openConfigPluginId === pluginId) {
    closeConfig(container);
    return;
  }

  // 先收起其它已展开的
  closeConfig(container);

  const schema = safeParse(Host.getConfigSchema(pluginId), null);
  if (!schema) { toast('该插件无配置项'); return; }

  state.openConfigPluginId = pluginId;
  area.classList.add('open');
  btn.classList.add('active');

  // 插件自定义配置界面：宿主返回片段，直接内联（与主题同源，可用 TianyiHost）
  if (schema.customHtml) {
    const html = Host.getCustomConfigHtml(pluginId) || '';
    area.innerHTML = extractFragment(html);
    // 执行片段内的脚本（innerHTML 不会自动执行 script）
    runInlineScripts(area);
    return;
  }

  area.innerHTML = schema.sections.map(section => `
    <div class="config-section">
      <div class="section-title">${esc(section.title)}</div>
      ${section.fields.map(renderField).join('')}
    </div>
  `).join('');

  bindConfigControls(pluginId, area);
}

function closeConfig(container) {
  $$('.config-area', container).forEach(a => {
    a.innerHTML = '';
    a.classList.remove('open');
  });
  $$('[data-role="config"]', container).forEach(b => b.classList.remove('active'));
  state.openConfigPluginId = null;
}

/**
 * 从插件返回的 HTML 中提取可内联片段：
 * 保留 <style> 与 <body> 内容，丢弃 <html>/<head>/<meta> 等外层结构。
 */
function extractFragment(html) {
  const styles = (html.match(/<style[\s\S]*?<\/style>/gi) || []).join('\n');
  const bodyMatch = html.match(/<body[^>]*>([\s\S]*?)<\/body>/i);
  const body = bodyMatch ? bodyMatch[1] : html;
  return styles + body;
}

/** 执行内联 <script>（innerHTML 插入的脚本不会自动运行） */
function runInlineScripts(root) {
  $$('script', root).forEach(old => {
    const s = document.createElement('script');
    if (old.src) s.src = old.src;
    else s.textContent = old.textContent;
    old.parentNode.replaceChild(s, old);
  });
}

function bindConfigControls(pluginId, area) {
  // 开关：点击即写回
  $$('.switch[data-field]', area).forEach(sw => {
    sw.addEventListener('click', () => {
      const on = sw.classList.toggle('on');
      Host.writeConfig(pluginId, sw.dataset.field, on ? 'true' : 'false');
      toast('已保存');
    });
  });

  // 输入/下拉：change 时写回
  $$('input[data-field], select[data-field]', area).forEach(ctl => {
    ctl.addEventListener('change', () => {
      let value = ctl.value;
      if (ctl.dataset.type === 'INT') {
        const min = ctl.getAttribute('min');
        const max = ctl.getAttribute('max');
        let n = parseInt(value, 10);
        if (isNaN(n)) n = 0;
        if (min !== null && min !== '' && n < parseInt(min, 10)) n = parseInt(min, 10);
        if (max !== null && max !== '' && n > parseInt(max, 10)) n = parseInt(max, 10);
        value = String(n);
        ctl.value = value;
      }
      Host.writeConfig(pluginId, ctl.dataset.field, value);
      toast('已保存');
    });
  });
}

function renderField(field) {
  const type = field.type;
  const cur = (field.value !== undefined && field.value !== null && field.value !== '')
    ? field.value : field.defaultValue;
  let control = '';

  if (type === 'BOOLEAN') {
    control = `<button class="switch ${cur === 'true' ? 'on' : ''}"
      data-field="${esc(field.key)}" data-type="BOOLEAN"></button>`;
  } else if (type === 'SELECT') {
    const opts = (field.options || []).map(o =>
      `<option value="${esc(o.value)}" ${String(o.value) === String(cur) ? 'selected' : ''}>${esc(o.label)}</option>`
    ).join('');
    control = `<select data-field="${esc(field.key)}" data-type="SELECT">${opts}</select>`;
  } else if (type === 'INT') {
    control = `<input type="number" data-field="${esc(field.key)}" data-type="INT"
      value="${esc(cur)}"
      ${field.min != null ? `min="${field.min}"` : ''}
      ${field.max != null ? `max="${field.max}"` : ''}>`;
  } else {
    control = `<input type="text" data-field="${esc(field.key)}" data-type="TEXT" value="${esc(cur)}">`;
  }

  const desc = field.description ? `<div class="field-desc">${esc(field.description)}</div>` : '';

  if (type === 'BOOLEAN') {
    return `<div class="field"><div class="field-row">
      <div><div class="field-label">${esc(field.label)}</div>${desc}</div>${control}
    </div></div>`;
  }
  return `<div class="field">
    <div class="field-label">${esc(field.label)}</div>${desc}${control}
  </div>`;
}

/* ── 设置页 ───────────────────────────────────────────── */
function renderSettings(container) {
  const app = safeParse(Host.getAppInfo(), {});
  const theme = safeParse(Host.getThemeInfo(), { installed: false, source: 'builtin' });

  container.innerHTML = `
    <div class="page-title">设置</div>
    <div class="card">
      <div class="row" style="border:none;">
        <div class="row-icon">ℹ️</div>
        <div class="row-body">
          <div class="row-title">${esc(app.appName || '依见钟勤')}</div>
          <div class="row-sub">版本 ${esc(app.versionName || '-')}（${esc(app.versionCode || 0)}）</div>
        </div>
      </div>
      <div class="row">
        <div class="row-icon">🎨</div>
        <div class="row-body">
          <div class="row-title">当前主题</div>
          <div class="row-sub">${theme.installed ? '自定义主题包' : '内置默认主题'}</div>
        </div>
        <span class="badge">${theme.installed ? 'user' : 'builtin'}</span>
      </div>
    </div>
    <div class="card">
      <p class="muted">主题包与插件配置请到「插件」页对应插件的「配置」中管理。</p>
    </div>
  `;
}

/* ── 插件导航页 ───────────────────────────────────────── */
function renderPluginNav(item, container) {
  container.innerHTML = `
    <div class="page-title">${esc(item.label)}</div>
    <div id="plugin-nav-body"><div class="empty">加载中…</div></div>
  `;
  const body = document.getElementById('plugin-nav-body');
  const data = safeParse(Host.requestNavData(item.pluginId, item.id), null);

  if (!data) {
    body.innerHTML = '<div class="empty">该插件未提供数据</div>';
    return;
  }
  renderNavData(body, data, item);
}

function renderNavData(body, data, item) {
  // 权限类数据
  if (data.type === 'permissions' && Array.isArray(data.items)) {
    body.innerHTML = `
      <div class="card">
        ${data.items.map(p => `
          <div class="row">
            <div class="row-icon">${esc(p.icon)}</div>
            <div class="row-body">
              <div class="row-title">${esc(p.name)}</div>
              <div class="row-sub">${esc(p.hint)}</div>
            </div>
            <span class="badge ${p.granted ? '' : 'off'}">${p.granted ? '已授予' : '未授予'}</span>
          </div>
        `).join('')}
      </div>
      <p class="muted">${data.allGranted ? '全部权限已授予 ✓' : '部分权限未授予，可在插件页「重新引导」重新授权。'}</p>
    `;
    return;
  }

  // 统计类数据（含 daily 曲线）
  if (Array.isArray(data.daily)) {
    const stats = [
      { label: '今日', num: data.todayMinutes || 0, unit: '分' },
      { label: '本周', num: data.weekMinutes || 0, unit: '分' },
      { label: '连续', num: data.streakDays || 0, unit: '天' },
      { label: '累计', num: data.totalMinutes || 0, unit: '分' },
    ];
    const max = Math.max(...data.daily.map(x => x.minutes || 0), 1);

    body.innerHTML = `
      <div class="stat-grid">
        ${stats.map(s => `
          <div class="stat-cell">
            <div class="stat-num">${s.num}<span class="stat-unit">${s.unit}</span></div>
            <div class="stat-label">${s.label}</div>
          </div>
        `).join('')}
      </div>
      <div class="card" style="margin-top:12px;">
        <div class="section-title">近 7 天</div>
        <div class="bars">
          ${data.daily.map(d => {
            const h = Math.max(6, Math.round(((d.minutes || 0) / max) * 100));
            return `<div class="bar-wrap">
              <div class="bar" style="height:${h}px;"></div>
              <span class="bar-label">${esc(labelForDay(d.epochDay))}</span>
            </div>`;
          }).join('')}
        </div>
      </div>
      ${data.totalSessions === 0 ? '<p class="muted">还没有专注记录，完成一次专注后这里会显示数据。</p>' : ''}
    `;
    return;
  }

  // 通用键值渲染
  const rows = Object.entries(data)
    .filter(([, v]) => typeof v !== 'object')
    .map(([k, v]) => `
      <div class="row">
        <div class="row-body"><div class="row-title">${esc(k)}</div></div>
        <span class="badge">${esc(v)}</span>
      </div>
    `).join('');
  body.innerHTML = rows ? `<div class="card">${rows}</div>` : '<div class="empty">无数据</div>';
}

function labelForDay(epochDay) {
  const d = new Date(epochDay * 86400000);
  return ['日', '一', '二', '三', '四', '五', '六'][d.getDay()];
}

/* ── 返回键处理（宿主调用） ───────────────────────────── */
window.onTianyiBackPressed = function () {
  // 配置面板展开中 → 收起
  if (state.openConfigPluginId) {
    closeConfig(document.getElementById('page-container'));
    return true;
  }
  // 非首个 Tab → 回到首个 Tab
  if (state.currentNavId && state.navItems.length && state.currentNavId !== state.navItems[0].id) {
    navigate(0);
    return true;
  }
  return false; // 交给系统（退出应用）
};

/* ── 启动 ─────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', init);