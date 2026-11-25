import React from 'react';
import { Outlet } from 'react-router-dom';
import Navigation from './Navigation';

const Layout = () => {
    return (
        <div style={styles.container}>
            <a href="#main-content" className="skip-link">
                Skip to main content
            </a>
            <Navigation />
            <main id="main-content" style={styles.main} role="main">
                <Outlet />
            </main>
        </div>
    );
};

const styles = {
    container: {
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column'
    },
    main: {
        flex: 1,
        maxWidth: '1200px',
        width: '100%',
        margin: '0 auto',
        padding: '2rem 1rem'
    }
};

export default Layout;
