import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Login from './components/Login';
import Register from './components/Register';
import Home from './components/Home';
import RestaurantDashboard from './components/RestaurantDashboard';
import NGODashboard from './components/NGODashboard';
import AdminDashboard from './components/AdminDashboard';
import VolunteerDashboard from './components/VolunteerDashboard';
import './App.css';

const PrivateRoute = ({ children }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" />;
};

const DashboardRouter = () => {
  const { isRestaurant, isNGO, isAdmin, isVolunteer } = useAuth();
  
  if (isAdmin) {
    return <AdminDashboard />;
  } else if (isRestaurant) {
    return <RestaurantDashboard />;
  } else if (isNGO) {
    return <NGODashboard />;
  } else if (isVolunteer) {
    return <VolunteerDashboard />;
  }
  return <Navigate to="/login" />;
};

function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Router>
      <div className="App">
        <Routes>
          <Route 
            path="/login" 
            element={isAuthenticated ? <Navigate to="/dashboard" /> : <Login />} 
          />
          <Route 
            path="/register" 
            element={isAuthenticated ? <Navigate to="/dashboard" /> : <Register />} 
          />
          <Route 
            path="/dashboard" 
            element={
              <PrivateRoute>
                <DashboardRouter />
              </PrivateRoute>
            } 
          />
          <Route 
            path="/" 
            element={<Home />}
          />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
