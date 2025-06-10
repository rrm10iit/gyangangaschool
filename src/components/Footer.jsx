const Footer = () => {
  return (
    <footer className="mt-20 border-t border-neutral-300 dark:border-neutral-700 text-black dark:text-neutral-300">
      {/* Nav Links Row */}
      <div className="flex flex-wrap justify-center gap-8 py-6">
        {[
          { label: "About Us", href: "#about" },
          { label: "School Gallery", href: "#gallery" },
          { label: "Principal's Desk", href: "#principal" },
          { label: "Contact", href: "#contact" },
        ].map((item, index) => (
          <a
            key={index}
            href={item.href}
            className="hover:text-orange-600 transition-colors"
          >
            {item.label}
          </a>
        ))}
      </div>

      {/* Divider */}
      <div className="border-t border-neutral-200 dark:border-neutral-600 mx-4"></div>

      {/* Bottom Copyright */}
      <div className="text-center text-sm py-4 text-neutral-500 dark:text-neutral-400">
        © 2025 | RRM. All rights reserved.
      </div>
    </footer>
  );
};

export default Footer;
