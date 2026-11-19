-- Removes everything added by add-demo-data.sql and nothing else.
-- Real records are left alone: anything you entered yourself does not match these markers.
-- The fee structure (Rs 500 / 600 tuition, Rs 600 annual fee) came from your sheet, so it is kept.
\set ON_ERROR_STOP on
BEGIN;

CREATE TEMP TABLE removed AS
SELECT 'students' AS what, count(*) AS n FROM student WHERE admission_no LIKE 'DEMO-%'
UNION ALL SELECT 'fee payments', count(*) FROM fee_payment WHERE student_id IN (SELECT id FROM student WHERE admission_no LIKE 'DEMO-%')
UNION ALL SELECT 'demo salary records', count(*) FROM salary_record WHERE remarks = 'demo-data'
UNION ALL SELECT 'demo expenses', count(*) FROM expense WHERE created_by = 'demo-data'
UNION ALL SELECT 'demo enquiries', count(*) FROM enquiry WHERE phone LIKE '90000%'
UNION ALL SELECT 'sample announcements', count(*) FROM announcement WHERE title LIKE '[Sample]%'
UNION ALL SELECT 'sample management members', count(*) FROM management_member WHERE name LIKE 'Sample %'
UNION ALL SELECT 'sample gallery albums', count(*) FROM gallery_album WHERE title LIKE '[Sample]%';

DELETE FROM fee_payment WHERE student_id IN (SELECT id FROM student WHERE admission_no LIKE 'DEMO-%');
DELETE FROM student_document WHERE student_id IN (SELECT id FROM student WHERE admission_no LIKE 'DEMO-%');
DELETE FROM student WHERE admission_no LIKE 'DEMO-%';
DELETE FROM salary_record WHERE remarks = 'demo-data';
DELETE FROM expense WHERE created_by = 'demo-data';
DELETE FROM enquiry WHERE phone LIKE '90000%';
DELETE FROM announcement WHERE title LIKE '[Sample]%';
DELETE FROM management_member WHERE name LIKE 'Sample %';
DELETE FROM gallery_photo WHERE album_id IN (SELECT id FROM gallery_album WHERE title LIKE '[Sample]%');
DELETE FROM gallery_album WHERE title LIKE '[Sample]%';
-- the sample subjects and classes on the placeholder teachers (real teacher details are never touched)
UPDATE teacher SET subjects = NULL, classes_assigned = NULL, qualification = NULL, joining_date = NULL WHERE full_name LIKE 'Teacher %';

-- the first real receipt should be number 000001 again, but only if no receipts are left
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM fee_payment) THEN ALTER SEQUENCE receipt_seq RESTART WITH 1; END IF;
END $$;

INSERT INTO audit_log (user_email, action, entity_type, summary)
SELECT 'data-import', 'DELETE', 'Demo data', 'Removed ' || n || ' ' || what FROM removed WHERE n > 0;

SELECT what, n AS removed FROM removed ORDER BY what;
COMMIT;
