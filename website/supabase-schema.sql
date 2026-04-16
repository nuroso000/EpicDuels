-- ═══════════════════════════════════════════════════════════════
--  EpicDuels – Supabase Schema
--
--  Ausführen im Supabase Dashboard:
--  Project → SQL Editor → New Query → Paste → Run
-- ═══════════════════════════════════════════════════════════════

-- ── Tabelle ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS player_stats (
    player_uuid   TEXT        PRIMARY KEY,   -- Minecraft UUID (mit oder ohne Bindestriche)
    wins          INTEGER     NOT NULL DEFAULT 0,
    losses        INTEGER     NOT NULL DEFAULT 0,
    score         INTEGER     NOT NULL DEFAULT 0,   -- Punkte / ELO
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Auto-Update Timestamp ───────────────────────────────────────
CREATE OR REPLACE FUNCTION _update_timestamp()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_player_stats_updated ON player_stats;
CREATE TRIGGER trg_player_stats_updated
    BEFORE UPDATE ON player_stats
    FOR EACH ROW EXECUTE FUNCTION _update_timestamp();

-- ── Row Level Security ──────────────────────────────────────────
ALTER TABLE player_stats ENABLE ROW LEVEL SECURITY;

-- Website darf lesen (anon key)
CREATE POLICY "public_read"
    ON player_stats FOR SELECT
    TO anon
    USING (true);

-- Nur das Plugin (service_role) darf schreiben
-- (Der service_role Key umgeht RLS automatisch)

-- ── Index für schnelle Score-Sortierung ────────────────────────
CREATE INDEX IF NOT EXISTS idx_player_stats_score
    ON player_stats (score DESC, wins DESC);

-- ══════════════════════════════════════════════════════════════
--  Plugin-Integration (Pseudocode – Java JDBC / HTTP)
--
--  Das Plugin nutzt den Supabase service_role Key (NIEMALS den
--  anon Key im Plugin verwenden) und ruft nach jedem Duel auf:
--
--  UPSERT:
--    INSERT INTO player_stats (player_uuid, wins, losses, score)
--    VALUES (?, ?, ?, ?)
--    ON CONFLICT (player_uuid) DO UPDATE SET
--        wins   = EXCLUDED.wins,
--        losses = EXCLUDED.losses,
--        score  = EXCLUDED.score;
--
--  Oder via Supabase REST API:
--    POST /rest/v1/player_stats
--    Prefer: resolution=merge-duplicates
--    Body: { "player_uuid": "...", "wins": 5, "losses": 2, "score": 42 }
-- ══════════════════════════════════════════════════════════════
