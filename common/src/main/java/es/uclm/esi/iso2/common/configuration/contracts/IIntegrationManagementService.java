package es.uclm.esi.iso2.common.configuration.contracts;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.domain.ConnectionTestResult;
import es.uclm.esi.iso2.common.configuration.domain.HospitalSystemType;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationConfiguration;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.util.List;

/**
 * Manages the integrations with the external hospital systems (UC20): SSO, electronic health record, admissions and
 * RIS/PACS. Every operation needs the permission {@code CONFIGURE_INTEGRATIONS} of the requester, and every change of a
 * configuration is registered in the audit log. The connection test never changes anything.
 */
public interface IIntegrationManagementService {
    /**
     * Returns the integration of every external system, always in the order SSO, EHR, admissions and RIS/PACS.
     *
     * @param requester the username of the user who asks
     * @throws AccessDeniedException if the requester does not have the permission
     */
    List<IntegrationConfiguration> getIntegrations(String requester) throws AccessDeniedException;

    /**
     * Returns the integration of one external system.
     *
     * @param requester the username of the user who asks
     * @param system the external system
     * @throws AccessDeniedException if the requester does not have the permission
     */
    IntegrationConfiguration getIntegration(String requester, HospitalSystemType system)
            throws AccessDeniedException;

    /**
     * Saves the configuration of an integration: its endpoint, its settings, whether it is enabled and its mode. The
     * status is set by the server from {@code enabled}, whatever the configuration says.
     *
     * @param requester the username of the user who asks
     * @param configuration the new configuration
     * @return the saved configuration, with its status
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if a field is missing or invalid; every error comes together and nothing is saved
     */
    IntegrationConfiguration configureIntegration(String requester, IntegrationConfiguration configuration)
            throws AccessDeniedException, ValidationException;

    /**
     * Tests the connection with an external system through the interface of that system. It never changes the stored
     * configuration and it is not audited.
     *
     * @param requester the username of the user who asks
     * @param system the external system
     * @return whether the system answered, how long it took and the reason
     * @throws AccessDeniedException if the requester does not have the permission
     */
    ConnectionTestResult testConnection(String requester, HospitalSystemType system) throws AccessDeniedException;
}
