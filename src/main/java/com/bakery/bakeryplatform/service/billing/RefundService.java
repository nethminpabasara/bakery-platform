package com.bakery.bakeryplatform.service.billing;

import com.bakery.bakeryplatform.model.billing.RefundRequest;
import com.bakery.bakeryplatform.repository.billing.RefundRepository;

import com.bakery.bakeryplatform.exception.BusinessRuleException;
import com.bakery.bakeryplatform.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RefundService {
    private final RefundRepository refundRepository;

    public RefundService(RefundRepository refundRepository) {
        this.refundRepository = refundRepository;
    }

    public RefundRequest submitRefundRequest(RefundRequest request) {
        request.setId(null);
        request.setRequestDate(LocalDate.now());
        request.setStatus("PENDING");
        return refundRepository.save(request);
    }

    public RefundRequest updateRefundStatus(Long id, String newStatus) {
        String normalizedStatus = newStatus == null ? "" : newStatus.trim().toUpperCase(Locale.ROOT);
        if (!normalizedStatus.equals("APPROVED") && !normalizedStatus.equals("REJECTED")) {
            throw new BusinessRuleException("Refund status must be APPROVED or REJECTED.");
        }

        RefundRequest request = refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found with id " + id));
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessRuleException("Only pending refund requests can be processed.");
        }
        request.setStatus(normalizedStatus);
        return refundRepository.save(request);
    }

    public List<RefundRequest> getAllRefundRequest() {
        return refundRepository.findAll();
    }

    public void withdrawRefundRequest(Long id) {
        if (!refundRepository.existsById(id)) {
            throw new ResourceNotFoundException("Refund request not found with id " + id);
        }
        refundRepository.deleteById(id);
    }
}
