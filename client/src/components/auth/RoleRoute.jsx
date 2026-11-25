import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

/**
 * RoleRoute - Blocks access if user role is not in allowed list
 * @param {string[]} allow - Array of allowed roles
 */
const RoleRoute = ({ children, allow = [] }) => {
    const { role, isAuthenticated } = useAuth();

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    if (!allow.includes(role)) {
        // User doesn't have permission - redirect to dashboard
        return <Navigate to="/dashboard" replace />;
    }

    return children;
};

export default RoleRoute;
