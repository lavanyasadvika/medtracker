/**
 * Triggers a browser download for a Blob returned by an axios responseType:'blob' request.
 * The backend's CORS config only exposes the Authorization header (not Content-Disposition),
 * so the filename is generated client-side rather than read from the response headers.
 */
export const downloadBlob = (blob, filename) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
};
