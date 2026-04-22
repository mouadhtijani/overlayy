package eec.epi.scripts.inbound;


import com  .fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.account.AccountHierarchyApi;
import org.meveo.api.dto.account.AddressDto;
import org.meveo.api.dto.account.CRMAccountHierarchyDto;
import org.meveo.api.dto.account.ContactInformationDto;
import org.meveo.api.dto.account.NameDto;
import org.meveo.model.crm.AccountHierarchyTypeEnum;
import org.meveo.model.crm.Customer;
import org.meveo.model.notification.InboundRequest;
import org.meveo.model.payments.CustomerAccountStatusEnum;
import org.meveo.service.crm.impl.CustomerCategoryService;
import org.meveo.service.crm.impl.CustomerService;
import org.meveo.service.script.Script;

import java.util.Map;
import java.util.Optional;

/**
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class CustomerCreateOrUpdateWS extends Script {
    private InboundRequest inboundRequest;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CustomerService customerService = getServiceInterface(CustomerService.class.getSimpleName());
    private final AccountHierarchyApi accountHierarchyApi = getServiceInterface(AccountHierarchyApi.class.getSimpleName());
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
            Map<String, String> requestData = parseJson(inboundRequest.getBody());
            createCustomerHierarchy(requestData);
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


    private void createCustomerHierarchy(Map<String, String> requestData) {
        CRMAccountHierarchyDto crmAccountHierarchyDtoyDto = new CRMAccountHierarchyDto();
        try {
            crmAccountHierarchyDtoyDto.setCode(getRequiredValue(requestData.get("RNOCLI"),"RNOCLI"));
            crmAccountHierarchyDtoyDto.setName(getNameDto(requestData));
            crmAccountHierarchyDtoyDto.setDescription(crmAccountHierarchyDtoyDto.getName().getLastName());
            crmAccountHierarchyDtoyDto.setRegistrationNo(getRequiredValue(requestData.get("RIDET"),"RIDET"));
            crmAccountHierarchyDtoyDto.setAddress(getAddressDto(requestData));
            crmAccountHierarchyDtoyDto.setCrmAccountType("C_UA");
            crmAccountHierarchyDtoyDto.setCurrency("EUR");
            crmAccountHierarchyDtoyDto.setLanguage("FRA");
            crmAccountHierarchyDtoyDto.setContactInformation(getContactInformationDto(requestData));
            getRequiredValue(requestData.get("RNOCLI"),"RNOCLI");
            crmAccountHierarchyDtoyDto.setCustomerCategory("ADMINISTRATION");
                    //checkCustomerCategory(getRequiredValue(requestData.get("CATCLI"),"CATCLI"));
            crmAccountHierarchyDtoyDto.setSeller("EEC");
            crmAccountHierarchyDtoyDto.setBillingCycle("CYC_CLIENT_MENSUEL");
            crmAccountHierarchyDtoyDto.setCountry("FR");
        }catch (Exception e){
            handleException(e);
        }
        accountHierarchyApi.createOrUpdateCRMAccountHierarchy(crmAccountHierarchyDtoyDto);
        setResponse(200, "CustomerHierarchy with code " + crmAccountHierarchyDtoyDto.getCode() + " was successfully created or updated");
    }

    private ContactInformationDto getContactInformationDto(Map<String, String> requestData) {
        ContactInformationDto contactInfo = new ContactInformationDto();
        contactInfo.setEmail(getRequiredValue(requestData.get("COMAIL"),"COMAIL"));
        contactInfo.setPhone(getRequiredValue(requestData.get("CPHONE"),"CPHONE"));
        return contactInfo;
    }

    private AddressDto getAddressDto(Map<String, String> requestData) {
        AddressDto addressDto = new AddressDto();
        addressDto.setAddress1(getRequiredValue(requestData.get("CNORUE"),"CNORUE"));
        addressDto.setCity(getRequiredValue(requestData.get("CCIPTT"),"CCIPTT"));
        addressDto.setCountry("NC");
        addressDto.setZipCode(getRequiredValue(requestData.get("CCDCOM"),"CCDCOM"));
        return addressDto;
    }

    private NameDto getNameDto(Map<String, String> requestData) {
        NameDto nameDto = new NameDto();
        nameDto.setLastName(getRequiredValue(requestData.get("CNOM"),"CNOM"));
        return nameDto;
    }

    public String getRequiredValue(String param,String pramName) throws IllegalArgumentException {
        return Optional.ofNullable(param)
                .filter(p -> !p.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Le paramètre"+ pramName +" est requis et ne peut pas être vide."));
    }
    public String getOptionalValue(String param) {
        return Optional.ofNullable(param)
                .filter(p -> !p.isBlank())
                .orElse("");
    }


    private String checkCustomerCategory(String cusCatCode) throws IllegalArgumentException {
        if("ADMINISTRATION".equals(cusCatCode)
                ||"PARTICULIER_PRIVE".equals(cusCatCode)
                ||"PROFESSIONNEL_PRIVE".equals(cusCatCode))
            throw  new IllegalArgumentException("CustomerCategory must be ADMINISTRATION or PARTICULIER_PRIVE or PROFESSIONNEL_PRIVE");
        return cusCatCode;
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
