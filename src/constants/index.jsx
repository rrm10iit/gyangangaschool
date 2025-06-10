import { BotMessageSquare } from "lucide-react";
import { BatteryCharging } from "lucide-react";
import { Fingerprint } from "lucide-react";
import { ShieldHalf } from "lucide-react";
import { PlugZap } from "lucide-react";
import { GlobeLock } from "lucide-react";
import { Wallet, BookOpenText, Users2, Trophy, Palette} from "lucide-react"; // Importing relevant icons


import user1 from "../assets/profile-pictures/user1.jpg";
import user2 from "../assets/profile-pictures/user2.jpg";
import user3 from "../assets/profile-pictures/user3.jpg";
import user4 from "../assets/profile-pictures/user4.jpg";
import user5 from "../assets/profile-pictures/user5.jpg";
import user6 from "../assets/profile-pictures/user6.jpg";

export const navItems = [
  { label: "About Us", href: "#" },
  { label: "School Gallery", href: "#" },
  { label: "Principle's Desk", href: "#" },
  { label: "Contact", href: "#" },
];

export const testimonials = [
  {
    user: "Ananya Mishra (IAS)",
    company: "District Magistrate, Prayagraj",
    image: user1,
    text: "Visiting Gyan Ganga School was truly inspiring. The emphasis on both academics and values ensures holistic development of students. The environment is disciplined, nurturing, and forward-looking.",
  },
  {
    user: "Rajeev Sharma (IAS)",
    company: "Former Education Secretary, Uttar Pradesh",
    image: user2,
    text: "I commend the efforts of the school staff and administration for maintaining such high standards in education and character-building. The dedication towards student growth is remarkable.",
  },
  {
    user: "Neelam Verma",
    company: "Parent of Class 8 Student",
    image: user3,
    text: "I’ve seen a drastic improvement in my son’s communication and confidence since joining the school. The English speaking classes and co-curricular focus are really making a difference.",
  },
  {
    user: "Suresh Kumar",
    company: "Parent of Class 6 Student",
    image: user4,
    text: "The school provides a well-rounded experience for children. My daughter looks forward to her singing and sports sessions just as much as her studies.",
  },
  {
    user: "Priya Singh",
    company: "Parent of Alumni",
    image: user5,
    text: "My child studied here from Nursery to Class 8 and is now excelling in high school. Gyan Ganga School laid the perfect foundation – both academically and morally.",
  },
  {
    user: "Dr. Kavita Tiwari",
    company: "Educationist & Guest Speaker",
    image: user6,
    text: "I was impressed by the school's integration of cultural values with modern pedagogy. It’s rare to see such a balance of academics, arts, and discipline in one institution.",
  },
];


export const features = [
  {
    icon: <BookOpenText />,
    text: "Quality Education",
    description:
      "Our school delivers a strong academic foundation with experienced teachers and a curriculum designed to nurture curiosity, creativity, and critical thinking.",
  },
  {
    icon: <Wallet />,
    text: "Affordable Fees",
    description:
      "We believe education should be accessible to all. Our transparent and affordable fee structure ensures every child has the opportunity to learn and grow.",
  },
  {
    icon: <Trophy />,
    text: "Sports & Fitness",
    description:
      "From football to athletics, we promote physical well-being and teamwork through a variety of sports and fitness programs.",
  },
  {
    icon: <Palette />,
    text: "Cultural Activities",
    description:
      "Our school encourages participation in arts, music, drama, and festivals to help students express themselves and celebrate diversity.",
  },
  {
    icon: <Users2 />,
    text: "Holistic Development",
    description:
      "We focus on developing well-rounded individuals by blending academics, character education, and co-curricular engagement.",
  },
];

export const checklistItems = [
  {
    title: "English & Hindi Medium Instruction",
    description:
      "Classes from Nursery to 8th are conducted in English medium, while Hindi medium is also offered following the UP Board curriculum.",
  },
  {
    title: "UP Board Curriculum",
    description:
      "Our academic program adheres to the Uttar Pradesh (UP) Board, ensuring a standardized and recognized syllabus across subjects.",
  },
  {
    title: "English Speaking Classes",
    description:
      "Dedicated English speaking sessions are conducted for all classes to build communication skills and confidence in students.",
  },
  {
    title: "Singing, Cultural & Sports Activities",
    description:
      "Students engage in regular singing classes, cultural programs, and a wide range of sports activities to support their physical and creative development.",
  },
];

export const pricingOptions = [
  {
    title: "Free",
    price: "$0",
    features: [
      "Private board sharing",
      "5 Gb Storage",
      "Web Analytics",
      "Private Mode",
    ],
  },
  {
    title: "Pro",
    price: "$10",
    features: [
      "Private board sharing",
      "10 Gb Storage",
      "Web Analytics (Advance)",
      "Private Mode",
    ],
  },
  {
    title: "Enterprise",
    price: "$200",
    features: [
      "Private board sharing",
      "Unlimited Storage",
      "High Performance Network",
      "Private Mode",
    ],
  },
];

export const resourcesLinks = [
  { href: "#", text: "Getting Started" },
  { href: "#", text: "Documentation" },
  { href: "#", text: "Tutorials" },
  { href: "#", text: "API Reference" },
  { href: "#", text: "Community Forums" },
];

export const platformLinks = [
  { href: "#", text: "Features" },
  { href: "#", text: "Supported Devices" },
  { href: "#", text: "System Requirements" },
  { href: "#", text: "Downloads" },
  { href: "#", text: "Release Notes" },
];

export const communityLinks = [
  { href: "#", text: "Events" },
  { href: "#", text: "Meetups" },
  { href: "#", text: "Conferences" },
  { href: "#", text: "Hackathons" },
  { href: "#", text: "Jobs" },
];
