package info.kgeorgiy.ja.kupriyanov.student;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {

    private final Comparator<Student> STUDENT_COMPARATOR = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);

    private  final Comparator<Student> STUDENT_COMPARATOR_REVERSED_ID = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId, Comparator.reverseOrder());

    private List<String> mapStudentsToList(List<Student> students, Function<Student, String> mapper) {
        return students.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    private List<Student> sortStudents(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream()
                .filter(predicate) //up: исправил
                // 1000000
                .sorted(STUDENT_COMPARATOR)
                // 1
                // 1000000
                .sorted(STUDENT_COMPARATOR)
                // 1
                .filter(predicate)

                .collect(Collectors.toList());
    }

    public List<String> getFirstNames(List<Student> students) {
        return mapStudentsToList(students, Student::getFirstName);
    }

    public List<String> getLastNames(List<Student> students) {
        return mapStudentsToList(students, Student::getLastName);
    }
    public List<GroupName> getGroups(List<Student> students) {
        if (students == null) {
            return Collections.emptyList();
        }
        return students.stream()
                .map(Student::getGroup)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<String> getFullNames(List<Student> students) {
        return mapStudentsToList(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Comparator.comparing(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Comparator.comparing(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream()
                .sorted(Comparator.comparing(Student::getId))
                .collect(Collectors.toList());
    }
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream()
                .sorted(STUDENT_COMPARATOR_REVERSED_ID) //исправил компоратор на каждый вызов
                // Comparator на каждый вызов
                .collect(Collectors.toList());
    }


    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return sortStudents(students, student -> student.getFirstName().equals(name));
    }

    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortStudents(students, student -> student.getLastName().equals(name));
    }

    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return sortStudents(students, student -> student.getGroup().equals(group));
    }


    // Stream api
    //up: исправил
    @Override
    // Stream api

    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream()
                .filter(student -> student.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, (a, b) -> a.compareTo(b) < 0 ? a : b));
    }


}