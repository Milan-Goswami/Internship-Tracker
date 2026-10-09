import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { portfolioData } from '../data/portfolio';
import { Menu, X, ArrowUpRight } from 'lucide-react';
import ThemeToggle from './ThemeToggle';
import './Navbar.css';

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [activeSection, setActiveSection] = useState('home');

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 40);
      if (window.scrollY < 80) {
        setActiveSection('home');
      }
    };
    window.addEventListener('scroll', handleScroll, { passive: true });

    const sectionIds = ['home', 'projects', 'academic', 'learning', 'contact'];
    const sections = sectionIds.map(id => document.getElementById(id)).filter(Boolean);

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setActiveSection(entry.target.id);
          }
        });
      },
      {
        rootMargin: '-25% 0px -50% 0px',
        threshold: 0
      }
    );

    sections.forEach((sec) => observer.observe(sec));

    const handleEsc = (e) => {
      if (e.key === 'Escape') setMenuOpen(false);
    };
    window.addEventListener('keydown', handleEsc);

    return () => {
      window.removeEventListener('scroll', handleScroll);
      window.removeEventListener('keydown', handleEsc);
      observer.disconnect();
    };
  }, []);

  const handleNavClick = (sectionId) => {
    setActiveSection(sectionId);
    setMenuOpen(false);
  };

  return (
    <>
      <header className={`navbar-header ${scrolled ? 'header-scrolled' : ''}`}>
        <div className="container nav-outer-container">
          
          {/* Left: Brand Identity matching reference "Elian Ross" typography */}
          <a 
            href="#home" 
            className="brand-signature" 
            onClick={() => handleNavClick('home')}
            aria-label="Milan Goswami home"
          >
            <span className="brand-italic">Milan Goswami</span>
          </a>

          {/* Right Controls: Theme Toggle + Circular Hamburger Menu */}
          <div className="nav-right-cluster">
            <ThemeToggle id="header-theme-toggle" />
            <button 
              type="button"
              className="nav-circle-toggle"
              onClick={() => setMenuOpen(!menuOpen)}
              aria-expanded={menuOpen}
              aria-label={menuOpen ? "Close navigation menu" : "Open navigation menu"}
            >
              {menuOpen ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
            </button>
          </div>

        </div>
      </header>

      {/* Slide-in Navigation Drawer */}
      <AnimatePresence>
        {menuOpen && (
          <motion.div 
            className="nav-drawer-backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.25 }}
            onClick={() => setMenuOpen(false)}
          >
            <motion.div 
              className="nav-drawer-panel"
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'spring', damping: 26, stiffness: 240 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="drawer-header">
                <span className="drawer-brand-italic">Milan Goswami</span>
                <button 
                  type="button" 
                  className="drawer-close-btn"
                  onClick={() => setMenuOpen(false)}
                  aria-label="Close menu"
                >
                  <X size={22} aria-hidden="true" />
                </button>
              </div>

              <div className="drawer-nav-list">
                <a 
                  href="#home" 
                  className={`drawer-link ${activeSection === 'home' ? 'link-active' : ''}`}
                  onClick={() => handleNavClick('home')}
                >
                  <span className="link-num">01</span>
                  <span className="link-text">Overview</span>
                </a>
                <a 
                  href="#projects" 
                  className={`drawer-link ${activeSection === 'projects' ? 'link-active' : ''}`}
                  onClick={() => handleNavClick('projects')}
                >
                  <span className="link-num">02</span>
                  <span className="link-text">Featured Projects</span>
                </a>
                <a 
                  href="#academic" 
                  className={`drawer-link ${activeSection === 'academic' ? 'link-active' : ''}`}
                  onClick={() => handleNavClick('academic')}
                >
                  <span className="link-num">03</span>
                  <span className="link-text">Academic Progression</span>
                </a>
                <a 
                  href="#learning" 
                  className={`drawer-link ${activeSection === 'learning' ? 'link-active' : ''}`}
                  onClick={() => handleNavClick('learning')}
                >
                  <span className="link-num">04</span>
                  <span className="link-text">Currently Learning</span>
                </a>
                <a 
                  href="#contact" 
                  className={`drawer-link ${activeSection === 'contact' ? 'link-active' : ''}`}
                  onClick={() => handleNavClick('contact')}
                >
                  <span className="link-num">05</span>
                  <span className="link-text">Get in Touch</span>
                </a>
              </div>

              <div className="drawer-footer">
                <div className="drawer-theme-preference-row">
                  <span className="drawer-footer-title">APPEARANCE</span>
                  <ThemeToggle id="drawer-theme-toggle" />
                </div>
                <span className="drawer-footer-title">CONNECT</span>
                <div className="drawer-social-links">
                  <a href={portfolioData.links.github} target="_blank" rel="noopener noreferrer">
                    GitHub <ArrowUpRight size={13} aria-hidden="true" />
                  </a>
                  <a href={portfolioData.links.linkedin} target="_blank" rel="noopener noreferrer">
                    LinkedIn <ArrowUpRight size={13} aria-hidden="true" />
                  </a>
                  <a href={portfolioData.links.leetcode} target="_blank" rel="noopener noreferrer">
                    LeetCode <ArrowUpRight size={13} aria-hidden="true" />
                  </a>
                </div>
                <div className="drawer-email-tag">
                  <a href={`mailto:${portfolioData.profile.email}`}>
                    {portfolioData.profile.email}
                  </a>
                </div>
              </div>

            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
