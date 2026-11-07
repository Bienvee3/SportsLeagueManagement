/* Sports League Management — client interactions (Bootstrap 5) */

(function () {
  'use strict';

  /* ---- sidebar toggle (mobile) ---- */
  const sidebar   = document.getElementById('sidebar');
  const backdrop  = document.getElementById('sidebar-backdrop');
  const menuBtn   = document.getElementById('menu-toggle-btn');

  function openSidebar() {
    sidebar  && sidebar.classList.add('show');
    backdrop && backdrop.classList.add('show');
    document.body.style.overflow = 'hidden';
  }

  function closeSidebar() {
    sidebar  && sidebar.classList.remove('show');
    backdrop && backdrop.classList.remove('show');
    document.body.style.overflow = '';
  }

  function toggleSidebar() {
    const isMobile = window.innerWidth < 992;
    if (isMobile) {
      if (sidebar && sidebar.classList.contains('show')) {
        closeSidebar();
      } else {
        openSidebar();
      }
    } else {
      document.body.classList.toggle('sidebar-collapsed');
      document.body.style.overflow = '';
    }
  }

  menuBtn  && menuBtn.addEventListener('click', toggleSidebar);
  backdrop && backdrop.addEventListener('click', closeSidebar);

  /* Close drawer on resize to desktop */
  window.addEventListener('resize', () => {
    if (window.innerWidth >= 992) closeSidebar();
  });

  /* ---- role preview switcher (dev) ---- */
  const roleSwitch = document.querySelector('.role-switch');
  if (roleSwitch) {
    const csrf = document.querySelector('meta[name="_csrf"]').content;
    const previewing = roleSwitch.dataset.previewing === 'true';
    const post = (url, body) => fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
      headers: {'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-TOKEN': csrf},
      body
    });
    roleSwitch.querySelectorAll('[data-role-switch]').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (btn.classList.contains('active')) return;
        roleSwitch.classList.add('busy');
        try {
          if (previewing) await post('/dev/switch-exit', '');
          const target = btn.dataset.roleSwitch;
          if (target !== 'ADMIN') {
            await post('/dev/switch', 'username=' + encodeURIComponent(target));
          }
        } finally {
          window.location.assign('/');
        }
      });
    });
  }

  /* ---- app language toggle (EN/HR) ---- */
  const langToggle = document.getElementById('langToggle');
  if (langToggle) {
    langToggle.addEventListener('change', () => {
      const url = new URL(window.location.href);
      url.searchParams.set('appLang', langToggle.checked ? 'hr' : 'en');
      window.location.assign(url.toString());
    });
  }


  /* ---- AI chronicle (inline, all roles) ---- */
  const chronicleCard = document.getElementById('chronicle-card');
  if (chronicleCard) {
    const out = document.getElementById('chronicle-text');
    const csrf = document.querySelector('meta[name="_csrf"]').content;
    chronicleCard.querySelectorAll('[data-chronicle-lang]').forEach(btn => {
      btn.addEventListener('click', async () => {
        out.className = 'chronicle-text mb-0 text-secondary';
        out.textContent = 'Generating chronicle…';
        chronicleCard.querySelectorAll('button').forEach(b => b.disabled = true);
        try {
          const res = await fetch(chronicleCard.dataset.url + '?lang=' + btn.dataset.chronicleLang, {
            method: 'POST', credentials: 'same-origin', headers: {'X-CSRF-TOKEN': csrf}
          });
          if (!res.ok) throw new Error('HTTP ' + res.status);
          const data = await res.json();
          out.className = 'chronicle-text mb-0';
          out.textContent = data.text;
        } catch (e) {
          out.className = 'chronicle-text mb-0 text-danger';
          out.textContent = 'Could not generate the chronicle (' + e.message + ').';
        } finally {
          chronicleCard.querySelectorAll('button').forEach(b => b.disabled = false);
        }
      });
    });
  }
  /* ---- confirm dangerous actions ---- */
  document.querySelectorAll('[data-confirm]').forEach(btn => {
    btn.addEventListener('click', e => {
      if (!confirm(btn.dataset.confirm)) e.preventDefault();
    });
  });

  /* ---- auto-dismiss alerts ---- */
  document.querySelectorAll('.alert-dismissible.auto-dismiss').forEach(el => {
    setTimeout(() => {
      const bsAlert = bootstrap.Alert.getOrCreateInstance(el);
      bsAlert && bsAlert.close();
    }, 4000);
  });

})();
