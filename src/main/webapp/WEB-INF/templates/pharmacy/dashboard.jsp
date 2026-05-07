<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Pharmacy Dashboard</title>
    <jsp:include page="../common/header.jsp" />
</head>
<body>
    <div class="container mt-4">
        <h2 class="mb-1">Pharmacy Dashboard</h2>
        <p class="text-muted">Manage inventory, prescriptions, and medical supplies.</p>

        <div class="alert alert-success" id="welcome-msg">Loading…</div>

        <div class="row mt-4 g-3">
            <div class="col-md-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">💊 Prescription Queue</h5>
                        <p class="card-text text-muted">Process pending prescriptions from doctors.</p>
                        <button class="btn btn-outline-success btn-sm" onclick="loadPendingQueue()">Load Queue</button>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">📦 Inventory</h5>
                        <p class="card-text text-muted">View medication stock levels.</p>
                        <button class="btn btn-outline-success btn-sm" onclick="loadInventory()">View Stock</button>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">🏭 Suppliers</h5>
                        <p class="card-text text-muted">Manage procurement and suppliers.</p>
                        <button class="btn btn-outline-success btn-sm" disabled>Coming soon</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- Prescription Queue table -->
        <div class="card mt-4 d-none" id="queue-card">
            <div class="card-header fw-semibold">Pending Prescriptions</div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-light">
                            <tr><th>Rx #</th><th>Patient</th><th>Medication</th><th>Dosage</th><th>Qty</th><th>Status</th></tr>
                        </thead>
                        <tbody id="queue-tbody">
                            <tr><td colspan="6" class="text-center py-3 text-muted">Loading…</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Inventory table -->
        <div class="card mt-4 d-none" id="inv-card">
            <div class="card-header fw-semibold">Medication Inventory</div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-light">
                            <tr><th>Code</th><th>Medication</th><th>Stock</th><th>Reorder At</th><th>Status</th></tr>
                        </thead>
                        <tbody id="inv-tbody">
                            <tr><td colspan="5" class="text-center py-3 text-muted">Loading…</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <jsp:include page="../common/footer.jsp" />
    <script>
    (function(){
        var user = JSON.parse(sessionStorage.getItem('hms_user') || '{}');
        var el = document.getElementById('welcome-msg');
        if (el) el.textContent = 'Welcome, ' + (user.fullName || user.email) + '! You have new prescription requests.';
    })();

    function loadPendingQueue() {
        document.getElementById('queue-card').classList.remove('d-none');
        var tbody = document.getElementById('queue-tbody');
        apiGet('/api/pharmacy/prescriptions/pending')
            .then(function(data) {
                if (!data || !data.length) { tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-muted">Queue is empty.</td></tr>'; return; }
                tbody.innerHTML = data.map(function(r) {
                    return '<tr><td>#' + r.id + '</td><td>' + (r.patientName||'—') + '</td><td class="fw-medium">' +
                        (r.medicationName||'—') + '</td><td>' + (r.dosage||'—') + '</td><td>' +
                        (r.quantity||'—') + '</td><td><span class="badge bg-warning text-dark">' + r.status + '</span></td></tr>';
                }).join('');
            })
            .catch(function(e) { tbody.innerHTML = '<tr><td colspan="6" class="text-danger text-center py-3">' + e.message + '</td></tr>'; });
    }

    function loadInventory() {
        document.getElementById('inv-card').classList.remove('d-none');
        var tbody = document.getElementById('inv-tbody');
        apiGet('/api/pharmacy/inventory')
            .then(function(data) {
                if (!data || !data.length) { tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">No inventory data.</td></tr>'; return; }
                tbody.innerHTML = data.map(function(m) {
                    var low = m.stockLevel <= m.reorderThreshold;
                    return '<tr><td class="small">' + (m.medicationCode||'—') + '</td><td class="fw-medium">' +
                        (m.medicationName||'—') + '</td><td class="' + (low?'text-danger fw-bold':'') + '">' +
                        (m.stockLevel??'—') + '</td><td>' + (m.reorderThreshold??'—') + '</td><td>' +
                        '<span class="badge bg-' + (low?'danger':'success') + '">' + (low?'LOW':'OK') + '</span></td></tr>';
                }).join('');
            })
            .catch(function(e) { tbody.innerHTML = '<tr><td colspan="5" class="text-danger text-center py-3">' + e.message + '</td></tr>'; });
    }
    </script>
</body>
</html>
