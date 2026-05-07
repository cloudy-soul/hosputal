package com.hospital.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Serves all JSP page routes.
 * All routes are public at HTTP level - JWT is validated client-side in JS.
 */
@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           org.springframework.ui.Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid credentials");
        }
        if (logout != null) {
            model.addAttribute("message", "Logged out successfully");
        }
        // Don't redirect here - just return the login page view
        return "login";
    }
    
    

    

    @GetMapping("/logout")
    public String logout() {
        // Client-side logout clears sessionStorage; this just redirects to login
        return "redirect:/login";
    }

    // ── Doctor ────────────────────────────────────────────────────
    @GetMapping("/doctor/dashboard")
    public String doctorDashboard() {
        return "doctor/dashboard";
    }

    @GetMapping("/doctor/patients")
    public String doctorPatients() {
        return "doctor/patients";
    }

    // ── Pharmacy ──────────────────────────────────────────────────
    @GetMapping("/pharmacy/dashboard")
    public String pharmacyDashboard() {
        return "pharmacy/dashboard";
    }

    // ── Maintenance ───────────────────────────────────────────────
    @GetMapping("/maintenance/dashboard")
    public String maintenanceDashboard() {
        return "maintenance/dashboard";
    }

    @GetMapping("/maintenance/maintenance")
    public String maintenanceMaintenance() {
        return "maintenance/maintenance";
    }
}
