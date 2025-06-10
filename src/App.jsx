import Navbar from "./components/Navbar";
import HeroSection from "./components/HeroSection";
import FeatureSection from "./components/FeatureSection";
import Curriculum from "./components/Curriculum";
import Footer from "./components/Footer";
import Contact from "./components/Contact";
import Testimonials from "./components/Testimonials";

const App = () => {
  return (
    <>
      <Navbar />
      <div className="max-w-7xl mx-auto pt-20 px-6">
        <HeroSection />
        <FeatureSection />
        <Curriculum />
        <Contact />
        <Testimonials />
        <Footer />
      </div>
    </>
  );
};

export default App;
