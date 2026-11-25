import { useState, useEffect } from 'react';
import api from '../../api/api';
import '../inventory/MedicineForm.css';
import './AdminPage.css';

/** GET/PUT /api/admin/settings - expiry lead time (FR-19) + default low-stock threshold (FR-24). */
const AlertSettings = () => {
    const [formData, setFormData] = useState({ expiryLeadDays: '', defaultLowStockThreshold: '' });
    const [errors, setErrors] = useState({});
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState('');

    useEffect(() => {
        fetchSettings();
    }, []);

    const fetchSettings = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.get('/admin/settings');
            setFormData({
                expiryLeadDays: response.data.expiryLeadDays ?? '',
                defaultLowStockThreshold: response.data.defaultLowStockThreshold ?? ''
            });
        } catch (err) {
            console.error('Error fetching alert settings:', err);
            setError('Failed to load alert settings. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const validate = () => {
        const newErrors = {};

        const days = parseInt(formData.expiryLeadDays, 10);
        if (formData.expiryLeadDays === '' || Number.isNaN(days) || days < 0 || days > 3650) {
            newErrors.expiryLeadDays = 'Expiry lead days must be between 0 and 3650';
        }

        const threshold = parseFloat(formData.defaultLowStockThreshold);
        if (formData.defaultLowStockThreshold === '' || Number.isNaN(threshold) || threshold <= 0) {
            newErrors.defaultLowStockThreshold = 'Default low-stock threshold must be greater than zero';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: '' }));
        }
        if (successMessage) {
            setSuccessMessage('');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSuccessMessage('');
        setError(null);

        if (!validate()) {
            return;
        }

        setSaving(true);
        try {
            const response = await api.put('/admin/settings', {
                expiryLeadDays: parseInt(formData.expiryLeadDays, 10),
                defaultLowStockThreshold: parseFloat(formData.defaultLowStockThreshold)
            });
            setFormData({
                expiryLeadDays: response.data.expiryLeadDays,
                defaultLowStockThreshold: response.data.defaultLowStockThreshold
            });
            setSuccessMessage('Alert settings updated successfully.');
        } catch (err) {
            console.error('Error updating alert settings:', err);
            setError(err.response?.data?.message || 'Failed to update alert settings. Please try again.');
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <div className="loading-spinner">Loading alert settings...</div>;
    }

    return (
        <div className="admin-section">
            <h2>Alert Settings</h2>
            <p className="admin-section-subtitle">
                Controls the daily expiry/low-stock alert emails sent to ADMIN and PHARMACIST users.
                Changes apply immediately and reset to the configured defaults on server restart.
            </p>

            {error && <div className="error-message">{error}</div>}
            {successMessage && <div className="admin-success-message">{successMessage}</div>}

            <form onSubmit={handleSubmit} className="medicine-form admin-settings-form">
                <div className="form-group">
                    <label htmlFor="expiryLeadDays">Expiry Lead Time (days) *</label>
                    <input
                        type="number"
                        id="expiryLeadDays"
                        name="expiryLeadDays"
                        value={formData.expiryLeadDays}
                        onChange={handleChange}
                        min="0"
                        max="3650"
                        className={errors.expiryLeadDays ? 'error' : ''}
                    />
                    {errors.expiryLeadDays && <span className="error-text">{errors.expiryLeadDays}</span>}
                </div>

                <div className="form-group">
                    <label htmlFor="defaultLowStockThreshold">Default Low Stock Threshold *</label>
                    <input
                        type="number"
                        id="defaultLowStockThreshold"
                        name="defaultLowStockThreshold"
                        value={formData.defaultLowStockThreshold}
                        onChange={handleChange}
                        step="0.01"
                        min="0.01"
                        className={errors.defaultLowStockThreshold ? 'error' : ''}
                    />
                    {errors.defaultLowStockThreshold && (
                        <span className="error-text">{errors.defaultLowStockThreshold}</span>
                    )}
                </div>

                <div className="form-actions">
                    <button type="submit" className="btn btn-primary" disabled={saving}>
                        {saving ? 'Saving...' : 'Save Settings'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default AlertSettings;
