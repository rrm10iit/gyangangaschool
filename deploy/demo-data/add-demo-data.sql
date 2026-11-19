-- Demonstration data for the admin panel, modelled on "Gyan Ganga Finance Nov 2026.xlsx".
-- Every row can be removed again with remove-demo-data.sql (they are marked: admission no "DEMO-...", phone numbers
-- "90000xxxxx", collected_by / created_by "demo-data", salary remarks "demo-data", titles "[Sample] ...").
--
-- Numbers taken from the sheet (Nursery to Class 8, 159 students):
--   Tuition per month: Rs 500 (Nursery to Class 5) and Rs 600 (Class 6 to 8). Some students pay Rs 300 (7 in Nursery group)
--   and some pay nothing (free seats), exactly as in the "Monthly Income" sheet.
--   Annual / admission fee: Rs 600 per student per year (159 x 600 in the sheet).
--   Monthly tuition collected when everyone is current = Rs 71,700 (24,600 + 5,000 + 7,500 + 5,000 + 5,000 + 6,000 + 6,600 + 5,400 + 6,600).
-- Nursery 55 in the sheet is split here into Nursery 25, LKG 18, UKG 12.
\set ON_ERROR_STOP on
BEGIN;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM student) OR EXISTS (SELECT 1 FROM fee_payment) OR EXISTS (SELECT 1 FROM fee_structure) THEN
    RAISE EXCEPTION 'Students, payments or a fee structure already exist. Aborting so nothing real is mixed up.';
  END IF;
  IF (SELECT count(*) FROM school_class) <> 11 THEN
    RAISE EXCEPTION 'Expected the 11 standard classes (Nursery to Class 8). Aborting.';
  END IF;
END $$;

SELECT setseed(0.42) AS "fixed random seed (same result every run)";

-- ------------------------------------------------------------------ plan from the sheet
CREATE TEMP TABLE plan (class_name text, n int, rate int, free_n int, low_n int, age int);
INSERT INTO plan VALUES
  ('Nursery', 25, 500, 1, 3, 3),
  ('LKG',     18, 500, 1, 2, 4),
  ('UKG',     12, 500, 1, 2, 5),
  ('Class 1', 12, 500, 2, 0, 6),
  ('Class 2', 15, 500, 0, 0, 7),
  ('Class 3', 15, 500, 5, 0, 8),
  ('Class 4', 12, 500, 2, 0, 9),
  ('Class 5', 14, 500, 2, 0, 10),
  ('Class 6', 13, 600, 2, 0, 11),
  ('Class 7', 10, 600, 1, 0, 12),
  ('Class 8', 13, 600, 2, 0, 13);

-- ------------------------------------------------------------------ fee structure (this is real, from the sheet)
INSERT INTO fee_structure (class_id, head_name, amount, frequency)
SELECT c.id, 'Tuition fee', p.rate, 'MONTHLY' FROM plan p JOIN school_class c ON c.name = p.class_name;
INSERT INTO fee_structure (class_id, head_name, amount, frequency)
SELECT c.id, 'Admission / annual fee', 600, 'ANNUAL' FROM plan p JOIN school_class c ON c.name = p.class_name;

-- ------------------------------------------------------------------ students
CREATE TEMP TABLE gen AS
SELECT c.id AS class_id, p.class_name, p.rate, p.age, p.free_n, p.low_n, c.display_order, g,
       row_number() OVER (PARTITION BY p.class_name ORDER BY random()) AS r,
       (random() < 0.5) AS is_girl,
       1 + floor(random() * 15)::int AS gi,
       1 + floor(random() * 12)::int AS mo, 1 + floor(random() * 27)::int AS dy
FROM plan p JOIN school_class c ON c.name = p.class_name
CROSS JOIN LATERAL generate_series(1, p.n) AS g;

