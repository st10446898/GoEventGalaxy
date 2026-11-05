-- Migration 11: Push notifications device tokens and notification preferences

-- 1. Device tokens table for FCM registration tokens
create table if not exists public.device_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id text not null,
    token text not null unique,
    platform text not null default 'android',
    updated_at timestamptz not null default now()
);

-- 2. Notification preferences table for user opt-in/opt-out switches
create table if not exists public.notification_preferences (
    user_id text primary key,
    events_enabled boolean not null default true,
    rewards_enabled boolean not null default true,
    bookings_enabled boolean not null default true,
    updated_at timestamptz not null default now()
);

-- 3. Enable Row Level Security (RLS) on both tables
alter table public.device_tokens enable row level security;
alter table public.notification_preferences enable row level security;

-- RLS policies for device_tokens (user_id = public.current_uid())
create policy "Users can select own device tokens"
    on public.device_tokens for select
    to authenticated
    using (user_id = public.current_uid());

create policy "Users can insert own device tokens"
    on public.device_tokens for insert
    to authenticated
    with check (user_id = public.current_uid());

create policy "Users can update own device tokens"
    on public.device_tokens for update
    to authenticated
    using (user_id = public.current_uid())
    with check (user_id = public.current_uid());

create policy "Users can delete own device tokens"
    on public.device_tokens for delete
    to authenticated
    using (user_id = public.current_uid());

-- RLS policies for notification_preferences (user_id = public.current_uid())
create policy "Users can select own notification preferences"
    on public.notification_preferences for select
    to authenticated
    using (user_id = public.current_uid());

create policy "Users can insert own notification preferences"
    on public.notification_preferences for insert
    to authenticated
    with check (user_id = public.current_uid());

create policy "Users can update own notification preferences"
    on public.notification_preferences for update
    to authenticated
    using (user_id = public.current_uid())
    with check (user_id = public.current_uid());

create policy "Users can delete own notification preferences"
    on public.notification_preferences for delete
    to authenticated
    using (user_id = public.current_uid());

-- 4. Grant privileges to authenticated role only, revoke from anon
revoke all on public.device_tokens from anon;
grant select, insert, update, delete on public.device_tokens to authenticated;

revoke all on public.notification_preferences from anon;
grant select, insert, update, delete on public.notification_preferences to authenticated;

-- 5. Ensure tables are not in any Realtime publication (protects device tokens)
alter publication supabase_realtime drop table if exists public.device_tokens;
alter publication supabase_realtime drop table if exists public.notification_preferences;
