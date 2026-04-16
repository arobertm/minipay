package ro.minipay.settlement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.minipay.settlement.model.SettlementBatch;
import ro.minipay.settlement.model.SettlementRecord;
import ro.minipay.settlement.service.SettlementService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST API for settlement data.
 *
 * GET /settlements/batches?date=2024-01-15      — batches for a date
 * GET /settlements/batches/merchant/{id}        — batches for a merchant
 * GET /settlements/records?date=2024-01-15      — individual records for a date
 * GET /settlements/records                      — all records
 * POST /settlements/reconcile?date=2024-01-15   — on-demand reconciliation
 */
@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/batches")
    public ResponseEntity<List<SettlementBatch>> getBatchesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(settlementService.getBatchesByDate(date));
    }

    @GetMapping("/batches/merchant/{merchantId}")
    public ResponseEntity<List<SettlementBatch>> getBatchesByMerchant(
            @PathVariable String merchantId) {
        return ResponseEntity.ok(settlementService.getBatchesByMerchant(merchantId));
    }

    @GetMapping("/records")
    public ResponseEntity<List<SettlementRecord>> getRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date != null) {
            return ResponseEntity.ok(settlementService.getRecordsByDate(date));
        }
        return ResponseEntity.ok(settlementService.getAllRecords());
    }

    @PostMapping("/reconcile")
    public ResponseEntity<Map<String, Object>> reconcile(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now().minusDays(1);
        List<SettlementBatch> batches = settlementService.reconcileDate(target);
        return ResponseEntity.ok(Map.of(
                "date", target.toString(),
                "batchesCreated", batches.size(),
                "batches", batches
        ));
    }
}
