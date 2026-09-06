-- Run this in Supabase SQL Editor

DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN
    SELECT c.conname,
           c.conrelid::regclass AS src_table,
           pg_attribute.attname AS src_column
    FROM pg_constraint c
    JOIN pg_attribute ON pg_attribute.attrelid = c.conrelid
                     AND pg_attribute.attnum = ANY(c.conkey)
    WHERE c.confrelid = 'students'::regclass
      AND c.contype = 'f'
  LOOP
    EXECUTE format(
      'ALTER TABLE %s DROP CONSTRAINT IF EXISTS %I',
      r.src_table, r.conname
    );
    EXECUTE format(
      'ALTER TABLE %s ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES students(id) ON DELETE CASCADE',
      r.src_table, r.conname, r.src_column
    );
    RAISE NOTICE 'OK: %.% (%I) -> CASCADE', r.src_table, r.src_column, r.conname;
  END LOOP;
END $$;

ALTER TABLE attendance_logs
  DROP CONSTRAINT IF EXISTS attendance_logs_verification_id_fkey,
  ADD CONSTRAINT attendance_logs_verification_id_fkey
    FOREIGN KEY (verification_id) REFERENCES verification_logs(id) ON DELETE CASCADE;
