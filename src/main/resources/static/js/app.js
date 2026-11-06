/* Sports League Management — client interactions (Bootstrap 5) */

(function () {
  'use strict';

  /* ---- sidebar toggle (mobile) ---- */
  const sidebar   = document.getElementById('sidebar');
  const backdrop  = document.getElementById('sidebar-backdrop');
  const menuBtn   = document.getElementById('menu-toggle-btn');

  function openSidebar() {
    sidebar && sidebar.classList.add('show');
    backdrop && (backdrop.style.display = 'block');
    document.body.style.overflow = 'hidden';
  }

  function closeSidebar() {
    sidebar && sidebar.classList.remove('show');
    backdrop && (backdrop.style.display = 'none');
    document.body.style.overflow = '';
  }

  menuBtn  && menuBtn.addEventListener('click', openSidebar);
  backdrop && backdrop.addEventListener('click', closeSidebar);

  /* ---- flash toast ---- */
  const flashEl = document.getElementById('flash-data');
  if (flashEl && flashEl.dataset.msg) {
    const container = document.getElementById('toast-container');
    if (container) {
      const div = document.createElement('div');
      div.className = 'toast align-items-center text-bg-dark border-0 show';
      div.setAttribute('role', 'alert');
      div.innerHTML = `
        <div class="d-flex">
          <div class="toast-body">${flashEl.dataset.msg}</div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto"
                  data-bs-dismiss="toast"></button>
        </div>`;
      container.appendChild(div);
      setTimeout(() => div.remove(), 4000);
    }
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
