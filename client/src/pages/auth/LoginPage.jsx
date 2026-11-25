import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/api';

const LoginPage = () => {
    const [formData, setFormData] = useState({
        email: '',
        password: ''
    });
    const [errors, setErrors] = useState({});
    const [apiError, setApiError] = useState('');
    const [loading, setLoading] = useState(false);
    const [successMessage, setSuccessMessage] = useState('');

    const { login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const from = location.state?.from?.pathname || '/dashboard';

    // Handle success message from navigation state (e.g., after signup)
    useEffect(() => {
        if (location.state?.message) {
            setSuccessMessage(location.state.message);
            // Clear the message from location state to prevent persistence
            window.history.replaceState({}, document.title);
        }
    }, [location]);

    // Email validation regex
    const validateEmail = (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    };

    // Form validation
    const validateForm = () => {
        const newErrors = {};

        if (!formData.email.trim()) {
            newErrors.email = 'Email is required';
        } else if (!validateEmail(formData.email)) {
            newErrors.email = 'Please enter a valid email address';
        }

        if (!formData.password) {
            newErrors.password = 'Password is required';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));

        // Clear field-specific error when user starts typing
        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }

        // Clear API error when user modifies form
        if (apiError) {
            setApiError('');
        }

        // Clear success message when user starts typing
        if (successMessage) {
            setSuccessMessage('');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Clear previous messages
        setApiError('');
        setSuccessMessage('');

        // Validate form
        if (!validateForm()) {
            return;
        }

        setLoading(true);

        try {
            const response = await api.post('/auth/login', {
                email: formData.email.trim(),
                password: formData.password
            });

            // Store token and user in AuthContext
            login(response.data);

            // Redirect to intended page or dashboard
            navigate(from, { replace: true });
        } catch (err) {
            if (err.response?.status === 401) {
                // LLD-01: Show "Invalid email or password" for 401
                setApiError('Invalid email or password');
            } else if (err.message === 'Invalid authentication response received from server') {
                setApiError('Login succeeded, but the server returned an invalid authentication payload.');
            } else {
                setApiError(
                    err.response?.data?.message || 'Login failed. Please verify the backend is running and CORS is configured.'
                );
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <div style={styles.header}>
                    <span style={styles.icon}>💊</span>
                    <h1 style={styles.title}>MedTracker Login</h1>
                </div>

                {successMessage && (
                    <div style={styles.success} role="alert" aria-live="polite">
                        {successMessage}
                    </div>
                )}

                {apiError && (
                    <div style={styles.apiError} role="alert" aria-live="assertive">
                        {apiError}
                    </div>
                )}

                <form onSubmit={handleSubmit} style={styles.form} noValidate>
                    <div style={styles.formGroup}>
                        <label htmlFor="email" style={styles.label}>
                            Email Address <span style={styles.required}>*</span>
                        </label>
                        <input
                            id="email"
                            name="email"
                            type="email"
                            value={formData.email}
                            onChange={handleChange}
                            required
                            autoComplete="email"
                            style={{
                                ...styles.input,
                                ...(errors.email ? styles.inputError : {})
                            }}
                            aria-required="true"
                            aria-invalid={!!errors.email}
                            aria-describedby={errors.email ? 'email-error' : undefined}
                        />
                        {errors.email && (
                            <span id="email-error" style={styles.fieldError} role="alert">
                                {errors.email}
                            </span>
                        )}
                    </div>

                    <div style={styles.formGroup}>
                        <label htmlFor="password" style={styles.label}>
                            Password <span style={styles.required}>*</span>
                        </label>
                        <input
                            id="password"
                            name="password"
                            type="password"
                            value={formData.password}
                            onChange={handleChange}
                            required
                            autoComplete="current-password"
                            style={{
                                ...styles.input,
                                ...(errors.password ? styles.inputError : {})
                            }}
                            aria-required="true"
                            aria-invalid={!!errors.password}
                            aria-describedby={errors.password ? 'password-error' : undefined}
                        />
                        {errors.password && (
                            <span id="password-error" style={styles.fieldError} role="alert">
                                {errors.password}
                            </span>
                        )}
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        style={{
                            ...styles.button,
                            ...(loading ? styles.buttonDisabled : {})
                        }}
                        aria-busy={loading}
                    >
                        {loading ? 'Logging in...' : 'Login'}
                    </button>
                </form>

                <p style={styles.signupLink}>
                    Don't have an account? <Link to="/signup" style={styles.link}>Sign up</Link>
                </p>
            </div>
        </div>
    );
};

const styles = {
    container: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        backgroundColor: '#f5f5f5',
        padding: '1rem'
    },
    card: {
        backgroundColor: '#fff',
        padding: '2rem',
        borderRadius: '8px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        width: '100%',
        maxWidth: '420px'
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '0.75rem',
        marginBottom: '1.5rem'
    },
    icon: {
        fontSize: '2rem'
    },
    title: {
        fontSize: '1.5rem',
        margin: 0,
        color: '#2c3e50'
    },
    success: {
        backgroundColor: '#d4edda',
        color: '#155724',
        padding: '0.75rem',
        borderRadius: '4px',
        marginBottom: '1rem',
        fontSize: '0.875rem',
        border: '1px solid #c3e6cb'
    },
    apiError: {
        backgroundColor: '#fee',
        color: '#c33',
        padding: '0.75rem',
        borderRadius: '4px',
        marginBottom: '1rem',
        fontSize: '0.875rem',
        border: '1px solid #fcc'
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: '1.25rem'
    },
    formGroup: {
        display: 'flex',
        flexDirection: 'column',
        gap: '0.5rem'
    },
    label: {
        fontSize: '0.875rem',
        fontWeight: '500',
        color: '#555'
    },
    required: {
        color: '#e74c3c'
    },
    input: {
        padding: '0.75rem',
        border: '1px solid #ddd',
        borderRadius: '4px',
        fontSize: '1rem',
        transition: 'border-color 0.2s'
    },
    inputError: {
        borderColor: '#e74c3c'
    },
    fieldError: {
        fontSize: '0.75rem',
        color: '#e74c3c',
        marginTop: '-0.25rem'
    },
    button: {
        padding: '0.75rem',
        backgroundColor: '#3498db',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        fontSize: '1rem',
        fontWeight: '500',
        cursor: 'pointer',
        transition: 'background-color 0.2s',
        marginTop: '0.5rem'
    },
    buttonDisabled: {
        backgroundColor: '#95a5a6',
        cursor: 'not-allowed'
    },
    signupLink: {
        marginTop: '1.5rem',
        textAlign: 'center',
        fontSize: '0.875rem',
        color: '#555'
    },
    link: {
        color: '#3498db',
        textDecoration: 'none',
        fontWeight: '500'
    }
};

export default LoginPage;
