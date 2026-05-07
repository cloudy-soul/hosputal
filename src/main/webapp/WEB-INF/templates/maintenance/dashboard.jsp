<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Maintenance Dashboard</title>
    <jsp:include page="../common/header.jsp" />
</head>
<body>
    <div class="container mt-4">
        <h2 class="mb-1">Maintenance Dashboard</h2>
        <p class="text-muted">Monitor facility maintenance tasks, work orders, and equipment.</p>

        <!-- KPI cards -->
        <div class="row g-3 mt-1" id="kpi-row">
            <div class="col-6 col-md-3"><div class="card text-center p-3 border-top border-4 border-primary"><div class="small text-muted text-uppercase fw-semibold">Open Orders</div><div class="fs-3 fw-bold" id="kpi-open">—</div></div></div>
            <div class="col-6 col-md-3"><div class="card text-center p-3 border-top border-4 border-info"><div class="small text-muted text-uppercase fw-semibold">In Progress</div><div class="fs-3 fw-bold" id="kpi-prog">—</div></div></div>
            <div class="col-6 col-md-3"><div class="card text-center p-3 border-top border-4 border-success"><div class="small text-muted text-uppercase fw-semibold">Completed</div><div class="fs-3 fw-bold" id="kpi-done">—</div></div></div>
            <div class="col-6 col-md-3"><div class="card text-center p-3 border-top border-4 border-danger"><div class="small text-muted text-uppercase fw-semibold">Emergencies</div><div class="fs-3 fw-bold" id="kpi-emer">—</div></div></div>
        </div>

        <div class="row g-3 mt-1">
            <div class="col-md-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">🔧 Work Orders</h5>
                        <p class="card-text text-muted">Create and manage maintenance work orders.</p>
                        <button class="btn btn-outline-warning btn-sm" onclick="loadWorkOrders()">View Orders</button>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">👷 Technicians</h5>
                        <p class="card-text text-muted">Manage technician assignments and on-call status.</p>
                        <button class="btn btn-outline-warning btn-sm" onclick="loadTechnicians()">View Team</button>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">⚙️ Spare Parts</h5>
                        <p class="card-text text-muted">Monitor spare parts inventory and stock levels.</p>
                        <button class="btn btn-outline-warning btn-sm" onclick="loadSpareParts()">View Stock</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- Dynamic data table -->
        <div class="card mt-4 d-none" id="data-card">
            <div class="card-header fw-semibold" id="data-card-title">Data</div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-light"><tr id="data-thead"></tr></thead>
                        <tbody id="data-tbody"></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <jsp:include page="../common/footer.jsp" />
    <script>
    // Load dashboard stats on page load
    apiGet('/api/maintenance/dashboard/stats').then(function(s){
        if (!s) return;
        var ws = s.workOrdersByStatus || {};
        document.getElementById('kpi-open').textContent = ws.OPEN ?? s.openCount ?? '—';
        document.getElementById('kpi-prog').textContent = ws.IN_PROGRESS ?? s.inProgressCount ?? '—';
        document.getElementById('kpi-done').textContent = ws.COMPLETED ?? s.completedCount ?? '—';
        document.getElementById('kpi-emer').textContent = s.activeEmergencyCount ?? s.emergencyCount ?? '—';
    }).catch(function(){});

    function showTable(title, headers, rows) {
        document.getElementById('data-card').classList.remove('d-none');
        document.getElementById('data-card-title').textContent = title;
        document.getElementById('data-thead').innerHTML = headers.map(function(h){ return '<th>'+h+'</th>'; }).join('');
        document.getElementById('data-tbody').innerHTML = rows;
    }

    function loadWorkOrders() {
        apiGet('/api/maintenance/work-orders').then(function(data){
            var rows = !data||!data.length ? '<tr><td colspan="5" class="text-center py-3 text-muted">No work orders.</td></tr>' :
                data.map(function(w){
                    var pBadge = {LOW:'secondary',MEDIUM:'warning',HIGH:'danger',EMERGENCY:'danger'}[w.priority]||'secondary';
                    var sBadge = {OPEN:'primary',IN_PROGRESS:'info',COMPLETED:'success',CANCELLED:'secondary'}[w.status]||'secondary';
                    return '<tr><td class="small text-muted">#'+w.id+'</td><td class="fw-medium">'+
                        (w.title||'—').substring(0,40)+'</td><td>'+
                        '<span class="badge bg-'+pBadge+'">'+w.priority+'</span></td><td>'+
                        '<span class="badge bg-'+sBadge+'">'+w.status+'</span></td><td class="small text-muted">'+
                        (w.assignedTechnicianName||'Unassigned')+'</td></tr>';
                }).join('');
            showTable('Work Orders', ['ID','Title','Priority','Status','Assigned To'], rows);
        }).catch(function(e){ showTable('Work Orders',['Error'],[e.message]); });
    }

    function loadTechnicians() {
        apiGet('/api/maintenance/technicians').then(function(data){
            var list = Array.isArray(data) ? data : (data.content||[]);
            var rows = !list.length ? '<tr><td colspan="4" class="text-center py-3 text-muted">No technicians.</td></tr>' :
                list.map(function(t){
                    return '<tr><td class="fw-medium">'+(t.fullName||t.technicianName||'—')+'</td><td>'+
                        '<span class="badge bg-secondary">'+(t.specialization||'—')+'</span></td><td class="small">'+
                        (t.employeeId||'—')+'</td><td>'+
                        ((t.onCall||t.isOnCall)?'<span class="badge bg-success">On Call</span>':'<span class="badge bg-secondary">Off</span>')+
                        '</td></tr>';
                }).join('');
            showTable('Technicians', ['Name','Specialization','Employee ID','On Call'], rows);
        }).catch(function(e){ showTable('Technicians',['Error'],[e.message]); });
    }

    function loadSpareParts() {
        apiGet('/api/maintenance/spare-parts').then(function(data){
            var rows = !data||!data.length ? '<tr><td colspan="5" class="text-center py-3 text-muted">No parts.</td></tr>' :
                data.map(function(p){
                    var low = p.stockQuantity <= (p.reorderThreshold||0);
                    return '<tr><td class="small fw-medium">'+(p.partNumber||'—')+'</td><td>'+(p.partName||'—')+'</td><td>'+
                        '<span class="badge bg-secondary">'+(p.category||'—')+'</span></td><td class="'+(low?'text-danger fw-bold':'')+'">'+
                        (p.stockQuantity??'—')+'</td><td>'+
                        '<span class="badge bg-'+(low?'danger':'success')+'">'+(low?'LOW':'OK')+'</span></td></tr>';
                }).join('');
            showTable('Spare Parts', ['Part #','Name','Category','Stock','Status'], rows);
        }).catch(function(e){ showTable('Spare Parts',['Error'],[e.message]); });
    }
    </script>
</body>
</html>
