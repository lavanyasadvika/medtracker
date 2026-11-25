import { useState } from 'react';
import api from '../../api/api';
import './MedicineForm.css';

const EditMedicine = ({ medicine, onClose, onSuccess }) => {
    const [formData, setFormData] = useState({
        drugName: medicine.drugName || '',
        manufacturer: medicine.manufacturer || '',
        category: medicine.category || '',
        batchNumber: medicine.batchNumber || '',
        barcodeData: medicine.barcodeData || '',
        expiryDate: medicine.expiryDate || '',
        quantity: medicine.quantity || '',
        price: medicine.price || '',
        lowStockThreshold: medicine.lowStockThreshold || '',
        source: medicine.source || 'MANUAL'
    });
    const [errors, setErrors] = useState({});
    const [submitting, setSubmitting] = useState(false);

    const categories = [
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

    const validateForm = () => {
        const newErrors = {};

        if (!formData.drugName.trim()) {
            newErrors.drugName = 'Drug name is required';
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
        } else if (parseInt(formData.quantity, 10) <= 0) {
            newErrors.quantity = 'Quantity must be greater than 0';
        }

        if (formData.lowStockThreshold && parseInt(formData.lowStockThreshold, 10) <= 0) {
            newErrors.lowStockThreshold = 'Low stock threshold must be greater than 0';
        }

        if (formData.price && parseFloat(formData.price) <= 0) {
            newErrors.price = 'Price must be greater than 0';
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

            const response = await api.put(`/medicines/${medicine.id}`, payload);
            onSuccess(response.data);
        } catch (err) {
            console.error('Error updating medicine:', err);

            if (err.response?.data?.message) {
                alert(err.response.data.message);
            } else {
                alert('Failed to update medicine. Please try again.');
            }
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Edit Medicine</h2>
                    <button onClick={onClose} className="close-btn">×</button>
                </div>

                <form onSubmit={handleSubmit} className="medicine-form">
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
                        <label htmlFor="category">Category</label>
                        <select
                            id="category"
                            name="category"
                            value={formData.category}
                            onChange={handleChange}
                            className={errors.category ? 'error' : ''}
                        >
                            <option value="">Select category</option>
                            {categories.map(cat => (
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
                            min="1"
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
                            min="0.01"
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
                            min="1"
                            className={errors.lowStockThreshold ? 'error' : ''}
                        />
                        {errors.lowStockThreshold && <span className="error-text">{errors.lowStockThreshold}</span>}
                    </div>

                    <div className="form-actions">
                        <button type="button" onClick={onClose} className="btn btn-secondary">
                            Cancel
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Updating...' : 'Update Medicine'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default EditMedicine;
