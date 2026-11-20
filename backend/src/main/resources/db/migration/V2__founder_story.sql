-- The founder's story for the About page. Everything here can be edited later in Admin > Page content ("About page").
-- The photo is left empty here and is set separately, so a fresh install never points at a missing file.
INSERT INTO site_setting (setting_key, group_name, label, kind, value_en, value_hi, sort_order)
SELECT v.k, 'About page', v.label, v.kind, v.en, v.hi,
       COALESCE((SELECT sort_order FROM site_setting WHERE setting_key = 'about.history'), 100)
FROM (VALUES
  ('about.founder.headline', 'Founder: headline', 'TEXT',
   'She Turned Her Home Into a School, and Never Turned a Child Away',
   'जिसने अपने घर को विद्यालय बना दिया और किसी बच्चे को लौटाया नहीं'),

  ('about.founder.name', 'Founder: name', 'TEXT',
   'Madhuri Mishra',
   'माधुरी मिश्रा'),

  ('about.founder.story', 'Founder: story (leave one blank line between paragraphs)', 'LONG',
   $en$In 1970, Madhuri Mishra single-handedly took on the responsibility of educating every child who came to her. She had no separate building, so she began in the one place she had: the house in which she and her family lived.

Every morning, her home became a school. When the day's classes ended, the tables and chairs were cleared away and the rooms went back to being a family's home. That daily routine of setting up and putting away said more about her dedication than any words could.

Behind it was a simple and noble belief: that every child deserves a good education, no matter what their background.

She held to that belief even when the school was doing well. She refused to raise the fees beyond a certain amount, so that no student would ever have to leave. And when a family could not pay, she would not remove the child from the school.

She was just as steadfast with children who struggled. When a student performed poorly, she gave him or her another chance to try again, and she never worried about what it might do to the school's results. For her, a child's future mattered more than the school's record.

Madhuri Mishra left us in 2022. The school she began in her own home continues to follow in her footsteps, and today it is led by its Principal, Dr. Jaya Mishra.$en$,
   $hi$सन् 1970 में माधुरी मिश्रा ने अकेले ही हर बच्चे को शिक्षा देने की ज़िम्मेदारी अपने कंधों पर उठाई। उनके पास अलग इमारत नहीं थी, इसलिए उन्होंने वहीं से शुरुआत की जहाँ वे अपने परिवार के साथ रहती थीं, अपने घर से।

हर सुबह उनका घर विद्यालय बन जाता था। दिन की कक्षाएँ समाप्त होते ही मेज़-कुर्सियाँ हटा दी जाती थीं और घर फिर परिवार का घर बन जाता था। रोज़ मेज़-कुर्सियाँ लगाने और हटाने का यह क्रम उनके समर्पण को किसी भी शब्द से बेहतर बयान करता था।

इसके पीछे एक सरल और उदात्त विचार था: हर बच्चा अच्छी शिक्षा का हकदार है, उसकी पृष्ठभूमि चाहे जो हो।

विद्यालय अच्छा चल रहा था, फिर भी वे अपने इस विचार पर अडिग रहीं। उन्होंने शुल्क एक निश्चित राशि से अधिक बढ़ाने से इनकार कर दिया, ताकि किसी विद्यार्थी को विद्यालय न छोड़ना पड़े। और जब कोई परिवार शुल्क नहीं दे पाता था, तब भी उन्होंने बच्चे को विद्यालय से नहीं हटाया।

जो बच्चे पढ़ाई में पिछड़ जाते थे, उनके प्रति भी वे उतनी ही दृढ़ रहीं। किसी विद्यार्थी का प्रदर्शन कमज़ोर होता तो वे उसे दोबारा प्रयास का अवसर देती थीं, और इसकी चिंता नहीं करती थीं कि विद्यालय के परिणामों पर इसका क्या असर पड़ेगा। उनके लिए बच्चे का भविष्य विद्यालय के रिकॉर्ड से कहीं बड़ा था।

माधुरी मिश्रा वर्ष 2022 में हमें छोड़कर चली गईं। उन्होंने अपने ही घर में जिस विद्यालय की नींव रखी, वह आज भी उनके पदचिह्नों पर चल रहा है, और आज प्रधानाचार्य डॉ. जया मिश्रा के नेतृत्व में आगे बढ़ रहा है।$hi$),

  ('about.founder.photo', 'Founder: photo', 'IMAGE', NULL, NULL),

  ('about.founder.caption', 'Founder: photo caption (optional)', 'TEXT', NULL, NULL)
) AS v(k, label, kind, en, hi)
WHERE NOT EXISTS (SELECT 1 FROM site_setting s WHERE s.setting_key = v.k);
