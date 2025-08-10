import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

class Student {
    String name;
    int rollNumber;
    // mark = -1 means Absent
    Map<String, Integer> subjectMarks = new LinkedHashMap<>();
    float average;
    char grade;

    public Student(String name, int rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
    }

    // Calculate average and grade using globalSubjects as denominator when > 0
    public void calculateAverageAndGrade(int globalSubjects) {
        int sum = 0;
        int denominator;

        if (globalSubjects > 0) {
            denominator = globalSubjects;
            for (Integer mark : subjectMarks.values()) {
                if (mark != null && mark >= 0) sum += mark;
            }
            // missing subjects count as absent (0)
        } else {
            denominator = (subjectMarks.size() > 0) ? subjectMarks.size() : 1;
            for (Integer mark : subjectMarks.values()) {
                if (mark != null && mark >= 0) sum += mark;
            }
        }

        if (denominator <= 0) average = 0f;
        else average = (float) sum / denominator;

        if (average >= 90) grade = 'A';
        else if (average >= 80) grade = 'B';
        else if (average >= 70) grade = 'C';
        else if (average >= 60) grade = 'D';
        else grade = 'F';
    }
}

public class StudentGradeManagerGUI extends JFrame {
    private final List<Student> students = new ArrayList<>();
    private JTable table;
    private DefaultTableModel tableModel;

    // Default global subject list
    private List<String> subjectNames = new ArrayList<>(Arrays.asList(
            "Tamil", "English", "Maths", "Science", "Social"
    ));

    public StudentGradeManagerGUI() {
        setTitle("Student Grade Manager");
        setSize(980, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Menu Bar
        JMenuBar menuBar = new JMenuBar();

        JMenu adminMenu = new JMenu("Admin");
        JMenuItem manageSubjectsItem = new JMenuItem("Manage Subjects");
        manageSubjectsItem.addActionListener(e -> manageSubjectsDialog());
        adminMenu.add(manageSubjectsItem);
        menuBar.add(adminMenu);

        JMenu viewMenu = new JMenu("View");
        JMenuItem displayMenuItem = new JMenuItem("Display All Students");
        displayMenuItem.addActionListener(e -> refreshTable());
        viewMenu.add(displayMenuItem);

        JMenu searchMenu = new JMenu("Search");
        JMenuItem searchByRollItem = new JMenuItem("Search by Roll Number");
        searchByRollItem.addActionListener(e -> searchByRollDialog());
        JMenuItem searchByNameItem = new JMenuItem("Search by Name");
        searchByNameItem.addActionListener(e -> searchByNameDialog());
        searchMenu.add(searchByRollItem);
        searchMenu.add(searchByNameItem);

        menuBar.add(viewMenu);
        menuBar.add(searchMenu);
        setJMenuBar(menuBar);

        // Top panel with buttons
        JPanel topPanel = new JPanel();
        JButton addBtn = new JButton("Add Student");
        JButton updateBtn = new JButton("Update Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton saveBtn = new JButton("Save to CSV");
        JButton loadBtn = new JButton("Load from CSV");
        JButton top3Btn = new JButton("Show Top 3");
        JButton failedBtn = new JButton("Show Failed");
        JButton studentLoginBtn = new JButton("Student Login");

        topPanel.add(addBtn);
        topPanel.add(updateBtn);
        topPanel.add(deleteBtn);
        topPanel.add(saveBtn);
        topPanel.add(loadBtn);
        topPanel.add(top3Btn);
        topPanel.add(failedBtn);
        topPanel.add(studentLoginBtn);

        add(topPanel, BorderLayout.NORTH);

        // Table Model & JTable
        tableModel = new DefaultTableModel(new String[] {
                "Roll Number", "Name", "Subjects & Marks", "Average", "Grade"
        }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Button Actions
        addBtn.addActionListener(e -> showAddStudentDialog());
        updateBtn.addActionListener(e -> showUpdateStudentDialog());
        deleteBtn.addActionListener(e -> deleteSelectedStudent());
        saveBtn.addActionListener(e -> saveToCSV());
        loadBtn.addActionListener(e -> loadFromCSV());
        top3Btn.addActionListener(e -> showTop3Students());
        failedBtn.addActionListener(e -> showFailedStudents());
        studentLoginBtn.addActionListener(e -> showLoginDialog());

        // NOTE: removed testAddSampleStudent() so no default student is added on startup

        refreshTable();
    }

    // Manage global subject list (add/remove). Maintains order.
    private void manageSubjectsDialog() {
        JDialog dialog = new JDialog(this, "Manage Subjects (Global)", true);
        dialog.setSize(520, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String s : subjectNames) listModel.addElement(s);

        JList<String> subjList = new JList<>(listModel);
        subjList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dialog.add(new JScrollPane(subjList), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton addBtn = new JButton("Add Subject");
        JButton removeBtn = new JButton("Remove Selected");
        JButton doneBtn = new JButton("Done");

        bottom.add(addBtn);
        bottom.add(removeBtn);
        bottom.add(doneBtn);
        dialog.add(bottom, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(dialog, "Enter new subject name:");
            if (name == null) return;
            name = name.trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Subject name cannot be empty.");
                return;
            }
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).equalsIgnoreCase(name)) {
                    JOptionPane.showMessageDialog(dialog, "Subject already exists.");
                    return;
                }
            }
            listModel.addElement(name);
        });

