# Handling Chinese Paths in Web Applications

Guide for handling Chinese filenames and paths in HTTP requests, file uploads, and downloads.

## URL Encoding

### URL Encoding Rules

Chinese characters in URLs must be percent-encoded:
- `文档` → `%E6%96%87%E6%A1%A3`
- `测试.txt` → `%E6%B5%8B%E8%AF%95.txt`

### Python (Flask/Django)

```python
from urllib.parse import quote, unquote

# Encode for URL
filename = "中文报告.txt"
encoded = quote(filename)  # %E4%B8%AD%E6%96%87%E6%8A%A5%E5%91%8A.txt

# Decode from URL
decoded = unquote(encoded)  # 中文报告.txt

# Flask download with proper headers
from flask import send_file, request

@app.route('/download/<path:filename>')
def download_file(filename):
    # Decode URL-encoded filename
    decoded_filename = unquote(filename)
    
    # Encode for Content-Disposition (RFC 5987)
    encoded_filename = quote(decoded_filename)
    
    response = send_file(decoded_filename, as_attachment=True)
    response.headers['Content-Disposition'] = (
        f"attachment; filename*=UTF-8''{encoded_filename}"
    )
    return response
```

### Java (Spring Boot)

```java
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class FileController {
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        // Decode URL-encoded filename
        String originalFilename = file.getOriginalFilename();
        // MultipartFile usually handles encoding automatically
        
        Path target = Paths.get("uploads", originalFilename);
        Files.copy(file.getInputStream(), target);
        
        return ResponseEntity.ok().body("Uploaded: " + originalFilename);
    }
    
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        // Decode URL-encoded path variable
        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        
        Path filePath = Paths.get("uploads", decodedFilename);
        Resource resource = new UrlResource(filePath.toUri());
        
        // Encode for Content-Disposition header
        String encodedFilename = URLEncoder.encode(decodedFilename, StandardCharsets.UTF_8)
            .replace("+", "%20");
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename*=UTF-8''" + encodedFilename)
            .body(resource);
    }
}
```

### Node.js (Express)

```javascript
const express = require('express');
const path = require('path');

const app = express();

// Download with Chinese filename
app.get('/download/:filename', (req, res) => {
    // URL is automatically decoded by Express
    const filename = req.params.filename;
    const filePath = path.join(__dirname, 'uploads', filename);
    
    // Encode for Content-Disposition
    const encodedFilename = encodeURIComponent(filename);
    
    res.setHeader('Content-Disposition', 
        `attachment; filename*=UTF-8''${encodedFilename}`);
    res.sendFile(filePath);
});

// File upload
const multer = require('multer');
const upload = multer({ dest: 'uploads/' });

app.post('/upload', upload.single('file'), (req, res) => {
    // Multer preserves original filename encoding
    console.log('Original name:', req.file.originalname);
    console.log('Saved as:', req.file.filename);
    res.json({ success: true });
});
```

## Content-Disposition Header

### RFC 5987 Format

For non-ASCII filenames in HTTP headers, use the `filename*` parameter:

```
Content-Disposition: attachment; filename*=UTF-8''%E4%B8%AD%E6%96%87.txt
```

### Browser Compatibility

| Method | Chrome | Firefox | Safari | Edge | IE11 |
|--------|--------|---------|--------|------|------|
| `filename="..."` | ✓ ASCII only | ✓ ASCII only | ✓ ASCII only | ✓ ASCII only | ✓ ASCII only |
| `filename*=UTF-8''...` | ✓ | ✓ | ✓ | ✓ | ✗ |
| Both combined | ✓ | ✓ | ✓ | ✓ | ✓ (falls back) |

### Recommended Approach

```python
# Send both for maximum compatibility
def create_content_disposition(filename):
    # ASCII fallback
    ascii_filename = filename.encode('ascii', 'ignore').decode()
    
    # RFC 5987 encoding
    from urllib.parse import quote
    encoded = quote(filename, safe='')
    
    if ascii_filename == filename:
        return f'attachment; filename="{filename}"'
    else:
        return f'attachment; filename="{ascii_filename}"; filename*=UTF-8\'{encoded}\''
```

## Form Upload Encoding

### HTML Form

```html
<!-- Ensure form has proper encoding -->
<form action="/upload" method="post" enctype="multipart/form-data">
    <input type="file" name="file" accept=".txt,.pdf">
    <button type="submit">Upload</button>
</form>
```

### AJAX Upload (JavaScript)

```javascript
// Using FormData
const formData = new FormData();
const fileInput = document.getElementById('file');
const file = fileInput.files[0];

// Original filename is preserved
formData.append('file', file, file.name);

fetch('/upload', {
    method: 'POST',
    body: formData
});
```

## Testing Chinese Paths in Web Apps

### Test Cases

1. **Simple Chinese filename**
   - Upload: `文档.txt`
   - Download: Should preserve name

2. **Mixed characters**
   - Upload: `Report_2024_报告.pdf`
   - Verify encoding/decoding

3. **Special characters**
   - Upload: `文件(1) [备份].txt`
   - Check URL encoding

4. **Long filenames**
   - Upload: `这是一个非常长的中文文件名用于测试系统处理能力.txt`
   - Check truncation handling

### CURL Testing

```bash
# Upload with Chinese filename
curl -X POST -F "file=@文档.txt" http://localhost:8080/upload

# Download with URL encoding
curl -O -J http://localhost:8080/download/%E6%96%87%E6%A1%A3.txt
```
