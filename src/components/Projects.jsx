import { useState, useRef, useEffect, useCallback } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { ArrowUpRight, Maximize2, X, ChevronLeft, ChevronRight, Layers, ShieldCheck, CheckCircle2, Lock } from 'lucide-react';
import { TechBadge } from './TechIcons';
import './Projects.css';

gsap.registerPlugin(ScrollTrigger);

const projectsList = [
  {
    id: 'nova',
    name: 'Nova Online Examination System',
    typeBadge: 'SHIPPED JAVA WEB APPLICATION',
    themeAccent: '#10B981',
    themeGlow: 'rgba(16, 185, 129, 0.18)',
    tagline: 'High-Throughput Assessment Engine',
    leadHeadline: 'Java web platform with automated scoring & strict role authorization.',
    summary: 'Architected with MVC JSP/Servlets and Apache Tomcat 9. Features teacher/student role separation, dynamic question bank management, and real-time timed test session enforcement.',
    highlights: [
      'Role-Based Access Control (Teacher vs Student Separation)',
      'Automated Scoring Engine with Immediate Grading Feedback',
      'Session State & Countdown Enforcement on Tomcat 9'
    ],
    url: 'http://localhost:8080/nova-exam-system',
    repoUrl: 'https://github.com/Milan-Goswami/NovaOnlineExamSystem',
    keyStack: ['Java', 'JSP & Servlets', 'MySQL', 'Apache Tomcat', 'Maven'],
    screens: [
      {
        title: 'Question & Exam Management',
        src: '/assets/nova-dashboard.png',
        desc: 'Administrative hub for test creation, question CRUD, and exam scheduling.'
      },
      {
        title: 'Live Timed Exam Interface',
        src: '/assets/nova-exam-interface.png',
        desc: 'Real-time student test session with active countdown enforcement and automated scoring.'
      },
      {
        title: 'Security & Access Portal',
        src: '/assets/nova-home.png',
        desc: 'Role-based authentication gateway separating student testing from teacher controls.'
      }
    ]
  },
  {
    id: 'insurance',
    name: 'Insurance Management System',
    typeBadge: 'ENTERPRISE J2EE APPLICATION',
    themeAccent: '#0284C7',
    themeGlow: 'rgba(2, 132, 199, 0.18)',
    tagline: 'Policy Lifecycle & Claims Engine',
    leadHeadline: 'Mission-critical policy operations & customer records platform.',
    summary: 'Built on a 3-tier enterprise architecture backed by 3NF normalized MySQL schemas. Implements strict JDBC ACID transaction bounds for policy issuance, premium calculations, and customer KYC records.',
    highlights: [
      '3-Tier J2EE Architecture with DAO Pattern & PreparedStatements',
      'ACID-Bound JDBC Transactions Preventing Data Orphans',
      'Customer KYC Directory with Policy & Claims History'
    ],
    url: 'http://localhost:8080/insurance-system',
    repoUrl: 'https://github.com/Milan-Goswami/Insurance-Management-System',
    keyStack: ['Java', 'J2EE Servlets', 'MySQL', 'JDBC ACID', 'Tomcat'],
    screens: [
      {
        title: 'Central Operations Hub',
        src: '/assets/insurance-dashboard.png',
        desc: 'Administrative operations hub displaying active policies, customer volumes, and claims.'
      },
      {
        title: 'Customer KYC Directory',
        src: '/assets/insurance-customers.png',
        desc: 'Customer management directory linking policy records through normalized relational keys.'
      },
      {
        title: 'Policy Lifecycle Engine',
        src: '/assets/insurance-home.png',
        desc: 'Policy enrollment and premium calculation with database transaction enforcement.'
      }
    ]
  }
];