        removeBtn.addActionListener(e -> {
            int idx = subjList.getSelectedIndex();
            if (idx == -1) {
                JOptionPane.showMessageDialog(dialog, "Select a subject to remove.");
                return;
            }
            String subjToRemove = listModel.get(idx);
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Remove subject '" + subjToRemove + "' from global list? This will remove this subject from all students.",
                    "Confirm Remove", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            listModel.remove(idx);

            // Remove from all students
            for (Student s : students) {
                s.subjectMarks.remove(subjToRemove);
            }
        });

        doneBtn.addActionListener(e -> {
            List<String> newNames = new ArrayList<>();
            for (int i = 0; i < listModel.size(); i++) newNames.add(listModel.get(i));
            subjectNames = newNames;

            // Align each student's marks to new subject order and fill missing with Absent
            for (Student s : students) {
                Map<String, Integer> newMap = new LinkedHashMap<>();
                for (String subj : subjectNames) {
                    Integer val = s.subjectMarks.containsKey(subj) ? s.subjectMarks.get(subj) : -1;
                    newMap.put(subj, val);
                }
                s.subjectMarks = newMap;
                s.calculateAverageAndGrade(subjectNames.size());
            }

            refreshTable();
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    // Always show subjects in global order. Sort students A->Z by name.
    private void refreshTable() {
        students.sort((s1, s2) -> s1.name.compareToIgnoreCase(s2.name));
        tableModel.setRowCount(0);

        for (Student s : students) {
            StringBuilder subjMarks = new StringBuilder();
            for (String subj : subjectNames) {
                Integer val = s.subjectMarks.get(subj);
                String value = (val == null || val == -1) ? "Absent" : String.valueOf(val);
                subjMarks.append(subj).append(": ").append(value).append("; ");
            }
            String subjMarksStr = subjMarks.length() > 2 ? subjMarks.substring(0, subjMarks.length() - 2) : subjMarks.toString();
            tableModel.addRow(new Object[] { s.rollNumber, s.name, subjMarksStr, String.format("%.2f", s.average), s.grade });
        }
    }

    private void showAddStudentDialog() {
        JDialog dialog = new JDialog(this, "Add Student", true);
        dialog.setSize(520, 650);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        JTextField nameField = new JTextField();
        JTextField rollField = new JTextField();

        panel.add(new JLabel("Student Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Roll Number:"));
        panel.add(rollField);

        List<JTextField> markFields = new ArrayList<>();
        panel.add(new JLabel("Enter marks for these subjects (leave empty for Absent):"));

        for (String subj : subjectNames) {
            panel.add(new JLabel(subj + ":"));
            JTextField f = new JTextField();
            markFields.add(f);
            panel.add(f);
        }

        JButton saveBtn = new JButton("Save");
        panel.add(saveBtn);
        dialog.add(new JScrollPane(panel), BorderLayout.CENTER);

        saveBtn.addActionListener(ev -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Name cannot be empty."); return; }
            int roll;
            try { roll = Integer.parseInt(rollField.getText().trim()); }
            catch (NumberFormatException ex) { JOptionPane.showMessageDialog(dialog, "Invalid roll number."); return; }
            if (findStudentByRoll(roll) != null) { JOptionPane.showMessageDialog(dialog, "Roll number already exists."); return; }

            Map<String, Integer> subjMarks = new LinkedHashMap<>();
            for (int i = 0; i < subjectNames.size(); i++) {
                String subj = subjectNames.get(i);
                String markStr = markFields.get(i).getText().trim();
                if (markStr.isEmpty()) subjMarks.put(subj, -1);
                else {
                    try {
                        int m = Integer.parseInt(markStr);
                        if (m < 0 || m > 100) { JOptionPane.showMessageDialog(dialog, "Mark must be 0-100 for " + subj); return; }
                        subjMarks.put(subj, m);
                    } catch (NumberFormatException ex) {
                        subjMarks.put(subj, -1);
                    }
                }
            }

            Student student = new Student(name, roll);
            student.subjectMarks = subjMarks;
            student.calculateAverageAndGrade(subjectNames.size());
            students.add(student);
            refreshTable();
            JOptionPane.showMessageDialog(dialog, "Student added successfully!");
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private void showUpdateStudentDialog() {
        int sel = table.getSelectedRow();
        if (sel == -1) { JOptionPane.showMessageDialog(this, "Please select a student to update."); return; }
        Student s = students.get(sel);

        JDialog dialog = new JDialog(this, "Update Student", true);
        dialog.setSize(520, 650);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(0,1,6,6));
        JTextField nameField = new JTextField(s.name);
        panel.add(new JLabel("Student Name:"));
        panel.add(nameField);

        List<JTextField> markFields = new ArrayList<>();
        for (String subj : subjectNames) {
            panel.add(new JLabel(subj + ":"));
            Integer val = s.subjectMarks.get(subj);
            JTextField f = new JTextField((val == null || val == -1) ? "" : String.valueOf(val));
            markFields.add(f);
            panel.add(f);
        }

        JButton saveBtn = new JButton("Save");
        panel.add(saveBtn);
        dialog.add(new JScrollPane(panel), BorderLayout.CENTER);

        saveBtn.addActionListener(ev -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Name cannot be empty."); return; }
            s.name = newName;

            Map<String, Integer> newMarks = new LinkedHashMap<>();
            for (int i = 0; i < subjectNames.size(); i++) {
                String subj = subjectNames.get(i);
                String markStr = markFields.get(i).getText().trim();
                if (markStr.isEmpty()) newMarks.put(subj, -1);
                else {
                    try {
                        int m = Integer.parseInt(markStr);
                        if (m < 0 || m > 100) { JOptionPane.showMessageDialog(dialog, "Mark must be 0-100 for " + subj); return; }
                        newMarks.put(subj, m);
                    } catch (NumberFormatException ex) {
                        newMarks.put(subj, -1);
                    }
                }
            }

            s.subjectMarks = newMarks;
            s.calculateAverageAndGrade(subjectNames.size());
            refreshTable();
            JOptionPane.showMessageDialog(dialog, "Student updated successfully.");
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private void deleteSelectedStudent() {
        int sel = table.getSelectedRow();
        if (sel == -1) { JOptionPane.showMessageDialog(this, "Select a student to delete."); return; }
        Student s = students.get(sel);
        int c = JOptionPane.showConfirmDialog(this, "Delete " + s.name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            students.remove(sel);
            refreshTable();
        }
    }

    private void saveToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) return;
        File file = fileChooser.getSelectedFile();
        if (file.exists()) {
            int c = JOptionPane.showConfirmDialog(this, "Overwrite file?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("#SUBJECTNAMES=" + String.join(";;", subjectNames));
            for (Student s : students) {
                StringBuilder sb = new StringBuilder();
                String safeName = s.name.contains(",") ? "\"" + s.name + "\"" : s.name;
                sb.append(safeName).append(",").append(s.rollNumber);
                for (String subj : subjectNames) {
                    Integer val = s.subjectMarks.get(subj);
                    String markStr = (val == null || val == -1) ? "Absent" : String.valueOf(val);
                    sb.append(",").append(subj).append(":").append(markStr);
                }
                pw.println(sb.toString());
            }
            JOptionPane.showMessageDialog(this, "Saved successfully.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage());
        }
    }

    private void loadFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) return;
        File file = fileChooser.getSelectedFile();
        if (!file.exists()) { JOptionPane.showMessageDialog(this, "File not found."); return; }

        String[] modes = {"Append", "Overwrite"};
        int choice = JOptionPane.showOptionDialog(this, "Load mode:", "Load", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, modes, modes[0]);
        boolean overwrite = choice == 1;
        if (overwrite) students.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            // Read possible header
            br.mark(8192);
            String first = br.readLine();
            if (first != null && first.startsWith("#SUBJECTNAMES=")) {
                String namesStr = first.substring("#SUBJECTNAMES=".length());
                String[] arr = namesStr.split(";;", -1);
                List<String> newNames = new ArrayList<>();
                for (String nm : arr) if (!nm.trim().isEmpty()) newNames.add(nm);
                if (!newNames.isEmpty()) subjectNames = newNames;
            } else {
                br.reset();
            }

            int loaded = 0, skipped = 0, dup = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = splitCSVLine(line);
                if (parts.length < 2) { skipped++; continue; }

                String name = parts[0].trim();
                if (name.startsWith("\"") && name.endsWith("\"")) name = name.substring(1, name.length()-1);
                if (name.isEmpty()) { skipped++; continue; }

                int roll;
                try { roll = Integer.parseInt(parts[1].trim()); }
                catch (NumberFormatException ex) { skipped++; continue; }

                if (findStudentByRoll(roll) != null) { dup++; continue; }

                Student s = new Student(name, roll);
                boolean ok = true;
                for (int i = 2; i < parts.length; i++) {
                    String[] sm = parts[i].split(":", 2);
                    if (sm.length != 2) { ok = false; break; }
                    String subj = sm[0].trim();
                    String markPart = sm[1].trim();
                    if (subj.isEmpty()) { ok = false; break; }
                    if (markPart.equalsIgnoreCase("Absent")) s.subjectMarks.put(subj, -1);
                    else {
                        try {
                            int m = Integer.parseInt(markPart);
                            if (m < 0 || m > 100) { ok = false; break; }
                            s.subjectMarks.put(subj, m);
                        } catch (NumberFormatException ex) { ok = false; break; }
                    }
                }
                if (!ok) { skipped++; continue; }

                // Align student marks to global subject order and fill missing with Absent
                Map<String, Integer> aligned = new LinkedHashMap<>();
                for (String subj : subjectNames) {
                    aligned.put(subj, s.subjectMarks.containsKey(subj) ? s.subjectMarks.get(subj) : -1);
                }
                s.subjectMarks = aligned;

                s.calculateAverageAndGrade(subjectNames.size());
                students.add(s);
                loaded++;
            }

            refreshTable();
            JOptionPane.showMessageDialog(this, "Load complete: " + loaded + " loaded, " + skipped + " skipped, " + dup + " duplicates.");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage());
        }
    }

