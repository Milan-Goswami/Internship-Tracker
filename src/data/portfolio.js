export const portfolioData = {
  profile: {
    name: "Milan Goswami",
    title: "Software Developer",
    secondaryTitle: "Java Developer",
    location: "Pune, Maharashtra, India",
    status: "MCA Student",
    availability: "Open to Software Developer / Java Developer opportunities",
    email: "milangoswami879@gmail.com",
    heroDescription: "I’m Milan Goswami, a Software Developer focused on Java, backend development, and building practical full-stack applications.",
    about: "I'm an MCA student with hands-on experience across Java, web development, Flutter, databases, and application development."
  },
  skills: [
    "Java", "Spring Boot", "Spring MVC", "Spring Data JPA", "Hibernate/JPA",
    "REST APIs", "MySQL", "JDBC", "JSP", "Servlets",
    "J2EE", "Maven", "Git/GitHub", "Firebase", "Postman"
  ],
  learning: [
    "Spring Boot",
    "Spring Data JPA",
    "REST APIs",
    "System Design",
    "Microservices",
    "DSA"
  ],

  projects: [
    {
      id: 1,
      name: "Insurance Management System",
      description: "A full-stack J2EE web application with an admin dashboard for managing policies, customers, and claims.",
      technologies: ["Java", "JSP", "Servlets", "MySQL", "JDBC"],
      links: { github: "https://github.com/Milan-Goswami/Insurance-Management-System" },
      media: {
        type: "image",
        primary: "/assets/insurance-dashboard.png",
        gallery: ["/assets/insurance-dashboard.png", "/assets/insurance-customers.png"],
        poster: null,
        technicalType: "schema"
      }
    },
    {
      id: 2,
      name: "Nova Online Examination System",
      description: "A Java-based web application for conducting online examinations using JSP/Servlets, MySQL, and Apache Tomcat.",
      technologies: ["Java", "JSP", "Servlets", "MySQL", "Maven", "HTML", "CSS", "JavaScript"],
      links: { github: "https://github.com/Milan-Goswami/NovaOnlineExamSystem" },
      media: {
        type: "image",
        primary: "/assets/nova-dashboard.png",
        gallery: ["/assets/nova-dashboard.png", "/assets/nova-home.png", "/assets/nova-exam-interface.png"],
        poster: null,
        technicalType: "code"
      }
    }
  ],
  education: [
    {
      degree: "MCA",
      institution: "Dr. D. Y. Patil Vidyapeeth",
      period: "2025 — Present"
    },
    {
      degree: "BCA",
      institution: "Sutex Bank College of Computer Applications & Science, Surat",
      period: "2022 — 2025"
    }
  ],
  certifications: [
    {
      name: "DSA with Java — Completion Certificate",
      issuer: "Various"
    },
    {
      name: "Java (Basic)",
      issuer: "HackerRank"
    },
    {
      name: "INNIXO Hackathon Participation Certificate",
      issuer: "INNIXO"
    },
    {
      name: "Intel Unnati AI Program Participation Certificate",
      issuer: "Intel"
    },
    {
      name: "Inter College Hackathon Participation Certificate",
      issuer: "Inter College"
    }
  ],
  links: {
    github: "https://github.com/Milan-Goswami",
    linkedin: "https://www.linkedin.com/in/milan-goswami01/",
    leetcode: "https://leetcode.com/u/pro_milan/"
  }
};
