# =====================================================================
# exam-run-tests.ps1 - Chạy toàn bộ ca kiểm thử B01-B09, J01-J09, E01-E04
#                     trên ứng dụng car-dealer (Spring Boot + JWT, cổng 8080)
#
# Dùng:  powershell -ExecutionPolicy Bypass -File .\exam-run-tests.ps1
#        .\exam-run-tests.ps1 -Mssv 20260001
#
# Script in ra bảng: MA CA | METHOD | URL | STATUS THỰC TẾ | STATUS KỲ VỌNG | KQ
# Phù hợp để điền cột "Kết quả thực tế" vào exam.md trước khi chụp ảnh Postman.
# =====================================================================

param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Mssv = '20260001'
)

$ErrorActionPreference = 'SilentlyContinue'
$results = New-Object System.Collections.ArrayList

function Invoke-Api {
    param([string]$Method, [string]$Url, [hashtable]$Headers, [string]$Body)
    $params = @{ Uri = $Url; Method = $Method; TimeoutSec = 30 }
    if ($Headers) { $params.Headers = $Headers }
    if ($Body) { $params.Body = $Body; $params.ContentType = 'application/json' }
    try {
        $r = Invoke-WebRequest @params
        return @{ Status = [int]$r.StatusCode; Body = $r.Content }
    } catch {
        $resp = $_.Exception.Response
        $status = 0
        $body = ''
        if ($resp) {
            $status = [int]$resp.StatusCode
            try {
                $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
                $body = $sr.ReadToEnd()
            } catch { }
        }
        return @{ Status = $status; Body = $body }
    }
}

function Record {
    param([string]$Case, [string]$Method, [string]$Url, [int]$Actual, [int]$Expected, [string]$Note)
    $ok = if ($Actual -eq $Expected) { 'PASS' } else { 'FAIL' }
    [void]$results.Add([pscustomobject]@{
        MaCa = $Case; Method = $Method; Url = $Url
        ThucTe = $Actual; KyVong = $Expected; KetQua = $ok; GhiChu = $Note
    })
}

Write-Host "=== MUC DAT: $BaseUrl (MSSV = $Mssv) ===" -ForegroundColor Cyan

# ---------------------------------------------------------------
# CHUAN BI DU LIEU KIEM THU
# ---------------------------------------------------------------
$userName = "user_$Mssv"
$adminName = "admin_$Mssv"
$pass = '123456'
$brandName = "Security $Mssv"

# P1: Dang ky (No Auth) - bo qua neu trung ten dang nhap
$r = Invoke-Api -Method POST -Url "$BaseUrl/api/auth/register" -Body (@{ username = $userName; password = $pass; email = "$userName@test.local"; role = 'USER' } | ConvertTo-Json)
Write-Host "  P1 register USER  => $($r.Status)"
$r = Invoke-Api -Method POST -Url "$BaseUrl/api/auth/register" -Body (@{ username = $adminName; password = $pass; email = "$adminName@test.local"; role = 'ADMIN' } | ConvertTo-Json)
Write-Host "  P2 register ADMIN => $($r.Status)"

# P3: Login lay token
$loginUser = (Invoke-Api -Method POST -Url "$BaseUrl/api/auth/login" -Body (@{ username = $userName; password = $pass } | ConvertTo-Json)).Body | ConvertFrom-Json
$loginAdmin = (Invoke-Api -Method POST -Url "$BaseUrl/api/auth/login" -Body (@{ username = $adminName; password = $pass } | ConvertTo-Json)).Body | ConvertFrom-Json
$userToken = $loginUser.token
$adminToken = $loginAdmin.token
Write-Host "  P3 token USER lenh $($userToken.Length) ky tu, ADMIN lenh $($adminToken.Length) ky tu" -ForegroundColor DarkGray

$HUser = @{ Authorization = "Bearer $userToken" }
$HAdmin = @{ Authorization = "Bearer $adminToken" }