    // split CSV but keep quoted names intact
    private String[] splitCSVLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                cur.append(c);
            } else if (c == ',' && !inQuotes) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    private void showTop3Students() {
        if (students.isEmpty()) { JOptionPane.showMessageDialog(this, "No students available."); return; }
        List<Student> copy = new ArrayList<>(students);
        copy.sort((a,b) -> Float.compare(b.average, a.average));
        StringBuilder sb = new StringBuilder("Top 3 Students:\n");
        for (int i = 0; i < Math.min(3, copy.size()); i++) {
            Student s = copy.get(i);
            sb.append(String.format("%d. %s (Roll: %d) - Average: %.2f, Grade: %c\n", i+1, s.name, s.rollNumber, s.average, s.grade));
        }
        JOptionPane.showMessageDialog(this, sb.toString());
    }

    private void showFailedStudents() {
        StringBuilder sb = new StringBuilder("Failed Students (Grade F):\n");
        boolean found = false;
        for (Student s : students) {
            if (s.grade == 'F') {
                sb.append(String.format("%s (Roll: %d) - Average: %.2f\n", s.name, s.rollNumber, s.average));
                found = true;
            }
        }
        if (!found) sb.append("None");
        JOptionPane.showMessageDialog(this, sb.toString());
    }

    private void searchByRollDialog() {
        String in = JOptionPane.showInputDialog(this, "Enter Roll Number to search:");
        if (in == null || in.trim().isEmpty()) return;
        try {
            int roll = Integer.parseInt(in.trim());
            Student s = findStudentByRoll(roll);
            if (s == null) { JOptionPane.showMessageDialog(this, "No student found with roll " + roll); return; }
            showStudentInfoDialog(s);
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid roll number."); }
    }

    private void searchByNameDialog() {
        String name = JOptionPane.showInputDialog(this, "Enter Name or part of Name to search:");
        if (name == null || name.trim().isEmpty()) return;
        String low = name.toLowerCase();
        StringBuilder sb = new StringBuilder("Search Results:\n");
        boolean any = false;
        for (Student s : students) {
            if (s.name.toLowerCase().contains(low)) {
                sb.append(String.format("Roll: %d, Name: %s, Average: %.2f, Grade: %c\n", s.rollNumber, s.name, s.average, s.grade));
                any = true;
            }
        }
        if (!any) JOptionPane.showMessageDialog(this, "No students found matching \"" + name + "\".");
        else JOptionPane.showMessageDialog(this, sb.toString());
    }

    private void showStudentInfoDialog(Student s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(s.name).append("\n");
        sb.append("Roll Number: ").append(s.rollNumber).append("\n");
        sb.append("Subjects & Marks:\n");
        for (String subj : subjectNames) {
            Integer v = s.subjectMarks.get(subj);
            sb.append("  ").append(subj).append(": ").append((v == null || v == -1) ? "Absent" : v).append("\n");
        }
        sb.append(String.format("Average: %.2f\n", s.average));
        sb.append("Grade: ").append(s.grade).append("\n");
        JOptionPane.showMessageDialog(this, sb.toString(), "Student Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showLoginDialog() {
        JDialog dialog = new JDialog(this, "Student Login", true);
        dialog.setSize(300, 180);
        dialog.setLayout(new GridLayout(3,2,6,6));
        dialog.setLocationRelativeTo(this);

        dialog.add(new JLabel("Enter Roll Number:"));
        JTextField rollField = new JTextField();
        dialog.add(rollField);

        JButton loginBtn = new JButton("Login");
        dialog.add(new JLabel(""));
        dialog.add(loginBtn);

        loginBtn.addActionListener(e -> {
            String s = rollField.getText().trim();
            if (s.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Enter roll number."); return; }
            try {
                int roll = Integer.parseInt(s);
                Student st = findStudentByRoll(roll);
                if (st == null) { JOptionPane.showMessageDialog(dialog, "No student with roll " + roll); return; }
                dialog.dispose();
                showStudentInfoDialog(st);
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(dialog, "Invalid roll number."); }
        });

        dialog.setVisible(true);
    }

    // Single findStudentByRoll method (no duplicates)
    private Student findStudentByRoll(int roll) {
        for (Student s : students) if (s.rollNumber == roll) return s;
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new StudentGradeManagerGUI().setVisible(true);
        });
    }
}