CREATE TEMP TABLE gen2 AS
SELECT s.*, ((s.seq * 7) % 20) + 1 AS fi, (((s.seq * 7) / 20) % 16) + 1 AS li,
       CASE WHEN s.r <= s.free_n THEN 'FREE' WHEN s.r <= s.free_n + s.low_n THEN 'LOW' ELSE 'NORMAL' END AS tier
FROM (SELECT gen.*, row_number() OVER (ORDER BY display_order, g) AS seq FROM gen) s;

INSERT INTO student (admission_no, full_name, class_id, section, roll_no, date_of_birth, gender, guardian_name,
                     guardian_phone, contact_phone, address, admission_date, fee_discount, status)
SELECT 'DEMO-' || lpad(seq::text, 4, '0'),
       CASE WHEN is_girl
            THEN (ARRAY['Ananya','Diya','Saanvi','Myra','Aadhya','Kiara','Riya','Priya','Anika','Navya','Pari','Ishita','Khushi','Muskan','Pooja','Sakshi','Neha','Shreya','Tanvi','Aisha'])[fi]
            ELSE (ARRAY['Aarav','Vivaan','Aditya','Arjun','Krish','Ishaan','Rohan','Kabir','Ayaan','Dev','Atharv','Reyansh','Shaurya','Yash','Harsh','Aman','Nikhil','Rahul','Sahil','Mohit'])[fi]
       END || ' ' || (ARRAY['Sharma','Verma','Yadav','Mishra','Tiwari','Singh','Pandey','Gupta','Dubey','Maurya','Kesarwani','Patel','Srivastava','Shukla','Pal','Kushwaha'])[li],
       class_id, 'A', g,
       make_date(2026 - age, mo, dy),
       CASE WHEN is_girl THEN 'Female' ELSE 'Male' END,
       (ARRAY['Ramesh','Suresh','Mahesh','Dinesh','Rajesh','Anil','Sunil','Manoj','Vijay','Sanjay','Ajay','Deepak','Ravi','Pramod','Umesh'])[gi]
         || ' ' || (ARRAY['Sharma','Verma','Yadav','Mishra','Tiwari','Singh','Pandey','Gupta','Dubey','Maurya','Kesarwani','Patel','Srivastava','Shukla','Pal','Kushwaha'])[li],
       '90000' || lpad(seq::text, 5, '0'),
       '90000' || lpad(seq::text, 5, '0'),
       'Rasulabad, Teliarganj, Prayagraj',
       make_date(2026 - least(floor(random() * greatest(age - 3, 1))::int, 6), 4, 1 + floor(random() * 15)::int),
       CASE tier WHEN 'FREE' THEN rate * 12 WHEN 'LOW' THEN 2400 ELSE 0 END,
       'ACTIVE'
FROM gen2 ORDER BY seq;

-- ------------------------------------------------------------------ fee payments, April to November 2026
CREATE TEMP TABLE pay_plan AS
SELECT s.id AS student_id, g.tier,
       CASE g.tier WHEN 'FREE' THEN 0 WHEN 'LOW' THEN 300 ELSE g.rate END AS monthly,
       -- about 4 in 10 regular-fee students are one to three months behind (this feeds the overdue list)
       CASE WHEN g.tier = 'NORMAL' AND random() > 0.62 THEN 1 + floor(random() * 3)::int ELSE 0 END AS behind,
       (random() < 0.90) AS pays_annual,
       (random() < 0.80) AS annual_in_april
FROM student s JOIN gen2 g ON g.seq = substring(s.admission_no FROM 6)::int;

CREATE TEMP TABLE pay_rows AS
SELECT pp.student_id, pp.monthly::numeric AS amount,
       least((date '2026-04-01' + make_interval(months => m))::date + floor(random() * 25)::int, current_date) AS pdate,
       'Tuition ' || to_char(date '2026-04-01' + make_interval(months => m), 'Mon') AS for_period,
       random() AS rm
