// ============ DOM Cache (performance: avoid repeated getElementById) ============
const dom = {};
[
  'bankList', 'bankSelect', 'historyList',
  'statSessions', 'statAvgAcc', 'statTotalTime',
  'dropZone', 'fileInput',
  'bankSearch', 'wordListOverlay', 'wordListTitle', 'wordListSearch', 'wordListBody', 'wordListClose', 'wordCount',
  'learnSetup', 'learnSession', 'learnResult',
  'resumeBanner', 'btnResume', 'btnClearSaved',
  'btnStart', 'btnPause', 'btnQuit',
  'flashcardArea', 'quizArea', 'typingArea',
  'cardContainer', 'card', 'cardTag', 'cardWord', 'cardDef', 'cardHint', 'learnActions', 'cardStar',
  'srsActions',
  'btnForgot', 'btnRemember',
  'quizTag', 'quizLabel', 'quizQuestion', 'quizOptions', 'quizStar',
  'typingTag', 'typingQuestion', 'typingHint', 'typingInput', 'typingFeedback', 'typingActions', 'btnTypingNext', 'typingStar',
  'pauseOverlay', 'pauseInfo', 'btnContinue', 'btnSaveQuit',
  'resultTitle', 'resultBank', 'resultTotal', 'resultRemembered', 'resultAccuracy', 'resultDuration', 'resultAvgTime', 'resultForgotten',
  'btnAgain', 'btnBackToSetup',
  'progressText', 'timerDisplay',
  'themeToggle',
  'goalInput', 'goalSave', 'goalDone', 'goalTotal', 'ringFill', 'streakNum', 'goalCalendar',
  'statsSummary'
].forEach(id => { dom[id] = document.getElementById(id); });

// ============ Data Layer ============
const DB = {
  get(key, def) {
    try { const v = localStorage.getItem('vocab_' + key); return v ? JSON.parse(v) : def; }
    catch { return def; }
  },
  set(key, val) { localStorage.setItem('vocab_' + key, JSON.stringify(val)); },
  remove(key) { localStorage.removeItem('vocab_' + key); },
  getBanks() { return this.get('banks', []); },
  setBanks(b) { this.set('banks', b); },
  getHistory() { return this.get('history', []); },
  setHistory(h) { this.set('history', h); },
  addHistory(entry) {
    const h = this.getHistory();
    h.unshift(entry);
    if (h.length > 200) h.length = 200;
    this.setHistory(h);
    return h;
  },
  getWrongBook() { return this.get('wrongBook', []); },
  setWrongBook(wb) { this.set('wrongBook', wb); },
  getTheme() { return this.get('theme', 'system'); },
  setTheme(t) { this.set('theme', t); },
  getFavorites() { return this.get('favorites', []); },
  setFavorites(f) { this.set('favorites', f); },
  getDailyGoal() { return this.get('dailyGoal', 20); },
  setDailyGoal(n) { this.set('dailyGoal', n); },
  getCheckins() { return this.get('checkins', {}); },
  setCheckins(c) { this.set('checkins', c); },
  getSrsData() { return this.get('srs', {}); },
  setSrsData(d) { this.set('srs', d); }
};

// ============ Theme Management ============
function applyTheme(theme) {
  const root = document.documentElement;
  if (theme === 'dark') {
    root.setAttribute('data-theme', 'dark');
  } else if (theme === 'light') {
    root.setAttribute('data-theme', 'light');
  } else {
    // system: remove attribute so @media (prefers-color-scheme) takes effect
    root.removeAttribute('data-theme');
  }
  // Update toggle button icon
  const btn = dom.themeToggle;
  if (!btn) return;
  if (theme === 'dark') btn.textContent = '🌙';
  else if (theme === 'light') btn.textContent = '☀️';
  else btn.textContent = window.matchMedia('(prefers-color-scheme: dark)').matches ? '🌙' : '☀️';
}

function cycleTheme() {
  const current = DB.getTheme();
  const next = current === 'system' ? 'dark' : current === 'dark' ? 'light' : 'system';
  DB.setTheme(next);
  applyTheme(next);
}

// Listen for system theme changes when in 'system' mode
const mq = window.matchMedia('(prefers-color-scheme: dark)');
mq.addEventListener('change', () => {
  if (DB.getTheme() === 'system') applyTheme('system');
});

// ============ Bank Management ============
function parseTxt(text) {
  const lines = text.split(/\r?\n/).filter(l => l.trim());
  const cards = [];
  for (const line of lines) {
    let parts = null;
    if (line.includes('\t')) parts = line.split('\t');
    else if (line.includes(' - ')) parts = line.split(' - ');
    else if (line.includes(' | ')) parts = line.split(' | ');
    else if (line.includes('|')) parts = line.split('|');
    else if (line.includes('：')) parts = line.split('：');
    else if (line.includes(':')) parts = line.split(':');
    if (parts && parts.length >= 2) {
      cards.push({ word: parts[0].trim(), definition: parts.slice(1).join(' ').trim() });
    }
  }
  return cards;
}

function importBank(name, cards) {
  const banks = DB.getBanks();
  const existing = banks.findIndex(b => b.name === name);
  if (existing >= 0) {
    banks[existing].cards = cards;
    banks[existing].count = cards.length;
    banks[existing].updatedAt = Date.now();
  } else {
    banks.push({ name, cards, count: cards.length, createdAt: Date.now(), updatedAt: Date.now() });
  }
  DB.setBanks(banks);
  renderBanks();
}

function deleteBank(name) {
  let banks = DB.getBanks();
  banks = banks.filter(b => b.name !== name);
  DB.setBanks(banks);
  // Clean up SRS data for deleted bank
  const srs = DB.getSrsData();
  if (srs[name]) { delete srs[name]; DB.setSrsData(srs); }
  renderBanks();
  populateBankSelect();
}

function getBank(name) {
  return DB.getBanks().find(b => b.name === name);
}

