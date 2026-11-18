package com.gyanganga.school.seed;

import com.gyanganga.school.config.AppProps;
import com.gyanganga.school.model.*;
import com.gyanganga.school.repo.Repos;
import com.gyanganga.school.service.FileStorageService;
import com.gyanganga.school.service.FinanceService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Creates the first admin account and the base site content on an empty database.
 * Demo records (students, teachers, finance, gallery) are added only when SEED_DEMO_DATA=true.
 */
@Component
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppProps props;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final FileStorageService storage;
    private final Repos.UserRepo users;
    private final Repos.ClassRepo classes;
    private final Repos.SettingRepo settings;
    private final Repos.FacilityRepo facilities;
    private final Repos.TestimonialRepo testimonials;
    private final Repos.ManagementRepo management;
    private final Repos.StudentRepo students;
    private final Repos.TeacherRepo teachers;
    private final Repos.FeeStructureRepo structures;
    private final Repos.PaymentRepo payments;
    private final Repos.SalaryRepo salaries;
    private final Repos.ExpenseRepo expenses;
    private final Repos.AlbumRepo albums;
    private final Repos.PhotoRepo photos;
    private final Repos.AnnouncementRepo announcements;
    private final Repos.EnquiryRepo enquiries;

    public DataSeeder(AppProps props, PasswordEncoder encoder, TransactionTemplate tx, FileStorageService storage,
                      Repos.UserRepo users, Repos.ClassRepo classes, Repos.SettingRepo settings,
                      Repos.FacilityRepo facilities, Repos.TestimonialRepo testimonials,
                      Repos.ManagementRepo management, Repos.StudentRepo students, Repos.TeacherRepo teachers,
                      Repos.FeeStructureRepo structures, Repos.PaymentRepo payments, Repos.SalaryRepo salaries,
                      Repos.ExpenseRepo expenses, Repos.AlbumRepo albums, Repos.PhotoRepo photos,
                      Repos.AnnouncementRepo announcements, Repos.EnquiryRepo enquiries) {
        this.props = props;
        this.encoder = encoder;
        this.tx = tx;
        this.storage = storage;
        this.users = users;
        this.classes = classes;
        this.settings = settings;
        this.facilities = facilities;
        this.testimonials = testimonials;
        this.management = management;
        this.students = students;
        this.teachers = teachers;
        this.structures = structures;
        this.payments = payments;
        this.salaries = salaries;
        this.expenses = expenses;
        this.albums = albums;
        this.photos = photos;
        this.announcements = announcements;
        this.enquiries = enquiries;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(s -> {
            seedAdmin();
            if (classes.count() == 0) seedBase();
            if (props.seedDemoData() && students.count() == 0) seedDemo();
        });
    }

    // ------------------------------------------------------------------ admin

    private void seedAdmin() {
        if (users.count() > 0) return;
        String password = props.adminPassword();
        boolean generated = password == null || password.isBlank();
        if (generated) {
            byte[] b = new byte[12];
            new SecureRandom().nextBytes(b);
            password = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        }
        AppUser u = new AppUser();
        u.setEmail(props.adminEmail().trim().toLowerCase());
        u.setFullName("Administrator");
        u.setRole(Enums.Role.ADMIN);
        u.setPasswordHash(encoder.encode(password));
        users.save(u);
        if (generated) {
            log.warn("=====================================================================");
            log.warn(" ADMIN_PASSWORD was not set. A one-time password was generated:");
            log.warn("   email:    {}", u.getEmail());
            log.warn("   password: {}", password);
            log.warn(" Sign in and change it now (Admin > My account).");
            log.warn("=====================================================================");
        } else {
            log.info("Created admin account {}", u.getEmail());
        }
    }

    // ------------------------------------------------------------------ base content

    private void seedBase() {
        log.info("Seeding base content");
        String[][] cls = {
                {"Nursery", "3+ years", "Play-based learning that builds language, motor skills and curiosity.", "खेल आधारित शिक्षा जो भाषा, गतिशील कौशल और जिज्ञासा को बढ़ाती है।", "English, Hindi, Numbers, Rhymes, Art & Craft"},
                {"LKG", "4+ years", "Early literacy and numeracy through stories, songs and activities.", "कहानियों, गीतों और गतिविधियों के माध्यम से आरंभिक पठन-लेखन और गणित।", "English, Hindi, Mathematics, EVS, Rhymes, Art & Craft"},
                {"UKG", "5+ years", "A confident start to reading, writing and basic arithmetic before Class 1.", "कक्षा 1 से पहले पढ़ने, लिखने और बुनियादी गणित की आत्मविश्वासपूर्ण शुरुआत।", "English, Hindi, Mathematics, EVS, General Knowledge, Art & Craft"},
                {"Class 1", "6+ years", "Strong foundations in reading, writing and mathematics.", "पढ़ने, लिखने और गणित की मजबूत नींव।", "English, Hindi, Mathematics, EVS, General Knowledge, Computer Basics"},
                {"Class 2", "7+ years", "Building fluency, number sense and the habit of curiosity.", "प्रवाह, संख्या ज्ञान और जिज्ञासा की आदत का विकास।", "English, Hindi, Mathematics, EVS, General Knowledge, Computer Basics"},
                {"Class 3", "8+ years", "Introduction to science and social studies alongside languages and maths.", "भाषा और गणित के साथ विज्ञान और सामाजिक विज्ञान का परिचय।", "English, Hindi, Mathematics, Science, Social Studies, Computer"},
                {"Class 4", "9+ years", "Deeper concepts, project work and regular English speaking practice.", "गहरी अवधारणाएँ, प्रोजेक्ट कार्य और अंग्रेज़ी बोलचाल का नियमित अभ्यास।", "English, Hindi, Mathematics, Science, Social Studies, Computer"},
                {"Class 5", "10+ years", "Analytical thinking and independent study skills.", "विश्लेषणात्मक सोच और स्वतंत्र अध्ययन कौशल।", "English, Hindi, Mathematics, Science, Social Studies, Computer, Sanskrit"},
                {"Class 6", "11+ years", "Subject-wise learning with practical activities and regular assessment.", "व्यावहारिक गतिविधियों और नियमित मूल्यांकन के साथ विषयवार शिक्षा।", "English, Hindi, Mathematics, Science, Social Studies, Computer, Sanskrit"},
                {"Class 7", "12+ years", "Strengthening concepts and building confidence for senior classes.", "अवधारणाओं को मजबूत करना और उच्च कक्षाओं के लिए आत्मविश्वास बनाना।", "English, Hindi, Mathematics, Science, Social Studies, Computer, Sanskrit"},
                {"Class 8", "13+ years", "Preparing students for high school with a focus on concepts, discipline and values.", "अवधारणाओं, अनुशासन और मूल्यों पर ध्यान देते हुए छात्रों को हाई स्कूल के लिए तैयार करना।", "English, Hindi, Mathematics, Science, Social Studies, Computer, Sanskrit"}
        };
        int order = 1;
        for (String[] c : cls) {
            SchoolClass sc = new SchoolClass();
            sc.setName(c[0]);
            sc.setAgeGroup(c[1]);
            sc.setDescription(c[2]);
            sc.setDescriptionHi(c[3]);
            sc.setSubjects(c[4]);
            sc.setSections("A");
            sc.setSeats(40);
            sc.setDisplayOrder(order++);
            classes.save(sc);
        }

        int o = 0;
        // school
        set("School", "school.name", "School name", Enums.SettingKind.TEXT, "Gyan Ganga School", "ज्ञान गंगा स्कूल", o++);
        set("School", "school.motto", "Motto", Enums.SettingKind.TEXT, "Honesty, Knowledge, Enthusiasm", "सत्यनिष्ठा, ज्ञान, उत्साह", o++);
        set("School", "school.phone", "Phone (UPDATE: placeholder)", Enums.SettingKind.TEXT, "+91 00000 00000", null, o++);
        set("School", "school.email", "Email (UPDATE: placeholder; enquiries are also sent here)", Enums.SettingKind.TEXT, "gyangangaschool@example.com", null, o++);
        set("School", "school.address", "Address", Enums.SettingKind.LONG,
                "Gyan Ganga School Road, Rasulabad, Teliarganj, Prayagraj, Uttar Pradesh 211002",
                "ज्ञान गंगा स्कूल रोड, रसूलाबाद, तेलियरगंज, प्रयागराज, उत्तर प्रदेश 211002", o++);
        set("School", "school.map", "Google Maps embed URL", Enums.SettingKind.URL,
                "https://www.google.com/maps?q=Rasulabad+Teliarganj+Prayagraj+211002&output=embed", null, o++);
        set("School", "school.hours", "Office hours", Enums.SettingKind.TEXT, "Monday to Saturday, 8:00 AM to 2:00 PM", "सोमवार से शनिवार, सुबह 8:00 से दोपहर 2:00 बजे तक", o++);
        set("School", "school.founded", "Year founded (UPDATE: placeholder; used for 'years of service')", Enums.SettingKind.TEXT, "2010", null, o++);
        set("School", "school.board", "Board / curriculum", Enums.SettingKind.TEXT, "UP Board", "यूपी बोर्ड", o++);
        set("School", "school.medium", "Medium of instruction", Enums.SettingKind.TEXT, "English and Hindi", "अंग्रेज़ी और हिंदी", o++);

        // home
        set("Home page", "home.hero.title", "Hero title", Enums.SettingKind.TEXT,
                "Education for All, Excellence for Each", "सबके लिए शिक्षा, हर एक के लिए उत्कृष्टता", o++);
        set("Home page", "home.hero.subtitle", "Hero subtitle", Enums.SettingKind.LONG,
                "A caring school in Prayagraj for Nursery to Class 8, where strong academics meet good values, sports and culture.",
                "प्रयागराज में नर्सरी से कक्षा 8 तक का एक स्नेहिल विद्यालय, जहाँ सशक्त शिक्षा के साथ संस्कार, खेल और संस्कृति का मेल है।", o++);
        set("Home page", "home.hero.image", "Hero image", Enums.SettingKind.IMAGE, "/images/students.jpg", null, o++);
        set("Home page", "home.intro", "Introduction", Enums.SettingKind.LONG,
                "Gyan Ganga School offers a disciplined, nurturing environment with experienced teachers, English-speaking practice, "
                        + "and regular sports, singing and cultural activities. Classes from Nursery to 8th are taught in English medium, "
                        + "and Hindi medium is also available.",
                "ज्ञान गंगा स्कूल अनुभवी शिक्षकों, अंग्रेज़ी बोलचाल के अभ्यास तथा नियमित खेल, संगीत और सांस्कृतिक गतिविधियों के साथ एक अनुशासित और स्नेहपूर्ण वातावरण देता है। "
                        + "नर्सरी से 8वीं तक की कक्षाएँ अंग्रेज़ी माध्यम में चलती हैं, और हिंदी माध्यम भी उपलब्ध है।", o++);
        set("Home page", "admission.cta", "Admission call-to-action text", Enums.SettingKind.LONG,
                "Admissions are open. Visit the school office or send us an enquiry and we will get back to you.",
                "प्रवेश जारी हैं। विद्यालय कार्यालय आएँ या हमें पूछताछ भेजें, हम आपसे संपर्क करेंगे।", o++);

        // about
        set("About page", "about.history", "Our story", Enums.SettingKind.LONG,
                "Gyan Ganga School was started in Rasulabad, Teliarganj, Prayagraj with a simple belief: every child deserves good education at a fair cost. "
                        + "Over the years the school has grown into a trusted place of learning for families in the neighbourhood, "
                        + "guided by its motto of honesty, knowledge and enthusiasm.",
                "ज्ञान गंगा स्कूल की शुरुआत रसूलाबाद, तेलियरगंज, प्रयागराज में एक सरल विश्वास के साथ हुई कि हर बच्चे को उचित खर्च में अच्छी शिक्षा मिलनी चाहिए। "
                        + "सत्यनिष्ठा, ज्ञान और उत्साह के ध्येय से प्रेरित होकर यह विद्यालय आसपास के परिवारों के लिए एक विश्वसनीय शिक्षा केंद्र बन चुका है।", o++);
        set("About page", "about.vision", "Vision", Enums.SettingKind.LONG,
                "To nurture confident, honest and capable young people who carry the light of knowledge into their families and society.",
                "ऐसे आत्मविश्वासी, ईमानदार और सक्षम युवाओं का निर्माण करना जो ज्ञान का प्रकाश अपने परिवार और समाज तक पहुँचाएँ।", o++);
        set("About page", "about.mission", "Mission", Enums.SettingKind.LONG,
                "To provide quality, affordable education with strong values, balanced with sports, arts and culture, so that every child can grow in mind, body and character.",
                "मूल्यों पर आधारित गुणवत्तापूर्ण और किफ़ायती शिक्षा देना, जिसमें खेल, कला और संस्कृति का संतुलन हो, ताकि हर बच्चा मन, शरीर और चरित्र से विकसित हो।", o++);
        set("About page", "about.principal.name", "Principal's name (UPDATE)", Enums.SettingKind.TEXT, "Principal", "प्रधानाचार्य", o++);
        set("About page", "about.principal.photo", "Principal's photo", Enums.SettingKind.IMAGE, null, null, o++);
        set("About page", "about.principal.message", "Principal's message (UPDATE)", Enums.SettingKind.LONG,
                "Dear parents and students, education at Gyan Ganga School is about more than marks. We want every child to learn with joy, speak with confidence and live with honesty. "
                        + "Together with our teachers and with your support, we work to give each child the best start in life.",
                "प्रिय अभिभावकों और विद्यार्थियों, ज्ञान गंगा स्कूल में शिक्षा केवल अंकों तक सीमित नहीं है। हम चाहते हैं कि हर बच्चा आनंद से सीखे, आत्मविश्वास से बोले और ईमानदारी से जिए। "
                        + "हमारे शिक्षकों और आपके सहयोग से हम हर बच्चे को जीवन की सर्वश्रेष्ठ शुरुआत देने का प्रयास करते हैं।", o++);

        // facilities
        Object[][] fac = {
                {"Library", "पुस्तकालय", "A quiet reading space with story books, reference books and magazines for every age group.", "हर आयु वर्ग के लिए कहानी की पुस्तकों, संदर्भ ग्रंथों और पत्रिकाओं वाला शांत पठन स्थान।", "library"},
                {"Laboratories", "प्रयोगशालाएँ", "Hands-on learning in science and computer labs that turns concepts into experiments.", "विज्ञान और कंप्यूटर प्रयोगशालाओं में प्रायोगिक शिक्षा जो अवधारणाओं को प्रयोगों में बदलती है।", "labs"},
                {"Sports Ground", "खेल का मैदान", "Open ground for football, athletics and games that build fitness and teamwork.", "फ़ुटबॉल, एथलेटिक्स और खेलों के लिए खुला मैदान जो स्वास्थ्य और टीम भावना बढ़ाता है।", "sports"},
                {"Transport", "परिवहन", "Safe school transport for students from nearby areas.", "आसपास के क्षेत्रों के विद्यार्थियों के लिए सुरक्षित विद्यालय परिवहन।", "transport"},
                {"Smart Classrooms", "स्मार्ट कक्षाएँ", "Digital teaching aids that make lessons visual, interactive and easy to remember.", "डिजिटल शिक्षण साधन जो पाठों को दृश्यात्मक, संवादात्मक और यादगार बनाते हैं।", "smart"},
                {"Canteen", "कैंटीन", "Clean, simple and healthy food and snacks for students.", "विद्यार्थियों के लिए स्वच्छ, सादा और पौष्टिक भोजन व नाश्ता।", "canteen"}
        };
        int fo = 1;
        for (Object[] f : fac) {
            Facility x = new Facility();
            x.setTitle((String) f[0]);
            x.setTitleHi((String) f[1]);
            x.setDescription((String) f[2]);
            x.setDescriptionHi((String) f[3]);
            x.setIcon((String) f[4]);
            x.setDisplayOrder(fo++);
            facilities.save(x);
        }

        // testimonials carried over from the previous website
        String[][] t = {
                {"Ananya Mishra (IAS)", "District Magistrate, Prayagraj", "user1.jpg",
                        "Visiting Gyan Ganga School was truly inspiring. The emphasis on both academics and values ensures holistic development of students. The environment is disciplined, nurturing, and forward-looking."},
                {"Rajeev Sharma (IAS)", "Former Education Secretary, Uttar Pradesh", "user2.jpg",
                        "I commend the efforts of the school staff and administration for maintaining such high standards in education and character-building. The dedication towards student growth is remarkable."},
                {"Neelam Verma", "Parent of Class 8 Student", "user3.jpg",
                        "I've seen a drastic improvement in my son's communication and confidence since joining the school. The English speaking classes and co-curricular focus are really making a difference."},
                {"Suresh Kumar", "Parent of Class 6 Student", "user4.jpg",
                        "The school provides a well-rounded experience for children. My daughter looks forward to her singing and sports sessions just as much as her studies."},
                {"Priya Singh", "Parent of Alumni", "user5.jpg",
                        "My child studied here from Nursery to Class 8 and is now excelling in high school. Gyan Ganga School laid the perfect foundation, both academically and morally."},
                {"Dr. Kavita Tiwari", "Educationist & Guest Speaker", "user6.jpg",
                        "I was impressed by the school's integration of cultural values with modern pedagogy. It's rare to see such a balance of academics, arts, and discipline in one institution."}
        };
        int to = 1;
        for (String[] r : t) {
            Testimonial x = new Testimonial();
            x.setName(r[0]);
            x.setRole(r[1]);
            x.setQuote(r[3]);
            x.setPhotoUrl(seedImage(r[2]));
            x.setDisplayOrder(to++);
            testimonials.save(x);
        }
    }

    private void set(String group, String key, String label, Enums.SettingKind kind, String en, String hi, int order) {
        SiteSetting s = new SiteSetting();
        s.setSettingKey(key);
        s.setGroupName(group);
        s.setLabel(label);
        s.setKind(kind);
        s.setValueEn(en);
        s.setValueHi(hi);
        s.setSortOrder(order);
        settings.save(s);
    }

    private String seedImage(String file) {
        try (InputStream in = new ClassPathResource("seed-images/" + file).getInputStream()) {
            return storage.copySeedImage(in, file);
        } catch (IOException e) {
            log.warn("Seed image {} not found", file);
            return null;
        }
    }

    // ------------------------------------------------------------------ demo data

    private void seedDemo() {
        log.info("Seeding demo data (students, teachers, finance, gallery)");
        Random rnd = new Random(42);
        LocalDate today = LocalDate.now();
        int year = FinanceService.academicYearOf(today);

        // management & announcements
        String[][] mgmt = {{"Chairman", "अध्यक्ष"}, {"Manager", "प्रबंधक"}, {"Principal", "प्रधानाचार्य"}};
        int mo = 1;
        for (String[] m : mgmt) {
            ManagementMember x = new ManagementMember();
            x.setName("Sample " + m[0]);
            x.setDesignation(m[0]);
            x.setDesignationHi(m[1]);
            x.setDisplayOrder(mo++);
            management.save(x);
        }
        announce("Admissions open for the new session", "नए सत्र के लिए प्रवेश प्रारंभ",
                "Admissions for Nursery to Class 8 are open. Please visit the school office or send an enquiry from the Contact page.",
                "नर्सरी से कक्षा 8 तक के प्रवेश प्रारंभ हैं। कृपया विद्यालय कार्यालय आएँ या संपर्क पृष्ठ से पूछताछ भेजें।", today, true);
        announce("Annual sports day", "वार्षिक खेल दिवस",
                "Our annual sports day will be held on the school ground. Parents are warmly invited.",
                "हमारा वार्षिक खेल दिवस विद्यालय के मैदान में आयोजित होगा। अभिभावकों का स्वागत है।", today.minusDays(10), false);
        announce("Parent-teacher meeting", "अभिभावक-शिक्षक बैठक",
                "A parent-teacher meeting will be held for all classes. Please check your child's diary for the time.",
                "सभी कक्षाओं के लिए अभिभावक-शिक्षक बैठक होगी। समय के लिए कृपया अपने बच्चे की डायरी देखें।", today.minusDays(25), false);

        // gallery with a sample image
        String sample = seedImage("school-1.jpg");
        String[][] alb = {{"Annual Day", "वार्षिक उत्सव", "ANNUAL_DAY"}, {"Sports Day", "खेल दिवस", "SPORTS"}, {"Cultural Programme", "सांस्कृतिक कार्यक्रम", "EVENTS"}};
        for (String[] a : alb) {
            GalleryAlbum g = new GalleryAlbum();
            g.setTitle(a[0]);
            g.setTitleHi(a[1]);
            g.setCategory(a[2]);
            g.setEventDate(today.minusDays(30 + rnd.nextInt(60)));
            albums.save(g);
            if (sample != null) {
                GalleryPhoto p = new GalleryPhoto();
                p.setAlbumId(g.getId());
                p.setUrl(sample);
                p.setCaption("Sample photo. Replace from Admin > Gallery.");
                photos.save(p);
            }
        }

        // teachers
        String[] tFirst = {"Sunita", "Rakesh", "Meena", "Anil", "Pooja", "Vikas", "Kavita", "Sanjay", "Neha", "Manoj"};
        String[] tLast = {"Verma", "Mishra", "Yadav", "Tiwari", "Singh", "Pandey", "Gupta", "Srivastava", "Dubey", "Maurya"};
        String[] subj = {"English", "Hindi", "Mathematics", "Science", "Social Studies", "Computer", "Sanskrit", "Art & Craft", "Physical Education", "General Knowledge"};
        String[] qual = {"B.A., B.Ed.", "M.A., B.Ed.", "B.Sc., B.Ed.", "M.Sc., B.Ed.", "B.Com., B.Ed.", "BCA, B.Ed.", "M.A. (Sanskrit), B.Ed.", "B.F.A., B.Ed.", "B.P.Ed.", "B.A., B.T.C."};
        List<Teacher> tl = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Teacher t = new Teacher();
            t.setEmployeeNo(String.format("T%03d", i + 1));
            t.setFullName(tFirst[i] + " " + tLast[i]);
            t.setSubjects(subj[i]);
            t.setClassesAssigned(i < 5 ? "Nursery, LKG, UKG, Class 1" : "Class 5, Class 6, Class 7, Class 8");
            t.setQualification(qual[i]);
            t.setPhone("98765" + String.format("%05d", 10000 + i * 137));
            t.setEmail(tFirst[i].toLowerCase() + "@example.com");
            t.setJoiningDate(LocalDate.of(2015 + i % 8, 4 + i % 3, 1));
            t.setMonthlySalary(BigDecimal.valueOf(5000 + 600L * i));
            tl.add(teachers.save(t));
        }

        // fee structure
        List<SchoolClass> cl = classes.findAllByOrderByDisplayOrderAscIdAsc();
        for (int i = 0; i < cl.size(); i++) {
            fee(cl.get(i), "Tuition fee", 350 + 60L * i, Enums.Frequency.MONTHLY);
            fee(cl.get(i), "Activity & exam fee", 300 + 25L * i, Enums.Frequency.QUARTERLY);
            fee(cl.get(i), "Annual charges", 1500, Enums.Frequency.ANNUAL);
        }

        // students
        String[] first = {"Aarav", "Vivaan", "Aditya", "Arjun", "Krish", "Ishaan", "Rohan", "Kabir", "Ayaan", "Dev",
                "Ananya", "Diya", "Saanvi", "Myra", "Aadhya", "Kiara", "Riya", "Priya", "Anika", "Navya"};
        String[] last = {"Sharma", "Verma", "Yadav", "Mishra", "Tiwari", "Singh", "Pandey", "Gupta", "Dubey", "Maurya", "Kesarwani", "Patel"};
        int months = Math.max(1, Math.min(12, (today.getYear() - year) * 12 + today.getMonthValue() - 4 + 1));
        int seq = 1;
        List<Student> sl = new ArrayList<>();
        for (SchoolClass c : cl) {
            for (int k = 0; k < 12; k++) {
                Student s = new Student();
                s.setAdmissionNo(String.format("GGS%d%03d", year, seq++));
                boolean girl = rnd.nextInt(2) == 0;
                s.setFullName((girl ? first[10 + rnd.nextInt(10)] : first[rnd.nextInt(10)]) + " " + last[rnd.nextInt(last.length)]);
                s.setClassId(c.getId());
                s.setSection("A");
                s.setRollNo(k + 1);
                s.setGender(girl ? "Female" : "Male");
                int age = 3 + (int) (c.getDisplayOrder() - 1);
                s.setDateOfBirth(LocalDate.of(year - age, 1 + rnd.nextInt(12), 1 + rnd.nextInt(27)));
                s.setGuardianName(first[rnd.nextInt(10)] + " " + s.getFullName().split(" ")[1]);
                s.setGuardianPhone("9" + (800000000L + rnd.nextInt(99999999)));
                s.setContactPhone(s.getGuardianPhone());
                s.setAddress("Rasulabad, Teliarganj, Prayagraj");
                s.setAdmissionDate(LocalDate.of(year, 4, 1 + rnd.nextInt(10)));
                if (rnd.nextInt(15) == 0) s.setFeeDiscount(BigDecimal.valueOf(1000));
                sl.add(students.save(s));
            }
        }

        // payments: each student has paid monthly tuition up to a random month
        List<FeeStructure> heads = structures.findAll();
        for (Student s : sl) {
            BigDecimal tuition = heads.stream()
                    .filter(h -> h.getClassId().equals(s.getClassId()) && h.getFrequency() == Enums.Frequency.MONTHLY)
                    .map(FeeStructure::getAmount).findFirst().orElse(BigDecimal.valueOf(400));
            int paidUpTo = rnd.nextInt(10) < 6 ? months : Math.max(0, months - 1 - rnd.nextInt(3));
            for (int m = 0; m < paidUpTo; m++) {
                LocalDate d = LocalDate.of(year, 4, 1).plusMonths(m).withDayOfMonth(5 + rnd.nextInt(15));
                if (d.isAfter(today)) d = today;
                pay(s, tuition, d, "Tuition " + d.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH),
                        year, rnd);
            }
            if (rnd.nextInt(10) < 7) {
                pay(s, BigDecimal.valueOf(1500), LocalDate.of(year, 4, 10 + rnd.nextInt(10)), "Annual charges", year, rnd);
            }
        }

        // salaries and expenses
        String[] cats = {"Electricity", "Stationery", "Maintenance", "Sports equipment", "Events", "Cleaning"};
        for (int m = 0; m < months; m++) {
            LocalDate base = LocalDate.of(year, 4, 1).plusMonths(m);
            String ym = String.format("%d-%02d", base.getYear(), base.getMonthValue());
            for (Teacher t : tl) {
                SalaryRecord r = new SalaryRecord();
                r.setTeacherId(t.getId());
                r.setMonth(ym);
                r.setBaseAmount(t.getMonthlySalary());
                r.setAllowances(BigDecimal.ZERO);
                r.setDeductions(BigDecimal.ZERO);
                r.setNetAmount(t.getMonthlySalary());
                boolean paid = m < months - 1;
                r.setStatus(paid ? Enums.SalaryStatus.PAID : Enums.SalaryStatus.PENDING);
                if (paid) {
                    LocalDate pd = base.plusMonths(1).withDayOfMonth(Math.min(5, 28));
                    r.setPaidDate(pd.isAfter(today) ? today : pd);
                    r.setMode(Enums.PayMode.BANK);
                }
                salaries.save(r);
            }
            for (int k = 0; k < 3; k++) {
                Expense e = new Expense();
                e.setCategory(cats[rnd.nextInt(cats.length)]);
                e.setDescription("Monthly " + e.getCategory().toLowerCase());
                e.setAmount(BigDecimal.valueOf(1500 + rnd.nextInt(6000)));
                LocalDate d = base.withDayOfMonth(3 + rnd.nextInt(24));
                e.setExpenseDate(d.isAfter(today) ? today : d);
                e.setMode(Enums.PayMode.CASH);
                e.setCreatedBy("system");
                expenses.save(e);
            }
        }

        // a couple of sample enquiries
        String[][] enq = {{"Rekha Devi", "9812345678", "Nursery", "I would like to know about admission and fees."},
                {"Amit Kumar", "9898989898", "Class 3", "Is transport available from Naini?"}};
        for (String[] e : enq) {
            Enquiry x = new Enquiry();
            x.setName(e[0]);
            x.setPhone(e[1]);
            x.setClassInterested(e[2]);
            x.setMessage(e[3]);
            enquiries.save(x);
        }
    }

    private void announce(String title, String titleHi, String body, String bodyHi, LocalDate date, boolean pinned) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setTitleHi(titleHi);
        a.setBody(body);
        a.setBodyHi(bodyHi);
        a.setPublishDate(date);
        a.setPinned(pinned);
        announcements.save(a);
    }

    private void fee(SchoolClass c, String head, long amount, Enums.Frequency f) {
        FeeStructure x = new FeeStructure();
        x.setClassId(c.getId());
        x.setHeadName(head);
        x.setAmount(BigDecimal.valueOf(amount));
        x.setFrequency(f);
        structures.save(x);
    }

    private void pay(Student s, BigDecimal amount, LocalDate date, String period, int year, Random rnd) {
        FeePayment p = new FeePayment();
        p.setStudentId(s.getId());
        p.setAmount(amount);
        p.setPaymentDate(date);
        p.setAcademicYear(year);
        Enums.PayMode[] modes = Enums.PayMode.values();
        p.setMode(modes[rnd.nextInt(3)]);
        p.setForPeriod(period);
        p.setCollectedBy("system");
        p.setReceiptNo("GGS/" + FinanceService.academicYearLabel(year) + "/" + String.format("%06d", payments.nextReceiptNumber()));
        payments.save(p);
    }
}
