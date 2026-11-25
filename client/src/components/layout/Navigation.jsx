import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { hasRoutePermission } from '../../utils/rolePermissions';

const Navigation = () => {
    const { role, logout, user } = useAuth();

    const navItems = [
        { path: '/dashboard', label: 'Dashboard', icon: '📊' },
        { path: '/inventory', label: 'Inventory', icon: '📦' },
        { path: '/scan', label: 'Scan', icon: '📷' },
        { path: '/reports', label: 'Reports', icon: '📈' },
        { path: '/admin', label: 'Admin', icon: '⚙️' }
    ];

    // Filter nav items based on role permissions
    const accessibleNavItems = navItems.filter((item) =>
        hasRoutePermission(role, item.path)
    );

    const handleLogout = () => {
        logout();
    };

    return (
        <nav
            style={styles.nav}
            role="navigation"
            aria-label="Main navigation"
        >
            <div style={styles.navContainer}>
                <div style={styles.brand}>
                    <span style={styles.brandIcon}>💊</span>
                    <h1 style={styles.brandText}>MedTracker</h1>
                </div>

                <ul style={styles.navList} role="menubar">
                    {accessibleNavItems.map((item) => (
                        <li key={item.path} style={styles.navItem} role="none">
                            <NavLink
                                to={item.path}
                                style={({ isActive }) => ({
                                    ...styles.navLink,
                                    ...(isActive ? styles.navLinkActive : {})
                                })}
                                role="menuitem"
                            >
                                <span aria-hidden="true">{item.icon}</span>
                                <span>{item.label}</span>
                            </NavLink>
                        </li>
                    ))}
                </ul>

                <div style={styles.userSection}>
          <span style={styles.userName} aria-label={`Logged in as ${user?.name || 'User'}`}>
            {user?.name || 'User'}
          </span>
                    <span style={styles.userRole} aria-label={`Role: ${role}`}>
            ({role})
          </span>
                    <button
                        onClick={handleLogout}
                        style={styles.logoutButton}
                        aria-label="Logout"
                    >
                        Logout
                    </button>
                </div>
            </div>
        </nav>
    );
};

const styles = {
    nav: {
        backgroundColor: '#2c3e50',
        color: '#ecf0f1',
        padding: '0 1rem',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
    },
    navContainer: {
        maxWidth: '1200px',
        margin: '0 auto',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '2rem',
        minHeight: '60px'
    },
    brand: {
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem'
    },
    brandIcon: {
        fontSize: '1.5rem'
    },
    brandText: {
        fontSize: '1.25rem',
        fontWeight: '600',
        margin: 0
    },
    navList: {
        display: 'flex',
        listStyle: 'none',
        gap: '0.5rem',
        margin: 0,
        padding: 0,
        flex: 1
    },
    navItem: {
        margin: 0
    },
    navLink: {
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
        padding: '0.5rem 1rem',
        color: '#ecf0f1',
        textDecoration: 'none',
        borderRadius: '4px',
        transition: 'background-color 0.2s'
    },
    navLinkActive: {
        backgroundColor: '#34495e',
        fontWeight: '600'
    },
    userSection: {
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem'
    },
    userName: {
        fontWeight: '500'
    },
    userRole: {
        fontSize: '0.875rem',
        opacity: 0.8
    },
    logoutButton: {
        padding: '0.5rem 1rem',
        backgroundColor: '#e74c3c',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer',
        fontSize: '0.875rem',
        fontWeight: '500',
        transition: 'background-color 0.2s'
    }
};

export default Navigation;
