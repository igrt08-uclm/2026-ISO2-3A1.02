package es.uclm.esi.iso2.common.configuration.contracts;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.util.List;

/**
 * Manages the examination catalogue and the other parameters of the system (UC17). Every operation needs the
 * permission {@code CONFIGURE_SYSTEM} of the requester, and every change is registered in the audit log. Nothing is
 * deleted: the entries are deactivated.
 */
public interface ISystemParameterService {
    /**
     * Returns every test type, including the deactivated ones.
     *
     * @param requester the username of the user who asks
     * @throws AccessDeniedException if the requester does not have the permission
     */
    List<TestType> getTestTypes(String requester) throws AccessDeniedException;

    /**
     * Returns one test type.
     *
     * @param requester the username of the user who asks
     * @param testTypeId the id of the test type
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if there is no test type with that id
     */
    TestType getTestType(String requester, String testTypeId) throws AccessDeniedException, ValidationException;

    /**
     * Creates a test type.
     *
     * @param requester the username of the user who asks
     * @param testType the new test type
     * @return the saved test type
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if a field is missing or invalid, or the id already exists
     */
    TestType createTestType(String requester, TestType testType) throws AccessDeniedException, ValidationException;

    /**
     * Changes a test type.
     *
     * @param requester the username of the user who asks
     * @param testTypeId the id of the test type to change
     * @param testType the new data; its id must be the same
     * @return the saved test type
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if a field is invalid or the test type does not exist
     */
    TestType updateTestType(String requester, String testTypeId, TestType testType)
            throws AccessDeniedException, ValidationException;

    /**
     * Deactivates a test type; it is never deleted, so that the orders that use it keep their reference.
     *
     * @param requester the username of the user who asks
     * @param testTypeId the id of the test type
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if the test type does not exist
     */
    void deactivateTestType(String requester, String testTypeId) throws AccessDeniedException, ValidationException;

    /**
     * Returns every protocol, including the deactivated ones.
     *
     * @param requester the username of the user who asks
     * @throws AccessDeniedException if the requester does not have the permission
     */
    List<Protocol> getProtocols(String requester) throws AccessDeniedException;

    /**
     * Creates a protocol, or changes it if its id already exists.
     *
     * @param requester the username of the user who asks
     * @param protocol the protocol to save
     * @return the saved protocol
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if a field is missing or invalid
     */
    Protocol saveProtocol(String requester, Protocol protocol) throws AccessDeniedException, ValidationException;

    /**
     * Deactivates a protocol.
     *
     * @param requester the username of the user who asks
     * @param protocolId the id of the protocol
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if the protocol does not exist
     */
    void deactivateProtocol(String requester, String protocolId) throws AccessDeniedException, ValidationException;

    /**
     * Returns every schedule, including the deactivated ones.
     *
     * @param requester the username of the user who asks
     * @throws AccessDeniedException if the requester does not have the permission
     */
    List<Schedule> getSchedules(String requester) throws AccessDeniedException;

    /**
     * Creates the schedule of a resource, or changes it if the resource already has one.
     *
     * @param requester the username of the user who asks
     * @param schedule the schedule to save
     * @return the saved schedule
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if a field is missing or invalid, or the end is not after the start
     */
    Schedule saveSchedule(String requester, Schedule schedule) throws AccessDeniedException, ValidationException;

    /**
     * Deactivates the schedule of a resource.
     *
     * @param requester the username of the user who asks
     * @param resource the id of the room or resource
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if the resource has no schedule
     */
    void deactivateSchedule(String requester, String resource) throws AccessDeniedException, ValidationException;

    /**
     * Returns every appointment policy, including the deactivated ones.
     *
     * @param requester the username of the user who asks
     * @throws AccessDeniedException if the requester does not have the permission
     */
    List<AppointmentPolicy> getAppointmentPolicies(String requester) throws AccessDeniedException;

    /**
     * Creates an appointment policy, or changes it if its id already exists.
     *
     * @param requester the username of the user who asks
     * @param policy the policy to save
     * @return the saved policy
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if a field is missing or invalid
     */
    AppointmentPolicy saveAppointmentPolicy(String requester, AppointmentPolicy policy)
            throws AccessDeniedException, ValidationException;

    /**
     * Deactivates an appointment policy.
     *
     * @param requester the username of the user who asks
     * @param policyId the id of the policy
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if the policy does not exist
     */
    void deactivateAppointmentPolicy(String requester, String policyId)
            throws AccessDeniedException, ValidationException;

    /**
     * Returns every priority, including the deactivated ones.
     *
     * @param requester the username of the user who asks
     * @throws AccessDeniedException if the requester does not have the permission
     */
    List<Priority> getPriorities(String requester) throws AccessDeniedException;

    /**
     * Creates a priority, or changes it if its id already exists.
     *
     * @param requester the username of the user who asks
     * @param priority the priority to save
     * @return the saved priority
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if a field is missing or invalid
     */
    Priority savePriority(String requester, Priority priority) throws AccessDeniedException, ValidationException;

    /**
     * Deactivates a priority.
     *
     * @param requester the username of the user who asks
     * @param priorityId the id of the priority
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if the priority does not exist
     */
    void deactivatePriority(String requester, String priorityId) throws AccessDeniedException, ValidationException;
}
