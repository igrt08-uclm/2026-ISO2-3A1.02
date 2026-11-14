package es.uclm.esi.iso2.client.configuration.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Screen of UC17: the administrator lists, creates, changes and deactivates the parameters of the system, with one tab
 * per group. Every operation goes to the server with the username of the requester, and the server checks the
 * permission {@code CONFIGURE_SYSTEM}.
 */
public class IU_ConfigureParameters extends JPanel {
    private static final Color ERROR_COLOR = new Color(0xB00020);
    private static final String ERROR_ICON = "⚠ ";
    private static final Color SUCCESS_COLOR = new Color(0x1B5E20);
    private static final String SUCCESS_ICON = "✔ ";

    private final transient ISystemParameterService service;
    private final String requester;
    private final transient Predicate<String> confirm;
    private final transient List<Runnable> disableActions = new ArrayList<>();

    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel lblStatus = new JLabel(" ");
    private final JButton btnBack = new JButton("Back");
    private boolean denied;

    /**
     * Creates the screen and reads every group from the server.
     *
     * @param service the service of the parameters of the system
     * @param requester the username of the logged user
     * @param confirm asks the question of a deactivation and returns {@code true} if the user accepts it; the tests
     *        pass an answer, so that they do not open a window
     * @param onBack is run when the user presses <em>Back</em>
     */
    public IU_ConfigureParameters(ISystemParameterService service, String requester, Predicate<String> confirm,
            Runnable onBack) {
        super(new BorderLayout(0, 12));
        this.service = service;
        this.requester = requester;
        this.confirm = confirm;
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel lblTitle = new JLabel("Configure system parameters");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 18f));
        btnBack.setMnemonic('B');
        btnBack.addActionListener(event -> onBack.run());

        TestTypePanel testTypes = new TestTypePanel(service, requester, confirm, this::showOperationResultSuccess,
                this::showOperationResultError, this::accessDenied);
        addTab("Test types", KeyEvent.VK_T, testTypes, testTypes::disableAll);
        // The priorities may have changed in their tab: the list of the test types is read when the tab is shown.
        tabs.addChangeListener(event -> {
            if (tabs.getSelectedComponent() == testTypes) {
                testTypes.refreshPriorities();
            }
        });
        ParameterGroupPanel<Protocol> protocols = protocolsPanel();
        addTab("Protocols", KeyEvent.VK_P, protocols, protocols::disableAll);
        ParameterGroupPanel<AppointmentPolicy> policies = policiesPanel();
        addTab("Appointment policies", KeyEvent.VK_A, policies, policies::disableAll);
        ParameterGroupPanel<Priority> priorities = prioritiesPanel();
        addTab("Priorities", KeyEvent.VK_R, priorities, priorities::disableAll);

        add(lblTitle, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    /** Shows the message of a change that was made, in green. */
    public void showOperationResultSuccess(String message) {
        lblStatus.setForeground(SUCCESS_COLOR);
        lblStatus.setText(SUCCESS_ICON + message);
    }

    /** Shows the message of a change that was rejected, in red. */
    public void showOperationResultError(String message) {
        lblStatus.setForeground(ERROR_COLOR);
        lblStatus.setText(ERROR_ICON + message);
    }

    /** The server says that the user does not have the permission: nothing can be done but go back. */
    private void accessDenied() {
        denied = true;
        showOperationResultError("You do not have permission to configure the system.");
        disableActions.forEach(Runnable::run);
    }

    private void addTab(String title, int mnemonic, JPanel panel, Runnable disable) {
        tabs.addTab(title, panel);
        tabs.setMnemonicAt(tabs.getTabCount() - 1, mnemonic);
        disableActions.add(disable);
        if (denied) {
            disable.run();
        }
    }

    private ParameterGroupPanel<Protocol> protocolsPanel() {
        return new ParameterGroupPanel<>("protocol", new ParameterGroupPanel.Operations<>() {
            @Override
            public List<Protocol> list() throws AccessDeniedException {
                return service.getProtocols(requester);
            }

            @Override
            public void save(Protocol entry) throws AccessDeniedException,
                    ValidationException {
                service.saveProtocol(requester, entry);
            }

            @Override
            public void deactivate(String id) throws AccessDeniedException,
                    ValidationException {
                service.deactivateProtocol(requester, id);
            }
        }, new ParameterGroupPanel.Adapter<>() {
            @Override
            public String id(Protocol entry) {
                return entry.getId();
            }

            @Override
            public String name(Protocol entry) {
                return entry.getName();
            }

            @Override
            public String description(Protocol entry) {
                return entry.getDescription();
            }

            @Override
            public boolean isActive(Protocol entry) {
                return entry.isActive();
            }

            @Override
            public Protocol create(String id, String name, String description) {
                return new Protocol(id, name, description, true);
            }
        }, confirm, this::showOperationResultSuccess, this::showOperationResultError, this::accessDenied);
    }

    private ParameterGroupPanel<AppointmentPolicy> policiesPanel() {
        return new ParameterGroupPanel<>("appointment policy", new ParameterGroupPanel.Operations<>() {
            @Override
            public List<AppointmentPolicy> list() throws AccessDeniedException {
                return service.getAppointmentPolicies(requester);
            }

            @Override
            public void save(AppointmentPolicy entry)
                    throws AccessDeniedException,
                    ValidationException {
                service.saveAppointmentPolicy(requester, entry);
            }

            @Override
            public void deactivate(String id) throws AccessDeniedException,
                    ValidationException {
                service.deactivateAppointmentPolicy(requester, id);
            }
        }, new ParameterGroupPanel.Adapter<>() {
            @Override
            public String id(AppointmentPolicy entry) {
                return entry.getId();
            }

            @Override
            public String name(AppointmentPolicy entry) {
                return entry.getName();
            }

            @Override
            public String description(AppointmentPolicy entry) {
                return entry.getDescription();
            }

            @Override
            public boolean isActive(AppointmentPolicy entry) {
                return entry.isActive();
            }

            @Override
            public AppointmentPolicy create(String id, String name, String description) {
                return new AppointmentPolicy(id, name, description, true);
            }
        }, confirm, this::showOperationResultSuccess, this::showOperationResultError, this::accessDenied);
    }

    private ParameterGroupPanel<Priority> prioritiesPanel() {
        return new ParameterGroupPanel<>("priority", new ParameterGroupPanel.Operations<>() {
            @Override
            public List<Priority> list() throws AccessDeniedException {
                return service.getPriorities(requester);
            }

            @Override
            public void save(Priority entry) throws AccessDeniedException,
                    ValidationException {
                service.savePriority(requester, entry);
            }

            @Override
            public void deactivate(String id) throws AccessDeniedException,
                    ValidationException {
                service.deactivatePriority(requester, id);
            }
        }, new ParameterGroupPanel.Adapter<>() {
            @Override
            public String id(Priority entry) {
                return entry.getId();
            }

            @Override
            public String name(Priority entry) {
                return entry.getName();
            }

            @Override
            public String description(Priority entry) {
                return entry.getDescription();
            }

            @Override
            public boolean isActive(Priority entry) {
                return entry.isActive();
            }

            @Override
            public Priority create(String id, String name, String description) {
                return new Priority(id, name, description, true);
            }
        }, confirm, this::showOperationResultSuccess, this::showOperationResultError, this::accessDenied);
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(lblStatus, BorderLayout.WEST);
        panel.add(btnBack, BorderLayout.EAST);
        return panel;
    }
}
