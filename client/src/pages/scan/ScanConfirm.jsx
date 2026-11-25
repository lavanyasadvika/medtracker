import { useState } from 'react';
import api from '../../api/api';
import '../inventory/MedicineForm.css';
import './ScanPage.css';

const CATEGORIES = [
    'Analgesic',
    'Antibiotic',
    'Antiseptic',
    'Antiviral',
    'Cardiovascular',
    'Dermatological',
    'Gastrointestinal',
    'Respiratory',
    'Other'
];

const SOURCE_LABELS = {
    BARCODE: '📷 Barcode Scan',
    GROQ: '🤖 AI Label Recognition',
    MANUAL: '✍️ Manual Entry'
};

/**
 * Pre-filled confirmation form shown after a successful /scan/upload call (FR-11).
 * The scan only returns drugName/expiryDate/source/manufacturer/barcode - the remaining
 * medicine fields are always entered/reviewed here before the record is persisted.
 */
const ScanConfirm = ({ scanResult, onCancel, onSuccess }) => {
    const source = scanResult?.source || 'MANUAL';

    const [formData, setFormData] = useState({
        drugName: scanResult?.drugName || '',
        manufacturer: scanResult?.manufacturer || '',
        category: '',
        batchNumber: '',
        barcodeData: scanResult?.barcode || '',
        expiryDate: scanResult?.expiryDate || '',
        quantity: '',
        price: '',
        lowStockThreshold: '',
        source
    });
    const [errors, setErrors] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState('');

    const validateForm = () => {
        const newErrors = {};

        if (!formData.drugName.trim()) {
            newErrors.drugName = 'Drug name is required';
        }

        if (!formData.category) {
            newErrors.category = 'Category is required';
        }

        if (!formData.expiryDate) {
            newErrors.expiryDate = 'Expiry date is required';
        } else {
            const expiryDate = new Date(formData.expiryDate);
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            if (expiryDate < today) {
                newErrors.expiryDate = 'Expiry date cannot be in the past';
            }
        }

        if (!formData.quantity) {
            newErrors.quantity = 'Quantity is required';
        } else if (parseInt(formData.quantity, 10) < 0) {
            newErrors.quantity = 'Quantity must be 0 or greater';
        }

        if (formData.lowStockThreshold && parseInt(formData.lowStockThreshold, 10) < 0) {
            newErrors.lowStockThreshold = 'Low stock threshold must be 0 or greater';
        }

        if (formData.price && parseFloat(formData.price) < 0) {
            newErrors.price = 'Price must be 0 or greater';
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
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitError('');

        if (!validateForm()) {
            return;
        }

        setSubmitting(true);

        try {
            const payload = {
                drugName: formData.drugName.trim(),
                manufacturer: formData.manufacturer.trim() || null,
                category: formData.category,
                batchNumber: formData.batchNumber.trim() || null,
                barcodeData: formData.barcodeData.trim() || null,
                expiryDate: formData.expiryDate,
                quantity: parseInt(formData.quantity, 10),
                price: formData.price ? parseFloat(formData.price) : null,
                lowStockThreshold: formData.lowStockThreshold ? parseInt(formData.lowStockThreshold, 10) : null,
                source: formData.source
            };

            // Persistence only happens here - the /scan/upload call never auto-saves.
            const response = await api.post('/medicines', payload);
            onSuccess(response.data);
        } catch (err) {
            console.error('Error saving scanned medicine:', err);
            setSubmitError(err.response?.data?.message || 'Failed to save medicine. Please try again.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="scan-confirm-card">
            {scanResult?.message && (
                <div className="scan-info-banner">ℹ️ {scanResult.message}</div>
            )}

            <div className="scan-source-badge">
                Source: <strong>{SOURCE_LABELS[source] || source}</strong>
            </div>

            <form onSubmit={handleSubmit} className="medicine-form">
                {submitError && <div className="error-text scan-submit-error">{submitError}</div>}

                <div className="form-group">
                    <label htmlFor="drugName">Drug Name *</label>
                    <input
                        type="text"
                        id="drugName"
                        name="drugName"
                        value={formData.drugName}
                        onChange={handleChange}
                        className={errors.drugName ? 'error' : ''}
                    />
                    {errors.drugName && <span className="error-text">{errors.drugName}</span>}
                </div>

                <div className="form-group">
                    <label htmlFor="category">Category *</label>
                    <select
                        id="category"
                        name="category"
                        value={formData.category}
                        onChange={handleChange}
                        className={errors.category ? 'error' : ''}
                    >
                        <option value="">Select category</option>
                        {CATEGORIES.map(cat => (
                            <option key={cat} value={cat}>{cat}</option>
                        ))}
                    </select>
                    {errors.category && <span className="error-text">{errors.category}</span>}
                </div>

                <div className="form-group">
                    <label htmlFor="manufacturer">Manufacturer</label>
                    <input
                        type="text"
                        id="manufacturer"
                        name="manufacturer"
                        value={formData.manufacturer}
                        onChange={handleChange}
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="batchNumber">Batch Number</label>
                    <input
                        type="text"
                        id="batchNumber"
                        name="batchNumber"
                        value={formData.batchNumber}
                        onChange={handleChange}
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="barcodeData">Barcode</label>
                    <input
                        type="text"
                        id="barcodeData"
                        name="barcodeData"
                        value={formData.barcodeData}
                        onChange={handleChange}
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="expiryDate">Expiry Date *</label>
                    <input
                        type="date"
                        id="expiryDate"
                        name="expiryDate"
                        value={formData.expiryDate}
                        onChange={handleChange}
                        className={errors.expiryDate ? 'error' : ''}
                    />
                    {errors.expiryDate && <span className="error-text">{errors.expiryDate}</span>}
                </div>

                <div className="form-group">
                    <label htmlFor="quantity">Quantity *</label>
                    <input
                        type="number"
                        id="quantity"
                        name="quantity"
                        value={formData.quantity}
                        onChange={handleChange}
                        step="1"
                        min="0"
                        className={errors.quantity ? 'error' : ''}
                    />
                    {errors.quantity && <span className="error-text">{errors.quantity}</span>}
                </div>

                <div className="form-group">
                    <label htmlFor="price">Price</label>
                    <input
                        type="number"
                        id="price"
                        name="price"
                        value={formData.price}
                        onChange={handleChange}
                        step="0.01"
                        min="0"
                        className={errors.price ? 'error' : ''}
                    />
                    {errors.price && <span className="error-text">{errors.price}</span>}
                </div>

                <div className="form-group">
                    <label htmlFor="lowStockThreshold">Low Stock Threshold</label>
                    <input
                        type="number"
                        id="lowStockThreshold"
                        name="lowStockThreshold"
                        value={formData.lowStockThreshold}
                        onChange={handleChange}
                        step="1"
                        min="0"
                        className={errors.lowStockThreshold ? 'error' : ''}
                    />
                    {errors.lowStockThreshold && <span className="error-text">{errors.lowStockThreshold}</span>}
                </div>

                <div className="form-actions">
                    <button type="button" onClick={onCancel} className="btn btn-secondary" disabled={submitting}>
                        Back
                    </button>
                    <button type="submit" className="btn btn-primary" disabled={submitting}>
                        {submitting ? 'Saving...' : 'Confirm & Save'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default ScanConfirm;
