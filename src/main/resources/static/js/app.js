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

  /* ---- app language (EN/HR buttons) ---- */
  document.querySelectorAll('[data-app-lang]').forEach(btn => {
    btn.addEventListener('click', () => {
      if (btn.classList.contains('active')) return;
      const url = new URL(window.location.href);
      url.searchParams.set('appLang', btn.dataset.appLang);
      window.location.assign(url.toString());
    });
  });


  /* ---- AI chronicle (inline, all roles) ---- */
  const chronicleCard = document.getElementById('chronicle-card');
  if (chronicleCard) {
    const out = document.getElementById('chronicle-text');
    const button = chronicleCard.querySelector('[data-chronicle-lang]');
    const lang = button.dataset.chronicleLang;
    const csrf = document.querySelector('meta[name="_csrf"]').content;
    const hintHtml = out.innerHTML;
    const setBusy = busy => chronicleCard.querySelectorAll('button').forEach(b => b.disabled = busy);

    async function run(method) {
      out.className = 'chronicle-text mb-0 text-secondary';
      out.textContent = document.body.dataset.msgGenerating;
      setBusy(true);
      try {
        const res = await fetch(chronicleCard.dataset.url + '?lang=' + lang, {
          method, credentials: 'same-origin', headers: {'X-CSRF-TOKEN': csrf}
        });
        if (res.status === 204) {            // nothing generated yet
          out.className = 'chronicle-text mb-0 text-secondary small';
          out.innerHTML = hintHtml;
          return;
        }
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const data = await res.json();
        out.className = 'chronicle-text mb-0';
        out.textContent = data.text;
      } catch (e) {
        out.className = 'chronicle-text mb-0 text-danger';
        out.textContent = document.body.dataset.msgChronicleError + ' (' + e.message + ')';
      } finally {
        setBusy(false);
      }
    }

    button.addEventListener('click', () => run('POST'));   // write a new chronicle
    run('GET');                                            // show stored one, translating if needed
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
