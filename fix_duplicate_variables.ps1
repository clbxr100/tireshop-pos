# PowerShell script to fix duplicate variable declarations in SalesController.java

$filePath = "src/main/java/com/tireshop/controller/SalesController.java"
$content = Get-Content $filePath

# Replace the specific lines that have duplicate variable declarations
$lineNum = 0
$newContent = @()

foreach ($line in $content) {
    $lineNum++
    
    if ($lineNum -eq 2206 -and $line -match "Customer customer = sale.getCustomer\(\);") {
        # Comment out the duplicate Customer declaration
        $newContent += "        // Customer customer = sale.getCustomer();  // Commented out - duplicate variable"
    }
    elseif ($lineNum -eq 2211 -and $line -match "Vehicle vehicle = sale.getVehicle\(\);") {
        # Comment out the duplicate Vehicle declaration
        $newContent += "        // Vehicle vehicle = sale.getVehicle();  // Commented out - duplicate variable"
    }
    elseif ($lineNum -eq 2237 -and $line -match "ScrollPane scrollPane = new ScrollPane\(mainContent\);") {
        # Comment out the duplicate ScrollPane declaration
        $newContent += "        // ScrollPane scrollPane = new ScrollPane(mainContent);  // Commented out - duplicate variable"
    }
    elseif ($lineNum -eq 2238 -and $line -match "scrollPane.setFitToWidth\(true\);") {
        # Comment out the scrollPane usage
        $newContent += "        // scrollPane.setFitToWidth(true);  // Commented out - using duplicate variable"
    }
    elseif ($lineNum -eq 2239 -and $line -match "dialog.getDialogPane\(\).setContent\(scrollPane\);") {
        # Replace with the already existing scrollPane variable
        $newContent += "        // dialog.getDialogPane().setContent(scrollPane);  // Commented out - already set above"
    }
    else {
        $newContent += $line
    }
}

# Write the modified content back to the file
$newContent | Set-Content $filePath

Write-Host "Fixed duplicate variable declarations in SalesController.java" 