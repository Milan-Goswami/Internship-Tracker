import { useState, useRef, useEffect } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { Binary, Server, Database, ArrowLeftRight, Cpu, Boxes } from 'lucide-react';
import './CurrentlyLearning.css';

gsap.registerPlugin(ScrollTrigger);

const learningNodes = [
  {
    id: 'dsa',
    index: '01',
    name: 'DSA in Java',
    stage: 'Foundation',
    accent: '#EA580C',
    glow: 'rgba(234, 88, 12, 0.35)',
    bgSoft: 'rgba(234, 88, 12, 0.10)',
    icon: Binary,
    focus: 'Algorithms & Core Java',
    keyConcepts: ['Binary Trees', 'Two Pointers', 'Dynamic Programming'],
    desktopPos: { top: '8%', left: '12%' },
    align: 'left',
    connections: ['spring']
  },
  {
    id: 'spring',
    index: '02',
    name: 'Spring Boot',
    stage: 'Backend Core',
    accent: '#16A34A',
    glow: 'rgba(22, 163, 74, 0.35)',
    bgSoft: 'rgba(22, 163, 74, 0.10)',
    icon: Server,
    focus: 'Production Service Framework',
    keyConcepts: ['IoC & DI', 'REST APIs', 'Actuator'],
    desktopPos: { top: '8%', right: '12%' },
    align: 'right',
    connections: ['dsa', 'core', 'jpa', 'rest']
  },
  {
    id: 'jpa',
    index: '03',
    name: 'Spring Data JPA',
    stage: 'Data Tier',
    accent: '#0D9488',
    glow: 'rgba(13, 148, 136, 0.35)',
    bgSoft: 'rgba(13, 148, 136, 0.10)',
    icon: Database,
    focus: 'Persistence & Relational ORM',
    keyConcepts: ['JpaRepository', 'JPQL', 'Hibernate'],
    desktopPos: { top: '44%', left: '4%' },
    align: 'left',
    connections: ['spring', 'core', 'sysdesign']
  },
  {
    id: 'rest',
    index: '04',
    name: 'REST APIs',
    stage: 'Integration',
    accent: '#4F46E5',
    glow: 'rgba(79, 70, 229, 0.35)',
    bgSoft: 'rgba(79, 70, 229, 0.10)',
    icon: ArrowLeftRight,
    focus: 'Contract-First API Architecture',
    keyConcepts: ['HTTP Contracts', 'DTO Validation', 'OpenAPI'],
    desktopPos: { top: '44%', right: '4%' },
    align: 'right',
    connections: ['spring', 'core', 'sysdesign']
  },
  {
    id: 'sysdesign',
    index: '05',
    name: 'System Design',
    stage: 'Enterprise Scale',
    accent: '#7C3AED',
    glow: 'rgba(124, 58, 237, 0.35)',
    bgSoft: 'rgba(124, 58, 237, 0.10)',
    icon: Cpu,
    focus: 'Scalable Distributed Topologies',
    keyConcepts: ['Redis', 'DB Indexing', 'CAP Theorem'],
    desktopPos: { bottom: '8%', left: '14%' },
    align: 'left',
    connections: ['jpa', 'rest', 'microservices']
  },
  {
    id: 'microservices',
    index: '06',
    name: 'Microservices',
    stage: 'Distributed',
    accent: '#0284C7',
    glow: 'rgba(2, 132, 199, 0.35)',
    bgSoft: 'rgba(2, 132, 199, 0.10)',
    icon: Boxes,
    focus: 'Decoupled Enterprise Services',
    keyConcepts: ['Domain Boundaries', 'API Gateway', 'Discovery'],
    desktopPos: { bottom: '8%', right: '14%' },
    align: 'right',
    connections: ['sysdesign']
  }
];

