-- Run once in the SQL editor before enabling the Supabase backend.
-- Default table: player_stats. If renamed, rename every table/function below consistently.
create table if not exists public.player_stats (
    uuid text primary key,
    wins integer not null default 0 check (wins >= 0),
    losses integer not null default 0 check (losses >= 0)
);
create table if not exists public.player_stats_events (
    event_id uuid primary key,
    player_id text not null,
    win boolean not null
);
alter table public.player_stats enable row level security;
alter table public.player_stats_events enable row level security;
revoke all on public.player_stats, public.player_stats_events from anon, authenticated;
-- This server-only compatibility setup uses service_role. Keep its credential private.
-- A dedicated authenticated server role with narrowly scoped policies is preferable.
grant select, insert, update on public.player_stats to service_role;
grant select, insert on public.player_stats_events to service_role;
create or replace function public.player_stats_record_result(p_event_id uuid, p_player_id text, p_win boolean)
returns void language plpgsql security invoker set search_path = '' as $$
declare inserted integer;
begin
    insert into public.player_stats_events(event_id, player_id, win)
    values (p_event_id, p_player_id, p_win) on conflict (event_id) do nothing;
    get diagnostics inserted = row_count;
    if inserted = 0 then
        if not exists (select 1 from public.player_stats_events
                       where event_id = p_event_id and player_id = p_player_id and win = p_win) then
            raise exception 'Result event identity conflict';
        end if;
        return;
    end if;
    insert into public.player_stats as stats(uuid, wins, losses)
    values (p_player_id, case when p_win then 1 else 0 end, case when p_win then 0 else 1 end)
    on conflict (uuid) do update set wins = stats.wins + excluded.wins,
                                    losses = stats.losses + excluded.losses;
end;
$$;
revoke all on function public.player_stats_record_result(uuid, text, boolean) from public, anon, authenticated;
grant execute on function public.player_stats_record_result(uuid, text, boolean) to service_role;
