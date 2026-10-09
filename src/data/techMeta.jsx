// Authentic developer brand colors & recognized vector logos for technologies
export const techMeta = {
  'Java': {
    color: '#E76F00',
    bg: 'rgba(231, 111, 0, 0.12)',
    border: 'rgba(231, 111, 0, 0.35)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Steam curves */}
        <path d="M7 3.5c1.5 1.5.5 3.5 2 4.5" stroke="#E76F00" strokeWidth="1.8" strokeLinecap="round" />
        <path d="M11 2c2 2 1 4 2.5 5.5" stroke="#5382A1" strokeWidth="1.8" strokeLinecap="round" />
        <path d="M15 3.5c1 1.5 0 3 1.5 4" stroke="#E76F00" strokeWidth="1.8" strokeLinecap="round" />
        {/* Cup body & saucer */}
        <path d="M4 11h13a1 1 0 0 1 1 1v2.5a5.5 5.5 0 0 1-5.5 5.5h-3A5.5 5.5 0 0 1 4 14.5V11z" fill="#E76F00" fillOpacity="0.2" stroke="#E76F00" strokeWidth="1.8" />
        <path d="M18 12.5h1.5a2.5 2.5 0 0 1 0 5H17" stroke="#E76F00" strokeWidth="1.8" strokeLinecap="round" />
        <path d="M2 21h18" stroke="#5382A1" strokeWidth="2" strokeLinecap="round" />
      </svg>
    )
  },
  'Spring Boot': {
    color: '#6DB33F',
    bg: 'rgba(109, 179, 63, 0.14)',
    border: 'rgba(109, 179, 63, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Official Spring hexagonal leaf emblem */}
        <path 
          d="M12 2L3.5 7v10L12 22l8.5-5V7L12 2z" 
          stroke="#6DB33F" 
          strokeWidth="1.6" 
          fill="rgba(109, 179, 63, 0.15)"
        />
        <path 
          d="M16.8 8.5C14.2 8.3 11.5 9.7 10.3 12c-1 1.9-.8 4.2.3 5.9.8.1 1.6-.2 2.2-.7 1.8-1.5 2.4-3.9 3.2-6.1.4-.8.7-1.7.8-2.6z" 
          fill="#6DB33F"
        />
        <path 
          d="M10.5 12c1.2 1.2 3.2 2.2 4.8 2.4" 
          stroke="#FFFFFF" 
          strokeWidth="1.4" 
          strokeLinecap="round"
        />
      </svg>
    )
  },
  'Spring Data JPA': {
    color: '#0D9488',
    bg: 'rgba(13, 148, 136, 0.14)',
    border: 'rgba(13, 148, 136, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Relational database cylinders + Spring leaf dot */}
        <ellipse cx="12" cy="5" rx="8" ry="3" stroke="#0D9488" strokeWidth="1.8" fill="rgba(13, 148, 136, 0.2)" />
        <path d="M4 5v6c0 1.66 3.58 3 8 3s8-1.34 8-3V5" stroke="#0D9488" strokeWidth="1.8" />
        <path d="M4 11v6c0 1.66 3.58 3 8 3s8-1.34 8-3v-6" stroke="#0D9488" strokeWidth="1.8" />
        <circle cx="17.5" cy="17.5" r="3.5" fill="#6DB33F" stroke="#FFFFFF" strokeWidth="1.2" />
        <path d="M16.5 17.5l1 1 2-2" stroke="#FFFFFF" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    )
  },
  'Hibernate/JPA': {
    color: '#BCAE79',
    bg: 'rgba(188, 174, 121, 0.16)',
    border: 'rgba(188, 174, 121, 0.4)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Hibernate geometric stylized 'H' crest */}
        <rect x="3" y="3" width="18" height="18" rx="4" stroke="#BCAE79" strokeWidth="1.8" fill="rgba(188, 174, 121, 0.15)" />
        <path d="M7.5 7v10M16.5 7v10M7.5 12h9" stroke="#59666C" strokeWidth="2.4" strokeLinecap="round" />
        <circle cx="12" cy="12" r="1.8" fill="#E76F00" />
      </svg>
    )
  },
  'REST APIs': {
    color: '#6366F1',
    bg: 'rgba(99, 102, 241, 0.14)',
    border: 'rgba(99, 102, 241, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Connected API endpoints / contract nodes */}
        <circle cx="5" cy="12" r="3" fill="#6366F1" fillOpacity="0.2" stroke="#6366F1" strokeWidth="2" />
        <circle cx="19" cy="6" r="3" fill="#6366F1" fillOpacity="0.2" stroke="#6366F1" strokeWidth="2" />
        <circle cx="19" cy="18" r="3" fill="#6366F1" fillOpacity="0.2" stroke="#6366F1" strokeWidth="2" />
        <path d="M8 12h5m0 0l3-4m-3 4l3 4" stroke="#6366F1" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    )
  },
  'MySQL': {
    color: '#00758F',
    bg: 'rgba(0, 117, 143, 0.14)',
    border: 'rgba(0, 117, 143, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Iconic MySQL dolphin tail curve + wave crest */}
        <path 
          d="M19.5 7.5c-1.5-3-5-4.5-8.5-3.5C7.5 5 5 8 5.5 12c.5 4 4 7 8 7 3.5 0 6-2 7-4.5-1.5.5-3 0-4-1 1-1 2.5-1.5 4-1 .5-2-.2-3.8-1-5z" 
          fill="#00758F" 
          fillOpacity="0.25"
          stroke="#00758F" 
          strokeWidth="1.8" 
          strokeLinecap="round"
        />
        <circle cx="9" cy="8.5" r="1.2" fill="#F29111" />
        <path d="M4 19c2.5-1 5 1 7.5 0s5 1 7.5 0" stroke="#F29111" strokeWidth="1.5" strokeLinecap="round" />
      </svg>
    )
  },
  'JSP': {
    color: '#E76F00',
    bg: 'rgba(231, 111, 0, 0.12)',
    border: 'rgba(231, 111, 0, 0.32)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Code script tags <% %> with Java bean core */}
        <path d="M6 7l-4 5 4 5M18 7l4 5-4 5" stroke="#E76F00" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="12" cy="12" r="3" fill="#E76F00" fillOpacity="0.3" stroke="#2563EB" strokeWidth="1.8" />
        <path d="M12 9v6" stroke="#2563EB" strokeWidth="1.5" strokeLinecap="round" />
      </svg>
    )
  },
  'Servlets': {
    color: '#0284C7',
    bg: 'rgba(2, 132, 199, 0.14)',
    border: 'rgba(2, 132, 199, 0.35)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Java Servlet pipeline server engine */}
        <rect x="3" y="4" width="18" height="6" rx="2" stroke="#0284C7" strokeWidth="1.8" fill="rgba(2, 132, 199, 0.15)" />
        <rect x="3" y="14" width="18" height="6" rx="2" stroke="#0284C7" strokeWidth="1.8" fill="rgba(2, 132, 199, 0.15)" />
        <circle cx="7" cy="7" r="1" fill="#0284C7" />
        <circle cx="7" cy="17" r="1" fill="#0284C7" />
        <path d="M14 7h4M14 17h4" stroke="#0284C7" strokeWidth="1.5" strokeLinecap="round" />
        <path d="M12 10v4m-2-2l2 2 2-2" stroke="#E76F00" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    )
  },
  'J2EE': {
    color: '#1E40AF',
    bg: 'rgba(30, 64, 175, 0.14)',
    border: 'rgba(30, 64, 175, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Enterprise multi-tier architecture pyramid */}
        <polygon points="12 2 2 7 12 12 22 7 12 2" stroke="#1E40AF" strokeWidth="1.8" fill="rgba(30, 64, 175, 0.2)" />
        <polyline points="2 12 12 17 22 12" stroke="#1E40AF" strokeWidth="1.8" />
        <polyline points="2 17 12 22 22 17" stroke="#1E40AF" strokeWidth="1.8" />
        <circle cx="12" cy="7" r="1.5" fill="#E76F00" />
      </svg>
    )
  },
  'Maven': {
    color: '#C71A36',
    bg: 'rgba(199, 26, 54, 0.14)',
    border: 'rgba(199, 26, 54, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Apache Maven origami quill feather / build diamond */}
        <path 
          d="M12 2L4.5 9.5l4 4L12 10l3.5 3.5 4-4L12 2z" 
          fill="#C71A36" 
          fillOpacity="0.2"
          stroke="#C71A36" 
          strokeWidth="1.8" 
        />
        <path d="M12 10v12" stroke="#C71A36" strokeWidth="2" strokeLinecap="round" />
        <path d="M8.5 13.5l3.5 3.5 3.5-3.5" stroke="#0284C7" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    )
  },
  'Git/GitHub': {
    color: '#F05032',
    bg: 'rgba(240, 80, 50, 0.14)',
    border: 'rgba(240, 80, 50, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Git diamond branch architecture */}
        <rect x="4.5" y="4.5" width="15" height="15" rx="3" transform="rotate(45 12 12)" stroke="#F05032" strokeWidth="1.8" fill="rgba(240, 80, 50, 0.18)" />
        <circle cx="12" cy="7" r="1.8" fill="#F05032" />
        <circle cx="12" cy="17" r="1.8" fill="#F05032" />
        <circle cx="16" cy="12" r="1.8" fill="#24292F" />
        <path d="M12 8.8v6.4M12 12l2.6 0" stroke="#F05032" strokeWidth="1.6" strokeLinecap="round" />
      </svg>
    )
  },
  'React': {
    color: '#149ECA',
    bg: 'rgba(20, 158, 202, 0.14)',
    border: 'rgba(20, 158, 202, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Official React atomic core and ellipses */}
        <ellipse cx="12" cy="12" rx="9" ry="3.8" stroke="#149ECA" strokeWidth="1.6" />
        <ellipse cx="12" cy="12" rx="9" ry="3.8" transform="rotate(60 12 12)" stroke="#149ECA" strokeWidth="1.6" />
        <ellipse cx="12" cy="12" rx="9" ry="3.8" transform="rotate(120 12 12)" stroke="#149ECA" strokeWidth="1.6" />
        <circle cx="12" cy="12" r="2" fill="#149ECA" />
      </svg>
    )
  },
  'JavaScript': {
    color: '#D97706',
    bg: 'rgba(217, 119, 6, 0.14)',
    border: 'rgba(217, 119, 6, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* JS golden badge with stylized JS monogram */}
        <rect x="3" y="3" width="18" height="18" rx="3" fill="#F59E0B" fillOpacity="0.25" stroke="#D97706" strokeWidth="1.8" />
        <path d="M8 12v4a1.5 1.5 0 0 0 3 0v-1" stroke="#D97706" strokeWidth="2" strokeLinecap="round" />
        <path d="M14 17a2 2 0 0 0 2-2c0-1.5-2-1.5-2-3a2 2 0 0 1 2-2" stroke="#D97706" strokeWidth="2" strokeLinecap="round" />
      </svg>
    )
  },
  'HTML/CSS': {
    color: '#E34F26',
    bg: 'rgba(227, 79, 38, 0.14)',
    border: 'rgba(227, 79, 38, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Dual HTML5 / CSS3 shields */}
        <path d="M4 3l1.5 15L12 21l6.5-3L20 3H4z" fill="rgba(227, 79, 38, 0.2)" stroke="#E34F26" strokeWidth="1.8" strokeLinejoin="round" />
        <path d="M12 6v12l4.5-2 .8-9H12z" fill="#E34F26" fillOpacity="0.35" />
        <path d="M8 9h8m-8 4h7.2l-.5 3-2.7 1-2.7-1" stroke="#1572B6" strokeWidth="1.4" strokeLinecap="round" />
      </svg>
    )
  },
  'Postman': {
    color: '#FF6C37',
    bg: 'rgba(255, 108, 55, 0.14)',
    border: 'rgba(255, 108, 55, 0.38)',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" className="brand-svg" aria-hidden="true">
        {/* Postman flight trail & rocket trajectory */}
        <circle cx="12" cy="12" r="9" stroke="#FF6C37" strokeWidth="1.8" fill="rgba(255, 108, 55, 0.15)" />
        <path d="M7 16l8-8m0 0h-5m5 0v5" stroke="#FF6C37" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    )
  }
};