export default function CurrentlyLearning() {
  const [activeNodeId, setActiveNodeId] = useState(null);
  const sectionRef = useRef(null);

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) return;

    let ctx = gsap.context(() => {
      gsap.from('.constellation-editorial-header', {
        scrollTrigger: { trigger: sectionRef.current, start: 'top 85%', once: true },
        y: 25, opacity: 0, duration: 0.8, ease: 'power3.out'
      });

      gsap.from('.constellation-stage-svg', {
        scrollTrigger: { trigger: sectionRef.current, start: 'top 85%', once: true },
        opacity: 0, scale: 0.95, duration: 1.1, ease: 'power2.out'
      });

      gsap.fromTo('.constellation-node-anchor',
        { scale: 0.8, opacity: 0 },
        {
          scrollTrigger: { trigger: sectionRef.current, start: 'top 85%', once: true },
          scale: 1,
          opacity: 1,
          duration: 0.7,
          stagger: 0.08,
          ease: 'back.out(1.4)',
          clearProps: 'opacity,transform'
        }
      );
    }, sectionRef);

    return () => ctx.revert();
  }, []);

  // Determine if a connection is active based on hovered node
  const isConnectionActive = (n1, n2) => {
    if (!activeNodeId) return false;
    const active = learningNodes.find(n => n.id === activeNodeId);
    if (!active) return false;
    if (active.id === n1 && active.connections.includes(n2)) return true;
    if (active.id === n2 && active.connections.includes(n1)) return true;
    if (active.id === n1 || active.id === n2) return true;
    return false;
  };

  return (
    <section id="currently-learning" className="learning-constellation-space section" ref={sectionRef}>
      
      {/* Background Living Ambient Light */}
      <div className="constellation-ambient-aura" aria-hidden="true">
        <div className="constellation-orb orb-emerald"></div>
        <div className="constellation-orb orb-violet"></div>
      </div>

      <div className="container">
        
        {/* Section Editorial Header */}
        <div className="constellation-editorial-header text-center">
          <div className="constellation-kicker">
            <span className="constellation-pulse-dot" aria-hidden="true"></span>
            ACTIVE KNOWLEDGE EXPANSION
          </div>
          <h2 className="display-2 mt-2">Currently Learning</h2>
          <p className="constellation-lead-text">
            A topological map of core engineering technologies and distributed paradigms I am actively connecting.
          </p>
        </div>

        {/* ======================================================== */}
        {/* DESKTOP ARCHITECTURAL KNOWLEDGE MAP (Constellation Orbit) */}
        {/* ======================================================== */}
        <div 
          className={`desktop-constellation-stage ${activeNodeId ? 'has-active-node' : ''}`}
          role="region" 
          aria-label="Interactive backend knowledge constellation map"
        >
          
          {/* SVG Vector Coordinate Grid, Orbital Rings & Pathways */}
          <svg 
            className="constellation-stage-svg" 
            viewBox="0 0 1000 620" 
            fill="none" 
            preserveAspectRatio="xMidYMid meet"
            aria-hidden="true"
          >
            <defs>
              {/* Radial Core Halo Gradient */}
              <radialGradient id="coreHaloGrad" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stopColor="#16A34A" stopOpacity="0.18" />
                <stop offset="60%" stopColor="#16A34A" stopOpacity="0.04" />
                <stop offset="100%" stopColor="#16A34A" stopOpacity="0" />
              </radialGradient>

              {/* Linear Stream Gradients */}
              <linearGradient id="streamDsaSpring" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#EA580C" />
                <stop offset="100%" stopColor="#16A34A" />
              </linearGradient>
              <linearGradient id="streamSpringCore" x1="50%" y1="0%" x2="50%" y2="100%">
                <stop offset="0%" stopColor="#16A34A" />
                <stop offset="100%" stopColor="#10B981" />
              </linearGradient>
              <linearGradient id="streamCoreJpa" x1="100%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#10B981" />
                <stop offset="100%" stopColor="#0D9488" />
              </linearGradient>
              <linearGradient id="streamCoreRest" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#10B981" />
                <stop offset="100%" stopColor="#4F46E5" />
              </linearGradient>
              <linearGradient id="streamJpaSys" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#0D9488" />
                <stop offset="100%" stopColor="#7C3AED" />
              </linearGradient>
              <linearGradient id="streamRestSys" x1="100%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#4F46E5" />
                <stop offset="100%" stopColor="#7C3AED" />
              </linearGradient>
              <linearGradient id="streamSysMicro" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#7C3AED" />
                <stop offset="100%" stopColor="#0284C7" />
              </linearGradient>
            </defs>

            {/* Concentric Architectural Orbital Rings */}
            <circle cx="500" cy="310" r="120" className="constellation-orbit-ring ring-inner" />
            <circle cx="500" cy="310" r="230" className="constellation-orbit-ring ring-mid" />
            <circle cx="500" cy="310" r="350" className="constellation-orbit-ring ring-outer" />

            {/* Subtle Coordinate Axis Crosshairs */}
            <line x1="500" y1="40" x2="500" y2="580" className="constellation-axis-line" />
            <line x1="80" y1="310" x2="920" y2="310" className="constellation-axis-line" />

            {/* Central Nexus Halo */}
            <circle cx="500" cy="310" r="95" fill="url(#coreHaloGrad)" />

            {/* ======================================================== */}
            {/* KINETIC CONSTELLATION PATHWAYS                           */}
            {/* ======================================================== */}
            
            {/* Path 1: DSA (180, 100) -> Spring Boot (820, 100) */}
            <path 
              d="M 220 105 C 380 70, 620 70, 780 105" 
              className={`constellation-path-base ${isConnectionActive('dsa', 'spring') ? 'path-active' : ''}`}
            />
            <path 
              d="M 220 105 C 380 70, 620 70, 780 105" 
              stroke="url(#streamDsaSpring)" 
              className={`constellation-path-stream ${isConnectionActive('dsa', 'spring') ? 'stream-active' : ''}`}
            />

            {/* Path 2: Spring Boot (780, 105) -> Central Core (500, 310) */}
            <path 
              d="M 780 105 C 680 180, 580 230, 500 310" 
              className={`constellation-path-base ${isConnectionActive('spring', 'core') ? 'path-active' : ''}`}
            />
            <path 
              d="M 780 105 C 680 180, 580 230, 500 310" 
              stroke="url(#streamSpringCore)" 
              className={`constellation-path-stream ${isConnectionActive('spring', 'core') ? 'stream-active' : ''}`}
            />

            {/* Path 3: Central Core (500, 310) -> JPA (160, 310) */}
            <path 
              d="M 500 310 C 380 300, 260 305, 160 310" 
              className={`constellation-path-base ${isConnectionActive('jpa', 'core') ? 'path-active' : ''}`}
            />
            <path 
              d="M 500 310 C 380 300, 260 305, 160 310" 
              stroke="url(#streamCoreJpa)" 
              className={`constellation-path-stream ${isConnectionActive('jpa', 'core') ? 'stream-active' : ''}`}
            />

            {/* Path 4: Central Core (500, 310) -> REST APIs (840, 310) */}
            <path 
              d="M 500 310 C 620 300, 740 305, 840 310" 
              className={`constellation-path-base ${isConnectionActive('rest', 'core') ? 'path-active' : ''}`}
            />
            <path 
              d="M 500 310 C 620 300, 740 305, 840 310" 
              stroke="url(#streamCoreRest)" 
              className={`constellation-path-stream ${isConnectionActive('rest', 'core') ? 'stream-active' : ''}`}
            />

            {/* Path 5: JPA (160, 310) -> System Design (240, 515) */}
            <path 
              d="M 160 310 C 170 410, 190 470, 240 515" 
              className={`constellation-path-base ${isConnectionActive('jpa', 'sysdesign') ? 'path-active' : ''}`}
            />
            <path 
              d="M 160 310 C 170 410, 190 470, 240 515" 
              stroke="url(#streamJpaSys)" 
              className={`constellation-path-stream ${isConnectionActive('jpa', 'sysdesign') ? 'stream-active' : ''}`}
            />

            {/* Path 6: REST APIs (840, 310) -> System Design (240, 515) */}
            <path 
              d="M 840 310 C 700 440, 440 480, 240 515" 
              className={`constellation-path-base ${isConnectionActive('rest', 'sysdesign') ? 'path-active' : ''}`}
            />
            <path 
              d="M 840 310 C 700 440, 440 480, 240 515" 
              stroke="url(#streamRestSys)" 
              className={`constellation-path-stream ${isConnectionActive('rest', 'sysdesign') ? 'stream-active' : ''}`}
            />

            {/* Path 7: System Design (240, 515) -> Microservices (760, 515) */}
            <path 
              d="M 240 515 C 400 545, 600 545, 760 515" 
              className={`constellation-path-base ${isConnectionActive('sysdesign', 'microservices') ? 'path-active' : ''}`}
            />
            <path 
              d="M 240 515 C 400 545, 600 545, 760 515" 
              stroke="url(#streamSysMicro)" 
              className={`constellation-path-stream ${isConnectionActive('sysdesign', 'microservices') ? 'stream-active' : ''}`}
            />
          </svg>

          {/* ======================================================== */}
          {/* CENTRAL NEXUS ANCHOR (Ecosystem Focal Lockup)           */}
          {/* ======================================================== */}
          <div className="constellation-central-core" aria-hidden="true">
            <div className="core-orbital-ring"></div>
            <div className="core-identity-lockup">
              <span className="core-mono-kicker">ARCHITECTURAL CORE</span>
              <span className="core-main-title">Java Backend</span>
              <span className="core-sub-discipline">ECOSYSTEM TOPOLOGY</span>
            </div>
          </div>

          {/* ======================================================== */}
          {/* 6 FLOATING CONSTELLATION NODES & EDITORIAL TYPOGRAPHY    */}
          {/* ======================================================== */}
          {learningNodes.map((node) => {
            const IconComp = node.icon;
            const isHovered = activeNodeId === node.id;

            return (
              <div
                key={node.id}
                className={`constellation-node-anchor align-${node.align} ${isHovered ? 'node-focused' : ''}`}
                style={{
                  ...node.desktopPos,
                  '--node-accent': node.accent,
                  '--node-glow': node.glow,
                  '--node-bg-soft': node.bgSoft
                }}
                onMouseEnter={() => setActiveNodeId(node.id)}
                onMouseLeave={() => setActiveNodeId(null)}
                tabIndex={0}
                onFocus={() => setActiveNodeId(node.id)}
                onBlur={() => setActiveNodeId(null)}
                role="article"
                aria-label={`${node.name} - ${node.stage}`}
              >
                {/* Luminous Node Orb Disc (No box!) */}
                <div className="node-orb-cluster">
                  <div className="node-orbital-pulse" aria-hidden="true"></div>
                  <div className="node-orb-disc" aria-hidden="true">
                    <IconComp size={17} className="node-icon-glyph" />
                    <span className="node-index-pip">/{node.index}</span>
                  </div>
                </div>

                {/* Free-Floating Editorial Typography Cluster (No container card!) */}
                <div className="node-editorial-cluster">
                  <div className="node-meta-kicker">
                    <span className="node-kicker-text">{node.stage}</span>
                  </div>

                  <h3 className="node-title-heading">{node.name}</h3>
                  <p className="node-focus-phrase">{node.focus}</p>

                  <div className="node-concepts-chain" aria-label="Key concepts">
                    {node.keyConcepts.map((concept, cIdx) => (
                      <span key={cIdx} className="node-concept-atom">
                        {concept}
                        {cIdx < node.keyConcepts.length - 1 && (
                          <span className="atom-bullet" aria-hidden="true">·</span>
                        )}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            );
          })}

        </div>

        {/* ======================================================== */}
        {/* MOBILE VERTICAL WINDING CONSTELLATION (< 768px)          */}
        {/* ======================================================== */}
        <div 
          className="mobile-constellation-track"
          role="region" 
          aria-label="Mobile vertical knowledge constellation"
        >
          {/* Continuous Undulating Winding Constellation SVG Spine */}
          <svg 
            className="mobile-winding-spine-svg" 
            viewBox="0 0 40 780" 
            fill="none" 
            preserveAspectRatio="none"
            aria-hidden="true"
          >
            <path 
              d="M 20 0 C 35 130, 5 260, 20 390 C 35 520, 5 650, 20 780" 
              className="mobile-spine-base" 
            />
            <path 
              d="M 20 0 C 35 130, 5 260, 20 390 C 35 520, 5 650, 20 780" 
              className="mobile-spine-beam" 
            />
          </svg>

          {/* 6 Sequential Nodes along the Winding Path (No rectangular cards!) */}
          <div className="mobile-constellation-nodes">
            {learningNodes.map((node, nIdx) => {
              const IconComp = node.icon;
              const isEven = nIdx % 2 === 1;

              return (
                <div 
                  key={node.id} 
                  className={`mobile-node-item ${isEven ? 'shift-right' : 'shift-left'}`}
                  style={{
                    '--node-accent': node.accent,
                    '--node-glow': node.glow,
                    '--node-bg-soft': node.bgSoft
                  }}
                >
                  {/* Glowing Node Joint on the Spine */}
                  <div className="mobile-node-joint" aria-hidden="true">
                    <div className="joint-outer-pulse"></div>
                    <div className="joint-core-disc">
                      <IconComp size={15} />
                    </div>
                  </div>

                  {/* Free-Floating Editorial Content Cluster beside Node */}
                  <div className="mobile-node-body">
                    <div className="mobile-meta-kicker">
                      <span className="mobile-index">/{node.index}</span>
                      <span className="mobile-stage">{node.stage}</span>
                    </div>

                    <h3 className="mobile-title">{node.name}</h3>
                    <p className="mobile-focus">{node.focus}</p>

                    <div className="mobile-concepts-row">
                      {node.keyConcepts.map((concept, cIdx) => (
                        <span key={cIdx} className="mobile-concept-atom">
                          {concept}
                          {cIdx < node.keyConcepts.length - 1 && (
                            <span className="atom-bullet" aria-hidden="true">·</span>
                          )}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

        </div>

      </div>
    </section>
  );
}
