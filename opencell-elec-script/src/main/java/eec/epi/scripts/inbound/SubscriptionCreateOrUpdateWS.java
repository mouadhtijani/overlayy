package eec.epi.scripts.inbound;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.billing.SubscriptionApi;

import org.meveo.model.billing.*;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.cpq.ProductVersion;
import org.meveo.model.mediation.Access;
import org.meveo.model.notification.InboundRequest;
import org.meveo.service.billing.impl.ServiceInstanceService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.billing.impl.UserAccountService;
import org.meveo.service.catalog.impl.OfferTemplateService;
import org.meveo.service.cpq.ProductVersionService;
import org.meveo.service.medina.impl.AccessService;
import org.meveo.service.script.Script;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class SubscriptionCreateOrUpdateWS extends Script {
    private InboundRequest inboundRequest;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SubscriptionApi subscriptionApi = getServiceInterface(SubscriptionApi.class.getSimpleName());
    private final UserAccountService userAccountService = getServiceInterface(UserAccountService.class.getSimpleName());
    private final ProductVersionService productVersionService = getServiceInterface(ProductVersionService.class.getSimpleName());
    private final OfferTemplateService offerTemplateService = getServiceInterface(OfferTemplateService.class.getSimpleName());
    private final ServiceInstanceService serviceInstanceService = getServiceInterface(ServiceInstanceService.class.getSimpleName());
    private final AccessService accessService = getServiceInterface(AccessService.class.getSimpleName());
    private final SubscriptionService subscriptionService = getServiceInterface(SubscriptionService.class.getSimpleName());
    private ProductVersion productVersion;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void execute(Map<String, Object> methodContext) throws BusinessException {
        try {
            inboundRequest = (InboundRequest) methodContext.get("CONTEXT_ENTITY");
            if (!"application/json".equalsIgnoreCase(inboundRequest.getContentType())) {
                throw new UnsupportedOperationException("Unsupported content type: " + inboundRequest.getContentType());
            }
            if (!"POST".equals(inboundRequest.getMethod())) {
                throw new IllegalArgumentException("Method " + inboundRequest.getMethod() + " Not Allowed");
            }
            productVersion = findProductVersionByCode();
            Map<String, String> requestData = parseJson(inboundRequest.getBody());
            createNewSubscription(requestData);
        } catch (Exception e) {
            setResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    public void handleException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            setResponse(400, "Bad request: " + e.getMessage());
        } else if (e instanceof NullPointerException) {
            setResponse(422, "Unprocessable entity: Null value is not allowed");
        } else {
            setResponse(500, "Internal server error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }




    private void createNewSubscription(Map<String, String> requestData) {
        Subscription subscription = null;
        try {
            String userAccountCode = getRequiredValue(requestData.get("RNOCLI"), "RNOCLI");
            UserAccount userAccount = userAccountService.findByCode(userAccountCode);
            if (userAccount == null) {
                setResponse(404, "userAccount with RNOCLI = " + userAccountCode + " not found");
                return;
            }

            subscription = new Subscription();
            subscription.setCode(getRequiredValue(requestData.get("RNOSUB"), "RNOSUB"));
            Date SUDAT = sdf.parse(getRequiredValue(requestData.get("SUDAT"), "SUDAT"));
            subscription.setSubscriptionDate(SUDAT);
            subscription.setStatus(SubscriptionStatusEnum.CREATED);
            subscription.setOffer(findOfferTemplateByCode("OFFER_BORNE_VE_HTA"));
            subscription.setSeller(userAccount.getBillingAccount().getCustomerAccount().getCustomer().getSeller());
            subscription.setCfValue("cf_nature", "HTA");
            subscription.setUserAccount(userAccount);

            subscriptionService.create(subscription);
            //ServiceInstance serviceInstance = createServiceInstance(subscription, SUDAT);
            //serviceInstanceService.serviceActivation(serviceInstance);
            Access access = new Access();
            access.setAccessUserId(getRequiredValue(requestData.get("ACPDS"), "ACPDS"));
            access.setSubscription(subscription);
            accessService.create(access);
            setResponse(200, "Subscription with code " + subscription.getCode() + " was successfully created");
        } catch (Exception e) {
            handleException(e);
            String errorCode = subscription != null ? subscription.getCode() : "unknown";
            setResponse(500, "Failed to create subscription " + errorCode + ": " + e.getMessage());
        }
    }
    private ServiceInstance createServiceInstance(Subscription subscription, Date deliveryDate) {
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setCode(productVersion.getProduct().getCode());
        serviceInstance.setDescription(productVersion.getProduct().getDescription());
        serviceInstance.setSubscription(subscription);
        serviceInstance.setQuantity(BigDecimal.ONE);
        serviceInstance.setStatus(InstanceStatusEnum.INACTIVE);
        serviceInstance.setSubscriptionDate(new Date());
        serviceInstance.setDeliveryDate(deliveryDate);
        serviceInstance.setProductVersion(productVersion);
        serviceInstanceService.create(serviceInstance);
        return serviceInstance;
    }


    private OfferTemplate findOfferTemplateByCode(String code) {
        return Optional.ofNullable(code)
                .filter(c -> !c.isBlank())
                .map(offerTemplateService::findByCode)
                .orElseThrow(() -> new BusinessException("OfferTemplate not found for code: " + code));
    }



    private ProductVersion findProductVersionByCode() {
        return Optional.ofNullable("PROD_CONSO_BVE_HTA")
                .filter(c -> !c.isBlank())
                .map(productVersionService::findLastVersionByCode)
                .flatMap(list -> list.stream().findFirst())
                .orElseThrow(() -> new BusinessException("No ProductVersion found for product code: " + "PROD_CONSO_BVE_HTA"));
    }

    public String getRequiredValue(String param, String pramName) throws IllegalArgumentException {
        return Optional.ofNullable(param)
                .filter(p -> !p.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Le paramètre" + pramName + " est requis et ne peut pas être vide."));
    }

    public String getOptionalValue(String param) {
        return Optional.ofNullable(param)
                .filter(p -> !p.isBlank())
                .orElse("");
    }


    /**
     * Parses JSON input into a Map.
     *
     * @param jsonBody The JSON string.
     * @return A map containing parsed key-value pairs.
     * @throws JsonProcessingException If JSON parsing fails.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> parseJson(String jsonBody) throws JsonProcessingException {
        return objectMapper.readValue(jsonBody, Map.class);
    }

    /**
     * Sets the response status and body.
     *
     * @param code The HTTP status code.
     * @param body The response body.
     */
    public void setResponse(int code, String body) {
        inboundRequest.setResponseStatus(code);
        inboundRequest.setResponseBody(body);
    }
}
