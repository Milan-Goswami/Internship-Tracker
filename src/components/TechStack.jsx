import { portfolioData } from '../data/portfolio';
import { TechBadge } from './TechIcons';
import './TechStack.css';

export default function TechStack() {
  return (
    <section id="tech-stack" className="techstack-section">
      <div className="container">
        <h3 className="section-eyebrow text-center mb-4">Core Engineering Stack</h3>
      </div>
      <div className="tech-marquee-container">
        <div className="tech-marquee">
          {[...portfolioData.skills, ...portfolioData.skills, ...portfolioData.skills].map((skill, i) => (
            <div key={i} className="tech-marquee-item">
              <TechBadge name={skill} size="lg" />
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
