/* ═══════════════════════════════════════════════
   EpicDuels Leaderboard — app.js
   Fetches player_stats from Supabase, resolves
   Minecraft names via playerdb.co (CORS-safe),
   and renders an MCTiers-style leaderboard.
═══════════════════════════════════════════════ */

// ── Supabase client ──────────────────────────
const { createClient } = supabase;
const db = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ── Name / skin cache (localStorage, 24 h) ───
const CACHE_KEY = 'epicduels_mc_cache_v1';
const CACHE_TTL = 24 * 60 * 60 * 1000;

function readCache() {
    try { return JSON.parse(localStorage.getItem(CACHE_KEY) || '{}'); }
    catch { return {}; }
}

function writeCache(cache) {
    try { localStorage.setItem(CACHE_KEY, JSON.stringify(cache)); }
    catch { /* quota exceeded – ignore */ }
}

// Strip dashes from UUID for skin APIs
function rawUuid(uuid) { return uuid.replace(/-/g, ''); }

// Build head image URL (crafatar with overlay)
function headUrl(uuid) {
    return `https://crafatar.com/avatars/${rawUuid(uuid)}?size=48&overlay=true&default=MHF_Steve`;
}

// Fetch a single player name from playerdb.co with cache
async function fetchName(uuid) {
    const cache = readCache();
    const entry = cache[uuid];
    if (entry && (Date.now() - entry.ts) < CACHE_TTL) return entry.name;

    try {
        const r = await fetch(`https://playerdb.co/api/player/minecraft/${uuid}`, { cache: 'force-cache' });
        if (r.ok) {
            const json = await r.json();
            if (json.success && json.data?.player?.username) {
                const name = json.data.player.username;
                const updated = readCache();
                updated[uuid] = { name, ts: Date.now() };
                writeCache(updated);
                return name;
            }
        }
    } catch { /* network error */ }

    // Fallback: shorten UUID
    return uuid.substring(0, 8) + '…';
}

// Fetch all names with limited concurrency (avoids rate limits)
async function fetchAllNames(uuids, concurrency = 4) {
    const results = new Array(uuids.length).fill('');
    let next = 0;

    async function worker() {
        while (next < uuids.length) {
            const i = next++;
            results[i] = await fetchName(uuids[i]);
        }
    }

    const workers = Array.from({ length: Math.min(concurrency, uuids.length) }, worker);
    await Promise.all(workers);
    return results;
}

// ── Rank tier label from score ────────────────
function rankTier(score) {
    if (score >= 500) return { label: '✦ Legend',  cls: 'tier-legend'  };
    if (score >= 300) return { label: '✦ Master',  cls: 'tier-master'  };
    if (score >= 150) return { label: '◆ Diamond', cls: 'tier-diamond' };
    if (score >= 75)  return { label: '★ Gold',    cls: 'tier-gold'    };
    if (score >= 25)  return { label: '◈ Silver',  cls: 'tier-silver'  };
    if (score >= 1)   return { label: '◉ Bronze',  cls: 'tier-bronze'  };
    return               { label: 'Rookie',         cls: 'tier-rookie'  };
}

// ── Medal badge HTML ──────────────────────────
function rankBadge(pos) {
    if (pos === 1) return `<div class="rank-badge rn1">🥇</div>`;
    if (pos === 2) return `<div class="rank-badge rn2">🥈</div>`;
    if (pos === 3) return `<div class="rank-badge rn3">🥉</div>`;
    return `<div class="rank-badge rnx">${pos}</div>`;
}

// ── Win-rate helpers ──────────────────────────
function winRate(wins, losses) {
    const total = wins + losses;
    if (total === 0) return { pct: 0, str: '—' };
    const pct = Math.round((wins / total) * 100);
    return { pct, str: `${pct}%` };
}

function wrClass(pct) {
    if (pct >= 60) return 'col-wr good';
    if (pct >= 40) return 'col-wr mid';
    return 'col-wr';
}

