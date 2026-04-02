import React from 'react';
import { Link } from 'react-router-dom';

const Home = () => {
  return (
    <div className="home-page">
      <nav className="navbar">
        <div className="navbar-brand">
          <Link to="/" className="brand-link">
            <span className="brand-icon">🍽️</span>
            <span className="brand-text">Food Rescue</span>
          </Link>
        </div>
        <div className="navbar-links">
          <Link to="/login" className="btn btn-outline">Sign In</Link>
          <Link to="/register" className="btn btn-primary">Register</Link>
        </div>
      </nav>

      <section className="hero">
        <div className="hero-container">
          <h1 className="hero-title">Fight Hunger,<br />Reduce Waste</h1>
          <p className="hero-subtitle">
            Connecting food donors with NGOs to reduce waste and feed those in need.
            Join our community making a difference one meal at a time.
          </p>
          <div className="hero-cta">
            <Link to="/register" className="btn btn-primary btn-lg">Get Started</Link>
            <Link to="/login" className="btn btn-secondary btn-lg">Sign In</Link>
          </div>
        </div>
      </section>

      <section className="features">
        <div className="container">
          <h2 className="section-title">How It Works</h2>
          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon icon-orange">🍕</div>
              <h3>Donors Post</h3>
              <p>Restaurants and individuals list surplus food available for donation</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon icon-blue">🤝</div>
              <h3>NGOs Claim</h3>
              <p>Verified NGOs browse and claim available food for their causes</p>
            </div>
            <div className="feature-card">
              <div className="feature-icon icon-green">🚚</div>
              <h3>Volunteers Deliver</h3>
              <p>Volunteers pick up and deliver food to beneficiaries in need</p>
            </div>
          </div>
        </div>
      </section>

      <section className="roles">
        <div className="container">
          <h2 className="section-title">Join Us Today</h2>
          <div className="roles-grid">
            <div className="role-card role-restaurant">
              <div className="role-icon">🏪</div>
              <h3>Restaurant</h3>
              <p>Donate surplus food and help reduce waste while feeding those in need</p>
              <Link to="/register" className="role-link">Join as Restaurant →</Link>
            </div>
            <div className="role-card role-ngo">
              <div className="role-icon">🏛️</div>
              <h3>NGO</h3>
              <p>Claim food donations for your organization and distribute to beneficiaries</p>
              <Link to="/register" className="role-link">Join as NGO →</Link>
            </div>
            <div className="role-card role-volunteer">
              <div className="role-icon">🚐</div>
              <h3>Volunteer</h3>
              <p>Help deliver food from donors to NGOs and make a direct impact</p>
              <Link to="/register" className="role-link">Join as Volunteer →</Link>
            </div>
          </div>
        </div>
      </section>

      <section className="cta-section">
        <div className="container">
          <h2>Ready to Make a Difference?</h2>
          <p>Join thousands of donors, NGOs, and volunteers fighting hunger and food waste.</p>
          <Link to="/register" className="btn btn-primary btn-lg">Get Started Today</Link>
        </div>
      </section>

      <footer className="home-footer">
        <p>&copy; 2024 Food Rescue. All rights reserved.</p>
      </footer>
    </div>
  );
};

export default Home;