export default function Projects() {
  const sectionRef = useRef(null);
  const [activeProjectIdx, setActiveProjectIdx] = useState(0);
  const [screenIndices, setScreenIndices] = useState({ nova: 0, insurance: 0 });

  // Lightbox Modal state
  const [lightbox, setLightbox] = useState({
    isOpen: false,
    projectId: 'nova',
    screenIndex: 0
  });

  const activeProject = projectsList[activeProjectIdx];
  const activeScreenIdx = screenIndices[activeProject.id] || 0;
  const activeScreen = activeProject.screens[activeScreenIdx];

  const handleScreenChange = (screenIdx) => {
    setScreenIndices(prev => ({
      ...prev,
      [activeProject.id]: screenIdx
    }));
  };

  const openLightbox = (projectId, screenIndex = 0) => {
    setLightbox({ isOpen: true, projectId, screenIndex });
  };

  const closeLightbox = useCallback(() => {
    setLightbox(prev => ({ ...prev, isOpen: false }));
  }, []);

  const nextLightboxScreen = useCallback((e) => {
    if (e) e.stopPropagation();
    const project = projectsList.find(p => p.id === lightbox.projectId) || projectsList[0];
    setLightbox(prev => ({
      ...prev,
      screenIndex: (prev.screenIndex + 1) % project.screens.length
    }));
  }, [lightbox.projectId]);

  const prevLightboxScreen = useCallback((e) => {
    if (e) e.stopPropagation();
    const project = projectsList.find(p => p.id === lightbox.projectId) || projectsList[0];
    setLightbox(prev => ({
      ...prev,
      screenIndex: (prev.screenIndex - 1 + project.screens.length) % project.screens.length
    }));
  }, [lightbox.projectId]);

  useEffect(() => {
    if (!lightbox.isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') closeLightbox();
      if (e.key === 'ArrowRight') nextLightboxScreen();
      if (e.key === 'ArrowLeft') prevLightboxScreen();
    };

    window.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'auto';
    };
  }, [lightbox.isOpen, closeLightbox, nextLightboxScreen, prevLightboxScreen]);

  useEffect(() => {
    let ctx = gsap.context(() => {
      gsap.from('.showroom-header', {
        scrollTrigger: { trigger: sectionRef.current, start: 'top 85%', once: true },
        y: 30, opacity: 0, duration: 0.8, ease: 'power3.out'
      });

      gsap.from('.showroom-stage-card', {
        scrollTrigger: { trigger: '.showroom-stage-card', start: 'top 85%', once: true },
        y: 35, opacity: 0, duration: 0.9, ease: 'power2.out'
      });
    }, sectionRef);

    return () => ctx.revert();
  }, []);

  const currentLightboxProject = projectsList.find(p => p.id === lightbox.projectId) || projectsList[0];
  const currentLightboxScreen = currentLightboxProject.screens[lightbox.screenIndex];

  return (
    <section id="projects" className="projects-showroom-section section" ref={sectionRef}>
      <div className="container">
        
        {/* Section Header */}
        <div className="showroom-header text-center">
          <div className="showroom-eyebrow">
            <span className="eyebrow-bullet"></span>
            SHIPPED SOFTWARE PRODUCTS
          </div>
          <h2 className="display-2 mt-2">Featured Applications</h2>
          <p className="showroom-subtitle">
            Authentic full-stack Java systems built with normalized MySQL relational databases and production architectures.
          </p>

          {/* High-Contrast Editorial Project Selector */}
          <div className="showroom-selector-bar" role="tablist" aria-label="Select software project">
            {projectsList.map((proj, idx) => (
              <button
                key={proj.id}
                type="button"
                role="tab"
                aria-selected={idx === activeProjectIdx}
                className={`selector-tab ${idx === activeProjectIdx ? 'tab-selected' : ''}`}
                onClick={() => setActiveProjectIdx(idx)}
              >
                <span className="selector-num">0{idx + 1}</span>
                <span className="selector-name">{proj.name}</span>
              </button>
            ))}
          </div>
        </div>

        {/* BALANCED PRODUCT SHOWCASE STAGE */}
        <div 
          className="showroom-stage-card"
          style={{ 
            '--proj-accent': activeProject.themeAccent,
            '--proj-glow': activeProject.themeGlow
          }}
        >
          
          {/* Top Bar: Category Pill & Live Localhost URL */}
          <div className="stage-top-bar">
            <div className="stage-meta-left">
              <span className="stage-type-badge">
                <ShieldCheck size={13} aria-hidden="true" />
                {activeProject.typeBadge}
              </span>
              <span className="stage-tagline-text">{activeProject.tagline}</span>
            </div>
            <div className="stage-meta-right">
              <span className="stage-url-indicator">
                <span className="url-dot"></span>
                {activeProject.url}
              </span>
            </div>
          </div>

          {/* Integrated Split Studio Grid (Visual + Info Harmoniously Balanced) */}
          <div className="stage-studio-grid">
            
            {/* Left Column: Technical Narrative & Key Specifications */}
            <div className="stage-info-pane">
              <div className="info-title-block">
                <h3 className="stage-project-title">{activeProject.name}</h3>
                <p className="stage-lead-headline">{activeProject.leadHeadline}</p>
                <p className="stage-summary-text">{activeProject.summary}</p>
              </div>

              {/* Architectural Highlights Checklist with colored accents */}
              <div className="stage-highlights-list" aria-label="Architectural Highlights">
                {activeProject.highlights.map((item, hIdx) => (
                  <div key={hIdx} className="stage-highlight-item">
                    <CheckCircle2 size={14} className="highlight-check-icon" aria-hidden="true" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>

              {/* Key Tech Stack Badges */}
              <div className="stage-stack-group" aria-label="Core technologies used">
                {activeProject.keyStack.map(tech => (
                  <TechBadge key={tech} name={tech} size="sm" />
                ))}
              </div>

              {/* Action Buttons */}
              <div className="stage-actions-row">
                <a 
                  href={activeProject.repoUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="stage-primary-btn"
                >
                  <span>View GitHub Repository</span>
                  <ArrowUpRight size={15} aria-hidden="true" />
                </a>

                <button
                  type="button"
                  className="stage-secondary-btn"
                  onClick={() => openLightbox(activeProject.id, activeScreenIdx)}
                  title="Inspect Fullscreen"
                >
                  <Maximize2 size={14} aria-hidden="true" />
                  <span>Inspect Screenshots (3)</span>
                </button>
              </div>
            </div>

            {/* Right Column: Balanced Application Studio Viewport with Depth & Perspective */}
            <div className="stage-visual-pane">
              
              {/* Subtle Project Accent Light Glow */}
              <div className="visual-stage-ambient-glow" aria-hidden="true"></div>

              {/* Sophisticated Framed Browser Device */}
              <div className="studio-browser-frame">
                
                {/* Browser Chrome Header */}
                <div className="studio-chrome-bar">
                  <div className="chrome-dots" aria-hidden="true">
                    <span className="dot red"></span>
                    <span className="dot yellow"></span>
                    <span className="dot green"></span>
                  </div>

                  <div className="chrome-address-pill">
                    <Lock size={11} className="lock-icon" aria-hidden="true" />
                    <span className="address-text">{activeProject.url}</span>
                  </div>

                  <button
                    type="button"
                    className="chrome-expand-btn"
                    onClick={() => openLightbox(activeProject.id, activeScreenIdx)}
                    aria-label="Inspect screenshot fullscreen"
                    title="Inspect Fullscreen"
                  >
                    <Maximize2 size={13} aria-hidden="true" />
                  </button>
                </div>

                {/* Scaled High-Fidelity Screenshot Canvas */}
                <div 
                  className="studio-screen-viewport"
                  onClick={() => openLightbox(activeProject.id, activeScreenIdx)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => { if (e.key === 'Enter') openLightbox(activeProject.id, activeScreenIdx); }}
                  aria-label="Click to inspect high-resolution screenshot"
                >
                  <img 
                    key={`${activeProject.id}-${activeScreenIdx}`}
                    src={activeScreen.src} 
                    alt={`${activeProject.name} — ${activeScreen.title}`}
                    className="studio-screen-img"
                    loading="eager"
                  />
                  <div className="studio-screen-hover-prompt">
                    <span className="hover-prompt-pill">
                      <Maximize2 size={13} aria-hidden="true" />
                      Click for Fullscreen
                    </span>
                  </div>
                </div>

                {/* Multi-Screen Switcher Tabs */}
                <div className="studio-screen-switcher" role="tablist" aria-label="Switch screen views">
                  {activeProject.screens.map((screen, sIdx) => (
                    <button
                      key={sIdx}
                      type="button"
                      role="tab"
                      aria-selected={sIdx === activeScreenIdx}
                      className={`studio-switch-btn ${sIdx === activeScreenIdx ? 'switch-active' : ''}`}
                      onClick={() => handleScreenChange(sIdx)}
                    >
                      <span className="switch-number">0{sIdx + 1}</span>
                      <span className="switch-name">{screen.title.split(' ')[0]} {screen.title.split(' ')[1] || ''}</span>
                    </button>
                  ))}
                </div>

              </div>

              {/* Caption Tag */}
              <div className="studio-active-caption">
                <Layers size={13} className="caption-icon" aria-hidden="true" />
                <span className="caption-text">{activeScreen.desc}</span>
              </div>

            </div>

          </div>

        </div>

      </div>

      {/* FULLSCREEN LIGHTBOX MODAL (Preserved with full fidelity) */}
      {lightbox.isOpen && (
        <div 
          className="project-lightbox"
          role="dialog"
          aria-modal="true"
          aria-label={`Fullscreen preview of ${currentLightboxProject.name}`}
          onClick={closeLightbox}
        >
          <div className="lightbox-content" onClick={(e) => e.stopPropagation()}>
            
            <div className="lightbox-header">
              <div className="lightbox-title-wrap">
                <span className="lightbox-live-dot" aria-hidden="true"></span>
                <h4 className="lightbox-title">{currentLightboxProject.name}</h4>
                <span className="lightbox-screen-count">
                  View {lightbox.screenIndex + 1} of {currentLightboxProject.screens.length}
                </span>
              </div>
              
              <button 
                type="button" 
                className="lightbox-close-btn"
                onClick={closeLightbox}
                aria-label="Close fullscreen view (Escape)"
                title="Close (Escape)"
              >
                <X size={20} aria-hidden="true" />
              </button>
            </div>

            <div className="lightbox-image-stage">
              <button 
                type="button" 
                className="lightbox-nav-btn prev"
                onClick={prevLightboxScreen}
                aria-label="Previous screenshot (Left arrow)"
                title="Previous (Left arrow)"
              >
                <ChevronLeft size={24} aria-hidden="true" />
              </button>

              <div className="lightbox-image-container">
                <img 
                  src={currentLightboxScreen.src} 
                  alt={`${currentLightboxProject.name} - ${currentLightboxScreen.title}`}
                  className="lightbox-main-img" 
                />
              </div>

              <button 
                type="button" 
                className="lightbox-nav-btn next"
                onClick={nextLightboxScreen}
                aria-label="Next screenshot (Right arrow)"
                title="Next (Right arrow)"
              >
                <ChevronRight size={24} aria-hidden="true" />
              </button>
            </div>

            <div className="lightbox-footer">
              <div className="lightbox-caption-col">
                <strong className="lightbox-caption-title">{currentLightboxScreen.title}:</strong>
                <span className="lightbox-caption-desc">{currentLightboxScreen.desc}</span>
              </div>

              <div className="lightbox-thumbnails">
                {currentLightboxProject.screens.map((screen, sIdx) => (
                  <button
                    key={sIdx}
                    type="button"
                    className={`lightbox-thumb-btn ${sIdx === lightbox.screenIndex ? 'thumb-active' : ''}`}
                    onClick={() => setLightbox(prev => ({ ...prev, screenIndex: sIdx }))}
                    aria-label={`Switch to ${screen.title}`}
                  >
                    <img src={screen.src} alt="" aria-hidden="true" />
                  </button>
                ))}
              </div>
            </div>

          </div>
        </div>
      )}

    </section>
  );
}