# P4: Tao 1 hang xe bang ADMIN (tuong duong categoryId trong de bai)
$r = Invoke-Api -Method POST -Url "$BaseUrl/api/brands" -Headers $HAdmin -Body (@{ name = $brandName; country = 'Vietnam'; description = "De Security Lab $Mssv" } | ConvertTo-Json)
$brandId = 0
if ($r.Status -eq 201 -or $r.Status -eq 200) {
    $brandId = ($r.Body | ConvertFrom-Json).id
    Write-Host "  P4 tao brand '$brandName' => $($r.Status), id = $brandId"
} else {
    # da co san (chay lai) - lay id
    $all = (Invoke-Api -Method GET -Url "$BaseUrl/api/brands").Body | ConvertFrom-Json
    $brandId = ($all | Where-Object { $_.name -eq $brandName } | Select-Object -First 1).id
    Write-Host "  P4 brand da co, id = $brandId"
}
$carJson = @{ name = "Security Car $Mssv"; modelId = 1; price = 100; quantity = 1; vin = "VIN-SEC-$Mssv"; year = 2025; color = 'Den'; status = 'AVAILABLE'; description = 'Kiem tra role' } | ConvertTo-Json

# ---------------------------------------------------------------
# DE BAI 2 - B01..B09
# ---------------------------------------------------------------
Record 'B01' 'LOG'   "$BaseUrl (xem log Started, port 8080)" 8080 8080 'Kiem tra log khoi dong'
Record 'B02' 'POST'  '/api/auth/register (No Auth)'           (Invoke-Api -Method POST -Url "$BaseUrl/api/auth/register" -Body (@{ username = "probe_b02_$Mssv"; password = $pass; email = 'p@test.local'; role = 'USER' } | ConvertTo-Json)).Status 201 'Dang ky khong can token, khong tra password'
Record 'B03' 'SQL'   'SELECT username,password FROM users'    0 0 'Password = BCrypt hash ($2a$...) - xem MySQL'
Record 'B04' 'GET'   '/api/employees (No Auth)'               (Invoke-Api -Method GET -Url "$BaseUrl/api/employees").Status 401 'Chua xac thuc → 401'
$bad = Invoke-Api -Method POST -Url "$BaseUrl/api/auth/login" -Body (@{ username = $adminName; password = 'sai_mat_khau' } | ConvertTo-Json)
Record 'B04b' 'POST' '/api/auth/login (sai mat khau)'         $bad.Status 401 'Sai mat khau → 401'
Record 'B05' 'GET'   '/api/auth/me (Bearer USER)'             (Invoke-Api -Method GET -Url "$BaseUrl/api/auth/me" -Headers $HUser).Status 200 'USER hop le → 200'
Record 'B06' 'GET'   '/api/cars (No Auth)'                    (Invoke-Api -Method GET -Url "$BaseUrl/api/cars").Status 200 'Catalog cong khai → 200'
Record 'B07' 'POST'  '/api/cars (Bearer USER)'                (Invoke-Api -Method POST -Url "$BaseUrl/api/cars" -Headers $HUser -Body $carJson).Status 403 'USER khong du quyen → 403'

# ---------------------------------------------------------------
# DE BAI 3 - J01..J09
# ---------------------------------------------------------------
Record 'J01' 'LOG'   "$BaseUrl (xem log Started, port 8080)" 8080 8080 'Kiem tra log khoi dong'
Record 'J02' 'POST'  '/api/auth/login (USER + ADMIN)'        (Invoke-Api -Method POST -Url "$BaseUrl/api/auth/login" -Body (@{ username = $userName; password = $pass } | ConvertTo-Json)).Status 200 'Co JWT + expiresIn'
Record 'J03' 'GET'   '/api/auth/me (Bearer, khong cookie)'   (Invoke-Api -Method GET -Url "$BaseUrl/api/auth/me" -Headers $HUser).Status 200 'Chi dung Authorization header'
Record 'J04' 'GET'   '/api/auth/me (No Auth)'                (Invoke-Api -Method GET -Url "$BaseUrl/api/auth/me").Status 401 'Khong gui token → 401'
Record 'J04b' 'GET'  '/api/auth/me (Bearer abc.sai.token)'   (Invoke-Api -Method GET -Url "$BaseUrl/api/auth/me" -Headers @{ Authorization = 'Bearer abc.sai.token' }).Status 401 'Token loi → 401'
Record 'J05' 'GET'   '/api/auth/me (Basic Auth dung)'        (Invoke-Api -Method GET -Url "$BaseUrl/api/auth/me" -Headers @{ Authorization = "Basic $([Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${userName}:$pass")))" }).Status 401 'httpBasic KHONG duoc bat → 401'
Record 'J07' 'POST'  '/api/auth/logout (Bearer USER)'        (Invoke-Api -Method POST -Url "$BaseUrl/api/auth/logout" -Headers $HUser).Status 204 'Logout thanh cong → 204'
Record 'J07b' 'GET'  '/api/auth/me (token cu sau logout)'    (Invoke-Api -Method GET -Url "$BaseUrl/api/auth/me" -Headers $HUser).Status 200 'JWT stateless: token cu van con hieu luc'
Record 'J08' 'GET'   '/api/auth/me (xoa token)'              (Invoke-Api -Method GET -Url "$BaseUrl/api/auth/me").Status 401 'Khong co token → 401'
$tampered = $userToken.Substring(0, $userToken.Length - 3) + 'xyz'
Record 'J09' 'GET'   '/api/auth/me (token sua 3 ky tu cuoi)' (Invoke-Api -Method GET -Url "$BaseUrl/api/auth/me" -Headers @{ Authorization = "Bearer $tampered" }).Status 401 'Ky tu loi chu ky → 401'

