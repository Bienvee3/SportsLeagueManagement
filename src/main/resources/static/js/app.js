/* Sports League Management — client interactions
   No data rendering: all content is server-side Thymeleaf.
   Handles: sidebar toggle, theme, toasts, modals, tabs, chronicle AJAX.
*/

/* ---------- theme ---------- */
(function() {
  const saved = localStorage.getItem('lo.theme');
  if (saved === 'dark') document.documentElement.setAttribute('data-theme', 'dark');
  if (saved === 'light') document.documentElement.setAttribute('data-theme', 'light');
})();

document.addEventListener('DOMContentLoaded', () => {

  /* ---------- sidebar (mobile) ---------- */
  const shell = document.querySelector('.shell');
  document.querySelectorAll('[data-act="open-nav"]').forEach(btn =>
    btn.addEventListener('click', () => shell?.classList.add('nav-open')));
  document.querySelectorAll('[data-act="close-nav"], .scrim').forEach(el =>
    el.addEventListener('click', () => shell?.classList.remove('nav-open')));

  /* ---------- theme toggle ---------- */
  document.querySelectorAll('[data-act="toggle-theme"]').forEach(btn => {
    btn.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme') ||
        (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('lo.theme', next);
    });
  });

  /* ---------- user menu popover ---------- */
  const userWrap = document.querySelector('.user-menu-wrap');
  if (userWrap) {
    const popover = userWrap.querySelector('.popover');
    const trigger = userWrap.querySelector('.user-btn');
    trigger?.addEventListener('click', e => {
      e.stopPropagation();
      popover?.classList.toggle('open');
    });
    document.addEventListener('click', () => popover?.classList.remove('open'));
  }

  /* ---------- flash toast ---------- */
  const toastMsg = document.getElementById('flash-toast')?.textContent?.trim();
  if (toastMsg) showToast(toastMsg);

  /* ---------- confirm-delete modal ---------- */
  document.querySelectorAll('[data-confirm]').forEach(btn => {
    btn.addEventListener('click', e => {
      const msg = btn.dataset.confirm || 'Are you sure?';
      if (!confirm(msg)) e.preventDefault();
    });
  });

  /* ---------- tabs (anchor-based) ---------- */
  document.querySelectorAll('.tabs .tab[href]').forEach(tab => {
    tab.addEventListener('click', () => {
      const panel = tab.getAttribute('href')?.slice(1);
      document.querySelectorAll('.tab-panel').forEach(p => {
        p.hidden = p.id !== panel;
      });
      document.querySelectorAll('.tab').forEach(t => {
        t.setAttribute('aria-selected', t === tab ? 'true' : 'false');
        t.classList.toggle('active', t === tab);
      });
    });
  });

  /* activate first tab panel if none active */
  const firstTab = document.querySelector('.tabs .tab[href]');
  if (firstTab && !document.querySelector('.tab[aria-selected="true"]')) {
    firstTab.click();
  }

  /* ---------- chronicle AJAX ---------- */
  const chronicleBtn = document.getElementById('chronicle-btn');
  if (chronicleBtn) {
    chronicleBtn.addEventListener('click', async () => {
      const fixtureId = chronicleBtn.dataset.fixtureId;
      const lang = document.getElementById('chronicle-lang')?.value || 'ENGLISH';
      const resultEl = document.getElementById('chronicle-result');
      const skeletonEl = document.getElementById('chronicle-skeleton');

      chronicleBtn.classList.add('loading');
      chronicleBtn.disabled = true;
      if (skeletonEl) skeletonEl.hidden = false;
      if (resultEl) resultEl.hidden = true;

      try {
        const resp = await fetch(`/api/fixtures/${fixtureId}/chronicle?lang=${lang}`, {
          method: 'POST',
          headers: { 'X-XSRF-TOKEN': getCsrfToken() }
        });
        if (!resp.ok) throw new Error(`${resp.status}`);
        const data = await resp.json();
        if (resultEl) {
          resultEl.textContent = data.text;
          resultEl.hidden = false;
        }
        if (skeletonEl) skeletonEl.hidden = true;
        showToast('Chronicle generated.');
      } catch (err) {
        showToast('Failed to generate chronicle: ' + err.message);
        if (skeletonEl) skeletonEl.hidden = true;
      } finally {
        chronicleBtn.classList.remove('loading');
        chronicleBtn.disabled = false;
      }
    });
  }
});

/* ---------- helpers ---------- */
function showToast(msg) {
  const wrap = document.querySelector('.toast-wrap');
  if (!wrap) return;
  const el = document.createElement('div');
  el.className = 'toast';
  el.textContent = msg;
  wrap.appendChild(el);
  setTimeout(() => el.remove(), 4000);
}

function getCsrfToken() {
  return document.cookie.split(';')
    .map(c => c.trim())
    .find(c => c.startsWith('XSRF-TOKEN='))
    ?.split('=')[1] ?? '';
}
