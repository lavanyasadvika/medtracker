import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

const AuthContext = createContext(null);

const AUTH_TOKEN_KEY = 'authToken';
const AUTH_USER_KEY = 'authUser';

const buildUserFromAuthResponse = (authResponse) => ({
    name: authResponse.name,
    email: authResponse.email,
    role: authResponse.role
});

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(null);
    const [user, setUser] = useState(null);
    const [role, setRole] = useState(null);
    const [loading, setLoading] = useState(true);

    // Initialize auth state from sessionStorage on mount
    useEffect(() => {
        const storedToken = sessionStorage.getItem(AUTH_TOKEN_KEY);
        const storedUser = sessionStorage.getItem(AUTH_USER_KEY);

        if (storedToken && storedUser) {
            try {
                const userData = JSON.parse(storedUser);
                setToken(storedToken);
                setUser(userData);
                setRole(userData.role);
            } catch (error) {
                console.error('Failed to parse stored user data:', error);
                sessionStorage.removeItem(AUTH_TOKEN_KEY);
                sessionStorage.removeItem(AUTH_USER_KEY);
            }
        }
        setLoading(false);
    }, []);

    /**
     * Login function
     * @param {Object} authResponse - Response from /auth/login containing { token, tokenType, expiresInMs, role, name, email }
     */
    const login = (authResponse) => {
        const authToken = authResponse?.token;
        const authUser = buildUserFromAuthResponse(authResponse || {});

        if (!authToken || !authUser.role) {
            throw new Error('Invalid authentication response received from server');
        }

        // Store in memory
        setToken(authToken);
        setUser(authUser);
        setRole(authUser.role);

        // Persist to sessionStorage for reload survival
        sessionStorage.setItem(AUTH_TOKEN_KEY, authToken);
        sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(authUser));
    };

    /**
     * Logout function - clears all auth state
     */
    const logout = () => {
        setToken(null);
        setUser(null);
        setRole(null);
        sessionStorage.removeItem(AUTH_TOKEN_KEY);
        sessionStorage.removeItem(AUTH_USER_KEY);
    };

    const value = useMemo(() => ({
        token,
        user,
        role,
        login,
        logout,
        isAuthenticated: !!token,
        loading
    }), [loading, role, token, user]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
