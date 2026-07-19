package com.adren.travel.ads.internal;

import com.adren.travel.ads.AdAccountView;
import com.adren.travel.ads.AdCampaignCreativeVariantView;
import com.adren.travel.ads.AdCampaignView;
import com.adren.travel.ads.CreateCampaignCommand;
import com.adren.travel.ads.SubmitCampaignInputsCommand;
import com.adren.travel.ai.AiApi;
import com.adren.travel.booking.BookingApi;
import com.adren.travel.booking.PackageView;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdsServiceImplTest {

    @Mock
    BookingApi bookingApi;

    @Mock
    AiApi aiApi;

    @Mock
    AdAccountRepository adAccountRepository;

    @Mock
    MetaAdsClient metaAdsClient;

    @Mock
    AdCampaignRepository adCampaignRepository;

    @Mock
    AdCampaignCreativeVariantRepository creativeVariantRepository;

    @Mock
    ApplicationEventPublisher events;

    private AdsServiceImpl service() {
        return new AdsServiceImpl(bookingApi, aiApi, adAccountRepository, metaAdsClient, adCampaignRepository,
            creativeVariantRepository, events);
    }

    @Test
    void provisionAdAccountCreatesANewAccountViaTheMetaClientWhenNoneExistsADS01() {
        UUID consultantId = UUID.randomUUID();
        when(adAccountRepository.findByConsultantId(consultantId)).thenReturn(Optional.empty());
        when(metaAdsClient.provisionAdAccount(consultantId)).thenReturn("stub-bm-123");
        when(adAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdAccountView view = service().provisionAdAccount(consultantId);

        assertThat(view.consultantId()).isEqualTo(consultantId);
        assertThat(view.metaBusinessManagerId()).isEqualTo("stub-bm-123");
        verify(metaAdsClient).provisionAdAccount(consultantId);
        verify(adAccountRepository).save(any());
    }

    @Test
    void provisionAdAccountIsIdempotentReturningTheExistingAccountRatherThanProvisioningATwinADS01() {
        UUID consultantId = UUID.randomUUID();
        AdAccount existing = new AdAccount(UUID.randomUUID(), consultantId, "stub-bm-already-provisioned");
        when(adAccountRepository.findByConsultantId(consultantId)).thenReturn(Optional.of(existing));

        AdAccountView view = service().provisionAdAccount(consultantId);

        assertThat(view.metaBusinessManagerId()).isEqualTo("stub-bm-already-provisioned");
        verify(metaAdsClient, never()).provisionAdAccount(any());
        verify(adAccountRepository, never()).save(any());
    }

    @Test
    void createCampaignStartsInPendingApprovalWithThePackagesOwnCurrencyAndPublishesAnEventADS02() {
        UUID packageId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        PackageView travelPackage = new PackageView(packageId, UUID.randomUUID(), consultantId, "Goa Escape",
            "A relaxing package", LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(20000), BigDecimal.valueOf(5000), CurrencyCode.INR, 4, false);
        when(bookingApi.findPackageById(packageId)).thenReturn(travelPackage);
        when(adCampaignRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdCampaignView view = service().createCampaign(new CreateCampaignCommand(packageId));

        assertThat(view.status()).isEqualTo("PENDING_APPROVAL");
        assertThat(view.packageId()).isEqualTo(packageId);
        assertThat(view.consultantId()).isEqualTo(consultantId);
        assertThat(view.budgetCapCurrency()).isEqualTo(CurrencyCode.INR);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(com.adren.travel.ads.event.AdCampaignCreatedEvent.class);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitCampaignInputsSetsFieldsAndPublishesAnEventWhenCalledByTheOwningConsultantADS03() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new SubmitCampaignInputsCommand(campaignId, "Adults 25-45", new BigDecimal("500.00"), 14);

        AdCampaignView view = service().submitCampaignInputs(command);

        assertThat(view.audienceDescription()).isEqualTo("Adults 25-45");
        assertThat(view.budgetCapAmount()).isEqualByComparingTo("500.00");
        assertThat(view.durationDays()).isEqualTo(14);
        verify(adCampaignRepository).save(campaign);
        verify(events).publishEvent(any(com.adren.travel.ads.event.AdCampaignInputsSubmittedEvent.class));
    }

    @Test
    void submitCampaignInputsRejectsACallerFromAnotherConsultantADS03() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), ownerConsultantId, CurrencyCode.INR);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        var command = new SubmitCampaignInputsCommand(campaignId, "Adults 25-45", new BigDecimal("500.00"), 14);

        assertThatThrownBy(() -> service().submitCampaignInputs(command)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void generateCreativeForCampaignPersistsEverySurvivingVariantADS04() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, packageId, consultantId, CurrencyCode.INR);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        authenticateAs(Role.CONSULTANT, consultantId);
        PackageView travelPackage = new PackageView(packageId, UUID.randomUUID(), consultantId, "Goa Escape",
            "A relaxing package", LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(20000), BigDecimal.valueOf(5000), CurrencyCode.INR, 4, false);
        when(bookingApi.findPackageById(packageId)).thenReturn(travelPackage);
        UUID auditLogId = UUID.randomUUID();
        when(aiApi.generateAdCreative(any())).thenReturn(new com.adren.travel.ai.AdCreativeSuggestion(auditLogId,
            List.of(new com.adren.travel.ai.AdCreativeVariant("Escape to Goa", "Book now at INR 25000"),
                new com.adren.travel.ai.AdCreativeVariant("Goa Awaits", "Sun, sand, and savings"))));

        var result = service().generateCreativeForCampaign(campaignId, 2);

        assertThat(result).isInstanceOf(com.adren.travel.ai.AdCreativeSuggestion.class);
        verify(creativeVariantRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void generateCreativeForCampaignPersistsNothingWhenNoViableCreativeSurvivesADS04() {
        UUID consultantId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, packageId, consultantId, CurrencyCode.INR);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        authenticateAs(Role.CONSULTANT, consultantId);
        PackageView travelPackage = new PackageView(packageId, UUID.randomUUID(), consultantId, "Goa Escape",
            "A relaxing package", LocalDate.now().plusDays(30), LocalDate.now().plusDays(90),
            BigDecimal.valueOf(20000), BigDecimal.valueOf(5000), CurrencyCode.INR, 4, false);
        when(bookingApi.findPackageById(packageId)).thenReturn(travelPackage);
        when(aiApi.generateAdCreative(any()))
            .thenReturn(new com.adren.travel.ai.NoViableAdCreative(UUID.randomUUID(), "No candidate referenced the real price"));

        var result = service().generateCreativeForCampaign(campaignId, 2);

        assertThat(result).isInstanceOf(com.adren.travel.ai.NoViableAdCreative.class);
        verify(creativeVariantRepository, never()).save(any());
    }

    @Test
    void findCreativeVariantsForCampaignRejectsACallerFromAnotherConsultantADS04() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), ownerConsultantId, CurrencyCode.INR);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> service().findCreativeVariantsForCampaign(campaignId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approveCreativeVariantFlipsTheFlagAndPublishesAnEventADS05() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        AdCampaignCreativeVariant variant =
            new AdCampaignCreativeVariant(variantId, campaignId, "Escape to Goa", "Book now", null);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(creativeVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        authenticateAs(Role.CONSULTANT, consultantId);

        AdCampaignCreativeVariantView view = service().approveCreativeVariant(campaignId, variantId);

        assertThat(view.approved()).isTrue();
        verify(creativeVariantRepository).save(variant);
        verify(events).publishEvent(any(com.adren.travel.ads.event.AdCampaignCreativeVariantApprovedEvent.class));
    }

    @Test
    void approveCreativeVariantRejectsAVariantThatDoesNotBelongToTheGivenCampaignADS05() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        UUID otherCampaignId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        AdCampaignCreativeVariant variant =
            new AdCampaignCreativeVariant(variantId, otherCampaignId, "Escape to Goa", "Book now", null);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(creativeVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        authenticateAs(Role.CONSULTANT, consultantId);

        assertThatThrownBy(() -> service().approveCreativeVariant(campaignId, variantId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approveCreativeVariantRejectsACallerFromAnotherConsultantADS05() {
        UUID ownerConsultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), ownerConsultantId, CurrencyCode.INR);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> service().approveCreativeVariant(campaignId, variantId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void submitCampaignForPolicyReviewTransitionsWhenEveryVariantIsApprovedADS06() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        AdCampaignCreativeVariant variant =
            new AdCampaignCreativeVariant(UUID.randomUUID(), campaignId, "Escape to Goa", "Book now", null);
        variant.approve();
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(creativeVariantRepository.findByCampaignId(campaignId)).thenReturn(List.of(variant));
        authenticateAs(Role.CONSULTANT, consultantId);

        AdCampaignView view = service().submitCampaignForPolicyReview(campaignId);

        assertThat(view.status()).isEqualTo("PENDING_POLICY_REVIEW");
        verify(events).publishEvent(any(com.adren.travel.ads.event.AdCampaignSubmittedForPolicyReviewEvent.class));
    }

    @Test
    void submitCampaignForPolicyReviewBlocksSubmissionWhenAVariantIsNotYetApprovedADS05() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        AdCampaignCreativeVariant approved =
            new AdCampaignCreativeVariant(UUID.randomUUID(), campaignId, "Escape to Goa", "Book now", null);
        approved.approve();
        AdCampaignCreativeVariant unapproved =
            new AdCampaignCreativeVariant(UUID.randomUUID(), campaignId, "Goa Awaits", "Sun and sand", null);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(creativeVariantRepository.findByCampaignId(campaignId)).thenReturn(List.of(approved, unapproved));
        authenticateAs(Role.CONSULTANT, consultantId);

        assertThatThrownBy(() -> service().submitCampaignForPolicyReview(campaignId))
            .isInstanceOf(IllegalStateException.class);
        verify(events, never()).publishEvent(any(com.adren.travel.ads.event.AdCampaignSubmittedForPolicyReviewEvent.class));
    }

    @Test
    void submitCampaignForPolicyReviewBlocksSubmissionWhenNoVariantsExistAtAllADS05() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(creativeVariantRepository.findByCampaignId(campaignId)).thenReturn(List.of());
        authenticateAs(Role.CONSULTANT, consultantId);

        assertThatThrownBy(() -> service().submitCampaignForPolicyReview(campaignId))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectCampaignPolicyReviewTransitionsAndStoresTheReasonADS06() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        campaign.submitForPolicyReview();
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

        AdCampaignView view = service().rejectCampaignPolicyReview(campaignId, "Unverified claim in headline");

        assertThat(view.status()).isEqualTo("REJECTED");
        assertThat(view.rejectionReason()).isEqualTo("Unverified claim in headline");
        verify(events).publishEvent(any(com.adren.travel.ads.event.AdCampaignPolicyReviewRejectedEvent.class));
    }

    @Test
    void findCampaignsPendingPolicyReviewReturnsOnlyThatStatusADS06() {
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), UUID.randomUUID(), CurrencyCode.INR);
        campaign.submitForPolicyReview();
        when(adCampaignRepository.findByStatus(eq(AdCampaignStatus.PENDING_POLICY_REVIEW), any()))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(campaign)));

        var page = service().findCampaignsPendingPolicyReview(org.springframework.data.domain.Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).campaignId()).isEqualTo(campaignId);
    }

    @Test
    void launchCampaignTransitionsToLiveStoresTheMetaRefAndPublishesAnEventADS07() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        campaign.submitForPolicyReview();
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(metaAdsClient.launchCampaign(campaignId)).thenReturn("stub-campaign-abc");

        AdCampaignView view = service().launchCampaign(campaignId);

        assertThat(view.status()).isEqualTo("LIVE");
        assertThat(view.metaCampaignRef()).isEqualTo("stub-campaign-abc");
        verify(events).publishEvent(any(com.adren.travel.ads.event.AdCampaignLaunchedEvent.class));
    }

    @Test
    void launchCampaignRejectsACampaignThatHasNotPassedPolicyReviewADS07() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        when(adCampaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service().launchCampaign(campaignId)).isInstanceOf(IllegalStateException.class);
        verify(metaAdsClient, never()).launchCampaign(any());
    }

    @Test
    void findCampaignsForConsultantReturnsThatConsultantsOwnCampaignsADS09() {
        UUID consultantId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        AdCampaign campaign = new AdCampaign(campaignId, UUID.randomUUID(), consultantId, CurrencyCode.INR);
        when(adCampaignRepository.findByConsultantId(eq(consultantId), any()))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(campaign)));
        authenticateAs(Role.CONSULTANT, consultantId);

        var page = service().findCampaignsForConsultant(consultantId, org.springframework.data.domain.Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).campaignId()).isEqualTo(campaignId);
    }

    @Test
    void findCampaignsForConsultantRejectsACallerFromAnotherConsultantADS09() {
        UUID ownerConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThatThrownBy(() -> service().findCampaignsForConsultant(
            ownerConsultantId, org.springframework.data.domain.Pageable.unpaged()))
            .isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