FROM pay_plan pp, generate_series(0, 7) AS m
WHERE pp.monthly > 0 AND m <= 7 - pp.behind
UNION ALL
SELECT pp.student_id, 600::numeric,
       CASE WHEN pp.annual_in_april THEN date '2026-04-03' + floor(random() * 25)::int
            ELSE date '2026-05-02' + floor(random() * 25)::int END,
       'Admission / annual fee', random()
FROM pay_plan pp WHERE pp.pays_annual;

INSERT INTO fee_payment (receipt_no, student_id, amount, payment_date, academic_year, mode, for_period, reference, collected_by)
SELECT 'GGS/2026-27/' || lpad(nextval('receipt_seq')::text, 6, '0'), student_id, amount, pdate, 2026, mode,
       for_period,
       CASE mode WHEN 'UPI' THEN 'UPI' || (100000000000 + floor(random() * 899999999999))::bigint
                 WHEN 'BANK' THEN 'NEFT' || (10000000 + floor(random() * 89999999))::bigint
                 WHEN 'CHEQUE' THEN 'CHQ' || (100000 + floor(random() * 899999))::int END,
       'demo-data'
FROM (SELECT student_id, amount, pdate, for_period,
             CASE WHEN rm < 0.68 THEN 'CASH' WHEN rm < 0.93 THEN 'UPI' WHEN rm < 0.98 THEN 'BANK' ELSE 'CHEQUE' END AS mode
      FROM pay_rows ORDER BY pdate, student_id) x;

-- ------------------------------------------------------------------ teachers: subjects, classes and so on (names stay "Teacher n")
UPDATE teacher t SET subjects = v.subjects, classes_assigned = v.classes, qualification = v.qual, joining_date = v.joined::date
FROM (VALUES
  ('T001', 'English, Hindi',              'Class 6, Class 7, Class 8', 'M.A., B.Ed.',          '2016-04-01'),
  ('T002', 'Mathematics',                 'Class 6, Class 7, Class 8', 'M.Sc., B.Ed.',         '2017-04-01'),
  ('T003', 'Science',                     'Class 6, Class 7, Class 8', 'B.Sc., B.Ed.',         '2018-07-01'),
  ('T004', 'Social Studies, Hindi',       'Class 3, Class 4, Class 5', 'M.A., B.Ed.',          '2019-04-01'),
  ('T005', 'English, EVS',                'Class 3, Class 4, Class 5', 'B.A., B.Ed.',          '2020-04-01'),
  ('T006', 'Mathematics, Science',        'Class 3, Class 4, Class 5', 'B.Sc., B.Ed.',         '2021-04-01'),
  ('T007', 'All subjects',                'Class 1, Class 2',          'B.A., B.T.C.',         '2021-07-01'),
  ('T008', 'All subjects',                'Class 1, Class 2',          'B.A., B.T.C.',         '2022-04-01'),
  ('T009', 'Rhymes, Art and Craft',       'Nursery, LKG, UKG',         'Intermediate, N.T.T.', '2022-04-01'),
  ('T010', 'English, Numbers',            'Nursery, LKG, UKG',         'B.A., N.T.T.',         '2023-04-01'),
  ('T011', 'Hindi, Rhymes',               'Nursery, LKG, UKG',         'Intermediate, N.T.T.', '2024-04-01'),
  ('T012', 'Computer, Physical Education','Class 1 to Class 8',        'BCA',                  '2025-04-01')
) AS v(emp, subjects, classes, qual, joined)
WHERE t.employee_no = v.emp;

-- ------------------------------------------------------------------ salaries for April to October 2026 (paid on the 5th of the next month)
INSERT INTO salary_record (teacher_id, month, base_amount, allowances, deductions, net_amount, status, paid_date, mode, remarks)
SELECT t.id, to_char(m, 'YYYY-MM'), t.monthly_salary, 0, 0, t.monthly_salary, 'PAID',
       least((m::date + interval '1 month' + interval '4 days')::date, current_date),
       CASE WHEN t.monthly_salary >= 5000 THEN 'BANK' ELSE 'CASH' END, 'demo-data'
