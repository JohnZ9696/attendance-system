-- Attendance System schema (Supabase / PostgreSQL)

create table if not exists system_settings (
  key        text primary key,
  value      text not null,
  updated_at timestamptz not null default now()
);

create table if not exists students (
  id                  uuid primary key default gen_random_uuid(),
  mssv                text unique not null,
  full_name           text not null,
  uid                 text unique,
  face_embedding      jsonb,
  face_model          text,
  embedding_dimension integer,
  is_active           boolean not null default true,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now()
);

create table if not exists verification_logs (
  id                 uuid primary key default gen_random_uuid(),
  student_id         uuid references students(id),
  scanned_uid        text not null,
  similarity_percent numeric(5,2),
  liveness_passed    boolean,
  result             text not null check (result in ('PENDING','VERIFIED','RFID_INVALID','CAMERA_OFFLINE','CAPTURE_TIMEOUT','LIVENESS_FAILED','FACE_BELOW_THRESHOLD','FACE_MATCH_TIMEOUT','ERROR')),
  failure_reason     text,
  model_name         text,
  model_version      text,
  notification_sent  boolean not null default false,
  notification_error text,
  started_at         timestamptz not null default now(),
  completed_at       timestamptz
);

create table if not exists attendance_logs (
  id              uuid primary key default gen_random_uuid(),
  student_id      uuid not null references students(id),
  verification_id uuid unique references verification_logs(id),
  attendance_date date not null,
  check_time      timestamptz not null,
  status          text not null check (status in ('ON_TIME','LATE')),
  late_minutes    integer,
  unique (student_id, attendance_date)
);

create table if not exists assistance_requests (
  id                 uuid primary key default gen_random_uuid(),
  student_id         uuid references students(id),
  source             text check (source in ('PUSH_BUTTON','WEB')),
  message            text not null,
  status             text not null check (status in ('OPEN','ACKNOWLEDGED','RESOLVED')),
  notification_sent  boolean not null default false,
  notification_error text,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now(),
  resolved_at        timestamptz
);

insert into system_settings (key, value)
values ('attendance.classStartTime', '07:30'),
       ('attendance.lateGraceMinutes', '15')
on conflict (key) do nothing;