package es.uclm.esi.iso2.client.patients.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of the screen {@link IU_ManagePatients} of UC12 (#45): the table and the line with the number found, the form
 * of the selected patient, the search (Q3, Q4), the validation of the client with its messages and red borders (Q5),
 * the registration and the modification (Q7), the duplicate (Q6), the errors of the server, the denied access, and the
 * question before the unsaved changes are thrown away (Q8). The screen is only built, never shown, so the tests need
 * no display; every question is answered by the test, which also records it.
 */
@DisplayName("IU_ManagePatients")
class IU_ManagePatientsTest {
    private static final String REQUESTER = "lmartin";
    private static final String SEARCH = "Search";
    private static final String IDENTIFIER = "Identifier (health card number)";
    private static final String NATIONAL_ID = "National ID (optional)";
    private static final String FULL_NAME = "Full name";
    private static final String BIRTH = "Date of birth (dd/MM/yyyy)";
    private static final String SEX = "Sex";
    private static final String ADDRESS = "Address";
    private static final String PHONE = "Phone";
    private static final String EMAIL = "E-mail";
    private static final List<String> TEXT_FIELDS =
            List.of(IDENTIFIER, NATIONAL_ID, FULL_NAME, BIRTH, ADDRESS, PHONE, EMAIL);
    private static final String DISCARD = "Discard the unsaved changes?";
    private static final String DENIED = "⚠ You do not have permission to manage patients.";
    private static final String DUPLICATE = "⚠ A patient with this identifier already exists: the record is loaded"
            + " so that you can modify it.";
    private static final String BAD_DATE = "⚠ The date of birth must be written as dd/MM/yyyy.";
    private static final String BAD_EMAIL = "⚠ The e-mail must have the form name@domain.ext.";
    private static final String BAD_PHONE = "⚠ The phone must have 9 to 15 digits.";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color GREEN = new Color(0x1B5E20);
    private static final Color RED = new Color(0xB00020);
    private static final int LUCIA = 0;
    private static final int MARIO = 1;
    private static final int ELENA = 2;

    private FakePatientService service;
    private List<String> questions;
    private boolean answer;
    private int backs;

    @BeforeEach
    void setUp() {
        service = FakePatientService.withThreePatients();
        questions = new ArrayList<>();
        answer = true;
        backs = 0;
    }

    private IU_ManagePatients open() {
        return new IU_ManagePatients(service, REQUESTER, question -> {
            questions.add(question);
            return answer;
        }, () -> backs++);
    }

    private static void select(IU_ManagePatients screen, int row) {
        Screen.table(screen).setRowSelectionInterval(row, row);
    }

    private static int selectedRow(IU_ManagePatients screen) {
        return Screen.table(screen).getSelectedRow();
    }

    private static JTextField field(IU_ManagePatients screen, String label) {
        return Screen.field(screen, label);
    }

    private static String text(IU_ManagePatients screen, String label) {
        return field(screen, label).getText();
    }

    private static void type(IU_ManagePatients screen, String label, String text) {
        field(screen, label).setText(text);
    }

    private static JComboBox<Sex> sex(IU_ManagePatients screen) {
        return Screen.combo(screen, SEX);
    }

    private static void save(IU_ManagePatients screen) {
        Screen.button(screen, "Save").doClick();
    }

    private static void newPatient(IU_ManagePatients screen) {
        Screen.button(screen, "New patient").doClick();
    }

    private static void search(IU_ManagePatients screen, String text) {
        type(screen, SEARCH, text);
        Screen.button(screen, SEARCH).doClick();
    }

    /** A new patient that the client accepts, with every field written. */
    private static void fillValid(IU_ManagePatients screen) {
        type(screen, IDENTIFIER, "HC-0000004");
        type(screen, NATIONAL_ID, "87654321X");
        type(screen, FULL_NAME, "Ana Nueva Inventada");
        type(screen, BIRTH, "01/02/1990");
        sex(screen).setSelectedItem(Sex.FEMALE);
        type(screen, ADDRESS, "Calle Nueva 4, 13001 Ciudad Real");
        type(screen, PHONE, "600000004");
        type(screen, EMAIL, "ana.nueva@example.test");
    }

    private static boolean marked(JComponent field) {
        Border border = field.getBorder();
        return border instanceof LineBorder line && RED.equals(line.getLineColor());
    }

    private static List<String> markedFields(IU_ManagePatients screen) {
        List<String> marked = new ArrayList<>();
        for (String label : List.of(IDENTIFIER, NATIONAL_ID, FULL_NAME, BIRTH, SEX, ADDRESS, PHONE, EMAIL)) {
            if (marked(Screen.labelled(screen, label))) {
                marked.add(label);
            }
        }
        return marked;
    }

    private static List<String> form(IU_ManagePatients screen) {
        List<String> values = new ArrayList<>();
        for (String label : TEXT_FIELDS) {
            values.add(text(screen, label));
        }
        values.add(String.valueOf(sex(screen).getSelectedItem()));
        return values;
    }

    private static String rendered(JComboBox<Sex> combo, Sex value) {
        Component component =
                combo.getRenderer().getListCellRendererComponent(new JList<>(), value, 0, false, false);
        return ((JLabel) component).getText();
    }

    @Nested
    @DisplayName("1. Display")
    class Display {
        @Test
        @DisplayName("TC12C-01 Opens with every patient, three columns, dates as dd/MM/yyyy and nothing selected")
        void opensWithEveryPatient() {
            // Act
            IU_ManagePatients screen = open();
            JTable table = Screen.table(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("Identifier", "Full name", "Date of birth"),
                            List.of(table.getColumnName(0), table.getColumnName(1), table.getColumnName(2))),
                    () -> assertEquals(List.of("HC-0000001", "HC-0000002", "HC-0000003"), Screen.column(table, 0)),
                    () -> assertEquals(List.of("Lucía Ejemplo Prueba", "Mario Demo Simulado",
                            "Elena Muestra Ficticia"), Screen.column(table, 1)),
                    () -> assertEquals(List.of("14/03/1980", "02/11/1975", "25/07/2001"), Screen.column(table, 2)),
                    () -> assertFalse(table.isCellEditable(0, 0)),
                    () -> assertEquals("3 patients found.", Screen.found(screen)),
                    () -> assertEquals(-1, table.getSelectedRow()),
                    () -> assertEquals(List.of("", "", "", "", "", "", "", "null"), form(screen)),
                    () -> assertTrue(field(screen, IDENTIFIER).isEnabled()),
                    () -> assertEquals(" ", Screen.status(screen)),
                    () -> assertEquals(List.of(""), service.searches()),
                    () -> assertEquals(List.of(REQUESTER), service.requesters()),
                    () -> assertTrue(markedFields(screen).isEmpty()),
                    () -> assertTrue(questions.isEmpty()));
        }

        @Test
        @DisplayName("TC12C-02 The line says 1 patient found (singular) and 0 patients found with an empty table")
        void lineWithOneAndNone() {
            // Arrange
            IU_ManagePatients screen = open();

            // Act
            search(screen, "HC-0000003");
            String one = Screen.found(screen);
            int oneRow = Screen.table(screen).getRowCount();
            service = new FakePatientService();
            IU_ManagePatients empty = open();

            // Assert
            assertAll(
                    () -> assertEquals("1 patient found.", one),
                    () -> assertEquals(1, oneRow),
                    () -> assertEquals("0 patients found.", Screen.found(empty)),
                    () -> assertEquals(0, Screen.table(empty).getRowCount()));
        }

        @Test
        @DisplayName("TC12C-03 The sex is chosen from Female, Male and Unspecified, with nothing chosen at the start")
        void sexIsChosenFromAList() {
            // Act
            JComboBox<Sex> combo = sex(open());

            // Assert
            assertAll(
                    () -> assertEquals(3, combo.getItemCount()),
                    () -> assertEquals(List.of(Sex.FEMALE, Sex.MALE, Sex.UNSPECIFIED),
                            List.of(combo.getItemAt(0), combo.getItemAt(1), combo.getItemAt(2))),
                    () -> assertEquals(-1, combo.getSelectedIndex()),
                    () -> assertEquals("Female", rendered(combo, Sex.FEMALE)),
                    () -> assertEquals("Male", rendered(combo, Sex.MALE)),
                    () -> assertEquals("Unspecified", rendered(combo, Sex.UNSPECIFIED)),
                    () -> assertEquals(" ", rendered(combo, null)),
                    () -> assertFalse(combo.isEditable()));
        }

        @Test
        @DisplayName("TC12C-04 Keyboard: Alt+H, Alt+N, Alt+S, Alt+B and the letters of the fields")
        void keyboardShortcuts() {
            // Act
            IU_ManagePatients screen = open();

            // Assert
            assertAll(
                    () -> assertEquals(KeyEvent.VK_H, Screen.button(screen, SEARCH).getMnemonic()),
                    () -> assertEquals(KeyEvent.VK_N, Screen.button(screen, "New patient").getMnemonic()),
                    () -> assertEquals(KeyEvent.VK_S, Screen.button(screen, "Save").getMnemonic()),
                    () -> assertEquals(KeyEvent.VK_B, Screen.button(screen, "Back").getMnemonic()),
                    () -> assertEquals(KeyEvent.VK_I, Screen.labelOf(screen, IDENTIFIER).getDisplayedMnemonic()),
                    () -> assertEquals(KeyEvent.VK_A, Screen.labelOf(screen, NATIONAL_ID).getDisplayedMnemonic()),
                    () -> assertEquals(KeyEvent.VK_F, Screen.labelOf(screen, FULL_NAME).getDisplayedMnemonic()),
                    () -> assertEquals(KeyEvent.VK_D, Screen.labelOf(screen, BIRTH).getDisplayedMnemonic()),
                    () -> assertEquals(KeyEvent.VK_X, Screen.labelOf(screen, SEX).getDisplayedMnemonic()),
                    () -> assertEquals(KeyEvent.VK_R, Screen.labelOf(screen, ADDRESS).getDisplayedMnemonic()),
                    () -> assertEquals(KeyEvent.VK_O, Screen.labelOf(screen, PHONE).getDisplayedMnemonic()),
                    () -> assertEquals(KeyEvent.VK_E, Screen.labelOf(screen, EMAIL).getDisplayedMnemonic()));
        }

        @Test
        @DisplayName("TC12C-05 Back runs its action and sends nothing")
        void backRunsItsAction() {
            // Arrange
            IU_ManagePatients screen = open();

            // Act
            Screen.button(screen, "Back").doClick();

            // Assert
            assertAll(
                    () -> assertEquals(1, backs),
                    () -> assertEquals(1, service.searches().size()),
                    () -> assertEquals(0, service.changes()));
        }
    }

    @Nested
    @DisplayName("2. Form of the selected patient")
    class SelectedPatient {
        @Test
        @DisplayName("TC12C-06 Pressing a row loads every field of that patient, with the identifier locked")
        void rowLoadsThePatient() {
            // Arrange
            IU_ManagePatients screen = open();

            // Act
            select(screen, MARIO);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("HC-0000002", "12345678Z", "Mario Demo Simulado", "02/11/1975",
                            "Avenida de Muestra 22, 45001 Toledo", "600000002", "mario.demo@example.test", "MALE"),
                            form(screen)),
                    () -> assertFalse(field(screen, IDENTIFIER).isEnabled()),
                    () -> assertTrue(field(screen, FULL_NAME).isEnabled()),
                    () -> assertTrue(sex(screen).isEnabled()),
                    () -> assertTrue(Screen.enabled(screen, "Save")),
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-06 Pressing another row loads the other patient; empty optional data gives empty fields")
        void anotherRowAndEmptyOptionalData() {
            // Arrange
            service.add(new Patient("P-0009", "HC-0000009", null, "Sin Datos Opcionales", LocalDate.of(1999, 1, 1),
                    Sex.MALE, null, null, null, true));
            IU_ManagePatients screen = open();
            select(screen, LUCIA);

            // Act
            select(screen, 3);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("HC-0000009", "", "Sin Datos Opcionales", "01/01/1999", "", "", "",
                            "MALE"), form(screen)),
                    () -> assertTrue(questions.isEmpty()));
        }

        @Test
        @DisplayName("TC12C-07 New patient empties the form, clears the selection and unlocks the identifier")
        void newPatientEmptiesTheForm() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, ELENA);

            // Act
            newPatient(screen);

            // Assert
            assertAll(
                    () -> assertEquals(-1, selectedRow(screen)),
                    () -> assertEquals(List.of("", "", "", "", "", "", "", "null"), form(screen)),
                    () -> assertTrue(field(screen, IDENTIFIER).isEnabled()),
                    () -> assertTrue(questions.isEmpty()));
        }
    }

    @Nested
    @DisplayName("3. Search")
    class Search {
        @Test
        @DisplayName("TC12C-08 A text is sent to the server and the matches are shown, with nothing selected")
        void searchWithAText() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);

            // Act
            search(screen, "mario");

            // Assert
            assertAll(
                    () -> assertEquals(List.of("", "mario"), service.searches()),
                    () -> assertEquals(List.of("HC-0000002"), Screen.column(Screen.table(screen), 0)),
                    () -> assertEquals("1 patient found.", Screen.found(screen)),
                    () -> assertEquals(-1, selectedRow(screen)),
                    () -> assertEquals("", text(screen, IDENTIFIER)),
                    () -> assertTrue(questions.isEmpty()));
        }

        @Test
        @DisplayName("TC12C-09 An empty text lists every patient again")
        void emptyTextListsEveryone() {
            // Arrange
            IU_ManagePatients screen = open();
            search(screen, "elena");

            // Act
            search(screen, "");

            // Assert
            assertAll(
                    () -> assertEquals(List.of("", "elena", ""), service.searches()),
                    () -> assertEquals(3, Screen.table(screen).getRowCount()),
                    () -> assertEquals("3 patients found.", Screen.found(screen)));
        }

        @Test
        @DisplayName("TC12C-10 A text with no match gives an empty table and 0 patients found")
        void noMatch() {
            // Arrange
            IU_ManagePatients screen = open();

            // Act
            search(screen, "zzz");

            // Assert
            assertAll(
                    () -> assertEquals(0, Screen.table(screen).getRowCount()),
                    () -> assertEquals("0 patients found.", Screen.found(screen)),
                    () -> assertEquals(List.of("", "", "", "", "", "", "", "null"), form(screen)));
        }

        @Test
        @DisplayName("TC12C-11 The Enter key in the search box searches")
        void enterSearches() {
            // Arrange
            IU_ManagePatients screen = open();
            type(screen, SEARCH, "HC-00000");

            // Act
            field(screen, SEARCH).postActionEvent();

            // Assert
            assertAll(
                    () -> assertEquals(List.of("", "HC-00000"), service.searches()),
                    () -> assertEquals("3 patients found.", Screen.found(screen)));
        }
    }

    @Nested
    @DisplayName("4. Validation in the client")
    class Validation {
        @Test
        @DisplayName("TC12C-12 An empty form gives the four errors of the required data together, in red, and marks them")
        void emptyFormGivesFourErrors() {
            // Arrange
            IU_ManagePatients screen = open();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The identifier is required. The full name is required. The date of birth"
                            + " is required. The sex is required.", Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()),
                    () -> assertEquals(List.of(IDENTIFIER, FULL_NAME, BIRTH, SEX), markedFields(screen)),
                    () -> assertEquals(0, service.changes()));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {IDENTIFIER, FULL_NAME, BIRTH})
        @DisplayName("TC12C-13 A required text with only spaces gives only its error and marks only its field")
        void requiredTextWithSpaces(String label) {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, label, "   ");

            // Act
            save(screen);

            // Assert
            String expected = switch (label) {
                case IDENTIFIER -> "⚠ The identifier is required.";
                case FULL_NAME -> "⚠ The full name is required.";
                default -> "⚠ The date of birth is required.";
            };
            assertAll(
                    () -> assertEquals(expected, Screen.status(screen)),
                    () -> assertEquals(List.of(label), markedFields(screen)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-13 No sex chosen gives only its error and marks the list")
        void noSex() {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            sex(screen).setSelectedIndex(-1);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The sex is required.", Screen.status(screen)),
                    () -> assertEquals(List.of(SEX), markedFields(screen)),
                    () -> assertEquals(0, service.changes()));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"31/02/2000", "29/02/2001", "31/04/2000", "32/01/2000", "00/01/2000", "15/13/2000",
            "1/2/2000", "2000-02-01", "01-02-2000", "01/02/00", "abc", "01/02/2000x"})
        @DisplayName("TC12C-14 A date that is badly written or not real is rejected and nothing is sent")
        void badDate(String date) {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, BIRTH, date);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(BAD_DATE, Screen.status(screen)),
                    () -> assertEquals(List.of(BIRTH), markedFields(screen)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-15 A date of birth tomorrow is in the future and is rejected")
        void tomorrowIsRejected() {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, BIRTH, LocalDate.now().plusDays(1).format(DATE));

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The date of birth cannot be in the future.", Screen.status(screen)),
                    () -> assertEquals(List.of(BIRTH), markedFields(screen)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-15 Today, 29/02 of a leap year and spaces around the date are accepted")
        void validDateBoundaries() {
            // Arrange
            LocalDate today = LocalDate.now();
            IU_ManagePatients screen = open();
            List<LocalDate> sent = new ArrayList<>();

            // Act
            for (String date : List.of(today.format(DATE), "29/02/2000", "  01/01/1900  ")) {
                newPatient(screen);
                fillValid(screen);
                type(screen, IDENTIFIER, "HC-" + date.strip());
                type(screen, BIRTH, date);
                save(screen);
                sent.add(service.registrations().get(service.registrations().size() - 1).getDateOfBirth());
            }

            // Assert
            assertEquals(List.of(today, LocalDate.of(2000, 2, 29), LocalDate.of(1900, 1, 1)), sent);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"a@b.c", "name@domain.ext", "name.surname@sub.domain.ext", ""})
        @DisplayName("TC12C-16 E-mails with the form name@domain.ext, or no e-mail, are accepted")
        void validEmail(String email) {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, EMAIL, email);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(1, service.registrations().size()),
                    () -> assertEquals(email, service.registrations().get(0).getEmail()));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"name.domain.ext", "name@domain", "name@@domain.ext", "name@do@main.ext",
            "@domain.ext", "name@.ext", "na me@domain.ext", "name@domain."})
        @DisplayName("TC12C-16 E-mails without @, without a dot after it, with two @ or with spaces are rejected")
        void invalidEmail(String email) {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, EMAIL, email);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(BAD_EMAIL, Screen.status(screen)),
                    () -> assertEquals(List.of(EMAIL), markedFields(screen)),
                    () -> assertEquals(0, service.changes()));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"600000001", "123456789012345", "600 000 001", "+34600000001", "+34 600 000 001",
            ""})
        @DisplayName("TC12C-17 Phones of 9 and 15 digits, with spaces or a leading +, or no phone, are accepted")
        void validPhone(String phone) {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, PHONE, phone);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(1, service.registrations().size()),
                    () -> assertEquals(phone, service.registrations().get(0).getPhone()));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"60000000", "1234567890123456", "60000000a", "+", "++34600000001", "34+600000001",
            "600-000-001", "+ 6000000"})
        @DisplayName("TC12C-17 Phones of 8 or 16 digits, with letters or other signs are rejected")
        void invalidPhone(String phone) {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, PHONE, phone);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(BAD_PHONE, Screen.status(screen)),
                    () -> assertEquals(List.of(PHONE), markedFields(screen)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-18 Every error comes together in the order of the server, and the form keeps what was typed")
        void everyErrorTogether() {
            // Arrange
            IU_ManagePatients screen = open();
            type(screen, BIRTH, LocalDate.now().plusYears(1).format(DATE));
            type(screen, EMAIL, "wrong");
            type(screen, PHONE, "123");
            type(screen, ADDRESS, "Kept");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The identifier is required. The full name is required. The date of birth"
                            + " cannot be in the future. The sex is required. The e-mail must have the form"
                            + " name@domain.ext. The phone must have 9 to 15 digits.", Screen.status(screen)),
                    () -> assertEquals(List.of(IDENTIFIER, FULL_NAME, BIRTH, SEX, PHONE, EMAIL),
                            markedFields(screen)),
                    () -> assertEquals("wrong", text(screen, EMAIL)),
                    () -> assertEquals("123", text(screen, PHONE)),
                    () -> assertEquals("Kept", text(screen, ADDRESS)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-19 The red borders go away when the form is saved right, and with New patient")
        void marksAreCleared() {
            // Arrange
            IU_ManagePatients screen = open();
            save(screen);
            List<String> afterEmpty = markedFields(screen);

            // Act
            newPatient(screen);
            List<String> afterNew = markedFields(screen);
            type(screen, PHONE, "1");
            save(screen);
            fillValid(screen);
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(4, afterEmpty.size()),
                    () -> assertTrue(afterNew.isEmpty()),
                    () -> assertTrue(markedFields(screen).isEmpty()),
                    () -> assertEquals("✔ Patient P-0004 registered.", Screen.status(screen)));
        }
    }

    @Nested
    @DisplayName("5. Registration and modification")
    class Saving {
        @Test
        @DisplayName("TC12C-20 A valid new patient is registered: message, table reloaded, patient selected, search emptied")
        void validRegistration() {
            // Arrange
            IU_ManagePatients screen = open();
            search(screen, "mario");
            newPatient(screen);
            fillValid(screen);
            type(screen, FULL_NAME, "  Ana Nueva Inventada  ");
            type(screen, ADDRESS, "  Calle Nueva 4, 13001 Ciudad Real ");

            // Act
            save(screen);

            // Assert
            Patient sent = service.registrations().get(0);
            assertAll(
                    () -> assertNull(sent.getPatientId()),
                    () -> assertEquals("HC-0000004", sent.getIdentifier()),
                    () -> assertEquals("87654321X", sent.getNationalId()),
                    () -> assertEquals("Ana Nueva Inventada", sent.getFullName()),
                    () -> assertEquals(LocalDate.of(1990, 2, 1), sent.getDateOfBirth()),
                    () -> assertEquals(Sex.FEMALE, sent.getSex()),
                    () -> assertEquals("Calle Nueva 4, 13001 Ciudad Real", sent.getAddress()),
                    () -> assertEquals("600000004", sent.getPhone()),
                    () -> assertEquals("ana.nueva@example.test", sent.getEmail()),
                    () -> assertEquals("✔ Patient P-0004 registered.", Screen.status(screen)),
                    () -> assertEquals(GREEN, Screen.statusLabel(screen).getForeground()),
                    () -> assertEquals(List.of("", "mario", ""), service.searches()),
                    () -> assertEquals("", text(screen, SEARCH)),
                    () -> assertEquals(4, Screen.table(screen).getRowCount()),
                    () -> assertEquals("4 patients found.", Screen.found(screen)),
                    () -> assertEquals(3, selectedRow(screen)),
                    () -> assertEquals("HC-0000004", text(screen, IDENTIFIER)),
                    () -> assertFalse(field(screen, IDENTIFIER).isEnabled()),
                    () -> assertTrue(questions.isEmpty()));
        }

        @Test
        @DisplayName("TC12C-20 After a registration the form is clean: New patient asks nothing")
        void afterRegistrationTheFormIsClean() {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            save(screen);

            // Act
            newPatient(screen);

            // Assert
            assertAll(
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertEquals(-1, selectedRow(screen)));
        }

        @Test
        @DisplayName("TC12C-21 A valid change of the selected patient is saved and the same patient stays selected")
        void validModification() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, MARIO);
            type(screen, PHONE, "+34 600 111 222");
            type(screen, NATIONAL_ID, "");
            sex(screen).setSelectedItem(Sex.UNSPECIFIED);

            // Act
            save(screen);

            // Assert
            Patient sent = service.modifications().get(0);
            assertAll(
                    () -> assertEquals(List.of("P-0002"), service.modifiedIds()),
                    () -> assertEquals("HC-0000002", sent.getIdentifier()),
                    () -> assertEquals("+34 600 111 222", sent.getPhone()),
                    () -> assertEquals("", sent.getNationalId()),
                    () -> assertEquals(Sex.UNSPECIFIED, sent.getSex()),
                    () -> assertTrue(service.registrations().isEmpty()),
                    () -> assertEquals("✔ Patient P-0002 saved.", Screen.status(screen)),
                    () -> assertEquals(GREEN, Screen.statusLabel(screen).getForeground()),
                    () -> assertEquals(MARIO, selectedRow(screen)),
                    () -> assertEquals("+34 600 111 222", text(screen, PHONE)),
                    () -> assertFalse(field(screen, IDENTIFIER).isEnabled()),
                    () -> assertTrue(questions.isEmpty()));
        }

        @Test
        @DisplayName("TC12C-21 A registered patient can be modified next: Patient P-0004 saved")
        void registerThenModify() {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            save(screen);

            // Act
            type(screen, ADDRESS, "Calle Cambiada 5");
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("P-0004"), service.modifiedIds()),
                    () -> assertEquals("✔ Patient P-0004 saved.", Screen.status(screen)),
                    () -> assertEquals("Calle Cambiada 5", service.stored().get(3).getAddress()));
        }

        @Test
        @DisplayName("TC12C-22 An invalid change is not sent; the patient stays selected with what was typed")
        void invalidModification() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, ELENA);
            type(screen, EMAIL, "elena.example.test");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(BAD_EMAIL, Screen.status(screen)),
                    () -> assertEquals(0, service.changes()),
                    () -> assertEquals(ELENA, selectedRow(screen)),
                    () -> assertEquals("elena.example.test", text(screen, EMAIL)));
        }
    }

    @Nested
    @DisplayName("6. Duplicate")
    class Duplicate {
        @Test
        @DisplayName("TC12C-23 An identifier that exists says so and loads the existing patient, with the identifier locked")
        void duplicateLoadsTheExistingPatient() {
            // Arrange
            IU_ManagePatients screen = open();
            search(screen, "elena");
            newPatient(screen);
            fillValid(screen);
            type(screen, IDENTIFIER, " hc-0000002 ");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(DUPLICATE, Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()),
                    () -> assertEquals(1, service.registrations().size()),
                    () -> assertEquals(3, service.stored().size()),
                    () -> assertEquals("", text(screen, SEARCH)),
                    () -> assertEquals(3, Screen.table(screen).getRowCount()),
                    () -> assertEquals(MARIO, selectedRow(screen)),
                    () -> assertEquals(List.of("HC-0000002", "12345678Z", "Mario Demo Simulado", "02/11/1975",
                            "Avenida de Muestra 22, 45001 Toledo", "600000002", "mario.demo@example.test", "MALE"),
                            form(screen)),
                    () -> assertFalse(field(screen, IDENTIFIER).isEnabled()),
                    () -> assertTrue(markedFields(screen).isEmpty()),
                    () -> assertTrue(questions.isEmpty()));
        }

        @Test
        @DisplayName("TC12C-23 After the duplicate, Save modifies the existing patient")
        void saveAfterTheDuplicateModifies() {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, IDENTIFIER, "HC-0000001");
            save(screen);

            // Act
            type(screen, PHONE, "699999999");
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("P-0001"), service.modifiedIds()),
                    () -> assertEquals("✔ Patient P-0001 saved.", Screen.status(screen)));
        }
    }

    @Nested
    @DisplayName("7. Errors of the server")
    class ServerErrors {
        @Test
        @DisplayName("TC12C-24 The errors of the server are shown together, with no field marked, and the form is kept")
        void serverErrorsOnModification() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);
            type(screen, FULL_NAME, "Lucía Cambiada");
            service.rejectNextChange("The identifier cannot be changed", "The phone must have 9 to 15 digits");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The identifier cannot be changed. The phone must have 9 to 15 digits.",
                            Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()),
                    () -> assertTrue(markedFields(screen).isEmpty()),
                    () -> assertEquals(LUCIA, selectedRow(screen)),
                    () -> assertEquals("Lucía Cambiada", text(screen, FULL_NAME)),
                    () -> assertEquals("Lucía Ejemplo Prueba", service.stored().get(0).getFullName()));
        }

        @Test
        @DisplayName("TC12C-24 An error of the server on a registration is shown and nothing is registered")
        void serverErrorOnRegistration() {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            service.rejectNextChange("The patient is required");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The patient is required.", Screen.status(screen)),
                    () -> assertEquals(3, service.stored().size()),
                    () -> assertEquals("HC-0000004", text(screen, IDENTIFIER)),
                    () -> assertEquals(-1, selectedRow(screen)));
        }
    }

    @Nested
    @DisplayName("8. Denied access")
    class Denied {
        private void assertOnlyBackEnabled(IU_ManagePatients screen) {
            List<Boolean> others = new ArrayList<>();
            for (String label : List.of(SEARCH, IDENTIFIER, NATIONAL_ID, FULL_NAME, BIRTH, SEX, ADDRESS, PHONE,
                    EMAIL)) {
                others.add(Screen.labelled(screen, label).isEnabled());
            }
            assertAll(
                    () -> assertEquals(DENIED, Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()),
                    () -> assertFalse(Screen.enabled(screen, SEARCH)),
                    () -> assertFalse(Screen.enabled(screen, "New patient")),
                    () -> assertFalse(Screen.enabled(screen, "Save")),
                    () -> assertFalse(Screen.table(screen).isEnabled()),
                    () -> assertFalse(others.contains(true)),
                    () -> assertTrue(Screen.enabled(screen, "Back")));
        }

        @Test
        @DisplayName("TC12C-25 When the server denies the opening, the table is empty and only Back is enabled")
        void deniedWhenOpening() {
            // Arrange
            service.denyAll();

            // Act
            IU_ManagePatients screen = open();
            Screen.button(screen, "Back").doClick();

            // Assert
            assertAll(
                    () -> assertOnlyBackEnabled(screen),
                    () -> assertEquals(0, Screen.table(screen).getRowCount()),
                    () -> assertEquals(" ", Screen.found(screen)),
                    () -> assertEquals(1, backs));
        }

        @Test
        @DisplayName("TC12C-26 The permission revoked while the screen is open: Save gives the message and only Back")
        void deniedWhenSaving() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, MARIO);
            service.denyAll();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertOnlyBackEnabled(screen),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-26 The permission revoked while the screen is open: Search gives the message and only Back")
        void deniedWhenSearching() {
            // Arrange
            IU_ManagePatients screen = open();
            service.denyAll();

            // Act
            search(screen, "mario");

            // Assert
            assertAll(
                    () -> assertOnlyBackEnabled(screen),
                    () -> assertEquals("3 patients found.", Screen.found(screen)));
        }
    }

    @Nested
    @DisplayName("9. Unsaved changes")
    class UnsavedChanges {
        private void change(IU_ManagePatients screen, String label) {
            if (SEX.equals(label)) {
                sex(screen).setSelectedItem(Sex.MALE);
            } else {
                type(screen, label, text(screen, label) + "9");
            }
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {IDENTIFIER, NATIONAL_ID, FULL_NAME, BIRTH, SEX, ADDRESS, PHONE, EMAIL})
        @DisplayName("TC12C-27 A change in any field of the shown patient asks before another row is shown; accept")
        void anotherRowAccept(String label) {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);
            change(screen, label);

            // Act
            select(screen, ELENA);

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(ELENA, selectedRow(screen)),
                    () -> assertEquals("HC-0000003", text(screen, IDENTIFIER)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-28 Cancel on another row: the selection goes back and the changes are kept")
        void anotherRowCancel() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);
            type(screen, FULL_NAME, "Changed");
            answer = false;

            // Act
            select(screen, MARIO);

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(LUCIA, selectedRow(screen)),
                    () -> assertEquals("Changed", text(screen, FULL_NAME)),
                    () -> assertEquals("HC-0000001", text(screen, IDENTIFIER)));
        }

        @Test
        @DisplayName("TC12C-28 Cancel on New patient: nothing changes")
        void newPatientCancel() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, MARIO);
            type(screen, PHONE, "611111111");
            answer = false;

            // Act
            newPatient(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(MARIO, selectedRow(screen)),
                    () -> assertEquals("611111111", text(screen, PHONE)));
        }

        @Test
        @DisplayName("TC12C-28 Cancel on Search (button or Enter): nothing is searched and nothing changes")
        void searchCancel() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, MARIO);
            type(screen, EMAIL, "changed@example.test");
            answer = false;

            // Act
            search(screen, "elena");
            field(screen, SEARCH).postActionEvent();

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD, DISCARD), questions),
                    () -> assertEquals(List.of(""), service.searches()),
                    () -> assertEquals(3, Screen.table(screen).getRowCount()),
                    () -> assertEquals(MARIO, selectedRow(screen)),
                    () -> assertEquals("changed@example.test", text(screen, EMAIL)));
        }

        @Test
        @DisplayName("TC12C-29 Accept on New patient empties the form and nothing is sent")
        void newPatientAccept() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, MARIO);
            type(screen, PHONE, "611111111");

            // Act
            newPatient(screen);

            // Assert
            assertAll(
                    () -> assertFalse(questions.isEmpty()),
                    () -> assertTrue(questions.stream().allMatch(DISCARD::equals)),
                    () -> assertEquals(-1, selectedRow(screen)),
                    () -> assertEquals(List.of("", "", "", "", "", "", "", "null"), form(screen)),
                    () -> assertTrue(field(screen, IDENTIFIER).isEnabled()),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-29 Accept on Search asks once and searches")
        void searchAccept() {
            // Arrange
            IU_ManagePatients screen = open();
            type(screen, FULL_NAME, "Someone");

            // Act
            search(screen, "elena");

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(List.of("", "elena"), service.searches()),
                    () -> assertEquals(List.of("HC-0000003"), Screen.column(Screen.table(screen), 0)),
                    () -> assertEquals("", text(screen, FULL_NAME)));
        }

        @Test
        @DisplayName("TC12C-30 No question when the form is clean: rows, New patient and Search")
        void noQuestionWhenClean() {
            // Arrange
            IU_ManagePatients screen = open();

            // Act
            newPatient(screen);
            select(screen, LUCIA);
            select(screen, MARIO);
            newPatient(screen);
            search(screen, "lucía");

            // Assert
            assertAll(
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertEquals(List.of("", "lucía"), service.searches()));
        }

        @Test
        @DisplayName("TC12C-30 Spaces at the ends and a change undone by hand are not changes")
        void spacesAndUndoneChangesAreNotChanges() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);
            type(screen, FULL_NAME, "  Lucía Ejemplo Prueba  ");
            type(screen, PHONE, "699999999");
            type(screen, PHONE, "600000001");
            sex(screen).setSelectedItem(Sex.MALE);
            sex(screen).setSelectedItem(Sex.FEMALE);

            // Act
            select(screen, MARIO);
            int afterRow = selectedRow(screen);
            newPatient(screen);
            type(screen, ADDRESS, "   ");
            search(screen, "");

            // Assert
            assertAll(
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertEquals(MARIO, afterRow),
                    () -> assertEquals(2, service.searches().size()));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {IDENTIFIER, NATIONAL_ID, FULL_NAME, BIRTH, SEX, ADDRESS, PHONE, EMAIL})
        @DisplayName("TC12C-31 A form of a new patient with any data asks before a row is shown; cancel keeps it")
        void newPatientFormCancel(String label) {
            // Arrange
            IU_ManagePatients screen = open();
            change(screen, label);
            List<String> typed = form(screen);
            answer = false;

            // Act
            select(screen, MARIO);

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(-1, selectedRow(screen)),
                    () -> assertEquals(typed, form(screen)),
                    () -> assertTrue(field(screen, IDENTIFIER).isEnabled()));
        }

        @Test
        @DisplayName("TC12C-31 A form of a new patient with data, accept: the row is shown")
        void newPatientFormAccept() {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);

            // Act
            select(screen, ELENA);

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals("HC-0000003", text(screen, IDENTIFIER)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-32 While the selection is being dragged nothing changes; the form follows when it is released")
        void draggingTheSelection() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);
            type(screen, ADDRESS, "Changed");
            javax.swing.ListSelectionModel selection = Screen.table(screen).getSelectionModel();
            Consumer<Integer> drag = row -> Screen.table(screen).setRowSelectionInterval(row, row);

            // Act
            selection.setValueIsAdjusting(true);
            drag.accept(MARIO);
            drag.accept(ELENA);
            List<String> whileDragging = new ArrayList<>(questions);
            String addressWhileDragging = text(screen, ADDRESS);
            selection.setValueIsAdjusting(false);

            // Assert
            assertAll(
                    () -> assertTrue(whileDragging.isEmpty()),
                    () -> assertEquals("Changed", addressWhileDragging),
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals("HC-0000003", text(screen, IDENTIFIER)));
        }
    }

    @Nested
    @DisplayName("10. Fixes after the findings of #122")
    class FindingsFixes {
        @Test
        @DisplayName("TC12C-36 New patient with unsaved changes asks once; cancel keeps the changes and the patient")
        void newPatientAsksOnceCancel() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, MARIO);
            type(screen, ADDRESS, "Calle Cambiada 9");
            answer = false;

            // Act
            newPatient(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(MARIO, selectedRow(screen)),
                    () -> assertEquals("HC-0000002", text(screen, IDENTIFIER)),
                    () -> assertEquals("Calle Cambiada 9", text(screen, ADDRESS)),
                    () -> assertFalse(field(screen, IDENTIFIER).isEnabled()));
        }

        @Test
        @DisplayName("TC12C-36 New patient with unsaved changes asks once; accept empties the form")
        void newPatientAsksOnceAccept() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, MARIO);
            type(screen, ADDRESS, "Calle Cambiada 9");

            // Act
            newPatient(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(-1, selectedRow(screen)),
                    () -> assertEquals(List.of("", "", "", "", "", "", "", "null"), form(screen)),
                    () -> assertTrue(field(screen, IDENTIFIER).isEnabled()),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-37 Dragging the selection away and back to the same row keeps the changes, without asking")
        void dragAwayAndBackKeepsChanges() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);
            type(screen, FULL_NAME, "Lucía Cambiada");
            javax.swing.ListSelectionModel selection = Screen.table(screen).getSelectionModel();

            // Act
            selection.setValueIsAdjusting(true);
            select(screen, MARIO);
            select(screen, ELENA);
            select(screen, LUCIA);
            selection.setValueIsAdjusting(false);

            // Assert
            assertAll(
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertEquals(LUCIA, selectedRow(screen)),
                    () -> assertEquals("HC-0000001", text(screen, IDENTIFIER)),
                    () -> assertEquals("Lucía Cambiada", text(screen, FULL_NAME)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-37 Another row, cancel, and the same row again: one question and the changes are kept")
        void anotherRowCancelThenSameRowKeepsChanges() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);
            type(screen, PHONE, "699999999");
            answer = false;

            // Act
            select(screen, ELENA);
            select(screen, LUCIA);

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(LUCIA, selectedRow(screen)),
                    () -> assertEquals("699999999", text(screen, PHONE)),
                    () -> assertEquals("HC-0000001", text(screen, IDENTIFIER)));
        }

        @Test
        @DisplayName("TC12C-38 Back with unsaved changes asks once; cancel does not go back and keeps the changes")
        void backWithChangesCancel() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, ELENA);
            type(screen, EMAIL, "elena.cambiada@example.test");
            answer = false;

            // Act
            Screen.button(screen, "Back").doClick();

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(0, backs),
                    () -> assertEquals(ELENA, selectedRow(screen)),
                    () -> assertEquals("elena.cambiada@example.test", text(screen, EMAIL)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-38 Back with unsaved changes asks once; accept goes back and sends nothing")
        void backWithChangesAccept() {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);

            // Act
            Screen.button(screen, "Back").doClick();

            // Assert
            assertAll(
                    () -> assertEquals(List.of(DISCARD), questions),
                    () -> assertEquals(1, backs),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-38 Back without changes goes back without asking")
        void backWithoutChanges() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);

            // Act
            Screen.button(screen, "Back").doClick();

            // Assert
            assertAll(
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertEquals(1, backs));
        }

        @Test
        @DisplayName("TC12C-39 The message area is at the centre of the footer and a long error keeps its whole plain text")
        void longErrorInTheCentreOfTheFooter() {
            // Arrange
            IU_ManagePatients screen = open();
            JLabel status = Screen.statusLabel(screen);
            java.awt.Container footer = status.getParent();
            screen.showOperationResultError("Short.");
            Dimension shortSize = status.getPreferredSize();
            String longError = "The identifier is required. The full name is required. The date of birth is"
                    + " required. The sex is required. The e-mail must have the form name@domain.ext. The phone must"
                    + " have 9 to 15 digits.";

            // Act
            screen.showOperationResultError(longError);
            Dimension longSize = status.getPreferredSize();

            // Assert
            assertAll(
                    () -> assertTrue(footer.getLayout() instanceof BorderLayout),
                    () -> assertEquals(BorderLayout.CENTER,
                            ((BorderLayout) footer.getLayout()).getConstraints(status)),
                    () -> assertEquals(BorderLayout.EAST, ((BorderLayout) footer.getLayout())
                            .getConstraints(Screen.button(screen, "Back"))),
                    () -> assertEquals("⚠ " + longError, Screen.status(screen)),
                    () -> assertFalse(Screen.status(screen).contains("<html")),
                    () -> assertEquals(shortSize.width, longSize.width),
                    () -> assertTrue(longSize.height > shortSize.height));
        }

        @Test
        @DisplayName("TC12C-40 Alt+H is only the Search button: the label Search and the other labels have no Alt+H")
        void onlyOneAltH() {
            // Act
            IU_ManagePatients screen = open();

            // Assert
            assertAll(
                    () -> assertEquals(KeyEvent.VK_H, Screen.button(screen, SEARCH).getMnemonic()),
                    () -> assertEquals(0, Screen.labelOf(screen, SEARCH).getDisplayedMnemonic()),
                    () -> assertEquals(1, Screen.all(screen, javax.swing.AbstractButton.class).stream()
                            .filter(button -> button.getMnemonic() == KeyEvent.VK_H).count()),
                    () -> assertTrue(Screen.all(screen, JLabel.class).stream()
                            .noneMatch(label -> label.getDisplayedMnemonic() == KeyEvent.VK_H)));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"ana@example.test.", "ana@example..test"})
        @DisplayName("TC12C-41 E-mails with an empty part after a dot are rejected and nothing is sent")
        void emailWithAnEmptyPart(String email) {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, EMAIL, email);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(BAD_EMAIL, Screen.status(screen)),
                    () -> assertEquals(List.of(EMAIL), markedFields(screen)),
                    () -> assertEquals(0, service.changes()));
        }

        @Test
        @DisplayName("TC12C-42 The date of birth 01/01/1900 is accepted")
        void earliestBirthIsAccepted() {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, BIRTH, "01/01/1900");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(1, service.registrations().size()),
                    () -> assertEquals(LocalDate.of(1900, 1, 1), service.registrations().get(0).getDateOfBirth()),
                    () -> assertEquals("✔ Patient P-0004 registered.", Screen.status(screen)));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"31/12/1899", "01/01/1899"})
        @DisplayName("TC12C-42 A date of birth before 01/01/1900 is rejected and nothing is sent")
        void birthBefore1900IsRejected(String date) {
            // Arrange
            IU_ManagePatients screen = open();
            fillValid(screen);
            type(screen, BIRTH, date);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The date of birth cannot be before 01/01/1900.", Screen.status(screen)),
                    () -> assertEquals(List.of(BIRTH), markedFields(screen)),
                    () -> assertEquals(0, service.changes()));
        }
    }

    @Nested
    @DisplayName("Public methods of the design")
    class DesignMethods {
        @Test
        @DisplayName("TC12C-01 displayPatients shows the given patients with none selected; onSearch asks the server")
        void displayAndSearch() {
            // Arrange
            IU_ManagePatients screen = open();
            select(screen, LUCIA);

            // Act
            screen.displayPatients(List.of(service.stored().get(2)));
            List<String> shown = Screen.column(Screen.table(screen), 0);
            int selected = selectedRow(screen);
            screen.onSearch("HC-0000001");

            // Assert
            assertAll(
                    () -> assertEquals(List.of("HC-0000003"), shown),
                    () -> assertEquals(-1, selected),
                    () -> assertEquals(List.of("HC-0000001"), Screen.column(Screen.table(screen), 0)),
                    () -> assertEquals(List.of("", "HC-0000001"), service.searches()));
        }

        @Test
        @DisplayName("TC12C-24 The messages: success in green with ✔, errors in red with ⚠ and each one with a full stop")
        void messages() {
            // Arrange
            IU_ManagePatients screen = open();

            // Act
            screen.showOperationResultSuccess("Done.");
            String success = Screen.status(screen);
            Color successColour = Screen.statusLabel(screen).getForeground();
            screen.showValidationErrors(List.of("One", "Two"));

            // Assert
            assertAll(
                    () -> assertEquals("✔ Done.", success),
                    () -> assertEquals(GREEN, successColour),
                    () -> assertEquals("⚠ One. Two.", Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()));
        }
    }
}
