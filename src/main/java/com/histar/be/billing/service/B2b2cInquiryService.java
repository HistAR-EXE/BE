package com.histar.be.billing.service;

import com.histar.be.billing.dto.B2b2cInquiryItem;
import com.histar.be.billing.dto.B2b2cInquiryRequest;
import com.histar.be.billing.dto.B2b2cInquiryResponse;
import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import com.histar.be.billing.repository.HeritageDigitizationInquiryRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class B2b2cInquiryService {

    private final HeritageDigitizationInquiryRepository repository;

    @Transactional
    public B2b2cInquiryResponse submit(B2b2cInquiryRequest request) {
        Instant now = Instant.now();
        HeritageDigitizationInquiry saved = repository.save(HeritageDigitizationInquiry.builder()
                .siteName(request.siteName().trim())
                .contactName(request.contactName().trim())
                .contactEmail(request.contactEmail().trim())
                .contactPhone(request.contactPhone() != null ? request.contactPhone().trim() : null)
                .packageType(request.packageType().trim().toUpperCase())
                .message(request.message())
                .status("NEW")
                .createdAt(now)
                .updatedAt(now)
                .build());
        return new B2b2cInquiryResponse(saved.getId(), saved.getStatus(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<B2b2cInquiryItem> listAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toItem)
                .toList();
    }

    private B2b2cInquiryItem toItem(HeritageDigitizationInquiry inquiry) {
        return new B2b2cInquiryItem(
                inquiry.getId(),
                inquiry.getSiteName(),
                inquiry.getContactName(),
                inquiry.getContactEmail(),
                inquiry.getContactPhone(),
                inquiry.getPackageType(),
                inquiry.getMessage(),
                inquiry.getStatus(),
                inquiry.getCreatedAt());
    }
}
