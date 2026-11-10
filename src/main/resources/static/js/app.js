/* Sports League Management — client interactions (Bootstrap 5) */

(function () {
  'use strict';

  /* ---- app language (EN/HR buttons) ---- */
  document.querySelectorAll('[data-app-lang]').forEach(btn => {
    btn.addEventListener('click', () => {
      if (btn.classList.contains('active')) return;
      const url = new URL(window.location.href);
      url.searchParams.set('appLang', btn.dataset.appLang);
      window.location.assign(url.toString());
    });
  });


  /* ---- AI chronicle (all roles read; only ADMIN/OFFICIAL get the Generate button) ---- */
  const chronicleCard = document.getElementById('chronicle-card');
  if (chronicleCard) {
    const out = document.getElementById('chronicle-text');
    const generateBtn = chronicleCard.querySelector('[data-chronicle-generate]'); // absent for VIEWER
    const canWrite = generateBtn !== null;
    const url = chronicleCard.dataset.url;
    const query = '?lang=' + chronicleCard.dataset.chronicleLang;
    const csrf = document.querySelector('meta[name="_csrf"]').content;
    const hintHtml = out.innerHTML;
    const setBusy = busy => chronicleCard.querySelectorAll('button').forEach(b => b.disabled = busy);

    const showText = text => { out.className = 'chronicle-text mb-0'; out.textContent = text; };
    const showHint = () => { out.className = 'chronicle-text mb-0 text-secondary small'; out.innerHTML = hintHtml; };
    const showWorking = () => { out.className = 'chronicle-text mb-0 text-secondary'; out.textContent = document.body.dataset.msgGenerating; };

    async function call(method, path) {
      const res = await fetch(url + path + query, {
        method, credentials: 'same-origin', headers: {'X-CSRF-TOKEN': csrf}
      });
      if (res.status === 204) return null;            // nothing stored / nothing to translate
      if (!res.ok) throw new Error('HTTP ' + res.status);
      return (await res.json()).text;
    }

    async function guarded(task) {
      setBusy(true);
      try {
        const text = await task();
        text === null ? showHint() : showText(text);
      } catch (e) {
        out.className = 'chronicle-text mb-0 text-danger';
        out.textContent = document.body.dataset.msgChronicleError + ' (' + e.message + ')';
      } finally {
        setBusy(false);
      }
    }

    // Page load: the stored chronicle (read-only GET, no provider call). ADMIN/OFFICIAL additionally
    // translate a chronicle that only exists in the other language via the explicit translate action.
    guarded(async () => {
      const stored = await call('GET', '');
      if (stored !== null || !canWrite) return stored;
      showWorking();
      return call('POST', '/translate');
    });

    if (canWrite) {
      generateBtn.addEventListener('click', () => guarded(() => { showWorking(); return call('POST', ''); }));
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

  /* ---- match sheet: lineup row checkboxes (starter unchecks bench and vice versa) ---- */
  document.querySelectorAll('tr[data-lineup-row]').forEach(row => {
    const starter = row.querySelector('input[name="starterIds"]');
    const bench   = row.querySelector('input[name="benchIds"]');
    if (starter && bench) {
      starter.addEventListener('change', () => { if (starter.checked) bench.checked = false; });
      bench.addEventListener('change',   () => { if (bench.checked) starter.checked = false; });
    }
  });

  /* ---- match event form: toggle between lineup select and free-text name by team ---- */
  const eventForm = document.querySelector('form[data-event-form]');
  if (eventForm) {
    const teamSelect = eventForm.querySelector('select[name="teamId"]');
    const textFields = eventForm.querySelectorAll('[data-text-field]');
    const lineupFields = eventForm.querySelectorAll('[data-lineup-field]');
    const playerSelects = eventForm.querySelectorAll('select[name="playerId"], select[name="relatedPlayerId"]');

    function syncEventFields() {
      const opt = teamSelect.selectedOptions[0];
      const hasLineup = opt && opt.dataset.hasLineup === 'true';
      const teamId = opt ? opt.value : '';

      textFields.forEach(wrapper => {
        wrapper.classList.toggle('d-none', hasLineup);
        wrapper.querySelectorAll('input, select').forEach(input => input.disabled = hasLineup);
      });
      lineupFields.forEach(wrapper => {
        wrapper.classList.toggle('d-none', !hasLineup);
        wrapper.querySelectorAll('input, select').forEach(input => input.disabled = !hasLineup);
      });
      playerSelects.forEach(sel => {
        Array.from(sel.options).forEach((option, idx) => {
          if (idx === 0) return; // keep "Select player…"
          const matches = option.dataset.team === teamId;
          option.hidden = !matches;
          option.disabled = !matches;
        });
        if (sel.selectedOptions[0] && sel.selectedOptions[0].disabled) {
          sel.value = '';
        }
      });
    }

    teamSelect && teamSelect.addEventListener('change', syncEventFields);
    syncEventFields();
  }

})();
