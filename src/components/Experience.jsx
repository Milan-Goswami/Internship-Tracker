import { useState, useRef, useEffect } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { GraduationCap, ArrowRight, Award, CheckCircle2, Sparkles, Terminal, ShieldCheck, ExternalLink } from 'lucide-react';
import CertificateModal from './CertificateModal';

// Authentic certificate assets
import javaBasicCert from '../assets/Java_basic_certificate.png';
import dsaCert from '../assets/Certificate of Appreciation_page-0001.jpg';
import intelUnnatiCert from '../assets/Intel_unnati_certificate.png';
import innohackCert from '../assets/innohack_milan_certificat.png';
import innixoCert from '../assets/INNIXO_hackathon_certifictae.jpg';

import './Experience.css';

gsap.registerPlugin(ScrollTrigger);

const verifiedCertifications = [
  {
    id: 'java-basic',
    name: 'Java (Basic)',
    issuer: 'HackerRank',
    credentialId: 'F73C10A493B6',
    recipient: 'Milan Goswami',
    type: 'Skill Verified',
    accentColor: '#16A34A',
    accentLight: 'rgba(22, 163, 74, 0.12)',
    asset: javaBasicCert
  },
  {
    id: 'dsa-alpha',
    name: 'DSA with Java',
    issuer: 'Apna College (Alpha)',
    credentialId: '6a523c913ed3668974013c4e',
    recipient: 'Milan Goswami',
    type: 'Course Completion',
    accentColor: '#E76F00',
    accentLight: 'rgba(231, 111, 0, 0.12)',
    asset: dsaCert
  },
  {
    id: 'intel-unnati',
    name: 'Intel Unnati AI Program',
    issuer: 'Intel Corporation & DPU',
    recipient: 'Milan Ashokgiri Goswami',
    type: 'Technical Track',
    accentColor: '#2563EB',
    accentLight: 'rgba(37, 99, 235, 0.12)',
    asset: intelUnnatiCert
  },
  {
    id: 'innohack',
    name: 'InnoHack 2.0 Hackathon',
    issuer: 'Dr. D. Y. Patil Vidyapeeth',
    recipient: 'Milan Goswami',
    type: 'Competition',
    accentColor: '#8B5CF6',
    accentLight: 'rgba(139, 92, 246, 0.12)',
    asset: innohackCert
  },
  {
    id: 'innixo',
    name: 'INNIXO Hackathon',
    issuer: 'Unstop & DPU (Hacksphere 1.0)',
    recipient: 'Milan Goswami',
    type: 'Hackathon',
    accentColor: '#D97706',
    accentLight: 'rgba(217, 119, 6, 0.12)',
    asset: innixoCert
  }
];

