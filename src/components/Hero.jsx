import { useRef, useEffect, useState } from 'react';
import gsap from 'gsap';
import { FileText, ArrowUpRight, GraduationCap } from 'lucide-react';
import { portfolioData } from '../data/portfolio';
import milanHeroImg from '../assets/milan-hero.png';
import ResumeModal from './ResumeModal';
import './Hero.css';

export default function Hero() {
  const { profile } = portfolioData;
  const heroRef = useRef(null);
  const canvasRef = useRef(null);
  const portraitRef = useRef(null);
  const [isResumeOpen, setIsResumeOpen] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let animationFrameId;
    let width = canvas.width = canvas.offsetWidth;
    let height = canvas.height = canvas.offsetHeight;

    const handleResize = () => {
      if (!canvas) return;
      width = canvas.width = canvas.offsetWidth;
      height = canvas.height = canvas.offsetHeight;
    };
    window.addEventListener('resize', handleResize);

    // Living atmospheric particles array (32 floating organic motes)
    const particles = Array.from({ length: 32 }, () => ({
      x: Math.random() * width,
      y: Math.random() * height,
      radius: Math.random() * 2.6 + 1.0,
      speedX: (Math.random() - 0.5) * 0.45,
      speedY: -Math.random() * 0.5 - 0.25,
      opacity: Math.random() * 0.55 + 0.25,
      pulse: Math.random() * Math.PI * 2,
      pulseSpeed: Math.random() * 0.03 + 0.015
    }));

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    let mouseX = 0;
    let mouseY = 0;
    let targetMouseX = 0;
    let targetMouseY = 0;

    const handleMouseMove = (e) => {
      if (prefersReducedMotion) return;
      const rect = canvas.getBoundingClientRect();
      const halfW = width / 2 || 1;
      const halfH = height / 2 || 1;
      const normX = ((e.clientX - rect.left) - halfW) / halfW;
      const normY = ((e.clientY - rect.top) - halfH) / halfH;
      targetMouseX = Math.max(-1, Math.min(1, normX));
      targetMouseY = Math.max(-1, Math.min(1, normY));
    };

    const handleMouseLeave = () => {
      targetMouseX = 0;
      targetMouseY = 0;
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseleave', handleMouseLeave);

    let angle = 0;

    const render = () => {
      ctx.clearRect(0, 0, width, height);

      const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
      const fadeColor = isDark ? 'rgba(12, 15, 12, 0)' : 'rgba(255, 255, 255, 0)';

      if (!prefersReducedMotion) {
        angle += 0.010;
        // Calmer, silky lerp damping (0.020 dark / 0.025 light) prevents aggressive cursor following
        const lerpFactor = isDark ? 0.020 : 0.025;
        mouseX += (targetMouseX - mouseX) * lerpFactor;
        mouseY += (targetMouseY - mouseY) * lerpFactor;
      }

      // Phase 16 Motion Refinement: Bounded subtle displacement (max ±22px dark / ±28px light)
      const maxCenterShiftX = isDark ? 22 : 28;
      const maxCenterShiftY = isDark ? 16 : 20;
      const maxOuterShiftX = isDark ? 12 : 16;
      const maxOuterShiftY = isDark ? 10 : 14;

      const shiftCenterX = mouseX * maxCenterShiftX;
      const shiftCenterY = mouseY * maxCenterShiftY;
      const shiftOuterX = mouseX * maxOuterShiftX;
      const shiftOuterY = mouseY * maxOuterShiftY;

      // Layer 1: Left Atmospheric Lime Radial Bloom (Calm, subtle organic breathing)
      const leftX = width * 0.18 + Math.sin(angle * 0.7) * 20 + shiftOuterX;
      const leftY = height * 0.42 + Math.cos(angle * 0.6) * 16 + shiftOuterY;
      const leftRadius = width * (0.42 + Math.sin(angle * 0.5) * 0.03);
      const leftGrad = ctx.createRadialGradient(leftX, leftY, 0, leftX, leftY, leftRadius);
      leftGrad.addColorStop(0, isDark ? 'rgba(156, 240, 52, 0.14)' : 'rgba(156, 240, 52, 0.42)');
      leftGrad.addColorStop(0.35, isDark ? 'rgba(164, 249, 65, 0.07)' : 'rgba(164, 249, 65, 0.24)');
      leftGrad.addColorStop(0.70, isDark ? 'rgba(188, 252, 105, 0.02)' : 'rgba(188, 252, 105, 0.08)');
      leftGrad.addColorStop(1, fadeColor);
      ctx.fillStyle = leftGrad;
      ctx.fillRect(0, 0, width, height);

      // Layer 2: Right Atmospheric Emerald Radial Bloom
      const rightX = width * 0.82 + Math.cos(angle * 0.65) * 18 + shiftOuterX;
      const rightY = height * 0.40 + Math.sin(angle * 0.7) * 15 + shiftOuterY;
      const rightRadius = width * (0.42 + Math.cos(angle * 0.45) * 0.03);
      const rightGrad = ctx.createRadialGradient(rightX, rightY, 0, rightX, rightY, rightRadius);
      rightGrad.addColorStop(0, isDark ? 'rgba(16, 185, 129, 0.12)' : 'rgba(16, 185, 129, 0.32)');
      rightGrad.addColorStop(0.35, isDark ? 'rgba(164, 249, 65, 0.06)' : 'rgba(164, 249, 65, 0.22)');
      rightGrad.addColorStop(0.70, isDark ? 'rgba(190, 252, 110, 0.02)' : 'rgba(190, 252, 110, 0.07)');
      rightGrad.addColorStop(1, fadeColor);
      ctx.fillStyle = rightGrad;
      ctx.fillRect(0, 0, width, height);

      // Layer 3: Central Behind-Portrait Aura (Soft, calm halo over graphite, stable behind portrait)
      const centerX = width * 0.50 + shiftCenterX;
      const centerY = height * 0.54 + Math.sin(angle * 0.55) * 12 + shiftCenterY;
      const centerRadius = width * (0.42 + Math.sin(angle * 0.45) * 0.025);
      const centerGrad = ctx.createRadialGradient(centerX, centerY, 20, centerX, centerY, centerRadius);
      centerGrad.addColorStop(0, isDark ? 'rgba(156, 240, 52, 0.18)' : 'rgba(156, 240, 52, 0.50)');
      centerGrad.addColorStop(0.35, isDark ? 'rgba(52, 211, 153, 0.09)' : 'rgba(16, 185, 129, 0.32)');
      centerGrad.addColorStop(0.70, isDark ? 'rgba(164, 249, 65, 0.025)' : 'rgba(205, 254, 130, 0.10)');
      centerGrad.addColorStop(1, fadeColor);
      ctx.fillStyle = centerGrad;
      ctx.fillRect(0, 0, width, height);

      // Layer 4 (Light Mode Only): Soft Skylight & Pale Mint Atmospheric Drift (Calm, Airy, Luminous)
      if (!isDark) {
        const skyX = width * 0.50 + Math.sin(angle * 0.4) * 28 + shiftOuterX * 0.4;
        const skyY = height * 0.28 + Math.cos(angle * 0.35) * 18 + shiftOuterY * 0.4;
        const skyRadius = width * (0.48 + Math.sin(angle * 0.3) * 0.03);
        const skyGrad = ctx.createRadialGradient(skyX, skyY, 0, skyX, skyY, skyRadius);
        skyGrad.addColorStop(0, 'rgba(220, 252, 231, 0.28)'); // Pale mint atmospheric cloud
        skyGrad.addColorStop(0.45, 'rgba(240, 253, 244, 0.14)');
        skyGrad.addColorStop(0.80, 'rgba(254, 249, 195, 0.05)'); // Warm morning diffusion
        skyGrad.addColorStop(1, fadeColor);
        ctx.fillStyle = skyGrad;
        ctx.fillRect(0, 0, width, height);
      }

      // Living floating light motes / particles (gentle background ambiance)
      for (let i = 0; i < particles.length; i++) {
        const p = particles[i];
        p.x += p.speedX;
        p.y += p.speedY;
        p.pulse += p.pulseSpeed;

        if (p.y < -10) {
          p.y = height + 10;
          p.x = Math.random() * width;
        }
        if (p.x < -10) p.x = width + 10;
        if (p.x > width + 10) p.x = -10;

        const baseOpacity = p.opacity * (0.6 + 0.4 * Math.sin(p.pulse));
        const moteAlpha = isDark ? baseOpacity * 0.65 : baseOpacity * 0.32;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx.fillStyle = isDark
          ? `rgba(164, 249, 65, ${moteAlpha})`
          : `rgba(22, 163, 74, ${moteAlpha})`;
        ctx.fill();
      }

      animationFrameId = requestAnimationFrame(render);
    };
    render();

    // GSAP Entrance
    if (!prefersReducedMotion) {
      const gsapCtx = gsap.context(() => {
        const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });

        tl.from('.ref-headline-group', { y: 35, opacity: 0, duration: 0.9 })
          .from(portraitRef.current, { y: 45, opacity: 0, scale: 0.98, duration: 1.0 }, '-=0.6')
          .from('.ref-pill-left', { x: -25, opacity: 0, duration: 0.7 }, '-=0.5')
          .from('.ref-copy-right', { x: 25, opacity: 0, duration: 0.7 }, '-=0.7')
          .from('.ref-badge-bottom-left', { y: 20, opacity: 0, duration: 0.6 }, '-=0.5')
          .from('.ref-cta-bottom-right', { y: 20, opacity: 0, duration: 0.6 }, '-=0.6');
      }, heroRef);

      return () => {
        cancelAnimationFrame(animationFrameId);
        window.removeEventListener('resize', handleResize);
        window.removeEventListener('mousemove', handleMouseMove);
        window.removeEventListener('mouseleave', handleMouseLeave);
        gsapCtx.revert();
      };
    }

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseleave', handleMouseLeave);
    };
  }, []);

  return (
    <section id="home" className="reference-hero-section" ref={heroRef}>
      
      {/* Living Atmospheric Mesh Canvas matching reference's lime aura */}
      <canvas ref={canvasRef} className="ref-hero-ambient-canvas" aria-hidden="true" />

      {/* Decorative radial gradients as CSS fallback & extra glow depth */}
      <div className="ref-hero-ambient-layer" aria-hidden="true">
        <div className="ambient-blob blob-left"></div>
        <div className="ambient-blob blob-right"></div>
        <div className="ambient-blob blob-center"></div>
      </div>

      <div className="container ref-hero-stage">
        
        {/* Display Headline */}
        <div className="ref-headline-group">
          <h1 className="ref-display-title">
            <span className="ref-title-sans">Hi I&apos;m Milan</span>
            <span className="ref-title-serif">Java Developer</span>
          </h1>
        </div>

        {/* Central Portrait Composition Layered with Headline */}
        <div className="ref-portrait-container" ref={portraitRef}>
          <img 
            src={milanHeroImg} 
            alt={profile.name} 
            className="ref-portrait-img"
            loading="eager"
            fetchPriority="high"
          />
        </div>

        {/* Floating Mid-Left Capsule: "Available for new opportunities" */}
        <div className="ref-pill-left">
          <span className="ref-pill-pulse-dot" aria-hidden="true"></span>
          <span className="ref-pill-text">Available for new opportunities</span>
        </div>

        {/* Floating Mid-Right Editorial Statement */}
        <div className="ref-copy-right">
          <p className="ref-statement-text">
            passionate about architecting reliable backend services, relational persistence, and production-grade software.
          </p>
        </div>

        {/* Floating Bottom-Left Trust Proof & Academic Indicator */}
        <div className="ref-badge-bottom-left">
          <div className="ref-avatar-stack" aria-hidden="true">
            <span className="avatar-disc avatar-java" title="Java">
              <svg viewBox="0 0 24 24" fill="none" className="mini-brand-svg">
                <path d="M7 3.5c1.5 1.5.5 3.5 2 4.5" stroke="#E76F00" strokeWidth="1.8" strokeLinecap="round" />
                <path d="M11 2c2 2 1 4 2.5 5.5" stroke="#5382A1" strokeWidth="1.8" strokeLinecap="round" />
                <path d="M4 11h13v2.5a5.5 5.5 0 0 1-5.5 5.5h-3A5.5 5.5 0 0 1 4 14.5V11z" fill="#E76F00" stroke="#E76F00" strokeWidth="1.6" />
              </svg>
            </span>
            <span className="avatar-disc avatar-spring" title="Spring Boot">
              <svg viewBox="0 0 24 24" fill="none" className="mini-brand-svg">
                <path d="M12 2L3.5 7v10L12 22l8.5-5V7L12 2z" stroke="#6DB33F" strokeWidth="1.6" fill="#6DB33F" />
              </svg>
            </span>
            <span className="avatar-disc avatar-edu" title="MCA @ DPU">
              <GraduationCap size={14} className="mini-edu-icon" />
            </span>
          </div>
          <div className="ref-trust-text">
            <strong>MCA @ Dr. D. Y. Patil</strong> &amp; BCA Graduate. Focused on high-performance Java backends &amp; relational data.
          </div>
        </div>

        {/* Floating Bottom-Right Primary CTA Button: "View Resume" */}
        <div className="ref-cta-bottom-right">
          <button 
            type="button" 
            onClick={() => setIsResumeOpen(true)}
            className="ref-view-resume-btn"
            aria-label="View Milan Goswami's professional resume"
            title="Open resume document viewer"
          >
            <span className="btn-icon-core" aria-hidden="true">
              <FileText size={16} />
            </span>
            <span className="btn-label">View Resume</span>
            <span className="btn-arrow-core" aria-hidden="true">
              <ArrowUpRight size={15} />
            </span>
          </button>
        </div>

      </div>

      {/* Interactive Professional Resume Viewer Modal */}
      <ResumeModal isOpen={isResumeOpen} onClose={() => setIsResumeOpen(false)} />

    </section>
  );
}
