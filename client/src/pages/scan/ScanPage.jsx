import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/api';
import AddMedicine from '../inventory/AddMedicine';
import ScanConfirm from './ScanConfirm';
import './ScanPage.css';

const DEFAULT_FAILURE_MESSAGE =
    'Unable to extract medicine details from the image. Please retake the photo or enter details manually.';

/**
 * Scan flow (BRD scan flowchart / LLD-02): upload image -> POST /api/scan/upload ->
 * pre-fill ScanConfirm on success, or offer retake/manual-entry on 422 extraction failure.
 * The scan call never persists anything - saving only happens when ScanConfirm submits
 * to POST /api/medicines.
 */
const ScanPage = () => {
    const navigate = useNavigate();
    const galleryInputRef = useRef(null);
    const cameraInputRef = useRef(null);

    const [imageFile, setImageFile] = useState(null);
    const [previewUrl, setPreviewUrl] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [scanResult, setScanResult] = useState(null);
    const [scanFailure, setScanFailure] = useState(null);
    const [showManualEntry, setShowManualEntry] = useState(false);

    const resetScan = () => {
        setImageFile(null);
        setPreviewUrl(null);
        setScanResult(null);
        setScanFailure(null);
    };

    const handleFileSelected = (e) => {
        const file = e.target.files?.[0];
        e.target.value = '';
        if (!file) {
            return;
        }

        setScanResult(null);
        setScanFailure(null);
        setImageFile(file);
        setPreviewUrl(URL.createObjectURL(file));
    };

    const handleUpload = async () => {
        if (!imageFile) {
            return;
        }

        setUploading(true);
        setScanFailure(null);

        try {
            const formData = new FormData();
            formData.append('image', imageFile);

            // Unset the instance's default JSON header so axios sends the raw FormData and
            // the browser can set 'multipart/form-data; boundary=...' itself.
            const response = await api.post('/scan/upload', formData, {
                headers: { 'Content-Type': undefined }
            });
            setScanResult(response.data);
        } catch (err) {
            console.error('Error scanning image:', err);
            setScanFailure({
                message: err.response?.data?.message || DEFAULT_FAILURE_MESSAGE
            });
        } finally {
            setUploading(false);
        }
    };

    const handleScanConfirmed = () => {
        navigate('/inventory');
    };

    const handleManualEntrySuccess = () => {
        setShowManualEntry(false);
        navigate('/inventory');
    };

    if (scanResult) {
        return (
            <div className="scan-container">
                <div className="scan-header">
                    <h1>Confirm Scanned Medicine</h1>
                    <p className="scan-subtitle">Review and complete the details before saving.</p>
                </div>
                <ScanConfirm
                    scanResult={scanResult}
                    onCancel={resetScan}
                    onSuccess={handleScanConfirmed}
                />
            </div>
        );
    }

    return (
        <div className="scan-container">
            <div className="scan-header">
                <h1>Scan Medicine</h1>
                <p className="scan-subtitle">
                    Upload a photo of the medicine label or barcode to auto-fill the inventory form.
                </p>
            </div>

            <div className="scan-card">
                {previewUrl ? (
                    <div className="scan-preview">
                        <img src={previewUrl} alt="Selected medicine" className="scan-preview-image" />
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={resetScan}
                            disabled={uploading}
                        >
                            Choose a Different Image
                        </button>
                    </div>
                ) : (
                    <div className="scan-dropzone">
                        <span className="scan-icon" aria-hidden="true">📷</span>
                        <p>Select a photo of the medicine label or barcode</p>
                        <div className="scan-upload-actions">
                            <button
                                type="button"
                                className="btn btn-primary"
                                onClick={() => galleryInputRef.current?.click()}
                            >
                                Choose from Gallery
                            </button>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => cameraInputRef.current?.click()}
                            >
                                Use Camera
                            </button>
                        </div>

                        {/* Two inputs: plain file picker vs. capture="environment" to prefer the rear camera on mobile. */}
                        <input
                            ref={galleryInputRef}
                            type="file"
                            accept="image/*"
                            onChange={handleFileSelected}
                            className="scan-file-input"
                        />
                        <input
                            ref={cameraInputRef}
                            type="file"
                            accept="image/*"
                            capture="environment"
                            onChange={handleFileSelected}
                            className="scan-file-input"
                        />
                    </div>
                )}

                {imageFile && (
                    <button
                        type="button"
                        className="btn btn-primary scan-submit-btn"
                        onClick={handleUpload}
                        disabled={uploading}
                    >
                        {uploading ? 'Scanning...' : 'Scan Image'}
                    </button>
                )}

                {scanFailure && (
                    <div className="scan-failure">
                        <p className="scan-failure-message">⚠️ {scanFailure.message}</p>
                        <div className="scan-failure-actions">
                            <button type="button" className="btn btn-secondary" onClick={resetScan}>
                                Retake Image
                            </button>
                            <button type="button" className="btn btn-primary" onClick={() => setShowManualEntry(true)}>
                                Enter Manually
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {showManualEntry && (
                <AddMedicine
                    onClose={() => setShowManualEntry(false)}
                    onSuccess={handleManualEntrySuccess}
                />
            )}
        </div>
    );
};

export default ScanPage;