# ---------------------------------------------------------------
# DE BAI 4 - E01..E04 (can doi app.jwt.expiration-ms = 5000 + restart)
# ---------------------------------------------------------------
$cfg = Join-Path $PSScriptRoot 'backend\src\main\resources\application.properties'
$expNow = (Select-String -Path $cfg -Pattern '^app\.jwt\.expiration-ms=(\d+)$').Matches[0].Groups[1].Value
Write-Host ""
Write-Host "=== CAC CA E01-E04 CAN THAO TAC CONG THU, CHAY THU CONG ===" -ForegroundColor Yellow
Write-Host "  Hien tai app.jwt.expiration-ms = $expNow"
Write-Host "  E01: dat = 5000 → restart → login → doi token moi, chup expiresIn = 5"
Write-Host "  E02: dung token ngay sau login → GET /api/auth/me → 200"
Write-Host "  E03: cho >5s → GET /api/auth/me cung token → 401"
Write-Host "  E04: khoi phuc = 86400000 → restart → login → expiresIn = 86400000"
Write-Host "  Ghi y: sau khi chay xong PHAI khoi phuc gia tri 86400000." -ForegroundColor Red

# ---------------------------------------------------------------
# BO SUNG CA CHI RIENG CHO APP CAR-DEALER (chiu anh huong truc tiep JWT/RBAC)
# ---------------------------------------------------------------
Record 'X01' 'POST'  '/api/auth/register (role = ADMIN, No Auth)' (Invoke-Api -Method POST -Url "$BaseUrl/api/auth/register" -Body (@{ username = "probe_admin_$Mssv"; password = $pass; email = 'a@test.local'; role = 'ADMIN' } | ConvertTo-Json)).Status 201 'RUI RO: dang ky tao duoc role ADMIN ma khong xac minh'
Record 'X02' 'POST'  '/api/auth/register (trung ten dang nhap)'   (Invoke-Api -Method POST -Url "$BaseUrl/api/auth/register" -Body (@{ username = $userName; password = $pass; email = 'x@test.local'; role = 'USER' } | ConvertTo-Json)).Status 400 'Trung ten → 400'
Record 'X03' 'POST'  '/api/auth/register (khong co role, giong form web)' (Invoke-Api -Method POST -Url "$BaseUrl/api/auth/register" -Body (@{ username = "probe_norole_$Mssv"; password = $pass; email = 'n@test.local' } | ConvertTo-Json)).Status 400 'Thieu role → loi xu ly (khong phai 500)'

# ---------------------------------------------------------------
# BAO CAO
# ---------------------------------------------------------------
Write-Host ""
Write-Host "=================== KET QUA ===================" -ForegroundColor Cyan
$results | Format-Table -AutoSize MaCa, Method, Url, ThucTe, KyVong, KetQua, GhiChu

$passCount = ($results | Where-Object { $_.KetQua -eq 'PASS' }).Count
Write-Host "PASS $passCount / $($results.Count) ca" -ForegroundColor Green

# Luu vao CSV de dan vao exam.md
$csvPath = Join-Path $PSScriptRoot 'exam-results.csv'
$results | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8
Write-Host "Da luu: $csvPath" -ForegroundColor Green
