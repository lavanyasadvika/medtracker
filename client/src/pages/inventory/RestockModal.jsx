import { useState } from 'react';
import api from '../../api/api';
import './MedicineForm.css';

const RestockModal = ({ medicine, onClose, onSuccess }) => {
    const [quantity, setQuantity] = useState('');
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();

        const restockQty = parseInt(quantity, 10);

        if (!quantity || restockQty <= 0) {
            setError('Please enter a valid quantity greater than 0');
            return;
        }

        setSubmitting(true);
        setError('');

        try {
            const response = await api.put(`/medicines/${medicine.id}/restock`, {
                quantity: restockQty
            });
            onSuccess(response.data);
        } catch (err) {
            console.error('Error restocking medicine:', err);

            if (err.response?.data?.message) {
                setError(err.response.data.message);
            } else {
                setError('Failed to restock medicine. Please try again.');
            }
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content modal-small" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Restock Medicine</h2>
                    <button onClick={onClose} className="close-btn">×</button>
                </div>

                <div className="medicine-info">
                    <p><strong>Medicine:</strong> {medicine.drugName}</p>
                    <p><strong>Current Quantity:</strong> {medicine.quantity}</p>
                    <p><strong>Low Stock Threshold:</strong> {medicine.lowStockThreshold ?? 'Not set'}</p>
                </div>

                <form onSubmit={handleSubmit} className="medicine-form">
                    <div className="form-group">
                        <label htmlFor="quantity">Quantity to Add *</label>
                        <input
                            type="number"
                            id="quantity"
                            value={quantity}
                            onChange={(e) => {
                                setQuantity(e.target.value);
                                setError('');
                            }}
                            step="1"
                            min="1"
                            className={error ? 'error' : ''}
                            autoFocus
                        />
                        {error && <span className="error-text">{error}</span>}
                    </div>

                    {quantity && parseInt(quantity, 10) > 0 && (
                        <div className="info-box">
                            <p>New quantity will be: <strong>{medicine.quantity + parseInt(quantity, 10)}</strong></p>
                        </div>
                    )}

                    <div className="form-actions">
                        <button type="button" onClick={onClose} className="btn btn-secondary">
                            Cancel
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Restocking...' : 'Restock'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default RestockModal;
