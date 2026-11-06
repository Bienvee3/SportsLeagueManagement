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
