package com.hospital.dto;

/**
 * RequestDtos.java was previously a multi-class holder with Lombok annotations.
 * That caused duplicate/conflicting public classes when Lombok processing failed.
 *
 * This file now contains a harmless holder to avoid duplicate class definitions
 * — the real DTOs live in dedicated files in this package (e.g. DispenseRequest.java,
 * ReceiveShipmentRequest.java, RejectPrescriptionRequest.java, etc.).
 */
public final class RequestDtos {
    private RequestDtos() {}
}