export default function Experience() {
  const [hoveredMilestone, setHoveredMilestone] = useState(null);
  const [selectedCertificate, setSelectedCertificate] = useState(null);
  const sectionRef = useRef(null);

  useEffect(() => {
    let ctx = gsap.context(() => {
      // 1. Heading reveal
      gsap.from('.academic-editorial-header', {
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top 85%',
          once: true
        },
        y: 30,
        opacity: 0,
        duration: 0.85,
        ease: 'power3.out'
      });

      // 2. Dual milestones spatial reveal
      gsap.from('.milestone-bca', {
        scrollTrigger: {
          trigger: '.academic-spatial-horizon',
          start: 'top 85%',
          once: true
        },
        x: -35,
        opacity: 0,
        duration: 0.85,
        ease: 'power3.out'
      });

      gsap.from('.academic-central-trajectory', {
        scrollTrigger: {
          trigger: '.academic-spatial-horizon',
          start: 'top 85%',
          once: true
        },
        scale: 0.8,
        opacity: 0,
        duration: 0.75,
        ease: 'power2.out'
      });

      gsap.from('.milestone-mca', {
        scrollTrigger: {
          trigger: '.academic-spatial-horizon',
          start: 'top 85%',
          once: true
        },
        x: 35,
        opacity: 0,
        duration: 0.85,
        ease: 'power3.out'
      });

      // 3. Evidence deck reveal
      gsap.from('.academic-evidence-deck', {
        scrollTrigger: {
          trigger: '.academic-evidence-deck',
          start: 'top 90%',
          once: true
        },
        y: 20,
        opacity: 0,
        duration: 0.7,
        ease: 'power2.out'
      });
    }, sectionRef);

    return () => ctx.revert();
  }, []);

  return (
    <section id="academic" className="academic-spatial-section section" ref={sectionRef}>
      
      {/* Cohesive Living Atmosphere */}
      <div className="academic-ambient-atmosphere" aria-hidden="true">
        <div className="atmosphere-bloom-bca"></div>
        <div className="atmosphere-bloom-mca"></div>
        <div className="academic-coordinates-grid"></div>
      </div>

      <div className="container">
        
        {/* Section Header */}
        <div className="academic-editorial-header text-center">
          <div className="academic-kicker">
            <span className="kicker-glow-dot"></span>
            FORMAL COMPUTER SCIENCE FOUNDATIONS
          </div>
          <h2 className="display-2 mt-2">Academic Progression</h2>
          <p className="academic-mission-lead">
            A continuous trajectory from foundational undergraduate computer science into advanced postgraduate enterprise engineering.
          </p>
        </div>

        {/* Integrated Academic Monument: Dual Milestones + Seamless Central Trajectory + Integrated Evidence Deck */}
        <div className="academic-monument-card">
          
          {/* Top Horizon: BCA Milestone · Central Stream · MCA Milestone */}
          <div className="academic-spatial-horizon">
            
            {/* ======================================================== */}
            {/* MILESTONE 01: BCA FOUNDATION */}
            {/* ======================================================== */}
            <div 
              className={`academic-milestone-chamber milestone-bca ${hoveredMilestone === 'BCA' ? 'milestone-focused' : ''}`}
              onMouseEnter={() => setHoveredMilestone('BCA')}
              onMouseLeave={() => setHoveredMilestone(null)}
            >
              <div className="milestone-accent-border border-bca" aria-hidden="true"></div>
              <div className="milestone-monogram-watermark watermark-bca" aria-hidden="true">BCA</div>

              {/* Milestone Anchor */}
              <div className="milestone-anchor-header">
                <div className="bca-seal-apparatus">
                  <div className="bca-seal-orbit-ring" aria-hidden="true"></div>
                  <div className="bca-seal-medallion">
                    <GraduationCap size={28} className="bca-seal-cap" aria-hidden="true" />
                    <span className="bca-seal-monogram">BCA</span>
                  </div>
                </div>

                <div className="milestone-meta-col">
                  <span className="milestone-stage-tag tag-bca">
                    <ShieldCheck size={12} />
                    <span>FOUNDATION CREDENTIAL</span>
                  </span>
                  <div className="bca-status-pill">
                    <CheckCircle2 size={13} className="pill-check" />
                    <span>DEGREE CONFERRED</span>
                  </div>
                  <span className="bca-period-display">2022 &mdash; 2025</span>
                </div>
              </div>

              {/* Core Typography */}
              <div className="milestone-body">
                <h3 className="milestone-degree-title bca-title">
                  Bachelor of Computer Applications
                </h3>

                <div className="milestone-institution-row">
                  <strong className="inst-name">Sutex Bank College of Computer Applications &amp; Science</strong>
                  <span className="inst-affil">Veer Narmad South Gujarat University (VNSGU) &middot; Surat</span>
                </div>

                <div className="milestone-summary-capsules">
                  <span className="summary-pill">Java OOP &amp; Paradigms</span>
                  <span className="summary-pill">Data Structures</span>
                  <span className="summary-pill">MySQL Relational Schemas</span>
                  <span className="summary-pill">MVC Architecture</span>
                </div>
              </div>

            </div>

            {/* ======================================================== */}
            {/* SEAMLESS CENTRAL TRAJECTORY CONNECTOR (Zero awkward black bar) */}
            {/* ======================================================== */}
            <div className="academic-central-trajectory" aria-hidden="true">
              <svg className="trajectory-flow-svg" viewBox="0 0 160 80" fill="none">
                <defs>
                  <linearGradient id="journeyFlowGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%" stopColor="#10B981" />
                    <stop offset="50%" stopColor="#A4F941" />
                    <stop offset="100%" stopColor="#8B5CF6" />
                  </linearGradient>
                  <filter id="photonFlowGlow" x="-20%" y="-20%" width="140%" height="140%">
                    <feGaussianBlur stdDeviation="3" result="blur" />
                    <feMerge>
                      <feMergeNode in="blur" />
                      <feMergeNode in="SourceGraphic" />
                    </feMerge>
                  </filter>
                </defs>
                <path 
                  d="M 10 40 C 50 15, 110 65, 150 40" 
                  stroke="url(#journeyFlowGrad)" 
                  strokeWidth="2.5" 
                  strokeDasharray="6 6"
                  className="trajectory-flow-dash" 
                />
                <circle r="4.5" fill="#A4F941" filter="url(#photonFlowGlow)" className="trajectory-photon-dot">
                  <animateMotion 
                    path="M 10 40 C 50 15, 110 65, 150 40" 
                    dur="3s" 
                    repeatCount="indefinite" 
                  />
                </circle>
              </svg>

              {/* Luminous Waypoint Capsule */}
              <div className="trajectory-waypoint-pill">
                <span className="waypoint-pip"></span>
                <span className="waypoint-text">FOUNDATION</span>
                <ArrowRight size={11} className="waypoint-arrow" />
                <span className="waypoint-text text-mca">ADVANCED</span>
              </div>
            </div>

            {/* ======================================================== */}
            {/* MILESTONE 02: MCA ADVANCED POSTGRADUATE */}
            {/* ======================================================== */}
            <div 
              className={`academic-milestone-chamber milestone-mca ${hoveredMilestone === 'MCA' ? 'milestone-focused' : ''}`}
              onMouseEnter={() => setHoveredMilestone('MCA')}
              onMouseLeave={() => setHoveredMilestone(null)}
            >
              <div className="milestone-accent-border border-mca" aria-hidden="true"></div>
              <div className="milestone-monogram-watermark watermark-mca" aria-hidden="true">MCA</div>

              {/* Milestone Anchor */}
              <div className="milestone-anchor-header">
                <div className="mca-nexus-apparatus">
                  <div className="mca-resonance-ring ring-1" aria-hidden="true"></div>
                  <div className="mca-resonance-ring ring-2" aria-hidden="true"></div>
                  <div className="mca-resonance-ring ring-3" aria-hidden="true"></div>
                  
                  <div className="mca-nexus-core">
                    <Sparkles size={28} className="mca-spark-icon" aria-hidden="true" />
                    <span className="mca-nexus-monogram">MCA</span>
                  </div>
                </div>

                <div className="milestone-meta-col">
                  <span className="milestone-stage-tag tag-mca">
                    <Terminal size={12} />
                    <span>ADVANCED POSTGRADUATE</span>
                  </span>
                  <div className="mca-status-pill">
                    <span className="mca-pulse-beacon" aria-hidden="true"></span>
                    <span>CURRENTLY ENROLLED</span>
                  </div>
                  <span className="mca-period-display">2025 &mdash; Present</span>
                </div>
              </div>

              {/* Core Typography */}
              <div className="milestone-body">
                <h3 className="milestone-degree-title mca-title">
                  Master of Computer Applications
                </h3>

                <div className="milestone-institution-row">
                  <strong className="inst-name">Dr. D. Y. Patil Vidyapeeth (DPU)</strong>
                  <span className="inst-affil">Pune, Maharashtra &middot; Enterprise Software Track</span>
                </div>

                <div className="milestone-summary-capsules">
                  <span className="summary-pill pill-violet">Enterprise Java</span>
                  <span className="summary-pill pill-violet">Distributed Systems</span>
                  <span className="summary-pill pill-violet">Cloud Persistence</span>
                  <span className="summary-pill pill-violet">High-Throughput Services</span>
                </div>
              </div>

            </div>

          </div>

          {/* ======================================================== */}
          {/* INTEGRATED EVIDENCE DECK: INTERACTIVE CREDENTIAL TOKENS */}
          {/* ======================================================== */}
          <div className="academic-evidence-deck">
            <div className="evidence-deck-header">
              <div className="evidence-header-main">
                <Award size={15} className="evidence-award-icon" aria-hidden="true" />
                <span className="evidence-title">VERIFIED ACADEMIC ASSESSMENTS &amp; TECHNICAL COMPETITIONS</span>
              </div>
              <span className="evidence-click-hint">Click token to view verified certificate asset &rarr;</span>
            </div>

            <div className="evidence-chips-cluster" role="list" aria-label="Verified Certificates and Competitions">
              {verifiedCertifications.map((cert) => (
                <button
                  key={cert.id}
                  type="button"
                  className="evidence-token-card evidence-chip"
                  onClick={() => setSelectedCertificate(cert)}
                  aria-label={`Inspect verified certificate: ${cert.name} by ${cert.issuer}`}
                  style={{
                    '--token-accent': cert.accentColor,
                    '--token-accent-light': cert.accentLight
                  }}
                >
                  <div className="token-thumb-chassis" aria-hidden="true">
                    <img src={cert.asset} alt="" className="token-micro-img" />
                    <span className="token-thumb-lens">
                      <ExternalLink size={11} />
                    </span>
                  </div>

                  <div className="token-body-col">
                    <div className="token-top-row">
                      <span className="chip-pip" style={{ background: cert.accentColor }} aria-hidden="true"></span>
                      <span className="chip-type">{cert.type}</span>
                      {cert.credentialId && (
                        <span className="token-id-badge">ID: {cert.credentialId.slice(0, 8)}</span>
                      )}
                    </div>
                    <strong className="chip-name">{cert.name}</strong>
                    <span className="chip-issuer">&middot; {cert.issuer}</span>
                  </div>
                </button>
              ))}
            </div>
          </div>

        </div>

      </div>

      {/* Interactive Certificate Preview Modal */}
      <CertificateModal 
        certificate={selectedCertificate} 
        isOpen={!!selectedCertificate} 
        onClose={() => setSelectedCertificate(null)} 
      />

    </section>
  );
}
