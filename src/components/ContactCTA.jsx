import { useRef, useEffect } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { ArrowRight } from 'lucide-react';
import { portfolioData } from '../data/portfolio';
import './ContactCTA.css';

gsap.registerPlugin(ScrollTrigger);

export default function ContactCTA() {
  const sectionRef = useRef(null);

  useEffect(() => {
    let ctx = gsap.context(() => {
      gsap.from('.cta-editorial-stage', {
        scrollTrigger: { trigger: sectionRef.current, start: 'top 80%', once: true },
        y: 40, opacity: 0, duration: 0.9, ease: 'power3.out'
      });
    }, sectionRef);

    return () => ctx.revert();
  }, []);

  return (
    <section id="contact" className="closing-cta-section section" ref={sectionRef}>
      
      {/* Soft Atmospheric Lime Halo */}
      <div className="closing-ambient-glow" aria-hidden="true"></div>

      <div className="container">
        <div className="cta-editorial-stage text-center">
          
          <div className="cta-status-tag">
            <span className="cta-live-dot" aria-hidden="true"></span>
            <span>OPEN TO SOFTWARE DEVELOPER OPPORTUNITIES</span>
          </div>

          <h2 className="cta-display-heading">
            Ready to engineer <br />
            <span className="cta-heading-serif">reliable backend systems.</span>
          </h2>

          <p className="cta-lead-text">
            Specialized in Java, Spring Boot, and normalized MySQL relational architectures. <br />
            Available for software developer roles in Pune or remote.
          </p>

          <div className="cta-buttons-cluster">
            
            {/* Primary Action */}
            <a 
              href={`mailto:${portfolioData.profile.email}`} 
              className="cta-pill-primary"
            >
              <span>Get in Touch</span>
              <ArrowRight size={16} aria-hidden="true" />
            </a>

          </div>

        </div>
      </div>
    </section>
  );
}
