import { useState, useRef, useEffect } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { Copy, Check, ArrowUp, ArrowUpRight, MapPin } from 'lucide-react';
import { portfolioData } from '../data/portfolio';
import './Footer.css';

gsap.registerPlugin(ScrollTrigger);

const CURRENT_YEAR = new Date().getFullYear();

export default function Footer() {
  const footerRef = useRef(null);
  const [copied, setCopied] = useState(false);
  const [istTime, setIstTime] = useState('');
  const [activeHoverNode, setActiveHoverNode] = useState(null);

  useEffect(() => {
    const updateTime = () => {
      try {
        const options = {
          timeZone: 'Asia/Kolkata',
          hour: '2-digit',
          minute: '2-digit',
          hour12: true
        };
        const formatter = new Intl.DateTimeFormat([], options);
        setIstTime(formatter.format(new Date()));
      } catch {
        setIstTime('');
      }
    };

    updateTime();
    const interval = setInterval(updateTime, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleCopyEmail = async () => {
    const email = portfolioData.profile.email;
    let success = false;
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(email);
        success = true;
      }
    } catch {
      success = false;
    }

    if (!success) {
      try {
        const textArea = document.createElement('textarea');
        textArea.value = email;
        textArea.style.position = 'fixed';
        textArea.style.left = '-9999px';
        document.body.appendChild(textArea);
        textArea.select();
        success = document.execCommand('copy');
        document.body.removeChild(textArea);
      } catch {
        success = false;
      }
    }

    if (success) {
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } else {
      window.location.href = `mailto:${email}`;
    }
  };

  useEffect(() => {
    let ctx = gsap.context(() => {
      gsap.from('.footer-final-scene-stage', {
        scrollTrigger: { trigger: footerRef.current, start: 'top 85%', once: true },
        y: 35, opacity: 0, duration: 0.9, ease: 'power2.out'
      });

      gsap.from('.footer-nav-colophon-bar', {
        scrollTrigger: { trigger: '.footer-nav-colophon-bar', start: 'top 92%', once: true },
        y: 20, opacity: 0, duration: 0.6, ease: 'power2.out'
      });
    }, footerRef);

    return () => ctx.revert();
  }, []);

  const scrollToTop = (e) => {
    e.preventDefault();
    if (window.lenis) {
      window.lenis.scrollTo(0, { duration: 1.2, easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)) });
    } else {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  return (
    <footer className="footer-final-scene" ref={footerRef}>
      
      {/* Living Atmospheric Dusk Canopy */}
      <div className="footer-dusk-canopy" aria-hidden="true">
        <div className="dusk-pool-emerald"></div>
        <div className="dusk-pool-lime"></div>
        <div className="dusk-pool-cyan"></div>
        <div className="dusk-grid-mesh"></div>
      </div>

      <div className="container">
        
        {/* Main Final Scene Stage (Asymmetric Constellation Architecture) */}
        <div className="footer-final-scene-stage">
          
          {/* Identity & Technical Telemetry Chamber */}
          <div className="footer-identity-monolith">
            
            <div className="identity-lead-group">
              <span className="identity-pre-kicker">// ENGINEERING ARCHITECTURE &amp; SPECIALIZATION</span>
              <h3 className="identity-focus-heading">High-Throughput Java Backend Engineering</h3>
              <p className="identity-credo-copy">
                Architecting reliable backend services, normalized relational schemas, and production-grade enterprise software.
              </p>
            </div>

            {/* Context Telemetry & IST Widget */}
            <div className="identity-telemetry-row">
              <div className="telemetry-badge-location">
                <MapPin size={13} className="loc-pin" />
                <span>Pune, Maharashtra, India</span>
              </div>

              <div className="telemetry-badge-ist">
                <span className="ist-live-ping" aria-hidden="true"></span>
                <span>IST (UTC+5:30){istTime && ` \u00B7 ${istTime}`}</span>
              </div>
            </div>

            {/* Direct Email Action Capsule */}
            <div className="identity-email-capsule">
              <span className="capsule-label">DIRECT INQUIRIES:</span>
              <div className="capsule-action-tray">
                <a href={`mailto:${portfolioData.profile.email}`} className="capsule-email-link">
                  {portfolioData.profile.email}
                </a>
                <button
                  type="button"
                  onClick={handleCopyEmail}
                  className={`capsule-copy-pill ${copied ? 'copy-success' : ''}`}
                  aria-label={copied ? "Email copied" : "Copy email address"}
                  title="Copy email to clipboard"
                >
                  {copied ? <Check size={13} aria-hidden="true" /> : <Copy size={13} aria-hidden="true" />}
                  <span>{copied ? "Copied" : "Copy"}</span>
                </button>
              </div>
            </div>

          </div>

          {/* Interactive Celestial Social Constellation (Interconnected Orbit) */}
          <div className="footer-social-constellation">
            
            <div className="constellation-header-row">
              <span className="constellation-kicker">// CONNECTED PROFILES &amp; CODEBASES</span>
              <span className="constellation-live-status">
                <span className="live-dot-glow"></span>
                <span>ACTIVE</span>
              </span>
            </div>

            {/* Animated SVG Connection Web between nodes */}
            <div className="constellation-nodes-cluster">
              
              {/* GitHub Celestial Node */}
              <a 
                href={portfolioData.links.github} 
                target="_blank" 
                rel="noopener noreferrer"
                className={`celestial-social-node node-github ${activeHoverNode === 'github' ? 'node-active' : ''}`}
                onMouseEnter={() => setActiveHoverNode('github')}
                onMouseLeave={() => setActiveHoverNode(null)}
              >
                <div className="node-icon-core">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4" />
                    <path d="M9 18c-4.51 2-5-2-7-2" />
                  </svg>
                </div>
                <div className="node-text-wrap">
                  <div className="node-platform-line">
                    <strong className="node-platform">GitHub</strong>
                    <span className="node-dot dot-github"></span>
                  </div>
                  <span className="node-handle">@Milan-Goswami</span>
                  <span className="node-caption">Software Repositories &middot; Source Code</span>
                </div>
                <ArrowUpRight size={15} className="node-arrow" aria-hidden="true" />
              </a>

              {/* LinkedIn Celestial Node */}
              <a 
                href={portfolioData.links.linkedin} 
                target="_blank" 
                rel="noopener noreferrer"
                className={`celestial-social-node node-linkedin ${activeHoverNode === 'linkedin' ? 'node-active' : ''}`}
                onMouseEnter={() => setActiveHoverNode('linkedin')}
                onMouseLeave={() => setActiveHoverNode(null)}
              >
                <div className="node-icon-core">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z" />
                    <rect x="2" y="9" width="4" height="12" />
                    <circle cx="4" cy="4" r="2" />
                  </svg>
                </div>
                <div className="node-text-wrap">
                  <div className="node-platform-line">
                    <strong className="node-platform">LinkedIn</strong>
                    <span className="node-dot dot-linkedin"></span>
                  </div>
                  <span className="node-handle">Milan Goswami</span>
                  <span className="node-caption">Professional Network &middot; Career Opportunities</span>
                </div>
                <ArrowUpRight size={15} className="node-arrow" aria-hidden="true" />
              </a>

              {/* LeetCode Celestial Node */}
              <a 
                href={portfolioData.links.leetcode} 
                target="_blank" 
                rel="noopener noreferrer"
                className={`celestial-social-node node-leetcode ${activeHoverNode === 'leetcode' ? 'node-active' : ''}`}
                onMouseEnter={() => setActiveHoverNode('leetcode')}
                onMouseLeave={() => setActiveHoverNode(null)}
              >
                <div className="node-icon-core">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <polyline points="16 18 22 12 16 6" />
                    <polyline points="8 6 2 12 8 18" />
                  </svg>
                </div>
                <div className="node-text-wrap">
                  <div className="node-platform-line">
                    <strong className="node-platform">LeetCode</strong>
                    <span className="node-dot dot-leetcode"></span>
                  </div>
                  <span className="node-handle">@pro_milan</span>
                  <span className="node-caption">Algorithms in Java &middot; Data Structures</span>
                </div>
                <ArrowUpRight size={15} className="node-arrow" aria-hidden="true" />
              </a>

            </div>

          </div>

        </div>

        {/* Closing Navigation & Colophon Bar */}
        <div className="footer-nav-colophon-bar">
          
          <nav className="footer-nav-links" aria-label="Footer navigation">
            <a href="#home">Overview</a>
            <a href="#projects">Featured Projects</a>
            <a href="#academic">Academic Progression</a>
            <a href="#learning">Currently Learning</a>
            <a href="#contact">Get in Touch</a>
          </nav>

          <div className="footer-colophon-middle">
            <span className="colophon-copyright">
              &copy; {CURRENT_YEAR} Milan Goswami. All rights reserved.
            </span>
          </div>

          <button 
            type="button" 
            onClick={scrollToTop} 
            className="footer-ascend-pill"
            aria-label="Scroll back to top"
            title="Ascend to top of page"
          >
            <ArrowUp size={14} aria-hidden="true" />
            <span>ASCEND TOP</span>
          </button>

        </div>

      </div>
    </footer>
  );
}
