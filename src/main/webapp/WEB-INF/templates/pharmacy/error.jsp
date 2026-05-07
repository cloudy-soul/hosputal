<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Error</title>
    <jsp:include page="common/header.jsp" />
</head>
<body>
    <div class="container mt-5">
        <div class="alert alert-danger" role="alert">
            <h4 class="alert-heading">An Error Occurred!</h4>
            <p>We're sorry, but something went wrong. Please try again later.</p>
            <hr>
            <p class="mb-0">If the problem persists, please contact support.</p>
        </div>
        <a href="/" class="btn btn-primary">Go to Home</a>
    </div>
    <jsp:include page="common/footer.jsp" />
</body>
</html>