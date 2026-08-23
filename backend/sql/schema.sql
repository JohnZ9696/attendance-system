-- Attendance System schema (Supabase / PostgreSQL)
-- Run this in the Supabase SQL Editor.

create table if not exists students (
  id           uuid primary key default gen_random_uuid(),
  mssv         text unique not null,
  full_name    text not null,
  uid          text unique,
  face_embedding jsonb,
  created_at   timestamptz not null default now()
);

create table if not exists attendance_logs (
  id         uuid primary key default gen_random_uuid(),
  student_id uuid not null references students(id),
  check_time timestamptz not null default now(),
  method     text not null check (method in ('RFID', 'FACE')),
  status     text not null check (status in ('IN', 'OUT', 'LATE'))
);

create table if not exists assistance_requests (
  id         uuid primary key default gen_random_uuid(),
  student_id uuid references students(id),
  message    text not null,
  created_at timestamptz not null default now()
);

create table if not exists system_settings (
  key        text primary key,
  value      text not null,
  updated_at timestamptz not null default now()
);

insert into system_settings (key, value)
values ('attendance.classStartTime', '07:30'),
       ('attendance.lateGraceMinutes', '15')
on conflict (key) do nothing;

-- Optional indexes for common queries
create index if not exists idx_attendance_student on attendance_logs (student_id);
create index if not exists idx_attendance_check_time on attendance_logs (check_time);
create index if not exists idx_assistance_student on assistance_requests (student_id);
