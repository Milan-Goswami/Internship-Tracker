import { useEffect } from 'react';
import Lenis from 'lenis';
import 'lenis/dist/lenis.css';
import { Helmet, HelmetProvider } from 'react-helmet-async';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import './styles/global.css';

gsap.registerPlugin(ScrollTrigger);
import Navbar from './components/Navbar';
import Hero from './components/Hero';
import TechStack from './components/TechStack';
import Projects from './components/Projects';
import Experience from './components/Experience';
import CurrentlyLearning from './components/CurrentlyLearning';
import ContactCTA from './components/ContactCTA';
import Footer from './components/Footer';

function App() {
  useEffect(() => {
    const lenis = new Lenis({
      duration: 1.2,
      easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
      direction: 'vertical',
      gestureDirection: 'vertical',
      smooth: true,
      mouseMultiplier: 1,
      smoothTouch: false,
      touchMultiplier: 2,
      infinite: false,
    });

    lenis.on('scroll', ScrollTrigger.update);

    gsap.ticker.add((time) => {
      lenis.raf(time * 1000);
    });
    
    gsap.ticker.lagSmoothing(0);

    // Make lenis globally available for anchors if needed
    window.lenis = lenis;

    const refreshTimer = setTimeout(() => {
      ScrollTrigger.refresh();
    }, 400);

    const onLoad = () => ScrollTrigger.refresh();
    window.addEventListener('load', onLoad);

    return () => {
      clearTimeout(refreshTimer);
      window.removeEventListener('load', onLoad);
      gsap.ticker.remove((time) => lenis.raf(time * 1000));
      lenis.destroy();
    };
  }, []);

  return (
    <HelmetProvider>
      <Helmet>
        <title>Milan Goswami — Software Developer | Java Developer</title>
        <meta name="description" content="Portfolio of Milan Goswami, a Software Developer and Java Developer focused on Java, backend development, full-stack applications, and practical software engineering projects." />
        <meta name="author" content="Milan Goswami" />
        <meta property="og:type" content="website" />
        <meta property="og:title" content="Milan Goswami — Software Developer | Java Developer" />
        <meta property="og:description" content="Portfolio of Milan Goswami, a Software Developer and Java Developer focused on Java, backend development, full-stack applications, and practical software engineering projects." />
        <meta property="og:image" content="/assets/nova-dashboard.png" />
        <meta property="og:image:alt" content="Milan Goswami — Software Developer Portfolio" />
        <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:title" content="Milan Goswami — Software Developer | Java Developer" />
        <meta name="twitter:description" content="Portfolio of Milan Goswami, a Software Developer and Java Developer focused on Java, backend development, full-stack applications, and practical software engineering projects." />
        <meta name="twitter:image" content="/assets/nova-dashboard.png" />
      </Helmet>
      
      <div className="app-container">
        <Navbar />
        
        <main>
          <Hero />
          <TechStack />
          <Projects />
          <Experience />
          <CurrentlyLearning />
          <ContactCTA />
        </main>
        
        <Footer />
      </div>
    </HelmetProvider>
  );
}

export default App;
