<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Hospital Management - Login</title>
    <jsp:include page="common/header.jsp" />
</head>
<body>
    <div class="container mt-5">
        <div class="row justify-content-center">
            <div class="col-md-5">
                <div class="card shadow-lg border-0">
                    <div class="card-header bg-dark text-white text-center py-3">
                        <h4><i class="bi bi-shield-lock"></i> Secure Login</h4>
                    </div>
                    <div class="card-body p-4">
                        <form action="/perform_login" method="post">
                            <div>
                                <label>Email:</label>
                                <input type="text" name="username" required/>
                            </div>
                            <div>
                                <label>Password:</label>
                                <input type="password" name="password" required/>
                            </div>
                            <button type="submit">Login</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <jsp:include page="common/footer.jsp" />
</body>
</html>