// ── XSS-safe text ─────────────────────────────
function esc(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

// ── Format numbers ────────────────────────────
function fmt(n) { return Number(n).toLocaleString('de-DE'); }

// ── Render a skeleton row while names load ────
function skeletonRow(pos) {
    return `
    <div class="lb-row ${pos <= 3 ? 'r' + pos : ''}">
        <div class="rank-col">${rankBadge(pos)}</div>
        <div class="player-cell">
            <div class="player-head skel" style="width:44px;height:44px;"></div>
            <div class="player-info">
                <div class="skel" style="width:120px;height:14px;margin-bottom:5px;"></div>
                <div class="skel" style="width:70px;height:11px;"></div>
            </div>
        </div>
        <div class="col-wins skel" style="width:40px;height:13px;margin-left:auto;"></div>
        <div class="col-losses skel" style="width:35px;height:13px;margin-left:auto;"></div>
        <div class="col-wr skel" style="width:38px;height:13px;margin-left:auto;"></div>
        <div class="col-score skel" style="width:48px;height:14px;margin-left:auto;"></div>
    </div>`;
}

// ── Render final leaderboard row ──────────────
function buildRow(player, pos, name) {
    const wins   = player.wins   ?? 0;
    const losses = player.losses ?? 0;
    const score  = player.score  ?? wins * 10;   // graceful fallback if no score column
    const { pct, str: wrStr } = winRate(wins, losses);
    const tier   = rankTier(score);
    const rowCls = pos <= 3 ? `lb-row r${pos}` : 'lb-row';
    const uuid   = player.player_uuid;

    return `
    <div class="${rowCls}">
        <div class="rank-col">${rankBadge(pos)}</div>
        <div class="player-cell">
            <img class="player-head"
                 src="${headUrl(uuid)}"
                 alt="${esc(name)}"
                 loading="lazy"
                 onerror="this.onerror=null;this.src='https://mc-heads.net/avatar/steve/48'">
            <div class="player-info">
                <div class="player-name">${esc(name)}</div>
                <div class="player-tier ${tier.cls}">${tier.label}  ·  ${fmt(score)} Pkt.</div>
            </div>
        </div>
        <div class="col-wins">${fmt(wins)}</div>
        <div class="col-losses">${fmt(losses)}</div>
        <div class="${wrClass(pct)}">${wrStr}</div>
        <div class="col-score">${fmt(score)}</div>
    </div>`;
}

// ── Podium (top-3 cards above table) ─────────
function buildPodium(top3, names) {
    const medals = ['🥇', '🥈', '🥉'];
    const classes = ['p1', 'p2', 'p3'];
    return top3.map((p, i) => {
        const wins   = p.wins   ?? 0;
        const losses = p.losses ?? 0;
        const score  = p.score  ?? wins * 10;
        const name   = names[i];
        return `
        <div class="podium-card ${classes[i]}">
            <div class="podium-medal">${medals[i]}</div>
            <img class="podium-head"
                 src="${headUrl(p.player_uuid)}"
                 alt="${esc(name)}"
                 loading="lazy"
                 onerror="this.onerror=null;this.src='https://mc-heads.net/avatar/steve/48'">
            <div class="podium-name">${esc(name)}</div>
            <div class="podium-stats">
                <div class="podium-stat">
                    <strong>${fmt(score)}</strong>
                    Score
                </div>
                <div class="podium-stat">
                    <strong>${fmt(wins)}</strong>
                    Wins
                </div>
                <div class="podium-stat">
                    <strong>${fmt(losses)}</strong>
                    Losses
                </div>
            </div>
        </div>`;
    }).join('');
}

// ── Main load function ────────────────────────
async function loadLeaderboard() {
    const elLoading = document.getElementById('st-loading');
    const elError   = document.getElementById('st-error');
    const elEmpty   = document.getElementById('st-empty');
    const elRows    = document.getElementById('lb-rows');
    const elPodium  = document.getElementById('podium');
    const elUpdated = document.getElementById('last-updated');

    // Reset state
    elLoading.hidden = false;
    elError.hidden   = true;
    elEmpty.hidden   = true;
    elRows.innerHTML = '';
    elPodium.hidden  = true;
    elPodium.innerHTML = '';

    try {
        // ── 1. Fetch data from Supabase ──────────
        const { data, error } = await db
            .from(TABLE_NAME)
            .select('player_uuid, wins, losses, score')
            .order('score',  { ascending: false, nullsFirst: false })
            .order('wins',   { ascending: false })
            .limit(100);

        if (error) throw error;

        elLoading.hidden = true;

        if (!data || data.length === 0) {
            elEmpty.hidden = false;
            return;
        }

        // ── 2. Show skeletons immediately ────────
        elRows.innerHTML = data.map((_, i) => skeletonRow(i + 1)).join('');

        // ── 3. Resolve names (concurrently, cached) ──
        const uuids = data.map(p => p.player_uuid);
        const names = await fetchAllNames(uuids);

        // ── 4. Render final rows ─────────────────
        elRows.innerHTML = data.map((p, i) => buildRow(p, i + 1, names[i])).join('');

        // ── 5. Podium for top-3 ──────────────────
        if (data.length >= 3) {
            elPodium.innerHTML = buildPodium(data.slice(0, 3), names.slice(0, 3));
            elPodium.hidden = false;
        }

        // ── 6. Last-updated timestamp ────────────
        elUpdated.textContent = 'Stand: ' + new Date().toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });

    } catch (err) {
        console.error('[EpicDuels LB]', err);
        elLoading.hidden = true;
        document.getElementById('err-text').textContent =
            err.message?.includes('fetch')
                ? 'Supabase nicht erreichbar. Prüfe deine config.js.'
                : `Fehler: ${err.message}`;
        elError.hidden = false;
    }
}

// ── Boot ──────────────────────────────────────
loadLeaderboard();

// Auto-refresh every 5 minutes
setInterval(loadLeaderboard, 5 * 60 * 1000);
