"""
SmartHospital Full Smoke Test — headed Chrome.
Usage:  python -X utf8 smoke_test.py
"""

import time, json, sys
from datetime import date, timedelta
from playwright.sync_api import sync_playwright

BASE   = "http://localhost"
EMAIL  = "admin@hospital001.com"
PASSWD = "Admin@1234"
TENANT = "hospital_001"

ISSUES = []
PASSES = 0

def log(status, module, detail):
    global PASSES
    tag = {"PASS": "[PASS]", "FAIL": "[FAIL]", "ISSUE": "[WARN]", "INFO": "[INFO]"}.get(status, "     ")
    print(f"  {tag} [{module}] {detail}", flush=True)
    if status == "PASS":
        PASSES += 1
    elif status in ("FAIL", "ISSUE"):
        ISSUES.append(f"[{status}] [{module}] {detail}")

def section(title):
    print(f"\n{'='*65}", flush=True)
    print(f"  {title}", flush=True)
    print(f"{'='*65}", flush=True)

def api(page, method, path, body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    opts = json.dumps({"method": method, "headers": headers,
                       "body": json.dumps(body) if body else None})
    result = page.evaluate(f"""
    async () => {{
      const r = await fetch("{BASE}{path}", {opts});
      return {{ status: r.status, body: await r.text() }};
    }}""")
    try:
        result["json"] = json.loads(result["body"])
    except Exception:
        result["json"] = None
    return result

def data_count(r):
    """Return int count, or string label on non-standard shapes."""
    if not r["json"] or not r["json"].get("success"):
        return 0
    d = r["json"].get("data")
    if d is None:
        return 0
    if isinstance(d, list):
        return len(d)          # flat list — treat as count
    if isinstance(d, dict):
        v = d.get("total", d.get("totalElements"))
        return v if v is not None else -1
    return 0

def first_id(r):
    """Extract first item ID from paginated or list response."""
    if not r["json"] or not r["json"].get("success"):
        return None
    d = r["json"].get("data")
    if isinstance(d, list) and d:
        return d[0].get("id")
    if isinstance(d, dict):
        content = d.get("content", [])
        return content[0].get("id") if content else None
    return None

def go(page, path):
    page.goto(BASE + path)
    page.wait_for_load_state("networkidle", timeout=12000)
    time.sleep(0.8)

PAST_60 = (date.today() - timedelta(days=60)).isoformat()
PAST_30 = (date.today() - timedelta(days=30)).isoformat()

# ─────────────────────────────────────────────────────────────
with sync_playwright() as p:
    browser = p.chromium.launch(
        executable_path="C:/Program Files/Google/Chrome/Application/chrome.exe",
        headless=False, slow_mo=350,
        args=["--start-maximized"]
    )
    ctx  = browser.new_context(no_viewport=True)
    page = ctx.new_page()

    # ── 1. FRONTEND ───────────────────────────────────────────
    section("1. FRONTEND LOAD")
    go(page, "/")
    log("PASS" if page.title() else "FAIL", "Frontend", f"title='{page.title()}'")

    # ── 2. AUTH ───────────────────────────────────────────────
    section("2. AUTH — Login")
    go(page, "/login")
    time.sleep(0.5)

    # Form: tenantId (index 0), email (index 1), password (index 2)
    try:
        page.locator('#tenantId').fill(TENANT)
        page.locator('#email').fill(EMAIL)
        page.locator('#password').fill(PASSWD)
        page.locator('button[type="submit"]').click()
        page.wait_for_url(f"{BASE}/dashboard", timeout=12000)
        log("PASS", "Auth/Login", "UI login → /dashboard")
    except Exception as e:
        log("FAIL", "Auth/Login", f"UI login failed: {str(e)[:100]}")

    # Grab token from localStorage (Zustand persist)
    TOKEN = page.evaluate("""() => {
        for (const k of Object.keys(localStorage)) {
            try {
                const d = JSON.parse(localStorage.getItem(k));
                const t = d?.state?.accessToken || d?.accessToken;
                if (t && t.startsWith('ey')) return t;
            } catch {}
        }
        return null;
    }""")
    REFRESH_TOKEN = page.evaluate("""() => {
        for (const k of Object.keys(localStorage)) {
            try {
                const d = JSON.parse(localStorage.getItem(k));
                const t = d?.state?.refreshToken || d?.refreshToken;
                if (t && t.startsWith('ey')) return t;
            } catch {}
        }
        return null;
    }""")

    if not TOKEN:
        r = api(page, "POST", "/api/v1/auth/login",
                {"email": EMAIL, "password": PASSWD, "tenantId": TENANT})
        if r["json"] and r["json"].get("success"):
            TOKEN = r["json"]["data"]["tokens"]["accessToken"]
            REFRESH_TOKEN = r["json"]["data"]["tokens"]["refreshToken"]
            log("PASS", "Auth/Token", "JWT via API fallback")
        else:
            log("FAIL", "Auth/Token", "Cannot get JWT")
    else:
        log("PASS", "Auth/Token", "JWT found in localStorage")

    # /me
    r = api(page, "GET", "/api/v1/auth/me", token=TOKEN)
    if r["status"] == 200:
        d = r["json"]["data"]
        log("PASS", "Auth/Me", f"email={d.get('email')}, role={d.get('roles')}")
        exposed = [k for k in ("password","authorities","credentialsNonExpired",
                               "accountNonLocked","accountNonExpired","enabled") if k in d]
        if exposed:
            log("ISSUE", "Auth/Me",
                f"/me leaks Spring Security internals: {exposed} — UserPrincipal serialised directly")
    else:
        log("FAIL", "Auth/Me", f"status={r['status']}")

    # Token refresh
    if REFRESH_TOKEN:
        r = api(page, "POST", f"/api/v1/auth/refresh?refreshToken={REFRESH_TOKEN}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Auth/Refresh",
            f"POST /refresh → {r['status']}")
        if r["status"] == 200:
            # Update token to the rotated one
            TOKEN = r["json"]["data"]["accessToken"]
            REFRESH_TOKEN = r["json"]["data"]["refreshToken"]
    else:
        log("ISSUE", "Auth/Refresh", "No refresh token available")

    # Unauthorized probe
    r = api(page, "GET", "/api/v1/patients?size=1")
    log("PASS" if r["status"] == 401 else "ISSUE", "Auth/Unprotected",
        f"request without token → {r['status']} (expected 401)")

    r = api(page, "GET", "/api/v1/patients?size=1", token="bad.jwt.token")
    log("PASS" if r["status"] == 401 else "ISSUE", "Auth/BadToken",
        f"invalid JWT → {r['status']} (expected 401)")

    # ── 3. DASHBOARD ──────────────────────────────────────────
    section("3. DASHBOARD")
    # (already on /dashboard after login)
    time.sleep(1.5)

    r = api(page, "GET", "/api/v1/ipd/dashboard", token=TOKEN)
    log("PASS" if r["status"] == 200 else "ISSUE", "Dashboard/IPD",
        f"GET /ipd/dashboard → {r['status']}, data={list(r['json']['data'].keys()) if r['json'] and r['json'].get('data') else '?'}")

    r = api(page, "GET", "/api/v1/finance/summary", token=TOKEN)
    if r["status"] == 200:
        d = r["json"]["data"]
        income = d.get("totalIncome", 0)
        exp    = d.get("totalExpenses", 0)
        net    = d.get("netRevenue", 0)
        log("PASS", "Dashboard/FinanceSummary",
            f"income={income}, expenses={exp}, net={net}")
        if income == 0 and exp == 0:
            log("ISSUE", "Dashboard/FinanceSummary",
                "All zeroes despite 25 income + 20 expense records seeded — summary ignores seeded data or wrong date range")
    else:
        log("FAIL", "Dashboard/FinanceSummary", f"status={r['status']}")

    # ── 4. PATIENTS ───────────────────────────────────────────
    section("4. PATIENTS")
    go(page, "/patients")

    r = api(page, "GET", "/api/v1/patients?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Patients/List", f"total={data_count(r)}")
    PATIENT_ID = first_id(r)

    r_all  = api(page, "GET", "/api/v1/patients?size=200", token=TOKEN)
    r_srch = api(page, "GET", "/api/v1/patients?query=Kumar&size=200", token=TOKEN)  # API uses 'query', not 'search'
    all_n  = data_count(r_all)
    srch_n = data_count(r_srch)
    if isinstance(srch_n, int) and isinstance(all_n, int) and srch_n < all_n:
        log("PASS", "Patients/Search", f"query=Kumar filters: {srch_n} of {all_n}")
    else:
        log("ISSUE", "Patients/Search",
            f"query=Kumar returns {srch_n} of {all_n} — full-text search not filtering")

    if PATIENT_ID:
        r = api(page, "GET", f"/api/v1/patients/{PATIENT_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Patients/GetById",
            f"status={r['status']}")

    r = api(page, "GET", "/api/v1/patients/00000000-0000-0000-0000-000000000000", token=TOKEN)
    log("PASS" if r["status"] == 404 else "ISSUE", "Patients/404",
        f"non-existent → {r['status']} (expected 404)")

    r = api(page, "GET", "/api/v1/patients/not-a-uuid", token=TOKEN)
    log("PASS" if r["status"] in (400, 404) else "ISSUE", "Patients/BadUUID",
        f"malformed UUID → {r['status']}")

    # ── 5. FRONT OFFICE ───────────────────────────────────────
    section("5. FRONT OFFICE")
    go(page, "/frontoffice/appointments")

    r_nd = api(page, "GET", "/api/v1/frontoffice/appointments?size=5", token=TOKEN)
    r_d  = api(page, "GET", f"/api/v1/frontoffice/appointments?date={PAST_60}&size=5", token=TOKEN)
    r_up = api(page, "GET", "/api/v1/frontoffice/appointments/upcoming?size=10", token=TOKEN)

    log("PASS" if r_nd["status"] == 200 else "FAIL",
        "FrontOffice/Appointments/All", f"no-date total={data_count(r_nd)}")
    if data_count(r_nd) == 0:
        log("ISSUE", "FrontOffice/Appointments/All",
            "GET /appointments (no date) returns 0 — requires date param; no way to list all appointments")

    log("PASS" if r_d["status"] == 200 else "FAIL",
        "FrontOffice/Appointments/ByDate", f"date={PAST_60} total={data_count(r_d)}")
    log("PASS" if r_up["status"] == 200 else "FAIL",
        "FrontOffice/Upcoming", f"total={data_count(r_up)}")

    # Tokens
    r = api(page, "GET", "/api/v1/frontoffice/tokens", token=TOKEN)
    d = r["json"].get("data") if r["json"] else None
    if r["status"] == 200 and isinstance(d, list):
        log("ISSUE", "FrontOffice/Tokens",
            f"GET /tokens returns flat list ({len(d)} items) — inconsistent with all other paginated list endpoints")
    elif r["status"] == 200:
        log("PASS", "FrontOffice/Tokens", f"total={data_count(r)}")
    else:
        log("FAIL", "FrontOffice/Tokens", f"status={r['status']}")

    APPT_ID = first_id(r_up) if r_up["json"] else None
    if APPT_ID:
        r = api(page, "GET", f"/api/v1/frontoffice/appointments/{APPT_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "FrontOffice/GetById",
            f"GET /appointments/{{id}} → {r['status']}")

    # Create appointment
    r = api(page, "GET", "/api/v1/hr/employees?size=1", token=TOKEN)
    DOC_ID = first_id(r)
    if PATIENT_ID and DOC_ID:
        body = {"patientId": PATIENT_ID, "doctorId": DOC_ID,
                "appointmentDate": (date.today() + timedelta(days=5)).isoformat(),
                "timeSlot": "11:00", "appointmentType": "CONSULTATION"}
        r = api(page, "POST", "/api/v1/frontoffice/appointments", body, token=TOKEN)
        log("PASS" if r["status"] in (200, 201) else "FAIL",
            "FrontOffice/Create", f"POST /appointments → {r['status']}")
        if r["status"] not in (200, 201):
            log("INFO", "FrontOffice/Create", f"error: {str(r['body'])[:200]}")

    # ── 6. OPD ────────────────────────────────────────────────
    section("6. OPD — Visits")
    go(page, "/opd")

    r_nd = api(page, "GET", "/api/v1/opd/visits?size=5", token=TOKEN)
    r_d  = api(page, "GET", f"/api/v1/opd/visits?date={PAST_60}&size=5", token=TOKEN)
    log("PASS" if r_nd["status"] == 200 else "FAIL",
        "OPD/Visits/NoDate", f"no-date total={data_count(r_nd)} (today, expected 0)")
    log("PASS" if r_d["status"] == 200 else "FAIL",
        "OPD/Visits/WithDate", f"date={PAST_60} total={data_count(r_d)}")
    if data_count(r_d) == 0:
        log("ISSUE", "OPD/Visits",
            f"0 visits on {PAST_60} — date-specific endpoint; no range or all-visits endpoint available")

    OPD_ID = first_id(r_d)
    if not OPD_ID and PATIENT_ID:
        r = api(page, "GET", f"/api/v1/opd/visits/patient/{PATIENT_ID}?size=1", token=TOKEN)
        OPD_ID = first_id(r)

    if OPD_ID:
        r = api(page, "GET", f"/api/v1/opd/visits/{OPD_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "OPD/GetById",
            f"status={r['status']}")
        r = api(page, "GET", f"/api/v1/opd/visits/{OPD_ID}/prescription", token=TOKEN)
        log("PASS" if r["status"] in (200, 404) else "ISSUE", "OPD/Prescription",
            f"→ {r['status']} {'(no prescription yet — ok)' if r['status']==404 else ''}")
        r = api(page, "GET", f"/api/v1/opd/visits/{OPD_ID}/bill", token=TOKEN)
        log("PASS" if r["status"] in (200, 404) else "ISSUE", "OPD/Bill",
            f"→ {r['status']}")
    else:
        log("ISSUE", "OPD/GetById", "Could not find any OPD visit ID to test")

    # ── 7. IPD ────────────────────────────────────────────────
    section("7. IPD — Admissions, Wards, Beds")
    go(page, "/ipd")

    r = api(page, "GET", "/api/v1/ipd/admissions?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "IPD/Admissions", f"total={data_count(r)}")
    IPD_ID = first_id(r)

    if IPD_ID:
        r2 = api(page, "GET", f"/api/v1/ipd/admissions/{IPD_ID}", token=TOKEN)
        log("PASS" if r2["status"] == 200 else "FAIL", "IPD/GetById",
            f"status={r2['json']['data'].get('status') if r2['json'] else '?'}")

    for s in ["ADMITTED", "DISCHARGED"]:
        r = api(page, "GET", f"/api/v1/ipd/admissions?status={s}&size=5", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", f"IPD/Filter/{s}",
            f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/ipd/wards?size=10", token=TOKEN)
    ward_data = r["json"].get("data") if r["json"] else None
    if r["status"] == 200 and isinstance(ward_data, list):
        log("ISSUE", "IPD/Wards",
            f"GET /wards returns flat list({len(ward_data)}) — not paginated; inconsistent with other list endpoints")
        WARD_ID = ward_data[0]["id"] if ward_data else None
    elif r["status"] == 200:
        log("PASS", "IPD/Wards", f"total={data_count(r)}")
        WARD_ID = first_id(r)
    else:
        log("FAIL", "IPD/Wards", f"status={r['status']}")
        WARD_ID = None

    if WARD_ID:
        r = api(page, "GET", f"/api/v1/ipd/wards/{WARD_ID}/beds?size=20", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "IPD/Beds",
            f"total={data_count(r)}")
        r = api(page, "GET", f"/api/v1/ipd/wards/{WARD_ID}/beds/available", token=TOKEN)
        beds_data = r["json"].get("data") if r["json"] else None
        if r["status"] == 200 and isinstance(beds_data, list):
            log("ISSUE", "IPD/BedsAvailable",
                f"GET /beds/available returns flat list({len(beds_data)}) — inconsistent")
        else:
            log("PASS" if r["status"] == 200 else "FAIL", "IPD/BedsAvailable",
                f"status={r['status']}, total={data_count(r)}")

    if IPD_ID:
        r = api(page, "GET", f"/api/v1/ipd/admissions/{IPD_ID}", token=TOKEN)
        if r["json"] and r["json"]["data"].get("status") == "ADMITTED":
            r2 = api(page, "POST", f"/api/v1/ipd/admissions/{IPD_ID}/charges",
                     {"category":"BED_CHARGE","description":"Room charge","amount":600}, token=TOKEN)
            log("PASS" if r2["status"] in (200,201) else "ISSUE",
                "IPD/AddCharge", f"POST /admissions/{{id}}/charges → {r2['status']}")

    # ── 8. PHARMACY ───────────────────────────────────────────
    section("8. PHARMACY")
    go(page, "/pharmacy")

    r = api(page, "GET", "/api/v1/pharmacy/categories?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Pharmacy/Categories", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/pharmacy/medicines?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Pharmacy/Medicines", f"total={data_count(r)}")
    MED_ID = first_id(r)

    if MED_ID:
        r = api(page, "GET", f"/api/v1/pharmacy/medicines/{MED_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Pharmacy/MedById",
            f"status={r['status']}")
        r = api(page, "GET", f"/api/v1/pharmacy/stock/{MED_ID}", token=TOKEN)
        s_data = r["json"].get("data") if r["json"] else None
        if r["status"] == 200 and isinstance(s_data, list):
            log("ISSUE", "Pharmacy/Stock",
                f"GET /stock/{{id}} returns flat list({len(s_data)}) — inconsistent")
        else:
            log("PASS" if r["status"] == 200 else "FAIL", "Pharmacy/Stock",
                f"status={r['status']}")

    r = api(page, "GET", "/api/v1/pharmacy/medicines/low-stock", token=TOKEN)
    ls_data = r["json"].get("data") if r["json"] else None
    if r["status"] == 200 and isinstance(ls_data, list):
        log("ISSUE", "Pharmacy/LowStock",
            f"GET /medicines/low-stock returns flat list({len(ls_data)}) — inconsistent")
    else:
        log("PASS" if r["status"] == 200 else "FAIL", "Pharmacy/LowStock",
            f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/pharmacy/stock/expiring?days=90", token=TOKEN)
    log("PASS" if r["status"] == 200 else "ISSUE", "Pharmacy/Expiring",
        f"status={r['status']}, total={data_count(r)}")

    r = api(page, "GET", "/api/v1/pharmacy/bills?size=3", token=TOKEN)
    log("ISSUE" if r["status"] == 405 else ("PASS" if r["status"] == 200 else "INFO"),
        "Pharmacy/BillsListAll",
        f"GET /bills (no filter) → {r['status']} — no list-all bills endpoint")

    if PATIENT_ID:
        r = api(page, "GET", f"/api/v1/pharmacy/bills/patient/{PATIENT_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Pharmacy/BillsByPatient",
            f"total={data_count(r)}")

    # ── 9. PATHOLOGY ──────────────────────────────────────────
    section("9. PATHOLOGY / LAB")
    go(page, "/pathology")

    r = api(page, "GET", "/api/v1/pathology/tests?size=20", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Pathology/TestCatalog", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/pathology/orders?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Pathology/Orders", f"total={data_count(r)}")
    LAB_ID = first_id(r)

    if LAB_ID:
        r = api(page, "GET", f"/api/v1/pathology/orders/{LAB_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Pathology/OrderById",
            f"status={r['status']}")

    if PATIENT_ID:
        r = api(page, "GET", f"/api/v1/pathology/orders/patient/{PATIENT_ID}?size=3", token=TOKEN)
        log("PASS" if r["status"] == 200 else "ISSUE", "Pathology/ByPatient",
            f"total={data_count(r)}")

    # ── 10. RADIOLOGY ─────────────────────────────────────────
    section("10. RADIOLOGY")
    go(page, "/radiology")

    r = api(page, "GET", "/api/v1/radiology/modalities?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Radiology/Modalities", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/radiology/studies?size=20", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Radiology/Studies", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/radiology/orders?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Radiology/Orders", f"total={data_count(r)}")
    RAD_ID = first_id(r)

    if RAD_ID:
        r = api(page, "GET", f"/api/v1/radiology/orders/{RAD_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Radiology/OrderById",
            f"status={r['status']}")

    if PATIENT_ID:
        r = api(page, "GET", f"/api/v1/radiology/orders/patient/{PATIENT_ID}?size=3", token=TOKEN)
        log("PASS" if r["status"] == 200 else "ISSUE", "Radiology/ByPatient",
            f"total={data_count(r)}")

    # ── 11. BLOOD BANK ────────────────────────────────────────
    section("11. BLOOD BANK")
    go(page, "/bloodbank")

    r = api(page, "GET", "/api/v1/bloodbank/donors?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "BloodBank/Donors", f"total={data_count(r)}")
    DONOR_ID = first_id(r)

    if DONOR_ID:
        r = api(page, "GET", f"/api/v1/bloodbank/donors/{DONOR_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "BloodBank/DonorById",
            f"status={r['status']}")

    r = api(page, "GET", "/api/v1/bloodbank/units?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "BloodBank/Units", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/bloodbank/units/available?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 else "ISSUE", "BloodBank/AvailableUnits",
        f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/bloodbank/requests?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "BloodBank/Requests", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/bloodbank/issues?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 else "FAIL", "BloodBank/Issues",
        f"total={data_count(r)}")

    # /bloodbank/tokens does not exist — OPD tokens live under /frontoffice/tokens.
    # A 404 here is correct; 500 was the bug (now fixed via GlobalExceptionHandler).
    r = api(page, "GET", "/api/v1/bloodbank/tokens?size=5", token=TOKEN)
    log("PASS" if r["status"] == 404 else "ISSUE", "BloodBank/Tokens",
        f"non-existent route → {r['status']} (expected 404, tokens are under /frontoffice)")

    # ── 12. HR ────────────────────────────────────────────────
    section("12. HUMAN RESOURCES")
    go(page, "/hr/employees")

    r = api(page, "GET", "/api/v1/hr/employees?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "HR/Employees", f"total={data_count(r)}")
    EMP_ID = first_id(r)

    if EMP_ID:
        r = api(page, "GET", f"/api/v1/hr/employees/{EMP_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "HR/EmployeeById",
            f"status={r['status']}")
        r = api(page, "GET", f"/api/v1/hr/employees/{EMP_ID}/attendance?size=5", token=TOKEN)
        log("PASS" if r["status"] == 200 else "ISSUE", "HR/EmployeeAttendance",
            f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/hr/departments?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "HR/Departments", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/hr/designations?size=20", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "HR/Designations", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/hr/leave?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 else "FAIL", "HR/Leave",
        f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/hr/attendance?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 else "ISSUE", "HR/Attendance",
        f"status={r['status']}, total={data_count(r)}")

    # ── 13. INVENTORY ─────────────────────────────────────────
    section("13. INVENTORY")
    go(page, "/inventory")

    r = api(page, "GET", "/api/v1/inventory/categories?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Inventory/Categories", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/inventory/items?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Inventory/Items", f"total={data_count(r)}")
    INV_ID = first_id(r)

    if INV_ID:
        r = api(page, "GET", f"/api/v1/inventory/items/{INV_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Inventory/ItemById",
            f"status={r['status']}")

    r = api(page, "GET", "/api/v1/inventory/receipts?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Inventory/Receipts", f"total={data_count(r)}")

    if INV_ID:
        r = api(page, "GET", f"/api/v1/inventory/receipts/{first_id(api(page,'GET','/api/v1/inventory/receipts?size=1',token=TOKEN))}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Inventory/ReceiptById",
            f"status={r['status']}")

    r = api(page, "GET", "/api/v1/inventory/issues?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 else "FAIL", "Inventory/Issues",
        f"total={data_count(r)}")

    # ── 14. FINANCE ───────────────────────────────────────────
    section("14. FINANCE")
    go(page, "/finance")

    r = api(page, "GET", "/api/v1/finance/income?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Finance/Income", f"total={data_count(r)}")
    INC_ID = first_id(r)

    if INC_ID:
        r = api(page, "GET", f"/api/v1/finance/income/{INC_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Finance/IncomeById",
            f"status={r['status']}")

    r = api(page, "GET", "/api/v1/finance/expenses?size=5", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Finance/Expenses", f"total={data_count(r)}")
    EXP_ID = first_id(r)

    if EXP_ID:
        r = api(page, "GET", f"/api/v1/finance/expenses/{EXP_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "Finance/ExpenseById",
            f"status={r['status']}")

    r = api(page, "GET", "/api/v1/finance/expense-categories?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "Finance/ExpenseCategories", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/finance/summary", token=TOKEN)
    log("PASS" if r["status"] == 200 else "FAIL", "Finance/Summary",
        f"net={r['json']['data'].get('netRevenue') if r['json'] else '?'}")

    r = api(page, "GET", f"/api/v1/finance/summary?from={PAST_60}&to={PAST_30}", token=TOKEN)
    if r["status"] == 200:
        d = r["json"]["data"]
        log("PASS", "Finance/SummaryDateRange",
            f"income={d.get('totalIncome')}, expenses={d.get('totalExpenses')}")
        if d.get("totalIncome", 0) == 0 and d.get("totalExpenses", 0) == 0:
            log("ISSUE", "Finance/SummaryDateRange",
                "Income and expenses both 0 in 30-day window despite seeded data — summary may use wrong date field")
    else:
        log("FAIL", "Finance/SummaryDateRange", f"status={r['status']}")

    # ── 15. OPERATION THEATRE ─────────────────────────────────
    section("15. OPERATION THEATRE")
    go(page, "/operation")

    r = api(page, "GET", "/api/v1/operation/theatres?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "OT/Theatres", f"total={data_count(r)}")

    r = api(page, "GET", "/api/v1/operation/schedules?size=10", token=TOKEN)
    log("PASS" if r["status"] == 200 and data_count(r) > 0 else "FAIL",
        "OT/Schedules", f"total={data_count(r)}")
    OT_ID = first_id(r)

    if OT_ID:
        r = api(page, "GET", f"/api/v1/operation/schedules/{OT_ID}", token=TOKEN)
        log("PASS" if r["status"] == 200 else "FAIL", "OT/ScheduleById",
            f"status={r['status']}")

    for s in ["SCHEDULED", "COMPLETED", "CANCELLED"]:
        r = api(page, "GET", f"/api/v1/operation/schedules?status={s}&size=3", token=TOKEN)
        log("PASS" if r["status"] == 200 else "ISSUE", f"OT/Filter/{s}",
            f"total={data_count(r)}")

    # ── 16. SPA NAVIGATION ────────────────────────────────────
    section("16. SPA — Page navigation (auth guard + render)")
    routes = [
        ("/dashboard",                  "Dashboard"),
        ("/patients",                   "Patients"),
        ("/frontoffice/appointments",   "Front Office"),
        ("/opd",                        "OPD"),
        ("/ipd",                        "IPD"),
        ("/pharmacy",                   "Pharmacy"),
        ("/pathology",                  "Pathology"),
        ("/radiology",                  "Radiology"),
        ("/bloodbank",                  "Blood Bank"),
        ("/hr/employees",               "HR Employees"),
        ("/inventory",                  "Inventory"),
        ("/finance",                    "Finance"),
        ("/operation",                  "Operation Theatre"),
    ]
    for route, label in routes:
        page.goto(BASE + route)
        page.wait_for_load_state("networkidle", timeout=10000)
        time.sleep(0.4)
        final_url = page.url
        if "/login" in final_url:
            log("FAIL", f"Nav/{label}", "Bounced to /login — auth guard rejected")
        else:
            # Look for crash / unhandled error boundary text
            body_text = page.locator("body").inner_text()
            if any(x in body_text for x in ("Something went wrong", "Unhandled error",
                                             "Cannot read properties", "is not a function")):
                log("FAIL", f"Nav/{label}", "Error boundary or JS crash on page")
            else:
                log("PASS", f"Nav/{label}", f"Loaded → {final_url}")

    # ── FINAL SUMMARY ─────────────────────────────────────────
    section(f"SMOKE TEST COMPLETE  —  {PASSES} passed, {len(ISSUES)} issues")
    if ISSUES:
        print("\n  All issues:", flush=True)
        for i, issue in enumerate(ISSUES, 1):
            print(f"  {i:2}. {issue}", flush=True)
    else:
        print("  No issues — all checks passed!", flush=True)

    time.sleep(5)
    browser.close()
    sys.exit(1 if ISSUES else 0)
