import { useState } from 'react';
import api from '../../api/api';
import './MedicineForm.css';

const DispenseModal = ({ medicine, onClose, onSuccess }) => {
    const [quantity, setQuantity] = useState('');
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();

        const dispenseQty = parseInt(quantity, 10);

        if (!quantity || dispenseQty <= 0) {
            setError('Please enter a valid quantity greater than 0');
            return;
        }

        if (dispenseQty > medicine.quantity) {
            setError(`Cannot dispense ${dispenseQty}. Only ${medicine.quantity} available.`);
            return;
        }

        setSubmitting(true);
        setError('');

        try {
            const response = await api.post(`/medicines/${medicine.id}/dispense`, {
                quantity: dispenseQty
            });
            onSuccess(response.data);
        } catch (err) {
            console.error('Error dispensing medicine:', err);

            if (err.response?.status === 400 && err.response?.data?.available !== undefined) {
                setError(`Insufficient stock, available: ${err.response.data.available}`);
            } else if (err.response?.data?.message) {
                setError(err.response.data.message);
            } else {
                setError('Failed to dispense medicine. Please try again.');
            }
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content modal-small" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Dispense Medicine</h2>
                    <button onClick={onClose} className="close-btn">×</button>
                </div>

                <div className="medicine-info">
                    <p><strong>Medicine:</strong> {medicine.drugName}</p>
                    <p><strong>Available Quantity:</strong> {medicine.quantity}</p>
                </div>

                <form onSubmit={handleSubmit} className="medicine-form">
                    <div className="form-group">
                        <label htmlFor="quantity">Quantity to Dispense *</label>
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
                            max={medicine.quantity}
                            className={error ? 'error' : ''}
                            autoFocus
                        />
                        {error && <span className="error-text">{error}</span>}
                    </div>

                    <div className="form-actions">
                        <button type="button" onClick={onClose} className="btn btn-secondary">
                            Cancel
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Dispensing...' : 'Dispense'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default DispenseModal;
