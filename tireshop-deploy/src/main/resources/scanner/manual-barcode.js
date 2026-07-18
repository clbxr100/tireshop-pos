function submitManualBarcode() {
    clearError();
    const barcodeInput = document.getElementById('manual-barcode');
    const barcode = barcodeInput.value.trim();
    
    if (!barcode) {
        showError('Please enter a barcode');
        return;
    }
    
    // Clean up barcode - remove spaces and non-digit characters
    const cleanBarcode = barcode.replace(/[^0-9]/g, '');
    
    if (cleanBarcode.length < 8) {
        showError('Barcode must have at least 8 digits');
        return;
    }
    
    scannedBarcode = cleanBarcode;
    document.getElementById('barcode-value').textContent = 'Barcode: ' + scannedBarcode;
    document.getElementById('result').style.display = 'block';
    checkProductInfo(scannedBarcode);
} 