// ============ Export ============
function downloadTxt(filename, content) {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function exportBank(name) {
  const bank = getBank(name);
  if (!bank) return;
  const content = bank.cards.map(c => `${c.word} - ${c.definition}`).join('\n');
  downloadTxt(`${name}.txt`, content);
}

function exportWrongBook() {
  const wb = DB.getWrongBook();
  if (!wb.length) { alert('错题本为空'); return; }
  const content = wb.map(w => `${w.word} - ${w.definition}`).join('\n');
  downloadTxt(`错题本.txt`, content);
}

function exportFavorites() {
  const favs = DB.getFavorites();
  if (!favs.length) { alert('收藏夹为空'); return; }
  const content = favs.map(f => `${f.word} - ${f.definition}`).join('\n');
  downloadTxt(`收藏单词.txt`, content);
}

// ============ Wrong Book (错题本) ============
function addToWrongBook(word, definition, bankName) {
  const wb = DB.getWrongBook();
  const existing = wb.find(w => w.word === word && w.bankName === bankName);
  if (existing) {
    existing.wrongCount++;
    existing.lastWrongAt = Date.now();
  } else {
    wb.push({ word, definition, bankName, wrongCount: 1, lastWrongAt: Date.now() });
  }
  DB.setWrongBook(wb);
}

function removeFromWrongBook(word, bankName) {
  const wb = DB.getWrongBook().filter(w => !(w.word === word && w.bankName === bankName));
  DB.setWrongBook(wb);
}

// ============ Favorites (收藏单词) ============
function addFavorite(word, definition, bankName) {
  const favs = DB.getFavorites();
  if (!favs.find(f => f.word === word && f.bankName === bankName)) {
    favs.push({ word, definition, bankName, createdAt: Date.now() });
    DB.setFavorites(favs);
  }
}

function removeFavorite(word, bankName) {
  DB.setFavorites(DB.getFavorites().filter(f => !(f.word === word && f.bankName === bankName)));
}

function isFavorite(word, bankName) {
  return !!DB.getFavorites().find(f => f.word === word && f.bankName === bankName);
}

function toggleFavorite(word, definition, bankName) {
  if (isFavorite(word, bankName)) {
    removeFavorite(word, bankName);
    return false;
  } else {
    addFavorite(word, definition, bankName);
    return true;
  }
}

// Start learning from favorites
function startFavoritesLearn() {
  const favs = DB.getFavorites();
  if (!favs.length) { alert('收藏夹为空'); return; }

  document.querySelector('[data-tab="learn"]').click();

  const direction = document.querySelector('input[name="direction"]:checked').value;
  const mode = document.querySelector('input[name="mode"]:checked').value;
  clearSavedSession();

  const cards = favs.map(f => {
    const isWordFirst = direction === 'word-first';
    return {
      word: f.word,
      definition: f.definition,
      front: isWordFirst ? f.word : f.definition,
      back: isWordFirst ? f.definition : f.word,
      _originBank: f.bankName,
    };
  });

  learnState = {
    bankName: '收藏单词',
    cards: shuffle([...cards]),
    index: 0,
    remembered: 0,
    forgotten: 0,
    startTime: Date.now(),
    timerInterval: null,
    totalCards: cards.length,
    answered: false,
    mode: mode,
    direction: direction,
    isFavorites: true,
  };
  lastSessionWasFavorites = true;
  lastSessionWasWrongBook = false;

  dom.learnSetup.style.display = 'none';
  dom.learnSession.classList.add('active');
  dom.learnResult.classList.remove('active');
  showModeArea(mode);
  resetPauseButton();
  showModeQuestion();
  startTimer();
}

// ============ Star Button Helpers ============
function updateStarButton(btnEl, card) {
  if (!btnEl || !card) return;
  const bankName = card._originBank || (learnState ? learnState.bankName : '');
  const isFav = isFavorite(card.word, bankName);
  btnEl.textContent = isFav ? '⭐' : '☆';
  btnEl.classList.toggle('active', isFav);
}

function handleStarClick(btnEl, card) {
  if (!card) return;
  const bankName = card._originBank || (learnState ? learnState.bankName : '');
  const nowFav = toggleFavorite(card.word, card.definition, bankName);
  btnEl.textContent = nowFav ? '⭐' : '☆';
  btnEl.classList.toggle('active', nowFav);
  renderBanks(); // refresh favorites card
}

// ============ SRS (Spaced Repetition) ============
// SM-2 Algorithm implementation
function srsGetBankData(bankName) {
  const all = DB.getSrsData();
  if (!all[bankName]) all[bankName] = {};
  return all[bankName];
}

function srsSaveBankData(bankName, data) {
  const all = DB.getSrsData();
  all[bankName] = data;
  DB.setSrsData(all);
}

function srsEnsureCard(bankName, word, definition) {
  const data = srsGetBankData(bankName);
  if (!data[word]) {
    data[word] = { word, definition, ef: 2.5, interval: 0, repetitions: 0, nextReview: 0, lastReview: 0 };
    srsSaveBankData(bankName, data);
  }
  return data[word];
}

function srsUpdateCard(bankName, word, definition, rating) {
  const data = srsGetBankData(bankName);
  let card = data[word];
  if (!card) {
    card = { word, definition, ef: 2.5, interval: 0, repetitions: 0, nextReview: 0, lastReview: 0 };
    data[word] = card;
  }

  // SM-2 algorithm
  if (rating < 3) {
    card.repetitions = 0;
    card.interval = 0;
  } else {
    if (card.repetitions === 0) card.interval = 1;
    else if (card.repetitions === 1) card.interval = 6;
    else card.interval = Math.round(card.interval * card.ef);
    card.repetitions++;
  }

  // Update easiness factor
  card.ef = card.ef + (0.1 - (5 - rating) * (0.08 + (5 - rating) * 0.02));
  if (card.ef < 1.3) card.ef = 1.3;
  card.ef = Math.round(card.ef * 100) / 100;

  const intervalMs = card.interval * 86400000;
  card.nextReview = Date.now() + intervalMs;
  card.lastReview = Date.now();
  card.definition = definition;

  srsSaveBankData(bankName, data);
}

function srsGetDueCards(bankName) {
  const data = srsGetBankData(bankName);
  const now = Date.now();
  const due = [];
  for (const key in data) {
    if (data[key].nextReview <= now) {
      due.push(data[key]);
    }
  }
  return due;
}

function srsCountDue(bankName) {
  const data = srsGetBankData(bankName);
  const now = Date.now();
  let count = 0;
  for (const key in data) {
    if (data[key].nextReview <= now) count++;
  }
  return count;
}
function recordAnswerResult(card, isCorrect) {
  if (!learnState) return;
  if (learnState.isWrongBook) {
    if (isCorrect) {
      // Mastered: remove from wrong book
      removeFromWrongBook(card.word, card._originBank);
    } else {
      // Still wrong: increment count
      addToWrongBook(card.word, card.definition, card._originBank);
    }
  } else {
    if (!isCorrect) {
      // Wrong answer in normal session: add to wrong book
      addToWrongBook(card.word, card.definition, learnState.bankName);
    }
  }
}

// ============ Rendering ============
let _currentSearch = '';

function renderBanks(searchTerm) {
  const el = dom.bankList;
  const banks = DB.getBanks();
  const wrongBook = DB.getWrongBook();
  const search = (searchTerm !== undefined ? searchTerm : _currentSearch || '').trim().toLowerCase();
  _currentSearch = search;

  // Filter banks by search term
  const filteredBanks = search
    ? banks.filter(b => b.name.toLowerCase().includes(search))
    : banks;

  let html = '';

  // Wrong book card (shown when there are wrong words)
  if (wrongBook.length > 0) {
    const showWrong = !search || '错题本'.includes(search);
    if (showWrong) {
      const totalWrong = wrongBook.reduce((s, w) => s + w.wrongCount, 0);
      html += `
        <div class="bank-card wrong-book-card" data-bank="__wrongbook__" style="border-left:3px solid var(--amber);">
          <div class="info">
            <div class="name">📕 错题本</div>
            <div class="meta">${wrongBook.length} 个单词 · 累计错误 ${totalWrong} 次</div>
          </div>
          <div class="actions">
            <button class="btn-study">学习</button>
            <button class="btn-export">导出</button>
            <button class="btn-del">清空</button>
          </div>
        </div>`;
    }
  }

  // Favorites card
  const favorites = DB.getFavorites();
  if (favorites.length > 0) {
    const showFav = !search || '收藏'.includes(search);
    if (showFav) {
      html += `
        <div class="bank-card wrong-book-card" data-bank="__favorites__" style="border-left:3px solid var(--ink);">
          <div class="info">
            <div class="name">⭐ 收藏单词</div>
            <div class="meta">${favorites.length} 个单词</div>
          </div>
          <div class="actions">
            <button class="btn-study">学习</button>
            <button class="btn-export">导出</button>
            <button class="btn-del">清空</button>
          </div>
        </div>`;
    }
  }

  if (!banks.length && !wrongBook.length && !favorites.length) {
    el.innerHTML = '<div class="empty-state"><div class="icon">📖</div><p>还没有词库，导入一个 TXT 文件开始吧</p></div>';
    return;
  }

  if (!filteredBanks.length && !search) {
    // no banks at all
  } else if (!filteredBanks.length && search) {
    html += '<div class="empty-state"><div class="icon">🔍</div><p>没有找到匹配的词库</p></div>';
  }

  html += filteredBanks.map(function(b) {
    var due = typeof srsCountDue === 'function' ? srsCountDue(b.name) : 0;
    var dueBadge = due > 0 ? '<span class="due-badge">' + due + ' 待复习</span>' : '';
    var nameHtml = highlightMatch(escHtml(b.name), search);
    var metaHtml = b.count + ' 个单词 · ' + new Date(b.updatedAt).toLocaleDateString() + dueBadge;
    return '<div class="bank-card" data-bank="' + escAttr(b.name) + '">' +
      '<div class="info" data-action="view-words">' +
        '<div class="name">' + nameHtml + '</div>' +
        '<div class="meta">' + metaHtml + '</div>' +
      '</div>' +
      '<div class="actions">' +
        '<button class="btn-study">学习</button>' +
        '<button class="btn-export">导出</button>' +
        '<button class="btn-del">删除</button>' +
      '</div>' +
    '</div>';
  }).join('');

  el.innerHTML = html;
}

function highlightMatch(text, search) {
  if (!search) return text;
  const idx = text.toLowerCase().indexOf(search);
  if (idx === -1) return text;
  return text.slice(0, idx) + '<mark style="background:var(--ink-100);color:var(--ink);border-radius:3px;padding:0 2px;">' +
    text.slice(idx, idx + search.length) + '</mark>' + text.slice(idx + search.length);
}

function populateBankSelect() {
  const sel = dom.bankSelect;
  const banks = DB.getBanks();
  sel.innerHTML = '<option value="">-- 请选择词库 --</option>' +
    banks.map(b => `<option value="${escAttr(b.name)}">${escHtml(b.name)} (${b.count}词)</option>`).join('');
}

function renderHistory() {
  const history = DB.getHistory();
  const el = dom.historyList;
  if (!history.length) {
    el.innerHTML = '<div class="empty-history">暂无学习记录</div>';
    return;
  }
  const modeLabel = { flashcard: '闪卡', quiz: '答题', typing: '打字', srs: '间隔重复' };
  el.innerHTML = history.map(h => `
    <div class="history-item">
      <div class="left">
        <span class="bank-name">${escHtml(h.bankName)}</span>
        <span class="detail">${h.total} 题 · ${h.remembered} 正确</span>
        <span class="mode-tag">${modeLabel[h.mode] || '闪卡'}</span>
        ${h.interrupted ? '<span class="mode-tag" style="background:var(--amber-light);color:var(--amber);">中断</span>' : ''}
      </div>
      <div class="right">
        <div class="acc">${h.accuracy}%</div>
        <div class="date">${formatDuration(h.duration)} · ${new Date(h.date).toLocaleString()}</div>
      </div>
    </div>
  `).join('');
}

function updateStatsSummary() {
  const history = DB.getHistory();
  const sessions = history.length;
  if (!sessions) {
    dom.statSessions.textContent = '0';
    dom.statAvgAcc.textContent = '0%';
    dom.statTotalTime.textContent = '0m';
    return;
  }
  const avgAcc = Math.round(history.reduce((s, h) => s + h.accuracy, 0) / sessions);
  const totalTime = history.reduce((s, h) => s + h.duration, 0);
  dom.statSessions.textContent = sessions;
  dom.statAvgAcc.textContent = avgAcc + '%';
  dom.statTotalTime.textContent = Math.round(totalTime / 60) + 'm';
}

// ============ Daily Goal & Check-in ============
function recordCheckin(count) {
  const today = new Date().toISOString().slice(0, 10);
  const checkins = DB.getCheckins();
  checkins[today] = (checkins[today] || 0) + count;
  DB.setCheckins(checkins);
  updateGoalUI();
}

function calcStreak() {
  const checkins = DB.getCheckins();
  const goal = DB.getDailyGoal();
  let streak = 0;
  const d = new Date();
  // Check up to 365 days back
  for (let i = 0; i < 365; i++) {
    const key = d.toISOString().slice(0, 10);
    const count = checkins[key] || 0;
    if (count >= goal) {
      streak++;
      d.setDate(d.getDate() - 1);
    } else if (i === 0) {
      // Today hasn't been completed yet — allow checking yesterday
      d.setDate(d.getDate() - 1);
      continue; // don't break; check yesterday
    } else {
      break;
    }
  }
  return streak;
}

function updateGoalUI() {
  const goal = DB.getDailyGoal();
  const checkins = DB.getCheckins();
  const today = new Date().toISOString().slice(0, 10);
  const todayCount = checkins[today] || 0;
  const pct = Math.min(todayCount / goal, 1);

  // Ring
  const circumference = 2 * Math.PI * 54; // r=54
  dom.ringFill.style.strokeDashoffset = circumference * (1 - pct);
  dom.goalDone.textContent = todayCount;
  dom.goalTotal.textContent = goal;
  dom.goalInput.value = goal;

  // Streak
  dom.streakNum.textContent = calcStreak();

  // Calendar (last 28 days = 4 weeks)
  const calEl = dom.goalCalendar;
  const days = ['日', '一', '二', '三', '四', '五', '六'];
  let calHtml = days.map(d => `<div class="cal-day-label">${d}</div>`).join('');
  const now = new Date();
  for (let i = 27; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    const count = checkins[key] || 0;
    const isToday = i === 0;
    const classes = ['cal-day'];
    if (isToday) classes.push('today');
    if (count >= goal) classes.push('done');
    else if (count > 0) classes.push('partial');
    calHtml += `<div class="${classes.join(' ')}">${d.getDate()}</div>`;
  }
  calEl.innerHTML = calHtml;
}

// ============ Word List Overlay ============
function showWordList(bankName) {
  const bank = getBank(bankName);
  if (!bank) return;
  dom.wordListTitle.textContent = bankName;
  dom.wordListOverlay.classList.add('active');
  dom.wordListSearch.value = '';
  renderWordList(bank.cards, '');
}

function renderWordList(cards, search) {
  const term = search.trim().toLowerCase();
  const filtered = term
    ? cards.filter(c => c.word.toLowerCase().includes(term) || c.definition.toLowerCase().includes(term))
    : cards;
  dom.wordCount.textContent = filtered.length + ' 词';
  if (!filtered.length) {
    dom.wordListBody.innerHTML = '<div class="word-list-empty">没有匹配的单词</div>';
    return;
  }
  dom.wordListBody.innerHTML = filtered.map(c =>
    `<div class="word-list-item">
      <span class="wl-word">${highlightMatch(escHtml(c.word), term)}</span>
      <span class="wl-def">${highlightMatch(escHtml(c.definition), term)}</span>
    </div>`
  ).join('');
}

// ============ Learning Engine ============
let learnState = null;
let savedSessionCheck = null;
// Re-apply to "again" — look at previous session type
let lastSessionWasWrongBook = false;
let lastSessionWasFavorites = false;

function shuffle(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

function startLearn(bankName) {
  document.querySelector('[data-tab="learn"]').click();
  dom.bankSelect.value = bankName;
  beginSession();
}

// Start a study session from the wrong book
function startWrongBookLearn() {
  const wrongBook = DB.getWrongBook();
  if (!wrongBook.length) { alert('错题本为空'); return; }

  // Switch to learn tab (reads selected mode/direction from radio buttons)
  document.querySelector('[data-tab="learn"]').click();

  const direction = document.querySelector('input[name="direction"]:checked').value;
  const mode = document.querySelector('input[name="mode"]:checked').value;
  clearSavedSession();

  const cards = wrongBook.map(w => {
    const isWordFirst = direction === 'word-first';
    return {
      word: w.word,
      definition: w.definition,
      front: isWordFirst ? w.word : w.definition,
      back: isWordFirst ? w.definition : w.word,
      _originBank: w.bankName,
    };
  });

  learnState = {
    bankName: '错题本',
    cards: shuffle([...cards]),
    index: 0,
    remembered: 0,
    forgotten: 0,
    startTime: Date.now(),
    timerInterval: null,
    totalCards: cards.length,
    answered: false,
    mode: mode,
    direction: direction,
    isWrongBook: true,
  };
  lastSessionWasWrongBook = true;
  lastSessionWasFavorites = false;

  dom.learnSetup.style.display = 'none';
  dom.learnSession.classList.add('active');
  dom.learnResult.classList.remove('active');
  showModeArea(mode);
  resetPauseButton();
  showModeQuestion();
  startTimer();
}

// ============ Mode Area Helper (performance: centralize show/hide) ============
function showModeArea(mode) {
  dom.flashcardArea.style.display = (mode === 'flashcard' || mode === 'srs') ? 'block' : 'none';
  dom.quizArea.style.display = mode === 'quiz' ? 'block' : 'none';
  dom.typingArea.style.display = mode === 'typing' ? 'block' : 'none';
}

function getActiveArea() {
  if (!learnState) return null;
  return { flashcard: dom.flashcardArea, quiz: dom.quizArea, typing: dom.typingArea, srs: dom.flashcardArea }[learnState.mode] || dom.flashcardArea;
}

function showModeQuestion() {
  if (!learnState) return;
  if (learnState.mode === 'quiz') showQuizQuestion();
  else if (learnState.mode === 'typing') showTypingQuestion();
  else if (learnState.mode === 'srs') showSrsCard();
  else showCard();
}

// ============ Session Management (Pause / Resume) ============
function saveSession() {
  if (!learnState) return;
  const elapsed = Math.round((Date.now() - learnState.startTime) / 1000);
  const saved = {
    bankName: learnState.bankName,
    cards: learnState.cards,
    index: learnState.index,
    remembered: learnState.remembered,
    forgotten: learnState.forgotten,
    elapsedSeconds: elapsed,
    mode: learnState.mode,
    direction: learnState.direction,
    totalCards: learnState.totalCards,
    answered: false,
    isWrongBook: learnState.isWrongBook || false,
    isFavorites: learnState.isFavorites || false,
  };
  DB.set('savedSession', saved);
}

function clearSavedSession() {
  DB.remove('savedSession');
  dom.resumeBanner.style.display = 'none';
  savedSessionCheck = null;
}

function checkSavedSession() {
  const saved = DB.get('savedSession', null);
  const banner = dom.resumeBanner;
  const btn = dom.btnResume;
  if (saved && saved.cards && saved.cards.length) {
    savedSessionCheck = saved;
    btn.textContent = `▶ 继续上次「${escHtml(saved.bankName)}」(${saved.remembered + saved.forgotten}/${saved.totalCards})`;
    banner.style.display = 'block';
  } else {
    banner.style.display = 'none';
    savedSessionCheck = null;
  }
}

function resumeSession() {
  const saved = savedSessionCheck;
  if (!saved) return;
  clearSavedSession();

  // Wrong book sessions don't have a real bank entry; skip the bank lookup
  if (!saved.isWrongBook && !saved.isFavorites) {
    const bank = getBank(saved.bankName);
    if (!bank) { alert('词库已不存在'); return; }
  }

  learnState = {
    bankName: saved.bankName,
    cards: saved.cards,
    index: saved.index,
    remembered: saved.remembered,
    forgotten: saved.forgotten,
    startTime: Date.now() - (saved.elapsedSeconds * 1000),
    timerInterval: null,
    totalCards: saved.totalCards,
    answered: false,
    mode: saved.mode || 'flashcard',
    direction: saved.direction || 'word-first',
    isWrongBook: saved.isWrongBook || false,
    isFavorites: saved.isFavorites || false,
  };
  lastSessionWasWrongBook = saved.isWrongBook || false;
  lastSessionWasFavorites = saved.isFavorites || false;

  dom.learnSetup.style.display = 'none';
  dom.learnSession.classList.add('active');
  dom.learnResult.classList.remove('active');

  showModeArea(learnState.mode);
  resetPauseButton();
  showModeQuestion();
  startTimer();
}

function resetPauseButton() {
  dom.btnPause.classList.remove('paused');
  dom.btnPause.textContent = '⏸ 暂停';
}

function pauseSession() {
  if (!learnState) return;
  stopTimer();
  saveSession();

  const el = getActiveArea();
  if (el) el.style.opacity = '0.4';
  dom.btnPause.classList.add('paused');
  dom.btnPause.textContent = '⏸ 已暂停';
  dom.pauseOverlay.classList.add('active');
  dom.pauseInfo.textContent =
    `已保存至第 ${learnState.index + 1}/${learnState.totalCards} 题`;
}

function continueSession() {
  if (!learnState) return;
  dom.pauseOverlay.classList.remove('active');
  const el = getActiveArea();
  if (el) el.style.opacity = '1';
  resetPauseButton();
  startTimer();
}

function saveAndQuit() {
  if (!learnState) return;
  saveSession();
  dom.pauseOverlay.classList.remove('active');
  const el = getActiveArea();
  if (el) el.style.opacity = '1';
  stopTimer();
  learnState = null;
  dom.learnSession.classList.remove('active');
  dom.learnResult.classList.remove('active');
  dom.learnSetup.style.display = 'block';
  renderBanks(); // refresh wrong book card (may have changed during session)
  checkSavedSession();
}

// Auto-save on page leave
window.addEventListener('beforeunload', () => {
  if (learnState) saveSession();
});

// ============ Flashcard Mode ============
function beginSession() {
  const sel = dom.bankSelect;
  const name = sel.value;
  if (!name) { alert('请选择一个词库'); return; }
  const bank = getBank(name);
  if (!bank || !bank.cards.length) { alert('词库为空'); return; }

  const direction = document.querySelector('input[name="direction"]:checked').value;
  const mode = document.querySelector('input[name="mode"]:checked').value;
  clearSavedSession();
  lastSessionWasWrongBook = false;
  lastSessionWasFavorites = false;

  let sessionCards;

  if (mode === 'srs') {
    // SRS mode: only due cards
    const due = srsGetDueCards(name);
    if (!due.length) {
      alert('🎉 没有待复习的单词！请明天再来。');
      return;
    }
    // Map due SRS card data to session card format
    sessionCards = due.map(c => {
      const isWordFirst = direction === 'word-first';
      return {
        word: c.word,
        definition: c.definition,
        front: isWordFirst ? c.word : c.definition,
        back: isWordFirst ? c.definition : c.word
      };
    });
  } else {
    // Regular modes: all cards, also ensure SRS data exists for each
    sessionCards = bank.cards.map(c => {
      const isWordFirst = direction === 'word-first';
      // Ensure SRS entry exists for future SRS reviews
      srsEnsureCard(name, c.word, c.definition);
      return {
        word: c.word,
        definition: c.definition,
        front: isWordFirst ? c.word : c.definition,
        back: isWordFirst ? c.definition : c.word
      };
    });
  }

  learnState = {
    bankName: name,
    cards: shuffle([...sessionCards]),
    index: 0,
    remembered: 0,
    forgotten: 0,
    startTime: Date.now(),
    timerInterval: null,
    totalCards: sessionCards.length,
    answered: false,
    mode: mode,
    direction: direction,
  };

  dom.learnSetup.style.display = 'none';
  dom.learnSession.classList.add('active');
  dom.learnResult.classList.remove('active');

  showModeArea(mode);
  resetPauseButton();
  showModeQuestion();
  startTimer();
}

function showCard() {
  if (!learnState) return;
  const state = learnState;
  const card = state.cards[state.index];
  if (!card) { endSession(); return; }

  dom.progressText.textContent = `${state.index + 1} / ${state.totalCards}`;
  dom.cardTag.textContent = `${state.index + 1}/${state.totalCards}`;
  dom.cardWord.textContent = card.front;
  dom.cardDef.textContent = card.back;
  // Fix: toggle .show on the definition element (was .show-def on #card which matched no CSS rule)
  dom.cardDef.classList.remove('show');
  dom.learnActions.style.display = 'none';
  dom.cardHint.style.display = 'block';
  state.answered = false;
  updateStarButton(dom.cardStar, card);
}

function flipCard() {
  if (learnState && !learnState.answered) {
    // Fix: add .show to #cardDef to trigger CSS transition (was .show-def on #card)
    dom.cardDef.classList.add('show');
    dom.cardHint.style.display = 'none';
    dom.learnActions.style.display = 'flex';
  }
}

function answerCard(remembered) {
  if (!learnState || learnState.answered) return;
  learnState.answered = true;
  const card = learnState.cards[learnState.index];
  if (remembered) learnState.remembered++;
  else learnState.forgotten++;
  recordAnswerResult(card, remembered);

  dom.learnActions.style.display = 'none';

  setTimeout(() => {
    learnState.index++;
    if (learnState.index >= learnState.cards.length) {
      endSession();
    } else {
      showCard();
    }
  }, 200);
}

// ============ SRS Mode ============
function showSrsCard() {
  if (!learnState) return;
  const state = learnState;
  const card = state.cards[state.index];
  if (!card) { endSession(); return; }

  dom.progressText.textContent = `${state.index + 1} / ${state.totalCards}`;
  dom.cardTag.textContent = `${state.index + 1}/${state.totalCards}`;
  dom.cardWord.textContent = card.front;
  dom.cardDef.textContent = card.back;
  dom.cardDef.classList.remove('show');
  dom.cardHint.style.display = 'block';
  dom.learnActions.style.display = 'none';
  dom.srsActions.style.display = 'none';
  state.answered = false;
  updateStarButton(dom.cardStar, card);
}

function flipSrsCard() {
  if (learnState && learnState.mode === 'srs' && !learnState.answered) {
    dom.cardDef.classList.add('show');
    dom.cardHint.style.display = 'none';
    dom.srsActions.style.display = 'flex';
  }
}

function answerSrsCard(rating) {
  if (!learnState || learnState.answered) return;
  learnState.answered = true;
  const state = learnState;
  const card = state.cards[state.index];

  // Apply SM-2
  const bankName = card._originBank || state.bankName;
  if (bankName !== '错题本' && bankName !== '收藏单词') {
    srsUpdateCard(bankName, card.word, card.definition, rating);
  }

  // Track "remembered" for result screen (rating >= 3 = remembered)
  if (rating >= 3) state.remembered++;
  else state.forgotten++;

  dom.srsActions.style.display = 'none';

  setTimeout(() => {
    state.index++;
    if (state.index >= state.cards.length) {
      endSession();
    } else {
      showSrsCard();
    }
  }, 300);
}

// ============ Quiz Mode ============
function showQuizQuestion() {
  if (!learnState) return;
  const state = learnState;
  const card = state.cards[state.index];
  if (!card) { endSession(); return; }

  dom.progressText.textContent = `${state.index + 1} / ${state.totalCards}`;
  dom.quizTag.textContent = `${state.index + 1}/${state.totalCards}`;
  dom.quizQuestion.textContent = card.front;
  dom.quizLabel.textContent = '请选择正确的答案';
  state.answered = false;
  updateStarButton(dom.quizStar, card);

  // Build options: 1 correct + 3 distractors
  const correctAnswer = card.back;
  const distractors = [];
  const pool = state.cards.filter((_, i) => i !== state.index);
  const shuffledPool = shuffle([...pool]);
  for (let i = 0; i < shuffledPool.length && distractors.length < 3; i++) {
    if (shuffledPool[i].back !== correctAnswer) {
      distractors.push(shuffledPool[i].back);
    }
  }
  // Fallback if not enough unique distractors
  while (distractors.length < 3) {
    distractors.push('———');
  }

  const options = shuffle([correctAnswer, ...distractors]);
  const optionsEl = dom.quizOptions;
  optionsEl.innerHTML = options.map((opt, i) =>
    `<div class="quiz-option" data-index="${i}" data-correct="${opt === correctAnswer ? '1' : '0'}">${escHtml(opt)}</div>`
  ).join('');
}

function handleQuizAnswer(el) {
  if (!learnState || learnState.answered) return;
  learnState.answered = true;
  const card = learnState.cards[learnState.index];

  const isCorrect = el.dataset.correct === '1';
  if (isCorrect) learnState.remembered++;
  else learnState.forgotten++;
  recordAnswerResult(card, isCorrect);

  // Highlight
  document.querySelectorAll('.quiz-option').forEach(opt => {
    opt.classList.add('disabled');
    if (opt.dataset.correct === '1') opt.classList.add('correct');
    if (opt === el && !isCorrect) opt.classList.add('wrong');
  });

  setTimeout(() => {
    learnState.index++;
    if (learnState.index >= learnState.cards.length) {
      endSession();
    } else {
      showQuizQuestion();
    }
  }, 800);
}

// ============ Typing Mode ============
function showTypingQuestion() {
  if (!learnState) return;
  const state = learnState;
  const card = state.cards[state.index];
  if (!card) { endSession(); return; }

  dom.progressText.textContent = `${state.index + 1} / ${state.totalCards}`;
  dom.typingTag.textContent = `${state.index + 1}/${state.totalCards}`;
  // Typing mode: always show definition, type the word (typing Chinese definitions is impractical)
  dom.typingQuestion.textContent = card.definition;
  dom.typingHint.textContent = '请拼写单词，按回车提交';
  dom.typingInput.value = '';
  dom.typingInput.disabled = false;
  dom.typingInput.classList.remove('correct', 'wrong');
  dom.typingFeedback.textContent = '';
  dom.typingFeedback.className = 'feedback';
  dom.typingActions.style.display = 'none';
  state.answered = false;
  updateStarButton(dom.typingStar, card);
  dom.typingInput.focus();
}

function handleTypingSubmit() {
  if (!learnState || learnState.answered) return;
  const state = learnState;
  const card = state.cards[state.index];
  const userAnswer = dom.typingInput.value.trim().toLowerCase();
  const correctAnswer = card.word.trim().toLowerCase();

  state.answered = true;
  const isCorrect = userAnswer === correctAnswer;
  if (isCorrect) state.remembered++;
  else state.forgotten++;
  recordAnswerResult(card, isCorrect);

  dom.typingInput.classList.add(isCorrect ? 'correct' : 'wrong');
  dom.typingInput.disabled = true;
  dom.typingFeedback.textContent = isCorrect ? '✓ 正确！' : `✗ 正确答案：${card.word}`;
  dom.typingFeedback.className = 'feedback ' + (isCorrect ? 'correct' : 'wrong');
  dom.typingActions.style.display = 'flex';
  dom.btnTypingNext.focus();
}

function handleTypingNext() {
  if (!learnState || !learnState.answered) return;
  learnState.index++;
  if (learnState.index >= learnState.cards.length) endSession();
  else showTypingQuestion();
}

// ============ Results ============
function endSession() {
  const state = learnState;
  if (!state) return;
  stopTimer();

  const total = state.totalCards;
  const remembered = state.remembered;
  const forgotten = state.forgotten;
  const duration = Math.round((Date.now() - state.startTime) / 1000);
  const accuracy = total > 0 ? Math.round((remembered / total) * 100) : 0;
  const avgTime = total > 0 ? Math.round(duration / total) : 0;

  dom.learnSession.classList.remove('active');
  dom.learnResult.classList.add('active');
  const modeLabel = { quiz: '答题完成！', typing: '打字完成！', flashcard: '学习完成！', srs: '复习完成！' };
  const modeName = { quiz: '答题模式', typing: '打字模式', flashcard: '闪卡模式', srs: '间隔重复' };
  dom.resultTitle.textContent = modeLabel[state.mode] || '学习完成！';
  dom.resultBank.textContent = `词库：${state.bankName} · ${modeName[state.mode] || '闪卡模式'}`;
  dom.resultTotal.textContent = total;
  dom.resultRemembered.textContent = remembered;
  dom.resultForgotten.textContent = forgotten;
  dom.resultAccuracy.textContent = accuracy + '%';
  dom.resultDuration.textContent = formatDuration(duration);
  dom.resultAvgTime.textContent = avgTime + 's';

  // Save to history
  DB.addHistory({
    bankName: state.bankName,
    mode: state.mode || 'flashcard',
    total,
    remembered,
    forgotten,
    accuracy,
    duration,
    date: Date.now(),
  });
  updateStatsSummary();
  renderHistory();
  renderBanks(); // refresh wrong book card (words may have been removed/mastered)
  clearSavedSession();

  learnState = null;
  recordCheckin(total);
}

// Record partial stats for an interrupted (quit) session
function recordPartialStats(state) {
  const answered = state.remembered + state.forgotten;
  if (answered === 0) return;
  const duration = Math.round((Date.now() - state.startTime) / 1000);
  const accuracy = Math.round((state.remembered / answered) * 100);
  DB.addHistory({
    bankName: state.bankName,
    mode: state.mode || 'flashcard',
    total: answered,
    remembered: state.remembered,
    forgotten: state.forgotten,
    accuracy,
    duration,
    date: Date.now(),
    interrupted: true,
  });
  recordCheckin(answered);
  updateStatsSummary();
  renderHistory();
}

// ============ Timer ============
function startTimer() {
  stopTimer();
  if (!learnState) return;
  learnState.timerInterval = setInterval(() => {
    const elapsed = Math.round((Date.now() - learnState.startTime) / 1000);
    dom.timerDisplay.textContent = formatDuration(elapsed);
  }, 1000);
}

function stopTimer() {
  if (learnState && learnState.timerInterval) {
    clearInterval(learnState.timerInterval);
    learnState.timerInterval = null;
  }
}

function quitSession() {
  if (!learnState) return;
  stopTimer();
  const answered = learnState.remembered + learnState.forgotten;
  if (answered > 0 && !confirm('确定退出？本次答题将记入统计，但学习进度不会保存（可使用暂停按钮保存进度）。')) {
    startTimer();
    return;
  }
  // Record partial stats for the interrupted session
  if (answered > 0) {
    recordPartialStats(learnState);
  }
  // Refresh bank list (wrong book may have changed)
  renderBanks();
  learnState = null;
  dom.learnSession.classList.remove('active');
  dom.learnResult.classList.remove('active');
  dom.learnSetup.style.display = 'block';
}

// ============ Utilities ============
// Performance: regex-based escape avoids creating a DOM element per call
const _escMap = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
function escHtml(s) {
  return String(s).replace(/[&<>"']/g, c => _escMap[c]);
}
function escAttr(s) {
  return String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

// ============ Event Bindings ============
// Tabs — bottom tab bar
document.querySelectorAll('.tab-bar-item').forEach(tab => {
  tab.addEventListener('click', () => {
    if (learnState) { alert('学习中，请先结束或暂停当前学习'); return; }
    document.querySelectorAll('.tab-bar-item').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    tab.classList.add('active');
    document.getElementById('page-' + tab.dataset.tab).classList.add('active');
    if (tab.dataset.tab === 'stats') {
      updateStatsSummary();
      updateGoalUI();
      renderHistory();
    }
    if (tab.dataset.tab === 'learn') {
      populateBankSelect();
      checkSavedSession();
    }
  });
});

// Theme toggle
dom.themeToggle.addEventListener('click', cycleTheme);

// Goal save
dom.goalSave.addEventListener('click', () => {
  const val = parseInt(dom.goalInput.value);
  if (val > 0 && val <= 500) {
    DB.setDailyGoal(val);
    updateGoalUI();
  }
});
dom.goalInput.addEventListener('keydown', e => {
  if (e.key === 'Enter') dom.goalSave.click();
});

// Bank search
dom.bankSearch.addEventListener('input', e => {
  renderBanks(e.target.value);
});

// Word list overlay
dom.wordListClose.addEventListener('click', () => {
  dom.wordListOverlay.classList.remove('active');
});
dom.wordListOverlay.addEventListener('click', e => {
  if (e.target === dom.wordListOverlay) dom.wordListOverlay.classList.remove('active');
});
dom.wordListSearch.addEventListener('input', e => {
  const bankName = dom.wordListTitle.textContent;
  const bank = getBank(bankName);
  if (bank) renderWordList(bank.cards, e.target.value);
});

// Import file
dom.dropZone.addEventListener('click', () => {
  dom.fileInput.click();
});
dom.fileInput.addEventListener('change', e => {
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = ev => {
    const cards = parseTxt(ev.target.result);
    if (!cards.length) { alert('未解析到有效词条，请确保每行格式为：单词 - 释义'); return; }
    const name = file.name.replace(/\.txt$/i, '');
    importBank(name, cards);
    alert(`成功导入「${name}」(${cards.length} 个单词)`);
    populateBankSelect();
  };
  reader.readAsText(file);
  e.target.value = '';
});

// Drag & Drop
const dz = dom.dropZone;
dz.addEventListener('dragover', e => { e.preventDefault(); dz.classList.add('dragover'); });
dz.addEventListener('dragleave', () => dz.classList.remove('dragover'));
dz.addEventListener('drop', e => {
  e.preventDefault();
  dz.classList.remove('dragover');
  const file = e.dataTransfer.files[0];
  if (!file || !file.name.endsWith('.txt')) { alert('请导入 TXT 文件'); return; }
  const reader = new FileReader();
  reader.onload = ev => {
    const cards = parseTxt(ev.target.result);
    if (!cards.length) { alert('未解析到有效词条'); return; }
    const name = file.name.replace(/\.txt$/i, '');
    importBank(name, cards);
    alert(`成功导入「${name}」(${cards.length} 个单词)`);
    populateBankSelect();
  };
  reader.readAsText(file);
});

// Bank list action delegation
dom.bankList.addEventListener('click', e => {
  const btn = e.target.closest('button');
  const infoDiv = e.target.closest('.info');
  const card = e.target.closest('.bank-card');
  if (!card) return;
  const name = card.dataset.bank;

  // Click on info area → show word list (only for real banks, not wrong book)
  if (!btn && infoDiv && name !== '__wrongbook__' && name !== '__favorites__') {
    showWordList(name);
    return;
  }
  // Not clicking a button and not an info area click — ignore
  if (!btn) return;

  // Wrong book card has a special bank name
  if (name === '__wrongbook__') {
    if (btn.classList.contains('btn-study')) { startWrongBookLearn(); }
    if (btn.classList.contains('btn-export')) { exportWrongBook(); }
    if (btn.classList.contains('btn-del')) {
      if (confirm('清空错题本？此操作不可撤销。')) { DB.setWrongBook([]); renderBanks(); }
    }
    return;
  }

  // Favorites card
  if (name === '__favorites__') {
    if (btn.classList.contains('btn-study')) { startFavoritesLearn(); }
    if (btn.classList.contains('btn-export')) { exportFavorites(); }
    if (btn.classList.contains('btn-del')) {
      if (confirm('清空收藏夹？')) { DB.setFavorites([]); renderBanks(); }
    }
    return;
  }

  if (btn.classList.contains('btn-study')) { startLearn(name); }
  if (btn.classList.contains('btn-export')) { exportBank(name); }
  if (btn.classList.contains('btn-del')) {
    if (confirm(`删除词库「${name}」？`)) { deleteBank(name); }
  }
});

// Flashcard & SRS card flip
dom.cardContainer.addEventListener('click', e => {
  if (e.target.closest('.star-btn')) return;
  if (!learnState || learnState.answered) return;
  if (learnState.mode === 'srs') {
    flipSrsCard();
  } else {
    flipCard();
  }
});
dom.btnForgot.addEventListener('click', () => answerCard(false));
dom.btnRemember.addEventListener('click', () => answerCard(true));
dom.cardStar.addEventListener('click', () => {
  if (learnState) handleStarClick(dom.cardStar, learnState.cards[learnState.index]);
});

// SRS
dom.srsActions.addEventListener('click', e => {
  const btn = e.target.closest('.srs-btn');
  if (!btn) return;
  answerSrsCard(parseInt(btn.dataset.rating));
});

// Quiz
dom.quizOptions.addEventListener('click', e => {
  const opt = e.target.closest('.quiz-option');
  if (!opt || opt.classList.contains('disabled')) return;
  handleQuizAnswer(opt);
});
dom.quizStar.addEventListener('click', () => {
  if (learnState) handleStarClick(dom.quizStar, learnState.cards[learnState.index]);
});

// Typing
dom.typingInput.addEventListener('keydown', e => {
  if (e.key === 'Enter') {
    e.preventDefault();
    e.stopPropagation(); // prevent document handler from also advancing on the same keypress
    if (learnState && !learnState.answered) handleTypingSubmit();
  }
});
dom.btnTypingNext.addEventListener('click', handleTypingNext);
dom.typingStar.addEventListener('click', () => {
  if (learnState) handleStarClick(dom.typingStar, learnState.cards[learnState.index]);
});

// Session controls
dom.btnStart.addEventListener('click', beginSession);
dom.btnResume.addEventListener('click', resumeSession);
dom.btnClearSaved.addEventListener('click', () => {
  if (confirm('放弃已保存的进度？')) clearSavedSession();
});
dom.btnPause.addEventListener('click', pauseSession);
dom.btnContinue.addEventListener('click', continueSession);
dom.btnSaveQuit.addEventListener('click', saveAndQuit);
dom.btnQuit.addEventListener('click', quitSession);
dom.btnAgain.addEventListener('click', () => {
  dom.learnResult.classList.remove('active');
  if (lastSessionWasWrongBook) startWrongBookLearn();
  else if (lastSessionWasFavorites) startFavoritesLearn();
  else beginSession();
});
dom.btnBackToSetup.addEventListener('click', () => {
  dom.learnResult.classList.remove('active');
  dom.learnSetup.style.display = 'block';
});

// Keyboard shortcuts
document.addEventListener('keydown', e => {
  if (dom.pauseOverlay.classList.contains('active')) {
    if (e.key === 'Escape' || e.key === ' ') { e.preventDefault(); continueSession(); }
    return;
  }
  if (!learnState) return;

  // Typing mode: Enter submits / advances (input handler covers submit; here covers advance)
  if (learnState.mode === 'typing') {
    if (learnState.answered && e.key === 'Enter') {
      e.preventDefault();
      handleTypingNext();
    }
    return;
  }

  if (learnState.mode === 'quiz') {
    if (!learnState.answered) {
      const opts = document.querySelectorAll('.quiz-option:not(.disabled)');
      const num = parseInt(e.key);
      if (num >= 1 && num <= opts.length) { opts[num - 1].click(); }
    }
    return;
  }

  // SRS mode
  if (learnState.mode === 'srs') {
    if (!learnState.answered) {
      if (e.key === ' ' || e.key === 'Enter') { e.preventDefault(); flipSrsCard(); }
    } else {
      const rating = parseInt(e.key);
      if (!isNaN(rating) && rating >= 0 && rating <= 5) { e.preventDefault(); answerSrsCard(rating); }
    }
    return;
  }

  // Flashcard mode
  if (!learnState.answered) {
    if (e.key === ' ' || e.key === 'Enter') { e.preventDefault(); flipCard(); }
  } else {
    if (e.key === '1' || e.key === 'f') answerCard(false);
    if (e.key === '2' || e.key === 'j') answerCard(true);
  }
});

// ============ Init ============
applyTheme(DB.getTheme());
updateGoalUI();
renderBanks();
populateBankSelect();
checkSavedSession();
renderHistory();
updateStatsSummary();
