# Domain and Data Model

System only requires 5 tables. Supabase PostgreSQL is used.

## 1. system_settings
```sql
create table public.system_settings (
  key text not null,
  value text not null,
  updated_at timestamptz not null default now(),
  constraint system_settings_pkey primary key (key)
);
```

## 2. students
```sql
create table public.students (
  id uuid not null default gen_random_uuid(),
  mssv text not null,
  full_name text not null,
  uid text null,
  face_embedding jsonb null,
  face_model text null,
  embedding_dimension integer null,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint students_pkey primary key (id),
  constraint students_mssv_key unique (mssv),
  constraint students_uid_key unique (uid),
  constraint students_embedding_check check (
    case
      when face_embedding is null then
        face_model is null and embedding_dimension is null
      when jsonb_typeof(face_embedding) <> 'array' then
        false
      else
        face_model is not null
        and embedding_dimension > 0
        and jsonb_array_length(face_embedding) = embedding_dimension
    end
  )
);
```

## 3. verification_logs
```sql
create table public.verification_logs (
  id uuid not null default gen_random_uuid(),
  student_id uuid null,
  scanned_uid text not null,
  similarity_percent numeric(5,2) null,
  liveness_passed boolean null,
  result text not null default 'PENDING',
  failure_reason text null,
  model_name text null,
  model_version text null,
  notification_sent boolean not null default false,
  notification_error text null,
  started_at timestamptz not null default now(),
  completed_at timestamptz null,

  constraint verification_logs_pkey primary key (id),
  constraint verification_logs_student_fkey
    foreign key (student_id) references public.students (id),
  constraint verification_logs_similarity_check check (
    similarity_percent is null
    or similarity_percent between 0 and 100
  ),
  constraint verification_logs_result_check check (
    result in (
      'PENDING', 'VERIFIED', 'RFID_INVALID', 'CAMERA_OFFLINE',
      'CAPTURE_TIMEOUT', 'LIVENESS_FAILED', 'FACE_BELOW_THRESHOLD',
      'FACE_MATCH_TIMEOUT', 'ERROR'
    )
  ),
  constraint verification_logs_completed_check check (
    completed_at is null or completed_at >= started_at
  ),
  constraint verification_logs_verified_check check (
    result <> 'VERIFIED'
    or (
      student_id is not null
      and similarity_percent is not null
      and liveness_passed is true
    )
  )
);
```

## 4. attendance_logs
```sql
create table public.attendance_logs (
  id uuid not null default gen_random_uuid(),
  student_id uuid not null,
  verification_id uuid not null,
  attendance_date date not null,
  check_time timestamptz not null default now(),
  status text not null,
  late_minutes integer not null default 0,

  constraint attendance_logs_pkey primary key (id),
  constraint attendance_logs_student_fkey
    foreign key (student_id) references public.students (id),
  constraint attendance_logs_verification_fkey
    foreign key (verification_id) references public.verification_logs (id),
  constraint attendance_logs_verification_key unique (verification_id),
  constraint attendance_logs_one_per_day_key
    unique (student_id, attendance_date),
  constraint attendance_logs_status_check
    check (status in ('ON_TIME', 'LATE')),
  constraint attendance_logs_late_minutes_check
    check (
      (status = 'ON_TIME' and late_minutes = 0)
      or
      (status = 'LATE' and late_minutes >= 1)
    )
);
```

## 5. assistance_requests
```sql
create table public.assistance_requests (
  id uuid not null default gen_random_uuid(),
  student_id uuid null,
  source text not null,
  message text not null,
  status text not null default 'OPEN',
  notification_sent boolean not null default false,
  notification_error text null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  resolved_at timestamptz null,

  constraint assistance_requests_pkey primary key (id),
  constraint assistance_requests_student_fkey
    foreign key (student_id) references public.students (id),
  constraint assistance_requests_source_check
    check (source in ('PUSH_BUTTON', 'WEB')),
  constraint assistance_requests_status_check
    check (status in ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
  constraint assistance_requests_message_check
    check (length(btrim(message)) between 1 and 500),
  constraint assistance_requests_resolved_check
    check (status <> 'RESOLVED' or resolved_at is not null)
);
```

## 6. Required Indexes
```sql
create index if not exists idx_verification_student_time on public.verification_logs (student_id, started_at desc);
create index if not exists idx_verification_result_time on public.verification_logs (result, started_at desc);
create index if not exists idx_attendance_date on public.attendance_logs (attendance_date desc);
create index if not exists idx_attendance_status_date on public.attendance_logs (status, attendance_date desc);
create index if not exists idx_assistance_status_time on public.assistance_requests (status, created_at desc);
```

## 7. Database Cleanup

```sql
-- Backup the unused legacy attendance table
CREATE TABLE public.attendance_logs_legacy_backup AS
SELECT * FROM public.attendance_logs_legacy;

-- Drop the original legacy table as it is not referenced in the codebase
DROP TABLE public.attendance_logs_legacy;
```