FROM teacher t, generate_series(date '2026-04-01', date '2026-10-01', interval '1 month') AS m;

-- ------------------------------------------------------------------ expenses for April to October 2026 (same lines as the sheet's November list)
INSERT INTO expense (category, description, amount, expense_date, paid_to, mode, created_by)
SELECT e.category, e.description,
       CASE WHEN e.category = 'Electricity' THEN round((e.amount * (0.8 + random() * 0.4))::numeric, 2) ELSE e.amount END,
       m::date + floor(random() * 25)::int, e.paid_to,
       CASE WHEN e.category = 'Electricity' THEN 'UPI' ELSE 'CASH' END, 'demo-data'
FROM expense e CROSS JOIN generate_series(date '2026-04-01', date '2026-10-01', interval '1 month') AS m
WHERE e.created_by LIKE 'import:%';

INSERT INTO expense (category, description, amount, expense_date, mode, created_by) VALUES
  ('Sports equipment', 'Cricket kit and footballs',        3200, '2026-08-12', 'CASH', 'demo-data'),
  ('Events',           'Independence Day programme',       2400, '2026-08-14', 'CASH', 'demo-data'),
  ('Events',           'Teachers Day celebration',         1800, '2026-09-05', 'CASH', 'demo-data'),
  ('Cleaning',         'Deep cleaning before the session', 2600, '2026-04-02', 'CASH', 'demo-data');

-- ------------------------------------------------------------------ admission enquiries
INSERT INTO enquiry (name, phone, email, class_interested, message, status, created_at, updated_at) VALUES
  ('Sunita Devi',   '9000000101', NULL,                    'Nursery', 'Please tell me the admission process and fees for Nursery.',       'NEW',       now() - interval '1 day',   now() - interval '1 day'),
  ('Rakesh Kumar',  '9000000102', 'rakesh@example.com',    'Class 3', 'Is transport available from Naini side?',                          'NEW',       now() - interval '2 days',  now() - interval '2 days'),
  ('Meena Yadav',   '9000000103', NULL,                    'UKG',     'We want to visit the school on Saturday. What time is suitable?',  'NEW',       now() - interval '4 days',  now() - interval '4 days'),
  ('Anil Tiwari',   '9000000104', 'anil.t@example.com',    'Class 6', 'My son is in Class 5 elsewhere. Is mid-session admission possible?','CONTACTED', now() - interval '8 days',  now() - interval '6 days'),
  ('Pooja Singh',   '9000000105', NULL,                    'LKG',     'Do you have Hindi medium for LKG?',                                'CONTACTED', now() - interval '12 days', now() - interval '10 days'),
  ('Vikas Pandey',  '9000000106', NULL,                    'Class 1', 'Looking for admission for my daughter, age 6.',                    'CLOSED',    now() - interval '20 days', now() - interval '15 days'),
  ('Kavita Gupta',  '9000000107', 'kavita.g@example.com',  'Nursery', 'What documents are needed at the time of admission?',              'CLOSED',    now() - interval '25 days', now() - interval '22 days'),
  ('Sanjay Dubey',  '9000000108', NULL,                    'Class 8', 'Is there a science lab for the upper classes?',                    'CONTACTED', now() - interval '6 days',  now() - interval '5 days');

-- ------------------------------------------------------------------ announcements, management, gallery
INSERT INTO announcement (title, title_hi, body, body_hi, publish_date, pinned, published) VALUES
  ('[Sample] Admissions open for the new session', '[नमूना] नए सत्र के लिए प्रवेश प्रारंभ',
   'Admissions are open from Nursery to Class 8. Please visit the school office or send an enquiry from the Contact page.',
   'नर्सरी से कक्षा 8 तक प्रवेश प्रारंभ हैं। कृपया विद्यालय कार्यालय आएँ या संपर्क पृष्ठ से पूछताछ भेजें।', current_date, true, true),
  ('[Sample] Parent-teacher meeting', '[नमूना] अभिभावक-शिक्षक बैठक',
   'A parent-teacher meeting is held every month. Please check your child''s diary for the date.',
   'हर माह अभिभावक-शिक्षक बैठक होती है। तिथि के लिए कृपया अपने बच्चे की डायरी देखें।', current_date - 7, false, true),
  ('[Sample] Winter timing', '[नमूना] शीतकालीन समय',
   'School timing may change during the winter months. Updates will be shared here.',
   'शीतकाल में विद्यालय का समय बदल सकता है। सूचना यहीं दी जाएगी।', current_date - 14, false, true),
  ('[Sample] Annual day preparations', '[नमूना] वार्षिकोत्सव की तैयारी',
   'Students from all classes are taking part in singing and cultural programmes.',
   'सभी कक्षाओं के विद्यार्थी गायन और सांस्कृतिक कार्यक्रमों में भाग ले रहे हैं।', current_date - 21, false, true);

INSERT INTO management_member (name, designation, designation_hi, display_order, published) VALUES
  ('Sample Chairman',  'Chairman',  'अध्यक्ष',      1, true),
  ('Sample Manager',   'Manager',   'प्रबंधक',      2, true),
  ('Sample Principal', 'Principal', 'प्रधानाचार्य', 3, true);

WITH a AS (
  INSERT INTO gallery_album (title, title_hi, category, event_date, published) VALUES
    ('[Sample] Sports Day',          '[नमूना] खेल दिवस',          'SPORTS', current_date - 40, true),
    ('[Sample] Cultural Programme',  '[नमूना] सांस्कृतिक कार्यक्रम', 'EVENTS', current_date - 25, true)
  RETURNING id)
INSERT INTO gallery_photo (album_id, url, caption, display_order)
SELECT id, '/uploads/seed/school-1.jpg', 'Sample photo. Replace from Admin, Photo gallery.', 0 FROM a;

-- ------------------------------------------------------------------ activity log
INSERT INTO audit_log (user_email, action, entity_type, summary) VALUES
  ('data-import', 'CREATE', 'Demo data', 'Added demonstration students, fee structure, payments, salaries, expenses, enquiries and content, modelled on Gyan Ganga Finance Nov 2026.xlsx. Remove with deploy/demo-data/remove-demo-data.sql');

-- ------------------------------------------------------------------ result
SELECT 'students' AS what, count(*)::text AS value FROM student
UNION ALL SELECT 'students by tier (free / Rs300 / full)', (SELECT count(*) FROM gen2 WHERE tier = 'FREE') || ' / ' || (SELECT count(*) FROM gen2 WHERE tier = 'LOW') || ' / ' || (SELECT count(*) FROM gen2 WHERE tier = 'NORMAL')
UNION ALL SELECT 'April tuition collected (sheet says 71,700)', sum(amount)::text FROM fee_payment WHERE for_period = 'Tuition Apr'
UNION ALL SELECT 'receipts', count(*)::text FROM fee_payment
UNION ALL SELECT 'total fees collected so far', sum(amount)::text FROM fee_payment
UNION ALL SELECT 'students behind on tuition', count(*)::text FROM pay_plan WHERE behind > 0
UNION ALL SELECT 'teacher salary records (paid / pending)', (SELECT count(*) FROM salary_record WHERE status = 'PAID') || ' / ' || (SELECT count(*) FROM salary_record WHERE status = 'PENDING')
UNION ALL SELECT 'expense entries', count(*)::text FROM expense
UNION ALL SELECT 'expenses total', sum(amount)::text FROM expense
UNION ALL SELECT 'enquiries', count(*)::text FROM enquiry
UNION ALL SELECT 'announcements', count(*)::text FROM announcement
UNION ALL SELECT 'management members', count(*)::text FROM management_member
UNION ALL SELECT 'gallery albums / photos', (SELECT count(*) FROM gallery_album) || ' / ' || (SELECT count(*) FROM gallery_photo);

COMMIT;
