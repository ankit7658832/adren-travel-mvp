package com.adren.travel.booking.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** BOK-15's core acceptance criteria: a Voucher references the booking, has a pdfReference, and — in this Hotel-only vertical slice — always a null atolCertificateReference. */
@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    VoucherRepository voucherRepository;

    @Mock
    DocumentStorage documentStorage;

    VoucherService service;

    @Test
    void generatesAVoucherReferencingTheBookingWithAPdfReferenceAndNullAtolCertificate() {
        service = new VoucherService(voucherRepository, documentStorage);
        UUID bookingId = UUID.randomUUID();
        when(documentStorage.store(anyString(), any())).thenReturn("vouchers/" + bookingId + "/abc123.pdf");

        Voucher voucher = service.generateFor(bookingId);

        assertThat(voucher.getBookingId()).isEqualTo(bookingId);
        assertThat(voucher.getPdfReference()).isEqualTo("vouchers/" + bookingId + "/abc123.pdf");
        assertThat(voucher.getAtolCertificateReference()).isNull();
        assertThat(voucher.getGeneratedAt()).isNotNull();

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(voucher);
    }

    @Test
    void storesTheGeneratedPdfContentUnderAVouchersKeyPrefixForTheBooking() {
        service = new VoucherService(voucherRepository, documentStorage);
        UUID bookingId = UUID.randomUUID();
        when(documentStorage.store(anyString(), any())).thenReturn("stub-reference");

        service.generateFor(bookingId);

        ArgumentCaptor<String> keyPrefixCaptor = ArgumentCaptor.forClass(String.class);
        verify(documentStorage).store(keyPrefixCaptor.capture(), any());
        assertThat(keyPrefixCaptor.getValue()).isEqualTo("vouchers/" + bookingId);
    }
}
