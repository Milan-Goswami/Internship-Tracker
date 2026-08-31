# InternTrack — Internship Application Intelligence Platform

InternTrack is a production-grade full-stack platform designed for students, fresh graduates, and early-career job seekers to manage the complete end-to-end job search lifecycle.

---

## 🛠️ Technology Stack

- **Backend:** Java 21 LTS, Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Hibernate 6.5, MySQL Connector/J 8.3, Maven
- **Frontend:** React 18.3, JavaScript (ES2022), Vite 5, Custom Vanilla CSS Design System (Design Tokens)
- **Database:** MySQL 8.0+
- **Security:** Spring Security 6.3, Stateless JWT (HMAC-SHA512), Refresh Token Rotation

---

## 📋 Prerequisites

- **Java Development Kit (JDK):** Version 21 LTS
- **Node.js:** Version 20+ LTS (Node.js 24 LTS recommended)
- **Maven:** Version 3.9+ (or use the included mvnw wrapper)
- **MySQL Server:** Version 8.0+

---

## 🚀 Quick Start Guide

### 1. Environment Configuration

Copy the configuration template and configure your local database credentials:

`ash
cp .env.example .env
`

### 2. Backend Startup (Spring Boot)

Navigate to the ackend directory and run via Maven:

`ash
cd backend
./mvnw clean compile
./mvnw spring-boot:run
`

The backend server starts on port 8080.  
Verify health endpoint: GET http://localhost:8080/api/v1/health

### 3. Frontend Startup (React + Vite)

Navigate to the rontend directory and start the development server:

`ash
cd frontend
npm install
npm run dev
`

The frontend server starts on http://localhost:5173.

---

## 📁 Project Structure

`
InternTrack/
├── backend/                  # Spring Boot 3.3.5 Modular Monolith
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/interntrack/api/
│   │   │   │   ├── config/      # Infrastructure & security configuration
│   │   │   │   ├── controller/  # REST API Controllers (Health, Auth, Apps, etc.)
│   │   │   │   ├── dto/         # Request & Response Data Transfer Objects
│   │   │   │   ├── entity/      # JPA Domain Entities
│   │   │   │   ├── exception/   # Global exception handling & API error envelopes
│   │   │   │   ├── repository/  # Spring Data JPA Repositories
│   │   │   │   ├── security/    # JWT filters, token rotation, ownership guards
│   │   │   │   └── service/     # Business logic & skill matching engine
│   │   │   └── resources/       # application.yml configuration
│   │   └── test/                # JUnit 5 & Mockito test suites
│   ├── pom.xml
│   └── mvnw / mvnw.cmd
│
├── frontend/                 # React 18 / Vite Single Page Application
│   ├── src/
│   │   ├── assets/          # Static icons & branding assets
│   │   ├── components/      # Reusable UI components (Tables, Badges, Modals)
│   │   ├── context/         # React state context providers
│   │   ├── hooks/           # Custom React hooks
│   │   ├── layouts/         # App layouts (Sidebar, Command Bar)
│   │   ├── pages/           # Screen views (Dashboard, Applications, Skills)
│   │   ├── routes/          # React Router configuration
│   │   ├── services/        # Axios API client services
│   │   ├── styles/          # Design Tokens (tokens.css) & Global Reset
│   │   └── utils/           # Helper functions & formatting utilities
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
├── .env.example              # Environment variables template
├── .gitignore                # Version control ignore rules
└── *.md                      # Reconciled Architecture & Specification Documents
`

---

## 📖 Architecture & Planning Documents

For full product discovery, domain models, and API specifications, consult the project planning documents in the root directory:
- [PRODUCT_REQUIREMENTS.md](PRODUCT_REQUIREMENTS.md)
- [DOMAIN_MODEL.md](DOMAIN_MODEL.md)
- [SYSTEM_ARCHITECTURE.md](SYSTEM_ARCHITECTURE.md)
- [SECURITY_PLAN.md](SECURITY_PLAN.md)
- [API_PLAN.md](API_PLAN.md)
- [UI_DESIGN_SYSTEM.md](UI_DESIGN_SYSTEM.md)
- [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md)
- [DECISIONS.md](DECISIONS.md)