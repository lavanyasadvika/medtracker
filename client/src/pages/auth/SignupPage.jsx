import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../api/api';

const SignupPage = () => {
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        password: '',
        confirmPassword: '',
        role: 'VIEWER'
    });
    const [errors, setErrors] = useState({});
    const [apiError, setApiError] = useState('');
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);

    const navigate = useNavigate();

    // Email validation regex
    const validateEmail = (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    };

    // Form validation
    const validateForm = () => {
        const newErrors = {};

        if (!formData.name.trim()) {
            newErrors.name = 'Name is required';
        } else if (formData.name.trim().length < 2) {
            newErrors.name = 'Name must be at least 2 characters';
        }

        if (!formData.email.trim()) {
            newErrors.email = 'Email is required';
        } else if (!validateEmail(formData.email)) {
            newErrors.email = 'Please enter a valid email address';
        }

        if (!formData.password) {
            newErrors.password = 'Password is required';
        } else if (formData.password.length < 8) {
            newErrors.password = 'Password must be at least 8 characters';
        } else if (formData.password.length > 72) {
            newErrors.password = 'Password must not exceed 72 characters';
        }

        if (!formData.confirmPassword) {
            newErrors.confirmPassword = 'Please confirm your password';
        } else if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Passwords do not match';
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
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Clear previous errors
        setApiError('');
        setSuccess(false);

        // Validate form
        if (!validateForm()) {
            return;
        }

        setLoading(true);

        try {
            await api.post('/auth/register', {
                name: formData.name.trim(),
                email: formData.email.trim(),
                password: formData.password,
                role: formData.role
            });

            // Show success message
            setSuccess(true);

            // Redirect to login after 2 seconds
            setTimeout(() => {
                navigate('/login', {
                    state: { message: 'Account created successfully. Please log in.' }
                });
            }, 2000);
        } catch (err) {
            if (err.response?.status === 409) {
                // Handle duplicate email as field error
                setErrors(prev => ({
                    ...prev,
                    email: 'This email is already registered'
                }));
            } else {
                setApiError(
                    err.response?.data?.message || 'Registration failed. Please verify the backend is running and CORS is configured.'
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
                    <h1 style={styles.title}>Create Account</h1>
                </div>

                {apiError && (
                    <div style={styles.apiError} role="alert" aria-live="assertive">
                        {apiError}
                    </div>
                )}

                {success && (
                    <div style={styles.success} role="alert" aria-live="polite">
                        Account created successfully! Redirecting to login...
                    </div>
                )}

                <form onSubmit={handleSubmit} style={styles.form} noValidate>
                    <div style={styles.formGroup}>
                        <label htmlFor="name" style={styles.label}>
                            Full Name <span style={styles.required}>*</span>
                        </label>
                        <input
                            id="name"
                            name="name"
                            type="text"
                            value={formData.name}
                            onChange={handleChange}
                            required
                            autoComplete="name"
                            style={{
                                ...styles.input,
                                ...(errors.name ? styles.inputError : {})
                            }}
                            aria-required="true"
                            aria-invalid={!!errors.name}
                            aria-describedby={errors.name ? 'name-error' : undefined}
                        />
                        {errors.name && (
                            <span id="name-error" style={styles.fieldError} role="alert">
                {errors.name}
              </span>
                        )}
                    </div>

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
                            autoComplete="new-password"
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

                    <div style={styles.formGroup}>
                        <label htmlFor="confirmPassword" style={styles.label}>
                            Confirm Password <span style={styles.required}>*</span>
                        </label>
                        <input
                            id="confirmPassword"
                            name="confirmPassword"
                            type="password"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            required
                            autoComplete="new-password"
                            style={{
                                ...styles.input,
                                ...(errors.confirmPassword ? styles.inputError : {})
                            }}
                            aria-required="true"
                            aria-invalid={!!errors.confirmPassword}
                            aria-describedby={errors.confirmPassword ? 'confirm-password-error' : undefined}
                        />
                        {errors.confirmPassword && (
                            <span id="confirm-password-error" style={styles.fieldError} role="alert">
                {errors.confirmPassword}
              </span>
                        )}
                    </div>

                    <div style={styles.formGroup}>
                        <label htmlFor="role" style={styles.label}>
                            Role
                        </label>
                        <select
                            id="role"
                            name="role"
                            value={formData.role}
                            onChange={handleChange}
                            style={styles.select}
                            aria-label="Select user role"
                        >
                            <option value="USER">User</option>
                            <option value="VIEWER">Viewer</option>
                            <option value="PHARMACIST">Pharmacist</option>
                            <option value="ADMIN">Admin</option>
                        </select>
                        <span style={styles.helpText}>
              Public registration supports server roles USER, VIEWER, PHARMACIST, and ADMIN for demo access.
            </span>
                    </div>

                    <button
                        type="submit"
                        disabled={loading || success}
                        style={{
                            ...styles.button,
                            ...(loading || success ? styles.buttonDisabled : {})
                        }}
                        aria-busy={loading}
                    >
                        {loading ? 'Creating account...' : success ? 'Success!' : 'Sign Up'}
                    </button>
                </form>

                <p style={styles.loginLink}>
                    Already have an account? <Link to="/login" style={styles.link}>Login</Link>
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
    apiError: {
        backgroundColor: '#fee',
        color: '#c33',
        padding: '0.75rem',
        borderRadius: '4px',
        marginBottom: '1rem',
        fontSize: '0.875rem',
        border: '1px solid #fcc'
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
    select: {
        padding: '0.75rem',
        border: '1px solid #ddd',
        borderRadius: '4px',
        fontSize: '1rem',
        backgroundColor: '#fff',
        cursor: 'pointer'
    },
    fieldError: {
        fontSize: '0.75rem',
        color: '#e74c3c',
        marginTop: '-0.25rem'
    },
    helpText: {
        fontSize: '0.75rem',
        color: '#7f8c8d',
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
    loginLink: {
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

export default SignupPage;
