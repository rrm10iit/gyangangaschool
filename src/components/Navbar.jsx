import { Menu, X } from "lucide-react";
import { useState, useEffect } from "react";
import logo from "../assets/logo.png";
import { navItems } from "../constants";

const Navbar = () => {
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);

  const toggleNavbar = () => {
    setMobileDrawerOpen(!mobileDrawerOpen);
  };

  // Prevent background scroll when mobile menu is open
  useEffect(() => {
    if (mobileDrawerOpen) {
      document.body.classList.add("overflow-hidden");
    } else {
      document.body.classList.remove("overflow-hidden");
    }
  }, [mobileDrawerOpen]);

  return (
    <nav className="sticky top-0 z-50 py-3 backdrop-blur-lg border-b border-neutral-300 dark:border-neutral-700/80 bg-white dark:bg-black">
      <div className="container px-4 mx-auto relative lg:text-sm">
        <div className="flex justify-between items-center">
          {/* Logo + Title */}
          <div className="flex items-center flex-shrink-0 z-50">
            <img className="h-10 w-10 mr-2" src={logo} alt="Logo" />
            <span className="text-xl tracking-tight text-black dark:text-white">Gyan Ganga School</span>
          </div>

          {/* Desktop nav */}
          <ul className="hidden lg:flex ml-14 space-x-12">
            {navItems.map((item, index) => (
              <li key={index}>
                <a href={item.href}>{item.label}</a>
              </li>
            ))}
          </ul>

          {/* Mobile menu button */}
          <div className="lg:hidden z-50">
            <button onClick={toggleNavbar} className="text-black dark:text-white">
              {mobileDrawerOpen ? <X size={28} /> : <Menu size={28} />}
            </button>
          </div>
        </div>

        {/* Mobile nav drawer */}
        {mobileDrawerOpen && (
  <div className="fixed inset-0 z-40 bg-neutral-100 dark:bg-neutral-800 text-black dark:text-white min-h-screen flex flex-col items-center pt-24 px-6">
    <ul className="w-full text-center">
      {navItems.map((item, index) => (
        <li key={index} className="py-4 border-b border-neutral-300 dark:border-neutral-600">
          <a href={item.href}>{item.label}</a>
        </li>
      ))}
    </ul>
  </div>
)}
      </div>
    </nav>
  );
};

export default Navbar;
